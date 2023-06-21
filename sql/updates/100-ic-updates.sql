BEGIN;
SELECT _v.register_patch('100-ic-updates', NULL, NULL);

-- To be shown in patient-facing error messages, potentially elsewhere
ALTER TABLE institution ADD COLUMN clinical_support_phone_number TEXT;

-- Set a fake number for example institutions
UPDATE institution SET clinical_support_phone_number='+12155551234' WHERE institution_id IN ('COBALT', 'COBALT_IC');

CREATE TABLE anonymous_account_expiration_strategy (
  anonymous_account_expiration_strategy_id TEXT PRIMARY KEY,
  description TEXT NOT NULL
);

-- Default is current behavior (cookie with institution-specified lifetime)
-- Single-session means "session cookie" - disappears once the user closes the browser, or phone app
INSERT INTO anonymous_account_expiration_strategy VALUES ('DEFAULT', 'Default');
INSERT INTO anonymous_account_expiration_strategy VALUES ('SINGLE_SESSION', 'Single-Session');

ALTER TABLE institution ADD COLUMN anonymous_account_expiration_strategy_id TEXT NOT NULL REFERENCES anonymous_account_expiration_strategy DEFAULT 'DEFAULT';

-- New option for Quartet
INSERT INTO patient_order_resourcing_type VALUES ('QUARTET_REFERRAL', 'Quartet Referral', 4);

-- Who gets contacts in the event of an IC crisis.
-- Currently we only place phone calls, this might change in the future
CREATE TABLE patient_order_crisis_handler (
  patient_order_crisis_handler_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  institution_id TEXT REFERENCES institution NOT NULL,
  phone_number TEXT NOT NULL,
  name TEXT NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT FALSE,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX patient_order_crisis_handler_institution_phone_number_idx ON patient_order_crisis_handler (institution_id, LOWER(TRIM(phone_number)));
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON patient_order_crisis_handler FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

-- Performance updates
DROP VIEW v_patient_order;

--Updating recent_po_query to remove LIMIT 1
--Removing DISTINCT from main SELECT
--Adding DISTINCT to triage_query
--Removing po_query and modifying all references to use patient_order directly
CREATE or replace VIEW v_patient_order AS WITH
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
        poq.patient_order_id, string_agg(porfr.reason_for_referral, ', ' order by porfr.display_order) AS reason_for_referral
    from
        patient_order poq,
        patient_order_reason_for_referral porfr
    where
        poq.patient_order_id = porfr.patient_order_id
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
        app.start_time as appointment_start_time
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
    -- Pick the most recently-created screening session for the patient order
    select
        ss.*,
        a.first_name,
        a.last_name
    from
        patient_order poq
        join screening_session ss ON poq.patient_order_id = ss.patient_order_id
        join account a ON ss.created_by_account_id = a.account_id
        left join screening_session ss2 ON ss.patient_order_id = ss2.patient_order_id
        and ss.created < ss2.created
    where
        ss2.screening_session_id IS NULL
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
    -- Pick the most recently-closed patient order for the same MRN/institution combination
    select
        poq.patient_order_id,
        MAX(po2.episode_closed_at) AS most_recent_episode_closed_at
    from
        patient_order poq
        left join patient_order po2 ON LOWER(poq.patient_mrn) = LOWER(po2.patient_mrn)
        and poq.institution_id = po2.institution_id
    group by poq.patient_order_id
), triage_query AS (
    -- Pick the most-severe triage for each patient order.
    -- Use a window function because it's easier to handle the join needed to order by severity.
    -- Ignore SAFETY_PLANNING because that's not a "real" triage destination (it's handled separately)
    WITH poct_cte AS (
        SELECT
            poq.patient_order_id,
            poct.patient_order_care_type_id,
            poct.description AS patient_order_care_type_description,
            pot.patient_order_triage_id,
            RANK() OVER (
                PARTITION BY poq.patient_order_id
                ORDER BY
                    poct.severity DESC
            ) AS r
        from
            patient_order poq,
            patient_order_triage pot,
            patient_order_care_type poct
        where
            poq.patient_order_id = pot.patient_order_id
            and pot.patient_order_care_type_id = poct.patient_order_care_type_id
            AND pot.patient_order_care_type_id != 'SAFETY_PLANNING'
            and pot.active = true
    )
    SELECT DISTINCT
        patient_order_care_type_id,
        patient_order_care_type_description,
        patient_order_id
    FROM
        poct_cte
    WHERE
        r = 1
)
select
    tq.patient_order_care_type_id,
    tq.patient_order_care_type_description,
    coalesce(pooq.outreach_count, 0) AS outreach_count,
    poomaxq.max_outreach_date_time AS most_recent_outreach_date_time,
    coalesce(smgq.scheduled_message_group_count, 0) AS scheduled_message_group_count,
    smgmaxq.max_scheduled_message_group_date_time AS most_recent_scheduled_message_group_date_time,
    coalesce(pooq.outreach_count, 0) + coalesce(smgq.scheduled_message_group_count, 0) as total_outreach_count,
    GREATEST(poomaxq.max_outreach_date_time, smgmaxq.max_scheduled_message_group_date_time) AS most_recent_total_outreach_date_time,
    ssq.screening_session_id AS most_recent_screening_session_id,
    ssq.created AS most_recent_screening_session_created_at,
    ssq.created_by_account_id AS most_recent_screening_session_created_by_account_id,
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
    panel_account.first_name AS panel_account_first_name,
    panel_account.last_name AS panel_account_last_name,
    pod.description AS patient_order_disposition_description,
    CASE
        -- Screening completed, most severe level of care type triage is SPECIALTY
        WHEN tq.patient_order_care_type_id = 'SPECIALTY' THEN 'SPECIALTY_CARE'
        -- Screening completed, most severe level of care type triage is SUBCLINICAL
        WHEN tq.patient_order_care_type_id = 'SUBCLINICAL' THEN 'SUBCLINICAL'
        -- Screening completed, most severe level of care type triage is COLLABORATIVE.  Patient or MHIC can schedule with a provider
        WHEN tq.patient_order_care_type_id = 'COLLABORATIVE' THEN 'MHP'
        -- None of the above apply
        ELSE 'NOT_TRIAGED'
    END patient_order_triage_status_id,
    CASE
        -- Screening completed, most severe level of care type triage is SPECIALTY
        WHEN tq.patient_order_care_type_id = 'SPECIALTY' THEN 'Specialty Care'
        -- Screening completed, most severe level of care type triage is SUBCLINICAL
        WHEN tq.patient_order_care_type_id = 'SUBCLINICAL' THEN 'Subclinical'
        -- Screening completed, most severe level of care type triage is COLLABORATIVE.  Patient or MHIC can schedule with a provider
        WHEN tq.patient_order_care_type_id = 'COLLABORATIVE' THEN 'MHP'
        -- None of the above apply
        else 'Not Triaged'
    END patient_order_triage_status_description,
    pocr.description AS patient_order_closure_reason_description,
    (date_part('year', poq.order_date) - date_part('year', poq.patient_birthdate)::INT) AS patient_age_on_order_date,
    (date_part('year', poq.order_date) - date_part('year', poq.patient_birthdate)::INT) < 18 AS patient_below_age_threshold,
    rpq.most_recent_episode_closed_at,
    date_part('day', NOW() - rpq.most_recent_episode_closed_at) :: INT < 30 AS most_recent_episode_closed_within_date_threshold,
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
    rvtq.patient_order_voicemail_task_id AS most_recent_patient_order_voicemail_task_id,
    rvtq.patient_order_voicemail_task_completed AS most_recent_patient_order_voicemail_task_completed,
      rfrq.reason_for_referral,
      poipl.patient_order_insurance_plan_type_id as patient_order_insurance_plan_type_id,
      poipl.name as patient_order_insurance_plan_name,
      poipl.accepted as patient_order_insurance_plan_accepted,
      poipa.patient_order_insurance_payor_id as patient_order_insurance_payor_id,
      poipa.patient_order_insurance_payor_type_id,
      poipa.name as patient_order_insurance_payor_name,
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
        and poipl.patient_order_insurance_plan_id is not NULL
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
        and poipl.accepted -- insurance plan
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
    poq.*
from
    patient_order poq
    left join patient_order_disposition pod ON poq.patient_order_disposition_id = pod.patient_order_disposition_id
    left join patient_order_closure_reason pocr ON poq.patient_order_closure_reason_id = pocr.patient_order_closure_reason_id
    left join institution i ON poq.institution_id = i.institution_id
    left join patient_order_insurance_plan poipl ON poq.patient_order_insurance_plan_id = poipl.patient_order_insurance_plan_id
    left join patient_order_insurance_payor poipa ON poipl.patient_order_insurance_payor_id = poipa.patient_order_insurance_payor_id
    left join permitted_regions_query prq ON poq.institution_id = prq.institution_id
    left join patient_order_resource_check_in_response_status porcirs ON poq.patient_order_resource_check_in_response_status_id=porcirs.patient_order_resource_check_in_response_status_id
    left outer join address patient_address ON poq.patient_address_id = patient_address.address_id
    left outer join poo_query pooq ON poq.patient_order_id = pooq.patient_order_id
    left outer join poomax_query poomaxq ON poq.patient_order_id = poomaxq.patient_order_id
    left outer join smg_query smgq ON poq.patient_order_id = smgq.patient_order_id
    left outer join smgmax_query smgmaxq ON poq.patient_order_id = smgmaxq.patient_order_id
    left outer join ss_query ssq ON poq.patient_order_id = ssq.patient_order_id
    left outer join triage_query tq ON poq.patient_order_id = tq.patient_order_id
    left outer join account panel_account ON poq.panel_account_id = panel_account.account_id
    left outer join recent_po_query rpq ON poq.patient_order_id = rpq.patient_order_id
    left outer join recent_scheduled_screening_query rssq ON poq.patient_order_id = rssq.patient_order_id
    left outer join recent_appt_query raq on poq.patient_order_id=raq.patient_order_id
    left outer join recent_voicemail_task_query rvtq on poq.patient_order_id=rvtq.patient_order_id
    left outer join reason_for_referral_query rfrq on poq.patient_order_id=rfrq.patient_order_id
    left outer join patient_order_scheduled_message_group posmg ON poq.resource_check_in_scheduled_message_group_id=posmg.patient_order_scheduled_message_group_id AND posmg.deleted = FALSE
where
    poq.patient_order_disposition_id != 'ARCHIVED';

COMMIT;