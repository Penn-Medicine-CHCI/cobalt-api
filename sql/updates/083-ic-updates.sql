BEGIN;
SELECT _v.register_patch('083-ic-updates', NULL, NULL);

-- Optional field to store off SHA-256 checksum of raw order data to simplify duplicate import detection
ALTER TABLE patient_order_import ADD COLUMN raw_order_checksum VARCHAR;

-- These are whitelisted regions (i.e. states in the US) where patients must reside in order to use IC functionality.
-- Regions are implicitly located in the country indicated by the institution's locale
CREATE TABLE institution_integrated_care_region (
  institution_id VARCHAR NOT NULL REFERENCES institution,
  region_abbreviation VARCHAR NOT NULL, -- e.g. 'PA' for 'Pennsylvania'
  PRIMARY KEY (institution_id, region_abbreviation)
);

-- How many days to wait before "needs followup"
ALTER TABLE institution ADD COLUMN integrated_care_outreach_followup_day_offset INTEGER DEFAULT 4;

-- Keep track of "last_modified" separately from "last_updated".
-- * last_updated is only when the record itself is changed
-- * last_modified is when the record itself is changed OR if a related action is taken (e.g. outreach attempt created)
ALTER TABLE patient_order ADD COLUMN last_modified TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now();

CREATE FUNCTION set_last_modified() RETURNS TRIGGER AS $$
BEGIN
	NEW.last_modified := 'now';
	RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER set_last_modified BEFORE INSERT OR UPDATE ON patient_order FOR EACH ROW EXECUTE PROCEDURE set_last_modified();

CREATE TABLE patient_order_voicemail_task (
  patient_order_voicemail_task_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  patient_order_id UUID NOT NULL REFERENCES patient_order,
  account_id UUID NOT NULL REFERENCES account,
  message VARCHAR NOT NULL,
  completed BOOLEAN NOT NULL DEFAULT FALSE,
  created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON patient_order_voicemail_task FOR EACH ROW EXECUTE PROCEDURE set_last_updated();


-- Recreate view to add "last_modified" column and support voicemail tasks
DROP VIEW v_patient_order;
CREATE VIEW v_patient_order AS WITH po_query AS (
    select
        *
    from
        patient_order
    where
        patient_order_disposition_id != 'ARCHIVED'
),
poo_query AS (
    -- Count up the patient outreach attempts for each patient order
    select
        poq.patient_order_id,
        count(poo.*) AS outreach_count
    from
        patient_order_outreach poo,
        po_query poq
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
        po_query poq,
        patient_order_outreach poo
    where
        poq.patient_order_id = poo.patient_order_id
        and poo.deleted = false
    group by
        poo.patient_order_id
),
recent_appt_query AS (
    -- Pick the most recent appointment for each patient order
    select
        app.patient_order_id,
        app.appointment_id,
        p.provider_id,
        p.name as provider_name,
        app.start_time as appointment_start_time
    from
        po_query poq
        join appointment app ON poq.patient_order_id = app.patient_order_id
        join provider p ON app.provider_id  = p.provider_id
        left join appointment app2 ON app.patient_order_id = app2.patient_order_id
        and app.start_time > app2.start_time
    where
        app2.appointment_id  IS NULL
),
recent_voicemail_task_query AS (
    -- Pick the most recent voicemail task for each patient order
    select
        povt.patient_order_id,
        povt.patient_order_voicemail_task_id,
        povt.completed as patient_order_voicemail_task_completed
    from
        po_query poq
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
        po_query poq
        join screening_session ss ON poq.patient_order_id = ss.patient_order_id
        join account a ON ss.created_by_account_id = a.account_id
        left join screening_session ss2 ON ss.patient_order_id = ss2.patient_order_id
        and ss.created < ss2.created
    where
        ss2.screening_session_id IS NULL
),
recent_scheduled_screening_query AS (
    -- Pick the most recently-scheduled screening for the patient order
    select
        poss.*
    from
        po_query poq
        join patient_order_scheduled_screening poss on poq.patient_order_id = poss.patient_order_id
        left join patient_order_scheduled_screening poss2 ON poss.patient_order_id = poss2.patient_order_id
        and poss.scheduled_date_time < poss2.scheduled_date_time
        and poss.canceled = false
    where
        poss2.patient_order_scheduled_screening_id is NULL
), recent_po_query AS (
    -- Pick the most recently-closed patient order for the same MRN/institution combination
    -- TODO: this query needs to be fixed, should work similarly to recent_appt_query and others
    select
        poq.patient_order_id,
        po2.episode_closed_at AS most_recent_episode_closed_at
    from
        po_query poq
        left join patient_order po2 ON LOWER(poq.patient_mrn) = LOWER(po2.patient_mrn)
        and poq.institution_id = po2.institution_id
    where
        po2.episode_closed_at is not null
        and po2.created < poq.created
    order by
        po2.created desc
    limit
        1
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
            po_query poq,
            patient_order_triage pot,
            patient_order_care_type poct
        where
            poq.patient_order_id = pot.patient_order_id
            and pot.patient_order_care_type_id = poct.patient_order_care_type_id
            AND pot.patient_order_care_type_id != 'SAFETY_PLANNING'
            and pot.active = true
    )
    SELECT
        patient_order_care_type_id,
        patient_order_care_type_description,
        patient_order_id
    FROM
        poct_cte
    WHERE
        r = 1
) -- We need the DISTINCT here because patient outreach attempts with identical "most recent" times will cause duplicate rows
select
    distinct tq.patient_order_care_type_id,
    tq.patient_order_care_type_description,
    coalesce(pooq.outreach_count, 0) AS outreach_count,
    poomaxq.max_outreach_date_time AS most_recent_outreach_date_time,

    ssq.screening_session_id AS most_recent_screening_session_id,
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
        -- Unassigned
        WHEN poq.panel_account_id IS NULL THEN 'PENDING'
        -- Screening completed, most severe level of care type triage is SAFETY_PLANNING
        WHEN tq.patient_order_care_type_id = 'SAFETY_PLANNING' THEN 'SAFETY_PLANNING'
        -- Screening completed, most severe level of care type triage is SPECIALTY
        WHEN tq.patient_order_care_type_id = 'SPECIALTY' THEN 'SPECIALTY_CARE'
        -- Screening completed, most severe level of care type triage is SUBCLINICAL
        WHEN tq.patient_order_care_type_id = 'SUBCLINICAL' THEN 'SUBCLINICAL'
        -- Screening completed, most severe level of care type triage is COLLABORATIVE.  Patient or MHIC can schedule with a provider
        WHEN tq.patient_order_care_type_id = 'COLLABORATIVE' THEN 'BHP'
        -- Assigned, but none of the above apply.  Also note, we are in this state if unscheduled but "screening in progress"
        ELSE 'NEEDS_ASSESSMENT'
    END patient_order_status_id,
    CASE
        -- Unassigned
        WHEN poq.panel_account_id IS NULL THEN 'Pending'
        -- Screening completed, most severe level of care type triage is SAFETY_PLANNING
        WHEN tq.patient_order_care_type_id = 'SAFETY_PLANNING' THEN 'Safety Planning'
        -- Screening completed, most severe level of care type triage is SPECIALTY
        WHEN tq.patient_order_care_type_id = 'SPECIALTY' THEN 'Specialty Care'
        -- Screening completed, most severe level of care type triage is SUBCLINICAL
        WHEN tq.patient_order_care_type_id = 'SUBCLINICAL' THEN 'Subclinical'
        -- Screening completed, most severe level of care type triage is COLLABORATIVE.  Patient or MHIC can schedule with a provider
        WHEN tq.patient_order_care_type_id = 'COLLABORATIVE' THEN 'BHP'
        -- Assigned, but none of the above apply.  Also note, we are in this state if unscheduled but "screening in progress"
        else 'Needs Assessment'
    END patient_order_status_description,
    pocr.description AS patient_order_closure_reason_description,
    date_part(
        'year',
        age(poq.patient_birthdate AT TIME ZONE i.time_zone)
    ) :: INT < 18 AS patient_below_age_threshold,
    rpq.most_recent_episode_closed_at,
    date_part('day', NOW() - rpq.most_recent_episode_closed_at) :: INT < 30 AS most_recent_episode_closed_within_date_threshold,
    rssq.patient_order_scheduled_screening_id,
    rssq.scheduled_date_time AS patient_order_scheduled_screening_scheduled_date_time,
    rssq.calendar_url AS patient_order_scheduled_screening_calendar_url,
    raq.appointment_start_time,
    raq.provider_id,
    raq.provider_name,
    raq.appointment_id,
    rvtq.patient_order_voicemail_task_id AS most_recent_patient_order_voicemail_task_id,
    rvtq.patient_order_voicemail_task_completed AS most_recent_patient_order_voicemail_task_completed,
    poq.*
from
    po_query poq
    left join patient_order_disposition pod ON poq.patient_order_disposition_id = pod.patient_order_disposition_id
    left join patient_order_closure_reason pocr ON poq.patient_order_closure_reason_id = pocr.patient_order_closure_reason_id
    left join institution i ON poq.institution_id = i.institution_id
    left outer join poo_query pooq ON poq.patient_order_id = pooq.patient_order_id
    left outer join poomax_query poomaxq ON poq.patient_order_id = poomaxq.patient_order_id
    left outer join ss_query ssq ON poq.patient_order_id = ssq.patient_order_id
    left outer join triage_query tq ON poq.patient_order_id = tq.patient_order_id
    left outer join account panel_account ON poq.panel_account_id = panel_account.account_id
    left outer join recent_po_query rpq ON poq.patient_order_id = rpq.patient_order_id
    left outer join recent_scheduled_screening_query rssq ON poq.patient_order_id = rssq.patient_order_id
    left outer join recent_appt_query raq on poq.patient_order_id=raq.patient_order_id
    left outer join recent_voicemail_task_query rvtq on poq.patient_order_id=rvtq.patient_order_id;

COMMIT;