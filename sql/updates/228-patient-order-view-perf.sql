BEGIN;
SELECT _v.register_patch('228-patient-order-view-perf', NULL, NULL);

-- Includes rewrite of 'smg' for performance.
-- Previous definition is in 224-patient-order-view-perf.sql
CREATE OR REPLACE VIEW v_all_patient_order AS WITH
poo AS (
  SELECT
    patient_order_id,
    COUNT(*) AS outreach_count,
    MAX(outreach_date_time) AS max_outreach_date_time
  FROM
    patient_order_outreach
  WHERE
    deleted = FALSE
  GROUP BY
    patient_order_id
),
reason_for_referral_query AS (
    -- Pick reasons for referral for each patient order
    select
        por.patient_order_id,
        string_agg(porr.description, ', ' ORDER BY por.display_order) AS reason_for_referral
    from
        patient_order_referral por
    join
        patient_order_referral_reason porr ON por.patient_order_referral_reason_id = porr.patient_order_referral_reason_id
    group by
        por.patient_order_id
),
smg AS (
    SELECT
    posmg.patient_order_id,
    COUNT(*) AS scheduled_message_group_delivered_count,
    MAX(posmg.scheduled_at_date_time) AS max_delivered_scheduled_message_group_date_time
    FROM patient_order_scheduled_message_group posmg
    WHERE posmg.deleted = false
    AND EXISTS (
        SELECT 1
        FROM patient_order_scheduled_message posm
        JOIN scheduled_message sm
            ON sm.scheduled_message_id = posm.scheduled_message_id
        JOIN message_log ml
            ON ml.message_id = sm.message_id
            AND ml.message_status_id = 'DELIVERED'
        WHERE posm.patient_order_scheduled_message_group_id = posmg.patient_order_scheduled_message_group_id
        LIMIT 1
    )
    GROUP BY posmg.patient_order_id
),
next_resource_check_in_scheduled_message_group_query AS (
    -- Pick the next nondeleted scheduled message group in the future of type RESOURCE_CHECK_IN that has not yet been delivered
    select distinct on (posmg.patient_order_id)
        posmg.patient_order_id,
        posmg.patient_order_scheduled_message_group_id AS next_resource_check_in_scheduled_message_group_id,
        posmg.scheduled_at_date_time AS next_resource_check_in_scheduled_at_date_time
    from
        patient_order_scheduled_message_group posmg
    join
        patient_order po ON posmg.patient_order_id = po.patient_order_id
    join
        institution i ON po.institution_id = i.institution_id
    left join
        patient_order_scheduled_message posm ON posmg.patient_order_scheduled_message_group_id = posm.patient_order_scheduled_message_group_id
    left join
        scheduled_message sm ON posm.scheduled_message_id = sm.scheduled_message_id
    left join
        message_log ml ON sm.message_id = ml.message_id
    where
        posmg.patient_order_scheduled_message_type_id = 'RESOURCE_CHECK_IN'
        and posmg.deleted = FALSE
        and posmg.scheduled_at_date_time at time zone i.time_zone > now()
        and (ml.message_status_id IS NULL OR ml.message_status_id != 'DELIVERED')
    order by
        posmg.patient_order_id,
        posmg.scheduled_at_date_time,
        posmg.patient_order_scheduled_message_group_id
),
next_appt_query AS (
    select distinct on (app.patient_order_id)
        app.patient_order_id,
        app.appointment_id,
        app.canceled,
        p.provider_id,
        p.name AS provider_name,
        app.start_time AS appointment_start_time,
        app.created_by_account_id
    from
        appointment app
    join
        provider p ON app.provider_id = p.provider_id
    where
        app.canceled = false
    order by
        app.patient_order_id, app.start_time, app.appointment_id
),
recent_voicemail_task_query AS (
    -- Pick the most recent voicemail task for each patient order
    select distinct on (povt.patient_order_id)
        povt.patient_order_id,
        povt.patient_order_voicemail_task_id,
        povt.completed AS patient_order_voicemail_task_completed
    from
        patient_order_voicemail_task povt
    where
        povt.deleted = FALSE
    order by
        povt.patient_order_id, povt.created DESC, povt.patient_order_voicemail_task_id
),
next_scheduled_outreach_query AS (
    -- Pick the next active scheduled outreach for each patient order
    select distinct on (poso.patient_order_id)
        poso.patient_order_id,
        poso.patient_order_scheduled_outreach_id AS next_scheduled_outreach_id,
        poso.scheduled_at_date_time AS next_scheduled_outreach_scheduled_at_date_time,
        poso.patient_order_outreach_type_id AS next_scheduled_outreach_type_id,
        poso.patient_order_scheduled_outreach_reason_id AS next_scheduled_outreach_reason_id
    from
        patient_order_scheduled_outreach poso
    where
        poso.patient_order_scheduled_outreach_status_id = 'SCHEDULED'
    order by
        poso.patient_order_id, poso.scheduled_at_date_time, poso.patient_order_scheduled_outreach_id
),
most_recent_message_delivered_query AS (
    -- Pick the message that has been most recently delivered to the patient
    select distinct on (posmg.patient_order_id)
        posmg.patient_order_id,
        ml.delivered AS most_recent_message_delivered_at
    from
        patient_order_scheduled_message_group posmg
    join
        patient_order_scheduled_message posm ON posmg.patient_order_scheduled_message_group_id = posm.patient_order_scheduled_message_group_id
    join
        scheduled_message sm ON posm.scheduled_message_id = sm.scheduled_message_id
    join
        message_log ml ON sm.message_id = ml.message_id
    where
        ml.message_status_id = 'DELIVERED'
    order by
        posmg.patient_order_id, ml.delivered DESC
),
ss_query AS (
    -- Pick the most recently-created clinical screening session for the patient order
    select distinct on (ss.patient_order_id)
        ss.*,
        a.first_name,
        a.last_name,
        a.role_id
    from
        screening_session ss
    join
        screening_flow_version sfv ON ss.screening_flow_version_id = sfv.screening_flow_version_id
    join
        institution i ON sfv.screening_flow_id = i.integrated_care_screening_flow_id
    join
        account a ON ss.created_by_account_id = a.account_id
    where
        i.institution_id = a.institution_id
        and ss.skipped = false
    order by
        ss.patient_order_id, ss.created DESC
),
ss_intake_query AS (
    -- Pick the most recently-created intake screening session for the patient order
    select distinct on (ss.patient_order_id)
        ss.*,
        a.first_name,
        a.last_name,
        a.role_id
    from
        screening_session ss
    join
        screening_flow_version sfv ON ss.screening_flow_version_id = sfv.screening_flow_version_id
    join
        institution i ON sfv.screening_flow_id = i.integrated_care_intake_screening_flow_id
    join
        account a ON ss.created_by_account_id = a.account_id
    where
        i.institution_id = a.institution_id
        and ss.skipped = false
    order by
        ss.patient_order_id, ss.created DESC
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
    select distinct on (poss.patient_order_id)
        poss.*
    from
        patient_order_scheduled_screening poss
    where
        poss.canceled = FALSE
    order by
        poss.patient_order_id, poss.scheduled_date_time
), recent_po_query AS (
  -- For each patient order, look back at prior orders for the same patient & institution
  SELECT
    poq.patient_order_id,
    LAG(poq.episode_closed_at) OVER (
      PARTITION BY
        poq.patient_mrn,
        poq.institution_id
      ORDER BY
        poq.order_date
    ) AS most_recent_episode_closed_at
  FROM
    patient_order poq
)
select
    potg.patient_order_care_type_id,
    poct.description as patient_order_care_type_description,
    potg.patient_order_triage_source_id,
    coalesce(poo.outreach_count, 0) AS outreach_count,
    poo.max_outreach_date_time AS most_recent_outreach_date_time,
    coalesce(smg.scheduled_message_group_delivered_count, 0) AS scheduled_message_group_delivered_count,
    smg.max_delivered_scheduled_message_group_date_time AS most_recent_delivered_scheduled_message_group_date_time,
    coalesce(poo.outreach_count, 0) + coalesce(smg.scheduled_message_group_delivered_count, 0) as total_outreach_count,
    GREATEST(poo.max_outreach_date_time, smg.max_delivered_scheduled_message_group_date_time) AS most_recent_total_outreach_date_time,
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
        AND (coalesce(poo.outreach_count, 0) + coalesce(smg.scheduled_message_group_delivered_count, 0)) > 0
        -- 4. The most recent outreach plus the institution day offset is on or after "now" (normalized for institution timezone)
        -- Basically most_recent_total_outreach_date_time above + [institution offset] >= NOW()
        AND (
            (GREATEST(poo.max_outreach_date_time, smg.max_delivered_scheduled_message_group_date_time) + make_interval(days => i.integrated_care_outreach_followup_day_offset)) AT TIME ZONE i.time_zone <= NOW()
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
	      when poo.max_outreach_date_time AT TIME ZONE i.time_zone < now() then poo.max_outreach_date_time AT TIME ZONE i.time_zone
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
				WHEN nsoq.next_scheduled_outreach_scheduled_at_date_time = LEAST(
							 CASE WHEN ssq.screening_session_id IS NULL THEN rssq.scheduled_date_time ELSE '9999-12-31 23:59:59'::timestamptz END,
							 nsoq.next_scheduled_outreach_scheduled_at_date_time
						 )
						 AND nsoq.next_scheduled_outreach_reason_id = 'RESOURCE_FOLLOWUP'
				THEN 'RESOURCE_FOLLOWUP'
				WHEN nsoq.next_scheduled_outreach_scheduled_at_date_time = LEAST(
							 CASE WHEN ssq.screening_session_id IS NULL THEN rssq.scheduled_date_time ELSE '9999-12-31 23:59:59'::timestamptz END,
							 nsoq.next_scheduled_outreach_scheduled_at_date_time
						 )
						 AND nsoq.next_scheduled_outreach_reason_id = 'OTHER'
				THEN 'OTHER'
        when poo.max_outreach_date_time is null and smg.max_delivered_scheduled_message_group_date_time is null and ssiq.screening_session_id is null then 'WELCOME_MESSAGE'
        -- There has been some form of outreach but no screening session scheduled or started after X days
        WHEN ssq.screening_session_id is null and rssq.scheduled_date_time is null and (poo.max_outreach_date_time is not null or smg.max_delivered_scheduled_message_group_date_time is not null) and ((GREATEST(poo.max_outreach_date_time, smg.max_delivered_scheduled_message_group_date_time) + make_interval(days => i.integrated_care_outreach_followup_day_offset)) AT TIME ZONE i.time_zone <= NOW()) then 'ASSESSMENT_OUTREACH'
        -- Next resource check-in message
        when nrcismgq.next_resource_check_in_scheduled_message_group_id is not null then 'RESOURCE_CHECK_IN'
        -- TODO: RESOURCE_CHECK_IN_FOLLOWUP
        ELSE NULL
    END as next_contact_type_id,
	case
        WHEN ssq.screening_session_id is null and rssq.scheduled_date_time=LEAST(rssq.scheduled_date_time, nsoq.next_scheduled_outreach_scheduled_at_date_time) THEN rssq.scheduled_date_time
				-- Covers both RESOURCE_FOLLOWUP and OTHER
				WHEN nsoq.next_scheduled_outreach_scheduled_at_date_time = LEAST(
					 CASE
						 WHEN ssq.screening_session_id IS NULL THEN rssq.scheduled_date_time
						 ELSE '9999-12-31 23:59:59'::timestamptz
					 END,
					 nsoq.next_scheduled_outreach_scheduled_at_date_time
				)
				THEN nsoq.next_scheduled_outreach_scheduled_at_date_time
        when poo.max_outreach_date_time is null and smg.max_delivered_scheduled_message_group_date_time is null and ssiq.screening_session_id is null then null
        -- There has been some form of outreach but no screening session scheduled or started after X days
        WHEN ssq.screening_session_id is null and rssq.scheduled_date_time is null and (poo.max_outreach_date_time is not null or smg.max_delivered_scheduled_message_group_date_time is not null) and ((GREATEST(poo.max_outreach_date_time, smg.max_delivered_scheduled_message_group_date_time) + make_interval(days => i.integrated_care_outreach_followup_day_offset)) AT TIME ZONE i.time_zone <= NOW()) then NULL
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
    left outer join poo ON poq.patient_order_id = poo.patient_order_id
    left outer join smg ON poq.patient_order_id = smg.patient_order_id
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

COMMIT;