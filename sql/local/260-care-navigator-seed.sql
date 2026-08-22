BEGIN;
SELECT _v.register_patch(
	'260-local-only-care-navigator-seed',
	ARRAY[
		'259-local-only-provider-booking-seed',
		'260-care-navigator'
	],
	NULL
);

-- Local/bootstrap-only Care Navigator fixture. Production receives the support
-- role, capability type, and feature mapping from 260-care-navigator.sql, but
-- never receives this account, provider, availability, or screening data.
DO $$
DECLARE
	v_institution_id CONSTANT TEXT := 'COBALT';
	v_account_email_address CONSTANT TEXT := 'care-navigator@cobaltinnovations.org';
	v_account_password_hash CONSTANT TEXT := '$2a$10$M2tPoJ8eQr55OW4iOfpbBOpgqFWt0LxnvVBnW1a/1LhKNA6SuUN42';
	v_provider_name CONSTANT TEXT := 'Care Navigator';
	v_provider_url_name CONSTANT TEXT := 'cobalt-care-navigator';
	v_appointment_type_name CONSTANT TEXT := 'Care Navigation Consultation';
	v_provider_bio CONSTANT TEXT := 'Our Care Navigator is here to help you identify and connect with mental health and wellness resources that best fit your needs. During the video call, they''ll listen to your concerns, answer questions about available benefits and services, and help connect you with resources.';
	v_provider_description CONSTANT TEXT := 'Our Care Navigator is here to help you identify and connect with mental health and wellness resources that best fit your needs.';
	v_appointment_type_description CONSTANT TEXT := 'Your appointment is a 30 minute video call with a Care Navigator to discuss potential resources.';
	v_screening_name CONSTANT TEXT := 'Care Navigator Booking Assessment';
	v_screening_flow_name CONSTANT TEXT := 'Care Navigator Booking Intake';
	v_navigation_question_text CONSTANT TEXT := 'What would you like help navigating?';
	v_support_question_text CONSTANT TEXT := 'What type of support would be most useful right now?';
	v_follow_up_question_text CONSTANT TEXT := 'How would you prefer your Care Navigator to follow up?';
	v_context_question_text CONSTANT TEXT := 'Is there anything else you would like your Care Navigator to know?';
	v_provider_details_html CONSTANT TEXT := $details_html$
<section class="mb-8">
  <h2 class="mb-4">What is a Care Navigator</h2>
  <p class="mb-4 fs-large">Our Care Navigator is here to help you identify and connect with mental health and wellness resources that best fit your needs. During the video call, they'll listen to your concerns, answer questions about available benefits and services, and help connect you with resources.</p>
  <p class="mb-4 fs-large"><strong>Care Navigators are not licensed clinicians and do not provide medical or mental health treatment, diagnoses, therapy, or clinical recommendations.</strong> Their role is to help you understand your options and navigate available resources.</p>
  <p class="mb-2 fs-large">Below are some examples of how a Care Navigator can help:</p>
  <ul class="mb-4 fs-large">
    <li>Connect you with free, rapid access benefits like your Employee Assistance Program (EAP)</li>
    <li>Navigate available services for dependents</li>
    <li>Identify support groups and wellness resources</li>
    <li>Compare available behavioral health services</li>
    <li>Assist in navigating Cobalt website</li>
  </ul>
  <p class="mb-4 fs-large"><strong>Whether you're looking for care for yourself or someone you care about, a Care Navigator can help you identify resources and determine the next best steps.</strong></p>
  <p class="mb-4 fs-large">Care Navigator conversations are not intended to address emergency or crisis situations. <strong>If you are experiencing a medical or mental health emergency, or are concerned about your immediate safety or the safety of someone else, please call 911 or 988 immediately.</strong> Penn Medicine employees may also contact the EAP 24/7 Crisis Line.</p>
  <p class="mb-0 fs-large">Your privacy is important to us. When you schedule with a Care Navigator, we'll ask for your name and email address so we can contact you about your call and provide Care Navigator services. Your information is accessible only to authorized Penn Cobalt personnel and is protected in accordance with applicable privacy laws, including HIPAA when applicable. We only use or share your information as needed to provide services or as required by law. Any information shared is not documented in PennChart or linked to from your electronic health record.</p>
</section>
$details_html$;

	v_fixture_account_id CONSTANT UUID := 'ca4e0000-0000-4000-8000-000000000001';
	v_fixture_provider_id CONSTANT UUID := 'ca4e0000-0000-4000-8000-000000000002';
	v_appointment_type_id CONSTANT UUID := 'ca4e0000-0000-4000-8000-000000000003';
	v_logical_availability_id CONSTANT UUID := 'ca4e0000-0000-4000-8000-000000000004';
	v_screening_id CONSTANT UUID := 'ca4e0000-0000-4000-8000-000000000005';
	v_screening_version_id CONSTANT UUID := 'ca4e0000-0000-4000-8000-000000000006';
	v_screening_question_id CONSTANT UUID := 'ca4e0000-0000-4000-8000-000000000007';
	v_answer_option_provider_id CONSTANT UUID := 'ca4e0000-0000-4000-8000-000000000008';
	v_answer_option_options_id CONSTANT UUID := 'ca4e0000-0000-4000-8000-000000000009';
	v_answer_option_other_id CONSTANT UUID := 'ca4e0000-0000-4000-8000-00000000000a';
	v_screening_flow_id CONSTANT UUID := 'ca4e0000-0000-4000-8000-00000000000b';
	v_screening_flow_version_id CONSTANT UUID := 'ca4e0000-0000-4000-8000-00000000000c';
	v_institution_feature_id CONSTANT UUID := 'ca4e0000-0000-4000-8000-00000000000d';
	v_provider_location_id CONSTANT UUID := 'ca4e0000-0000-4000-8000-00000000000e';
	v_support_question_id CONSTANT UUID := 'ca4e0000-0000-4000-8000-00000000000f';
	v_support_provider_option_id CONSTANT UUID := 'ca4e0000-0000-4000-8000-000000000010';
	v_support_benefits_option_id CONSTANT UUID := 'ca4e0000-0000-4000-8000-000000000011';
	v_support_preparation_option_id CONSTANT UUID := 'ca4e0000-0000-4000-8000-000000000012';
	v_follow_up_question_id CONSTANT UUID := 'ca4e0000-0000-4000-8000-000000000013';
	v_follow_up_email_option_id CONSTANT UUID := 'ca4e0000-0000-4000-8000-000000000014';
	v_follow_up_phone_option_id CONSTANT UUID := 'ca4e0000-0000-4000-8000-000000000015';
	v_follow_up_either_option_id CONSTANT UUID := 'ca4e0000-0000-4000-8000-000000000016';
	v_context_question_id CONSTANT UUID := 'ca4e0000-0000-4000-8000-000000000017';
	v_context_answer_option_id CONSTANT UUID := 'ca4e0000-0000-4000-8000-000000000018';

	v_account_id UUID;
	v_provider_id UUID;
	v_scoring_function TEXT;
	v_orchestration_function CONSTANT TEXT := $orchestration$
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
$orchestration$;
	v_results_function CONSTANT TEXT := $results$
output.supportRoleRecommendations = [];
output.recommendLegacyContentAnswerIds = false;
output.legacyContentAnswerIds = [];
output.recommendedTagIds = [];
output.recommendedFeatureIds = [];
output.integratedCareTriages = [];
$results$;
	v_destination_function CONSTANT TEXT := $destination$
const screeningSessionScreening = (input.screeningSessionScreenings || [])[0];
const belowScoringThreshold = screeningSessionScreening
  ? Boolean(screeningSessionScreening.belowScoringThreshold)
  : true;

output.screeningSessionDestinationId = null;
output.context = {};

if (input.screeningSession.completed) {
  output.screeningSessionDestinationId = 'APPOINTMENT_BOOKING_CONFIRMATION';
  output.context.result = belowScoringThreshold ? 'FAILURE' : 'SUCCESS';
}
$destination$;
BEGIN
	-- Reuse a matching local row when one already exists, while retaining fixed
	-- UUIDs for clean database recreations.
	SELECT provider_id
	INTO v_provider_id
	FROM provider
	WHERE institution_id=v_institution_id
	AND LOWER(url_name)=LOWER(v_provider_url_name)
	ORDER BY provider_id
	LIMIT 1;

	IF v_provider_id IS NULL THEN
		v_provider_id := v_fixture_provider_id;

		INSERT INTO provider (
			provider_id,
			institution_id,
			name,
			title,
			entity,
			clinic,
			specialty,
			email_address,
			image_url,
			bio_url,
			website_url,
			locale,
			time_zone,
			active,
			scheduling_system_id,
			videoconference_platform_id,
			system_affinity_id,
			url_name,
			bio,
			description,
			tags,
			phone_number,
			display_phone_number_only_for_booking,
			details_html
		) VALUES (
			v_provider_id,
			v_institution_id,
			v_provider_name,
			'Care Navigator',
			'Cobalt',
			'Cobalt Care Navigation',
			'Care Navigation',
			v_account_email_address,
			'https://placehold.co/320x320/png?text=Care+Navigator',
			'https://fixtures.cobalt.care/providers/cobalt-care-navigator/bio',
			'https://fixtures.cobalt.care/providers/cobalt-care-navigator',
			'en-US',
			'America/New_York',
			TRUE,
			'COBALT',
			'SWITCHBOARD',
			'COBALT',
			v_provider_url_name,
			v_provider_bio,
			v_provider_description,
			'["Provider matching", "Care options", "Mental health navigation"]',
			'+12155551014',
			FALSE,
			v_provider_details_html
		)
		ON CONFLICT (provider_id) DO NOTHING;
	ELSE
		UPDATE provider
		SET name=v_provider_name,
			title='Care Navigator',
			entity='Cobalt',
			clinic='Cobalt Care Navigation',
			specialty='Care Navigation',
			email_address=v_account_email_address,
			image_url='https://placehold.co/320x320/png?text=Care+Navigator',
			bio_url='https://fixtures.cobalt.care/providers/cobalt-care-navigator/bio',
			website_url='https://fixtures.cobalt.care/providers/cobalt-care-navigator',
			locale='en-US',
			time_zone='America/New_York',
			active=TRUE,
			scheduling_system_id='COBALT',
			videoconference_platform_id='SWITCHBOARD',
			system_affinity_id='COBALT',
			bio=v_provider_bio,
			description=v_provider_description,
			tags='["Provider matching", "Care options", "Mental health navigation"]',
			phone_number='+12155551014',
			display_phone_number_only_for_booking=FALSE,
			details_html=v_provider_details_html
		WHERE provider_id=v_provider_id;
	END IF;

	SELECT account_id
	INTO v_account_id
	FROM account
	WHERE institution_id=v_institution_id
	AND account_source_id='EMAIL_PASSWORD'
	AND active=TRUE
	AND LOWER(email_address)=LOWER(v_account_email_address)
	ORDER BY account_id
	LIMIT 1;

	IF v_account_id IS NULL THEN
		v_account_id := v_fixture_account_id;

		INSERT INTO account (
			account_id,
			role_id,
			institution_id,
			account_source_id,
			email_address,
			password,
			first_name,
			last_name,
			display_name,
			provider_id,
			locale,
			time_zone,
			active,
			test_account
		) VALUES (
			v_account_id,
			'ADMINISTRATOR',
			v_institution_id,
			'EMAIL_PASSWORD',
			v_account_email_address,
			v_account_password_hash,
			'Cobalt',
			'Care Navigator',
			v_provider_name,
			v_provider_id,
			'en-US',
			'America/New_York',
			TRUE,
			TRUE
		)
		ON CONFLICT (account_id) DO NOTHING;
	ELSE
		UPDATE account
		SET role_id='ADMINISTRATOR',
			provider_id=v_provider_id,
			password=v_account_password_hash,
			first_name='Cobalt',
			last_name='Care Navigator',
			display_name=v_provider_name,
			locale='en-US',
			time_zone='America/New_York',
			active=TRUE,
			test_account=TRUE
		WHERE account_id=v_account_id;
	END IF;

	INSERT INTO provider_support_role (provider_id, support_role_id)
	VALUES (v_provider_id, 'CARE_NAVIGATOR')
	ON CONFLICT (provider_id, support_role_id) DO NOTHING;

	INSERT INTO account_capability (account_id, account_capability_type_id)
	VALUES (v_account_id, 'NAVIGATOR')
	ON CONFLICT (account_id, account_capability_type_id) DO NOTHING;

	INSERT INTO care_navigator_provider_account (
		provider_id,
		account_id,
		display_order
	) VALUES (
		v_provider_id,
		v_account_id,
		1
	)
	ON CONFLICT (provider_id, account_id) DO UPDATE
	SET display_order=EXCLUDED.display_order;

	INSERT INTO provider_payment_type (provider_id, payment_type_id)
	SELECT v_provider_id, 'NO_FEE'
	WHERE EXISTS (
		SELECT 1
		FROM payment_type
		WHERE payment_type_id='NO_FEE'
	)
	ON CONFLICT (provider_id, payment_type_id) DO NOTHING;

	INSERT INTO provider_location (
		provider_location_id,
		provider_id,
		address_id,
		name,
		short_name,
		display_order
	) VALUES (
		v_provider_location_id,
		v_provider_id,
		NULL,
		'Cobalt Virtual Care',
		'Virtual Care',
		1
	)
	ON CONFLICT (provider_location_id) DO UPDATE
	SET provider_id=EXCLUDED.provider_id,
		address_id=EXCLUDED.address_id,
		name=EXCLUDED.name,
		short_name=EXCLUDED.short_name,
		display_order=EXCLUDED.display_order;

	INSERT INTO provider_institution_location (
		provider_id,
		institution_location_id
	)
	SELECT
		v_provider_id,
		institution_location.institution_location_id
	FROM institution_location
	WHERE institution_location.institution_id=v_institution_id
	AND institution_location.name='Cobalt Virtual Care'
	AND NOT EXISTS (
		SELECT 1
		FROM provider_institution_location existing
		WHERE existing.provider_id=v_provider_id
		AND existing.institution_location_id=institution_location.institution_location_id
	);

	-- Make the existing feature discoverable in local/bootstrap environments.
	INSERT INTO institution_feature (
		institution_feature_id,
		institution_id,
		feature_id,
		nav_description,
		description,
		display_order,
		nav_visible,
		landing_page_visible,
		treatment_description,
		provider_id
	) VALUES (
		v_institution_feature_id,
		v_institution_id,
		'RESOURCE_NAVIGATOR',
		'Connect with a Care Navigator.',
		'Find help understanding care options and connecting with a mental health provider.',
		12,
		TRUE,
		TRUE,
		'Care navigation consultations',
		v_provider_id
	)
	ON CONFLICT (institution_id, feature_id) DO UPDATE
	SET nav_description=EXCLUDED.nav_description,
		description=EXCLUDED.description,
		display_order=EXCLUDED.display_order,
		nav_visible=EXCLUDED.nav_visible,
		landing_page_visible=EXCLUDED.landing_page_visible,
		treatment_description=EXCLUDED.treatment_description,
		provider_id=EXCLUDED.provider_id;

	v_scoring_function := FORMAT($scoring$
const questionIds = ['%s', '%s', '%s', '%s'];
const nextUnansweredQuestionId = questionIds.find((questionId) => {
  const selectedAnswerIds = input.screeningAnswerIdsByScreeningQuestionId[questionId] || [];
  return selectedAnswerIds.length === 0;
});

output.completed = nextUnansweredQuestionId === undefined;
output.score = { overallScore: output.completed ? 1 : 0 };
output.belowScoringThreshold = !output.completed;
output.nextScreeningQuestionId = output.completed ? null : nextUnansweredQuestionId;
$scoring$, v_screening_question_id, v_support_question_id, v_follow_up_question_id, v_context_question_id);

	INSERT INTO screening (
		screening_id,
		name,
		active_screening_version_id,
		created_by_account_id
	) VALUES (
		v_screening_id,
		v_screening_name,
		NULL,
		v_account_id
	)
	ON CONFLICT (screening_id) DO UPDATE
	SET name=EXCLUDED.name,
		created_by_account_id=EXCLUDED.created_by_account_id;

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
		v_account_id,
		1,
		v_scoring_function
	)
	ON CONFLICT (screening_version_id) DO UPDATE
	SET screening_id=EXCLUDED.screening_id,
		screening_type_id=EXCLUDED.screening_type_id,
		created_by_account_id=EXCLUDED.created_by_account_id,
		version_number=EXCLUDED.version_number,
		scoring_function=EXCLUDED.scoring_function;

	UPDATE screening
	SET active_screening_version_id=v_screening_version_id
	WHERE screening_id=v_screening_id;

	INSERT INTO screening_institution (screening_id, institution_id)
	VALUES (v_screening_id, v_institution_id)
	ON CONFLICT (screening_id, institution_id) DO NOTHING;

	INSERT INTO screening_question (
		screening_question_id,
		screening_version_id,
		screening_answer_format_id,
		screening_answer_content_hint_id,
		question_text,
		minimum_answer_count,
		maximum_answer_count,
		display_order,
		prefer_autosubmit,
		screening_question_submission_style_id
	) VALUES
		(v_screening_question_id, v_screening_version_id, 'SINGLE_SELECT', 'NONE',
			v_navigation_question_text, 1, 1, 1, TRUE, 'NEXT'),
		(v_support_question_id, v_screening_version_id, 'MULTI_SELECT', 'NONE',
			v_support_question_text, 1, 3, 2, FALSE, 'NEXT'),
		(v_follow_up_question_id, v_screening_version_id, 'SINGLE_SELECT', 'NONE',
			v_follow_up_question_text, 1, 1, 3, TRUE, 'NEXT'),
		(v_context_question_id, v_screening_version_id, 'FREEFORM_TEXT', 'NONE',
			v_context_question_text, 1, 1, 4, FALSE, 'NEXT')
	ON CONFLICT (screening_question_id) DO UPDATE
	SET screening_version_id=EXCLUDED.screening_version_id,
		screening_answer_format_id=EXCLUDED.screening_answer_format_id,
		screening_answer_content_hint_id=EXCLUDED.screening_answer_content_hint_id,
		question_text=EXCLUDED.question_text,
		minimum_answer_count=EXCLUDED.minimum_answer_count,
		maximum_answer_count=EXCLUDED.maximum_answer_count,
		display_order=EXCLUDED.display_order,
		prefer_autosubmit=EXCLUDED.prefer_autosubmit,
		screening_question_submission_style_id=EXCLUDED.screening_question_submission_style_id;

	INSERT INTO screening_answer_option (
		screening_answer_option_id,
		screening_question_id,
		answer_option_text,
		score,
		indicates_crisis,
		display_order
	) VALUES
		(v_answer_option_provider_id, v_screening_question_id, 'Finding a mental health provider', 1, FALSE, 1),
		(v_answer_option_options_id, v_screening_question_id, 'Understanding care options', 1, FALSE, 2),
		(v_answer_option_other_id, v_screening_question_id, 'Something else', 1, FALSE, 3),
		(v_support_provider_option_id, v_support_question_id, 'Finding an in-network provider', 1, FALSE, 1),
		(v_support_benefits_option_id, v_support_question_id, 'Understanding costs and benefits', 1, FALSE, 2),
		(v_support_preparation_option_id, v_support_question_id, 'Preparing for a first appointment', 1, FALSE, 3),
		(v_follow_up_email_option_id, v_follow_up_question_id, 'Email', 1, FALSE, 1),
		(v_follow_up_phone_option_id, v_follow_up_question_id, 'Phone', 1, FALSE, 2),
		(v_follow_up_either_option_id, v_follow_up_question_id, 'No preference', 1, FALSE, 3),
		(v_context_answer_option_id, v_context_question_id, 'Share any context that would be helpful.', 1, FALSE, 1)
	ON CONFLICT (screening_answer_option_id) DO UPDATE
	SET screening_question_id=EXCLUDED.screening_question_id,
		answer_option_text=EXCLUDED.answer_option_text,
		score=EXCLUDED.score,
		indicates_crisis=EXCLUDED.indicates_crisis,
		display_order=EXCLUDED.display_order;

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
		'PROVIDER_INTAKE',
		v_account_id,
		v_screening_flow_name
	)
	ON CONFLICT (screening_flow_id) DO UPDATE
	SET institution_id=EXCLUDED.institution_id,
		screening_flow_type_id=EXCLUDED.screening_flow_type_id,
		created_by_account_id=EXCLUDED.created_by_account_id,
		name=EXCLUDED.name;

	INSERT INTO screening_flow_version (
		screening_flow_version_id,
		screening_flow_id,
		initial_screening_id,
		phone_number_required,
		version_number,
		orchestration_function,
		results_function,
		destination_function,
		created_by_account_id,
		skippable,
		screening_flow_skip_type_id
	) VALUES (
		v_screening_flow_version_id,
		v_screening_flow_id,
		v_screening_id,
		FALSE,
		1,
		v_orchestration_function,
		v_results_function,
		v_destination_function,
		v_account_id,
		FALSE,
		'EXIT'
	)
	ON CONFLICT (screening_flow_version_id) DO UPDATE
	SET screening_flow_id=EXCLUDED.screening_flow_id,
		initial_screening_id=EXCLUDED.initial_screening_id,
		phone_number_required=EXCLUDED.phone_number_required,
		version_number=EXCLUDED.version_number,
		orchestration_function=EXCLUDED.orchestration_function,
		results_function=EXCLUDED.results_function,
		destination_function=EXCLUDED.destination_function,
		created_by_account_id=EXCLUDED.created_by_account_id,
		skippable=EXCLUDED.skippable,
		screening_flow_skip_type_id=EXCLUDED.screening_flow_skip_type_id;

	UPDATE screening_flow
	SET active_screening_flow_version_id=v_screening_flow_version_id
	WHERE screening_flow_id=v_screening_flow_id;

	INSERT INTO appointment_type (
		appointment_type_id,
		name,
		description,
		duration_in_minutes,
		deleted,
		scheduling_system_id,
		visit_type_id,
		screening_flow_id
	) VALUES (
		v_appointment_type_id,
		v_appointment_type_name,
		v_appointment_type_description,
		30,
		FALSE,
		'COBALT',
		'INITIAL',
		v_screening_flow_id
	)
	ON CONFLICT (appointment_type_id) DO UPDATE
	SET name=EXCLUDED.name,
		description=EXCLUDED.description,
		duration_in_minutes=EXCLUDED.duration_in_minutes,
		deleted=EXCLUDED.deleted,
		scheduling_system_id=EXCLUDED.scheduling_system_id,
		visit_type_id=EXCLUDED.visit_type_id,
		screening_flow_id=EXCLUDED.screening_flow_id;

	INSERT INTO provider_appointment_type (
		provider_id,
		appointment_type_id,
		display_order
	)
	SELECT
		v_provider_id,
		v_appointment_type_id,
		COALESCE((
			SELECT MAX(existing.display_order) + 1
			FROM provider_appointment_type existing
			WHERE existing.provider_id=v_provider_id
		), 1)
	WHERE NOT EXISTS (
		SELECT 1
		FROM provider_appointment_type existing
		WHERE existing.provider_id=v_provider_id
		AND existing.appointment_type_id=v_appointment_type_id
	);

	INSERT INTO logical_availability (
		logical_availability_id,
		provider_id,
		start_date_time,
		end_date_time,
		logical_availability_type_id,
		recurrence_type_id,
		recur_sunday,
		recur_monday,
		recur_tuesday,
		recur_wednesday,
		recur_thursday,
		recur_friday,
		recur_saturday,
		created_by_account_id,
		last_updated_by_account_id
	) VALUES (
		v_logical_availability_id,
		v_provider_id,
		TIMESTAMP '2026-01-05 09:00:00',
		TIMESTAMP '2099-12-31 17:00:00',
		'OPEN',
		'DAILY',
		FALSE,
		TRUE,
		TRUE,
		TRUE,
		TRUE,
		TRUE,
		FALSE,
		v_account_id,
		v_account_id
	)
	ON CONFLICT (logical_availability_id) DO UPDATE
	SET provider_id=EXCLUDED.provider_id,
		start_date_time=EXCLUDED.start_date_time,
		end_date_time=EXCLUDED.end_date_time,
		logical_availability_type_id=EXCLUDED.logical_availability_type_id,
		recurrence_type_id=EXCLUDED.recurrence_type_id,
		recur_sunday=EXCLUDED.recur_sunday,
		recur_monday=EXCLUDED.recur_monday,
		recur_tuesday=EXCLUDED.recur_tuesday,
		recur_wednesday=EXCLUDED.recur_wednesday,
		recur_thursday=EXCLUDED.recur_thursday,
		recur_friday=EXCLUDED.recur_friday,
		recur_saturday=EXCLUDED.recur_saturday,
		last_updated_by_account_id=EXCLUDED.last_updated_by_account_id;

	INSERT INTO logical_availability_appointment_type (
		logical_availability_id,
		appointment_type_id
	) VALUES (
		v_logical_availability_id,
		v_appointment_type_id
	)
	ON CONFLICT (logical_availability_id, appointment_type_id) DO NOTHING;
END $$;

COMMIT;
