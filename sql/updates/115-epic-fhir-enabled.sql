BEGIN;
SELECT _v.register_patch('115-epic-fhir-enabled', NULL, NULL);

ALTER TABLE institution ADD COLUMN epic_fhir_enabled BOOLEAN NOT NULL DEFAULT FALSE;

COMMIT;