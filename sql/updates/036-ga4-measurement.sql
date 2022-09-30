BEGIN;
SELECT _v.register_patch('036-ga4-measurement', NULL, NULL);

ALTER TABLE institution ADD COLUMN ga4_measurement_id TEXT;

COMMIT;