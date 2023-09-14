BEGIN;
SELECT _v.register_patch('123-institution-privacy-policy', NULL, NULL);

ALTER TABLE institution ADD COLUMN privacy_policy_url TEXT;

COMMIT;