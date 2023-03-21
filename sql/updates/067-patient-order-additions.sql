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

-- *** FOR TUESDAY Instead of the below, what we actually want is two separate things.
--
-- 1. patient_order_status should be all the open statuses
-- 2. patient_order_disposition should be OPEN, CLOSED, ARCHIVED
-- 3. patient_order_panel_type_id should be union of values from patient_order_status and patient_order_disposition (except OPEN?  idk...)
--
-- The reason for this is reporting - we would be able to answer questions like "how many SPECIALTY_CARE orders closed in the last month?"
-- And we'd back able to "flip back" to OPEN without needing to re-figure-out what the previously-OPEN state was to reconstitute it

-- TODO: update the below per the above.

ALTER TABLE patient_order_status ADD COLUMN display_order INTEGER;

-- Add correct statuses

-- Default for new imports; unassigned
INSERT INTO patient_order_status VALUES ('PENDING', 'Pending', 1);
-- Assigned, but none of the below apply.  Also note, we are in this state if unscheduled but "screening in progress"
INSERT INTO patient_order_status VALUES ('NEEDS_ASSESSMENT', 'Needs Assessment', 2);
-- Unscreened, but has a call scheduled with an MHIC to take the screening (uncanceled patient_order_scheduled_screening)
INSERT INTO patient_order_status VALUES ('SCHEDULED', 'Scheduled', 3);
-- Screening completed, most severe level of care type triage is SAFETY_PLANNING
INSERT INTO patient_order_status VALUES ('SAFETY_PLANNING', 'Safety Planning', 4);
-- Screening completed, most severe level of care type triage is SPECIALTY
INSERT INTO patient_order_status VALUES ('SPECIALTY_CARE', 'Specialty Care', 5);
-- Screening completed, most severe level of care type triage is SUBCLINICAL
INSERT INTO patient_order_status VALUES ('SUBCLINICAL', 'Subclinical', 6);
-- Screening completed, most severe level of care type triage is COLLABORATIVE.  Patient or MHIC can schedule with a provider
INSERT INTO patient_order_status VALUES ('BHP', 'BHP', 7);

-- Clean up now that we have correct data in patient_order_status.
-- It's OK to set all existing orders to PENDING instead of correctly migrating because we're not live yet.
UPDATE patient_order SET patient_order_status_id='PENDING';
ALTER TABLE patient_order ALTER COLUMN patient_order_status_id SET DEFAULT 'PENDING';

-- Get rid of unused statuses
DELETE FROM patient_order_status WHERE patient_order_status_id IN ('OPEN', 'CLOSED', 'ARCHIVED', 'DELETED');

ALTER TABLE patient_order_status ALTER COLUMN display_order SET NOT NULL;

-- Disposition says if we're currently open, closed, or archived.
CREATE TABLE patient_order_disposition (
  patient_order_disposition_id VARCHAR PRIMARY KEY,
  description VARCHAR NOT NULL,
  display_order INTEGER NOT NULL
);

INSERT INTO patient_order_disposition VALUES ('OPEN', 'Open', 1);
INSERT INTO patient_order_disposition VALUES ('CLOSED', 'Closed', 2);
INSERT INTO patient_order_disposition VALUES ('ARCHIVED', 'Archived', 3);

ALTER TABLE patient_order ADD COLUMN patient_order_disposition_id VARCHAR NOT NULL REFERENCES patient_order_disposition DEFAULT 'OPEN';

ALTER TABLE patient_order ADD COLUMN crisis_indicated BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE patient_order ADD COLUMN crisis_indicated_at TIMESTAMPTZ;

ALTER TABLE patient_order RENAME COLUMN episode_ended_at TO episode_closed_at;

DROP VIEW v_patient_order;

create or REPLACE VIEW v_patient_order AS
WITH
po_query AS (
  select *
  from patient_order
  where patient_order_status_id != 'ARCHIVED'
),
poo_query AS (
  -- Count up the patient outreach attempts for each patient order
  select poq.patient_order_id, count(poo.*) as outreach_count
  from patient_order_outreach poo, po_query poq
  where poq.patient_order_id=poo.patient_order_id
  group by poq.patient_order_id
),
poomax_query AS (
  -- Pick the most recent patient outreach attempt for each patient order
  select poo.*
  from po_query poq
  join patient_order_outreach poo on poq.patient_order_id=poo.patient_order_id
  left join patient_order_outreach poo2 on poo.patient_order_id = poo2.patient_order_id and poo.outreach_date_time < poo2.outreach_date_time
  where poo2.patient_order_outreach_id is null
),
ss_query as (
  -- Pick the most recently-created screening session for the patient order
  select ss.*, a.first_name, a.last_name
  from po_query poq
  join screening_session ss on poq.patient_order_id=ss.patient_order_id
  join account a on ss.created_by_account_id=a.account_id
  left join screening_session ss2 on ss.patient_order_id = ss2.patient_order_id and ss.created < ss2.created
  where ss2.screening_session_id is null
),
recent_po_query as (
  -- Pick the most recently-closed patient order for the same MRN/institution combination
  select poq.patient_order_id, po2.episode_closed_at as most_recent_episode_closed_at
  from po_query poq
  left join patient_order po2 on LOWER(poq.patient_mrn) = LOWER(po2.patient_mrn) and poq.institution_id = po2.institution_id
  where po2.episode_closed_at is not null
  and po2.created < poq.created
  order by po2.created desc
  limit 1
),
triage_query as (
  -- Pick the most-severe triage for each patient order.
  -- Use a window function because it's easier to handle the join needed to order by severity
	WITH poct_cte AS (
	   SELECT poq.patient_order_id, poct.patient_order_care_type_id, poct.description as patient_order_care_type_description, pot.patient_order_triage_id,
	            RANK() OVER (PARTITION BY poq.patient_order_id
	            ORDER BY poct.severity DESC
	            ) AS r
	      from po_query poq, patient_order_triage pot, patient_order_care_type poct
	      where poq.patient_order_id=pot.patient_order_id
		  and pot.patient_order_care_type_id=poct.patient_order_care_type_id
		  and pot.active=true
	)
	SELECT patient_order_care_type_id, patient_order_care_type_description, patient_order_id
	FROM poct_cte
	WHERE r = 1
)
-- We need the DISTINCT here because patient outreach attempts with identical "most recent" times will cause duplicate rows
select distinct
  tq.patient_order_care_type_id,
  tq.patient_order_care_type_description,
	coalesce(pooq.outreach_count, 0) as outreach_count,
	poomaxq.outreach_date_time as most_recent_outreach_date_time,
	ssq.screening_session_id as most_recent_screening_session_id,
	ssq.created_by_account_id as most_recent_screening_session_created_by_account_id,
	ssq.first_name as most_recent_screening_session_created_by_account_first_name,
	ssq.last_name as most_recent_screening_session_created_by_account_last_name,
	ssq.completed as most_recent_screening_session_completed,
	ssq.completed_at as most_recent_screening_session_completed_at,
	panel_account.first_name as panel_account_first_name,
	panel_account.last_name as panel_account_last_name,
	poss.description as patient_order_screening_status_description,
	pod.description as patient_order_disposition_description,
	pos.description as patient_order_status_description,
	pocr.description as patient_order_closure_reason_description,
	date_part('year', age(poq.patient_birthdate at time zone i.time_zone))::int < 18 as patient_below_age_threshold,
	rpq.most_recent_episode_closed_at,
	date_part('day', now() - rpq.most_recent_episode_closed_at)::int < 30 as most_recent_episode_closed_within_date_threshold,
	poq.*
from po_query poq
left join patient_order_screening_status poss on poq.patient_order_screening_status_id = poss.patient_order_screening_status_id
left join patient_order_disposition pod on poq.patient_order_disposition_id = pod.patient_order_disposition_id
left join patient_order_status pos on poq.patient_order_status_id = pos.patient_order_status_id
left join patient_order_closure_reason pocr on poq.patient_order_closure_reason_id = pocr.patient_order_closure_reason_id
left join institution i on poq.institution_id=i.institution_id
left outer join poo_query pooq ON poq.patient_order_id = pooq.patient_order_id
left outer join poomax_query poomaxq ON poq.patient_order_id = poomaxq.patient_order_id
left outer join ss_query ssq ON poq.patient_order_id = ssq.patient_order_id
left outer join triage_query tq ON poq.patient_order_id = tq.patient_order_id
left outer join account panel_account ON poq.panel_account_id = panel_account.account_id
left outer join recent_po_query rpq on poq.patient_order_id = rpq.patient_order_id;

COMMIT;