BEGIN;
SELECT _v.register_patch('031-group-session-request-updates', NULL, NULL);

ALTER TABLE institution ADD COLUMN recommend_group_session_requests BOOLEAN NOT NULL DEFAULT FALSE;

-- Turn this on for our example institution
UPDATE institution SET recommend_group_session_requests=TRUE WHERE institution_id = 'COBALT';

ALTER TABLE group_session_request ADD COLUMN data_collection_enabled BOOLEAN NOT NULL DEFAULT TRUE;

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
    gsr.en_search_vector,
    gsr.data_collection_enabled
   FROM group_session_request gsr
  WHERE gsr.group_session_request_status_id::text <> 'DELETED'::text;

COMMIT;