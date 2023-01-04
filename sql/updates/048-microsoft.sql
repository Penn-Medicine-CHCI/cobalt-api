BEGIN;
SELECT _v.register_patch('048-microsoft', NULL, NULL);

ALTER TABLE institution ADD COLUMN microsoft_tenant_id TEXT;
ALTER TABLE institution ADD COLUMN microsoft_client_id TEXT;

ALTER TABLE account ADD COLUMN microsoft_id TEXT;

COMMIT;