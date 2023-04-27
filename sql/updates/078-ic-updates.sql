BEGIN;
SELECT _v.register_patch('078-ic-updates', NULL, NULL);

ALTER TABLE institution ADD COLUMN default_from_email_address TEXT;

COMMIT;