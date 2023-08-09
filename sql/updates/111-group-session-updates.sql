BEGIN;
SELECT _v.register_patch('111-group-session-updates', NULL, NULL);

CREATE TABLE group_session_collection (
  group_session_collection_id UUID NOT NULL PRIMARY KEY,
  institution_id VARCHAR NOT NULL REFERENCES institution,
  description VARCHAR NOT NULL,
  display_order INTEGER NOT NULL,
  created timestamptz NOT NULL DEFAULT now(),
  last_updated timestamptz NOT NULL 
);

create trigger set_last_updated before
insert or update on group_session_collection for each row execute procedure set_last_updated();

CREATE TABLE group_session_learn_more_method (
  group_session_learn_more_method_id VARCHAR NOT NULL PRIMARY KEY,
  description VARCHAR NOT NULL,
  created timestamptz NOT NULL DEFAULT now(),
  last_updated timestamptz NOT NULL 
);

create trigger set_last_updated before
insert or update on group_session_learn_more_method for each row execute procedure set_last_updated();

INSERT INTO group_session_learn_more_method VALUES
('EMAIL', 'Email'),
('PHONE', 'Phone Number'),
('URL', 'URL');

ALTER TABLE group_session 
ADD COLUMN screening_flow_id UUID NULL REFERENCES screening_flow,
ADD COLUMN visible_flag BOOLEAN NOT NULL DEFAULT true,
ADD COLUMN group_session_collection_id UUID NULL REFERENCES group_session_collection,
ADD COLUMN followup_time_of_day TIME NULL,
ADD COLUMN followup_day_offset INTEGER NULL,
ADD COLUMN send_reminder_email BOOLEAN NOT NULL DEFAULT false,
ADD COLUMN reminder_email_content TEXT NULL,
ADD COLUMN single_session_flag BOOLEAN NOT NULL DEFAULT true,
ADD COLUMN date_time_description TEXT,
ADD COLUMN group_session_learn_more_method_id VARCHAR REFERENCES group_session_learn_more_method,
ADD COLUMN learn_more_description VARCHAR,
ALTER COLUMN start_date_time DROP NOT NULL,
ALTER COLUMN end_date_time DROP NOT NULL;

ALTER TABLE tag_group_session RENAME COLUMN tag_content_id TO tag_group_session_id;

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
    gs.submitter_name,
    gs.submitter_email_address,
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
	gs.date_time_description
   FROM group_session gs
  WHERE gs.group_session_status_id::text <> 'DELETED'::text;

COMMIT;

--Test

INSERT INTO group_session_collection values
(uuid_generate_v4(), 'COBALT', 'Test Collection 1', 1),
(uuid_generate_v4(), 'COBALT', 'Test Collection 2', 2);