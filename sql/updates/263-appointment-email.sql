BEGIN;
SELECT _v.register_patch('263-appointment-email', NULL, NULL);

ALTER TABLE appointment ADD COLUMN IF NOT EXISTS email_address TEXT NULL;

UPDATE appointment app
SET email_address = a.email_address
FROM account a
WHERE a.account_id = app.account_id
AND app.email_address IS NULL;

COMMIT;
