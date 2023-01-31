BEGIN;
SELECT _v.register_patch('056-order', NULL, NULL);

-- We might import orders via an EHR like Epic or via manual CSV upload
CREATE TABLE order_import_type (
  order_import_type_id VARCHAR PRIMARY KEY,
  description VARCHAR NOT NULL
);

INSERT INTO order_import_type VALUES ('CSV', 'CSV');
INSERT INTO order_import_type VALUES ('EPIC', 'Epic');

-- Keep track of who imports, and how they import
CREATE TABLE order_import (
  order_import_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  order_import_type_id VARCHAR NOT NULL REFERENCES order_import_type,
  institution_id VARCHAR NOT NULL REFERENCES institution,
  account_id UUID REFERENCES account,
  raw_order TEXT NOT NULL, -- permanent record of the CSV or EHR import data
  created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON order_import FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE patient_order_status (
  patient_order_status_id VARCHAR PRIMARY KEY,
  description VARCHAR NOT NULL,
  terminal BOOLEAN NOT NULL DEFAULT FALSE
);

INSERT INTO patient_order_status VALUES ('NEW', 'New');
INSERT INTO patient_order_status VALUES ('AWAITING_SCREENING', 'Awaiting Screening');
INSERT INTO patient_order_status VALUES ('SCREENING_IN_PROGRESS', 'Screening In Progress');
INSERT INTO patient_order_status VALUES ('SCHEDULED_WITH_PROVIDER', 'Scheduled With Provider');
INSERT INTO patient_order_status VALUES ('NEEDS_FURTHER_ASSESSMENT', 'Needs Further Assessment');
INSERT INTO patient_order_status VALUES ('GRADUATED', 'Graduated', TRUE);
INSERT INTO patient_order_status VALUES ('CONNECTED_TO_CARE', 'Connected To Care', TRUE);
INSERT INTO patient_order_status VALUES ('LOST_CONTACT', 'Lost Contact', TRUE);

-- The actual order, can be modified over time.
-- We keep track of changes by writing to the patient_order_tracking table
CREATE TABLE patient_order (
  patient_order_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  patient_order_status_id VARCHAR NOT NULL REFERENCES patient_order_status DEFAULT 'NEW',
  order_import_id UUID NOT NULL REFERENCES order_import,
  patient_account_id UUID REFERENCES account,
  encounter_department_id VARCHAR,
  encounter_department_id_type VARCHAR, -- not currently provided
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
  patient_id VARCHAR NOT NULL,
  patient_id_type VARCHAR NOT NULL,
  patient_birth_sex_id VARCHAR,
  patient_birthdate DATE,
  patient_address_line_1 VARCHAR,
  patient_address_line_2 VARCHAR,
  patient_city VARCHAR,
  patient_postal_code VARCHAR,
  patient_region VARCHAR, -- In the US, this is the state
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
  cc_recipients VARCHAR,
  last_active_medication_order_summary VARCHAR,
  medications VARCHAR,
  recent_psychotherapeutic_medications VARCHAR,
  created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON patient_order FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE patient_order_disposition (
  patient_order_disposition_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  patient_order_id UUID NOT NULL REFERENCES patient_order,
  patient_order_care_type_id TEXT NOT NULL REFERENCES patient_order_care_type,
  patient_order_care_focus_id TEXT NOT NULL REFERENCES patient_order_care_focus,
  created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON patient_order_disposition FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE patient_order_tracking_type (
  patient_order_tracking_id VARCHAR PRIMARY KEY,
  description VARCHAR NOT NULL
);

-- Data to include: old patient order status ID, new patient order status ID
INSERT INTO patient_order_tracking_type VALUES ('STATUS_CHANGED', 'Status Changed');
-- Data to include: screening session ID
INSERT INTO patient_order_tracking_type VALUES ('SELF_ADMINISTERED_SCREENING_SESSION_STARTED', 'Self-Administered Screening Session Started');
-- Data to include: screening session ID, triage ID
INSERT INTO patient_order_tracking_type VALUES ('SELF_ADMINISTERED_SCREENING_SESSION_COMPLETED', 'Self-Administered Screening Session Completed');
-- Data to include: screening session ID
INSERT INTO patient_order_tracking_type VALUES ('MHIC_ADMINISTERED_SCREENING_SESSION_STARTED', 'MHIC-Administered Screening Session Started');
-- Data to include: screening session ID, triage ID
INSERT INTO patient_order_tracking_type VALUES ('MHIC_ADMINISTERED_SCREENING_SESSION_COMPLETED', 'MHIC-Administered Screening Session Completed');

-- TODO: add other types, e.g. name/info changed

-- Keep an internal immutable history of any changes made to the patient order record over time
CREATE TABLE patient_order_tracking (
  patient_order_tracking_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  patient_order_tracking_type_id VARCHAR NOT NULL REFERENCES patient_order_tracking_type,
  patient_order_id UUID NOT NULL REFERENCES patient_order,
  account_id UUID REFERENCES account,
  metadata JSONB NOT NULL DEFAULT '{}'::JSONB, -- Bag for holding whatever data might be needed for this footprint
  created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON patient_order_tracking FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

COMMIT;