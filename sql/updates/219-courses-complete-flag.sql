BEGIN;
SELECT _v.register_patch('219-courses-complete-flag', NULL, NULL);

ALTER TABLE course_unit_type ADD COLUMN show_unit_as_complete BOOLEAN NOT NULL DEFAULT TRUE;

INSERT INTO course_unit_type VALUES ('THINGS_TO_SHARE', 'Things to Share', 'IMMEDIATELY', FALSE, FALSE);

ALTER TABLE screening_answer_option ADD COLUMN freeform_supplement_text_auto_show BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE screening_answer_option ADD COLUMN freeform_supplement_content_hint_id VARCHAR NULL REFERENCES screening_answer_content_hint;

COMMIT;