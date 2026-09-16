# Provider Booking V2 Rollout

Provider Booking V2 is controlled by `institution.booking_v2_enabled`. Keep the
flag disabled while applying and validating the database and application
changes described here.

## Production patch selection

Apply the production patches in this order:

1. `sql/updates/261-provider-booking-database.sql`
2. `sql/updates/262-care-navigator.sql`
3. `sql/updates/263-care-navigator-screening.sql`
4. `sql/updates/264-provider-institution-referrer.sql`

Both Care Navigator patches must be applied before deploying an application
build that exposes Care Navigator APIs. The first installs the shared encounter
schema and baseline behavior. The second installs the canonical intake and
attaches it to the configured COBALT Care Navigator provider's appointment
types. The provider-referrer patch must be applied before deploying an
application build that exposes referral-backed provider profiles. Provider,
staff mapping, appointment type, and availability provisioning remain
tenant-specific.

Never apply any of these local fixture patches to a production database:

- `sql/local/261-cobalt-provider-booking-configuration.sql`
- `sql/local/262-provider-booking-seed.sql`
- `sql/local/263-care-navigator-seed.sql`
- `sql/local/264-cobalt-employer-onboarding.sql`
- `sql/local/265-team-clinic-referral-provider-seed.sql`
- `sql/local/266-cobalt-employer-onboarding-single-question.sql`

They live outside the production update directory because they contain test
accounts, fixed fixture identifiers, synthetic clinical
records, and local-only availability. The `262` fixture also deliberately
enables V2 for the local COBALT institution.

`261-cobalt-provider-booking-configuration.sql` is every statement scoped to the
COBALT testing institution, configuring the Autism Clinic bootstrap fixture for
booking v2. Keep it as the worked example of what tenant configuration involves,
but provision a real tenant with its own reviewed patch rather than by copying
it. Nothing in the production chain depends on it.

`264-cobalt-employer-onboarding.sql` creates the COBALT-branded local onboarding
flow and snapshots the local COBALT institution locations as employer choices.
`266-cobalt-employer-onboarding-single-question.sql` removes its welcome and
completion prompts, leaving only the employer question.
Provision a real tenant, including PENN, with a reviewed enterprise patch using
tenant-approved copy and employer choices. Do not run the local fixture outside
local or bootstrap databases.

`265-team-clinic-referral-provider-seed.sql` exposes the existing local TEAM
Clinic institution referrer as a provider profile. It reuses the referrer's
one-question intake flow and destination while deliberately creating no native
appointment types, scheduling integration, contact information, locations, or
availability. It runs only when the optional full bootstrap dataset is present;
real institutions require their own reviewed enterprise fixture.

## Preflight checks

Run these checks against a recent production snapshot before scheduling the
production migration. Every query must return zero rows.

### Active native appointments with overlapping time ranges

```sql
SELECT
  first_appointment.appointment_id AS first_appointment_id,
  second_appointment.appointment_id AS second_appointment_id,
  first_appointment.provider_id,
  first_appointment.start_time AS first_start_time,
  first_appointment.end_time AS first_end_time,
  second_appointment.start_time AS second_start_time,
  second_appointment.end_time AS second_end_time
FROM appointment first_appointment
JOIN appointment second_appointment
  ON second_appointment.provider_id=first_appointment.provider_id
  AND second_appointment.appointment_id>first_appointment.appointment_id
WHERE first_appointment.canceled=FALSE
AND second_appointment.canceled=FALSE
AND first_appointment.scheduling_system_id='COBALT'
AND second_appointment.scheduling_system_id='COBALT'
AND tsrange(first_appointment.start_time, first_appointment.end_time, '[)')
  && tsrange(second_appointment.start_time, second_appointment.end_time, '[)');
```

Do not automatically delete or cancel rows returned by this query. Reconcile
each pair using the appointment audit trail and the operational scheduling
system before applying the migration.

### Appointment types shared across institutions

```sql
SELECT
  ata.appointment_type_id,
  ata.assessment_id,
  ARRAY_AGG(DISTINCT provider.institution_id ORDER BY provider.institution_id) AS institution_ids
FROM appointment_type_assessment ata
JOIN appointment_type app_type
  ON app_type.appointment_type_id=ata.appointment_type_id
JOIN provider_appointment_type pat
  ON pat.appointment_type_id=ata.appointment_type_id
JOIN provider
  ON provider.provider_id=pat.provider_id
WHERE ata.active=TRUE
AND COALESCE(app_type.deleted, FALSE)=FALSE
AND NOT EXISTS (
  SELECT 1
  FROM provider_appointment_type assessed_pat
  JOIN provider assessed_provider
    ON assessed_provider.provider_id=assessed_pat.provider_id
  JOIN institution assessed_institution
    ON assessed_institution.institution_id=assessed_provider.institution_id
  WHERE assessed_pat.appointment_type_id=ata.appointment_type_id
  AND assessed_institution.integrated_care_enabled=TRUE
)
GROUP BY ata.appointment_type_id, ata.assessment_id
HAVING COUNT(DISTINCT provider.institution_id)>1;
```

### Institutions without an account to own generated screening records

```sql
SELECT DISTINCT provider.institution_id
FROM appointment_type_assessment ata
JOIN appointment_type app_type
  ON app_type.appointment_type_id=ata.appointment_type_id
JOIN provider_appointment_type pat
  ON pat.appointment_type_id=ata.appointment_type_id
JOIN provider
  ON provider.provider_id=pat.provider_id
WHERE ata.active=TRUE
AND COALESCE(app_type.deleted, FALSE)=FALSE
AND NOT EXISTS (
  SELECT 1
  FROM provider_appointment_type assessed_pat
  JOIN provider assessed_provider
    ON assessed_provider.provider_id=assessed_pat.provider_id
  JOIN institution assessed_institution
    ON assessed_institution.institution_id=assessed_provider.institution_id
  WHERE assessed_pat.appointment_type_id=ata.appointment_type_id
  AND assessed_institution.integrated_care_enabled=TRUE
)
AND NOT EXISTS (
  SELECT 1
  FROM account
  WHERE account.institution_id=provider.institution_id
);
```

## Migration rehearsal

1. Restore a recent production snapshot into a non-production environment.
2. Run the preflight checks above.
3. Apply all four production patches listed above in the documented order.
4. Record migration duration and lock time for the `appointment` table.
5. Verify that `booking_v2_enabled` remains `FALSE` for every institution.
6. Run the API test suite and exercise V1 provider search before promoting the
   application build.

## Care Navigator tenant provisioning

After the shared patches are installed, create a reviewed, idempotent
production configuration patch for each tenant that will offer Care Navigator.
Make that patch depend on `262-care-navigator` and keep tenant-approved copy,
staff identities, scheduling hours, contact information, and generated UUIDs in
that patch. Do not copy or run the local fixture patches
in production.

Provision each tenant in this order so the database validation triggers can
enforce every relationship:

1. Create a dedicated, active native provider in the target institution with
   the approved name, description, privacy/crisis copy, locale, time zone,
   contact details, videoconference configuration, locations, and payment
   types. Do not repurpose a patient-triage provider with appointment history:
   assigning the Care Navigator role intentionally leaves its existing
   appointments outside care encounters.
2. Add `CARE_NAVIGATOR` to `provider_support_role` for the booking provider.
   This also makes the provider virtual-only. Verify
   `provider.virtual_appointments_only=TRUE` before publishing availability.
3. For every staff account that will serve the provider, verify that it is
   active, belongs to the same institution, and has role `ADMINISTRATOR` or
   `PROVIDER`. Grant the `NAVIGATOR` account capability, then insert its
   `care_navigator_provider_account` mapping and deliberate `display_order`.
   At least one currently valid mapping is required before launch.
4. Insert or update the tenant's `RESOURCE_NAVIGATOR` `institution_feature` and
   set its `provider_id` to the Care Navigator booking provider. Configure the
   approved navigation copy and visibility, but keep it hidden until the smoke
   tests below pass.
5. Create and activate the tenant-approved `PROVIDER_INTAKE` screening flow and
   screening version. The active screening must contain exactly one required
   contact-email question with `screening_answer_format_id='FREEFORM_TEXT'`,
   `screening_answer_content_hint_id='EMAIL_ADDRESS'`, and minimum/maximum
   answer counts of `1`. Include that question in the screening scoring and
   completion logic. More than one answered email-hint question makes Care
   Navigator contact extraction fail rather than choosing an ambiguous address.
6. Create the native appointment type, associate it with the active intake flow
   through `appointment_type.screening_flow_id`, and associate it with the
   provider through `provider_appointment_type`. The completed eligible path
   must produce `APPOINTMENT_BOOKING_CONFIRMATION` with a successful screening
   result.
7. Create approved `logical_availability` rows and associate each one with the
   appointment type through `logical_availability_appointment_type`. Confirm
   that the time zone, recurrence, start/end dates, duration, and
   videoconference behavior match the tenant's operating plan.

Use the database eligibility function installed by patch `262-care-navigator`
to verify every staff mapping; the raw presence of a mapping row is not
sufficient:

```sql
SELECT
  COUNT(*) AS mapping_count,
  BOOL_AND(care_navigator_account_can_serve_provider(
    mapping.account_id,
    mapping.provider_id
  )) AS all_currently_authorized
FROM care_navigator_provider_account mapping
WHERE mapping.provider_id=:provider_id;
```

This must return a positive `mapping_count` and
`all_currently_authorized=TRUE`. Also verify the tenant feature points to the
same active, virtual-only Care Navigator provider:

```sql
SELECT
  institution_feature.institution_id,
  institution_feature.provider_id,
  provider.active,
  provider.virtual_appointments_only
FROM institution_feature
JOIN provider
  ON provider.provider_id=institution_feature.provider_id
WHERE institution_feature.institution_id=:institution_id
AND institution_feature.feature_id='RESOURCE_NAVIGATOR'
AND provider.institution_id=institution_feature.institution_id
AND EXISTS (
  SELECT 1
  FROM provider_support_role
  WHERE provider_support_role.provider_id=provider.provider_id
  AND provider_support_role.support_role_id='CARE_NAVIGATOR'
);
```

This must return exactly one row with both booleans `TRUE`. Verify the active
intake screening has exactly one contact-email question:

```sql
SELECT COUNT(*) AS contact_email_question_count
FROM appointment_type
JOIN screening_flow
  ON screening_flow.screening_flow_id=appointment_type.screening_flow_id
JOIN screening_flow_version
  ON screening_flow_version.screening_flow_version_id=
    screening_flow.active_screening_flow_version_id
JOIN screening
  ON screening.screening_id=screening_flow_version.initial_screening_id
JOIN screening_version
  ON screening_version.screening_version_id=
    screening.active_screening_version_id
JOIN screening_question
  ON screening_question.screening_version_id=
    screening_version.screening_version_id
WHERE appointment_type.appointment_type_id=:appointment_type_id
AND screening_flow.institution_id=:institution_id
AND screening_question.screening_answer_format_id='FREEFORM_TEXT'
AND screening_question.screening_answer_content_hint_id='EMAIL_ADDRESS'
AND screening_question.minimum_answer_count=1
AND screening_question.maximum_answer_count=1;
```

This must return `1`.

## Tenant activation

Before enabling a tenant, verify all of the following:

- Appointment-type screening flows survive a read/edit/save round trip.
- A newer failed screening revokes an older successful screening.
- Only the active screening-flow version satisfies booking requirements.
- Native sequential and concurrent overlap tests pass.
- Clinic bookings preserve the provider attached to the selected slot.
- Pre-V2 appointments can be rescheduled safely.
- Any alternate appointment email address has been verified for the account.
- EPIC FHIR/MyChart and institution-location behavior has been validated for
  the tenant's configuration.
- The Care Navigator feature resolves to its configured booking provider.
- An eligible user can complete the intake, including the contact-email
  question, and book an offered Care Navigator slot.
- The new appointment receives a care encounter whose contact email matches
  the normalized screening response.
- Every mapped Navigator can view and cancel the appointment, while an
  unmapped Navigator cannot.
- Attendance, patient cancellation, encounter notes, and follow-up scheduling
  have been exercised without sending a real production message.

Enable one tenant at a time:

```sql
BEGIN;

UPDATE institution
SET booking_v2_enabled=TRUE
WHERE institution_id=:institution_id
AND integrated_care_enabled=FALSE;

UPDATE institution_feature
SET nav_visible=:approved_nav_visible,
  landing_page_visible=:approved_landing_page_visible
WHERE institution_id=:institution_id
AND feature_id='RESOURCE_NAVIGATOR'
AND provider_id=:provider_id;

COMMIT;
```

Both updates must affect exactly one row. Roll back the transaction if either
does not.

## Rollback

The experience can be returned to V1 without deleting migrated screening
records:

```sql
BEGIN;

UPDATE institution
SET booking_v2_enabled=FALSE
WHERE institution_id=:institution_id;

UPDATE institution_feature
SET nav_visible=FALSE,
  landing_page_visible=FALSE
WHERE institution_id=:institution_id
AND feature_id='RESOURCE_NAVIGATOR';

COMMIT;
```

After rollback, preserve appointment and screening data for investigation. Do
not reverse the schema migration, delete Care Navigator mappings, providers,
encounters, messages, or generated screening flows, or automatically cancel
already-booked appointments during incident response.
