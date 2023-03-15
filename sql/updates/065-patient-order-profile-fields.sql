BEGIN;
SELECT _v.register_patch('065-patient-order-profile-fields', NULL, NULL);

ALTER TABLE patient_order ADD COLUMN patient_ethnicity_id VARCHAR NOT NULL REFERENCES ethnicity(ethnicity_id) DEFAULT 'NOT_ASKED';
ALTER TABLE patient_order ADD COLUMN patient_race_id VARCHAR NOT NULL REFERENCES race(race_id) DEFAULT 'NOT_ASKED';
ALTER TABLE patient_order ADD COLUMN patient_gender_identity_id VARCHAR NOT NULL REFERENCES gender_identity(gender_identity_id) DEFAULT 'NOT_ASKED';
ALTER TABLE patient_order ADD COLUMN patient_language_code VARCHAR NOT NULL DEFAULT 'en';
ALTER TABLE patient_order ADD COLUMN patient_email_address VARCHAR;
ALTER TABLE patient_order RENAME COLUMN callback_phone_number TO patient_phone_number;

INSERT INTO patient_order_focus_type (patient_order_focus_type_id, description) VALUES ('EATING_DISORDER', 'Eating Disorder');

COMMIT;