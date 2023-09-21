BEGIN;
SELECT _v.register_patch('125-google-bigquery', NULL, NULL);

ALTER TABLE institution ADD COLUMN google_reporting_service_account_private_key TEXT;
ALTER TABLE institution ADD COLUMN ga4_property_id TEXT;

COMMIT;