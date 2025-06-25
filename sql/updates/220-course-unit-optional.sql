BEGIN;
SELECT _v.register_patch('220-course-unit-optional', NULL, NULL);

ALTER TABLE course_unit ADD COLUMN optional_unit BOOLEAN NOT NULL DEFAULT FALSE;

END;