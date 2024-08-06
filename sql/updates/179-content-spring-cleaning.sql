BEGIN;
SELECT _v.register_patch('179-content-spring-cleaning', NULL, NULL);

-- Introduce WEBSITE content type
INSERT INTO content_type (content_type_id, description, call_to_action) VALUES ('WEBSITE', 'Website', 'Visit Website');

-- Introduce content visibility: public vs. unlisted
CREATE TABLE content_visibility_type (
	content_visibility_type_id TEXT PRIMARY KEY,
  description VARCHAR NOT NULL
);

INSERT INTO content_visibility_type (content_visibility_type_id, description) VALUES ('PUBLIC', 'Public');
INSERT INTO content_visibility_type (content_visibility_type_id, description) VALUES ('UNLISTED', 'Unlisted');

-- Retrofit content to respect visibility
ALTER TABLE content ADD COLUMN content_visibility_type_id TEXT NOT NULL REFERENCES content_visibility_type DEFAULT 'PUBLIC';

-- Due to introducing content_visibility_type_id on the content table, we need to expose it in v_admin_content.
-- So, we recreate v_admin_content.

-- Because v_institution_content depends on v_admin_content, we have to drop it first.
DROP VIEW v_institution_content;
DROP VIEW v_admin_content;

-- Introduce new content_visibility_type_id column
CREATE VIEW v_admin_content
AS SELECT c.content_id,
    c.content_type_id,
    c.content_visibility_type_id,
    c.title,
    c.url,
    c.publish_start_date,
    c.publish_end_date,
    c.publish_recurring,
    CASE WHEN c.published =  false THEN 'DRAFT'
    WHEN NOW() BETWEEN c.publish_start_date AND COALESCE(c.publish_end_date, NOW() + INTERVAL '1 DAY') THEN 'LIVE'
    WHEN COALESCE(c.publish_end_date, NOW() + INTERVAL '1 DAY') < NOW() THEN 'EXPIRED'
    WHEN c.publish_start_date > NOW() THEN 'SCHEDULED'
    END as content_status_id,
    CASE WHEN c.published =  false THEN 'Draft'
    WHEN NOW() BETWEEN c.publish_start_date AND c.publish_end_date THEN 'Live'
    WHEN c.publish_end_date < NOW() THEN 'Expired'
    WHEN c.publish_start_date > NOW() THEN 'Scheduled'
    END as content_status_description,
    c.shared_flag,
    c.search_terms,
    c.duration_in_minutes,
    c.description,
    c.author,
    c.created,
    c.last_updated,
    c.en_search_vector,
    ct.description AS content_type_description,
    ct.call_to_action,
    c.owner_institution_id,
    i.name AS owner_institution,
    c.date_created,
    c.file_upload_id,
    c.image_file_upload_id,
    fu.url as file_url,
    fu.filename,
    fu.content_type as file_content_type,
    fu2.url as image_url,
    fu.filesize
   FROM content_type ct,
    institution i,
    content c
    LEFT OUTER JOIN file_upload fu ON c.file_upload_id = fu.file_upload_id
    LEFT OUTER JOIN file_upload fu2 ON c.image_file_upload_id = fu2.file_upload_id
  WHERE c.content_type_id::text = ct.content_type_id::text
  AND c.owner_institution_id::text = i.institution_id::text
  AND c.deleted_flag = false;

-- Recreate v_institution_content now that we have recrated v_admin_content
CREATE VIEW v_institution_content
AS SELECT vac.*,
	it.institution_id,
	it.created AS institution_created_date
 FROM v_admin_content vac,
	institution_content it
WHERE vac.content_id = it.content_id;

-- Introduce content audience: who the content is for (myself, someone else)
CREATE TABLE content_audience_type (
	content_audience_type_id TEXT PRIMARY KEY,
  description VARCHAR NOT NULL,
  sentence_representation VARCHAR NOT NULL,
  display_order INTEGER NOT NULL
);

INSERT INTO content_audience_type (content_audience_type_id, description, sentence_representation, display_order) VALUES ('MYSELF', 'Myself', 'myself', 1);
INSERT INTO content_audience_type (content_audience_type_id, description, sentence_representation, display_order) VALUES ('SOMEONE_ELSE', 'Someone Else', 'someone else', 2);

-- Introduce content subject: who the content is about (myself, someone else)
CREATE TABLE content_subject_type (
	content_subject_type_id TEXT PRIMARY KEY,
  description VARCHAR NOT NULL,
  sentence_representation VARCHAR NOT NULL,
  display_order INTEGER NOT NULL
);

INSERT INTO content_subject_type (content_subject_type_id, description, sentence_representation, display_order) VALUES ('MYSELF', 'Myself', 'myself', 1);
INSERT INTO content_subject_type (content_subject_type_id, description, sentence_representation, display_order) VALUES ('SOMEONE_ELSE', 'Someone Else', 'someone else', 2);

-- Introduce content subject: who the content is about (myself, someone else)
CREATE TABLE content_age_type (
	content_age_type_id TEXT PRIMARY KEY,
  description VARCHAR NOT NULL,
  sentence_representation VARCHAR NOT NULL,
  display_order INTEGER NOT NULL
);

INSERT INTO content_age_type (content_age_type_id, description, sentence_representation, display_order) VALUES ('ADULT', 'Adult', 'adult', 1);
INSERT INTO content_age_type (content_age_type_id, description, sentence_representation, display_order) VALUES ('CHILD', 'Child', 'adult', 2);
INSERT INTO content_age_type (content_age_type_id, description, sentence_representation, display_order) VALUES ('TEEN_OR_YOUNG_ADULT', 'Teen or Young Adult', 'teen or young adult', 3);
INSERT INTO content_age_type (content_age_type_id, description, sentence_representation, display_order) VALUES ('SENIOR', 'Senior', 'senior', 4);

-- Combine content audience, subject, and age into a "content target group"
CREATE TABLE content_target_group (
	content_target_group_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	content_id UUID NOT NULL REFERENCES content,
	content_audience_type_id TEXT NOT NULL REFERENCES content_audience_type,
	content_subject_type_id TEXT NOT NULL REFERENCES content_subject_type,
	content_age_type_id TEXT NOT NULL REFERENCES content_age_type,
	created_by_account_id UUID NOT NULL REFERENCES account(account_id),
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON content_target_group FOR EACH ROW EXECUTE PROCEDURE set_last_updated();
CREATE UNIQUE INDEX content_target_group_unique_idx ON content_target_group USING btree (content_id, content_audience_type_id, content_subject_type_id, content_age_type_id);

-- Introduce group session visibility: public vs. unlisted
CREATE TABLE group_session_visibility_type (
	group_session_visibility_type_id TEXT PRIMARY KEY,
  description VARCHAR NOT NULL
);

INSERT INTO group_session_visibility_type (group_session_visibility_type_id, description) VALUES ('PUBLIC', 'Public');
INSERT INTO group_session_visibility_type (group_session_visibility_type_id, description) VALUES ('UNLISTED', 'Unlisted');

-- Retrofit content to respect visibility
ALTER TABLE group_session ADD COLUMN group_session_visibility_type_id TEXT NOT NULL REFERENCES group_session_visibility_type DEFAULT 'PUBLIC';

-- Introduce new group_session_visibility_type_id column
DROP VIEW v_group_session;
CREATE VIEW v_group_session AS
 SELECT gs.group_session_id,
    gs.institution_id,
    gs.group_session_status_id,
    gs.assessment_id,
    gs.group_session_location_type_id,
    gs.group_session_visibility_type_id,
    gs.in_person_location,
    gs.title,
    gs.description,
    gs.facilitator_account_id,
    gs.facilitator_name,
    gs.facilitator_email_address,
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
    gs.registration_end_date_time,
    gs.image_file_upload_id,
    fu.url as image_file_upload_url
   FROM group_session gs LEFT OUTER JOIN group_session_collection gsc on gs.group_session_collection_id=gsc.group_session_collection_id
   LEFT OUTER JOIN file_upload fu ON gs.image_file_upload_id = fu.file_upload_id
  WHERE gs.group_session_status_id::text <> 'DELETED'::text;

COMMIT;