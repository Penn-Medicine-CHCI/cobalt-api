BEGIN;
SELECT _v.register_patch('110-ic-updates', NULL, NULL);

ALTER TABLE institution ADD COLUMN integrated_care_intake_screening_flow_id UUID REFERENCES screening_flow;

INSERT INTO screening_type (screening_type_id, description) VALUES ('IC_INTAKE', 'Integrated Care Intake');
INSERT INTO screening_flow_type (screening_flow_type_id, description) VALUES ('INTEGRATED_CARE_INTAKE', 'Integrated Care Intake');

-- Status flags driven by IC intake questions
CREATE TABLE patient_order_intake_wants_services_status (
  patient_order_intake_wants_services_status_id VARCHAR PRIMARY KEY,
  description VARCHAR NOT NULL
);

INSERT INTO patient_order_intake_wants_services_status VALUES ('UNKNOWN', 'Unknown');
INSERT INTO patient_order_intake_wants_services_status VALUES ('YES', 'Yes');
INSERT INTO patient_order_intake_wants_services_status VALUES ('NO', 'No');

CREATE TABLE patient_order_intake_location_status (
  patient_order_intake_location_status_id VARCHAR PRIMARY KEY,
  description VARCHAR NOT NULL
);

INSERT INTO patient_order_intake_location_status VALUES ('UNKNOWN', 'Unknown');
INSERT INTO patient_order_intake_location_status VALUES ('VALID', 'Valid');
INSERT INTO patient_order_intake_location_status VALUES ('INVALID', 'Invalid');

CREATE TABLE patient_order_intake_insurance_status (
  patient_order_intake_insurance_status_id VARCHAR PRIMARY KEY,
  description VARCHAR NOT NULL
);

INSERT INTO patient_order_intake_insurance_status VALUES ('UNKNOWN', 'Unknown');
INSERT INTO patient_order_intake_insurance_status VALUES ('VALID', 'Valid');
INSERT INTO patient_order_intake_insurance_status VALUES ('INVALID', 'Invalid');
INSERT INTO patient_order_intake_insurance_status VALUES ('CHANGED_RECENTLY', 'Changed Recently');

-- Need a few more focus types to conform to latest rules
INSERT INTO patient_order_focus_type VALUES ('PSYCHIATRY', 'Psychiatry');
INSERT INTO patient_order_focus_type VALUES ('LGBTIA', 'LGBTIA+ Competent Provider');

-- Add concept of strongly-typed "reason for referral" so we can more easily take into consideration when triaging
CREATE TABLE patient_order_referral_reason (
  patient_order_referral_reason_id VARCHAR PRIMARY KEY,
  description VARCHAR NOT NULL
);

INSERT INTO patient_order_referral_reason VALUES ('UNKNOWN', 'Unknown'); -- Special value
INSERT INTO patient_order_referral_reason VALUES ('ADJUSTMENT_DISORDERS', 'Adjustment disorders');
INSERT INTO patient_order_referral_reason VALUES ('ALCOHOL_MISUSE_OR_ADDICTION', 'Alcohol misuse or addiction');
INSERT INTO patient_order_referral_reason VALUES ('ANXIETY_SYMPTOMS', 'Anxiety symptoms');
INSERT INTO patient_order_referral_reason VALUES ('DRUG_MISUSE_OR_ADDICTION', 'Drug misuse or addiction');
INSERT INTO patient_order_referral_reason VALUES ('FEEDING_OR_EATING_DISORDERS', 'Feeding or eating disorders');
INSERT INTO patient_order_referral_reason VALUES ('IMPULSE_CONTROL_AND_CONDUCT_DISORDERS', 'Impulse control and conduct disorders');
INSERT INTO patient_order_referral_reason VALUES ('MOOD_OR_DEPRESSION_SYMPTOMS', 'Mood or depression symptoms');
INSERT INTO patient_order_referral_reason VALUES ('NEUROCOGNITIVE_DISORDERS', 'Neurocognitive disorders');
INSERT INTO patient_order_referral_reason VALUES ('NEURODEVELOPMENTAL_DISORDERS', 'Neurodevelopmental disorders');
INSERT INTO patient_order_referral_reason VALUES ('OBSESSIVE_COMPULSIVE_DISORDERS', 'Obsessive compulsive disorders');
INSERT INTO patient_order_referral_reason VALUES ('OPIOID_USE_DISORDER', 'Opioid Use Disorder (OUD)');
INSERT INTO patient_order_referral_reason VALUES ('PERSONALITY_DISORDERS', 'Personality disorders');
INSERT INTO patient_order_referral_reason VALUES ('PSYCHOPHARMACOLOGY_MANAGEMENT', 'Psychopharmacology management');
INSERT INTO patient_order_referral_reason VALUES ('PSYCHOSIS', 'Psychosis');
INSERT INTO patient_order_referral_reason VALUES ('PTSD_OR_TRAUMA_RELATED_SYMPTOMS', 'PTSD or trauma related symptoms');
INSERT INTO patient_order_referral_reason VALUES ('SEXUAL_INTEREST_OR_DISFUNCTION_OR_GENDER_DYSPHORIA', 'Sexual interest disorders / sexual dysfunction / gender dysphoria');
INSERT INTO patient_order_referral_reason VALUES ('SLEEP_WAKE_CYCLE_DISORDERS', 'Sleep-wake cycle disorders');
INSERT INTO patient_order_referral_reason VALUES ('TREATMENT_ENGAGEMENT', 'Treatment engagement');

-- Replaces patient_order_reason_for_referral
CREATE TABLE patient_order_referral (
  patient_order_id UUID NOT NULL REFERENCES patient_order,
  patient_order_referral_reason_id VARCHAR NOT NULL REFERENCES patient_order_referral_reason,
  display_order INTEGER NOT NULL,
  created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (patient_order_id, patient_order_referral_reason_id)
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON patient_order_referral FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

-- Migrate over existing referral data from patient_order_reason_for_referral.
-- First, fix up nonstandard name
UPDATE patient_order_reason_for_referral SET reason_for_referral='Drug misuse or addiction' where reason_for_referral='Alcohol or drug use or abuse';

-- Then, migrate in
INSERT INTO patient_order_referral (patient_order_id, patient_order_referral_reason_id, display_order)
SELECT porfr.patient_order_id, porr.patient_order_referral_reason_id, porfr.display_order
FROM patient_order_reason_for_referral porfr, patient_order_referral_reason porr
WHERE LOWER(porfr.reason_for_referral)=LOWER(porr.description);

-- Add concept of "patient_order_triage_group" so we can tie multiple triages together more formally than using "they have the same timestamp"
CREATE TABLE patient_order_triage_group (
  patient_order_triage_group_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  patient_order_id UUID NOT NULL REFERENCES patient_order,
  patient_order_care_type_id TEXT NOT NULL REFERENCES patient_order_care_type, -- This is the calculated + stored "final" care type based on evaluating all of the triages for this group
  patient_order_triage_override_reason_id TEXT NOT NULL REFERENCES patient_order_triage_override_reason,
  patient_order_triage_source_id TEXT NOT NULL REFERENCES patient_order_triage_source,
  account_id UUID NOT NULL REFERENCES account,
  screening_session_id UUID REFERENCES screening_session,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON patient_order_triage_group FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

-- Migrate over old groups
INSERT INTO patient_order_triage_group (patient_order_id, patient_order_care_type_id, patient_order_triage_override_reason_id, patient_order_triage_source_id, account_id, screening_session_id, active, created)
SELECT old.patient_order_id, 'UNSPECIFIED', old.patient_order_triage_override_reason_id, old.patient_order_triage_source_id, old.account_id, old.screening_session_id, old.active, old.created
FROM (
  SELECT COUNT(*), patient_order_id, active, created, patient_order_triage_source_id, patient_order_triage_override_reason_id, account_id, screening_session_id
  FROM patient_order_triage
  GROUP BY created, patient_order_id, active, patient_order_triage_source_id, patient_order_triage_override_reason_id, account_id, screening_session_id
) old;

ALTER TABLE patient_order_triage ADD COLUMN patient_order_triage_group_id UUID REFERENCES patient_order_triage_group;

UPDATE patient_order_triage pot
SET patient_order_triage_group_id=potg.patient_order_triage_group_id
FROM patient_order_triage_group potg
WHERE pot.patient_order_id=potg.patient_order_id
AND pot.created=potg.created;

-- Pick the old calculated patient_order_care_type_id value out of the view to store off on the patient_order_triage_group.
-- The old inactive ones will be left as UNSPECIFIED but that's OK because this is test data currently
UPDATE patient_order_triage_group potg
SET patient_order_care_type_id=po.patient_order_care_type_id
FROM v_all_patient_order po
WHERE po.patient_order_id=potg.patient_order_id
AND potg.active=TRUE;

-- Drop order views in preparation for modifying columns
DROP VIEW v_patient_order;
DROP VIEW v_all_patient_order;

ALTER TABLE patient_order_triage DROP COLUMN patient_order_id;
ALTER TABLE patient_order_triage DROP COLUMN active;
ALTER TABLE patient_order_triage DROP COLUMN account_id;
ALTER TABLE patient_order_triage DROP COLUMN screening_session_id;
ALTER TABLE patient_order_triage DROP COLUMN patient_order_triage_override_reason_id;
ALTER TABLE patient_order_triage DROP COLUMN patient_order_triage_source_id;

ALTER TABLE patient_order_triage ALTER COLUMN patient_order_triage_group_id SET NOT NULL;

-- No longer need this, it's determined by screening session orchestration
ALTER TABLE patient_order_care_type DROP COLUMN severity;

-- This is legacy and can be removed
DELETE FROM patient_order_care_type WHERE patient_order_care_type_id='SAFETY_PLANNING';

-- Using patient_order_referral_reason and patient_order_referral instead now
DROP TABLE patient_order_reason_for_referral;

-- Status flags driven by IC intake questions
ALTER TABLE patient_order ADD COLUMN patient_order_intake_wants_services_status_id VARCHAR NOT NULL REFERENCES patient_order_intake_wants_services_status DEFAULT 'UNKNOWN';
ALTER TABLE patient_order ADD COLUMN patient_order_intake_location_status_id VARCHAR NOT NULL REFERENCES patient_order_intake_location_status DEFAULT 'UNKNOWN';
ALTER TABLE patient_order ADD COLUMN patient_order_intake_insurance_status_id VARCHAR NOT NULL REFERENCES patient_order_intake_insurance_status DEFAULT 'UNKNOWN';

-- Added "most recent intake"-related columns
-- Modified triage selection to join on patient_order_triage_group, removed window function
-- Modified referral reasons to pull from patient_order_referral/patient_order_referral_reason
CREATE or replace VIEW v_all_patient_order AS WITH
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
    -- Count up the scheduled message groups for each patient order
    select
        poq.patient_order_id,
        count(posmg.*) AS scheduled_message_group_count
    from
        patient_order_scheduled_message_group posmg,
        patient_order poq
    where
        poq.patient_order_id = posmg.patient_order_id
        AND posmg.deleted=FALSE
    group by
        poq.patient_order_id
),
smgmax_query AS (
    -- Pick the most-distant scheduled message group for each patient order
    select
        posmg.patient_order_id, MAX(posmg.scheduled_at_date_time) as max_scheduled_message_group_date_time
    from
        patient_order poq,
        patient_order_scheduled_message_group posmg
    where
        poq.patient_order_id = posmg.patient_order_id
        and posmg.deleted = false
    group by
        posmg.patient_order_id
),
recent_appt_query AS (
    -- Pick the most recent appointment for each patient order
    select
        app.patient_order_id,
        app.appointment_id,
        app.canceled,
        p.provider_id,
        p.name as provider_name,
        app.start_time as appointment_start_time,
        app.created_by_account_id
    from
        patient_order poq
        join appointment app ON poq.patient_order_id = app.patient_order_id
        join provider p ON app.provider_id  = p.provider_id
        left join appointment app2 ON app.patient_order_id = app2.patient_order_id
        and app2.canceled=false
        and app.start_time > app2.start_time
    where
        app2.appointment_id IS NULL
        and app.canceled=false
),
recent_voicemail_task_query AS (
    -- Pick the most recent voicemail task for each patient order
    select
        povt.patient_order_id,
        povt.patient_order_voicemail_task_id,
        povt.completed as patient_order_voicemail_task_completed
    from
        patient_order poq
        join patient_order_voicemail_task povt ON poq.patient_order_id = povt.patient_order_id
        left join patient_order_voicemail_task povt2 ON povt.patient_order_id = povt2.patient_order_id
        and povt.created > povt2.created
    where
        povt2.patient_order_voicemail_task_id IS NULL
),
ss_query AS (
    -- Pick the most recently-created clinical screening session for the patient order
    select
        ss.*,
        a.first_name,
        a.last_name,
        a.role_id
    from
        patient_order poq
        join screening_session ss ON poq.patient_order_id = ss.patient_order_id
        join account a ON ss.created_by_account_id = a.account_id
        join institution i ON a.institution_id = i.institution_id
        join screening_flow_version sfv ON ss.screening_flow_version_id = sfv.screening_flow_version_id
        left join screening_session ss2 ON ss.patient_order_id = ss2.patient_order_id
        and ss.created < ss2.created
        left join screening_flow_version sfv2 ON ss2.screening_flow_version_id = sfv2.screening_flow_version_id
        and i.integrated_care_screening_flow_id=sfv2.screening_flow_id
    where
        ss2.screening_session_id IS NULL
        and i.integrated_care_screening_flow_id=sfv.screening_flow_id
),
ss_intake_query AS (
    -- Pick the most recently-created intake screening session for the patient order
    select
        ss.*,
        a.first_name,
        a.last_name,
        a.role_id
    from
        patient_order poq
        join screening_session ss ON poq.patient_order_id = ss.patient_order_id
        join account a ON ss.created_by_account_id = a.account_id
        join institution i ON a.institution_id = i.institution_id
        join screening_flow_version sfv ON ss.screening_flow_version_id = sfv.screening_flow_version_id
        left join screening_session ss2 ON ss.patient_order_id = ss2.patient_order_id
        and ss.created < ss2.created
        left join screening_flow_version sfv2 ON ss2.screening_flow_version_id = sfv2.screening_flow_version_id
        and i.integrated_care_screening_flow_id=sfv2.screening_flow_id
    where
        ss2.screening_session_id IS NULL
        and i.integrated_care_intake_screening_flow_id=sfv.screening_flow_id
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
    select
        poss.*
    from
        patient_order poq
        join patient_order_scheduled_screening poss on poq.patient_order_id = poss.patient_order_id
        left join patient_order_scheduled_screening poss2 ON poss.patient_order_id = poss2.patient_order_id
        and poss.scheduled_date_time < poss2.scheduled_date_time
        and poss2.canceled = false
    where
        poss2.patient_order_scheduled_screening_id is NULL
        and poss.canceled = false
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
    coalesce(smgq.scheduled_message_group_count, 0) AS scheduled_message_group_count,
    smgmaxq.max_scheduled_message_group_date_time AS most_recent_scheduled_message_group_date_time,
    coalesce(pooq.outreach_count, 0) + coalesce(smgq.scheduled_message_group_count, 0) as total_outreach_count,
    GREATEST(poomaxq.max_outreach_date_time, smgmaxq.max_scheduled_message_group_date_time) AS most_recent_total_outreach_date_time,
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
      -- 1. Order is open
        poq.patient_order_disposition_id='OPEN'
        -- 2. Screening has not been started or scheduled
        -- Basically patient_order_screening_status_id='NOT_SCREENED' above
        AND (ssq.screening_session_id IS NULL AND rssq.scheduled_date_time IS NULL)
        -- 3. At least one outreach has been performed (either sent or scheduled)
        -- Basically total_outreach_count > 0 above
        AND (coalesce(pooq.outreach_count, 0) + coalesce(smgq.scheduled_message_group_count, 0)) > 0
        -- 4. The most recent outreach plus the institution day offset is on or after "now" (normalized for institution timezone)
        -- Basically most_recent_total_outreach_date_time above + [institution offset] >= NOW()
        AND (
            (GREATEST(poomaxq.max_outreach_date_time, smgmaxq.max_scheduled_message_group_date_time) + make_interval(days => i.integrated_care_outreach_followup_day_offset)) AT TIME ZONE i.time_zone <= NOW()
        )
    ) AS outreach_followup_needed,

    raq.appointment_start_time,
    raq.provider_id,
    raq.provider_name,
    raq.appointment_id,
    CASE
        WHEN appointment_id IS NOT NULL THEN true
        ELSE FALSE
    END appointment_scheduled,
    CASE
        WHEN raq.created_by_account_id = poq.patient_account_id THEN true
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
    poq.*
from
    patient_order poq
    left join patient_order_disposition pod ON poq.patient_order_disposition_id = pod.patient_order_disposition_id
    left join patient_order_closure_reason pocr ON poq.patient_order_closure_reason_id = pocr.patient_order_closure_reason_id
    left join institution i ON poq.institution_id = i.institution_id
    left join permitted_regions_query prq ON poq.institution_id = prq.institution_id
    left join patient_order_resource_check_in_response_status porcirs ON poq.patient_order_resource_check_in_response_status_id=porcirs.patient_order_resource_check_in_response_status_id
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
    left outer join recent_appt_query raq on poq.patient_order_id=raq.patient_order_id
    left outer join recent_voicemail_task_query rvtq on poq.patient_order_id=rvtq.patient_order_id
    left outer join reason_for_referral_query rfrq on poq.patient_order_id=rfrq.patient_order_id
    left outer join patient_order_scheduled_message_group posmg ON poq.resource_check_in_scheduled_message_group_id=posmg.patient_order_scheduled_message_group_id AND posmg.deleted = FALSE;

CREATE or replace VIEW v_patient_order AS
SELECT * FROM v_all_patient_order
where patient_order_disposition_id != 'ARCHIVED';


COMMIT;
