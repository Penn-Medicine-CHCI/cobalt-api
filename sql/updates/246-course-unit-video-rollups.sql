BEGIN;
SELECT _v.register_patch('246-course-unit-video-rollups', NULL, NULL);

-- This patch introduces a 3-stage materialized-view pipeline for course-unit video playback analytics.
--
-- Why 3 stages instead of a single giant reporting query?
--
-- 1) EVENT_COURSE_UNIT_VIDEO is intentionally high-volume and noisy.
--    Many of those events are UI resize / metadata / visibility notifications that are useful for debugging
--    but are not directly meaningful for report output.
--
-- 2) The COURSE_MCB_DOWNLOAD report has stricter semantics for video fields than for generic unit dwell.
--    `*_video_time` wants actual watched playback time, not just "the page was open".
--    `*_video_complete` wants a "90% watched" style threshold.
--
-- 3) Rewinds and seeks require segment-level reasoning.
--    If a participant watches 0-30 seconds, jumps back to 20, and re-watches 20-50:
--      * cumulative watched time should be 60 seconds
--      * unique watched coverage should be 50 seconds
--    A simple MAX(percentComplete) or SUM(currentTime delta) does not capture that correctly.
--
-- The shape in this patch is:
--
--   analytics_native_event
--     -> mv_analytics_course_unit_video_event
--     -> mv_analytics_course_unit_video_segment
--     -> mv_analytics_course_unit_video_rollup
--
-- Refresh order matters and should always be:
--
--   REFRESH MATERIALIZED VIEW CONCURRENTLY mv_analytics_course_unit_video_event;
--   REFRESH MATERIALIZED VIEW CONCURRENTLY mv_analytics_course_unit_video_segment;
--   REFRESH MATERIALIZED VIEW CONCURRENTLY mv_analytics_course_unit_video_rollup;
--
-- The result is intentionally conservative:
-- * forward seeks do not count skipped content as watched
-- * backward seeks / rewinds do not lose the watch-time that occurred immediately before the seek
-- * if playback clock data is missing, we under-count instead of over-counting
--
-- The existing COURSE_MCB_DOWNLOAD report can keep using mv_analytics_dwell_time for:
-- * all non-video *_time columns
-- * *_visit columns, unless product later decides that "visit" should mean "play start"
--
-- The new video rollup can drive the video-specific columns as follows:
--
-- * *_video_time
--     SUM(cumulative_watched_seconds)
--     This counts actual watched playback time and includes rewatches.
--
-- * *_video_complete
--     CASE WHEN BOOL_OR(completed_90_percent) THEN 1 ELSE 0 END
--     This mirrors the product's current completion rule: if the player ever reports reaching the 90% mark,
--     the unit is considered complete even if the participant got there by scrubbing / seeking.
--
-- * *_video_visit
--     Continue using the existing page-view / dwell-based visit count for now.
--     If stakeholders later want "video starts" instead, play_start_event_count is exposed in the rollup.
--
-- Example join pattern for COURSE_MCB_DOWNLOAD:
--
--   WITH account_video_metrics AS (
--     SELECT
--       vr.account_id,
--       cu.reporting_key,
--       CASE WHEN BOOL_OR(vr.completed_90_percent) THEN 1 ELSE 0 END AS complete_value,
--       COALESCE(SUM(vr.cumulative_watched_seconds), 0)::DOUBLE PRECISION AS time_seconds
--     FROM report_accounts ra
--     JOIN mv_analytics_course_unit_video_rollup vr
--       ON vr.account_id = ra.account_id
--     JOIN course_unit cu
--       ON cu.course_unit_id = vr.course_unit_id
--     WHERE cu.course_unit_type_id = 'VIDEO'
--       AND NULLIF(REGEXP_REPLACE(cu.reporting_key, '\s+', '', 'g'), '') IS NOT NULL
--     GROUP BY vr.account_id, cu.reporting_key
--   )
--
-- Then merge account_video_metrics into the existing account_unit_metrics logic for VIDEO units only,
-- while leaving mv_analytics_dwell_time in place for non-video units and *_visit counts.

-- A partial index over the raw analytics table keeps materialized-view refreshes from repeatedly scanning
-- unrelated native event types.
CREATE INDEX analytics_native_event_course_unit_video_stream_idx
	ON analytics_native_event (
		institution_id,
		account_id,
		session_id,
		timestamp,
		analytics_native_event_id
	)
	WHERE analytics_native_event_type_id = 'EVENT_COURSE_UNIT_VIDEO';

-- Stage 1: normalize JSONB payloads from EVENT_COURSE_UNIT_VIDEO into typed columns.
--
-- Notes about the extracted columns:
--
-- * playback_asset_key
--     Stable per actual media asset being played. Prefer the Kaltura entry ID when present because
--     playlists can reuse the same course video wrapper while advancing through multiple actual assets.
--
-- * playback_stream_id
--     Stable per account/session/course-unit/course-session/playback-asset combination.
--     This gives downstream stages a single non-null grouping key and also serves as the rollup key.
--
-- * current_time_seconds / duration_seconds / percent_complete
--     These come from the player's eventPlaybackTime payload and are the core signal used by later stages.
--
-- * event_name classifications
--     We keep raw event_name plus some helper booleans for common categories. The later segment logic uses
--     both the timing deltas and the seek/play/pause classifications to build conservative watched intervals.
CREATE MATERIALIZED VIEW mv_analytics_course_unit_video_event AS
WITH typed_events AS (
	SELECT
		ane.analytics_native_event_id,
		ane.institution_id,
		ane.account_id,
		ane.session_id,
		ane.timestamp AS event_at,
		ane.timestamp_epoch_second,
		ane.timestamp_epoch_second_nano_offset,
		ane.webapp_url,
		ane.data AS raw_data,
		NULLIF(ane.data->>'courseUnitId', '')::UUID AS course_unit_id,
		NULLIF(ane.data->>'courseSessionId', '')::UUID AS course_session_id,
		NULLIF(ane.data->>'videoId', '')::UUID AS video_id,
		NULLIF(ane.data->>'currentlyPlayingVideoId', '')::UUID AS currently_playing_video_id,
		NULLIF(ane.data->>'currentlyPlayingKalturaEntryId', '') AS currently_playing_kaltura_entry_id,
		NULLIF(ane.data->'mediaProxy'->>'entryId', '') AS media_proxy_entry_id,
		LOWER(COALESCE(NULLIF(ane.data->>'eventName', ''), 'unknown')) AS event_name,
		COALESCE(ane.data->'eventPayload', '{}'::JSONB) AS event_payload,
		COALESCE(ane.data->'mediaProxy', '{}'::JSONB) AS media_proxy,
		COALESCE(ane.data->'eventPlaybackTime', '{}'::JSONB) AS event_playback_time,
		NULLIF(ane.data->'eventPlaybackTime'->>'currentTimeSeconds', '')::DOUBLE PRECISION AS current_time_seconds,
		NULLIF(ane.data->'eventPlaybackTime'->>'currentTimeFloorSeconds', '')::DOUBLE PRECISION AS current_time_floor_seconds,
		NULLIF(ane.data->'eventPlaybackTime'->>'durationSeconds', '')::DOUBLE PRECISION AS duration_seconds,
		NULLIF(ane.data->'eventPlaybackTime'->>'percentComplete', '')::DOUBLE PRECISION AS percent_complete,
		COALESCE(NULLIF(ane.data->'eventPlaybackTime'->>'playbackRate', '')::DOUBLE PRECISION, 1.0) AS playback_rate,
		NULLIF(ane.data->'eventPlaybackTime'->>'isPaused', '')::BOOLEAN AS is_paused
	FROM analytics_native_event ane
	WHERE ane.analytics_native_event_type_id = 'EVENT_COURSE_UNIT_VIDEO'
)
SELECT
	te.analytics_native_event_id,
	te.institution_id,
	te.account_id,
	te.session_id,
	te.event_at,
	te.timestamp_epoch_second,
	te.timestamp_epoch_second_nano_offset,
	te.webapp_url,
	te.raw_data,
	te.course_unit_id,
	te.course_session_id,
	te.video_id,
	te.currently_playing_video_id,
	te.currently_playing_kaltura_entry_id,
	te.media_proxy_entry_id,
	COALESCE(
		te.currently_playing_kaltura_entry_id,
		te.media_proxy_entry_id,
		te.currently_playing_video_id::TEXT,
		te.video_id::TEXT,
		te.analytics_native_event_id::TEXT
	) AS playback_asset_key,
	md5(CONCAT_WS('|',
		te.institution_id,
		COALESCE(te.account_id::TEXT, '[null-account]'),
		te.session_id::TEXT,
		COALESCE(te.course_unit_id::TEXT, '[null-course-unit]'),
		COALESCE(te.course_session_id::TEXT, '[null-course-session]'),
		COALESCE(
			te.currently_playing_kaltura_entry_id,
			te.media_proxy_entry_id,
			te.currently_playing_video_id::TEXT,
			te.video_id::TEXT,
			te.analytics_native_event_id::TEXT
		)
	)) AS playback_stream_id,
	te.event_name,
	te.event_payload,
	te.media_proxy,
	te.event_playback_time,
	te.current_time_seconds,
	te.current_time_floor_seconds,
	te.duration_seconds,
	te.percent_complete,
	te.playback_rate,
	te.is_paused,
	te.event_name IN (
		'play',
		'playing',
		'firstplay',
		'firstplaying',
		'playbackstart',
		'playkit-ui-userclickedplay'
	) AS is_play_event,
	te.event_name IN (
		'pause',
		'playkit-ui-userclickedpause'
	) AS is_pause_event,
	te.event_name IN (
		'seeking',
		'seeked',
		'playkit-ui-userseeked'
	) AS is_seek_event,
	te.event_name = 'initialization_error' AS is_initialization_error,
	te.event_name IN (
		'playkit-ui-guiresize',
		'playkit-ui-playerresize',
		'playkit-ui-videoresize',
		'playkit-ui-bottombarneedsresize',
		'playkit-ui-bottombarclientrectchanged',
		'playkit-ui-uivisibilitychanged',
		'playkit-ui-uiclicked',
		'visibilitychange',
		'resize'
	) AS is_ui_noise_event
FROM typed_events te;

CREATE UNIQUE INDEX mv_analytics_course_unit_video_event_pk_idx
	ON mv_analytics_course_unit_video_event (analytics_native_event_id);

CREATE INDEX mv_analytics_course_unit_video_event_stream_idx
	ON mv_analytics_course_unit_video_event (
		playback_stream_id,
		event_at,
		timestamp_epoch_second_nano_offset,
		analytics_native_event_id
	);

CREATE INDEX mv_analytics_course_unit_video_event_cu_idx
	ON mv_analytics_course_unit_video_event (
		course_unit_id,
		account_id,
		event_at
	);

-- Stage 2: build adjacent playback segments.
--
-- This stage is the heart of the normalization logic.
--
-- Each row represents the relationship between a normalized video event and the immediately preceding event in the
-- same playback stream. The derived columns explain how much of the elapsed wall-clock time can be safely treated
-- as actual watched playback.
--
-- Key heuristics:
--
-- * We partition by playback_stream_id, which keeps separate:
--     - different browser sessions
--     - different course units
--     - different actual media assets inside playlists
--
-- * "Contiguous playback" means the player clock advanced in a way that is consistent with wall time.
--   In that case, the watched interval is simply [previous_current_time_seconds, current_time_seconds].
--
-- * "Discontinuous jump" means seek / rewind / jump.
--   In those cases, current_time_seconds reflects the *post-seek* position, so using it directly would incorrectly
--   count skipped content as watched. Instead, we anchor watched time to the previous playback position and cap it by
--   elapsed wall-clock time times playback rate.
--
--   Example: previous clock = 16s, current clock = 63s, elapsed wall time = 2s
--            -> the user did not watch 47 seconds of content
--            -> we conservatively count roughly 2 seconds watched, starting from 16s
--
-- * Backward seek / rewind handling is the same idea in reverse:
--   if the clock moves backward, we still preserve any playback that happened before the seek by anchoring to the
--   previous position and capping by wall time. This lets cumulative watch time count rewatches correctly.
--
-- * We intentionally bias toward under-counting.
--   Missing playback clocks, large gaps, and paused gaps all resolve to zero watched seconds.
CREATE MATERIALIZED VIEW mv_analytics_course_unit_video_segment AS
WITH ordered_events AS (
	SELECT
		ve.*,
		ROW_NUMBER() OVER (
			PARTITION BY ve.playback_stream_id
			ORDER BY ve.event_at, ve.timestamp_epoch_second_nano_offset, ve.analytics_native_event_id
		) AS event_sequence,
		LAG(ve.analytics_native_event_id) OVER (
			PARTITION BY ve.playback_stream_id
			ORDER BY ve.event_at, ve.timestamp_epoch_second_nano_offset, ve.analytics_native_event_id
		) AS previous_analytics_native_event_id,
		LAG(ve.event_at) OVER (
			PARTITION BY ve.playback_stream_id
			ORDER BY ve.event_at, ve.timestamp_epoch_second_nano_offset, ve.analytics_native_event_id
		) AS previous_event_at,
		LAG(ve.event_name) OVER (
			PARTITION BY ve.playback_stream_id
			ORDER BY ve.event_at, ve.timestamp_epoch_second_nano_offset, ve.analytics_native_event_id
		) AS previous_event_name,
		LAG(ve.current_time_seconds) OVER (
			PARTITION BY ve.playback_stream_id
			ORDER BY ve.event_at, ve.timestamp_epoch_second_nano_offset, ve.analytics_native_event_id
		) AS previous_current_time_seconds,
		LAG(ve.duration_seconds) OVER (
			PARTITION BY ve.playback_stream_id
			ORDER BY ve.event_at, ve.timestamp_epoch_second_nano_offset, ve.analytics_native_event_id
		) AS previous_duration_seconds,
		LAG(ve.playback_rate) OVER (
			PARTITION BY ve.playback_stream_id
			ORDER BY ve.event_at, ve.timestamp_epoch_second_nano_offset, ve.analytics_native_event_id
		) AS previous_playback_rate,
		LAG(ve.is_paused) OVER (
			PARTITION BY ve.playback_stream_id
			ORDER BY ve.event_at, ve.timestamp_epoch_second_nano_offset, ve.analytics_native_event_id
		) AS previous_is_paused
	FROM mv_analytics_course_unit_video_event ve
),
derived_segments AS (
	SELECT
		oe.analytics_native_event_id AS segment_id,
		oe.playback_stream_id,
		oe.institution_id,
		oe.account_id,
		oe.session_id,
		oe.course_unit_id,
		oe.course_session_id,
		oe.video_id,
		oe.currently_playing_video_id,
		oe.playback_asset_key,
		oe.event_sequence,
		oe.previous_analytics_native_event_id,
		oe.previous_event_at,
		oe.event_at,
		oe.previous_event_name,
		oe.event_name,
		oe.previous_current_time_seconds,
		oe.current_time_seconds,
		oe.previous_duration_seconds,
		oe.duration_seconds,
		oe.percent_complete,
		oe.previous_playback_rate,
		oe.playback_rate,
		oe.previous_is_paused,
		oe.is_paused,
		oe.is_play_event,
		oe.is_pause_event,
		oe.is_seek_event,
		oe.is_initialization_error,
		EXTRACT(EPOCH FROM (oe.event_at - oe.previous_event_at))::DOUBLE PRECISION AS wall_seconds,
		(oe.current_time_seconds - oe.previous_current_time_seconds) AS raw_position_delta_seconds,
		CASE
			WHEN oe.previous_event_at IS NULL THEN NULL
			ELSE GREATEST(
				0,
				EXTRACT(EPOCH FROM (oe.event_at - oe.previous_event_at))::DOUBLE PRECISION
				* GREATEST(COALESCE(oe.previous_playback_rate, 1.0), 0)
			)
		END AS expected_position_delta_seconds,
		COALESCE(oe.previous_duration_seconds, oe.duration_seconds) AS effective_duration_seconds
	FROM ordered_events oe
),
classified_segments AS (
	SELECT
		ds.*,
		CASE
			WHEN ds.previous_event_at IS NULL THEN 'NO_PREVIOUS_EVENT'
			WHEN ds.previous_current_time_seconds IS NULL OR ds.current_time_seconds IS NULL THEN 'MISSING_PLAYBACK_POSITION'
			WHEN ds.wall_seconds IS NULL OR ds.wall_seconds <= 0 THEN 'NON_POSITIVE_WALL_TIME'
			WHEN ds.wall_seconds > 30 THEN 'SESSION_GAP'
			WHEN COALESCE(ds.previous_is_paused, FALSE) THEN 'PREVIOUS_EVENT_PAUSED'
			WHEN ds.is_initialization_error THEN 'INITIALIZATION_ERROR'
			WHEN ds.is_seek_event THEN 'SEEK_EVENT'
			WHEN ds.raw_position_delta_seconds < -0.5 THEN 'BACKWARD_SEEK_OR_REWIND'
			WHEN ds.raw_position_delta_seconds > COALESCE(ds.expected_position_delta_seconds, 0) + 5 THEN 'FORWARD_SEEK_OR_POSITION_JUMP'
			WHEN COALESCE(ds.is_paused, FALSE) AND COALESCE(ds.raw_position_delta_seconds, 0) = 0 THEN 'PAUSE_SNAPSHOT'
			ELSE 'CONTIGUOUS_PLAYBACK'
		END AS segment_classification
	FROM derived_segments ds
),
interval_segments AS (
	SELECT
		cs.*,
		CASE
			WHEN cs.segment_classification IN (
				'NO_PREVIOUS_EVENT',
				'MISSING_PLAYBACK_POSITION',
				'NON_POSITIVE_WALL_TIME',
				'SESSION_GAP',
				'PREVIOUS_EVENT_PAUSED',
				'INITIALIZATION_ERROR'
			) THEN NULL
			ELSE GREATEST(cs.previous_current_time_seconds, 0)
		END AS watched_interval_start_seconds,
		CASE
			WHEN cs.segment_classification IN (
				'NO_PREVIOUS_EVENT',
				'MISSING_PLAYBACK_POSITION',
				'NON_POSITIVE_WALL_TIME',
				'SESSION_GAP',
				'PREVIOUS_EVENT_PAUSED',
				'INITIALIZATION_ERROR'
			) THEN NULL
			WHEN cs.segment_classification IN (
				'SEEK_EVENT',
				'BACKWARD_SEEK_OR_REWIND',
				'FORWARD_SEEK_OR_POSITION_JUMP'
			) THEN LEAST(
				COALESCE(cs.effective_duration_seconds, cs.previous_current_time_seconds + COALESCE(cs.expected_position_delta_seconds, 0)),
				cs.previous_current_time_seconds + COALESCE(cs.expected_position_delta_seconds, 0)
			)
			WHEN cs.raw_position_delta_seconds <= 0 THEN cs.previous_current_time_seconds
			ELSE LEAST(
				COALESCE(cs.effective_duration_seconds, cs.current_time_seconds),
				cs.current_time_seconds
			)
		END AS watched_interval_end_seconds
	FROM classified_segments cs
)
SELECT
	isg.segment_id,
	isg.playback_stream_id,
	isg.institution_id,
	isg.account_id,
	isg.session_id,
	isg.course_unit_id,
	isg.course_session_id,
	isg.video_id,
	isg.currently_playing_video_id,
	isg.playback_asset_key,
	isg.event_sequence,
	isg.previous_analytics_native_event_id,
	isg.previous_event_at,
	isg.event_at,
	isg.previous_event_name,
	isg.event_name,
	isg.previous_current_time_seconds,
	isg.current_time_seconds,
	isg.previous_duration_seconds,
	isg.duration_seconds,
	isg.effective_duration_seconds,
	isg.percent_complete,
	isg.previous_playback_rate,
	isg.playback_rate,
	isg.previous_is_paused,
	isg.is_paused,
	isg.is_play_event,
	isg.is_pause_event,
	isg.is_seek_event,
	isg.is_initialization_error,
	isg.wall_seconds,
	isg.raw_position_delta_seconds,
	isg.expected_position_delta_seconds,
	isg.segment_classification,
	isg.watched_interval_start_seconds,
	CASE
		WHEN isg.watched_interval_start_seconds IS NULL OR isg.watched_interval_end_seconds IS NULL THEN NULL
		ELSE GREATEST(isg.watched_interval_end_seconds, isg.watched_interval_start_seconds)
	END AS watched_interval_end_seconds,
	CASE
		WHEN isg.watched_interval_start_seconds IS NULL OR isg.watched_interval_end_seconds IS NULL THEN 0
		ELSE GREATEST(isg.watched_interval_end_seconds - isg.watched_interval_start_seconds, 0)
	END AS watched_seconds_delta
FROM interval_segments isg;

CREATE UNIQUE INDEX mv_analytics_course_unit_video_segment_pk_idx
	ON mv_analytics_course_unit_video_segment (segment_id);

CREATE INDEX mv_analytics_course_unit_video_segment_stream_idx
	ON mv_analytics_course_unit_video_segment (
		playback_stream_id,
		event_sequence
	);

CREATE INDEX mv_analytics_course_unit_video_segment_cu_idx
	ON mv_analytics_course_unit_video_segment (
		course_unit_id,
		account_id,
		event_at
	);

-- Stage 3: roll up segments into stream-level playback metrics.
--
-- This view intentionally exposes both cumulative and unique watch metrics:
--
-- * cumulative_watched_seconds
--     Total playback time consumed, including rewatches after seeking backward.
--     This is the best fit for report fields described as "time watched".
--
-- * unique_watched_seconds
--     The distinct portion of the video timeline that was covered at least once.
--     This is useful for stricter analytics and QA because it cannot be faked by a large forward seek,
--     even though it is not the product's current completion definition.
--
-- * completed_90_percent
--     TRUE when the furthest reported player position reaches 90%.
--     This intentionally mirrors the product's current completion semantics.
--
-- * completed_90_percent_by_unique_coverage
--     Debugging / analysis aid. This is the stricter definition: distinct watched coverage reaches 90% of duration.
--     It is intentionally not the primary completion flag because the product currently allows insta-complete via seek.
--
-- The unique-interval merge uses a classic gaps-and-islands pattern. Positive watched intervals are ordered by
-- start/end, overlapping intervals are merged, and the merged span lengths are summed.
CREATE MATERIALIZED VIEW mv_analytics_course_unit_video_rollup AS
WITH stream_rollups AS (
	SELECT
		vs.playback_stream_id,
		MIN(COALESCE(vs.previous_event_at, vs.event_at)) AS first_event_at,
		MAX(vs.event_at) AS last_event_at,
		MIN(vs.institution_id) AS institution_id,
		MIN(vs.account_id) AS account_id,
		MIN(vs.session_id) AS session_id,
		MIN(vs.course_unit_id) AS course_unit_id,
		MIN(vs.course_session_id) AS course_session_id,
		MIN(vs.video_id) AS video_id,
		MIN(vs.currently_playing_video_id) AS currently_playing_video_id,
		MIN(vs.playback_asset_key) AS playback_asset_key,
		COUNT(*) AS event_count,
		COUNT(*) FILTER (
			WHERE vs.current_time_seconds IS NOT NULL
		) AS event_count_with_playback_clock,
		COUNT(*) FILTER (
			WHERE vs.event_name IN (
				'play',
				'firstplay',
				'playbackstart',
				'playkit-ui-userclickedplay'
			)
		) AS play_start_event_count,
		COUNT(*) FILTER (
			WHERE vs.is_pause_event
		) AS pause_event_count,
		COUNT(*) FILTER (
			WHERE vs.is_seek_event
		) AS seek_event_count,
		COUNT(*) FILTER (
			WHERE vs.segment_classification = 'BACKWARD_SEEK_OR_REWIND'
		) AS backward_seek_event_count,
		COUNT(*) FILTER (
			WHERE vs.segment_classification = 'FORWARD_SEEK_OR_POSITION_JUMP'
		) AS forward_seek_event_count,
		COUNT(*) FILTER (
			WHERE vs.segment_classification = 'SESSION_GAP'
		) AS long_gap_event_count,
		COUNT(*) FILTER (
			WHERE vs.is_initialization_error
		) AS initialization_error_count,
		MAX(vs.effective_duration_seconds) AS duration_seconds,
		MAX(vs.current_time_seconds) AS max_current_time_seconds,
		MAX(vs.percent_complete) AS max_percent_complete,
		COALESCE(SUM(vs.watched_seconds_delta), 0)::DOUBLE PRECISION AS cumulative_watched_seconds
	FROM mv_analytics_course_unit_video_segment vs
	GROUP BY vs.playback_stream_id
),
positive_intervals AS (
	SELECT
		vs.playback_stream_id,
		vs.watched_interval_start_seconds,
		vs.watched_interval_end_seconds
	FROM mv_analytics_course_unit_video_segment vs
	WHERE vs.watched_interval_start_seconds IS NOT NULL
		AND vs.watched_interval_end_seconds IS NOT NULL
		AND vs.watched_interval_end_seconds > vs.watched_interval_start_seconds
),
ordered_intervals AS (
	SELECT
		pi.*,
		MAX(pi.watched_interval_end_seconds) OVER (
			PARTITION BY pi.playback_stream_id
			ORDER BY pi.watched_interval_start_seconds, pi.watched_interval_end_seconds
			ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING
		) AS previous_interval_max_end_seconds
	FROM positive_intervals pi
),
interval_islands AS (
	SELECT
		oi.*,
		SUM(
			CASE
				WHEN oi.previous_interval_max_end_seconds IS NULL
					OR oi.watched_interval_start_seconds > oi.previous_interval_max_end_seconds
				THEN 1
				ELSE 0
			END
		) OVER (
			PARTITION BY oi.playback_stream_id
			ORDER BY oi.watched_interval_start_seconds, oi.watched_interval_end_seconds
		) AS interval_island_number
	FROM ordered_intervals oi
),
merged_intervals AS (
	SELECT
		ii.playback_stream_id,
		ii.interval_island_number,
		MIN(ii.watched_interval_start_seconds) AS unique_interval_start_seconds,
		MAX(ii.watched_interval_end_seconds) AS unique_interval_end_seconds
	FROM interval_islands ii
	GROUP BY ii.playback_stream_id, ii.interval_island_number
),
unique_interval_totals AS (
	SELECT
		mi.playback_stream_id,
		COALESCE(SUM(mi.unique_interval_end_seconds - mi.unique_interval_start_seconds), 0)::DOUBLE PRECISION AS unique_watched_seconds
	FROM merged_intervals mi
	GROUP BY mi.playback_stream_id
)
SELECT
	sr.playback_stream_id,
	sr.first_event_at,
	sr.last_event_at,
	sr.institution_id,
	sr.account_id,
	sr.session_id,
	sr.course_unit_id,
	sr.course_session_id,
	sr.video_id,
	sr.currently_playing_video_id,
	sr.playback_asset_key,
	sr.event_count,
	sr.event_count_with_playback_clock,
	sr.play_start_event_count,
	sr.pause_event_count,
	sr.seek_event_count,
	sr.backward_seek_event_count,
	sr.forward_seek_event_count,
	sr.long_gap_event_count,
	sr.initialization_error_count,
	sr.duration_seconds,
	sr.max_current_time_seconds,
	sr.max_percent_complete,
	sr.cumulative_watched_seconds,
	COALESCE(uit.unique_watched_seconds, 0)::DOUBLE PRECISION AS unique_watched_seconds,
	GREATEST(sr.cumulative_watched_seconds - COALESCE(uit.unique_watched_seconds, 0), 0)::DOUBLE PRECISION AS rewatched_seconds,
	CASE
		WHEN COALESCE(sr.duration_seconds, 0) <= 0 THEN NULL
		ELSE LEAST(100, (COALESCE(uit.unique_watched_seconds, 0) / sr.duration_seconds) * 100.0)
	END AS unique_percent_complete,
	CASE
		WHEN sr.max_percent_complete IS NULL THEN FALSE
		ELSE sr.max_percent_complete >= 90
	END AS completed_90_percent,
	CASE
		WHEN COALESCE(sr.duration_seconds, 0) <= 0 THEN FALSE
		ELSE COALESCE(uit.unique_watched_seconds, 0) >= sr.duration_seconds * 0.9
	END AS completed_90_percent_by_unique_coverage
FROM stream_rollups sr
LEFT JOIN unique_interval_totals uit
	ON uit.playback_stream_id = sr.playback_stream_id;

CREATE UNIQUE INDEX mv_analytics_course_unit_video_rollup_pk_idx
	ON mv_analytics_course_unit_video_rollup (playback_stream_id);

CREATE INDEX mv_analytics_course_unit_video_rollup_cu_idx
	ON mv_analytics_course_unit_video_rollup (
		course_unit_id,
		account_id,
		first_event_at
	);

CREATE INDEX mv_analytics_course_unit_video_rollup_account_idx
	ON mv_analytics_course_unit_video_rollup (
		account_id,
		first_event_at
	);

COMMIT;
