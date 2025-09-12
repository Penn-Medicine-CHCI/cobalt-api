BEGIN;
SELECT _v.register_patch('232-data-driven-reports', NULL, NULL);

-- What kinds of analytics are supported per-institution?
CREATE TABLE analytics_profile (
  analytics_profile_id TEXT PRIMARY KEY,
  description TEXT NOT NULL
);

INSERT INTO analytics_profile VALUES ('LEGACY', 'Legacy'); -- For legacy analytics_google_bigquery_event/analytics_mixpanel_event
INSERT INTO analytics_profile VALUES ('XRAY', 'X-Ray'); -- For X-Ray

ALTER TABLE institution ADD COLUMN analytics_profile_id TEXT NOT NULL REFERENCES analytics_profile DEFAULT 'XRAY';

-- "Standard" reports (CSV downloads) that live within the "Reports" section of the admin UI (dropdown with report names).
-- Only applicable for institutions with analytics_profile_id of 'XRAY'.
CREATE TABLE analytics_standard_report (
  analytics_standard_report_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  institution_id TEXT NOT NULL REFERENCES institution,
  report_type_id TEXT NOT NULL REFERENCES report_type,
  name TEXT NOT NULL,
  display_order INTEGER NOT NULL,
  created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON analytics_standard_report FOR EACH ROW EXECUTE PROCEDURE set_last_updated();
CREATE UNIQUE INDEX analytics_standard_report_irt ON analytics_standard_report (institution_id, report_type_id);
CREATE UNIQUE INDEX analytics_standard_report_name ON analytics_standard_report (institution_id, name);
CREATE UNIQUE INDEX analytics_standard_report_display_order ON analytics_standard_report (institution_id, display_order);

-- Logical groupings of analytics reports.
-- Each grouping is displayed as a tab (with specified 'name') in the admin UI.
-- Only applicable for institutions with analytics_profile_id of 'XRAY'.
CREATE TABLE analytics_report_group (
  analytics_report_group_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  institution_id TEXT NOT NULL REFERENCES institution,
  name TEXT NOT NULL,
  display_order INTEGER NOT NULL,
  created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON analytics_report_group FOR EACH ROW EXECUTE PROCEDURE set_last_updated();
CREATE UNIQUE INDEX idx_analytics_report_group_institution_display_order ON analytics_report_group (institution_id, display_order);
CREATE UNIQUE INDEX idx_analytics_report_group_institution_name ON analytics_report_group (institution_id, name);

-- Reports that live within a report group (admin UI analytics tab)
CREATE TABLE analytics_report_group_report (
  analytics_report_group_report_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  analytics_report_group_id UUID NOT NULL REFERENCES analytics_report_group,
  report_type_id TEXT NOT NULL REFERENCES report_type,
  display_order INTEGER NOT NULL,
  created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON analytics_report_group_report FOR EACH ROW EXECUTE PROCEDURE set_last_updated();
CREATE UNIQUE INDEX analytics_report_group_report_display_order ON analytics_report_group_report (analytics_report_group_id, display_order);

-- Add new X-Ray report types
INSERT INTO report_type VALUES ('ADMIN_ANALYTICS_ACCOUNT_VISITS', 'Admin Analytics - Account Visits', 100);
INSERT INTO report_type VALUES ('ADMIN_ANALYTICS_ACCOUNT_CREATION', 'Admin Analytics - Account Creation', 101);
INSERT INTO report_type VALUES ('ADMIN_ANALYTICS_ACCOUNT_REPEAT_VISITS', 'Admin Analytics - Account Repeat Visits', 102);
INSERT INTO report_type VALUES ('ADMIN_ANALYTICS_COURSE_ACCOUNT_VISITS', 'Admin Analytics - Course Account Visits', 103);
INSERT INTO report_type VALUES ('ADMIN_ANALYTICS_COURSE_AGGREGATE_VISITS', 'Admin Analytics - Course Aggregate Visits', 104);
INSERT INTO report_type VALUES ('ADMIN_ANALYTICS_COURSE_MODULE_ACCOUNT_VISITS', 'Admin Analytics - Course Module Account Visits', 105);
INSERT INTO report_type VALUES ('ADMIN_ANALYTICS_COURSE_DWELL_TIME', 'Admin Analytics - Course Dwell Time', 106);
INSERT INTO report_type VALUES ('ADMIN_ANALYTICS_COURSE_MODULE_DWELL_TIME', 'Admin Analytics - Course Module Dwell Time', 107);
INSERT INTO report_type VALUES ('ADMIN_ANALYTICS_COURSE_COMPLETION', 'Admin Analytics - Course Completion', 108);
INSERT INTO report_type VALUES ('ADMIN_ANALYTICS_COURSE_AGGREGATE_COMPLETIONS', 'Admin Analytics - Course Aggregate Completions', 109);
INSERT INTO report_type VALUES ('ADMIN_ANALYTICS_COURSE_MODULE_COMPLETION', 'Admin Analytics - Course Module Completion', 110);
INSERT INTO report_type VALUES ('ADMIN_ANALYTICS_ACCOUNT_LOCATION', 'Admin Analytics - Account Location', 111);
INSERT INTO report_type VALUES ('ADMIN_ANALYTICS_ACCOUNT_REFERRER', 'Admin Analytics - Account Referrer', 112);
INSERT INTO report_type VALUES ('ADMIN_ANALYTICS_ACCOUNT_ONBOARDING_RESULTS', 'Admin Analytics - Account Onboarding Results', 113);

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

-- Support for Kaltura playlists
ALTER TABLE video ADD COLUMN kaltura_playlist_id TEXT;


ALTER TABLE video ALTER COLUMN kaltura_entry_id DROP not null;

CREATE OR REPLACE FUNCTION video_validation()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.video_vendor_id = 'YOUTUBE' 
       AND NEW.youtube_id IS NULL THEN
        RAISE EXCEPTION 
            'youtube_id column must be non-null for video_vendor_id YOUTUBE';

    ELSIF NEW.video_vendor_id = 'KALTURA' 
       AND (
            NEW.kaltura_partner_id IS NULL 
            OR NEW.kaltura_uiconf_id IS NULL 
            OR NEW.kaltura_wid IS NULL
            OR (NEW.kaltura_entry_id IS NULL AND NEW.kaltura_playlist_id IS NULL)
       ) THEN
        RAISE EXCEPTION 
            'kaltura_partner_id, kaltura_uiconf_id, kaltura_wid must be non-null, and either kaltura_entry_id or kaltura_playlist_id must be non-null for video_vendor_id KALTURA';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


END;