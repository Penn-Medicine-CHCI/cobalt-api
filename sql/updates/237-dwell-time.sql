BEGIN;
SELECT _v.register_patch('237-dwell-time', NULL, NULL);

-- Dwell time is defined as how many heartbeats elapse between PAGE_VIEW events (each heartbeat is 5 seconds currently, so this is multiples of 5).
-- For PAGE_VIEW events with no intervening heartbeats (that is, someone visited a page and then quickly went to another w/o heartbeat), we assume 2.5 seconds instead of 0.
CREATE MATERIALIZED VIEW mv_analytics_dwell_time AS
WITH relevant_events AS (
  SELECT
    institution_id,
    account_id,
    session_id,
    timestamp,
    analytics_native_event_type_id,
    data
  FROM analytics_native_event
  WHERE
    (
      analytics_native_event_type_id = 'HEARTBEAT'
      OR analytics_native_event_type_id LIKE 'PAGE_VIEW_%'
    )
),
dwells AS (
  SELECT
    institution_id,
    account_id,
    session_id,
    timestamp,
    analytics_native_event_type_id,
    data,
    SUM(
      CASE
        WHEN analytics_native_event_type_id LIKE 'PAGE_VIEW_%' THEN 1
        ELSE 0
      END
    ) OVER (
      PARTITION BY account_id, session_id
      ORDER BY timestamp
    ) AS dwell_num
  FROM relevant_events
),
page_view_info AS (
  SELECT
    institution_id,
    account_id,
    session_id,
    dwell_num,
    timestamp                  AS page_viewed_at,
    analytics_native_event_type_id AS page_view_type,
    data                       AS page_view_data
  FROM dwells
  WHERE analytics_native_event_type_id LIKE 'PAGE_VIEW_%'
),
heartbeat_counts AS (
  SELECT
    account_id,
    session_id,
    dwell_num,
    COUNT(*) * 5              AS dwell_time_seconds
  FROM dwells
  WHERE analytics_native_event_type_id = 'HEARTBEAT'
  GROUP BY account_id, session_id, dwell_num
)
SELECT
  pv.institution_id,
  pv.account_id,
  pv.session_id,
  pv.dwell_num,
  pv.page_viewed_at,
  pv.page_view_type,
  COALESCE(hc.dwell_time_seconds, 2.5) AS dwell_time_seconds,
  pv.page_view_data,
  -- Extract course_unit_id for more efficient joins
  (pv.page_view_data->>'courseUnitId')::uuid AS course_unit_id
FROM page_view_info pv
LEFT JOIN heartbeat_counts hc
  ON hc.account_id  = pv.account_id
 AND hc.session_id  = pv.session_id
 AND hc.dwell_num   = pv.dwell_num;

-- Unique index enables REFRESH MATERIALIZED VIEW CONCURRENTLY mv_analytics_dwell_time
CREATE UNIQUE INDEX mv_analytics_dwell_time_pk_idx
ON mv_analytics_dwell_time (institution_id, account_id, session_id, dwell_num);

CREATE INDEX mv_analytics_dwell_time_inst_ts_idx ON mv_analytics_dwell_time (institution_id, page_viewed_at);

-- Special index for efficient queries over course unit dwell time
CREATE INDEX mv_analytics_dwell_time_cu_idx
ON mv_analytics_dwell_time (
  course_unit_id,
  institution_id,
  page_viewed_at
)
WHERE page_view_type = 'PAGE_VIEW_COURSE_UNIT';

ALTER TABLE course_session
  ADD COLUMN completed_at timestamp with time zone NULL;

UPDATE course_session
SET completed_at = last_updated
WHERE course_session_status_id = 'COMPLETED';

CREATE OR REPLACE FUNCTION course_session_set_completed_at()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  IF NEW.course_session_status_id = 'COMPLETED' THEN
    IF TG_OP = 'INSERT' THEN
      -- On insert into COMPLETED, set completed_at if not provided
      IF NEW.completed_at IS NULL THEN
        NEW.completed_at := now();
      END IF;
    ELSIF TG_OP = 'UPDATE' THEN
      -- Transition into COMPLETED: set completed_at to now
      IF OLD.course_session_status_id <> 'COMPLETED' THEN
        NEW.completed_at := now();
      ELSIF NEW.completed_at IS NULL THEN
        -- Still COMPLETED but somehow completed_at is null: fix it
        NEW.completed_at := now();
      END IF;
    END IF;
  ELSE
    -- Any non-COMPLETED status: ensure completed_at is null
    NEW.completed_at := NULL;
  END IF;

  RETURN NEW;
END;
$$;

CREATE TRIGGER trg_course_session_set_completed_at
BEFORE INSERT OR UPDATE OF course_session_status_id
ON course_session
FOR EACH ROW
EXECUTE FUNCTION course_session_set_completed_at();

ALTER TABLE course_session
  ADD CONSTRAINT course_session_completed_at_consistency_chk
  CHECK (
    (course_session_status_id = 'COMPLETED' AND completed_at IS NOT NULL)
    OR
    (course_session_status_id <> 'COMPLETED' AND completed_at IS NULL)
  );


ALTER TABLE course_session_unit
  ADD COLUMN completed_at timestamp with time zone NULL;

UPDATE course_session_unit
SET completed_at = last_updated
WHERE course_session_unit_status_id = 'COMPLETED';
  

CREATE OR REPLACE FUNCTION course_session_unit_set_completed_at()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  IF NEW.course_session_unit_status_id = 'COMPLETED' THEN
    IF TG_OP = 'INSERT' THEN
      -- On insert into COMPLETED, set completed_at if not provided
      IF NEW.completed_at IS NULL THEN
        NEW.completed_at := now();
      END IF;
    ELSIF TG_OP = 'UPDATE' THEN
      -- Transition into COMPLETED: set completed_at to now
      IF OLD.course_session_unit_status_id <> 'COMPLETED' THEN
        NEW.completed_at := now();
      ELSIF NEW.completed_at IS NULL THEN
        -- Still COMPLETED but somehow completed_at is null: fix it
        NEW.completed_at := now();
      END IF;
    END IF;
  ELSE
    -- Any non-COMPLETED status: ensure completed_at is null
    NEW.completed_at := NULL;
  END IF;

  RETURN NEW;
END;
$$;


CREATE TRIGGER trg_course_session_unit_set_completed_at
BEFORE INSERT OR UPDATE OF course_session_unit_status_id
ON course_session_unit
FOR EACH ROW
EXECUTE FUNCTION course_session_unit_set_completed_at();

ALTER TABLE course_session_unit
  ADD CONSTRAINT course_session_unit_completed_at_consistency_chk
  CHECK (
    (course_session_unit_status_id = 'COMPLETED' AND completed_at IS NOT NULL)
    OR
    (course_session_unit_status_id <> 'COMPLETED' AND completed_at IS NULL)
  );

COMMIT;