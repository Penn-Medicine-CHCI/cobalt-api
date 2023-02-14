BEGIN;
SELECT _v.register_patch('057-patient-order', NULL, NULL);

INSERT INTO support_role (support_role_id, description, display_order) VALUES ('PSYCHOLOGIST', 'Psychologist', 8);
INSERT INTO support_role (support_role_id, description, display_order) VALUES ('BHP', 'Behavioral Health Professional', 9);
INSERT INTO support_role (support_role_id, description, display_order) VALUES ('LCSW', 'Licensed Clinical Social Worker', 10);

ALTER TABLE account ADD COLUMN epic_patient_mrn TEXT;

INSERT INTO login_destination (login_destination_id, description) VALUES ('IC_PATIENT', 'IC Patient');

-- We might import orders via an EHR like Epic or via manual CSV upload
CREATE TABLE patient_order_import_type (
  patient_order_import_type_id VARCHAR PRIMARY KEY,
  description VARCHAR NOT NULL
);

INSERT INTO patient_order_import_type VALUES ('CSV', 'CSV');
INSERT INTO patient_order_import_type VALUES ('EPIC', 'Epic');

-- Keep track of who imports, and how they import
CREATE TABLE patient_order_import (
  patient_order_import_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  patient_order_import_type_id VARCHAR NOT NULL REFERENCES patient_order_import_type,
  institution_id VARCHAR NOT NULL REFERENCES institution,
  account_id UUID REFERENCES account, -- might be NULL if imported by an EHR background sync task, for example
  raw_order TEXT NOT NULL, -- permanent record of the CSV or EHR import data
  created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON patient_order_import FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE patient_order_status (
  patient_order_status_id VARCHAR PRIMARY KEY,
  description VARCHAR NOT NULL,
  terminal BOOLEAN NOT NULL DEFAULT FALSE
);

INSERT INTO patient_order_status VALUES ('NEW', 'New');
INSERT INTO patient_order_status VALUES ('AWAITING_SCREENING', 'Awaiting Screening');
INSERT INTO patient_order_status VALUES ('SCREENING_IN_PROGRESS', 'Screening In Progress');
INSERT INTO patient_order_status VALUES ('AWAITING_MHIC_SCHEDULING', 'Awaiting MHIC Scheduling');
INSERT INTO patient_order_status VALUES ('AWAITING_PROVIDER_SCHEDULING', 'Awaiting Provider Scheduling');
INSERT INTO patient_order_status VALUES ('AWAITING_SAFETY_PLANNING', 'Awaiting Safety Planning');
INSERT INTO patient_order_status VALUES ('SCHEDULED_WITH_MHIC', 'Scheduled With MHIC');
INSERT INTO patient_order_status VALUES ('SCHEDULED_WITH_PROVIDER', 'Scheduled With Provider');
INSERT INTO patient_order_status VALUES ('NEEDS_FURTHER_ASSESSMENT', 'Needs Further Assessment');
INSERT INTO patient_order_status VALUES ('CONNECTED_TO_CARE', 'Connected To Care', TRUE);
INSERT INTO patient_order_status VALUES ('LOST_CONTACT', 'Lost Contact', TRUE);
INSERT INTO patient_order_status VALUES ('CLOSED', 'Closed', TRUE);

-- The actual order, can be modified over time.
-- We keep track of changes by writing to the patient_order_tracking table
CREATE TABLE patient_order (
  patient_order_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  patient_order_status_id VARCHAR NOT NULL REFERENCES patient_order_status DEFAULT 'NEW',
  patient_order_import_id UUID NOT NULL REFERENCES patient_order_import,
  institution_id VARCHAR NOT NULL REFERENCES institution,
  patient_account_id UUID REFERENCES account,
  panel_account_id UUID REFERENCES account, -- which account's panel is holding this order, if any
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
  patient_birth_sex_id VARCHAR REFERENCES birth_sex(birth_sex_id),
  patient_birthdate DATE,
  patient_address_id UUID REFERENCES address,
  primary_payor VARCHAR,
  primary_plan VARCHAR,
  order_date DATE,
  order_age_in_minutes INTEGER,
  order_id VARCHAR NOT NULL,
  routing VARCHAR,
  reason_for_referral VARCHAR,
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

-- Keep track of screening sessions for this order.
-- Keyed off of the flow specfied on institution.integrated_care_screening_flow_id
CREATE TABLE patient_order_screening_session (
  patient_order_id UUID NOT NULL REFERENCES patient_order,
  screening_session_id UUID NOT NULL REFERENCES screening_session,
  created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (patient_order_id, screening_session_id)
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON patient_order_screening_session FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

-- EHR diagnoses that come in as part of the order import.  Can be multiple per order
CREATE TABLE patient_order_diagnosis (
  patient_order_diagnosis_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  patient_order_id UUID NOT NULL REFERENCES patient_order,
  diagnosis_id VARCHAR NOT NULL,
  diagnosis_id_type VARCHAR,
  diagnosis_name VARCHAR NOT NULL,
  display_order INTEGER NOT NULL,
  created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (patient_order_id, display_order)
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON patient_order_diagnosis FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

-- Recommended care type for order
CREATE TABLE patient_order_care_type (
  patient_order_care_type_id VARCHAR PRIMARY KEY,
  description VARCHAR NOT NULL
);

INSERT INTO patient_order_care_type VALUES ('UNSPECIFIED', 'Unspecified');
INSERT INTO patient_order_care_type VALUES ('SUBCLINICAL', 'Subclinical');
INSERT INTO patient_order_care_type VALUES ('SPECIALTY', 'Specialty');
INSERT INTO patient_order_care_type VALUES ('COLLABORATIVE', 'Collaborative');

CREATE TABLE patient_order_focus_type (
  patient_order_focus_type_id VARCHAR PRIMARY KEY,
  description VARCHAR NOT NULL
);

INSERT INTO patient_order_focus_type VALUES ('UNSPECIFIED', 'Unspecified');
INSERT INTO patient_order_focus_type VALUES ('SELF_DIRECTED', 'Self-Directed');
INSERT INTO patient_order_focus_type VALUES ('GENERAL', 'General');
INSERT INTO patient_order_focus_type VALUES ('GRIEF', 'Grief');
INSERT INTO patient_order_focus_type VALUES ('INSOMNIA', 'Insomnia');
INSERT INTO patient_order_focus_type VALUES ('PSYCHOTHERAPY', 'Psychotherapy');
INSERT INTO patient_order_focus_type VALUES ('ALCOHOL_USE_DISORDER', 'Alcohol Use Disorder');
INSERT INTO patient_order_focus_type VALUES ('SUBSTANCE_USE_DISORDER', 'Substance Use Disorder');
INSERT INTO patient_order_focus_type VALUES ('EVALUATION', 'Evaluation');
INSERT INTO patient_order_focus_type VALUES ('ADHD', 'ADHD');
INSERT INTO patient_order_focus_type VALUES ('TRAUMA', 'Trauma');
INSERT INTO patient_order_focus_type VALUES ('LCSW_CAPACITY', 'LCSW Capacity');
INSERT INTO patient_order_focus_type VALUES ('CRISIS_CARE', 'Crisis Care');

-- How was a triage assigned to the order?  e.g. via Cobalt screening result scoring or manually set by MHIC
CREATE TABLE patient_order_triage_source (
  patient_order_triage_source_id VARCHAR PRIMARY KEY,
  description VARCHAR NOT NULL
);

INSERT INTO patient_order_triage_source VALUES ('COBALT', 'Cobalt');
INSERT INTO patient_order_triage_source VALUES ('MANUALLY_SET', 'Manually Set');

-- Orders can have 0..n triages
CREATE TABLE patient_order_triage (
  patient_order_triage_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  patient_order_id UUID NOT NULL REFERENCES patient_order,
  patient_order_triage_source_id TEXT NOT NULL REFERENCES patient_order_triage_source,
  patient_order_care_type_id TEXT NOT NULL REFERENCES patient_order_care_type,
  patient_order_focus_type_id TEXT NOT NULL REFERENCES patient_order_focus_type,
  reason TEXT,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON patient_order_triage FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE patient_order_note (
  patient_order_note_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  patient_order_id UUID NOT NULL REFERENCES patient_order,
  account_id UUID NOT NULL REFERENCES account,
  note TEXT NOT NULL,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON patient_order_note FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE patient_order_event_type (
  patient_order_event_id VARCHAR PRIMARY KEY,
  description VARCHAR NOT NULL
);

-- Data to include: patient order import ID
INSERT INTO patient_order_event_type VALUES ('IMPORTED', 'Imported');
-- Data to include: old panel account ID, new panel account ID
INSERT INTO patient_order_event_type VALUES ('PANEL_CHANGED', 'Panel Changed');
-- Data to include: old patient order status ID, new patient order status ID
INSERT INTO patient_order_event_type VALUES ('STATUS_CHANGED', 'Status Changed');
-- Data to include: screening session ID
INSERT INTO patient_order_event_type VALUES ('SELF_ADMINISTERED_SCREENING_SESSION_STARTED', 'Self-Administered Screening Session Started');
-- Data to include: screening session ID, triage IDs
INSERT INTO patient_order_event_type VALUES ('SELF_ADMINISTERED_SCREENING_SESSION_COMPLETED', 'Self-Administered Screening Session Completed');
-- Data to include: screening session ID
INSERT INTO patient_order_event_type VALUES ('MHIC_ADMINISTERED_SCREENING_SESSION_STARTED', 'MHIC-Administered Screening Session Started');
-- Data to include: screening session ID, triage IDs
INSERT INTO patient_order_event_type VALUES ('MHIC_ADMINISTERED_SCREENING_SESSION_COMPLETED', 'MHIC-Administered Screening Session Completed');
-- Data to include: patient order note ID, account ID, note
INSERT INTO patient_order_event_type VALUES ('NOTE_CREATED', 'Note Created');
-- Data to include: patient order note ID, account ID, old note, new note
INSERT INTO patient_order_event_type VALUES ('NOTE_UPDATED', 'Note Updated');
-- Data to include: patient order note ID, account ID, note
INSERT INTO patient_order_event_type VALUES ('NOTE_DELETED', 'Note Deleted');

-- TODO: add other types, e.g. name/info changed

-- Keep an internal immutable history of any changes made to the patient order record over time
CREATE TABLE patient_order_event (
  patient_order_event_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  patient_order_event_type_id VARCHAR NOT NULL REFERENCES patient_order_event_type,
  patient_order_id UUID NOT NULL REFERENCES patient_order,
  account_id UUID REFERENCES account,
  message TEXT NOT NULL,
  metadata JSONB NOT NULL DEFAULT '{}'::JSONB, -- Bag for holding whatever data might be needed for this footprint
  created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON patient_order_event FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

COMMIT;