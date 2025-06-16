BEGIN;
SELECT _v.register_patch('218-courses-homepage', NULL, NULL);

CREATE TABLE institution_course_status (
institution_course_status_id TEXT NOT NULL PRIMARY KEY,
description TEXT NOT NULL
);


INSERT INTO institution_course_status VALUES ('COMING_SOON', 'Coming Soon');
INSERT INTO institution_course_status VALUES ('AVAILABLE', 'Available');

ALTER TABLE institution_course ADD COLUMN institution_course_status_id TEXT REFERENCES institution_course_status;

CREATE OR REPLACE VIEW v_course AS
SELECT c.*, ic.institution_id, ic.url_name, ic.display_order, ic.institution_course_status_id
FROM course c, institution_course ic
WHERE c.course_id=ic.course_id;

END;
