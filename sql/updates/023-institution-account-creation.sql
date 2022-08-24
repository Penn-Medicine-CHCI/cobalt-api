BEGIN;
SELECT _v.register_patch('023-institution-account-creation', NULL, NULL);

ALTER TABLE institution ADD COLUMN email_signup_enabled BOOLEAN NOT NULL DEFAULT FALSE;

COMMIT;