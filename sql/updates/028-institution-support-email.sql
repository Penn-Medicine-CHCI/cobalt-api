BEGIN;
SELECT _v.register_patch('028-institution-support-email', NULL, NULL);

ALTER TABLE institution ADD COLUMN support_email_address TEXT NOT NULL DEFAULT 'support@cobaltinnovations.org';

COMMIT;