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

-- Snapshot appointment contact fields at booking time so appointment records
-- keep the name/email used for scheduling even if account details later change.
ALTER TABLE appointment ADD COLUMN IF NOT EXISTS first_name TEXT NULL;
ALTER TABLE appointment ADD COLUMN IF NOT EXISTS last_name TEXT NULL;
ALTER TABLE appointment ADD COLUMN IF NOT EXISTS email_address TEXT NULL;

UPDATE appointment app
SET first_name=a.first_name,
	last_name=a.last_name,
	email_address=COALESCE(app.email_address, a.email_address)
FROM account a
WHERE a.account_id=app.account_id;

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

  output.screeningSessionDestinationId = 'APPOINTMENT_BOOKING_CONFIRMATION';
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





-- The Penn Autism Clinic referrer owns a fullscreen screening flow that routes
-- eligible users to a clinic-specific booking page. Reuse that flow for booking
-- requirements on the concrete appointment types shown in provider search.
WITH autism_referrer AS (
  SELECT intake_screening_flow_id
  FROM institution_referrer
  WHERE from_institution_id = 'COBALT'
    AND url_name = 'autism-clinic'
),
autism_appointment_type AS (
  SELECT appointment_type_id
  FROM appointment_type
  WHERE scheduling_system_id = 'COBALT'
    AND name IN ('Autism Clinic Intake Call', 'Autism Clinic Consult Call')
)
UPDATE appointment_type
SET screening_flow_id = autism_referrer.intake_screening_flow_id
FROM autism_referrer, autism_appointment_type
WHERE appointment_type.appointment_type_id = autism_appointment_type.appointment_type_id
  AND autism_referrer.intake_screening_flow_id IS NOT NULL;

UPDATE clinic
SET appointment_booking_level_id = 'CLINIC'
WHERE institution_id = 'COBALT'
  AND description = 'Penn Autism Clinic';


UPDATE screening_flow_version
SET destination_function = REPLACE(
	REPLACE(destination_function,
		'''PROVIDER_APPOINTMENT_BOOKING''',
		'''APPOINTMENT_BOOKING_CONFIRMATION'''),
	'"PROVIDER_APPOINTMENT_BOOKING"',
	'"APPOINTMENT_BOOKING_CONFIRMATION"')
WHERE screening_flow_id IN (
	SELECT DISTINCT screening_flow_id
	FROM appointment_type
	WHERE screening_flow_id IS NOT NULL
)
AND destination_function LIKE '%PROVIDER_APPOINTMENT_BOOKING%';


CREATE OR REPLACE FUNCTION pg_temp.url_encode(value TEXT)
RETURNS TEXT
LANGUAGE plpgsql
IMMUTABLE
STRICT
AS $$
DECLARE
	bytes BYTEA := CONVERT_TO(value, 'UTF8');
	byte_index INTEGER;
	byte_value INTEGER;
	encoded_value TEXT := '';
BEGIN
	FOR byte_index IN 0..OCTET_LENGTH(bytes) - 1 LOOP
		byte_value := GET_BYTE(bytes, byte_index);

		IF (byte_value BETWEEN ASCII('a') AND ASCII('z'))
				OR (byte_value BETWEEN ASCII('A') AND ASCII('Z'))
				OR (byte_value BETWEEN ASCII('0') AND ASCII('9'))
				OR byte_value IN (ASCII('-'), ASCII('.'), ASCII('_'), ASCII('~')) THEN
			encoded_value := encoded_value || CHR(byte_value);
		ELSE
			encoded_value := encoded_value || '%' || UPPER(LPAD(TO_HEX(byte_value), 2, '0'));
		END IF;
	END LOOP;

	RETURN encoded_value;
END;
$$;

WITH referrer_location AS (
	SELECT
		ir.institution_referrer_id,
		CASE
			WHEN COUNT(DISTINCT ifrl.institution_location_id) = 1 THEN MIN(ifrl.institution_location_id::TEXT)
			ELSE NULL
		END AS institution_location_id
	FROM
		institution_referrer ir
	LEFT OUTER JOIN institution_feature_institution_referrer ifir
		ON ifir.institution_referrer_id = ir.institution_referrer_id
	LEFT OUTER JOIN institution_feature_referrer_location ifrl
		ON ifrl.institution_feature_institution_referrer_id = ifir.institution_feature_institution_referrer_id
	GROUP BY
		ir.institution_referrer_id
), updated_result_screens AS (
	SELECT
		ir.institution_referrer_id,
		JSONB_OBJECT_AGG(
			result_screen.key,
			CASE
				WHEN booking_route.provider_search_result_type_id IS NOT NULL
						AND booking_route.provider_search_result_id IS NOT NULL
						AND booking_route.appointment_type_id IS NOT NULL
						AND booking_route.institution_location_id IS NOT NULL
						AND booking_route.feature_id IS NOT NULL THEN
					JSONB_SET(
						JSONB_SET(
							result_screen.value,
							'{booking,path}',
							TO_JSONB(FORMAT('/provider-confirm-appointment-time?providerSearchResultTypeId=%s&%s=%s&appointmentTypeId=%s&institutionLocationId=%s&featureId=%s',
								pg_temp.url_encode(booking_route.provider_search_result_type_id),
								CASE
									WHEN booking_route.provider_search_result_type_id = 'PROVIDER' THEN 'providerId'
									ELSE 'clinicId'
								END,
								pg_temp.url_encode(booking_route.provider_search_result_id),
								pg_temp.url_encode(booking_route.appointment_type_id),
								pg_temp.url_encode(booking_route.institution_location_id),
								pg_temp.url_encode(booking_route.feature_id))),
							TRUE
						),
						'{booking,appointmentTypeId}',
						TO_JSONB(booking_route.appointment_type_id),
						TRUE
					)
				ELSE result_screen.value
			END
		) AS result_screens
	FROM
		institution_referrer ir
	LEFT OUTER JOIN referrer_location
		ON referrer_location.institution_referrer_id = ir.institution_referrer_id
	CROSS JOIN LATERAL JSONB_EACH(
		CASE
			WHEN JSONB_TYPEOF(ir.metadata->'resultScreens') = 'object' THEN ir.metadata->'resultScreens'
			ELSE '{}'::JSONB
		END
	) AS result_screen(key, value)
	CROSS JOIN LATERAL (
		SELECT result_screen.value->'booking' AS booking
	) booking_context
	CROSS JOIN LATERAL (
		SELECT
			NULLIF(BTRIM(booking_context.booking->>'providerId'), '') AS provider_id,
			CASE
				WHEN UPPER(NULLIF(BTRIM(booking_context.booking->>'providerSearchResultTypeId'), '')) IN ('PROVIDER', 'CLINIC') THEN
					UPPER(NULLIF(BTRIM(booking_context.booking->>'providerSearchResultTypeId'), ''))
				ELSE NULL
			END AS provider_search_result_type_id,
			CASE
				WHEN NULLIF(BTRIM(booking_context.booking->>'clinicId'), '') IS NOT NULL THEN
					NULLIF(BTRIM(booking_context.booking->>'clinicId'), '')
				WHEN JSONB_TYPEOF(booking_context.booking->'clinicIds') = 'array'
						AND JSONB_ARRAY_LENGTH(booking_context.booking->'clinicIds') = 1 THEN
					NULLIF(BTRIM(booking_context.booking->'clinicIds'->>0), '')
				ELSE NULL
			END AS clinic_id,
			CASE
				WHEN NULLIF(BTRIM(booking_context.booking->>'appointmentTypeId'), '') IS NOT NULL THEN
					NULLIF(BTRIM(booking_context.booking->>'appointmentTypeId'), '')
				WHEN JSONB_TYPEOF(booking_context.booking->'appointmentTypeIds') = 'array'
						AND JSONB_ARRAY_LENGTH(booking_context.booking->'appointmentTypeIds') = 1 THEN
					NULLIF(BTRIM(booking_context.booking->'appointmentTypeIds'->>0), '')
				ELSE NULL
			END AS appointment_type_id,
			NULLIF(BTRIM(booking_context.booking->>'featureId'), '') AS feature_id,
			COALESCE(NULLIF(BTRIM(booking_context.booking->>'institutionLocationId'), ''), referrer_location.institution_location_id, 'na') AS institution_location_id
	) booking_parameter
	LEFT OUTER JOIN clinic booking_clinic
		ON booking_parameter.clinic_id ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
		AND booking_clinic.clinic_id = booking_parameter.clinic_id::UUID
	CROSS JOIN LATERAL (
		SELECT
			COALESCE(
				booking_parameter.provider_search_result_type_id,
				CASE
					WHEN booking_parameter.clinic_id IS NOT NULL
							AND booking_clinic.appointment_booking_level_id = 'CLINIC' THEN 'CLINIC'
					WHEN booking_parameter.provider_id IS NOT NULL THEN 'PROVIDER'
					WHEN booking_parameter.clinic_id IS NOT NULL THEN 'CLINIC'
					ELSE NULL
				END
			) AS provider_search_result_type_id
	) booking_type
	CROSS JOIN LATERAL (
		SELECT
			booking_type.provider_search_result_type_id,
			CASE
				WHEN booking_type.provider_search_result_type_id = 'PROVIDER' THEN booking_parameter.provider_id
				WHEN booking_type.provider_search_result_type_id = 'CLINIC' THEN booking_parameter.clinic_id
				ELSE NULL
			END AS provider_search_result_id,
			booking_parameter.appointment_type_id,
			booking_parameter.institution_location_id,
			booking_parameter.feature_id
	) booking_route
	GROUP BY
		ir.institution_referrer_id
)
UPDATE institution_referrer ir
SET metadata = JSONB_SET(ir.metadata, '{resultScreens}', updated_result_screens.result_screens, FALSE)
FROM updated_result_screens
WHERE ir.institution_referrer_id = updated_result_screens.institution_referrer_id
AND ir.metadata->'resultScreens' IS DISTINCT FROM updated_result_screens.result_screens;


COMMIT;
