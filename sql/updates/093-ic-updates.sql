BEGIN;
SELECT _v.register_patch('093-ic-updates', NULL, NULL);

-- As we progress through a specific screening during a screening session,
-- we might need to mark questions as "inapplicable" - e.g. "If answered 'yes' to question 2, then skip questions 3 and 4 and go to question 5".
--
-- This is a different concept than screening_session_answered_screening_question, which is used
-- to express that a question was actually answered, even if the "answer" was to skip or (for example) select zero options from a multiple choice
CREATE TABLE screening_session_inapplicable_screening_question (
  screening_session_inapplicable_screening_question_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	screening_session_screening_id UUID NOT NULL REFERENCES screening_session_screening,
	screening_question_id UUID NOT NULL REFERENCES screening_question,
	valid BOOLEAN NOT NULL DEFAULT TRUE, -- Similar to other screening tables, this "mark as inapplicable" can be invalidates if a user goes back and answers a question differently, potentially going down a different path/decision tree
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON screening_session_inapplicable_screening_question FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE VIEW v_screening_session_inapplicable_screening_question AS
SELECT *
FROM screening_session_inapplicable_screening_question
WHERE valid=TRUE;

-- Add Psychosis
INSERT INTO screening_type (screening_type_id, description) VALUES ('PSYCHOSIS', 'Psychosis');

-- Add C-SSRS-8.  Rename old C-SSRS to C-SSRS-3.
INSERT INTO screening_type (screening_type_id, description) VALUES ('C_SSRS_8', 'C-SSRS-8');
INSERT INTO screening_type (screening_type_id, description) VALUES ('C_SSRS_3', 'C-SSRS-3');

-- Migrate old data to use the new ID
UPDATE screening_version SET screening_type_id='C_SSRS_3' WHERE screening_type_id='C_SSRS';
UPDATE screening_flow_version_screening_type SET screening_type_id='C_SSRS_3' WHERE screening_type_id='C_SSRS';

-- Delete the old ID now that no one is using it
DELETE FROM screening_type WHERE screening_type_id='C_SSRS';

COMMIT;