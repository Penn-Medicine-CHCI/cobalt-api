BEGIN;
SELECT _v.register_patch('202-ic-config', NULL, NULL);

ALTER TABLE INSTITUTION ADD COLUMN integrated_care_patient_demographics_required BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE INSTITUTION ADD COLUMN integrated_care_patient_care_preference_visible BOOLEAN NOT NULL DEFAULT TRUE;

COMMIT;