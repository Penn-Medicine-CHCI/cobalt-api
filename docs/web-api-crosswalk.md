# Integrated Care API/Web Crosswalk

This maps web client methods in `cobalt-web-mirror-codex/src/lib/services/integrated-care-service.ts`
to API routes defined in `src/main/java/com/cobaltplatform/api/web/resource/PatientOrderResource.java`.

## Core Order Lifecycle

| Web method | HTTP | API route |
|---|---|---|
| `getPatientOrders` | GET | `/patient-orders` |
| `getPatientOrder` | GET | `/patient-orders/{patientOrderId}` |
| `getOpenOrderForCurrentPatient` | GET | `/patient-orders/open` |
| `getLatestPatientOrder` | GET | `/patient-orders/latest` |
| `patchPatientOrder` | PATCH | `/patient-orders/{patientOrderId}` |
| `closePatientOrder` | PUT | `/patient-orders/{patientOrderId}/close` |
| `openPatientOrder` | PUT | `/patient-orders/{patientOrderId}/open` |
| `resetPatientOrder` | PUT | `/patient-orders/{patientOrderId}/reset` |
| `getClinicalReport` | GET | `/patient-orders/{patientOrderId}/clinical-report` |

## Assignment, Search, and Imports

| Web method | HTTP | API route |
|---|---|---|
| `autocompletePatientOrders` | GET | `/patient-orders/autocomplete` |
| `assignPatientOrders` | POST | `/patient-orders/assign` |
| `importPatientOrders` | POST | `/patient-order-imports` |
| `getPanelAccounts` | GET | `/integrated-care/panel-accounts` |
| `getPanelCounts` | GET | `/integrated-care/panel-counts` |
| `getOverview` | GET | `/integrated-care/panel-today` |

## Triage, Consent, Resourcing, Safety

| Web method | HTTP | API route |
|---|---|---|
| `updatePatientOrderConsentStatus` | PUT | `/patient-orders/{patientOrderId}/consent-status` |
| `overrideTriage` | PUT | `/patient-orders/{patientOrderId}/patient-order-triage-groups` |
| `revertTriage` | PUT | `/patient-orders/{patientOrderId}/reset-patient-order-triages` |
| `updateResourcingStatus` | PUT | `/patient-orders/{patientOrderId}/patient-order-resourcing-status` |
| `updateSafetyPlanningStatus` | PUT | `/patient-orders/{patientOrderId}/patient-order-safety-planning-status` |
| `updatePatientOrderResourceCheckInResponseStatusId` | PUT | `/patient-orders/{patientOrderId}/patient-order-resource-check-in-response-status` |
| `getReferenceData` | GET | `/patient-orders/reference-data` |

## Notes, Outreach, Messaging, Tasks

| Web method | HTTP | API route |
|---|---|---|
| `postNote` / `updateNote` / `deleteNote` | POST/PUT/DELETE | `/patient-order-notes` (+ `/{id}` for update/delete) |
| `postPatientOrderOutreach` / `updatePatientOrderOutreach` / `deletePatientOrderOutreach` | POST/PUT/DELETE | `/patient-order-outreaches` (+ `/{id}` for update/delete) |
| `sendMessage` / `updateMessage` / `deleteMessage` | POST/PUT/DELETE | `/patient-order-scheduled-message-groups` (+ `/{id}` for update/delete) |
| `scheduleAssessment` / `updateScheduledAssessment` / `deleteScheduledAssessment` | POST/PUT/DELETE | `/patient-order-scheduled-screenings` (+ `/{id}` for update/delete) |
| `createVoicemailTask` / `updateVoicemailTask` | POST/PUT | `/patient-order-voicemail-tasks` (+ `/{id}` for update) |
| `completeVoicemailTask` | POST | `/patient-order-voicemail-tasks/{id}/complete` |
| `createScheduledOutreach` / `updateScheduledOutreach` | POST/PUT | `/patient-order-scheduled-outreaches` (+ `/{id}` for update) |
| `cancelScheduledOutreaach` | POST | `/patient-order-scheduled-outreaches/{id}/cancel` |
| `completeScheduledOutreaach` | POST | `/patient-order-scheduled-outreaches/{id}/complete` |

## Epic Department and Encounter Utilities

| Web method | HTTP | API route |
|---|---|---|
| `getEpicDepartments` | GET | `/integrated-care/epic-departments` |
| `setEpicDepartmentAvailabilityStatus` | PUT | `/integrated-care/epic-departments/{epicDepartmentId}` |
| `getEcounters` | GET | `/patient-orders/{patientOrderId}/encounters` |
| `setEncounterCsn` | PUT | `/patient-orders/{patientOrderId}/encounter-csn` |
| `overrideSchedulingEpicDepartment` | PUT | `/patient-orders/{patientOrderId}/override-scheduling-epic-department` |

## Maintenance Notes

- Keep this file updated whenever IC endpoint paths change.
- Existing web method names include legacy typos (`getEcounters`, `cancelScheduledOutreaach`, `completeScheduledOutreaach`); avoid accidental API path mismatch during refactors.
