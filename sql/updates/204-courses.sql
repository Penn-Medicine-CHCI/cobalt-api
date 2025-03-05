BEGIN;
SELECT _v.register_patch('204-courses', NULL, NULL);

-- Institutions can now have an "onboarding" screening flow that is required before proceeding further in the system
ALTER TABLE institution ADD COLUMN onboarding_screening_flow_id UUID REFERENCES screening_flow(screening_flow_id);

-- Introduce "course" feature
INSERT INTO feature (feature_id, navigation_header_id, name, url_name) VALUES
	('COURSE', 'CONNECT_WITH_SUPPORT', 'Complete self-guided coursework', '/courses');

-- A course is divided into modules, with each module divided into units.
CREATE TABLE course (
	course_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	title TEXT NOT NULL,
	description TEXT NOT NULL, -- Can include HTML
	focus TEXT NOT NULL, -- Can include HTML
	image_url TEXT NOT NULL,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON course FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

-- A course module is a logical grouping for course units.
CREATE TABLE course_module (
	course_module_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	course_id UUID NOT NULL REFERENCES course,
	title TEXT NOT NULL,
	estimated_completion_time_in_minutes INTEGER,
	display_order INTEGER NOT NULL,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON course_module FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

-- Course unit types, e.g. "video" or "card sort"
CREATE TABLE course_unit_type (
	course_unit_type_id TEXT PRIMARY KEY,
  description VARCHAR NOT NULL
);

-- A video to play
INSERT INTO course_unit_type VALUES ('VIDEO', 'Video');
-- An infographic to view and optionally download
INSERT INTO course_unit_type VALUES ('INFOGRAPHIC', 'Infographic');
-- Describes a homework assignment, with an optional download
INSERT INTO course_unit_type VALUES ('HOMEWORK', 'Homework');
-- A "card sorting" exercise (given a prompt, decide which bucket it fits into)
INSERT INTO course_unit_type VALUES ('CARD_SORT', 'Card Sort');
-- A set of questions to answer
INSERT INTO course_unit_type VALUES ('QUIZ', 'Quiz');
-- Given a set of statements, re-organize them into the correct order
INSERT INTO course_unit_type VALUES ('REORDER', 'Reorder');

-- In order to support card sort and reordering course units via screening questions, introduce new answer formats
INSERT INTO screening_answer_format (screening_answer_format_id, description) VALUES ('CARD_SORT', 'Card Sort');
INSERT INTO screening_answer_format (screening_answer_format_id, description) VALUES ('REORDER', 'Reorder');

-- Indicates we want the UI to constrain `FREEFORM_TEXT` input to integer values
INSERT INTO screening_answer_content_hint (screening_answer_content_hint_id, description) VALUES ('INTEGER', 'Integer');

-- Video vendors, e.g. Kaltura or YouTube
CREATE TABLE video_vendor (
	video_vendor_id TEXT PRIMARY KEY,
  description VARCHAR NOT NULL
);

INSERT INTO video_vendor VALUES ('KALTURA', 'Kaltura');
INSERT INTO video_vendor VALUES ('YOUTUBE', 'YouTube');

-- In order to support embedding videos into course units, we need to introduce a video construct
CREATE TABLE video (
  video_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  video_vendor_id TEXT NOT NULL REFERENCES video_vendor,
  youtube_id TEXT, -- only applicable to YOUTUBE vendor type
  kaltura_partner_id TEXT, -- only applicable to KALTURA vendor type
  kaltura_uiconf_id TEXT, -- only applicable to KALTURA vendor type
  kaltura_wid TEXT, -- only applicable to KALTURA vendor type
  kaltura_entry_id TEXT, -- only applicable to KALTURA vendor type
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON video FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

-- Ensure that data in video is appropriate for the type
CREATE OR REPLACE FUNCTION video_validation()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.video_vendor_id = 'YOUTUBE' AND NEW.youtube_id IS NULL THEN
        RAISE EXCEPTION 'youtube_id column must be non-null for video_vendor_id YOUTUBE';
    ELSIF NEW.video_vendor_id = 'KALTURA' AND (NEW.kaltura_partner_id IS NULL OR NEW.kaltura_uiconf_id IS NULL OR NEW.kaltura_wid IS NULL OR NEW.kaltura_entry_id IS NULL) THEN
        RAISE EXCEPTION 'kaltura_partner_id, kaltura_uiconf_id, kaltura_wid, and kaltura_entry_id columns must be non-null for video_vendor_id KALTURA';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER video_validation_tg
AFTER INSERT OR UPDATE ON video
FOR EACH ROW EXECUTE FUNCTION video_validation();

-- A course unit is the component where the user performs meaningful action within a course - watches a video, takes a quiz, etc.
CREATE TABLE course_unit (
	course_unit_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	course_module_id UUID NOT NULL REFERENCES course_module,
	course_unit_type_id TEXT NOT NULL REFERENCES course_unit_type,
	title TEXT NOT NULL,
	description TEXT, -- Can include HTML
	display_order INTEGER NOT NULL, -- Order within the module
	video_id UUID REFERENCES video, -- Only applies to VIDEO course_unit_type_id
	screening_flow_id UUID REFERENCES screening_flow, -- Only applies to units that include questions and answers, e.g. QUIZ course_unit_type_id
	image_url TEXT, -- Only applies to units that include an embedded image, e.g. INFOGRAPHIC course_unit_type_id
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON course_unit FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

-- Some units might contain a set of downloadable files, e.g. INFOGRAPHIC course_unit_type_id.
-- This lets us associate an ordered list of files with the unit.
-- We have an explicit course_unit_downloadable_file_id PK instead of composite (course_unit_id, file_upload_id) to make analytics tracking more robust.
CREATE TABLE course_unit_downloadable_file (
  course_unit_downloadable_file_id UUID NOT NULL PRIMARY KEY,
	course_unit_id UUID NOT NULL REFERENCES course_unit,
	file_upload_id UUID NOT NULL REFERENCES file_upload,
	display_order INTEGER NOT NULL,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_course_unit_downloadable_file_file_upload ON course_unit_downloadable_file (course_unit_id, file_upload_id);
CREATE UNIQUE INDEX idx_course_unit_downloadable_file_display_order ON course_unit_downloadable_file (course_unit_id, display_order);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON course_unit_downloadable_file FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

-- Ensure that data in course_unit is appropriate for the type
CREATE OR REPLACE FUNCTION course_unit_validation()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.course_unit_type_id = 'VIDEO' AND NEW.video_id IS NULL THEN
        RAISE EXCEPTION 'video_id must be set for VIDEO course unit types';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER course_unit_validation_tg
AFTER INSERT OR UPDATE ON course_unit
FOR EACH ROW EXECUTE FUNCTION course_unit_validation();

-- Is a course session in-progress, completed, or canceled?
CREATE TABLE course_session_status (
	course_session_status_id TEXT PRIMARY KEY,
  description VARCHAR NOT NULL
);

INSERT INTO course_session_status VALUES ('IN_PROGRESS', 'In-Progress');
INSERT INTO course_session_status VALUES ('COMPLETED', 'Completed');
INSERT INTO course_session_status VALUES ('CANCELED', 'Canceled');

-- Keeps track of a participant's "session" when taking a course.
-- All responses/events that occur while taking the course are tied to the session.
-- You might retake a course, for example, which would create a new session.
CREATE TABLE course_session (
	course_session_id UUID NOT NULL PRIMARY KEY,
  account_id UUID NOT NULL REFERENCES account,
  course_session_status_id TEXT NOT NULL REFERENCES course_session_status,
  created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON course_session FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

-- User has taken some kind of action that results in a module being marked as optional (e.g. answering a quiz in a way that indicates their child doesn't have ADHD, which would make the ADHD module optional for that session).
-- This lets the UI take custom action, e.g. showing the module below all the other ones and making it visually distinct.
CREATE TABLE course_session_optional_module (
	course_session_id UUID NOT NULL REFERENCES course_session,
  course_module_id UUID NOT NULL REFERENCES course_module,
  created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (course_session_id, course_module_id)
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON course_session_optional_module FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

-- Keeps track of unit progress during a session - was the unit completed or skipped?
CREATE TABLE course_session_unit_status (
	course_session_unit_status_id TEXT PRIMARY KEY,
  description VARCHAR NOT NULL
);

-- User has explicitly completed the unit
INSERT INTO course_session_unit_status VALUES ('COMPLETE', 'Complete');
-- User has explicitly skipped the unit
INSERT INTO course_session_unit_status VALUES ('SKIPPED', 'Skipped');

-- Tracks unit status for a given course session
CREATE TABLE course_session_unit (
  course_session_id UUID NOT NULL REFERENCES course_session,
  course_unit_id UUID NOT NULL REFERENCES course_unit,
	course_session_unit_status_id TEXT NOT NULL REFERENCES course_session_unit_status,
  created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (course_session_id, course_unit_id)
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON course_session_unit FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

-- How course units can depend on each other
CREATE TABLE course_unit_dependency_type (
	course_unit_dependency_type_id TEXT PRIMARY KEY,
  description VARCHAR NOT NULL
);

-- A strong dependency means a unit _must be completed_ prior to completing another
INSERT INTO course_unit_dependency_type VALUES ('STRONG', 'Strong');
-- A weak dependency means a unit _is recommended to be completed_ prior to completing another
INSERT INTO course_unit_dependency_type VALUES ('WEAK', 'Weak');

-- Units can depend on the completion of others, e.g. you can "unlock" a unit (strong) or get advised "you should probably complete this unit first" (weak)
CREATE TABLE course_unit_dependency (
	determinant_course_unit_id UUID NOT NULL REFERENCES course_unit(course_unit_id), -- The unit that must be completed first
	dependent_course_unit_id UUID NOT NULL REFERENCES course_unit(course_unit_id), -- The unit that depends on the completion
	course_unit_dependency_type_id TEXT NOT NULL REFERENCES course_unit_dependency_type,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	PRIMARY KEY (determinant_course_unit_id, dependent_course_unit_id)
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON course_unit_dependency FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

-- Associate courses with institutions
CREATE TABLE institution_course (
  institution_id VARCHAR NOT NULL REFERENCES institution,
	course_id UUID NOT NULL REFERENCES course,
	url_name TEXT NOT NULL,
	display_order INTEGER NOT NULL,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	PRIMARY KEY (course_id, institution_id)
);

-- Convenience view for including URL name and display order
CREATE VIEW v_course AS
SELECT c.*, ic.institution_id, ic.url_name, ic.display_order
FROM course c, institution_course ic
WHERE c.course_id=ic.course_id;

-- Can't duplicate display orders per-institution
CREATE UNIQUE INDEX idx_institution_course_display_order ON institution_course (institution_id, display_order);

-- Can't duplicate URL names per-institution
CREATE UNIQUE INDEX idx_institution_course_url_name ON institution_course (institution_id, url_name);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON institution_course FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

-- Prepare screening sessions to support courses
INSERT INTO screening_type (screening_type_id, description) VALUES ('COURSE_UNIT', 'Course Unit');
INSERT INTO screening_flow_type (screening_flow_type_id, description) VALUES ('ONBOARDING', 'Onboarding');
INSERT INTO screening_flow_type (screening_flow_type_id, description) VALUES ('COURSE_UNIT', 'Course Unit');
ALTER TABLE screening_session ADD COLUMN course_session_id UUID REFERENCES course_session;

-- *** Analytics updates to support courses ***

-- On the web, when the "courses" page is rendered.
-- There is no additional data associated with this event type.
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('PAGE_VIEW_COURSES', 'Page View (Courses)');

-- On the web, when the "course detail" page is rendered.
-- Additional data:
-- * courseId (UUID)
-- * courseSessionId (UUID) - optional, if a session has been started for this course
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('PAGE_VIEW_COURSE_DETAIL', 'Page View (Course Detail)');

-- On the web, when the "course unit" page is rendered.
-- Additional data:
-- * courseUnitId (UUID)
-- * courseSessionId (UUID) - optional, if a session has been started for this course
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('PAGE_VIEW_COURSE_UNIT', 'Page View (Course Unit)');

-- When a video that's part of a course unit fires off an event that we listen for, e.g. 'playerReady' or 'playerPaused'.
--
-- Additional data:
-- * courseUnitId (UUID)
-- * courseSessionId (UUID) - optional, if a session has been started for this course
-- * videoId (UUID)
-- * eventName (String) - the name of the event, e.g. 'playerReady', which is specific to the type of video (Kaltura, YouTube, ...)
-- * eventPayload (any) - optional, a payload for the event specific to the type of video (Kaltura, YouTube, ...)
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('EVENT_COURSE_UNIT_VIDEO', 'Event (Course Unit Video)');

-- When the user clicks to download a file that's part of a course unit.
--
-- Additional data:
-- * courseUnitId (UUID)
-- * courseSessionId (UUID) - optional, if a session has been started for this course
-- * courseUnitDownloadableFileId (UUID) - the file for which click-to-download was initiated
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('CLICKTHROUGH_COURSE_UNIT_DOWNLOADABLE_FILE', 'Clickthrough (Course Unit Downloadable File)');

COMMIT;