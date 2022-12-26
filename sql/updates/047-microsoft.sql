BEGIN;
SELECT _v.register_patch('047-microsoft', NULL, NULL);

ALTER TABLE institution ADD COLUMN microsoft_tenant_id TEXT;
ALTER TABLE institution ADD COLUMN microsoft_client_id TEXT;

COMMIT;