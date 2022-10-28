BEGIN;
SELECT _v.register_patch('41-screening-enhancements', NULL, NULL);

-- Optional text to be shown under the set of questions
ALTER TABLE screening_question ADD COLUMN footer_text TEXT;

-- Whether an answer supports a free-text supplement (e.g. "Other (specify below)").
-- Note: not compatible with questions that have screening_answer_format_id value FREEFORM_TEXT
ALTER TABLE screening_answer_option ADD COLUMN freeform_supplement BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE screening_answer_option ADD COLUMN freeform_supplement_description TEXT; -- optional placeholder



COMMIT;