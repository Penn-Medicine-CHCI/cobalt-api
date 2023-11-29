BEGIN;
SELECT _v.register_patch('136-late-november', NULL, NULL);

INSERT INTO screening_type (screening_type_id, description, overall_score_maximum) VALUES ('UCLA_LONELINESS', 'UCLA 3-Item Loneliness', 9);

ALTER TABLE group_session ADD COLUMN registration_end_date_time TIMESTAMP WITHOUT TIME ZONE;

DROP VIEW v_group_session;

-- Added registration_end_date_time
CREATE VIEW v_group_session AS
 SELECT gs.group_session_id,
    gs.institution_id,
    gs.group_session_status_id,
    gs.assessment_id,
    gs.group_session_location_type_id,
    gs.in_person_location,
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
    gs.send_followup_email,
    gs.followup_email_content,
    gs.followup_email_survey_url,
    gs.submitter_account_id,
    gs.target_email_address,
    gs.en_search_vector,
    ( SELECT count(*) AS count
           FROM group_session_reservation gsr
          WHERE gsr.group_session_id = gs.group_session_id AND gsr.canceled = false) AS seats_reserved,
    gs.seats - (( SELECT count(*) AS count
           FROM group_session_reservation gsr
          WHERE gsr.group_session_id = gs.group_session_id AND gsr.canceled = false)) AS seats_available,
    gs.screening_flow_id,
	gs.visible_flag,
	gs.group_session_collection_id,
	gs.followup_time_of_day,
	gs.followup_day_offset,
	gs.send_reminder_email,
	gs.reminder_email_content,
	gs.single_session_flag,
	gs.date_time_description,
    gs.group_session_learn_more_method_id,
    gs.learn_more_description,
    gs.different_email_address_for_notifications,
    gs.override_platform_name,
    gs.override_platform_email_image_url,
    gs.override_platform_support_email_address,
    gsc.url_name AS group_session_collection_url_name,
    gs.registration_end_date_time
   FROM group_session gs LEFT OUTER JOIN group_session_collection gsc on gs.group_session_collection_id=gsc.group_session_collection_id
  WHERE gs.group_session_status_id::text <> 'DELETED'::text;

ALTER TABLE provider ADD COLUMN url_name TEXT;

-- Populate URL names for providers with a variant of their name, e.g. "Test Provider" becomes 'test-provider'
UPDATE provider SET url_name = LOWER(REGEXP_REPLACE(REPLACE(name, ' ', '-'), '[^a-zA-Z\-]+', '', 'g'));

CREATE UNIQUE INDEX provider_unique_url_name_idx ON provider USING btree (institution_id, url_name);

ALTER TABLE provider ALTER COLUMN url_name SET NOT NULL;

COMMIT;