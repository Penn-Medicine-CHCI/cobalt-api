BEGIN;
SELECT _v.register_patch('266-local-only-cobalt-employer-onboarding-single-question', ARRAY['264-local-only-cobalt-employer-onboarding'], NULL);

-- Remove presentation prompts without disturbing answers or existing sessions.
UPDATE screening_question
SET pre_question_screening_confirmation_prompt_id = NULL
WHERE screening_question_id = 'c0ba1700-0000-4000-8000-000000000005';

UPDATE screening_flow_version
SET pre_completion_screening_confirmation_prompt_id = NULL
WHERE screening_flow_version_id = 'c0ba1700-0000-4000-8000-000000000004';

COMMIT;
