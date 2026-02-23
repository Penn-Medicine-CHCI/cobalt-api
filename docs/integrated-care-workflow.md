# Integrated Care Workflow (API)

This document maps the API-side integrated-care lifecycle from referral ingestion to order closure.

## Domain Terms

- `patient_order`: referral-derived work item used by MHICs and patient IC flows.
- `MHIC`: care coordination/admin role that manages open orders.
- `triage`: assignment of care type/focus and urgency path.
- `resourcing`: sharing external/community resources when collaborative care is not the immediate path.

## End-to-End Flow

1. Referral ingestion
- EPIC/HL7/FHIR feeds are handled through integration services.
- Key files:
  - `src/main/java/com/cobaltplatform/api/integration/hl7/Hl7Client.java`
  - `src/main/java/com/cobaltplatform/api/integration/epic/EpicSyncManager.java`
  - `src/main/java/com/cobaltplatform/api/integration/epic/EpicFhirSyncManager.java`

2. Order creation and sync
- Imported referrals become `patient_order` rows and related tracking records.
- Key files:
  - `src/main/java/com/cobaltplatform/api/service/PatientOrderSyncService.java`
  - `src/main/java/com/cobaltplatform/api/service/PatientOrderService.java`

3. Patient interaction surface
- Patient IC endpoints for open order retrieval, demographics patching, consent, assessments, and screening context.
- Primary resource:
  - `src/main/java/com/cobaltplatform/api/web/resource/PatientOrderResource.java`

4. MHIC operational surface
- Panel queues, assignment, outreach, voicemail tasks, scheduled messages/screenings, triage override/revert, resourcing, safety planning.
- Primary resource and service:
  - `src/main/java/com/cobaltplatform/api/web/resource/PatientOrderResource.java`
  - `src/main/java/com/cobaltplatform/api/service/PatientOrderService.java`

5. Closure and reporting
- Orders are closed/reopened and contribute to analytics/reporting exports.
- Relevant files:
  - `src/main/java/com/cobaltplatform/api/service/PatientOrderService.java`
  - `src/main/java/com/cobaltplatform/api/service/ReportingService.java`
  - `src/main/java/com/cobaltplatform/api/service/AnalyticsService.java`

## Typical Change Playbooks

### Add a new IC field

1. Add DB migration in `sql/updates/`.
2. Update model(s) under `src/main/java/com/cobaltplatform/api/model/`.
3. Update service logic in `PatientOrderService`.
4. Expose field in `PatientOrderApiResponse` factory/response models.
5. Add/adjust endpoint behavior in `PatientOrderResource` if needed.
6. Update web models/service consumers.

### Add a new MHIC action

1. Decide endpoint contract in `PatientOrderResource`.
2. Implement transaction-safe state updates in `PatientOrderService`.
3. Add footprint/event instrumentation where needed.
4. Add auth checks in `AuthorizationService`/resource path.
5. Update web IC services and route UX.

### Modify referral import behavior

1. Update integration parser/sync manager logic.
2. Validate order dedupe/idempotency paths in sync service.
3. Add tests for changed mapping logic.
4. Verify downstream MHIC queue behavior.

## Validation Checklist

- New/changed endpoint covered by unit/integration tests.
- Local IC patient flow still loads with an open order.
- MHIC list/search/assessment/order detail actions still work.
- CSV/reporting or scheduled jobs unaffected (or intentionally updated).
