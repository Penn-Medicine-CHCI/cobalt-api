BEGIN;
SELECT _v.register_patch('227-patient-order-study', NULL, NULL);

-- Associates a patient order with a study
CREATE TABLE patient_order_study (
	patient_order_id UUID NOT NULL REFERENCES patient_order,
	study_id UUID NOT NULL REFERENCES study,
	display_order SMALLINT NOT NULL,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	PRIMARY KEY (patient_order_id, study_id)
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON patient_order_study FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

COMMIT;