BEGIN;
SELECT _v.register_patch('256-provider-booking-screening', NULL, NULL);

-- Add the explicit lookup used by provider search to distinguish provider-level
-- booking rows from clinic aggregate booking rows.
CREATE TABLE IF NOT EXISTS appointment_booking_level (
	appointment_booking_level_id TEXT PRIMARY KEY,
	description TEXT NOT NULL
);

INSERT INTO appointment_booking_level (appointment_booking_level_id, description)
VALUES
	('PROVIDER', 'Provider'),
	('CLINIC', 'Clinic')
ON CONFLICT (appointment_booking_level_id) DO UPDATE
SET description = EXCLUDED.description;

-- Add the clinic booking-level column. Existing rows default to PROVIDER, and
-- branch databases that still have the temporary boolean are translated before
-- that branch-only column is removed.
ALTER TABLE clinic ADD COLUMN IF NOT EXISTS appointment_booking_level_id TEXT;

UPDATE clinic
SET appointment_booking_level_id='PROVIDER'
WHERE appointment_booking_level_id IS NULL
OR appointment_booking_level_id NOT IN (
	SELECT appointment_booking_level_id
	FROM appointment_booking_level
);

DO $$
BEGIN
	IF EXISTS (
		SELECT 1
		FROM information_schema.columns
		WHERE table_schema=CURRENT_SCHEMA()
		AND table_name='clinic'
		AND column_name='bookable_as_provider'
	) THEN
		UPDATE clinic
		SET appointment_booking_level_id='CLINIC'
		WHERE bookable_as_provider=TRUE;
	END IF;
END $$;

ALTER TABLE clinic ALTER COLUMN appointment_booking_level_id SET DEFAULT 'PROVIDER';
ALTER TABLE clinic ALTER COLUMN appointment_booking_level_id SET NOT NULL;

DO $$
BEGIN
	IF NOT EXISTS (
		SELECT 1
		FROM pg_constraint
		WHERE connamespace=CURRENT_SCHEMA()::REGNAMESPACE
		AND conname='clinic_appointment_booking_level_id_fkey'
	) THEN
		ALTER TABLE clinic
		ADD CONSTRAINT clinic_appointment_booking_level_id_fkey
		FOREIGN KEY (appointment_booking_level_id)
		REFERENCES appointment_booking_level(appointment_booking_level_id);
	END IF;
END $$;

ALTER TABLE clinic DROP COLUMN IF EXISTS bookable_as_provider;

-- Add the screening-flow pointer to appointment types. Values created earlier
-- on this feature branch are intentionally discarded and rebuilt below from the
-- active legacy appointment assessments.
ALTER TABLE appointment_type ADD COLUMN IF NOT EXISTS screening_flow_id UUID;
UPDATE appointment_type SET screening_flow_id=NULL;

DO $$
BEGIN
	IF NOT EXISTS (
		SELECT 1
		FROM pg_constraint
		WHERE connamespace=CURRENT_SCHEMA()::REGNAMESPACE
		AND conname='appointment_type_screening_flow_id_fkey'
	) THEN
		ALTER TABLE appointment_type
		ADD CONSTRAINT appointment_type_screening_flow_id_fkey
		FOREIGN KEY (screening_flow_id)
		REFERENCES screening_flow(screening_flow_id);
	END IF;
END $$;

-- Keep appointment type reads compatible with existing legacy assessment joins
-- while exposing the new screening flow relationship. Drop/recreate avoids
-- PostgreSQL's CREATE OR REPLACE VIEW column-order rename restriction when
-- branch databases already created the view with screening_flow_id in a
-- different position.
DROP VIEW IF EXISTS v_appointment_type;
CREATE VIEW v_appointment_type AS
SELECT
	app_type.appointment_type_id,
	app_type.acuity_appointment_type_id,
	app_type.name,
	app_type.description,
	app_type.duration_in_minutes,
	app_type.deleted,
	app_type.created,
	app_type.last_updated,
	app_type.scheduling_system_id,
	app_type.epic_visit_type_id,
	app_type.epic_visit_type_id_type,
	app_type.visit_type_id,
	app_type.hex_color,
	ata.assessment_id,
	app_type.epic_visit_type_system,
	app_type.screening_flow_id
FROM appointment_type app_type
LEFT OUTER JOIN appointment_type_assessment ata
	ON app_type.appointment_type_id = ata.appointment_type_id
	AND ata.active = TRUE
WHERE app_type.deleted = FALSE;

-- Refresh Cobalt provider-search fixture rows so local and test databases have
-- varied provider bio/description/phone data, clinic phone/image data, one
-- clinic-level booking aggregate, and feature/location-specific provider rows.
-- These UUID-scoped updates are no-ops in environments without the fixture rows.
UPDATE provider
SET bio=provider_fixture.bio,
	description=provider_fixture.description,
	phone_number=provider_fixture.phone_number,
	display_phone_number_only_for_booking=FALSE
FROM (VALUES
	('15f9711d-38e1-44a1-a933-f1522ddd2c81'::UUID, 'Dr. Allen focuses on reproductive psychiatry and collaborative medication planning.', 'Perinatal psychiatry consults for medication planning and continuity of care.', '+12155551001'),
	('31633b9d-651b-402b-9314-7def6af811b6'::UUID, 'Dr. Spence treats anxiety and trauma concerns with a structured psychiatry approach.', 'Psychiatry visits for anxiety, panic, and trauma-related symptoms.', '+12155551002'),
	('360d46c4-2ee9-4031-aab6-aa6a16f398d7'::UUID, 'Dr. Fritz provides cognitive therapy with practical goal setting between sessions.', 'Cognitive therapy appointments for mood, stress, and behavior change.', '+12155551003'),
	('dc7aeafd-0fc8-4c4d-b09a-09d4dc3079c1'::UUID, 'Rabbi Grayson offers spiritual support for identity, grief, and major transitions.', 'Spiritual care appointments for reflection, meaning, grief, and transition support.', '+12155551004'),
	('2d6b7032-0145-4273-84f5-94e7238bc331'::UUID, 'Dr. Watson coaches patients who are navigating substance use goals and recovery supports.', 'Coaching sessions for substance use goals and treatment navigation.', '+12155551005'),
	('ed461fc4-0436-4880-b340-b075d56a06f4'::UUID, 'Dr. Shaaban supports patients working on eating patterns, weight concerns, and motivation.', 'Coaching visits for eating, weight, and behavior change goals.', '+12155551006'),
	('9dcc6e07-821e-4b64-8975-aee5fcd5ca8b'::UUID, 'Dr. Jones works with veterans and families on short-term coping plans.', 'Coaching for military families, veterans, and related transition stress.', '+12155551007'),
	('a865013e-d50c-46fc-b828-0e5ccdac41b6'::UUID, 'Dr. Behavioral Sleep provides psychiatry support for insomnia and sleep routines.', 'Behavioral sleep appointments for insomnia and circadian rhythm concerns.', '+12155551008'),
	('eb19c43f-c452-407f-92c1-3602695bceb2'::UUID, 'Dr. Attention Deficit evaluates attention concerns and treatment planning needs.', 'Psychiatry visits for attention, focus, and executive functioning concerns.', '+12155551009'),
	('b988f30d-11a6-4818-9c34-ac6f7c429ee1'::UUID, 'Dr. EAP Clinic offers short-term psychiatry support for workplace stress.', 'Brief EAP psychiatry appointments for acute stress and work-related concerns.', '+12155551010'),
	('11e02870-30bc-4178-8614-16caf5fe8996'::UUID, 'Dr. No Intake is used to test direct booking without legacy intake requirements.', 'Direct booking psychiatry appointments without a clinic intake assessment.', '+12155551011')
) AS provider_fixture(provider_id, bio, description, phone_number)
WHERE provider.provider_id=provider_fixture.provider_id;

UPDATE clinic
SET phone_number=clinic_fixture.phone_number,
	image_url=clinic_fixture.image_url,
	appointment_booking_level_id=clinic_fixture.appointment_booking_level_id
FROM (VALUES
	('d789dbdb-6756-4293-836d-91b7329fb49c'::UUID, '+12155552001', 'https://www.fillmurray.com/640/360', 'PROVIDER'),
	('b6c5e9a3-6018-473d-86d4-2861a328e537'::UUID, '+12155552002', 'https://www.fillmurray.com/641/360', 'PROVIDER'),
	('7872559f-b5f6-449f-892d-3f312cd691ff'::UUID, '+12155552003', 'https://www.fillmurray.com/642/360', 'PROVIDER'),
	('ab629384-400a-4688-8465-04636ec2eaa2'::UUID, '+12155552004', 'https://www.fillmurray.com/643/360', 'CLINIC'),
	('03283875-eb33-42ff-8d14-2acb4a67b300'::UUID, '+12155552005', 'https://www.fillmurray.com/644/360', 'PROVIDER'),
	('3eeb5b48-4c9c-4601-a091-09af03abe3ef'::UUID, '+12155552006', 'https://www.fillmurray.com/645/360', 'PROVIDER'),
	('25fd7117-3013-4462-b7b4-63a9bf808f10'::UUID, '+12155552007', 'https://www.fillmurray.com/646/360', 'PROVIDER'),
	('b1f16a29-66ed-484f-a4ed-110fd8bdded5'::UUID, '+12155552008', 'https://www.fillmurray.com/647/360', 'PROVIDER'),
	('8a385c20-dec8-4535-8c6d-684f0e70bfc0'::UUID, '+12155552009', 'https://www.fillmurray.com/648/360', 'PROVIDER'),
	('adab724f-2de7-4824-a56f-50fe8554f730'::UUID, '+12155552010', 'https://www.fillmurray.com/649/360', 'PROVIDER'),
	('af1bb3fc-f5ab-49e2-8276-9727b58e9a93'::UUID, '+12155552011', 'https://www.fillmurray.com/650/360', 'PROVIDER')
) AS clinic_fixture(clinic_id, phone_number, image_url, appointment_booking_level_id)
WHERE clinic.clinic_id=clinic_fixture.clinic_id;

INSERT INTO provider_institution_location (
	provider_id,
	institution_location_id
)
SELECT
	provider.provider_id,
	institution_location.institution_location_id
FROM (VALUES
	('15f9711d-38e1-44a1-a933-f1522ddd2c81'::UUID, 'Cobalt General'),
	('31633b9d-651b-402b-9314-7def6af811b6'::UUID, 'Cobalt Health System'),
	('360d46c4-2ee9-4031-aab6-aa6a16f398d7'::UUID, 'Cobalt Health System'),
	('dc7aeafd-0fc8-4c4d-b09a-09d4dc3079c1'::UUID, 'Cobalt General'),
	('2d6b7032-0145-4273-84f5-94e7238bc331'::UUID, 'Cobalt General'),
	('ed461fc4-0436-4880-b340-b075d56a06f4'::UUID, 'Cobalt Health System'),
	('9dcc6e07-821e-4b64-8975-aee5fcd5ca8b'::UUID, 'Cobalt General'),
	('a865013e-d50c-46fc-b828-0e5ccdac41b6'::UUID, 'Cobalt General'),
	('eb19c43f-c452-407f-92c1-3602695bceb2'::UUID, 'Cobalt Health System'),
	('b988f30d-11a6-4818-9c34-ac6f7c429ee1'::UUID, 'Cobalt Health System'),
	('11e02870-30bc-4178-8614-16caf5fe8996'::UUID, 'Cobalt General')
) AS provider_location(provider_id, institution_location_name)
JOIN provider
	ON provider.provider_id=provider_location.provider_id
JOIN institution_location
	ON institution_location.institution_id=provider.institution_id
	AND institution_location.name=provider_location.institution_location_name
WHERE NOT EXISTS (
	SELECT 1
	FROM provider_institution_location existing_provider_location
	WHERE existing_provider_location.provider_id=provider.provider_id
	AND existing_provider_location.institution_location_id=institution_location.institution_location_id
);

-- Port active legacy appointment assessments into screening flows without
-- modifying the legacy assessment, question, answer, or session tables. This
-- creates new screening records and assigns the resulting flow to the
-- appointment type.
DO $$
DECLARE
	v_appointment_assessment RECORD;
	v_created_by_account_id UUID;
	v_destination_function TEXT;
	v_flow_name TEXT;
	v_institution_count INTEGER;
	v_institution_id TEXT;
	v_orchestration_function TEXT;
	v_question_count INTEGER;
	v_results_function TEXT;
	v_screening_flow_id UUID;
	v_screening_flow_version_id UUID;
	v_screening_id UUID;
	v_screening_version_id UUID;
	v_scoring_function TEXT;
BEGIN
	FOR v_appointment_assessment IN
		SELECT
			ata.assessment_id,
			ata.appointment_type_id,
			app_type.name AS appointment_type_name,
			assessment.minimum_eligibility_score,
			assessment.ineligible_message
		FROM appointment_type_assessment ata
		JOIN appointment_type app_type
			ON app_type.appointment_type_id=ata.appointment_type_id
		JOIN assessment
			ON assessment.assessment_id=ata.assessment_id
		WHERE ata.active=TRUE
		AND COALESCE(app_type.deleted, FALSE)=FALSE
		ORDER BY app_type.name, ata.appointment_type_id
	LOOP
		-- A generated provider intake flow belongs to the single institution that
		-- offers the appointment type.
		SELECT COUNT(DISTINCT provider.institution_id), MIN(provider.institution_id)
		INTO v_institution_count, v_institution_id
		FROM provider_appointment_type pat
		JOIN provider
			ON provider.provider_id=pat.provider_id
		WHERE pat.appointment_type_id=v_appointment_assessment.appointment_type_id;

		IF v_institution_count = 0 THEN
			RAISE NOTICE 'Skipping appointment type % assessment % because no provider institution could be determined.',
				v_appointment_assessment.appointment_type_id, v_appointment_assessment.assessment_id;
			CONTINUE;
		END IF;

		IF v_institution_count > 1 THEN
			RAISE EXCEPTION 'Appointment type % assessment % maps to multiple provider institutions; cannot choose a screening flow institution.',
				v_appointment_assessment.appointment_type_id, v_appointment_assessment.assessment_id;
		END IF;

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
			RAISE EXCEPTION 'Unable to port appointment type % assessment % because institution % has no account to own generated screening records.',
				v_appointment_assessment.appointment_type_id, v_appointment_assessment.assessment_id, v_institution_id;
		END IF;

		-- Materialize the legacy assessment chain so linked next_assessment_id
		-- assessments are preserved in the generated screening.
		DROP TABLE IF EXISTS tmp_provider_booking_assessment_chain;
		CREATE TEMP TABLE tmp_provider_booking_assessment_chain ON COMMIT DROP AS
		WITH RECURSIVE assessment_chain AS (
			SELECT
				assessment.assessment_id,
				assessment.next_assessment_id,
				assessment.minimum_eligibility_score,
				assessment.ineligible_message,
				1 AS assessment_order,
				ARRAY[assessment.assessment_id] AS assessment_path
			FROM assessment
			WHERE assessment.assessment_id=v_appointment_assessment.assessment_id

			UNION ALL

			SELECT
				next_assessment.assessment_id,
				next_assessment.next_assessment_id,
				next_assessment.minimum_eligibility_score,
				next_assessment.ineligible_message,
				assessment_chain.assessment_order + 1,
				assessment_chain.assessment_path || next_assessment.assessment_id
			FROM assessment_chain
			JOIN assessment next_assessment
				ON next_assessment.assessment_id=assessment_chain.next_assessment_id
			WHERE NOT next_assessment.assessment_id = ANY(assessment_chain.assessment_path)
		)
		SELECT *
		FROM assessment_chain;

		-- Build stable legacy-to-screening question mappings and preserve legacy
		-- display ordering across the full assessment chain.
		DROP TABLE IF EXISTS tmp_provider_booking_question_map;
		CREATE TEMP TABLE tmp_provider_booking_question_map ON COMMIT DROP AS
		SELECT
			question.question_id AS legacy_question_id,
			question.assessment_id AS legacy_assessment_id,
			uuid_generate_v4() AS screening_question_id,
			ROW_NUMBER() OVER (
				ORDER BY assessment_chain.assessment_order, question.display_order, question.question_id
			)::INTEGER AS display_order,
			COALESCE(answer_count.answer_count, 0)::INTEGER AS answer_count
		FROM tmp_provider_booking_assessment_chain assessment_chain
		JOIN question
			ON question.assessment_id=assessment_chain.assessment_id
		LEFT JOIN (
			SELECT question_id, COUNT(*) AS answer_count
			FROM answer
			GROUP BY question_id
		) answer_count
			ON answer_count.question_id=question.question_id;

		SELECT COUNT(*)
		INTO v_question_count
		FROM tmp_provider_booking_question_map;

		IF v_question_count = 0 THEN
			RAISE NOTICE 'Skipping appointment type % assessment % because no legacy questions were found.',
				v_appointment_assessment.appointment_type_id, v_appointment_assessment.assessment_id;
			CONTINUE;
		END IF;

		-- Build answer-option mappings, including a neutral option for legacy
		-- freeform questions so every screening question can be answered.
		DROP TABLE IF EXISTS tmp_provider_booking_answer_option_map;
		CREATE TEMP TABLE tmp_provider_booking_answer_option_map ON COMMIT DROP AS
		SELECT
			answer.answer_id AS legacy_answer_id,
			question_map.legacy_question_id,
			answer.next_question_id AS legacy_next_question_id,
			uuid_generate_v4() AS screening_answer_option_id,
			question_map.screening_question_id,
			answer.answer_text,
			answer.answer_value,
			COALESCE(answer.crisis, FALSE) AS crisis,
			COALESCE(answer.call, FALSE) AS call,
			answer.display_order
		FROM tmp_provider_booking_question_map question_map
		JOIN answer
			ON answer.question_id=question_map.legacy_question_id

		UNION ALL

		SELECT
			NULL::UUID AS legacy_answer_id,
			question_map.legacy_question_id,
			NULL::UUID AS legacy_next_question_id,
			uuid_generate_v4() AS screening_answer_option_id,
			question_map.screening_question_id,
			NULL::VARCHAR AS answer_text,
			0 AS answer_value,
			FALSE AS crisis,
			FALSE AS call,
			1 AS display_order
		FROM tmp_provider_booking_question_map question_map
		WHERE question_map.answer_count = 0;

		v_screening_id := uuid_generate_v4();
		v_screening_version_id := uuid_generate_v4();
		v_screening_flow_id := uuid_generate_v4();
		v_screening_flow_version_id := uuid_generate_v4();
		v_flow_name := FORMAT('Provider Intake: %s (%s)',
			v_appointment_assessment.appointment_type_name,
			v_appointment_assessment.appointment_type_id);

		-- Generate the screening JavaScript functions from the legacy graph:
		-- scoring preserves answer values and branching, orchestration reports
		-- completion/crisis state, and destination routes completed sessions back
		-- to provider appointment booking.
		v_scoring_function := FORMAT($scoring$
const minimumEligibilityScore = %s;
const questions = (input.screeningQuestionsWithAnswerOptions || [])
  .map((screeningQuestionWithAnswerOptions) => screeningQuestionWithAnswerOptions.screeningQuestion)
  .sort((first, second) => first.displayOrder - second.displayOrder);
const questionsById = {};
const questionIds = [];

questions.forEach((question) => {
  const questionId = String(question.screeningQuestionId);
  questionsById[questionId] = question;
  questionIds.push(questionId);
});

const answeredQuestionIds = new Set((input.answeredScreeningQuestionIds || []).map(String));

function answerOptionsForQuestionId(questionId) {
  const answerIds = input.screeningAnswerIdsByScreeningQuestionId[questionId] || [];

  return answerIds
    .map((answerId) => input.screeningAnswerOptionsByScreeningAnswerId[answerId])
    .filter((answerOption) => answerOption);
}

function nextQuestionAfter(question) {
  const currentIndex = questionIds.indexOf(String(question.screeningQuestionId));

  if (currentIndex < 0 || currentIndex + 1 >= questionIds.length) {
    return null;
  }

  return questionsById[questionIds[currentIndex + 1]];
}

let overallScore = 0;
let firstUnansweredQuestionId = null;

questions.forEach((question) => {
  answerOptionsForQuestionId(String(question.screeningQuestionId)).forEach((answerOption) => {
    overallScore += Number(answerOption.score || 0);
  });
});

let currentQuestion = questions[0] || null;
const visitedQuestionIds = new Set();

while (currentQuestion) {
  const questionId = String(currentQuestion.screeningQuestionId);

  if (visitedQuestionIds.has(questionId)) {
    break;
  }

  visitedQuestionIds.add(questionId);

  if (!answeredQuestionIds.has(questionId)) {
    firstUnansweredQuestionId = questionId;
    break;
  }

  const selectedAnswerOption = answerOptionsForQuestionId(questionId)
    .find((answerOption) => answerOption.metadata && answerOption.metadata.nextScreeningQuestionId);

  if (selectedAnswerOption && selectedAnswerOption.metadata.nextScreeningQuestionId) {
    currentQuestion = questionsById[String(selectedAnswerOption.metadata.nextScreeningQuestionId)] || null;
  } else {
    currentQuestion = nextQuestionAfter(currentQuestion);
  }
}

output.completed = firstUnansweredQuestionId === null;
output.score = { overallScore };
output.belowScoringThreshold = overallScore < minimumEligibilityScore;

if (!output.completed && firstUnansweredQuestionId) {
  output.nextScreeningQuestionId = firstUnansweredQuestionId;
}
$scoring$, v_appointment_assessment.minimum_eligibility_score);

		v_orchestration_function := $orchestration$
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

		v_results_function := $results$
output.supportRoleRecommendations = [];
output.recommendLegacyContentAnswerIds = false;
output.legacyContentAnswerIds = [];
output.recommendedTagIds = [];
output.recommendedFeatureIds = [];
output.integratedCareTriages = [];
$results$;

		v_destination_function := FORMAT($destination$
const minimumEligibilityScore = %s;
const ineligibleMessage = %s;
const screeningSessionScreening = (input.screeningSessionScreenings || [])[0];
const overallScore = screeningSessionScreening && screeningSessionScreening.scoreAsObject
  ? Number(screeningSessionScreening.scoreAsObject.overallScore || 0)
  : 0;

output.screeningSessionDestinationId = null;
output.context = {};

if (input.screeningSession.completed) {
  const eligible = overallScore >= minimumEligibilityScore;

  output.screeningSessionDestinationId = 'PROVIDER_APPOINTMENT_BOOKING';
  output.context.result = eligible ? 'SUCCESS' : 'FAILURE';

  if (!eligible && ineligibleMessage) {
    output.context.ineligibleMessage = ineligibleMessage;
  }
}
$destination$,
			v_appointment_assessment.minimum_eligibility_score,
			COALESCE(TO_JSON(v_appointment_assessment.ineligible_message)::TEXT, 'null'));

		-- Create the screening shell, version, institution link, questions, and
		-- answer options that mirror the legacy assessment content.
		INSERT INTO screening (
			screening_id,
			name,
			active_screening_version_id,
			created_by_account_id
		) VALUES (
			v_screening_id,
			v_flow_name,
			NULL,
			v_created_by_account_id
		);

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

		INSERT INTO screening_institution (
			screening_id,
			institution_id
		) VALUES (
			v_screening_id,
			v_institution_id
		);

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
			metadata
		)
		SELECT
			question_map.screening_question_id,
			v_screening_version_id,
			CASE
				WHEN question_type.requires_text_response THEN 'FREEFORM_TEXT'
				WHEN question_type.allow_multiple_answers THEN 'MULTI_SELECT'
				ELSE 'SINGLE_SELECT'
			END,
			CASE
				WHEN question.question_content_hint_id IN ('FIRST_NAME', 'LAST_NAME', 'PHONE_NUMBER', 'EMAIL_ADDRESS')
					THEN question.question_content_hint_id
				ELSE 'NONE'
			END,
			NULL,
			COALESCE(NULLIF(question.cms_question_text, ''), NULLIF(question.question_text, ''), NULLIF(assessment.base_question, ''), 'Question'),
			CASE WHEN question.answer_required THEN 1 ELSE 0 END,
			CASE
				WHEN question_type.allow_multiple_answers THEN GREATEST(question_map.answer_count, 1)
				ELSE 1
			END,
			question_map.display_order,
			JSONB_BUILD_OBJECT(
				'legacyAssessmentId', question.assessment_id::TEXT,
				'legacyQuestionId', question.question_id::TEXT
			)
		FROM tmp_provider_booking_question_map question_map
		JOIN question
			ON question.question_id=question_map.legacy_question_id
		JOIN assessment
			ON assessment.assessment_id=question.assessment_id
		LEFT JOIN question_type
			ON question_type.question_type_id=question.question_type_id;

		INSERT INTO screening_answer_option (
			screening_answer_option_id,
			screening_question_id,
			answer_option_text,
			score,
			indicates_crisis,
			display_order,
			metadata
		)
		SELECT
			answer_option_map.screening_answer_option_id,
			answer_option_map.screening_question_id,
			answer_option_map.answer_text,
			answer_option_map.answer_value,
			answer_option_map.crisis,
			answer_option_map.display_order,
			JSONB_STRIP_NULLS(JSONB_BUILD_OBJECT(
				'legacyAnswerId', answer_option_map.legacy_answer_id::TEXT,
				'legacyQuestionId', answer_option_map.legacy_question_id::TEXT,
				'legacyNextQuestionId', answer_option_map.legacy_next_question_id::TEXT,
				'nextScreeningQuestionId', next_question_map.screening_question_id::TEXT,
				'answerValue', answer_option_map.answer_value,
				'call', answer_option_map.call
			))
		FROM tmp_provider_booking_answer_option_map answer_option_map
		LEFT JOIN tmp_provider_booking_question_map next_question_map
			ON next_question_map.legacy_question_id=answer_option_map.legacy_next_question_id;

		-- Create the provider intake flow/version and assign it back to the
		-- appointment type that owns the active legacy assessment.
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
			v_flow_name
		);

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
			v_orchestration_function,
			v_results_function,
			v_destination_function,
			v_created_by_account_id
		);

		UPDATE screening_flow
		SET active_screening_flow_version_id=v_screening_flow_version_id
		WHERE screening_flow_id=v_screening_flow_id;

		UPDATE appointment_type
		SET screening_flow_id=v_screening_flow_id
		WHERE appointment_type_id=v_appointment_assessment.appointment_type_id;
	END LOOP;
END $$;

COMMIT;
