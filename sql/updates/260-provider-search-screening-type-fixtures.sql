BEGIN;
SELECT _v.register_patch('260-provider-search-screening-type-fixtures', NULL, NULL);

-- Provider search needs concrete, bookable rows for every screening type so QA
-- can run provider search -> screening -> appointment booking confirmation for
-- each screening taxonomy value without relying on legacy assessment fixtures.
DO $$
DECLARE
	v_created_by_account_id UUID;
	v_fixture_clinic_id UUID := UUID_GENERATE_V5('c65b62f1-b132-4d1b-ad43-26eae272a8b7'::UUID, 'provider-search-screening-type-fixture-clinic');
	v_institution_id TEXT := 'COBALT';
	v_screening_type RECORD;
BEGIN
	SELECT account_id
	INTO v_created_by_account_id
	FROM account
	WHERE institution_id=v_institution_id
	ORDER BY
		CASE WHEN role_id IN ('ADMINISTRATOR', 'SUPER_ADMINISTRATOR') THEN 0 ELSE 1 END,
		created,
		account_id
	LIMIT 1;

	IF v_created_by_account_id IS NULL THEN
		RAISE EXCEPTION 'Unable to create provider-search screening type fixtures because institution % has no account to own generated screening records.',
			v_institution_id;
	END IF;

	INSERT INTO clinic (
		clinic_id,
		description,
		treatment_description,
		phone_number,
		institution_id,
		show_intake_assessment_prompt,
		appointment_booking_level_id
	) VALUES (
		v_fixture_clinic_id,
		'Fixture Screening Type Coverage Clinic',
		'Provider-search fixture rows for screening type coverage.',
		'+12155553000',
		v_institution_id,
		FALSE,
		'PROVIDER'
	)
	ON CONFLICT (clinic_id) DO UPDATE
	SET description=EXCLUDED.description,
		treatment_description=EXCLUDED.treatment_description,
		phone_number=EXCLUDED.phone_number,
		institution_id=EXCLUDED.institution_id,
		show_intake_assessment_prompt=EXCLUDED.show_intake_assessment_prompt,
		appointment_booking_level_id=EXCLUDED.appointment_booking_level_id;

	FOR v_screening_type IN
		SELECT
			screening_type_id,
			description,
			ROW_NUMBER() OVER (ORDER BY screening_type_id)::INTEGER AS fixture_order
		FROM screening_type
		ORDER BY screening_type_id
	LOOP
		DECLARE
			v_appointment_type_id UUID := UUID_GENERATE_V5('c65b62f1-b132-4d1b-ad43-26eae272a8b7'::UUID, 'provider-search-screening-type-fixture-appointment-type:' || v_screening_type.screening_type_id);
			v_logical_availability_id UUID := UUID_GENERATE_V5('c65b62f1-b132-4d1b-ad43-26eae272a8b7'::UUID, 'provider-search-screening-type-fixture-logical-availability:' || v_screening_type.screening_type_id);
			v_provider_appointment_type_id UUID := UUID_GENERATE_V5('c65b62f1-b132-4d1b-ad43-26eae272a8b7'::UUID, 'provider-search-screening-type-fixture-provider-appointment-type:' || v_screening_type.screening_type_id);
			v_provider_clinic_id UUID := UUID_GENERATE_V5('c65b62f1-b132-4d1b-ad43-26eae272a8b7'::UUID, 'provider-search-screening-type-fixture-provider-clinic:' || v_screening_type.screening_type_id);
			v_provider_id UUID := UUID_GENERATE_V5('c65b62f1-b132-4d1b-ad43-26eae272a8b7'::UUID, 'provider-search-screening-type-fixture-provider:' || v_screening_type.screening_type_id);
			v_question_id UUID := UUID_GENERATE_V5('c65b62f1-b132-4d1b-ad43-26eae272a8b7'::UUID, 'provider-search-screening-type-fixture-question:' || v_screening_type.screening_type_id);
			v_screening_flow_id UUID := UUID_GENERATE_V5('c65b62f1-b132-4d1b-ad43-26eae272a8b7'::UUID, 'provider-search-screening-type-fixture-flow:' || v_screening_type.screening_type_id);
			v_screening_flow_version_id UUID := UUID_GENERATE_V5('c65b62f1-b132-4d1b-ad43-26eae272a8b7'::UUID, 'provider-search-screening-type-fixture-flow-version:' || v_screening_type.screening_type_id);
			v_screening_id UUID := UUID_GENERATE_V5('c65b62f1-b132-4d1b-ad43-26eae272a8b7'::UUID, 'provider-search-screening-type-fixture-screening:' || v_screening_type.screening_type_id);
			v_screening_version_id UUID := UUID_GENERATE_V5('c65b62f1-b132-4d1b-ad43-26eae272a8b7'::UUID, 'provider-search-screening-type-fixture-screening-version:' || v_screening_type.screening_type_id);
			v_yes_option_id UUID := UUID_GENERATE_V5('c65b62f1-b132-4d1b-ad43-26eae272a8b7'::UUID, 'provider-search-screening-type-fixture-answer-yes:' || v_screening_type.screening_type_id);
			v_no_option_id UUID := UUID_GENERATE_V5('c65b62f1-b132-4d1b-ad43-26eae272a8b7'::UUID, 'provider-search-screening-type-fixture-answer-no:' || v_screening_type.screening_type_id);
			v_name_suffix TEXT := REPLACE(LOWER(v_screening_type.screening_type_id), '_', '-');
		BEGIN
			INSERT INTO provider (
				provider_id,
				institution_id,
				name,
				title,
				email_address,
				locale,
				time_zone,
				entity,
				clinic,
				specialty,
				active,
				scheduling_system_id,
				system_affinity_id,
				url_name,
				videoconference_platform_id,
				phone_number,
				display_phone_number_only_for_booking,
				description
			) VALUES (
				v_provider_id,
				v_institution_id,
				FORMAT('Screening Fixture %s', v_screening_type.screening_type_id),
				'Screening Fixture',
				FORMAT('provider-search-screening-%s@example.com', v_name_suffix),
				'en-US',
				'America/New_York',
				'Fixture Screening Type Coverage Clinic',
				'Fixture Screening Type Coverage Clinic',
				v_screening_type.description,
				TRUE,
				'COBALT',
				'COBALT',
				FORMAT('provider-search-screening-%s', v_name_suffix),
				'SWITCHBOARD',
				'+12155553000',
				FALSE,
				FORMAT('Provider-search fixture for %s screening coverage.', v_screening_type.screening_type_id)
			)
			ON CONFLICT (provider_id) DO UPDATE
			SET institution_id=EXCLUDED.institution_id,
				name=EXCLUDED.name,
				title=EXCLUDED.title,
				email_address=EXCLUDED.email_address,
				locale=EXCLUDED.locale,
				time_zone=EXCLUDED.time_zone,
				entity=EXCLUDED.entity,
				clinic=EXCLUDED.clinic,
				specialty=EXCLUDED.specialty,
				active=EXCLUDED.active,
				scheduling_system_id=EXCLUDED.scheduling_system_id,
				system_affinity_id=EXCLUDED.system_affinity_id,
				url_name=EXCLUDED.url_name,
				videoconference_platform_id=EXCLUDED.videoconference_platform_id,
				phone_number=EXCLUDED.phone_number,
				display_phone_number_only_for_booking=EXCLUDED.display_phone_number_only_for_booking,
				description=EXCLUDED.description;

			INSERT INTO provider_support_role (
				provider_id,
				support_role_id
			) VALUES (
				v_provider_id,
				'COACH'
			)
			ON CONFLICT (provider_id, support_role_id) DO NOTHING;

			INSERT INTO provider_clinic (
				provider_clinic_id,
				provider_id,
				clinic_id,
				primary_clinic
			) VALUES (
				v_provider_clinic_id,
				v_provider_id,
				v_fixture_clinic_id,
				TRUE
			)
			ON CONFLICT (provider_clinic_id) DO UPDATE
			SET provider_id=EXCLUDED.provider_id,
				clinic_id=EXCLUDED.clinic_id,
				primary_clinic=EXCLUDED.primary_clinic;

			INSERT INTO screening (
				screening_id,
				name,
				active_screening_version_id,
				created_by_account_id
			) VALUES (
				v_screening_id,
				FORMAT('Provider Search Screening Type Fixture: %s', v_screening_type.screening_type_id),
				NULL,
				v_created_by_account_id
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
				v_screening_type.screening_type_id,
				v_created_by_account_id,
				1,
				$scoring$
const questionsWithAnswerOptions = input.screeningQuestionsWithAnswerOptions || [];
const question = questionsWithAnswerOptions.length > 0 ? questionsWithAnswerOptions[0].screeningQuestion : null;
const answeredQuestionIds = new Set((input.answeredScreeningQuestionIds || []).map(String));

output.completed = question ? answeredQuestionIds.has(String(question.screeningQuestionId)) : true;
output.score = { overallScore: 0 };
output.belowScoringThreshold = false;

if (!output.completed && question) {
  output.nextScreeningQuestionId = question.screeningQuestionId;
}
$scoring$
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

			INSERT INTO screening_institution (
				screening_id,
				institution_id
			) VALUES (
				v_screening_id,
				v_institution_id
			)
			ON CONFLICT (screening_id, institution_id) DO NOTHING;

			INSERT INTO screening_question (
				screening_question_id,
				screening_version_id,
				screening_answer_format_id,
				screening_answer_content_hint_id,
				intro_text,
				question_text,
				minimum_answer_count,
				maximum_answer_count,
				display_order,
				metadata,
				prefer_autosubmit,
				screening_question_submission_style_id
			) VALUES (
				v_question_id,
				v_screening_version_id,
				'SINGLE_SELECT',
				'NONE',
				NULL,
				FORMAT('Fixture question for %s booking screening coverage.', v_screening_type.screening_type_id),
				1,
				1,
				1,
				JSONB_BUILD_OBJECT('providerSearchScreeningTypeFixture', TRUE, 'screeningTypeId', v_screening_type.screening_type_id),
				TRUE,
				'NEXT'
			)
			ON CONFLICT (screening_question_id) DO UPDATE
			SET screening_version_id=EXCLUDED.screening_version_id,
				screening_answer_format_id=EXCLUDED.screening_answer_format_id,
				screening_answer_content_hint_id=EXCLUDED.screening_answer_content_hint_id,
				intro_text=EXCLUDED.intro_text,
				question_text=EXCLUDED.question_text,
				minimum_answer_count=EXCLUDED.minimum_answer_count,
				maximum_answer_count=EXCLUDED.maximum_answer_count,
				display_order=EXCLUDED.display_order,
				metadata=EXCLUDED.metadata,
				prefer_autosubmit=EXCLUDED.prefer_autosubmit,
				screening_question_submission_style_id=EXCLUDED.screening_question_submission_style_id;

			INSERT INTO screening_answer_option (
				screening_answer_option_id,
				screening_question_id,
				answer_option_text,
				score,
				indicates_crisis,
				display_order,
				metadata
			) VALUES
				(
					v_yes_option_id,
					v_question_id,
					'Continue',
					0,
					FALSE,
					1,
					JSONB_BUILD_OBJECT('providerSearchScreeningTypeFixture', TRUE)
				),
				(
					v_no_option_id,
					v_question_id,
					'Continue',
					0,
					FALSE,
					2,
					JSONB_BUILD_OBJECT('providerSearchScreeningTypeFixture', TRUE)
				)
			ON CONFLICT (screening_answer_option_id) DO UPDATE
			SET screening_question_id=EXCLUDED.screening_question_id,
				answer_option_text=EXCLUDED.answer_option_text,
				score=EXCLUDED.score,
				indicates_crisis=EXCLUDED.indicates_crisis,
				display_order=EXCLUDED.display_order,
				metadata=EXCLUDED.metadata;

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
				v_created_by_account_id,
				FORMAT('Provider Search Screening Type Fixture: %s', v_screening_type.screening_type_id)
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
				created_by_account_id
			) VALUES (
				v_screening_flow_version_id,
				v_screening_flow_id,
				v_screening_id,
				FALSE,
				1,
				$orchestration$
const screeningSessionScreening = (input.screeningSessionScreenings || [])[0];

output.completed = screeningSessionScreening ? Boolean(screeningSessionScreening.completed) : false;
output.crisisIndicated = false;
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

if (input.screeningSession.completed) {
  output.screeningSessionDestinationId = 'APPOINTMENT_BOOKING_CONFIRMATION';
  output.context.result = 'SUCCESS';
}
$destination$,
				v_created_by_account_id
			)
			ON CONFLICT (screening_flow_version_id) DO UPDATE
			SET screening_flow_id=EXCLUDED.screening_flow_id,
				initial_screening_id=EXCLUDED.initial_screening_id,
				phone_number_required=EXCLUDED.phone_number_required,
				version_number=EXCLUDED.version_number,
				orchestration_function=EXCLUDED.orchestration_function,
				results_function=EXCLUDED.results_function,
				destination_function=EXCLUDED.destination_function,
				created_by_account_id=EXCLUDED.created_by_account_id;

			UPDATE screening_flow
			SET active_screening_flow_version_id=v_screening_flow_version_id
			WHERE screening_flow_id=v_screening_flow_id;

			INSERT INTO appointment_type (
				appointment_type_id,
				acuity_appointment_type_id,
				name,
				description,
				duration_in_minutes,
				deleted,
				scheduling_system_id,
				visit_type_id,
				screening_flow_id
			) VALUES (
				v_appointment_type_id,
				NULL,
				FORMAT('Screening Fixture %s Appointment', v_screening_type.screening_type_id),
				FORMAT('%s provider-search booking fixture.', v_screening_type.description),
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
				provider_appointment_type_id,
				provider_id,
				display_order,
				appointment_type_id
			) VALUES (
				v_provider_appointment_type_id,
				v_provider_id,
				31000 + v_screening_type.fixture_order,
				v_appointment_type_id
			)
			ON CONFLICT (provider_appointment_type_id) DO UPDATE
			SET provider_id=EXCLUDED.provider_id,
				display_order=EXCLUDED.display_order,
				appointment_type_id=EXCLUDED.appointment_type_id;

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
				v_created_by_account_id,
				v_created_by_account_id
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
		END;
	END LOOP;
END $$;

COMMIT;
