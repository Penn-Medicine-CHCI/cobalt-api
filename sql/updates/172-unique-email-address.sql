BEGIN;
SELECT _v.register_patch('172-unique-email-address', NULL, NULL);

DROP INDEX idx_account_email_address;

CREATE UNIQUE INDEX idx_account_email_address ON account USING btree (lower((email_address)::text), institution_id) 
WHERE (((account_source_id)::text = 'EMAIL_PASSWORD'::text) AND (active = true));

COMMIT;