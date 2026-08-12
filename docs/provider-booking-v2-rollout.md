# Provider Booking V2 Rollout

Provider Booking V2 is controlled by `institution.booking_v2_enabled`. Keep the
flag disabled while applying and validating the database and application
changes described here.

## Production patch selection

Apply the production patches in this order:

1. `sql/updates/259-provider-booking-database.sql`
2. `sql/updates/259-cobalt-provider-booking-configuration.sql`

Never apply `sql/local/259-provider-booking-seed.sql` to a production database.
It lives outside the production update directory because it contains developer
fixtures and deliberately enables V2 for the local COBALT institution.

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
3. Apply the two production patches in the documented order.
4. Record migration duration and lock time for the `appointment` table.
5. Verify that `booking_v2_enabled` remains `FALSE` for every institution.
6. Run the API test suite and exercise V1 provider search before promoting the
   application build.

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

Enable one tenant at a time:

```sql
UPDATE institution
SET booking_v2_enabled=TRUE
WHERE institution_id=:institution_id
AND integrated_care_enabled=FALSE;
```

## Rollback

The experience can be returned to V1 without deleting migrated screening
records:

```sql
UPDATE institution
SET booking_v2_enabled=FALSE
WHERE institution_id=:institution_id;
```

After rollback, preserve appointment and screening data for investigation. Do
not reverse the schema migration or delete generated screening flows during an
incident response.
