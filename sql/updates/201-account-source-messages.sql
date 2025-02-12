BEGIN;
SELECT _v.register_patch('201-account-source-messages', NULL, NULL);

-- Optional institution-specific account source messaging, useful for informing users of things like "If you work at X, please use Anonymous access".
-- The supplement_message value can include HTML.
-- The supplement_message_style is from our styleguide, e.g. one of "primary, secondary, success, warning, danger, info, dark, light"
ALTER TABLE institution_account_source ADD COLUMN supplement_message VARCHAR;
ALTER TABLE institution_account_source ADD COLUMN supplement_message_style VARCHAR;

COMMIT;