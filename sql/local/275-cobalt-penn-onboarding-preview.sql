-- Local COBALT preview of the PENN two-level employer onboarding flow.
-- Publish new screening and flow versions so a completed older version does
-- not suppress this prompt. Historical versions and sessions are preserved.
BEGIN;
SELECT _v.register_patch(
  '275-local-only-cobalt-penn-onboarding-preview',
  ARRAY['274-local-only-cobalt-penn-location-preview'],
  NULL
);

DO $publish$
DECLARE
  v_screening_id CONSTANT uuid := 'c0ba1700-0000-4000-8000-000000000001';
  v_flow_id CONSTANT uuid := 'c0ba1700-0000-4000-8000-000000000003';
  v_old_screening_version_id CONSTANT uuid := 'c0ba1700-0000-4000-8000-000000000002';
  v_old_flow_version_id CONSTANT uuid := 'c0ba1700-0000-4000-8000-000000000004';
  v_screening_version_id uuid := uuid_generate_v5(uuid_ns_url(), 'cobalt-local-preview-penn-screening-version-2');
  v_flow_version_id uuid := uuid_generate_v5(uuid_ns_url(), 'cobalt-local-preview-penn-flow-version-2');
  v_employer_question_id uuid := uuid_generate_v5(uuid_ns_url(), 'cobalt-local-preview-penn-employer-question-2');
  v_uphs_question_id uuid := uuid_generate_v5(uuid_ns_url(), 'cobalt-local-preview-penn-uphs-question-2');
  v_upenn_question_id uuid := uuid_generate_v5(uuid_ns_url(), 'cobalt-local-preview-penn-upenn-question-2');
  v_created_by_account_id uuid;
  v_scoring_function text;
BEGIN
  IF (SELECT COUNT(*) FROM institution_location il
      JOIN institution_location_group g ON g.institution_location_group_id=il.institution_location_group_id
      WHERE il.institution_id='COBALT'
        AND g.name='University of Pennsylvania Health System (UPHS)') <> 10
      OR (SELECT COUNT(*) FROM institution_location il
      JOIN institution_location_group g ON g.institution_location_group_id=il.institution_location_group_id
      WHERE il.institution_id='COBALT'
        AND g.name='University of Pennsylvania (UPenn)') <> 3 THEN
    RAISE EXCEPTION 'Apply the local PENN location preview before publishing onboarding version 2';
  END IF;

  IF EXISTS (SELECT 1 FROM screening_version WHERE screening_version_id=v_screening_version_id) THEN
    IF (SELECT active_screening_version_id FROM screening WHERE screening_id=v_screening_id) <> v_screening_version_id
      OR (SELECT active_screening_flow_version_id FROM screening_flow WHERE screening_flow_id=v_flow_id) <> v_flow_version_id THEN
      RAISE EXCEPTION 'The local onboarding version 2 exists but is not active';
    END IF;
    RETURN;
  END IF;

  IF (SELECT active_screening_version_id FROM screening WHERE screening_id=v_screening_id) <> v_old_screening_version_id
    OR (SELECT active_screening_flow_version_id FROM screening_flow WHERE screening_flow_id=v_flow_id) <> v_old_flow_version_id THEN
    RAISE EXCEPTION 'COBALT onboarding is not on the expected local version 1';
  END IF;

  SELECT created_by_account_id INTO v_created_by_account_id
  FROM screening_version WHERE screening_version_id=v_old_screening_version_id;

  v_scoring_function := FORMAT($scoring$
const employerQuestionId = '%s';
const uphsQuestionId = '%s';
const upennQuestionId = '%s';
const employerAnswerIds = input.screeningAnswerIdsByScreeningQuestionId[employerQuestionId] || [];
const employerOptions = employerAnswerIds
  .map((answerId) => input.screeningAnswerOptionsByScreeningAnswerId[answerId])
  .filter((answerOption) => answerOption);
const employerAnswered = employerOptions.length === 1;
const selectedEmployer = employerAnswered ? employerOptions[0] : null;
const nextQuestionId = selectedEmployer && selectedEmployer.metadata
  ? String(selectedEmployer.metadata.nextScreeningQuestionId || '') : '';
const validBranchQuestionId = nextQuestionId === uphsQuestionId || nextQuestionId === upennQuestionId
  ? nextQuestionId : null;
const branchAnswerIds = validBranchQuestionId
  ? (input.screeningAnswerIdsByScreeningQuestionId[validBranchQuestionId] || []) : [];
const branchAnswered = !validBranchQuestionId || branchAnswerIds.length === 1;

output.completed = employerAnswered && branchAnswered;
output.score = { overallScore: output.completed ? 1 : 0 };
output.belowScoringThreshold = !output.completed;
output.nextScreeningQuestionId = !employerAnswered
  ? employerQuestionId
  : (branchAnswered ? null : validBranchQuestionId);
$scoring$, v_employer_question_id, v_uphs_question_id, v_upenn_question_id);

  INSERT INTO screening_version (
    screening_version_id, screening_id, screening_type_id,
    created_by_account_id, version_number, scoring_function
  ) VALUES (
    v_screening_version_id, v_screening_id, 'CUSTOM',
    v_created_by_account_id, 2, v_scoring_function
  );

  INSERT INTO screening_question (
    screening_question_id, screening_version_id, screening_answer_format_id,
    screening_answer_content_hint_id, question_text, footer_text,
    minimum_answer_count, maximum_answer_count, display_order,
    prefer_autosubmit, screening_question_submission_style_id, metadata
  ) VALUES
    (v_employer_question_id, v_screening_version_id, 'SINGLE_SELECT', 'NONE',
      'Please select your employer',
      'Cobalt uses your employer to personalize your experience and identify the benefits and services available to you. We do not share your individual response with your employer, manager, or coworkers.',
      1, 1, 1, TRUE, 'NEXT', '{"footerCallout":{"displayTypeId":"PRIMARY","title":"How we use this information"}}'::jsonb),
    (v_uphs_question_id, v_screening_version_id, 'SINGLE_SELECT', 'NONE',
      'Where do you work at UPHS?', NULL,
      1, 1, 2, TRUE, 'NEXT', '{"shouldUpdateAccountInstitutionLocation":true}'::jsonb),
    (v_upenn_question_id, v_screening_version_id, 'SINGLE_SELECT', 'NONE',
      'Where do you work at UPenn?', NULL,
      1, 1, 3, TRUE, 'NEXT', '{"shouldUpdateAccountInstitutionLocation":true}'::jsonb);

  INSERT INTO screening_answer_option (
    screening_answer_option_id, screening_question_id, answer_option_text,
    score, indicates_crisis, freeform_supplement, display_order, metadata
  ) VALUES
    (uuid_generate_v5(uuid_ns_url(), 'cobalt-local-preview-penn-employer-uphs-2'),
      v_employer_question_id, 'University of Pennsylvania Health System (UPHS)',
      1, FALSE, FALSE, 1, JSONB_BUILD_OBJECT('nextScreeningQuestionId', v_uphs_question_id::text)),
    (uuid_generate_v5(uuid_ns_url(), 'cobalt-local-preview-penn-employer-upenn-2'),
      v_employer_question_id, 'University of Pennsylvania (UPenn)',
      1, FALSE, FALSE, 2, JSONB_BUILD_OBJECT('nextScreeningQuestionId', v_upenn_question_id::text)),
    (uuid_generate_v5(uuid_ns_url(), 'cobalt-local-preview-penn-employer-decline-2'),
      v_employer_question_id, 'I''m not sure / I''d rather not say',
      1, FALSE, FALSE, 3, '{}'::jsonb);

  INSERT INTO screening_answer_option (
    screening_answer_option_id, screening_question_id, answer_option_text,
    score, indicates_crisis, freeform_supplement, display_order, metadata
  )
  SELECT uuid_generate_v5(uuid_ns_url(), 'cobalt-local-preview-penn-leaf-' || il.institution_location_id::text),
    CASE WHEN g.name='University of Pennsylvania Health System (UPHS)'
      THEN v_uphs_question_id ELSE v_upenn_question_id END,
    il.name, 1, FALSE, FALSE, il.display_order,
    JSONB_BUILD_OBJECT('institutionLocationId', il.institution_location_id::text)
  FROM institution_location il
  JOIN institution_location_group g ON g.institution_location_group_id=il.institution_location_group_id
  WHERE il.institution_id='COBALT'
    AND g.name IN ('University of Pennsylvania Health System (UPHS)', 'University of Pennsylvania (UPenn)');

  INSERT INTO screening_flow_version (
    screening_flow_version_id, screening_flow_id, initial_screening_id,
    pre_completion_screening_confirmation_prompt_id,
    screening_flow_skip_type_id, phone_number_required, skippable,
    version_number, initialization_function, orchestration_function,
    results_function, destination_function, created_by_account_id,
    minutes_until_retake, recommendation_expiration_minutes
  )
  SELECT v_flow_version_id, screening_flow_id, initial_screening_id,
    NULL, screening_flow_skip_type_id, phone_number_required, skippable,
    version_number + 1, initialization_function, orchestration_function,
    results_function, destination_function, created_by_account_id,
    minutes_until_retake, recommendation_expiration_minutes
  FROM screening_flow_version WHERE screening_flow_version_id=v_old_flow_version_id;

  INSERT INTO screening_flow_version_account_source (
    screening_flow_version_id, account_source_id, display_order
  )
  SELECT v_flow_version_id, account_source_id, display_order
  FROM screening_flow_version_account_source
  WHERE screening_flow_version_id=v_old_flow_version_id;

  INSERT INTO screening_flow_version_screening_type (
    screening_flow_version_id, screening_type_id
  )
  SELECT v_flow_version_id, screening_type_id
  FROM screening_flow_version_screening_type
  WHERE screening_flow_version_id=v_old_flow_version_id;

  UPDATE screening SET active_screening_version_id=v_screening_version_id
  WHERE screening_id=v_screening_id;
  UPDATE screening_flow SET active_screening_flow_version_id=v_flow_version_id
  WHERE screening_flow_id=v_flow_id;
END
$publish$;

-- Keep reruns aligned with the local preview, including when v2 already exists.
UPDATE screening_question
SET screening_question_submission_style_id='NEXT'
WHERE screening_version_id=uuid_generate_v5(uuid_ns_url(), 'cobalt-local-preview-penn-screening-version-2')
  AND question_text IN (
    'Please select your employer',
    'Where do you work at UPHS?',
    'Where do you work at UPenn?'
  );

UPDATE screening_question
SET metadata=JSONB_SET(
  COALESCE(metadata, '{}'::jsonb),
  '{footerCallout}',
  JSONB_BUILD_OBJECT('displayTypeId', 'PRIMARY', 'title', 'How we use this information')
)
WHERE screening_question_id=uuid_generate_v5(uuid_ns_url(), 'cobalt-local-preview-penn-employer-question-2');

UPDATE account_source
SET onboarding_screening_presentation_id='SMALL_MODAL'
WHERE account_source_id IN ('EMAIL_PASSWORD', 'ANONYMOUS', 'ANONYMOUS_IMPLICIT');

UPDATE institution_url
SET url='http://localhost:3002'
WHERE institution_id='COBALT' AND url='http://localhost:3000' AND preferred=TRUE;

COMMIT;
