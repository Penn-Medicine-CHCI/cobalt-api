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

INSERT INTO patient_order_outreach_result_status VALUES ('UNKNOWN', 'Unknown');
INSERT INTO patient_order_outreach_result_status VALUES ('CONNECTED', 'Connected');
INSERT INTO patient_order_outreach_result_status VALUES ('NOT_CONNECTED', 'Not Connected');

CREATE TABLE patient_order_outreach_result_type (
  patient_order_outreach_result_type_id VARCHAR PRIMARY KEY,
  patient_order_outreach_result_status_id VARCHAR REFERENCES patient_order_outreach_result_status,
  description VARCHAR NOT NULL
);

INSERT INTO patient_order_outreach_result_type VALUES ('NO_ANSWER', 'NOT_CONNECTED', 'No Answer');
INSERT INTO patient_order_outreach_result_type VALUES ('BUSY', 'NOT_CONNECTED', 'Busy');
INSERT INTO patient_order_outreach_result_type VALUES ('LEFT_VOICEMAIL', 'NOT_CONNECTED', 'Left Voicemail');
INSERT INTO patient_order_outreach_result_type VALUES ('LEFT_MESSAGE', 'NOT_CONNECTED', 'Left Message');
INSERT INTO patient_order_outreach_result_type VALUES ('DISCONNECTED', 'NOT_CONNECTED', 'Disconnected');
INSERT INTO patient_order_outreach_result_type VALUES ('WRONG_NUMBER', 'NOT_CONNECTED', 'Wrong Number');
INSERT INTO patient_order_outreach_result_type VALUES ('DISCUSSED_APPOINTMENT', 'CONNECTED', 'Discussed Appointment');
INSERT INTO patient_order_outreach_result_type VALUES ('DISCUSSED_DIGITAL_SCREENING_REMINDER', 'CONNECTED', 'Discussed Digital Screening Reminder');
INSERT INTO patient_order_outreach_result_type VALUES ('DISCUSSED_RESOURCES', 'CONNECTED', 'Discussed Resources');
INSERT INTO patient_order_outreach_result_type VALUES ('DISCUSSED_OTHER', 'CONNECTED', 'Discussed Other');
INSERT INTO patient_order_outreach_result_type VALUES ('SENT_RESOURCES', 'CONNECTED', 'Sent Resources');
INSERT INTO patient_order_outreach_result_type VALUES ('SENT_OTHER', 'CONNECTED', 'Sent Other');

-- We can drive UI by grouping different types/statuses of outreach for user to select from
CREATE TABLE patient_order_outreach_result (
  patient_order_outreach_result_id UUID PRIMARY KEY NOT NULL DEFAULT uuid_generate_v4(),
  patient_order_outreach_type_id VARCHAR REFERENCES patient_order_outreach_type,
  patient_order_outreach_result_type_id VARCHAR REFERENCES patient_order_outreach_result_type,
  display_order INTEGER NOT NULL
);

-- Phone Call
INSERT INTO patient_order_outreach_result(patient_order_outreach_type_id, patient_order_outreach_result_type_id, display_order)
VALUES ('PHONE_CALL', 'NO_ANSWER', 1);
INSERT INTO patient_order_outreach_result(patient_order_outreach_type_id, patient_order_outreach_result_type_id, display_order)
VALUES ('PHONE_CALL', 'BUSY', 2);
INSERT INTO patient_order_outreach_result(patient_order_outreach_type_id, patient_order_outreach_result_type_id, display_order)
VALUES ('PHONE_CALL', 'LEFT_VOICEMAIL', 3);
INSERT INTO patient_order_outreach_result(patient_order_outreach_type_id, patient_order_outreach_result_type_id, display_order)
VALUES ('PHONE_CALL', 'LEFT_MESSAGE', 4);
INSERT INTO patient_order_outreach_result(patient_order_outreach_type_id, patient_order_outreach_result_type_id, display_order)
VALUES ('PHONE_CALL', 'DISCONNECTED', 5);
INSERT INTO patient_order_outreach_result(patient_order_outreach_type_id, patient_order_outreach_result_type_id, display_order)
VALUES ('PHONE_CALL', 'WRONG_NUMBER', 6);
INSERT INTO patient_order_outreach_result(patient_order_outreach_type_id, patient_order_outreach_result_type_id, display_order)
VALUES ('PHONE_CALL', 'DISCUSSED_APPOINTMENT', 7);
INSERT INTO patient_order_outreach_result(patient_order_outreach_type_id, patient_order_outreach_result_type_id, display_order)
VALUES ('PHONE_CALL', 'DISCUSSED_DIGITAL_SCREENING_REMINDER', 8);
INSERT INTO patient_order_outreach_result(patient_order_outreach_type_id, patient_order_outreach_result_type_id, display_order)
VALUES ('PHONE_CALL', 'DISCUSSED_RESOURCES', 9);
INSERT INTO patient_order_outreach_result(patient_order_outreach_type_id, patient_order_outreach_result_type_id, display_order)
VALUES ('PHONE_CALL', 'DISCUSSED_OTHER', 10);

-- MyChart
INSERT INTO patient_order_outreach_result(patient_order_outreach_type_id, patient_order_outreach_result_type_id, display_order)
VALUES ('MYCHART_MESSAGE', 'SENT_RESOURCES', 11);
INSERT INTO patient_order_outreach_result(patient_order_outreach_type_id, patient_order_outreach_result_type_id, display_order)
VALUES ('MYCHART_MESSAGE', 'SENT_OTHER', 12);

ALTER TABLE patient_order_outreach ADD COLUMN patient_order_outreach_result_id UUID REFERENCES patient_order_outreach_result;

-- This is nonproduction data currently; assign an arbitrary result
UPDATE patient_order_outreach
SET patient_order_outreach_result_id=porr.patient_order_outreach_result_id
FROM patient_order_outreach_result porr
WHERE porr.patient_order_outreach_type_id='PHONE_CALL'
AND porr.patient_order_outreach_result_type_id='NO_ANSWER';

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
  posm.patient_order_scheduled_message_type_id,
  posmt.description AS patient_order_scheduled_message_type_description,
  posm.created,
  posm.last_updated,
  sm.scheduled_message_status_id,
  sm.scheduled_by_account_id,
  sm.scheduled_message_source_id,
  sm.message_type_id,
  mt.description AS message_type_description,
  sm.scheduled_at,
  sm.time_zone,
  sm.processed_at,
  sm.canceled_at,
  sm.errored_at
FROM
  patient_order_scheduled_message posm,
  patient_order_scheduled_message_type posmt,
  scheduled_message sm,
  message_type mt
WHERE
  posm.scheduled_message_id=sm.scheduled_message_id
  AND posm.patient_order_scheduled_message_type_id=posmt.patient_order_scheduled_message_type_id
  AND sm.message_type_id=mt.message_type_id;

-- TODO: finish up

-- v_patient_order needs:
-- patient_order_screening_session_status_id (NEEDS_ASSESSMENT, SCHEDULED, IN_PROGRESS, COMPLETE)
-- patient_order_closure_reason_description (plain English, e.g. "Refused Care")
--

COMMIT;