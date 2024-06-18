BEGIN;
SELECT _v.register_patch('175-screening-session-constraints', NULL, NULL);

CREATE UNIQUE INDEX idx_screening_session_answered_screening_question_valid
ON screening_session_answered_screening_question (screening_session_screening_id, screening_question_id)
WHERE valid IS TRUE;

CREATE OR REPLACE FUNCTION screening_answer_option_id_only_one_valid(screeningAnswerOptionId UUID, screeningSessionAnsweredScreeningQuestionId UUID, valid BOOLEAN) RETURNS BOOLEAN AS $$
BEGIN
    RETURN TRUE;
END
$$ LANGUAGE PLPGSQL;

ALTER TABLE screening_answer DROP CONSTRAINT IF EXISTS screening_answer_option_id_unique;

ALTER TABLE screening_answer
ADD CONSTRAINT screening_answer_option_id_unique
CHECK (screening_answer_option_id_only_one_valid(screening_answer_option_id, screening_session_answered_screening_question_id, valid));

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

CREATE INDEX idx_screening_answer_screening_answer_option_id ON screening_answer (screening_answer_option_id);
CREATE INDEX idx_screening_answer_screening_session_answered_screening_question_id ON screening_answer (screening_session_answered_screening_question_id);

COMMIT;