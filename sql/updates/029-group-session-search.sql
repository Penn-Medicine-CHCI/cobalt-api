BEGIN;
SELECT _v.register_patch('029-group-session-search', NULL, NULL);

ALTER TABLE group_session ADD COLUMN en_search_vector TSVECTOR GENERATED ALWAYS AS (to_tsvector('english', coalesce(title, '') || ' ' || coalesce(description, '')|| ' ' || coalesce(url_name, '')|| ' ' || coalesce(facilitator_name, ''))) STORED;
CREATE INDEX group_session_en_search_vector_idx ON group_session USING GIN (en_search_vector);

ALTER TABLE group_session_request ADD COLUMN en_search_vector TSVECTOR GENERATED ALWAYS AS (to_tsvector('english', coalesce(title, '') || ' ' || coalesce(description, '')|| ' ' || coalesce(url_name, '')|| ' ' || coalesce(facilitator_name, ''))) STORED;
CREATE INDEX group_session_request_en_search_vector_idx ON group_session_request USING GIN (en_search_vector);

DROP VIEW v_group_session;

CREATE VIEW v_group_session AS
 SELECT gs.group_session_id,
    gs.institution_id,
    gs.group_session_status_id,
    gs.assessment_id,
    gs.title,
    gs.description,
    gs.facilitator_account_id,
    gs.facilitator_name,
    gs.facilitator_email_address,
    gs.image_url,
    gs.videoconference_url,
    gs.start_date_time,
    gs.end_date_time,
    gs.seats,
    gs.url_name,
    gs.confirmation_email_content,
    gs.locale,
    gs.time_zone,
    gs.created,
    gs.last_updated,
    gs.group_session_scheduling_system_id,
    gs.schedule_url,
    gs.send_followup_email,
    gs.followup_email_content,
    gs.followup_email_survey_url,
    gs.submitter_account_id,
    gs.en_search_vector,
    ( SELECT count(*) AS count
           FROM group_session_reservation gsr
          WHERE gsr.group_session_id = gs.group_session_id AND gsr.canceled = false) AS seats_reserved,
    gs.seats - (( SELECT count(*) AS count
           FROM group_session_reservation gsr
          WHERE gsr.group_session_id = gs.group_session_id AND gsr.canceled = false)) AS seats_available
   FROM group_session gs
  WHERE gs.group_session_status_id::text <> 'DELETED'::text;


DROP VIEW v_group_session_request;

CREATE VIEW v_group_session_request AS
 SELECT gsr.group_session_request_id,
    gsr.institution_id,
    gsr.group_session_request_status_id,
    gsr.title,
    gsr.description,
    gsr.facilitator_account_id,
    gsr.facilitator_name,
    gsr.facilitator_email_address,
    gsr.image_url,
    gsr.url_name,
    gsr.custom_question_1,
    gsr.custom_question_2,
    gsr.created,
    gsr.last_updated,
    gsr.submitter_account_id,
    gsr.en_search_vector
   FROM group_session_request gsr
  WHERE gsr.group_session_request_status_id::text <> 'DELETED'::text;


COMMIT;