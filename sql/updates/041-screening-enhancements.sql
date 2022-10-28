BEGIN;
SELECT _v.register_patch('41-screening-enhancements', NULL, NULL);

-- Optional text to be shown under the set of questions
ALTER TABLE screening_question ADD COLUMN footer_text TEXT;

-- Whether an answer supports a free-text supplement (e.g. "Other (specify below)").
-- Note: not compatible with questions that have screening_answer_format_id value FREEFORM_TEXT
ALTER TABLE screening_answer_option ADD COLUMN freeform_supplement BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE screening_answer_option ADD COLUMN freeform_supplement_text TEXT; -- optional placeholder

CREATE TABLE screening_image (
	screening_image_id TEXT PRIMARY KEY,
	description TEXT NOT NULL
);

INSERT INTO screening_image VALUES ('APPOINTMENT', 'Appointment');
INSERT INTO screening_image VALUES ('CONNECTED_TO_CARE', 'Connected to care');
INSERT INTO screening_image VALUES ('CONNECTING_TO_CARE', 'Connecting to care');
INSERT INTO screening_image VALUES ('FEELING_RECENTLY', 'Feeling recently');
INSERT INTO screening_image VALUES ('GOALS', 'Goals');
INSERT INTO screening_image VALUES ('KEEP_GOING', 'Keep going');
INSERT INTO screening_image VALUES ('NEXT_APPOINTMENT_SCHEDULED', 'Next appointment scheduled');
INSERT INTO screening_image VALUES ('RESOURCES', 'Resources');
INSERT INTO screening_image VALUES ('SAFETY', 'Safety');
INSERT INTO screening_image VALUES ('SCREENING_COMPLETE', 'Screening complete');
INSERT INTO screening_image VALUES ('SCREENING_TO_DO', 'Screening to-do');
INSERT INTO screening_image VALUES ('WELCOME', 'Welcome');

-- Prompt usable in a screening - might be before a question is asked, or before the screening session is officially marked "complete"
CREATE TABLE screening_confirmation_prompt (
	screening_confirmation_prompt_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	screening_image_id TEXT REFERENCES screening_image,
	text TEXT NOT NULL,
	action_text TEXT NOT NULL,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON screening_confirmation_prompt FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

-- If this is non-null, application should block transitions to "complete" status pending acceptance of this confirmation prompt
ALTER TABLE screening_flow_version ADD COLUMN pre_completion_screening_confirmation_prompt_id UUID REFERENCES screening_confirmation_prompt(screening_confirmation_prompt_id);

-- If this is non-null, front-end should display the prompt prior to showing the question
ALTER TABLE screening_question ADD COLUMN pre_question_screening_confirmation_prompt_id UUID REFERENCES screening_confirmation_prompt(screening_confirmation_prompt_id);

COMMIT;