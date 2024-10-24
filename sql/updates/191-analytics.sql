BEGIN;
SELECT _v.register_patch('191-analytics', NULL, NULL);

ALTER TABLE analytics_native_event ADD COLUMN ip_address TEXT;

COMMIT;