BEGIN;
SELECT _v.register_patch('032-institution-updates', NULL, NULL);

ALTER TABLE institution ADD COLUMN immediate_access_enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE institution ADD COLUMN contact_us_enabled BOOLEAN NOT NULL DEFAULT TRUE;

COMMIT;