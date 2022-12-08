BEGIN;
SELECT _v.register_patch('045-consent-form-rejected', NULL, NULL);

ALTER TABLE account ADD COLUMN consent_form_rejected_date TIMESTAMP WITH TIME ZONE;

COMMIT;