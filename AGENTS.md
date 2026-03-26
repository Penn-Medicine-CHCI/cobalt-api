# Cobalt API Contributor Guide

## Purpose

`cobalt-api-mirror-codex` is the backend for Cobalt's mental-health product lines, including:

- Institutional/internal experiences (employee support, courses, content, scheduling)
- Integrated care (patient referrals, MHIC workflows, assessments, triage, outreach)

## Stack

- Java 17
- Maven
- Soklet web framework + Guice DI
- PostgreSQL (primary + read replica config)
- Redis cache
- Localstack for local AWS simulation

## Quick Start

1. Start Localstack + local Postgres:

```bash
./start-localstack
```

2. Bootstrap Localstack and DB:

```bash
./bootstrap
```

3. Start the API:

```bash
./start-backend
```

Optional environment overrides:

```bash
COBALT_API_ENV=local COBALT_API_PORT=4000 ./start-backend
```

## Tests

```bash
mvn test -Dgroups="com.cobaltplatform.api.UnitTest"
mvn test -Dgroups="com.cobaltplatform.api.IntegrationTest"
```

Integration tests require local dependencies (Postgres/Redis/Localstack) to be running.

## Directory Map

- `src/main/java/com/cobaltplatform/api/web/resource/`: HTTP resources/endpoints
- `src/main/java/com/cobaltplatform/api/service/`: core business logic
- `src/main/java/com/cobaltplatform/api/integration/`: external integrations (Epic, HL7, Twilio, etc.)
- `src/main/java/com/cobaltplatform/api/integration/enterprise/`: institution-specific plugin behavior
- `sql/initial/`: base schema + seed
- `sql/updates/`: incremental migrations
- `config/local/`: local runtime configuration and cert/key material

## Integrated Care Hotspots

- `src/main/java/com/cobaltplatform/api/service/PatientOrderService.java`
- `src/main/java/com/cobaltplatform/api/service/PatientOrderSyncService.java`
- `src/main/java/com/cobaltplatform/api/web/resource/PatientOrderResource.java`
- `src/main/java/com/cobaltplatform/api/integration/hl7/Hl7Client.java`
- `src/main/java/com/cobaltplatform/api/integration/epic/EpicSyncManager.java`
- `src/main/java/com/cobaltplatform/api/integration/epic/EpicFhirSyncManager.java`

## Migrations and Schema Changes

When adding schema changes:

1. Add a new `sql/updates/<NNN>-<description>.sql` file.
2. Update `sql/recreate-local` and `sql/recreate-bootstrap` to include the new script in order.
3. Keep migration ordering deterministic for local bootstrap and CI reliability.

## API Contract Changes

The web integrated-care client lives at:

- `../cobalt-web-mirror-codex/src/lib/services/integrated-care-service.ts`

If you change IC endpoint paths, payloads, or response fields:

1. Update the API resource and service logic.
2. Update web service method typings.
3. Update web models under `../cobalt-web-mirror-codex/src/lib/models/`.
4. Verify both MHIC and patient flows still load.

## Related Docs

- `docs/integrated-care-workflow.md`
- `docs/web-api-crosswalk.md`
