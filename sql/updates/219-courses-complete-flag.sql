BEGIN;

ALTER TABLE course_unit_type ADD COLUMN show_unit_as_complete BOOLEAN NOT NULL DEFAULT TRUE;

INSERT INTO course_unit_type VALUES ('THINGS_TO_SHARE', 'Things to Share', 'IMMEDIATELY', FALSE, FALSE);

COMMIT;