BEGIN;
SELECT _v.register_patch('187-content-spring-cleaning', NULL, NULL);

-- Footprint tracking
CREATE TABLE footprint_event_group_type (
  footprint_event_group_type_id VARCHAR PRIMARY KEY,
  description VARCHAR NOT NULL
);

INSERT INTO footprint_event_group_type VALUES ('UNSPECIFIED', 'Unspecified');
-- Content
INSERT INTO footprint_event_group_type VALUES ('CONTENT_CREATE', 'Content Create');
INSERT INTO footprint_event_group_type VALUES ('CONTENT_UPDATE', 'Content Update');
INSERT INTO footprint_event_group_type VALUES ('CONTENT_DELETE', 'Content Delete');
INSERT INTO footprint_event_group_type VALUES ('CONTENT_PUBLISH', 'Content Publish');
-- Group Sessions
INSERT INTO footprint_event_group_type VALUES ('GROUP_SESSION_CREATE', 'Group Session Create');
INSERT INTO footprint_event_group_type VALUES ('GROUP_SESSION_UPDATE', 'Group Session Update');
INSERT INTO footprint_event_group_type VALUES ('GROUP_SESSION_UPDATE_STATUS', 'Group Session Update Status');
INSERT INTO footprint_event_group_type VALUES ('GROUP_SESSION_RESERVATION_CREATE', 'Group Session Reservation Create');
INSERT INTO footprint_event_group_type VALUES ('GROUP_SESSION_RESERVATION_CANCEL', 'Group Session Reservation Cancel');
-- Appointments
INSERT INTO footprint_event_group_type VALUES ('APPOINTMENT_CREATE', 'Appointment Create');
INSERT INTO footprint_event_group_type VALUES ('APPOINTMENT_CREATE_MESSAGES', 'Appointment Create Messages');
INSERT INTO footprint_event_group_type VALUES ('APPOINTMENT_CANCEL', 'Appointment Cancel');
INSERT INTO footprint_event_group_type VALUES ('APPOINTMENT_RESCHEDULE', 'Appointment Reschedule');
-- Patient Orders
INSERT INTO footprint_event_group_type VALUES ('PATIENT_ORDER_UPDATE_GENERAL', 'Patient Order Update (General)');
INSERT INTO footprint_event_group_type VALUES ('PATIENT_ORDER_UPDATE_PANEL_ACCOUNT', 'Patient Order Update Panel Account');
INSERT INTO footprint_event_group_type VALUES ('PATIENT_ORDER_UPDATE_DISPOSITION', 'Patient Order Update Disposition');
INSERT INTO footprint_event_group_type VALUES ('PATIENT_ORDER_UPDATE_CONSENT', 'Patient Order Update Consent');
INSERT INTO footprint_event_group_type VALUES ('PATIENT_ORDER_UPDATE_RESOURCE_CHECK_IN_RESPONSE', 'Patient Order Update Resource Check-In Response');
INSERT INTO footprint_event_group_type VALUES ('PATIENT_ORDER_UPDATE_RESOURCING', 'Patient Order Update Resourcing');
INSERT INTO footprint_event_group_type VALUES ('PATIENT_ORDER_UPDATE_SAFETY_PLANNING', 'Patient Order Update Safety Planning');
INSERT INTO footprint_event_group_type VALUES ('PATIENT_ORDER_UPDATE_ENCOUNTER', 'Patient Order Update Encounter');
-- Patient Order Imports
INSERT INTO footprint_event_group_type VALUES ('PATIENT_ORDER_IMPORT_CREATE', 'Patient Order Import Create');
-- Patient Order Notes
INSERT INTO footprint_event_group_type VALUES ('PATIENT_ORDER_NOTE_CREATE', 'Patient Order Note Create');
INSERT INTO footprint_event_group_type VALUES ('PATIENT_ORDER_NOTE_UPDATE', 'Patient Order Note Update');
INSERT INTO footprint_event_group_type VALUES ('PATIENT_ORDER_NOTE_DELETE', 'Patient Order Note Delete');
-- Patient Order Voicemail Tasks
INSERT INTO footprint_event_group_type VALUES ('PATIENT_ORDER_VOICEMAIL_TASK_CREATE', 'Patient Order Voicemail Task Create');
INSERT INTO footprint_event_group_type VALUES ('PATIENT_ORDER_VOICEMAIL_TASK_UPDATE', 'Patient Order Voicemail Task Update');
INSERT INTO footprint_event_group_type VALUES ('PATIENT_ORDER_VOICEMAIL_TASK_DELETE', 'Patient Order Voicemail Task Delete');
INSERT INTO footprint_event_group_type VALUES ('PATIENT_ORDER_VOICEMAIL_TASK_COMPLETE', 'Patient Order Voicemail Task Complete');
-- Patient Order Scheduled Outreaches
INSERT INTO footprint_event_group_type VALUES ('PATIENT_ORDER_SCHEDULED_OUTREACH_CREATE', 'Patient Order Scheduled Outreach Create');
INSERT INTO footprint_event_group_type VALUES ('PATIENT_ORDER_SCHEDULED_OUTREACH_UPDATE', 'Patient Order Scheduled Outreach Update');
INSERT INTO footprint_event_group_type VALUES ('PATIENT_ORDER_SCHEDULED_OUTREACH_CANCEL', 'Patient Order Scheduled Outreach Cancel');
INSERT INTO footprint_event_group_type VALUES ('PATIENT_ORDER_SCHEDULED_OUTREACH_COMPLETE', 'Patient Order Scheduled Outreach Complete');
-- Patient Order Outreaches
INSERT INTO footprint_event_group_type VALUES ('PATIENT_ORDER_OUTREACH_CREATE', 'Patient Order Outreach Create');
INSERT INTO footprint_event_group_type VALUES ('PATIENT_ORDER_OUTREACH_UPDATE', 'Patient Order Outreach Update');
INSERT INTO footprint_event_group_type VALUES ('PATIENT_ORDER_OUTREACH_DELETE', 'Patient Order Outreach Delete');
-- Patient Order Scheduled Message Groups
INSERT INTO footprint_event_group_type VALUES ('PATIENT_ORDER_SCHEDULED_MESSAGE_GROUP_CREATE', 'Patient Order Scheduled Message Group Create');
INSERT INTO footprint_event_group_type VALUES ('PATIENT_ORDER_SCHEDULED_MESSAGE_GROUP_UPDATE', 'Patient Order Scheduled Message Group Update');
INSERT INTO footprint_event_group_type VALUES ('PATIENT_ORDER_SCHEDULED_MESSAGE_GROUP_DELETE', 'Patient Order Scheduled Message Group Delete');
-- Patient Order Scheduled Screenings
INSERT INTO footprint_event_group_type VALUES ('PATIENT_ORDER_SCHEDULED_SCREENING_CREATE', 'Patient Order Scheduled Screening Create');
INSERT INTO footprint_event_group_type VALUES ('PATIENT_ORDER_SCHEDULED_SCREENING_UPDATE', 'Patient Order Scheduled Screening Update');
INSERT INTO footprint_event_group_type VALUES ('PATIENT_ORDER_SCHEDULED_SCREENING_CANCEL', 'Patient Order Scheduled Screening Cancel');
-- Patient Order Triage Groups
INSERT INTO footprint_event_group_type VALUES ('PATIENT_ORDER_TRIAGE_GROUP_CREATE', 'Patient Order Triage Group Create');
INSERT INTO footprint_event_group_type VALUES ('PATIENT_ORDER_TRIAGE_GROUP_RESET', 'Patient Order Triage Group Reset');
-- Epic Departments
INSERT INTO footprint_event_group_type VALUES ('EPIC_DEPARTMENT_UPDATE', 'Epic Department Update');
-- Screening Answers
INSERT INTO footprint_event_group_type VALUES ('SCREENING_ANSWER_CREATE', 'Screening Answer Create');
-- Screening Sessions
INSERT INTO footprint_event_group_type VALUES ('SCREENING_SESSION_CREATE', 'Screening Session Create');
INSERT INTO footprint_event_group_type VALUES ('SCREENING_SESSION_SKIP', 'Screening Session Skip');
INSERT INTO footprint_event_group_type VALUES ('SCREENING_SESSION_SKIP_IMMEDIATELY', 'Screening Session Skip Immediately');
-- Accounts
INSERT INTO footprint_event_group_type VALUES ('ACCOUNT_CREATE_ANONYMOUS', 'Account Create (Anonymous)');
INSERT INTO footprint_event_group_type VALUES ('ACCOUNT_CREATE_MYCHART', 'Account Create (MyChart)');
INSERT INTO footprint_event_group_type VALUES ('ACCOUNT_CREATE_SSO', 'Account Create (SSO)');
INSERT INTO footprint_event_group_type VALUES ('ACCOUNT_UPDATE_EMAIL_ADDRESS', 'Account Update Email Address');
INSERT INTO footprint_event_group_type VALUES ('ACCOUNT_UPDATE_PHONE_NUMBER', 'Account Update Phone Number');
INSERT INTO footprint_event_group_type VALUES ('ACCOUNT_UPDATE_CONSENT_FORM_ACCEPTANCE', 'Account Update Consent Form Acceptance');
INSERT INTO footprint_event_group_type VALUES ('ACCOUNT_UPDATE_FORGOT_PASSWORD', 'Account Update Forgot Password');
INSERT INTO footprint_event_group_type VALUES ('ACCOUNT_UPDATE_RESET_PASSWORD', 'Account Update Reset Password');
INSERT INTO footprint_event_group_type VALUES ('ACCOUNT_UPDATE_LOCATION', 'Account Update Location');
-- Account Email Verifications
INSERT INTO footprint_event_group_type VALUES ('ACCOUNT_EMAIL_VERIFICATION_CREATE', 'Account Email Verification Create');
INSERT INTO footprint_event_group_type VALUES ('ACCOUNT_EMAIL_VERIFICATION_APPLY', 'Account Email Verification Apply');
-- Scheduled Messages
INSERT INTO footprint_event_group_type VALUES ('SCHEDULED_MESSAGE_SEND', 'Scheduled Message Send');
-- Remote Data Sync
INSERT INTO footprint_event_group_type VALUES ('REMOTE_DATA_SYNC', 'Remote Data Sync');

-- Footprint events are logically grouped together.
-- For example, a CONTENT_CREATE footprint_event_group might have many footprint_event
-- records associated (insert on content table, inserts on content_tag table, etc.)
CREATE TABLE footprint_event_group (
  footprint_event_group_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  footprint_event_group_type_id VARCHAR NOT NULL REFERENCES footprint_event_group_type,
  account_id UUID REFERENCES account,
  connection_username VARCHAR,
  connection_application_name VARCHAR,
  connection_ip_address VARCHAR,
  api_call_url VARCHAR, -- Set if this event group was created on a web request thread
  api_call_request_body VARCHAR, -- Set if this event group was created on a web request thread and request body exists
  background_thread_name VARCHAR, -- Set if this event group was created on a background thread
  created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON footprint_event_group FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

-- Matches Postgres' TG_OP values
CREATE TABLE footprint_event_operation_type (
  footprint_event_operation_type_id VARCHAR PRIMARY KEY,
  description VARCHAR NOT NULL
);

INSERT INTO footprint_event_operation_type VALUES ('INSERT', 'Insert');
INSERT INTO footprint_event_operation_type VALUES ('UPDATE', 'Update');
INSERT INTO footprint_event_operation_type VALUES ('DELETE', 'Delete');
INSERT INTO footprint_event_operation_type VALUES ('TRUNCATE', 'Truncate');

-- Track an insert/update/delete event for a single table.
-- Events can be logically grouped by footprint_event_group above.
CREATE TABLE footprint_event (
  footprint_event_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  footprint_event_group_id UUID NOT NULL REFERENCES footprint_event_group,
  footprint_event_operation_type_id VARCHAR NOT NULL REFERENCES footprint_event_operation_type,
  table_name VARCHAR NOT NULL,
  old_value JSONB, -- JSONB representation of old ROW via to_jsonb(). null if inserting
  new_value JSONB, -- JSONB representation of new ROW via to_jsonb(). null if deleting
  created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON footprint_event FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

-- Store off insert/update/delete footprint event for a single table.
-- If we have contextual information about the account or footprint event group, apply it.
CREATE OR REPLACE FUNCTION perform_footprint() RETURNS TRIGGER AS $$
DECLARE
  current_footprint_event_group_id_as_text TEXT;
  current_account_id_as_text TEXT;
  current_footprint_event_group_id UUID;
  current_account_id UUID;
  current_api_call_url TEXT;
  current_api_call_request_body TEXT;
  current_background_thread_name TEXT;
BEGIN
  -- Pull values from SET LOCAL (if available).
  -- Have to do this in two steps to ensure provided data appears valid for casting to UUID type
  SELECT current_setting('cobalt.footprint_event_group_id', TRUE) INTO current_footprint_event_group_id_as_text;
  SELECT current_setting('cobalt.account_id', TRUE) INTO current_account_id_as_text;
  SELECT NULLIF(current_setting('cobalt.api_call_url', TRUE), '') INTO current_api_call_url;
  SELECT NULLIF(current_setting('cobalt.api_call_request_body', TRUE), '') INTO current_api_call_request_body;
  SELECT NULLIF(current_setting('cobalt.background_thread_name', TRUE), '') INTO current_background_thread_name;

  IF LENGTH(current_footprint_event_group_id_as_text) = 36 THEN
    current_footprint_event_group_id := CAST(current_footprint_event_group_id_as_text AS UUID);
  END IF;

  IF LENGTH(current_account_id_as_text) = 36 THEN
    current_account_id := CAST(current_account_id_as_text AS UUID);
  END IF;

  -- If we don't have a defined event group, create one for this insert
  IF current_footprint_event_group_id IS NULL THEN
		current_footprint_event_group_id := uuid_generate_v4();
		INSERT INTO footprint_event_group (footprint_event_group_id, footprint_event_group_type_id, account_id, connection_username, connection_application_name, connection_ip_address, api_call_url, api_call_request_body, background_thread_name)
		SELECT current_footprint_event_group_id, 'UNSPECIFIED', current_account_id, usename, application_name, client_addr, current_api_call_url, current_api_call_request_body, current_background_thread_name
		FROM pg_stat_activity
		WHERE pid=pg_backend_pid();
  END IF;

  -- Perform the actual event tracking
	IF (TG_OP = 'INSERT') THEN
		INSERT INTO footprint_event (footprint_event_group_id, footprint_event_operation_type_id, table_name, old_value, new_value)
		SELECT current_footprint_event_group_id, 'INSERT', TG_TABLE_NAME, NULL, to_jsonb(NEW);
		RETURN NEW;
	ELSIF (TG_OP = 'UPDATE') THEN
		INSERT INTO footprint_event (footprint_event_group_id, footprint_event_operation_type_id, table_name, old_value, new_value)
		SELECT current_footprint_event_group_id, 'UPDATE', TG_TABLE_NAME, to_jsonb(OLD), to_jsonb(NEW);
		RETURN NEW;
	ELSIF (TG_OP = 'DELETE') THEN
	  INSERT INTO footprint_event (footprint_event_group_id, footprint_event_operation_type_id, table_name, old_value, new_value)
		SELECT current_footprint_event_group_id, 'DELETE', TG_TABLE_NAME, to_jsonb(OLD), NULL;
		RETURN OLD;
	END IF;

	RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Content
CREATE TRIGGER content_footprint AFTER INSERT OR UPDATE OR DELETE ON content FOR EACH ROW EXECUTE PROCEDURE perform_footprint();
CREATE TRIGGER institution_content_footprint AFTER INSERT OR UPDATE OR DELETE ON institution_content FOR EACH ROW EXECUTE PROCEDURE perform_footprint();
CREATE TRIGGER tag_content_footprint AFTER INSERT OR UPDATE OR DELETE ON tag_content FOR EACH ROW EXECUTE PROCEDURE perform_footprint();

-- Group Sessions
CREATE TRIGGER group_session_footprint AFTER INSERT OR UPDATE OR DELETE ON group_session FOR EACH ROW EXECUTE PROCEDURE perform_footprint();
CREATE TRIGGER tag_group_session_footprint AFTER INSERT OR UPDATE OR DELETE ON tag_group_session FOR EACH ROW EXECUTE PROCEDURE perform_footprint();
CREATE TRIGGER group_session_reservation_footprint AFTER INSERT OR UPDATE OR DELETE ON group_session_reservation FOR EACH ROW EXECUTE PROCEDURE perform_footprint();

-- Appointments
CREATE TRIGGER appointment_footprint AFTER INSERT OR UPDATE OR DELETE ON appointment FOR EACH ROW EXECUTE PROCEDURE perform_footprint();
CREATE TRIGGER appointment_scheduled_message_footprint AFTER INSERT OR UPDATE OR DELETE ON appointment_scheduled_message FOR EACH ROW EXECUTE PROCEDURE perform_footprint();

-- Patient Orders
CREATE TRIGGER patient_order_footprint AFTER INSERT OR UPDATE OR DELETE ON patient_order FOR EACH ROW EXECUTE PROCEDURE perform_footprint();
CREATE TRIGGER patient_order_import_footprint AFTER INSERT OR UPDATE OR DELETE ON patient_order_import FOR EACH ROW EXECUTE PROCEDURE perform_footprint();
CREATE TRIGGER patient_order_note_footprint AFTER INSERT OR UPDATE OR DELETE ON patient_order_note FOR EACH ROW EXECUTE PROCEDURE perform_footprint();
CREATE TRIGGER patient_order_voicemail_task_footprint AFTER INSERT OR UPDATE OR DELETE ON patient_order_voicemail_task FOR EACH ROW EXECUTE PROCEDURE perform_footprint();
CREATE TRIGGER patient_order_scheduled_outreach_footprint AFTER INSERT OR UPDATE OR DELETE ON patient_order_scheduled_outreach FOR EACH ROW EXECUTE PROCEDURE perform_footprint();
CREATE TRIGGER patient_order_outreach_footprint AFTER INSERT OR UPDATE OR DELETE ON patient_order_outreach FOR EACH ROW EXECUTE PROCEDURE perform_footprint();
CREATE TRIGGER patient_order_scheduled_message_group_footprint AFTER INSERT OR UPDATE OR DELETE ON patient_order_scheduled_message_group FOR EACH ROW EXECUTE PROCEDURE perform_footprint();
CREATE TRIGGER patient_order_scheduled_message_footprint AFTER INSERT OR UPDATE OR DELETE ON patient_order_scheduled_message FOR EACH ROW EXECUTE PROCEDURE perform_footprint();
CREATE TRIGGER patient_order_scheduled_screening_footprint AFTER INSERT OR UPDATE OR DELETE ON patient_order_scheduled_screening FOR EACH ROW EXECUTE PROCEDURE perform_footprint();
CREATE TRIGGER patient_order_triage_group_footprint AFTER INSERT OR UPDATE OR DELETE ON patient_order_triage_group FOR EACH ROW EXECUTE PROCEDURE perform_footprint();
CREATE TRIGGER patient_order_triage_footprint AFTER INSERT OR UPDATE OR DELETE ON patient_order_triage FOR EACH ROW EXECUTE PROCEDURE perform_footprint();

-- Epic Departments
CREATE TRIGGER epic_department_footprint AFTER INSERT OR UPDATE OR DELETE ON epic_department FOR EACH ROW EXECUTE PROCEDURE perform_footprint();

-- Screening Sessions
CREATE TRIGGER screening_session_footprint AFTER INSERT OR UPDATE OR DELETE ON screening_session FOR EACH ROW EXECUTE PROCEDURE perform_footprint();
CREATE TRIGGER screening_answer_footprint AFTER INSERT OR UPDATE OR DELETE ON screening_answer FOR EACH ROW EXECUTE PROCEDURE perform_footprint();

-- Accounts
CREATE TRIGGER account_footprint AFTER INSERT OR UPDATE OR DELETE ON account FOR EACH ROW EXECUTE PROCEDURE perform_footprint();
CREATE TRIGGER password_reset_request_footprint AFTER INSERT OR UPDATE OR DELETE ON password_reset_request FOR EACH ROW EXECUTE PROCEDURE perform_footprint();
CREATE TRIGGER account_email_verification_footprint AFTER INSERT OR UPDATE OR DELETE ON account_email_verification FOR EACH ROW EXECUTE PROCEDURE perform_footprint();

-- Scheduled Messages
CREATE TRIGGER scheduled_message_footprint AFTER INSERT OR UPDATE OR DELETE ON scheduled_message FOR EACH ROW EXECUTE PROCEDURE perform_footprint();

-- Institutions
CREATE TRIGGER institution_footprint AFTER INSERT OR UPDATE OR DELETE ON institution FOR EACH ROW EXECUTE PROCEDURE perform_footprint();

-- Useful for examining diffs between footprint events
--
-- Example:
-- > select jsonb_diff(old_value, new_value) from footprint_event where footprint_event_id='...'
--                                jsonb_diff
-- ----------------------------------------------------------------------
-- {"canceled": true, "last_updated": "2024-08-22T17:13:58.972176+00:00"}
--
-- Sourced from https://gist.github.com/jarppe/f3cdd32ec58a4bdfb29daa67ef6c3b78
--
-- Helper function to produce a diff of two jsonb values.
--
-- Accepts:
--   val1: original jsonb value
--   val2: updated jsonb value
--
-- Returns:
--   jsonb of changed values
--
-- Examples:
--   val1:     {"a": {"b": 1}}
--   val2:     {"a": {"b": 1, "c": 2}}
--   returns:  {"a": {"c": 2}}
--
create or replace function jsonb_diff
  (old jsonb, new jsonb)
  returns jsonb
  language 'plpgsql'
as
$$
declare
  result        jsonb;
  object_result jsonb;
  k             text;
  v             record;
  empty         jsonb = '{}'::jsonb;
begin
  if old is null or jsonb_typeof(old) = 'null'
    then
      return new;
  end if;

  if new is null or jsonb_typeof(new) = 'null'
    then
      return empty;
  end if;

  result = old;

  for k in select * from jsonb_object_keys(old)
    loop
      result = result || jsonb_build_object(k, null);
    end loop;

  for v in select * from jsonb_each(new)
    loop
      if jsonb_typeof(old -> v.key) = 'object' and jsonb_typeof(new -> v.key) = 'object'
        then
          object_result = jsonb_diff(old -> v.key, new -> v.key);
          if object_result = empty
            then
              result = result - v.key;
            else
              result = result || jsonb_build_object(v.key, object_result);
          end if;
        elsif old -> v.key = new -> v.key
          then
            result = result - v.key;
        else
          result = result || jsonb_build_object(v.key, v.value);
      end if;
    end loop;

  return result;
end;
$$;

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
    c.remote_data_flag,
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

-- Content audience type group, e.g. "The user's family member" is a group that could include audience types TODDLER, PRETEEN, etc.
CREATE TABLE content_audience_type_group (
	content_audience_type_group_id TEXT PRIMARY KEY,
  description VARCHAR NOT NULL,
  example_sentence VARCHAR NOT NULL,
  display_order INTEGER NOT NULL
);

INSERT INTO content_audience_type_group (content_audience_type_group_id, description, example_sentence, display_order)
VALUES ('MYSELF', 'The user (who is an adult)', 'I want resources for myself...', 1);

INSERT INTO content_audience_type_group (content_audience_type_group_id, description, example_sentence, display_order)
VALUES ('FAMILY_MEMBER', 'The user''s family member', 'I want resources for...', 2);

-- Introduce content audience: who the content is for (myself, someone else)
CREATE TABLE content_audience_type (
	content_audience_type_id TEXT PRIMARY KEY,
	content_audience_type_group_id TEXT NOT NULL REFERENCES content_audience_type_group,
  description VARCHAR NOT NULL,
  patient_representation VARCHAR NOT NULL,
  url_name VARCHAR NOT NULL,
  display_order INTEGER NOT NULL
);

CREATE UNIQUE INDEX content_audience_type_url_name_unique_idx ON content_audience_type USING btree (url_name);

INSERT INTO content_audience_type (content_audience_type_id, content_audience_type_group_id, description, patient_representation, url_name, display_order)
VALUES ('MYSELF', 'MYSELF', 'Myself', 'myself', 'myself', 1);

INSERT INTO content_audience_type (content_audience_type_id, content_audience_type_group_id, description, patient_representation, url_name, display_order)
VALUES ('CHILD', 'FAMILY_MEMBER', 'Toddler/Child', 'toddler/child', 'child',  2);

INSERT INTO content_audience_type (content_audience_type_id, content_audience_type_group_id, description, patient_representation, url_name, display_order)
VALUES ('TEEN', 'FAMILY_MEMBER', 'Preteen/Teen', 'preteen/teen', 'teen',  3);

INSERT INTO content_audience_type (content_audience_type_id, content_audience_type_group_id, description, patient_representation, url_name, display_order)
VALUES ('ADULT_CHILD', 'FAMILY_MEMBER', 'Adult Child', 'adult child', 'adult-child',  4);

INSERT INTO content_audience_type (content_audience_type_id, content_audience_type_group_id, description, patient_representation, url_name, display_order)
VALUES ('SPOUSE', 'FAMILY_MEMBER', 'Spouse (Adult)', 'spouse', 'spouse',  5);

INSERT INTO content_audience_type (content_audience_type_id, content_audience_type_group_id, description, patient_representation, url_name, display_order)
VALUES ('PARENT', 'FAMILY_MEMBER', 'Aging Parent (Senior)', 'aging parent', 'parent',  6);

-- Combine content with audience types (a single piece of content might have many audience types)
CREATE TABLE content_audience (
	content_id UUID NOT NULL REFERENCES content,
	content_audience_type_id TEXT NOT NULL REFERENCES content_audience_type,
	created_by_account_id UUID NOT NULL REFERENCES account(account_id),
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	CONSTRAINT content_audience_pkey PRIMARY KEY(content_id, content_audience_type_id)
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON content_audience FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

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