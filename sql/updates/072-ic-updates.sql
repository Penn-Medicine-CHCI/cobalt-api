BEGIN;
SELECT _v.register_patch('072-ic-updates', NULL, NULL);

DROP VIEW v_patient_order;

-- We'll use a new patient_order_safety_planning_status concept instead
ALTER TABLE patient_order DROP COLUMN crisis_indicated;
ALTER TABLE patient_order DROP COLUMN crisis_indicated_at;
ALTER TABLE patient_order DROP COLUMN safety_planning_needed;
ALTER TABLE patient_order DROP COLUMN resources_sent;

CREATE TABLE patient_order_safety_planning_status (
  patient_order_safety_planning_status_id VARCHAR PRIMARY KEY,
  description VARCHAR NOT NULL
);

INSERT INTO patient_order_safety_planning_status VALUES ('UNKNOWN', 'Unknown');
INSERT INTO patient_order_safety_planning_status VALUES ('NONE_NEEDED', 'None Needed');
INSERT INTO patient_order_safety_planning_status VALUES ('NEEDS_SAFETY_PLANNING', 'Needs Safety Planning');
INSERT INTO patient_order_safety_planning_status VALUES ('CONNECTED_TO_SAFETY_PLANNING', 'Connected To Safety Planning');

-- Safe to set 'UNKNOWN' default w/o migrating because all existing data is test data
ALTER TABLE patient_order ADD COLUMN patient_order_safety_planning_status_id VARCHAR NOT NULL REFERENCES patient_order_safety_planning_status DEFAULT 'UNKNOWN';
ALTER TABLE patient_order ADD COLUMN connected_to_safety_planning_at TIMESTAMPTZ;

CREATE TABLE patient_order_resourcing_status (
  patient_order_resourcing_status_id VARCHAR PRIMARY KEY,
  description VARCHAR NOT NULL
);

INSERT INTO patient_order_resourcing_status VALUES ('UNKNOWN', 'Unknown');
INSERT INTO patient_order_resourcing_status VALUES ('NONE_NEEDED', 'None Needed');
INSERT INTO patient_order_resourcing_status VALUES ('NEEDS_RESOURCES', 'Needs Resources');
INSERT INTO patient_order_resourcing_status VALUES ('SENT_RESOURCES', 'Sent Resources');

-- Safe to set 'UNKNOWN' default w/o migrating because all existing data is test data
ALTER TABLE patient_order ADD COLUMN patient_order_resourcing_status_id VARCHAR NOT NULL REFERENCES patient_order_resourcing_status DEFAULT 'UNKNOWN';
ALTER TABLE patient_order ADD COLUMN resources_sent_note VARCHAR;

-- How many weeks to wait between sending resources and sending a followup message (null for this and for integrated_care_sent_resources_followup_day_offset indicates never send followup)
ALTER TABLE institution ADD COLUMN integrated_care_sent_resources_followup_week_offset INTEGER DEFAULT 4;
ALTER TABLE institution ALTER COLUMN integrated_care_sent_resources_followup_day_offset SET DEFAULT 5;

DROP VIEW v_patient_order_scheduled_message;

-- To drive IC UI, conceptually we have _groups_ of scheduled messages (that is, a single logical message sent over e.g. SMS, email, ...)
-- So we modify existing model to support that via message groups
CREATE TABLE patient_order_scheduled_message_group (
  patient_order_scheduled_message_group_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  patient_order_id UUID NOT NULL REFERENCES patient_order,
  patient_order_scheduled_message_type_id VARCHAR NOT NULL REFERENCES patient_order_scheduled_message_type,
  created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON patient_order_scheduled_message_group FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

ALTER TABLE patient_order_scheduled_message DROP COLUMN patient_order_id;
ALTER TABLE patient_order_scheduled_message DROP COLUMN patient_order_scheduled_message_type_id;
ALTER TABLE patient_order_scheduled_message ADD COLUMN patient_order_scheduled_message_group_id UUID NOT NULL REFERENCES patient_order_scheduled_message_group;

-- Recreate view with latest columns
CREATE VIEW v_patient_order_scheduled_message AS
SELECT
  posmg.patient_order_scheduled_message_group_id,
  posmg.patient_order_id,
  posmg.patient_order_scheduled_message_type_id,
  posm.patient_order_scheduled_message_id,
  posm.scheduled_message_id,
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
  patient_order_scheduled_message_group posmg,
  scheduled_message sm,
  message_type mt
WHERE
  posm.scheduled_message_id=sm.scheduled_message_id
  AND posmg.patient_order_scheduled_message_type_id=posmt.patient_order_scheduled_message_type_id
  AND posmg.patient_order_scheduled_message_group_id=posm.patient_order_scheduled_message_group_id
  AND sm.message_type_id=mt.message_type_id;


-- Recreate view with latest columns

CREATE VIEW v_patient_order AS WITH po_query AS (
    select
        *
    from
        patient_order --  where patient_order_disposition_id != 'ARCHIVED'
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
    group by
        poq.patient_order_id
),
poomax_query AS (
    -- Pick the most recent patient outreach attempt for each patient order
    select
        poo.*
    from
        po_query poq
        join patient_order_outreach poo ON poq.patient_order_id = poo.patient_order_id
        left join patient_order_outreach poo2 ON poo.patient_order_id = poo2.patient_order_id
        and poo.outreach_date_time < poo2.outreach_date_time
    where
        poo2.patient_order_outreach_id IS NULL
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
        po_query poq,
        patient_order_scheduled_screening poss
    where
        poq.patient_order_id = poss.patient_order_id
        and poss.canceled = false
    order by
        poss.scheduled_date_time
    limit
        1
), recent_po_query AS (
    -- Pick the most recently-closed patient order for the same MRN/institution combination
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
    poomaxq.outreach_date_time AS most_recent_outreach_date_time,
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
        WHEN poq.panel_account_id IS NULL THEN 'PENDING' -- Screening completed, most severe level of care type triage is SAFETY_PLANNING
        WHEN tq.patient_order_care_type_id = 'SAFETY_PLANNING' THEN 'SAFETY_PLANNING' -- Screening completed, most severe level of care type triage is SPECIALTY
        WHEN tq.patient_order_care_type_id = 'SPECIALTY_CARE' THEN 'SPECIALTY_CARE' -- Screening completed, most severe level of care type triage is SUBCLINICAL
        WHEN tq.patient_order_care_type_id = 'SUBCLINICAL' THEN 'SUBCLINICAL' -- Screening completed, most severe level of care type triage is COLLABORATIVE.  Patient or MHIC can schedule with a provider
        WHEN tq.patient_order_care_type_id = 'COLLABORATIVE' THEN 'BHP' --  Unscreened, but has a call scheduled with an MHIC to take the screening (uncanceled patient_order_scheduled_screening)
        WHEN rssq.scheduled_date_time is not null THEN 'SCHEDULED' -- Assigned, but none of the above apply.  Also note, we are in this state if unscheduled but "screening in progress"
        ELSE 'NEEDS_ASSESSMENT'
    END patient_order_status_id,
    CASE
        -- Unassigned
        WHEN poq.panel_account_id IS NULL THEN 'Pending' -- Screening completed, most severe level of care type triage is SAFETY_PLANNING
        WHEN tq.patient_order_care_type_id = 'SAFETY_PLANNING' THEN 'Safety Planning' -- Screening completed, most severe level of care type triage is SPECIALTY
        WHEN tq.patient_order_care_type_id = 'SPECIALTY_CARE' THEN 'Specialty Care' -- Screening completed, most severe level of care type triage is SUBCLINICAL
        WHEN tq.patient_order_care_type_id = 'SUBCLINICAL' THEN 'Subclinical' -- Screening completed, most severe level of care type triage is COLLABORATIVE.  Patient or MHIC can schedule with a provider
        WHEN tq.patient_order_care_type_id = 'COLLABORATIVE' THEN 'BHP' --  Unscreened, but has a call scheduled with an MHIC to take the screening (uncanceled patient_order_scheduled_screening)
        WHEN rssq.scheduled_date_time is not null THEN 'Scheduled' -- Assigned, but none of the above apply.  Also note, we are in this state if unscheduled but "screening in progress"
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
    left outer join recent_scheduled_screening_query rssq ON poq.patient_order_id = rssq.patient_order_id;

COMMIT;