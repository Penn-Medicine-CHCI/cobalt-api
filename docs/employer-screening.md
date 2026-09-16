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
This setting applies to every account using that source across institutions.
Clients should fall back to the existing UI for absent or unrecognized values.
New visual presentations require client support, but the backend passes their
identifiers through without code changes. The setting controls onboarding
presentation only.

The account response also exposes `onboardingScreeningFlowAppliesToAccount`. It is
true only when the institution has an onboarding screening flow and its enterprise
plugin considers the account eligible. Completion remains a separate concern.

Focused verification:

```sh
mvn -Dtest=AccountOnboardingScreeningPresentationTests,CobaltEmployerOnboardingTests test
```

The service tests require the local services and a database rebuilt through
local patch 266.
