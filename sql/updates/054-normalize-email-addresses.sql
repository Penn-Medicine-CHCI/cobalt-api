BEGIN;
SELECT _v.register_patch('054-normalize-email-addresses', NULL, NULL);

-- Make sure emails are always normalized to lower case
UPDATE account SET email_address=LOWER(email_address) WHERE email_address != LOWER(email_address);
UPDATE account_email_verification SET email_address=LOWER(email_address) WHERE email_address != LOWER(email_address);

COMMIT;