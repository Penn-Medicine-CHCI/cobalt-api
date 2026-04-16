BEGIN;
SELECT _v.register_patch('249-institution-referrer-metadata', NULL, NULL);

ALTER TABLE institution_referrer
	ADD COLUMN IF NOT EXISTS metadata JSONB;

COMMIT;
