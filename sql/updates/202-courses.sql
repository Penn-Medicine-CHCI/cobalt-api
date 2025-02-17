BEGIN;
SELECT _v.register_patch('202-courses', NULL, NULL);

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
	title TEXT NOT NULL,
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

--
-- course_unit
-- Note: we probably only need a few fields: title, description (html), video_url, file_url, screening_flow_id (for card sort/questionnaire/reorder).
-- We might need to introduce a video_embed concept and link to that instead, like to support Kaltura embeds
-- We can enforce fields being set via DB triggers

--
-- course_session
-- Note: we will need to keep track of a set of inapplicable course unit IDs in a separate table

-- Course unit types, e.g. "video" or "card sort"
CREATE TABLE course_unit_dependency_type (
	course_unit_dependency_type_id TEXT PRIMARY KEY,
  description VARCHAR NOT NULL
);

INSERT INTO course_unit_dependency_type VALUES ('STRONG', 'Strong');
INSERT INTO course_unit_dependency_type VALUES ('WEAK', 'Weak');

-- course_unit_dependency

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

-- Can't duplicate display orders per-institution
CREATE UNIQUE INDEX idx_institution_course_display_order ON institution_course (institution_id, display_order);

-- Can't duplicate URL names per-institution
CREATE UNIQUE INDEX idx_institution_course_url_name ON institution_course (institution_id, url_name);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON institution_course FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

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

-- TODO: video events
-- TODO: clickthrough events

-- Additional notes

-- * POST /screening-answers should optionally return:
--   1. A "message" construct with type (success, warning, error) and title and message (html supported).
--   2. A "resultsByScreeningAnswerOptionId", where key is screening_answer_option_id and value is an object with "type" (success, warning, error) and "message" (optional) fields.

-- * Introduce ScreeningSessionDestinationId.NONE to indicate "don't go anywhere" after session is done.

-- * Completing a screening flow should return an optional set of inapplicableCourseUnitIds, so you can say "if user said their child doesn't have ADHD, hide the ADHD-related units"

COMMIT;