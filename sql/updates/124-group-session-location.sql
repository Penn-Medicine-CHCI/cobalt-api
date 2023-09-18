BEGIN;
SELECT _v.register_patch('124-group-session-location', NULL, NULL);

CREATE TABLE group_session_location_type (
  group_session_location_type_id TEXT PRIMARY KEY,
  description VARCHAR NOT NULL
);

ALTER TABLE group_session ADD COLUMN group_session_location_type_id TEXT NOT NULL REFERENCES group_session_location_type DEFAULT 'VIRTUAL';

DROP VIEW v_group_session;

-- Added group_session_location_type_id
CREATE VIEW v_group_session AS
 SELECT gs.group_session_id,
    gs.institution_id,
    gs.group_session_status_id,
    gs.assessment_id,
    gs.group_session_location_type_id,
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
    gs.different_email_address_for_notifications
   FROM group_session gs
  WHERE gs.group_session_status_id::text <> 'DELETED'::text;

COMMIT;