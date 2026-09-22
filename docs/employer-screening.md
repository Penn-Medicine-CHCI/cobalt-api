# Employer screening

Apply `sql/local/266-cobalt-employer-onboarding-single-question.sql` after the
local employer-onboarding fixture (264). This is local/bootstrap configuration;
real tenants require their own configuration patch.
Both local database rebuild scripts include it. It removes the welcome and
completion prompt references from the COBALT employer screening while preserving
existing sessions and answers. The employer question and decline option remain;
submitting either completes the screening.

Apply `sql/updates/265-onboarding-screening-presentation-and-cancellation-reasons.sql` before
running the updated API. It creates the final text-valued onboarding screening
presentation column directly; there is no intermediate boolean setting or conversion patch.

Account API responses expose `onboardingScreeningPresentationId`, read from
`account_source.onboarding_screening_presentation_id` on each response. This is a string,
so adding presentation identifiers requires no backend enum change. The database
default is `LARGE_MODAL`. Existing Cobalt SSO and UPHS/PennKey source rows start with
`SMALL_MODAL`. If these sources are provisioned later, set the presentation when
provisioning them.

Change a source's presentation with a data update; no backend release or restart is needed:

```sql
UPDATE account_source
SET onboarding_screening_presentation_id = 'SMALL_MODAL'
WHERE account_source_id IN ('PENN_SSO', 'PENN_KEY_SSO');
```

Use `LARGE_MODAL` for the existing screening UI or `SMALL_MODAL` for the compact modal.

Employer onboarding uses the `SUBMIT` screening-question submission style so
the final action does not imply that another question follows. Set
`screening_question.metadata.submitButtonText` to a nonblank string to replace
the default `Submit` label; the local and PENN employer fixtures use `Done`.
This setting applies to every account using that source across institutions.
Clients should fall back to the existing UI for absent or unrecognized values.
New visual presentations require client support, but the backend passes their
identifiers through without code changes. The setting controls onboarding
presentation only.

## Grouped institution locations

Migration `267-institution-location-group.sql` adds the
`institution_location_group` table and an optional
`institution_location.institution_location_group_id` reference. Institution-
location API responses expose group presentation data as an optional nested
object:

Feature databases that applied the earlier, unmerged `group_name` version of
migration 267 must be rebuilt. The replacement migration intentionally does not
retain or backfill that column.

```json
{
  "institutionLocationGroup": {
    "institutionLocationGroupId": "f6cd7e84-9c3a-4f14-8b74-138b5603d6bc",
    "name": "University of Pennsylvania Health System (UPHS)",
    "displayOrder": 1
  }
}
```

The locations response remains a flat list, and the single-location endpoint
uses the same optional nested representation. Grouped locations are ordered by
group `displayOrder` and then location `displayOrder`; ungrouped locations are
appended in location `displayOrder`. Institutions with no groups retain their
existing location ordering. Clients should group by
`institutionLocationGroupId`, use `name` as the option-group label, and render a
location with no group as an ordinary option.

An institution-location group is presentation metadata, not a parent location,
and has no provider-inheritance semantics. Provider and institution-referrer
eligibility continues to match the selected `institutionLocationId` exactly.

## Information footer callout

To request the blue informational treatment for a question's `footerText`, use
the following optional question metadata:

```json
{
  "footerCallout": {
    "title": "How we use this information",
    "displayTypeId": "PRIMARY"
  }
}
```

Clients that support this contract render the existing footer body with the
primary inline-alert styling and information icon. Missing, malformed, or
unrecognized callout metadata must fall back to the ordinary footer treatment.
This metadata does not change the question's input control or submission
behavior.

## PENN Booking V2 rollout contract

PENN configuration remains in the private tenant repository. Its rollout patch
must assert that `PENN.booking_v2_enabled` is `TRUE`; it must not enable Provider
Booking V2 implicitly. The patch publishes both a new screening version and a
new active screening-flow version. Completion checks are scoped to the active
flow version, so old sessions remain historical and every PENN account is
prompted once on its next homepage visit. Completing the replacement version
satisfies future completion checks. Disabling Booking V2 later does not
automatically restore the previous flow version.

Copy any `screening_flow_version_account_source` rows to the replacement flow
version so its audience remains unchanged.

The replacement screening is a single-select, auto-submit decision tree:

1. `Please select your employer`
   - `University of Pennsylvania Health System (UPHS)` routes to question 2a.
   - `University of Pennsylvania (UPenn)` routes to question 2b.
   - `I'm not sure / I'd rather not say` records the response and completes the
     session without changing the account's institution location.
2. Question 2a, `Where do you work at UPHS?`
   - `Pennsylvania Hospital (PAH)`
   - `Hospital of the University of Pennsylvania (HUP)`
   - `Penn Presbyterian Medical Center (PPMC)`
   - `Lancaster General Health (LGH)`
   - `Princeton Medical Center (PMC)`
   - `Doylestown`
   - `Chester County Hospital (CCH)`
   - `Outpatient Locations (e.g Cherry Hill, Radnor, Washington Sq.)`
   - `Corporate`
   - `Other`
3. Question 2b, `Where do you work at UPenn?`
   - `University`
   - `Perelman School of Medicine`
   - `Other`

All three questions use `prefer_autosubmit=TRUE`. Only questions 2a and 2b use
`shouldUpdateAccountInstitutionLocation`; their answer options carry the leaf
`institutionLocationId`. Scoring follows only the branch selected in question 1,
so replacing the employer answer invalidates the old branch and requires an
answer from the newly selected branch.

The PENN data patch should create fixed UPHS and UPenn
`institution_location_group` records. Reuse the existing broad UPHS and UPenn
location IDs as the corresponding `Other` entries, retain the LGH ID, and rename
the existing Princeton location to PMC. Add the remaining leaf locations and
assign every leaf through `institution_location_group_id` to the appropriate
question-1 employer group.

### PENN service eligibility

Reconcile both `provider_institution_location` and
`institution_feature_referrer_location` so provider search and referral-backed
services agree:

| Employer | Locations | Available services |
| --- | --- | --- |
| UPHS | PAH, HUP, PPMC, PMC, Doylestown, CCH, Outpatient Locations, Corporate, Other | The existing complete UPHS service set |
| UPHS | LGH | UPHS EAP, TEAM Clinic, Spiritual Support only |
| UPenn | University, Perelman School of Medicine, Other | UPenn EAP, TEAM Clinic, Spiritual Support only |

Dr. Steven Fetrow-Kiehl is not available to LGH. Resolve service providers and
referrers by stable IDs or canonical URL names and fail the tenant patch when an
expected record is missing or ambiguous. Providers without any location rows
are treated as globally available by Provider Booking V2, so any provider that
is not universal must receive explicit rows for every eligible leaf location.
The PENN enterprise plugin's legacy broad-location checks must likewise be
updated to recognize the applicable leaf location IDs.

The account response also exposes `onboardingScreeningFlowAppliesToAccount`. It is
true only when the institution has an onboarding screening flow and its enterprise
plugin considers the account eligible. Completion remains a separate concern.

Focused verification:

```sh
mvn -Dtest=AccountOnboardingScreeningPresentationTests,CobaltEmployerOnboardingTests,InstitutionResourceTests test
```

The service tests require the local services and a database rebuilt through
public patch 267 and local patch 266.
