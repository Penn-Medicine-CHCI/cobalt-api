BEGIN;
SELECT _v.register_patch('067-patient-order-additions', NULL, NULL);

CREATE TABLE patient_order_outreach_type (
  patient_order_outreach_type_id VARCHAR PRIMARY KEY,
  description VARCHAR NOT NULL,
  display_order INTEGER NOT NULL
);

INSERT INTO patient_order_outreach_type VALUES ('PHONE_CALL', 'Phone Call', 1);
INSERT INTO patient_order_outreach_type VALUES ('MYCHART_MESSAGE', 'MyChart Message', 2);

CREATE TABLE patient_order_outreach_result_status (
  patient_order_outreach_result_status_id VARCHAR PRIMARY KEY,
  description VARCHAR NOT NULL
);

INSERT INTO patient_order_outreach_result_status VALUES ('CONNECTED', 'Connected');
INSERT INTO patient_order_outreach_result_status VALUES ('NOT_CONNECTED', 'Not Connected');
INSERT INTO patient_order_outreach_result_status VALUES ('UNKNOWN', 'Unknown');

CREATE TABLE patient_order_outreach_result (
  patient_order_outreach_result_id VARCHAR PRIMARY KEY,
  patient_order_outreach_type_id VARCHAR REFERENCES patient_order_outreach_type,
  patient_order_outreach_result_status_id VARCHAR REFERENCES patient_order_outreach_result_status,
  description VARCHAR NOT NULL,
  display_order INTEGER NOT NULL
);

-- Phone Call
INSERT INTO patient_order_outreach_result(patient_order_outreach_result_id, patient_order_outreach_type_id, patient_order_outreach_result_status_id, description, display_order)
VALUES ('NO_ANSWER', 'PHONE_CALL', 'NOT_CONNECTED', 'No Answer', 1);
INSERT INTO patient_order_outreach_result(patient_order_outreach_result_id, patient_order_outreach_type_id, patient_order_outreach_result_status_id, description, display_order)
VALUES ('BUSY', 'PHONE_CALL', 'NOT_CONNECTED', 'Busy', 2);
INSERT INTO patient_order_outreach_result(patient_order_outreach_result_id, patient_order_outreach_type_id, patient_order_outreach_result_status_id, description, display_order)
VALUES ('LEFT_VOICEMAIL', 'PHONE_CALL', 'NOT_CONNECTED', 'Left Voicemail', 3);
INSERT INTO patient_order_outreach_result(patient_order_outreach_result_id, patient_order_outreach_type_id, patient_order_outreach_result_status_id, description, display_order)
VALUES ('LEFT_MESSAGE', 'PHONE_CALL', 'NOT_CONNECTED', 'Left Message', 4);
INSERT INTO patient_order_outreach_result(patient_order_outreach_result_id, patient_order_outreach_type_id, patient_order_outreach_result_status_id, description, display_order)
VALUES ('DISCONNECTED', 'PHONE_CALL', 'NOT_CONNECTED', 'Disconnected', 5);
INSERT INTO patient_order_outreach_result(patient_order_outreach_result_id, patient_order_outreach_type_id, patient_order_outreach_result_status_id, description, display_order)
VALUES ('WRONG_NUMBER', 'PHONE_CALL', 'NOT_CONNECTED', 'Wrong Number', 6);
INSERT INTO patient_order_outreach_result(patient_order_outreach_result_id, patient_order_outreach_type_id, patient_order_outreach_result_status_id, description, display_order)
VALUES ('DISCUSSED_APPOINTMENT', 'PHONE_CALL', 'CONNECTED', 'Discussed Appointment Time', 7);
INSERT INTO patient_order_outreach_result(patient_order_outreach_result_id, patient_order_outreach_type_id, patient_order_outreach_result_status_id, description, display_order)
VALUES ('DISCUSSED_DIGITAL_SCREENING', 'PHONE_CALL', 'CONNECTED', 'Discussed Digital Screening Reminder', 8);
INSERT INTO patient_order_outreach_result(patient_order_outreach_result_id, patient_order_outreach_type_id, patient_order_outreach_result_status_id, description, display_order)
VALUES ('DISCUSSED_RESOURCES', 'PHONE_CALL', 'CONNECTED', 'Discussed Resources', 9);
INSERT INTO patient_order_outreach_result(patient_order_outreach_result_id, patient_order_outreach_type_id, patient_order_outreach_result_status_id, description, display_order)
VALUES ('DISCUSSED_OTHER', 'PHONE_CALL', 'CONNECTED', 'Discussed Other', 10);

-- MyChart
INSERT INTO patient_order_outreach_result(patient_order_outreach_result_id, patient_order_outreach_type_id, patient_order_outreach_result_status_id, description, display_order)
VALUES ('SENT_RESOURCES', 'MYCHART_MESSAGE', 'CONNECTED', 'Sent Resources', 11);
INSERT INTO patient_order_outreach_result(patient_order_outreach_result_id, patient_order_outreach_type_id, patient_order_outreach_result_status_id, description, display_order)
VALUES ('SENT_OTHER', 'MYCHART_MESSAGE', 'CONNECTED', 'Sent Other', 12);

ALTER TABLE patient_order_outreach ADD COLUMN patient_order_outreach_type_id VARCHAR REFERENCES patient_order_outreach_type;
ALTER TABLE patient_order_outreach ADD COLUMN patient_order_outreach_result_id VARCHAR REFERENCES patient_order_outreach_result;

-- This is nonproduction data currently; assign an arbitrary value
UPDATE patient_order_outreach SET patient_order_outreach_type_id = 'PHONE_CALL';
UPDATE patient_order_outreach SET patient_order_outreach_result_id = 'NO_ANSWER';

-- Doesn't really make sense to have a default value
ALTER TABLE patient_order_outreach ALTER COLUMN patient_order_outreach_type_id SET NOT NULL;
ALTER TABLE patient_order_outreach ALTER COLUMN patient_order_outreach_result_id SET NOT NULL;

-- Different institutions can have different "branded" names for Epic MyChart
ALTER TABLE institution ADD COLUMN mychart_name VARCHAR NOT NULL DEFAULT 'MyChart';

-- How many days to wait between sending resources and sending a followup message (null indicates never send followup)
ALTER TABLE institution ADD COLUMN integrated_care_sent_resources_followup_day_offset INTEGER;

CREATE TABLE patient_order_scheduled_message_type (
  patient_order_scheduled_message_type_id VARCHAR PRIMARY KEY,
  template_name VARCHAR NOT NULL,
  description VARCHAR NOT NULL,
  display_order INTEGER NOT NULL
);

CREATE UNIQUE INDEX patient_order_scheduled_message_type_template_name_idx ON patient_order_scheduled_message_type USING btree (template_name);

INSERT INTO patient_order_scheduled_message_type VALUES ('WELCOME', 'IC_WELCOME', 'Welcome', 1);
INSERT INTO patient_order_scheduled_message_type VALUES ('RESOURCE_CHECK_IN', 'IC_RESOURCE_CHECK_IN', 'Resource Check-In', 2);

-- Keep track of scheduled messages for this order
ALTER TABLE patient_order_scheduled_message ADD COLUMN patient_order_scheduled_message_type_id VARCHAR NOT NULL REFERENCES patient_order_scheduled_message_type;

-- Doesn't make sense to associate the same scheduled message to multiple patient order scheduled messages
CREATE UNIQUE INDEX patient_order_scheduled_message_scheduled_message_id_idx ON patient_order_scheduled_message USING btree (scheduled_message_id);

-- Pull scheduled message fields into the patient order scheduled message so we have a view over everything needed to drive IC UI
CREATE VIEW v_patient_order_scheduled_message AS
SELECT
  posm.patient_order_scheduled_message_id,
  posm.patient_order_id,
  posm.scheduled_message_id,
  posm.created,
  posm.last_updated,
  sm.scheduled_message_status_id,
  sm.scheduled_by_account_id,
  sm.scheduled_message_source_id,
  sm.message_type_id,
  sm.scheduled_at,
  sm.time_zone,
  sm.processed_at,
  sm.canceled_at,
  sm.errored_at
FROM
  patient_order_scheduled_message posm,
  scheduled_message sm
WHERE
  posm.scheduled_message_id=sm.scheduled_message_id;

-- TODO: finish up

-- v_patient_order needs:
-- patient_order_screening_session_status_id (NEEDS_ASSESSMENT, SCHEDULED, IN_PROGRESS, COMPLETE)
-- patient_order_closure_reason_description (plain English, e.g. "Refused Care")
--

COMMIT;