BEGIN;
SELECT _v.register_patch(
	'264-local-only-cobalt-employer-onboarding',
	ARRAY['262-local-only-provider-booking-seed'],
	NULL
);

-- Local/bootstrap-only employer-onboarding fixture for the COBALT testing
-- institution. Real tenants receive their own reviewed configuration patch.
-- Nothing in the production update chain depends on this patch.

DO $$
DECLARE
	v_institution_id CONSTANT TEXT := 'COBALT';
	v_screening_id CONSTANT UUID := 'c0ba1700-0000-4000-8000-000000000001';
	v_screening_version_id CONSTANT UUID := 'c0ba1700-0000-4000-8000-000000000002';
	v_screening_flow_id CONSTANT UUID := 'c0ba1700-0000-4000-8000-000000000003';
	v_screening_flow_version_id CONSTANT UUID := 'c0ba1700-0000-4000-8000-000000000004';
	v_question_id CONSTANT UUID := 'c0ba1700-0000-4000-8000-000000000005';
	v_intro_prompt_id CONSTANT UUID := 'c0ba1700-0000-4000-8000-000000000006';
	v_completion_prompt_id CONSTANT UUID := 'c0ba1700-0000-4000-8000-000000000007';
	v_decline_answer_option_id CONSTANT UUID := 'c0ba1700-0000-4000-8000-000000000008';
	v_created_by_account_id UUID;
	v_existing_onboarding_screening_flow_id UUID;
	v_location_count INTEGER;
	v_scoring_function TEXT;
BEGIN
	-- Fail loudly if this local-only patch is invoked without its bootstrap data;
	-- silently registering an empty fixture would make a later retry impossible.
	IF NOT EXISTS (
		SELECT 1
		FROM institution
		WHERE institution_id=v_institution_id
	) THEN
		RAISE EXCEPTION 'The COBALT institution is required to configure local employer onboarding.';
	END IF;

	SELECT onboarding_screening_flow_id
	INTO v_existing_onboarding_screening_flow_id
	FROM institution
	WHERE institution_id=v_institution_id;

	IF v_existing_onboarding_screening_flow_id IS NOT NULL
		AND v_existing_onboarding_screening_flow_id <> v_screening_flow_id THEN
		RAISE EXCEPTION 'COBALT already references unexpected onboarding screening flow ID %.',
			v_existing_onboarding_screening_flow_id;
	END IF;

	SELECT COUNT(*)
	INTO v_location_count
	FROM institution_location
	WHERE institution_id=v_institution_id;

	IF v_location_count = 0 THEN
		RAISE EXCEPTION 'COBALT requires at least one institution location for employer onboarding.';
	END IF;

	SELECT account_id
	INTO v_created_by_account_id
	FROM account
	WHERE institution_id=v_institution_id
	AND role_id='ADMINISTRATOR'
	AND active=TRUE
	AND LOWER(email_address)=LOWER('admin@cobaltinnovations.org')
	ORDER BY account_id
	LIMIT 1;

	IF v_created_by_account_id IS NULL THEN
		RAISE EXCEPTION 'The COBALT administrator account is required to configure employer onboarding.';
	END IF;

	INSERT INTO screening_confirmation_prompt (
		screening_confirmation_prompt_id,
		screening_image_id,
		title_text,
		text,
		action_text
	) VALUES
		(v_intro_prompt_id, 'WELCOME', 'Welcome to Cobalt!',
			'To help connect you with the benefits and resources available to you, please tell us who your employer is.',
			'Continue'),
		(v_completion_prompt_id, 'SCREENING_COMPLETE', 'Thank you!',
			'You''re all set to use Cobalt.', 'Done');

	INSERT INTO screening (
		screening_id,
		name,
		active_screening_version_id,
		created_by_account_id
	) VALUES (
		v_screening_id,
		'COBALT Employer Onboarding',
		NULL,
		v_created_by_account_id
	);

	v_scoring_function := FORMAT($scoring$
const selectedAnswerIds = input.screeningAnswerIdsByScreeningQuestionId['%s'] || [];
output.completed = selectedAnswerIds.length === 1;
output.score = { overallScore: output.completed ? 1 : 0 };
output.belowScoringThreshold = !output.completed;
output.nextScreeningQuestionId = output.completed ? null : '%s';
$scoring$, v_question_id, v_question_id);

	INSERT INTO screening_version (
		screening_version_id,
		screening_id,
		screening_type_id,
		created_by_account_id,
		version_number,
		scoring_function
	) VALUES (
		v_screening_version_id,
		v_screening_id,
		'CUSTOM',
		v_created_by_account_id,
		1,
		v_scoring_function
	);

	UPDATE screening
	SET active_screening_version_id=v_screening_version_id
	WHERE screening_id=v_screening_id;

	INSERT INTO screening_institution (screening_id, institution_id)
	VALUES (v_screening_id, v_institution_id);

	INSERT INTO screening_question (
		screening_question_id,
		screening_version_id,
		pre_question_screening_confirmation_prompt_id,
		screening_answer_format_id,
		screening_answer_content_hint_id,
		question_text,
		footer_text,
		minimum_answer_count,
		maximum_answer_count,
		display_order,
		prefer_autosubmit,
		screening_question_submission_style_id,
		metadata
	) VALUES (
		v_question_id,
		v_screening_version_id,
		v_intro_prompt_id,
		'SINGLE_SELECT',
		'NONE',
		'Please select your employer',
		'Cobalt uses your employer to personalize your experience and identify the benefits and services available to you. We do not share your individual response with your employer, manager, or coworkers.',
		1,
		1,
		1,
		FALSE,
		'SUBMIT',
		'{"shouldUpdateAccountInstitutionLocation":true,"submitButtonText":"Done"}'::JSONB
	);

	-- Each screening version is a snapshot of the employer list. Future location
	-- changes should be published as a new screening and flow version.
	INSERT INTO screening_answer_option (
		screening_answer_option_id,
		screening_question_id,
		answer_option_text,
		score,
		indicates_crisis,
		freeform_supplement,
		display_order,
		metadata
	)
	SELECT
		uuid_generate_v4(),
		v_question_id,
		institution_location.name,
		1,
		FALSE,
		FALSE,
		ROW_NUMBER() OVER (ORDER BY institution_location.display_order, institution_location.institution_location_id),
		JSONB_BUILD_OBJECT('institutionLocationId', institution_location.institution_location_id::TEXT)
	FROM institution_location
	WHERE institution_location.institution_id=v_institution_id
	ORDER BY institution_location.display_order, institution_location.institution_location_id;

	INSERT INTO screening_answer_option (
		screening_answer_option_id,
		screening_question_id,
		answer_option_text,
		score,
		indicates_crisis,
		freeform_supplement,
		display_order,
		metadata
	) VALUES (
		v_decline_answer_option_id,
		v_question_id,
		'I''m not sure / I''d rather not say',
		1,
		FALSE,
		FALSE,
		v_location_count + 1,
		'{"declinesInstitutionLocation":true}'::JSONB
	);

	INSERT INTO screening_flow (
		screening_flow_id,
		institution_id,
		active_screening_flow_version_id,
		screening_flow_type_id,
		created_by_account_id,
		name
	) VALUES (
		v_screening_flow_id,
		v_institution_id,
		NULL,
		'ONBOARDING',
		v_created_by_account_id,
		'COBALT Employer Onboarding'
	);

	INSERT INTO screening_flow_version (
		screening_flow_version_id,
		screening_flow_id,
		initial_screening_id,
		pre_completion_screening_confirmation_prompt_id,
		phone_number_required,
		version_number,
		skippable,
		orchestration_function,
		results_function,
		destination_function,
		created_by_account_id
	) VALUES (
		v_screening_flow_version_id,
		v_screening_flow_id,
		v_screening_id,
		v_completion_prompt_id,
		FALSE,
		1,
		FALSE,
		$orchestration$
const screeningSessionScreening = (input.screeningSessionScreenings || [])[0];
const screeningResults = screeningSessionScreening
  ? (input.screeningResultsByScreeningSessionScreeningId[screeningSessionScreening.screeningSessionScreeningId] || [])
  : [];

output.completed = screeningSessionScreening ? Boolean(screeningSessionScreening.completed) : false;
output.crisisIndicated = screeningResults.some((screeningResult) => {
  return (screeningResult.screeningResponses || []).some((screeningResponse) => {
    return screeningResponse.screeningAnswerOption && screeningResponse.screeningAnswerOption.indicatesCrisis;
  });
});
$orchestration$,
		$results$
output.supportRoleRecommendations = [];
output.recommendLegacyContentAnswerIds = false;
output.legacyContentAnswerIds = [];
output.recommendedTagIds = [];
output.recommendedFeatureIds = [];
output.integratedCareTriages = [];
$results$,
		$destination$
output.screeningSessionDestinationId = null;
output.context = {};
$destination$,
		v_created_by_account_id
	);

	UPDATE screening_flow
	SET active_screening_flow_version_id=v_screening_flow_version_id
	WHERE screening_flow_id=v_screening_flow_id;

	UPDATE institution
	SET onboarding_screening_flow_id=v_screening_flow_id
	WHERE institution_id=v_institution_id;
END;
$$;

COMMIT;
