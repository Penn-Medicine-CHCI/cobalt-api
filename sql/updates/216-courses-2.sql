BEGIN;
SELECT _v.register_patch('216-courses-2', NULL, NULL);

-- Temporarily drop footprint trigger so we don't insert a lot of unnecessary footprint data
DROP TRIGGER screening_answer_footprint ON screening_answer;

--Add column answer_order to track the order that answers are provided

CREATE OR REPLACE FUNCTION screening_answer_option_id_only_one_valid(screeningAnswerOptionId UUID, screeningSessionAnsweredScreeningQuestionId UUID, valid BOOLEAN) RETURNS BOOLEAN AS $$
BEGIN
    RETURN TRUE;
END
$$ LANGUAGE PLPGSQL;

ALTER TABLE screening_answer DROP CONSTRAINT IF EXISTS screening_answer_option_id_unique;

ALTER TABLE screening_answer
ADD CONSTRAINT screening_answer_option_id_unique
CHECK (screening_answer_option_id_only_one_valid(screening_answer_option_id, screening_session_answered_screening_question_id, valid));

ALTER TABLE screening_answer ADD COLUMN answer_order INTEGER NOT NULL DEFAULT 1;

CREATE OR REPLACE FUNCTION screening_answer_option_id_only_one_valid(screeningAnswerOptionId UUID, screeningSessionAnsweredScreeningQuestionId UUID, valid BOOLEAN) RETURNS BOOLEAN AS $$
BEGIN
  IF (valid = false) THEN
    RETURN TRUE;
  END IF;

  RETURN
      NOT EXISTS(
      SELECT sa.screening_answer_option_id, ssasq.screening_session_screening_id, count(*)
      FROM screening_answer sa, screening_session_answered_screening_question ssasq
      WHERE ssasq.screening_session_answered_screening_question_id=sa.screening_session_answered_screening_question_id
      AND sa.screening_answer_option_id = screeningAnswerOptionId
      AND  ssasq.screening_session_screening_id =
      (SELECT ssasq2.screening_session_screening_id
         FROM screening_session_answered_screening_question ssasq2
        WHERE ssasq2.screening_session_answered_screening_question_id = screeningSessionAnsweredScreeningQuestionId)
      AND sa.valid = TRUE
      GROUP BY sa.screening_answer_option_id, ssasq.screening_session_screening_id
      HAVING COUNT(*) > 0
   );
END
$$ LANGUAGE PLPGSQL;

CREATE OR REPLACE VIEW v_screening_answer
AS
SELECT screening_answer_id,
    screening_answer_option_id,
    screening_session_answered_screening_question_id,
    created_by_account_id,
    text,
    valid,
    created,
    last_updated,
    answer_order
   FROM screening_answer
  WHERE valid = true;

CREATE OR REPLACE VIEW v_course_unit_downloadable_file
AS
SELECT cu.*, fi.url, fi.filename, fi.content_type, fi.filesize
FROM course_unit_downloadable_file cu, file_upload fi
WHERE cu.file_upload_id = fi.file_upload_id;

-- Restore footprint trigger on screening_answer
CREATE TRIGGER screening_answer_footprint AFTER INSERT OR UPDATE OR DELETE ON screening_answer FOR EACH ROW EXECUTE PROCEDURE perform_footprint();

COMMIT;

