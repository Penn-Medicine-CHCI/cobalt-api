BEGIN;
SELECT _v.register_patch('142-content-admin', NULL, NULL);

CREATE TABLE content_status
(content_status_id VARCHAR NOT NULL PRIMARY KEY,
 description VARCHAR NOT NULL,
 display_order INTEGER NOT NULL);

INSERT INTO content_status
(content_status_id, description, display_order)
VALUES
('DRAFT', 'Draft',1),
('SCHEDULED', 'Scheduled', 2),
('LIVE', 'Live', 3),
('EXPIRED', 'Expired', 4);

ALTER TABLE content ADD COLUMN shared_flag BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE content ADD COLUMN search_terms VARCHAR NULL;
ALTER TABLE content ADD COLUMN publish_start_date timestamptz NULL;
ALTER TABLE content ADD COLUMN publish_end_date timestamptz NULL;
ALTER TABLE content ADD COLUMN publish_recurring BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE content ADD COLUMN published BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE content ADD COLUMN file_upload_id UUID NULL REFERENCES file_upload(file_upload_id);
ALTER TABLE content ADD COLUMN image_file_upload_id UUID NULL REFERENCES file_upload(file_upload_id);

UPDATE content SET published = TRUE where owner_institution_approval_status_id='APPROVED';
UPDATE content SET shared_flag = TRUE WHERE visibility_id = 'PUBLIC';
UPDATE content SET publish_start_date = date_created;
UPDATE content SET publish_start_date = created WHERE publish_start_date IS NULL;
ALTER TABLE content ALTER COLUMN publish_start_date SET NOT NULL;

UPDATE content SET date_created = created WHERE date_created IS NULL;
ALTER TABLE content ALTER COLUMN date_created SET NOT NULL;

DROP VIEW v_admin_content;

ALTER TABLE content DROP COLUMN archived_flag;
ALTER TABLE content DROP COLUMN owner_institution_approval_status_id;
ALTER TABLE content DROP COLUMN other_institution_approval_status_id;
ALTER TABLE content DROP COLUMN visibility_id;
ALTER TABLE content DROP COLUMN content_type_label_id;
ALTER TABLE content DROP COLUMN image_url;

DROP TABLE visibility;
DROP TABLE approval_status;
DROP TABLE available_status;
DROP TABLE institution_network;
DROP TABLE content_type_label;
DROP TABLE answer_content;

ALTER TABLE institution_content DROP COLUMN approved_flag;

CREATE OR REPLACE VIEW v_admin_content
AS SELECT c.content_id,
    c.content_type_id,
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
    fu2.url as image_url
   FROM content_type ct,
    institution i,
    content c 
    LEFT OUTER JOIN file_upload fu ON c.file_upload_id = fu.file_upload_id
    LEFT OUTER JOIN file_upload fu2 ON c.image_file_upload_id = fu2.file_upload_id
  WHERE c.content_type_id::text = ct.content_type_id::text 
  AND c.owner_institution_id::text = i.institution_id::text
  AND c.deleted_flag = false;

CREATE OR REPLACE VIEW v_institution_content
AS SELECT vac.*,
    it.institution_id
   FROM v_admin_content vac,
    institution_content it
  WHERE vac.content_id = it.content_id;

CREATE OR REPLACE FUNCTION content_en_search_vector_update() RETURNS TRIGGER AS $$
BEGIN
    NEW.en_search_vector = TO_TSVECTOR('pg_catalog.english', COALESCE(NEW.title, '') || ' ' || COALESCE(NEW.description, '') || ' ' || COALESCE(NEW.author, '') || ' ' || COALESCE(NEW.search_terms, ''));
    RETURN NEW;
END
$$ LANGUAGE 'plpgsql';

CREATE INDEX idx_content_file_upload_id ON content(file_upload_id);
CREATE INDEX idx_content_image_file_upload_id ON content(image_file_upload_id);

ALTER TABLE group_session ADD COLUMN image_file_upload_id UUID NULL REFERENCES file_upload;

DROP VIEW v_group_session;

-- Added image_file_upload_id and image_file_upload_url and remove image_url
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

  ALTER TABLE group_session DROP COLUMN image_url;

  --create a default file_upload image for Group Sessions
  INSERT INTO file_upload
    (file_upload_id, account_id, url, filename,
     storage_key, content_type)
  VALUES
    ('3e39722c-5ae9-4e35-a1f6-e0adc0df391c', (SELECT account_id FROM account WHERE email_address = 'admin@cobaltinnovations.org'), 
     'https://cobalt-shared-media.s3.amazonaws.com/group-sessions/default-group-session.jpg', 'default-group-session',
     'default-group-session', 'application/jpeg');

  DELETE FROM file_upload_type WHERE file_upload_type_id = 'IMAGE';

  INSERT INTO file_upload_type VALUES ('CONTENT_IMAGE', 'Content Image');
  INSERT INTO file_upload_type VALUES ('GROUP_SESSION_IMAGE', 'Group Session Image');


COMMIT;