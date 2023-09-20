BEGIN;
SELECT _v.register_patch('125-google-bigquery', NULL, NULL);

ALTER TABLE institution ADD COLUMN google_bigquery_service_account_private_key TEXT;

COMMIT;