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

-- Need a way to keep track of whether or not results of screening scoring function indicate we are "below threshold" (i.e. clinically insignificant).
-- Given the flexibility of screenings, it's not straightforward to encode static scoring rules.
-- For example, "GAD-7 has a minimum threshold of 5" would be simple to encode (add a min_threshold field on screening_type or similar)
-- but "C-SSRS doesn't have a score threshold, it's defined as above threshold if either question 2 or 3 is answered 'Yes'" is
-- not so easy to encode in a generic way.
--
-- So, we have our screening scoring function JS return a "below threshold" flag and persist that to this table, giving
-- us as much flexibility as we need.
--
-- For now, leaving this nullable until we decide how (or if) we need to worry about migrating over existing data.
-- Currently this is only needed for display in Integrated Care
ALTER TABLE screening_session_screening ADD COLUMN below_scoring_threshold BOOLEAN;

-- Recreate our view due to new field
DROP VIEW v_screening_session_screening;

CREATE VIEW v_screening_session_screening AS
 SELECT screening_session_screening.screening_session_screening_id,
    screening_session_screening.screening_session_id,
    screening_session_screening.screening_version_id,
    screening_session_screening.screening_order,
    screening_session_screening.valid,
    screening_session_screening.completed,
    screening_session_screening.legacy_score,
    screening_session_screening.created,
    screening_session_screening.last_updated,
    screening_session_screening.score,
    screening_session_screening.below_scoring_threshold
   FROM screening_session_screening
  WHERE screening_session_screening.valid = true;

-- This is the absolute maximum overall score that can be achieved on a screening (i.e. all "worst" answers selected).
-- In theory this could instead be calculated by examining screening question/screening answer options and abstracted away using a view,
-- but the view becomes complicated quickly because it's not as simple as "sum the worst answers" - we have cases like
-- "this question supports 1-3 answers out of 5 possible" so you need a few layers of window functions to calculate using pure SQL.
-- For our current purposes, it's sufficient to just show the maximum value for some of the canned "formal" screenings, and that's
-- good enough...the purity of dynamic calculation via SQL is not worth the complexity atm.
ALTER TABLE screening_type ADD COLUMN overall_score_maximum INTEGER;

-- Minimum threshold (inclusive) for a score to be clinically significant.
-- Example: GAD-7 threshold is 5, therefore a score from 0-4 is not clinically significant.
ALTER TABLE screening_type ADD COLUMN overall_score_minimum_threshold INTEGER;

UPDATE screening_type SET overall_score_maximum = 21 WHERE screening_type_id='GAD_7';
UPDATE screening_type SET overall_score_maximum = 24 WHERE screening_type_id='PHQ_8';
UPDATE screening_type SET overall_score_maximum = 27 WHERE screening_type_id='PHQ_9';
UPDATE screening_type SET overall_score_maximum = 25 WHERE screening_type_id='WHO_5';
UPDATE screening_type SET overall_score_maximum = 5 WHERE screening_type_id='PC_PTSD_5';
UPDATE screening_type SET overall_score_maximum = 20 WHERE screening_type_id='ASRM';
UPDATE screening_type SET overall_score_maximum = 3 WHERE screening_type_id='C_SSRS';
UPDATE screening_type SET overall_score_maximum = 10 WHERE screening_type_id='DAST_10';
UPDATE screening_type SET overall_score_maximum = 12 WHERE screening_type_id='AUDIT_C';
UPDATE screening_type SET overall_score_maximum = 54 WHERE screening_type_id='MBI_9';
UPDATE screening_type SET overall_score_maximum = 1 WHERE screening_type_id='IC_INTRO';
UPDATE screening_type SET overall_score_maximum = 10 WHERE screening_type_id='IC_INTRO_CONDITIONS';
UPDATE screening_type SET overall_score_maximum = 6 WHERE screening_type_id='IC_INTRO_SYMPTOMS';
UPDATE screening_type SET overall_score_maximum = 3 WHERE screening_type_id='IC_DRUG_USE_FREQUENCY';
UPDATE screening_type SET overall_score_maximum = 1 WHERE screening_type_id='IC_DRUG_USE_OPIOID';
UPDATE screening_type SET overall_score_maximum = 1 WHERE screening_type_id='BPI_1';
UPDATE screening_type SET overall_score_maximum = 30 WHERE screening_type_id='PRIME_5';

-- Get rid of unused duplicate type
DELETE FROM screening_type WHERE screening_type_id='AUDIT_C_ALCOHOL';

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


CREATE TABLE patient_order_scheduled_screening (
  patient_order_scheduled_screening_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  patient_order_id UUID NOT NULL REFERENCES patient_order,
  account_id UUID NOT NULL REFERENCES account,
  scheduled_date_time TIMESTAMP NOT NULL,
  calendar_url TEXT,
  canceled BOOLEAN NOT NULL DEFAULT FALSE,
  canceled_at TIMESTAMPTZ,
  created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON patient_order_scheduled_screening FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

-- TODO: finish up

-- v_patient_order needs patient_order_panel_type_id (a synthetic ID with below values, recalculated and stored after patient order state changes
--   as opposed to being calculated on-the-fly by a view or similar)
-- 	  NEW,
--   	NEEDS_ASSESSMENT,
--   	SCHEDULED -- Unscreened. Has a call scheduled with an MHIC to take the screening
--   	SAFETY_PLANNING -- Screened. Most severe triage is SAFETY_PLANNING
--   	SPECIALTY_CARE -- Screened. Most severe triage is SPECIALTY_CARE
--   	BHP -- Screened. Associated patient has a non-canceled appointment scheduled with a provider that has support role BHP
--   	CLOSED -- PatientOrderStatusId=CLOSED
--    ARCHIVED -- PatientOrderStatusId=ARCHIVED
--
-- patient_order_screening_session_status_id (NEEDS_ASSESSMENT, SCHEDULED, IN_PROGRESS, COMPLETE)
-- patient_order_closure_reason_description (plain English, e.g. "Refused Care")
-- patient_under_18 (boolean, use timezone from institution related to order to determine this)
-- patient_has_recent_episode (boolean, has there been another episode for the same patient MRN/institution closed <= 30 days ago?)

COMMIT;