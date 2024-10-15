BEGIN;
SELECT _v.register_patch('189-order-extra-demographics', NULL, NULL);

-- Add missing option for race
INSERT INTO race (race_id, description, display_order) VALUES ('UNKNOWN', 'Unknown', 9);

-- See https://loinc.org/LL5144-2
-- See http://open.epic.com/FHIR/StructureDefinition/extension/calculated-pronouns-to-use-for-text
CREATE TABLE preferred_pronoun (
	preferred_pronoun_id TEXT PRIMARY KEY,
	description TEXT NOT NULL,
	display_order SMALLINT NOT NULL
);

INSERT INTO preferred_pronoun (preferred_pronoun_id, description, display_order) VALUES ('NOT_ASKED', 'Not asked', 1);
INSERT INTO preferred_pronoun (preferred_pronoun_id, description, display_order) VALUES ('HE_HIM_HIS_HIS_HIMSELF', 'he/him/his/his/himself', 2);
INSERT INTO preferred_pronoun (preferred_pronoun_id, description, display_order) VALUES ('SHE_HER_HER_HERS_HERSELF', 'she/her/her/hers/herself', 3);
INSERT INTO preferred_pronoun (preferred_pronoun_id, description, display_order) VALUES ('THEY_THEM_THEIR_THEIRS_THEMSELVES', 'they/them/their/theirs/themselves', 4);
INSERT INTO preferred_pronoun (preferred_pronoun_id, description, display_order) VALUES ('ZE_ZIR_ZIR_ZIRS_ZIRSELF', 'ze/zir/zir/zirs/zirself', 5);
INSERT INTO preferred_pronoun (preferred_pronoun_id, description, display_order) VALUES ('XIE_HIR_HERE_HIR_HIRS_HIRSELF', 'xie/hir ("here")/hir/hirs/hirself', 6);
INSERT INTO preferred_pronoun (preferred_pronoun_id, description, display_order) VALUES ('CO_CO_COS_COS_COSELF', 'co/co/cos/cos/coself', 7);
INSERT INTO preferred_pronoun (preferred_pronoun_id, description, display_order) VALUES ('EN_EN_ENS_ENS_ENSELF', 'en/en/ens/ens/enself', 8);
INSERT INTO preferred_pronoun (preferred_pronoun_id, description, display_order) VALUES ('EY_EM_EIR_EIRS_EMSELF', 'ey/em/eir/eirs/emself', 9);
INSERT INTO preferred_pronoun (preferred_pronoun_id, description, display_order) VALUES ('YO_YO_YOS_YOS_YOSELF', 'yo/yo/yos/yos/yoself', 10);
INSERT INTO preferred_pronoun (preferred_pronoun_id, description, display_order) VALUES ('VE_VIS_VER_VER_VERSELF', 've/vis/ver/ver/verself', 11);
INSERT INTO preferred_pronoun (preferred_pronoun_id, description, display_order) VALUES ('OTHER', 'Other', 12);
INSERT INTO preferred_pronoun (preferred_pronoun_id, description, display_order) VALUES ('DO_NOT_USE_PRONOUNS', 'Do not use pronouns', 13);
INSERT INTO preferred_pronoun (preferred_pronoun_id, description, display_order) VALUES ('NOT_DISCLOSED', 'Do not wish to disclose', 14);

CREATE TABLE clinical_sex (
	clinical_sex_id TEXT PRIMARY KEY,
	description TEXT NOT NULL,
	display_order SMALLINT NOT NULL
);

-- See https://confluence.hl7.org/display/VOC/Sex+For+Clinical+Use
INSERT INTO clinical_sex (clinical_sex_id, description, display_order) VALUES ('NOT_ASKED', 'Not asked', 1);
INSERT INTO clinical_sex (clinical_sex_id, description, display_order) VALUES ('MALE', 'Male', 2); -- LA2-8
INSERT INTO clinical_sex (clinical_sex_id, description, display_order) VALUES ('FEMALE', 'Female', 3); -- LA3-6
INSERT INTO clinical_sex (clinical_sex_id, description, display_order) VALUES ('SPECIFIED', 'Specified', 4); -- LA32840-3
INSERT INTO clinical_sex (clinical_sex_id, description, display_order) VALUES ('UNKNOWN', 'Unknown', 5); -- LA4489-6
INSERT INTO clinical_sex (clinical_sex_id, description, display_order) VALUES ('NOT_DISCLOSED', 'Do not wish to disclose', 6);

CREATE TABLE legal_sex (
	legal_sex_id TEXT PRIMARY KEY,
	description TEXT NOT NULL,
	display_order SMALLINT NOT NULL
);

-- See https://loinc.org/72143-1
INSERT INTO legal_sex (legal_sex_id, description, display_order) VALUES ('NOT_ASKED', 'Not asked', 1);
INSERT INTO legal_sex (legal_sex_id, description, display_order) VALUES ('MALE', 'Male', 2); -- LA2-8
INSERT INTO legal_sex (legal_sex_id, description, display_order) VALUES ('FEMALE', 'Female', 3); -- LA3-6
INSERT INTO legal_sex (legal_sex_id, description, display_order) VALUES ('UNDIFFERENTIATED', 'Undifferentiated', 4); -- LA18959-9
INSERT INTO legal_sex (legal_sex_id, description, display_order) VALUES ('NOT_DISCLOSED', 'Do not wish to disclose', 5);

CREATE TABLE administrative_gender (
	administrative_gender_id TEXT PRIMARY KEY,
	description TEXT NOT NULL,
	display_order SMALLINT NOT NULL
);

-- See https://hl7.org/fhir/R4/valueset-administrative-gender.html
INSERT INTO administrative_gender (administrative_gender_id, description, display_order) VALUES ('NOT_ASKED', 'Not asked', 1);
INSERT INTO administrative_gender (administrative_gender_id, description, display_order) VALUES ('MALE', 'Male', 2);
INSERT INTO administrative_gender (administrative_gender_id, description, display_order) VALUES ('FEMALE', 'Female', 3);
INSERT INTO administrative_gender (administrative_gender_id, description, display_order) VALUES ('OTHER', 'Other', 4);
INSERT INTO administrative_gender (administrative_gender_id, description, display_order) VALUES ('UNKNOWN', 'Unknown', 5);
INSERT INTO administrative_gender (administrative_gender_id, description, display_order) VALUES ('NOT_DISCLOSED', 'Do not wish to disclose', 6);

-- Add new columns to account
ALTER TABLE account ADD COLUMN preferred_pronoun_id TEXT NOT NULL REFERENCES preferred_pronoun (preferred_pronoun_id) DEFAULT 'NOT_ASKED';
ALTER TABLE account ADD COLUMN clinical_sex_id TEXT NOT NULL REFERENCES clinical_sex (clinical_sex_id) DEFAULT 'NOT_ASKED';
ALTER TABLE account ADD COLUMN legal_sex_id TEXT NOT NULL REFERENCES legal_sex (legal_sex_id) DEFAULT 'NOT_ASKED';
ALTER TABLE account ADD COLUMN administrative_gender_id TEXT NOT NULL REFERENCES administrative_gender (administrative_gender_id) DEFAULT 'NOT_ASKED';

-- With these new columns, we need to recreate our account view.
DROP VIEW v_account;

CREATE VIEW v_account AS
WITH account_capabilities_query AS (
	 -- Collect the capability types for each account
	 SELECT
			 account_id,
			 jsonb_agg(account_capability_type_id) as account_capability_type_ids
	 FROM
			 account_capability
  GROUP BY account_id
)
SELECT a.*, acq.account_capability_type_ids
FROM account a LEFT OUTER JOIN account_capabilities_query acq on a.account_id=acq.account_id
WHERE active=TRUE;

-- Add new columns to patient order
ALTER TABLE patient_order ADD COLUMN patient_preferred_pronoun_id TEXT NOT NULL REFERENCES preferred_pronoun (preferred_pronoun_id) DEFAULT 'NOT_ASKED';
ALTER TABLE patient_order ADD COLUMN patient_clinical_sex_id TEXT NOT NULL REFERENCES clinical_sex (clinical_sex_id) DEFAULT 'NOT_ASKED';
ALTER TABLE patient_order ADD COLUMN patient_legal_sex_id TEXT NOT NULL REFERENCES legal_sex (legal_sex_id) DEFAULT 'NOT_ASKED';
ALTER TABLE patient_order ADD COLUMN patient_administrative_gender_id TEXT NOT NULL REFERENCES administrative_gender (administrative_gender_id) DEFAULT 'NOT_ASKED';

-- With these new columns, we need to recreate our patient order views.
drop view v_patient_order;
drop view v_all_patient_order;

-- Adds patient_preferred_pronoun_id, patient_clinical_sex_id, patient_legal_sex_id, and patient_administrative_gender_id.
-- No actual change to view code here because they are pulled in by "poq.*" in the FROM clause.
CREATE OR REPLACE VIEW v_all_patient_order AS WITH
poo_query AS (
    -- Count up the patient outreach attempts for each patient order
    select
        poq.patient_order_id,
        count(poo.*) AS outreach_count
    from
        patient_order_outreach poo,
        patient_order poq
    where
        poq.patient_order_id = poo.patient_order_id
        AND poo.deleted=FALSE
    group by
        poq.patient_order_id
),
poomax_query AS (
    -- Pick the most recent patient outreach attempt for each patient order
    select
        poo.patient_order_id, MAX(poo.outreach_date_time) as max_outreach_date_time
    from
        patient_order poq,
        patient_order_outreach poo
    where
        poq.patient_order_id = poo.patient_order_id
        and poo.deleted = false
    group by
        poo.patient_order_id
),
reason_for_referral_query AS (
    -- Pick reasons for referral for each patient order
    select
        poq.patient_order_id, string_agg(porr.description, ', ' order by por.display_order) AS reason_for_referral
    from
        patient_order poq,
        patient_order_referral_reason porr,
        patient_order_referral por
    where
        poq.patient_order_id = por.patient_order_id
        AND por.patient_order_referral_reason_id=porr.patient_order_referral_reason_id
    group by
        poq.patient_order_id
),
smg_query AS (
    -- Count up the scheduled message groups with a successful delivery for each patient order
    select
        poq.patient_order_id,
        count(posmg.*) AS scheduled_message_group_delivered_count
    from
        patient_order_scheduled_message_group posmg,
        patient_order poq
    where
        poq.patient_order_id = posmg.patient_order_id
        AND posmg.deleted=false
        and EXISTS (
		    SELECT ml.message_id
		    FROM patient_order_scheduled_message posm, scheduled_message sm, message_log ml
		    WHERE posmg.patient_order_scheduled_message_group_id = posm.patient_order_scheduled_message_group_id
		    AND posm.scheduled_message_id=sm.scheduled_message_id
		    AND sm.message_id=ml.message_id
		    AND ml.message_status_id='DELIVERED'
		)
    group by
        poq.patient_order_id
),
smgmax_query AS (
    -- Pick the most-distant scheduled message group with a successful delivery for each patient order
    select
        posmg.patient_order_id, MAX(posmg.scheduled_at_date_time) as max_delivered_scheduled_message_group_date_time
    from
        patient_order poq,
        patient_order_scheduled_message_group posmg
    where
        poq.patient_order_id = posmg.patient_order_id
        and posmg.deleted = false
        and EXISTS (
		    SELECT ml.message_id
		    FROM patient_order_scheduled_message posm, scheduled_message sm, message_log ml
		    WHERE posmg.patient_order_scheduled_message_group_id = posm.patient_order_scheduled_message_group_id
		    AND posm.scheduled_message_id=sm.scheduled_message_id
		    AND sm.message_id=ml.message_id
		    AND ml.message_status_id='DELIVERED'
		)
    group by
        posmg.patient_order_id
),
next_resource_check_in_scheduled_message_group_query AS (
    -- Pick the next nondeleted scheduled message group in the future of type RESOURCE_CHECK_IN that has not yet been delivered
	select * from (
	  select
	    posmg.patient_order_id,
	    posmg.patient_order_scheduled_message_group_id as next_resource_check_in_scheduled_message_group_id,
	    posmg.scheduled_at_date_time as next_resource_check_in_scheduled_at_date_time,
	    rank() OVER (PARTITION BY posmg.patient_order_id ORDER BY posmg.scheduled_at_date_time, posmg.patient_order_scheduled_message_group_id) as ranked_value
	  from
	    patient_order poq, patient_order_scheduled_message_group posmg, institution i
	    where poq.patient_order_id = posmg.patient_order_id
	    and posmg.patient_order_scheduled_message_type_id='RESOURCE_CHECK_IN'
        and posmg.deleted=false
        and poq.institution_id=i.institution_id
        and posmg.scheduled_at_date_time at time zone i.time_zone > now()
        and not EXISTS (
		    SELECT ml.message_id
		    FROM patient_order_scheduled_message posm, scheduled_message sm, message_log ml
		    WHERE posmg.patient_order_scheduled_message_group_id = posm.patient_order_scheduled_message_group_id
		    AND posm.scheduled_message_id=sm.scheduled_message_id
		    AND sm.message_id=ml.message_id
		    AND ml.message_status_id='DELIVERED'
		)
	) subquery where ranked_value=1
),
next_appt_query AS (
	select * from (
	  select
	    app.patient_order_id,
	    app.appointment_id,
	    app.canceled,
	    p.provider_id,
	    p.name as provider_name,
	    app.start_time as appointment_start_time,
	    app.created_by_account_id,
	    rank() OVER (PARTITION BY app.patient_order_id ORDER BY app.start_time, app.appointment_id) as ranked_value
	  from
	    patient_order poq, appointment app, provider p
	    where poq.patient_order_id = app.patient_order_id
	    and app.provider_id=p.provider_id
	    and app.canceled=false
	    -- Not filtering on "> now()" because there should only ever be 1 uncanceled appointment per order.
	    -- We also don't want the appointment to "disappear" in the UI as soon as it starts.
	) subquery where ranked_value=1
),
recent_voicemail_task_query AS (
    -- Pick the most recent voicemail task for each patient order
	select * from (
	  select
	    povt.patient_order_id,
	    povt.patient_order_voicemail_task_id,
	    povt.completed as patient_order_voicemail_task_completed,
	    rank() OVER (PARTITION BY povt.patient_order_id ORDER BY povt.created DESC, povt.patient_order_voicemail_task_id) as ranked_value
	  from
	    patient_order poq, patient_order_voicemail_task povt
	    where poq.patient_order_id = povt.patient_order_id
        and povt.deleted = FALSE
	) subquery where ranked_value=1
),
next_scheduled_outreach_query AS (
    -- Pick the next active scheduled outreach for each patient order
	select * from (
	  select
	    poso.patient_order_id,
	    poso.patient_order_scheduled_outreach_id as next_scheduled_outreach_id,
	    poso.scheduled_at_date_time as next_scheduled_outreach_scheduled_at_date_time,
        poso.patient_order_outreach_type_id as next_scheduled_outreach_type_id,
        poso.patient_order_scheduled_outreach_reason_id as next_scheduled_outreach_reason_id,
	    rank() OVER (PARTITION BY poso.patient_order_id ORDER BY poso.scheduled_at_date_time, poso.patient_order_scheduled_outreach_id) as ranked_value
	  from
	    patient_order poq, patient_order_scheduled_outreach poso
	    where poq.patient_order_id = poso.patient_order_id
        and poso.patient_order_scheduled_outreach_status_id = 'SCHEDULED'
	) subquery where ranked_value=1
),
most_recent_message_delivered_query AS (
    -- Pick the message that has been most recently delivered to the patient
	select * from (
	  select
	    posmg.patient_order_id,
	    ml.delivered as most_recent_message_delivered_at,
	    rank() OVER (PARTITION BY posmg.patient_order_id ORDER BY ml.delivered DESC) as ranked_value
	  from
	    patient_order poq, patient_order_scheduled_message_group posmg, patient_order_scheduled_message posm, scheduled_message sm, message_log ml
	    where poq.patient_order_id = posmg.patient_order_id
	    and posmg.patient_order_scheduled_message_group_id=posm.patient_order_scheduled_message_group_id
        and posm.scheduled_message_id=sm.scheduled_message_id
        and sm.message_id=ml.message_id
        and ml.message_status_id='DELIVERED'
	) subquery where ranked_value=1
),
ss_query AS (
    -- Pick the most recently-created clinical screening session for the patient order
	select * from (
	  select
	    ss.*,
	    a.first_name,
	    a.last_name,
	    a.role_id,
	    rank() OVER (PARTITION BY ss.patient_order_id ORDER BY ss.created DESC) as ranked_value
	  from
	    patient_order poq, screening_session ss, account a, institution i, screening_flow_version sfv
	    where poq.patient_order_id = ss.patient_order_id
	    and i.integrated_care_screening_flow_id=sfv.screening_flow_id
	    and sfv.screening_flow_version_id =ss.screening_flow_version_id
	    and ss.created_by_account_id =a.account_id
	    and i.institution_id = a.institution_id
	) subquery where ranked_value=1
),
ss_intake_query AS (
    -- Pick the most recently-created intake screening session for the patient order
	select * from (
	  select
	    ss.*,
	    a.first_name,
	    a.last_name,
	    a.role_id,
	    rank() OVER (PARTITION BY ss.patient_order_id ORDER BY ss.created DESC) as ranked_value
	  from
	    patient_order poq, screening_session ss, account a, institution i, screening_flow_version sfv
	    where poq.patient_order_id = ss.patient_order_id
	    and i.integrated_care_intake_screening_flow_id=sfv.screening_flow_id
	    and sfv.screening_flow_version_id =ss.screening_flow_version_id
	    and ss.created_by_account_id =a.account_id
	    and i.institution_id = a.institution_id
	) subquery where ranked_value=1
),
permitted_regions_query AS (
    -- Pick the permitted set of IC regions (state abbreviations in the US) by institution as an array for easy access
    select
        institution_id,
        array_agg(region_abbreviation) as permitted_region_abbreviations
    from
        institution_integrated_care_region
    group by institution_id
),
recent_scheduled_screening_query AS (
    -- Pick the most recently-scheduled screening for the patient order
	select * from (
	  select
	    poss.*,
	    rank() OVER (PARTITION BY poss.patient_order_id ORDER BY poss.scheduled_date_time) as ranked_value
	  from
	    patient_order poq, patient_order_scheduled_screening poss
	    where poq.patient_order_id = poss.patient_order_id
	    and poss.canceled=FALSE
	) subquery where ranked_value=1
), recent_po_query AS (
    -- Get the last order based on the order date for this patient
    select
        poq.patient_order_id,
        lag(poq.episode_closed_at, 1) OVER
           (PARTITION BY  patient_mrn ORDER BY poq.order_date) as most_recent_episode_closed_at
    from
        patient_order poq
)
select
    potg.patient_order_care_type_id,
    poct.description as patient_order_care_type_description,
    potg.patient_order_triage_source_id,
    coalesce(pooq.outreach_count, 0) AS outreach_count,
    poomaxq.max_outreach_date_time AS most_recent_outreach_date_time,
    coalesce(smgq.scheduled_message_group_delivered_count, 0) AS scheduled_message_group_delivered_count,
    smgmaxq.max_delivered_scheduled_message_group_date_time AS most_recent_delivered_scheduled_message_group_date_time,
    coalesce(pooq.outreach_count, 0) + coalesce(smgq.scheduled_message_group_delivered_count, 0) as total_outreach_count,
    GREATEST(poomaxq.max_outreach_date_time, smgmaxq.max_delivered_scheduled_message_group_date_time) AS most_recent_total_outreach_date_time,
    ssq.screening_session_id AS most_recent_screening_session_id,
    ssq.created AS most_recent_screening_session_created_at,
    ssq.created_by_account_id AS most_recent_screening_session_created_by_account_id,
    ssq.role_id AS most_recent_screening_session_created_by_account_role_id,
    ssq.first_name AS most_recent_screening_session_created_by_account_first_name,
    ssq.last_name AS most_recent_screening_session_created_by_account_last_name,
    ssq.completed AS most_recent_screening_session_completed,
    ssq.completed_at AS most_recent_screening_session_completed_at,
    CASE
        WHEN ssq.completed = TRUE THEN 'COMPLETE'
        WHEN ssq.screening_session_id IS NOT NULL THEN 'IN_PROGRESS'
        WHEN rssq.scheduled_date_time IS NOT NULL THEN 'SCHEDULED'
        ELSE 'NOT_SCREENED'
    END patient_order_screening_status_id,
    CASE
        WHEN ssq.completed = TRUE THEN 'Complete'
        WHEN ssq.screening_session_id IS NOT NULL THEN 'In Progress'
        WHEN rssq.scheduled_date_time IS NOT NULL THEN 'Scheduled'
        ELSE 'Not Screened'
    END patient_order_screening_status_description,
    CASE
        WHEN poq.patient_account_id = ssq.created_by_account_id THEN true
        ELSE false
    END most_recent_screening_session_by_patient,
    (
    	ssq.screening_session_id IS NOT NULL
    	AND ssq.completed = FALSE
    	AND ssq.created < (NOW() - INTERVAL '1 hour')
    ) AS most_recent_screening_session_appears_abandoned,
    CASE
      WHEN ssq.completed = TRUE AND poq.encounter_synced_at IS NULL THEN 'NEEDS_DOCUMENTATION'
      WHEN ssq.completed = TRUE AND poq.encounter_synced_at IS NOT NULL THEN 'DOCUMENTED'
      ELSE 'NOT_DOCUMENTED'
    END patient_order_encounter_documentation_status_id,
    ssiq.screening_session_id AS most_recent_intake_screening_session_id,
    ssiq.created AS most_recent_intake_screening_session_created_at,
    ssiq.created_by_account_id AS most_recent_intake_screening_session_created_by_account_id,
    ssiq.role_id AS most_recent_intake_screening_session_created_by_account_role_id,
    ssiq.first_name AS most_recent_intake_screening_session_created_by_account_fn,
    ssiq.last_name AS most_recent_intake_screening_session_created_by_account_ln,
    ssiq.completed AS most_recent_intake_screening_session_completed,
    ssiq.completed_at AS most_recent_intake_screening_session_completed_at,
    CASE
        WHEN ssiq.completed = TRUE THEN 'COMPLETE'
        WHEN ssiq.screening_session_id IS NOT NULL THEN 'IN_PROGRESS'
        ELSE 'NOT_SCREENED'
    END patient_order_intake_screening_status_id,
    CASE
        WHEN ssiq.completed = TRUE THEN 'Complete'
        WHEN ssiq.screening_session_id IS NOT NULL THEN 'In Progress'
        ELSE 'Not Screened'
    END patient_order_intake_screening_status_description,
    CASE
        WHEN poq.patient_account_id = ssiq.created_by_account_id THEN true
        ELSE false
    END most_recent_intake_screening_session_by_patient,
    (
    	ssiq.screening_session_id IS NOT NULL
    	AND ssiq.completed = FALSE
    	AND ssiq.created < (NOW() - INTERVAL '1 hour')
    ) AS most_recent_intake_screening_session_appears_abandoned,

    (
      -- Intake must always be complete.
      -- But, it's possible to skip clinical if intake short-circuits it entirely.
      -- So, we're satisfied if intake is complete AND...
      (ssiq.screening_session_id IS NOT NULL AND ssiq.completed = TRUE)
      AND (
        -- Either clinical is complete
        (ssq.screening_session_id IS NOT NULL AND ssq.completed = TRUE)
        -- OR clinical does not exist at all (this occurs if intake short-circuits, clinical is never created)
        OR
        (ssq.screening_session_id IS NULL)
        -- OR clinical exists BUT was created prior to the most recent completed intake was created
        OR
        (ssq.screening_session_id IS NOT NULL AND ssq.completed = FALSE AND ssiq.created > ssq.created)
      )
    ) AS most_recent_intake_and_clinical_screenings_satisfied,

    panel_account.first_name AS panel_account_first_name,
    panel_account.last_name AS panel_account_last_name,
    pod.description AS patient_order_disposition_description,
    CASE
        -- Screening completed, most severe level of care type triage is SPECIALTY
        WHEN potg.patient_order_care_type_id = 'SPECIALTY' THEN 'SPECIALTY_CARE'
        -- Screening completed, most severe level of care type triage is SUBCLINICAL
        WHEN potg.patient_order_care_type_id = 'SUBCLINICAL' THEN 'SUBCLINICAL'
        -- Screening completed, most severe level of care type triage is COLLABORATIVE.  Patient or MHIC can schedule with a provider
        WHEN potg.patient_order_care_type_id = 'COLLABORATIVE' THEN 'MHP'
        -- None of the above apply
        ELSE 'NOT_TRIAGED'
    END patient_order_triage_status_id,
    CASE
        -- Screening completed, most severe level of care type triage is SPECIALTY
        WHEN potg.patient_order_care_type_id = 'SPECIALTY' THEN 'Specialty Care'
        -- Screening completed, most severe level of care type triage is SUBCLINICAL
        WHEN potg.patient_order_care_type_id = 'SUBCLINICAL' THEN 'Subclinical'
        -- Screening completed, most severe level of care type triage is COLLABORATIVE.  Patient or MHIC can schedule with a provider
        WHEN potg.patient_order_care_type_id = 'COLLABORATIVE' THEN 'MHP'
        -- None of the above apply
        else 'Not Triaged'
    END patient_order_triage_status_description,
    pocr.description AS patient_order_closure_reason_description,
    DATE_PART('year', AGE(poq.order_date, poq.patient_birthdate))::INT AS patient_age_on_order_date,
    DATE_PART('year', AGE(poq.order_date, poq.patient_birthdate))::INT < 18 AS patient_below_age_threshold,
    rpq.most_recent_episode_closed_at,
    DATE_PART('day', NOW() - rpq.most_recent_episode_closed_at)::INT < 30 AS most_recent_episode_closed_within_date_threshold,
    rssq.patient_order_scheduled_screening_id,
    rssq.scheduled_date_time AS patient_order_scheduled_screening_scheduled_date_time,
    rssq.calendar_url AS patient_order_scheduled_screening_calendar_url,

    -- Figure out "outreach followup needed".
    -- This means...
    (
       (
        -- 1. Order is open
        poq.patient_order_disposition_id='OPEN'
        AND (
					-- 2. ...and any of the following are indicated during intake:
					-- * Patient declines IC
					-- * Patient does not live in an acceptable location
					-- * Patient insurance is invalid or changed recently
					poq.patient_order_intake_wants_services_status_id='NO'
					OR poq.patient_order_intake_location_status_id='INVALID'
					OR poq.patient_order_intake_insurance_status_id IN ('INVALID', 'CHANGED_RECENTLY')
        )
       ) OR (
        -- 1. Order is open
        poq.patient_order_disposition_id='OPEN'
        -- 2. Screening has not been started or scheduled
        -- Basically patient_order_screening_status_id='NOT_SCREENED' above
        AND (ssq.screening_session_id IS NULL AND rssq.scheduled_date_time IS NULL)
        -- 3. At least one outreach has been performed (either recorded by MHIC or successfully-delivered SMS/email message)
        -- Basically total_outreach_count > 0 above
        AND (coalesce(pooq.outreach_count, 0) + coalesce(smgq.scheduled_message_group_delivered_count, 0)) > 0
        -- 4. The most recent outreach plus the institution day offset is on or after "now" (normalized for institution timezone)
        -- Basically most_recent_total_outreach_date_time above + [institution offset] >= NOW()
        AND (
            (GREATEST(poomaxq.max_outreach_date_time, smgmaxq.max_delivered_scheduled_message_group_date_time) + make_interval(days => i.integrated_care_outreach_followup_day_offset)) AT TIME ZONE i.time_zone <= NOW()
        ))
    ) AS outreach_followup_needed,

    naq.appointment_start_time,
    naq.provider_id,
    naq.provider_name,
    naq.appointment_id,
    CASE
        WHEN appointment_id IS NOT NULL THEN true
        ELSE FALSE
    END appointment_scheduled,
    CASE
        WHEN naq.created_by_account_id = poq.patient_account_id THEN true
        ELSE FALSE
    END appointment_scheduled_by_patient,
    rvtq.patient_order_voicemail_task_id AS most_recent_patient_order_voicemail_task_id,
    rvtq.patient_order_voicemail_task_completed AS most_recent_patient_order_voicemail_task_completed,
      rfrq.reason_for_referral,
      patient_address.street_address_1 as patient_address_street_address_1,
      patient_address.locality as patient_address_locality,
      patient_address.region as patient_address_region,
      patient_address.postal_code as patient_address_postal_code,
      patient_address.country_code as patient_address_country_code,
      patient_address.region=any(prq.permitted_region_abbreviations) as patient_address_region_accepted,
      -- "completed" means all fields filled in, but not necessarily valid
      (
        poq.patient_first_name IS NOT NULL
        and poq.patient_last_name IS NOT NULL
        and poq.patient_phone_number IS NOT NULL
        and poq.patient_email_address IS NOT NULL
        and poq.patient_birthdate IS NOT NULL
        and patient_address.street_address_1 IS NOT NULL
        and patient_address.locality IS NOT NULL
        and patient_address.region IS NOT NULL
        and patient_address.postal_code IS NOT NULL
      ) as patient_demographics_completed,
      -- "accepted" means all fields filled in AND meet requirements for IC (accepted state and insurance)
      (
        poq.patient_first_name IS NOT NULL
        and poq.patient_last_name IS NOT NULL
        and poq.patient_phone_number IS NOT NULL
        and poq.patient_email_address IS NOT NULL
        and poq.patient_birthdate IS NOT NULL
        and patient_address.street_address_1 IS NOT NULL
        and patient_address.locality IS NOT NULL
        and patient_address.region=any(prq.permitted_region_abbreviations)
        and patient_address.postal_code IS NOT NULL
      ) as patient_demographics_accepted,
    posmg.scheduled_at_date_time AS resource_check_in_scheduled_at_date_time,
    (
        poq.patient_order_resource_check_in_response_status_id = 'NONE'
        AND posmg.scheduled_at_date_time IS NOT NULL
        AND (posmg.scheduled_at_date_time AT TIME ZONE i.time_zone) < now()
    ) AS resource_check_in_response_needed,
    porcirs.description AS patient_order_resource_check_in_response_status_description,
    patient_demographics_confirmed_at IS NOT NULL AS patient_demographics_confirmed,
    DATE_PART('day', (COALESCE(poq.episode_closed_at, now()) - (poq.order_date + make_interval(mins => poq.order_age_in_minutes)))) AS episode_duration_in_days,
    ed.name AS epic_department_name,
    ed.department_id AS epic_department_department_id,
    mrmdq.most_recent_message_delivered_at,
    nsoq.next_scheduled_outreach_id,
    nsoq.next_scheduled_outreach_scheduled_at_date_time,
    nsoq.next_scheduled_outreach_type_id,
    nsoq.next_scheduled_outreach_reason_id,
    GREATEST(
      -- Most recent message delivery timestamp
      mrmdq.most_recent_message_delivered_at,
      -- Most recent recorded outreach record that is before "now"
      case
	      when poomaxq.max_outreach_date_time AT TIME ZONE i.time_zone < now() then poomaxq.max_outreach_date_time AT TIME ZONE i.time_zone
	      else null
	  end,
	  -- Most recently-started intake screening session where the MHIC is the one performing it (not the patient self-assessing)
	  case
	  	  when ssq.screening_session_id is not null and ((ssq.target_account_id is null) OR (ssq.target_account_id != ssq.created_by_account_id)) then ssq.created
	  	  else null
	  end
	  -- TODO: do we need to also take the most recently scheduled screening session for this order into account? For now, no, because in theory these would be recorded as outreaches by the MHIC.
    ) as last_contacted_at,
    -- TODO: take the following into account:
    -- 3. RESOURCE_CHECK_IN: if non-canceled scheduled message group with patient_order_scheduled_message_type_id of RESOURCE_CHECK_IN is scheduled for after 'now' in institution time zone
    CASE
        WHEN ssq.screening_session_id is null and rssq.scheduled_date_time=LEAST(rssq.scheduled_date_time, nsoq.next_scheduled_outreach_scheduled_at_date_time) THEN 'ASSESSMENT'
        WHEN nsoq.next_scheduled_outreach_scheduled_at_date_time=LEAST(rssq.scheduled_date_time, nsoq.next_scheduled_outreach_scheduled_at_date_time) and nsoq.next_scheduled_outreach_reason_id='RESOURCE_FOLLOWUP' THEN 'RESOURCE_FOLLOWUP'
        WHEN nsoq.next_scheduled_outreach_scheduled_at_date_time=LEAST(rssq.scheduled_date_time, nsoq.next_scheduled_outreach_scheduled_at_date_time) and nsoq.next_scheduled_outreach_reason_id='OTHER' THEN 'OTHER'
        when poomaxq.max_outreach_date_time is null and smgmaxq.max_delivered_scheduled_message_group_date_time is null and ssiq.screening_session_id is null then 'WELCOME_MESSAGE'
        -- There has been some form of outreach but no screening session scheduled or started after X days
        WHEN ssq.screening_session_id is null and rssq.scheduled_date_time is null and (poomaxq.max_outreach_date_time is not null or smgmaxq.max_delivered_scheduled_message_group_date_time is not null) and ((GREATEST(poomaxq.max_outreach_date_time, smgmaxq.max_delivered_scheduled_message_group_date_time) + make_interval(days => i.integrated_care_outreach_followup_day_offset)) AT TIME ZONE i.time_zone <= NOW()) then 'ASSESSMENT_OUTREACH'
        -- Next resource check-in message
        when nrcismgq.next_resource_check_in_scheduled_message_group_id is not null then 'RESOURCE_CHECK_IN'
        -- TODO: RESOURCE_CHECK_IN_FOLLOWUP
        ELSE NULL
    END as next_contact_type_id,
	case
        WHEN ssq.screening_session_id is null and rssq.scheduled_date_time=LEAST(rssq.scheduled_date_time, nsoq.next_scheduled_outreach_scheduled_at_date_time) THEN rssq.scheduled_date_time
        WHEN nsoq.next_scheduled_outreach_scheduled_at_date_time=LEAST(rssq.scheduled_date_time, nsoq.next_scheduled_outreach_scheduled_at_date_time) THEN nsoq.next_scheduled_outreach_scheduled_at_date_time
        when poomaxq.max_outreach_date_time is null and smgmaxq.max_delivered_scheduled_message_group_date_time is null and ssiq.screening_session_id is null then null
        -- There has been some form of outreach but no screening session scheduled or started after X days
        WHEN ssq.screening_session_id is null and rssq.scheduled_date_time is null and (poomaxq.max_outreach_date_time is not null or smgmaxq.max_delivered_scheduled_message_group_date_time is not null) and ((GREATEST(poomaxq.max_outreach_date_time, smgmaxq.max_delivered_scheduled_message_group_date_time) + make_interval(days => i.integrated_care_outreach_followup_day_offset)) AT TIME ZONE i.time_zone <= NOW()) then NULL
        -- Next resource check-in message
        when nrcismgq.next_resource_check_in_scheduled_message_group_id is not null then nrcismgq.next_resource_check_in_scheduled_at_date_time
        -- TODO: RESOURCE_CHECK_IN_FOLLOWUP
        ELSE NULL
    END as next_contact_scheduled_at,
    poq.*
from
    patient_order poq
    left join patient_order_disposition pod ON poq.patient_order_disposition_id = pod.patient_order_disposition_id
    left join patient_order_closure_reason pocr ON poq.patient_order_closure_reason_id = pocr.patient_order_closure_reason_id
    left join institution i ON poq.institution_id = i.institution_id
    left join permitted_regions_query prq ON poq.institution_id = prq.institution_id
    left join patient_order_resource_check_in_response_status porcirs ON poq.patient_order_resource_check_in_response_status_id=porcirs.patient_order_resource_check_in_response_status_id
    left join epic_department ed ON poq.epic_department_id = ed.epic_department_id
    left outer join address patient_address ON poq.patient_address_id = patient_address.address_id
    left outer join poo_query pooq ON poq.patient_order_id = pooq.patient_order_id
    left outer join poomax_query poomaxq ON poq.patient_order_id = poomaxq.patient_order_id
    left outer join smg_query smgq ON poq.patient_order_id = smgq.patient_order_id
    left outer join smgmax_query smgmaxq ON poq.patient_order_id = smgmaxq.patient_order_id
    left outer join ss_query ssq ON poq.patient_order_id = ssq.patient_order_id
    left outer join ss_intake_query ssiq ON poq.patient_order_id = ssiq.patient_order_id
    left outer join patient_order_triage_group potg ON poq.patient_order_id = potg.patient_order_id and potg.active=TRUE
    left outer join patient_order_care_type poct ON potg.patient_order_care_type_id = poct.patient_order_care_type_id
    left outer join account panel_account ON poq.panel_account_id = panel_account.account_id
    left outer join recent_po_query rpq ON poq.patient_order_id = rpq.patient_order_id
    left outer join recent_scheduled_screening_query rssq ON poq.patient_order_id = rssq.patient_order_id
    left outer join next_appt_query naq on poq.patient_order_id=naq.patient_order_id
    left outer join recent_voicemail_task_query rvtq on poq.patient_order_id=rvtq.patient_order_id
    left outer join reason_for_referral_query rfrq on poq.patient_order_id=rfrq.patient_order_id
    left outer join next_scheduled_outreach_query nsoq ON poq.patient_order_id=nsoq.patient_order_id
    left outer join most_recent_message_delivered_query mrmdq on poq.patient_order_id=mrmdq.patient_order_id
    left outer join next_resource_check_in_scheduled_message_group_query nrcismgq on poq.patient_order_id=nrcismgq.patient_order_id
    left outer join patient_order_scheduled_message_group posmg ON poq.resource_check_in_scheduled_message_group_id=posmg.patient_order_scheduled_message_group_id AND posmg.deleted = FALSE;

CREATE or replace VIEW v_patient_order AS
SELECT * FROM v_all_patient_order
WHERE patient_order_disposition_id != 'ARCHIVED';

COMMIT;