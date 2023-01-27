BEGIN;
SELECT _v.register_patch('054-order', NULL, NULL);

CREATE TABLE order_import_type (
  order_import_type_id VARCHAR PRIMARY KEY,
  description VARCHAR NOT NULL
);

INSERT INTO order_import_type VALUES ('CSV', 'CSV');
INSERT INTO order_import_type VALUES ('EPIC', 'Epic');

CREATE TABLE order_import (
	order_import_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	order_import_type_id VARCHAR NOT NULL REFERENCES order_import_type,
	institution_id VARCHAR NOT NULL REFERENCES institution,
  account_id UUID REFERENCES account,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON order_import FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE patient_order (
	patient_order_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	order_import_id UUID NOT NULL REFERENCES order_import,
	account_id UUID REFERENCES account,
	encounter_department_id VARCHAR,
  encounter_department_name VARCHAR,
  referring_practice_id VARCHAR, -- not currently provided
  referring_practice_id_type VARCHAR, -- not currently provided
  referring_practice_name VARCHAR,
  ordering_provider_id VARCHAR, -- not currently provided
  ordering_provider_id_type VARCHAR, -- not currently provided
  ordering_provider_name VARCHAR,
  billing_provider_id VARCHAR, -- not currently provided
  billing_provider_id_type VARCHAR, -- not currently provided
  billing_provider_name VARCHAR,
  patient_last_name VARCHAR NOT NULL,
  patient_first_name VARCHAR NOT NULL,
  patient_mrn VARCHAR NOT NULL,
  patient_uid VARCHAR NOT NULL,
  patient_sex VARCHAR,
  patient_birthdate DATE,
  patient_address_line_1 VARCHAR,
  patient_address_line_2 VARCHAR,
  patient_city VARCHAR,
  patient_postal_code VARCHAR,
  patient_region VARCHAR, -- In the US, this is the city
  patient_country_code VARCHAR,
  primary_payor VARCHAR,
  primary_plan VARCHAR,
  order_date DATE,
  order_age_in_minutes INTEGER,
  order_id VARCHAR NOT NULL,
  routing VARCHAR,
  reason_for_referral VARCHAR,
  diagnosis VARCHAR,
  associated_diagnosis VARCHAR,
  callback_phone_number VARCHAR,
  preferred_contact_hours VARCHAR,
  comments VARCHAR,
  img_cc_recipients VARCHAR,
  last_active_medication_order_summary VARCHAR,
  medications VARCHAR,
  recent_psychotherapeutic_medications VARCHAR,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON patient_order FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

COMMIT;