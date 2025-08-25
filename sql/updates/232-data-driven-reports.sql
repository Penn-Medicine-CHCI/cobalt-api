BEGIN;
SELECT _v.register_patch('232-data-driven-reports', NULL, NULL);

-- What kinds of analytics are supported per-institution?
CREATE TABLE analytics_profile (
  analytics_profile_id TEXT PRIMARY KEY,
  description TEXT NOT NULL
);

INSERT INTO analytics_profile VALUES ('LEGACY', 'Legacy'); -- For legacy analytics_google_bigquery_event/analytics_mixpanel_event
INSERT INTO analytics_profile VALUES ('XRAY', 'X-Ray'); -- For X-Ray

ALTER TABLE institution ADD COLUMN analytics_profile_id NOT NULL REFERENCES analytics_profile DEFAULT 'XRAY';

-- TODO: don't model legacy ADMIN_ANALYTICS_OVERVIEW etc.  Instead, add new ADMIN_ANALYTICS_V2_SITE_ACTIVE_ACCOUNTS, ADMIN_ANALYTICS_V2_SITE_CREATED_ACCOUNTS, ...
INSERT INTO site_location (site_location_id, description) VALUES
  ('ADMIN_REPORTS', 'Admin Reports'),
  ('ADMIN_ANALYTICS_XRAY_', 'Admin Analytics - Overview'),
  ('ADMIN_ANALYTICS_ASSESSMENTS_APPOINTMENTS', 'Admin Analytics - Assessments & Appointments'),
  ('ADMIN_ANALYTICS_GROUP_SESSIONS', 'Admin Analytics - Group Sessions'),
  ('ADMIN_ANALYTICS_RESOURCES_TOPICS', 'Admin Analytics - Resources & Topics'),
  ('ADMIN_ANALYTICS_TABLEAU', 'Admin Analytics - Tableau');

-- Reports are now customized per-institution for non-legacy-flag institutions.
--CREATE TABLE institution_report_type ()

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON institution_report_type FOR EACH ROW EXECUTE PROCEDURE set_last_updated();
CREATE UNIQUE INDEX idx_institution_report_type_i_rt ON institution_report_type (institution_id, report_type_id);

CREATE TABLE report_site_location (
	report_site_location_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	institution_report_type_id UUID NOT NULL REFERENCES institution_report_type,
	site_location_id TEXT REFERENCES site_location,
	display_order SMALLINT NOT NULL,
	created_by_account_id UUID NOT NULL REFERENCES account(account_id),
	created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
	last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON report_site_location FOR EACH ROW EXECUTE PROCEDURE set_last_updated();
CREATE UNIQUE INDEX idx_report_site_location_rt_sl ON report_site_location (institution_report_type_id, site_location_id);

-- Configure footprint event types for course sessions
INSERT INTO footprint_event_group_type VALUES ('COURSE_SESSION_CREATE', 'Course Session Create');
INSERT INTO footprint_event_group_type VALUES ('COURSE_UNIT_COMPLETE', 'Course Unit Complete');

-- Track footprint data for course sessions
CREATE TRIGGER course_session_footprint AFTER INSERT OR UPDATE OR DELETE ON course_session FOR EACH ROW EXECUTE PROCEDURE perform_footprint();
CREATE TRIGGER course_session_unit_footprint AFTER INSERT OR UPDATE OR DELETE ON course_session_unit FOR EACH ROW EXECUTE PROCEDURE perform_footprint();
CREATE TRIGGER course_session_optional_module_footprint AFTER INSERT OR UPDATE OR DELETE ON course_session_optional_module FOR EACH ROW EXECUTE PROCEDURE perform_footprint();

-- Introduce a way for a "triage message" (content is HTML) to be shown to users after completing a course unit.
-- In practice, this could be specified by the screening flow results function when the user completes a screening session for a course unit.
ALTER TABLE course_session_unit ADD COLUMN completion_message TEXT;

END;