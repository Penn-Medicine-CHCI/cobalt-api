BEGIN;
SELECT _v.register_patch('193-analytics', NULL, NULL);

ALTER TABLE analytics_native_event ALTER COLUMN ip_address TYPE inet USING ip_address::inet;

COMMIT;