BEGIN;
SELECT _v.register_patch('235-course-finalization', NULL, NULL);

-- We have a new "final" unit type with special presentation characteristics
INSERT INTO course_unit_type (course_unit_type_id, description, unit_completion_type_id, show_restart_activity_when_complete, show_unit_as_complete)
VALUES ('FINAL', 'Final', 'IMMEDIATELY', FALSE, FALSE);

-- We are no longer tying content directly to courses.  Instead, we are tying to course units.
DROP TABLE course_content;

-- Associate supplemental content with course units
CREATE TABLE course_unit_content (
	course_unit_id UUID NOT NULL REFERENCES course_unit,
	content_id UUID NOT NULL REFERENCES content,
	display_order INTEGER NOT NULL,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	PRIMARY KEY (course_unit_id, content_id)
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON course_unit_content FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

-- Performance index
CREATE INDEX ON course_unit_content (course_unit_id, display_order) INCLUDE (content_id);

-- Get rid of old analytics event type that we don't use anymore (CLICKTHROUGH_COURSE_CONTENT) and replace with new (CLICKTHROUGH_COURSE_UNIT_CONTENT)
DELETE FROM analytics_native_event_type where analytics_native_event_type_id='CLICKTHROUGH_COURSE_CONTENT';

-- When the user clicks to view a piece of content associated with a course unit.
--
-- Additional data:
-- * courseSessionId (UUID) - optional, if a session has been started for this course
-- * courseUnitId (UUID) - the course unit containing the clicked-on content
-- * contentId (UUID) - the content that was clicked
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('CLICKTHROUGH_COURSE_UNIT_CONTENT', 'Clickthrough (Course Unit Content)');

COMMIT;