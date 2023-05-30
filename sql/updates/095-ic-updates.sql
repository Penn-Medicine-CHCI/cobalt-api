BEGIN;
SELECT _v.register_patch('095-ic-updates', NULL, NULL);

-- We will be deprecating ga4_measurement_id in favor of these two fields.
-- Leaving it in place for now for backwards compatibility
ALTER TABLE institution ADD COLUMN ga4_patient_measurement_id TEXT;
ALTER TABLE institution ADD COLUMN ga4_staff_measurement_id TEXT;

-- Use the legacy field to populate ga4_patient_measurement_id
UPDATE institution SET ga4_patient_measurement_id=ga4_measurement_id;

COMMIT;