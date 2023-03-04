BEGIN;
SELECT _v.register_patch('058-patient-order-updates', NULL, NULL);

CREATE TABLE patient_order_closure_reason (
  patient_order_closure_reason_id VARCHAR PRIMARY KEY,
  description VARCHAR NOT NULL,
  display_order SMALLINT NOT NULL
);

INSERT INTO patient_order_closure_reason VALUES ('NOT_CLOSED', 'Not Closed', 1);
INSERT INTO patient_order_closure_reason VALUES ('INELIGIBLE_DUE_TO_INSURANCE', 'Ineligible due to insurance', 2);
INSERT INTO patient_order_closure_reason VALUES ('REFUSED_CARE', 'Refused care', 3);
INSERT INTO patient_order_closure_reason VALUES ('TRANSFERRED_TO_SAFETY_PLANNING', 'Transferred to safety planning', 4);
INSERT INTO patient_order_closure_reason VALUES ('SCHEDULED_WITH_SPECIALTY_CARE', 'Scheduled with specialty care', 5);
INSERT INTO patient_order_closure_reason VALUES ('SCHEDULED_WITH_BHP', 'Scheduled with BHP', 6);

ALTER TABLE patient_order ADD COLUMN patient_order_closure_reason_id VARCHAR NOT NULL REFERENCES patient_order_closure_reason DEFAULT 'NOT_CLOSED';

ALTER TABLE patient_order ADD COLUMN resources_sent BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE patient_order ADD COLUMN resources_sent_at TIMESTAMPTZ;

-- To support IC
INSERT INTO screening_type (screening_type_id, description) VALUES ('IC_INTRO', 'Intro');
INSERT INTO screening_type (screening_type_id, description) VALUES ('IC_INTRO_CONDITIONS', 'Conditions');
INSERT INTO screening_type (screening_type_id, description) VALUES ('IC_INTRO_SYMPTOMS', 'Symptoms');
INSERT INTO screening_type (screening_type_id, description) VALUES ('IC_DRUG_USE_FREQUENCY', 'Drug Use - Frequency');
INSERT INTO screening_type (screening_type_id, description) VALUES ('IC_DRUG_USE_OPIOID', 'Drug Use - Opioid');
INSERT INTO screening_type (screening_type_id, description) VALUES ('BPI_1', 'BPI-1');
INSERT INTO screening_type (screening_type_id, description) VALUES ('PRIME_5', 'PRIME-5');

INSERT INTO screening_flow_type (screening_flow_type_id, description) VALUES ('INTEGRATED_CARE', 'Integrated Care');

ALTER TABLE patient_order_triage ADD COLUMN screening_session_id UUID REFERENCES screening_session;
ALTER TABLE patient_order_triage ADD COLUMN account_id UUID REFERENCES account;
ALTER TABLE patient_order_triage ADD COLUMN display_order INTEGER NOT NULL;

INSERT INTO patient_order_care_type VALUES ('SAFETY_PLANNING', 'Safety Planning');

COMMIT;