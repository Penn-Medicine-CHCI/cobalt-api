BEGIN;
SELECT _v.register_patch('250-autism-clinic-referrer', NULL, NULL);

-- Drafted from the Figma Autism Clinic flow and supporting pilot outline PDF on 2026-03-24.
-- This patch aligns the eligibility questions to the PDF, seeds the Penn Autism Clinic and
-- John Skokowski as the single COBALT-scheduled provider, and routes eligible users through
-- a data-driven fullscreen results screen before sending them to a booking page filtered to the
-- correct provider and appointment type. Shared modal/page content is seeded here as
-- institution-scoped content snippets and referenced by key from both the referrer page
-- and the fullscreen screening questions.

CREATE TABLE IF NOT EXISTS content_snippet_type (
  content_snippet_type_id VARCHAR PRIMARY KEY
);

INSERT INTO content_snippet_type (content_snippet_type_id)
SELECT 'HTML'
WHERE NOT EXISTS (
  SELECT 1
  FROM content_snippet_type
  WHERE content_snippet_type_id = 'HTML'
);

INSERT INTO content_snippet_type (content_snippet_type_id)
SELECT 'TABLE'
WHERE NOT EXISTS (
  SELECT 1
  FROM content_snippet_type
  WHERE content_snippet_type_id = 'TABLE'
);

CREATE TABLE IF NOT EXISTS content_snippet (
  content_snippet_id UUID PRIMARY KEY,
  institution_id VARCHAR NOT NULL REFERENCES institution,
  content_snippet_key TEXT NOT NULL,
  content_snippet_type_id VARCHAR NOT NULL REFERENCES content_snippet_type,
  title TEXT,
  body_html TEXT,
  content JSONB NOT NULL DEFAULT '{}'::JSONB,
  dismiss_button_text TEXT,
  created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_content_snippet_institution_key
  ON content_snippet(institution_id, content_snippet_key);

DO $$
DECLARE
  v_created_by_email_address CONSTANT TEXT := 'admin@cobaltinnovations.org';
  v_created_by_account_id UUID;
  v_from_institution_id CONSTANT TEXT := 'COBALT';
  v_to_institution_id CONSTANT TEXT := 'COBALT_IC_SELF_REFERRAL';

  v_clinic_description CONSTANT TEXT := 'Penn Autism Clinic';
  v_clinic_treatment_description CONSTANT TEXT := 'Penn Autism Clinic intake and consult calls with a patient care manager.';
  v_placeholder_clinic_id CONSTANT UUID := '170a29e6-ae8d-41f5-a4d1-25305c2dd997';
  v_clinic_id UUID;

  v_provider_name CONSTANT TEXT := 'John Skokowski';
  v_provider_url_name CONSTANT TEXT := 'john-skokowski';
  v_provider_title CONSTANT TEXT := 'Patient Care Manager';
  v_provider_email_address CONSTANT TEXT := 'REPLACE_WITH_JOHN_SKOKOWSKI_EMAIL';
  v_provider_description CONSTANT TEXT := $provider_description$
    <p class="mb-0">
      John Skokowski is the Penn Autism Clinic patient care manager. He leads the initial intake or consult call,
      gathers the information needed for clinical review, and helps determine the most appropriate next step for care.
    </p>
  $provider_description$;
  v_placeholder_provider_id CONSTANT UUID := 'f56acf8f-3d10-431d-8f65-4c379658bfcc';
  v_provider_id UUID;

  v_intake_appointment_type_name CONSTANT TEXT := 'Autism Clinic Intake Call';
  v_consult_appointment_type_name CONSTANT TEXT := 'Autism Clinic Consult Call';
  v_placeholder_intake_appointment_type_id CONSTANT UUID := '7d16a9d5-7d7e-49db-b6b2-6639603db2e3';
  v_placeholder_consult_appointment_type_id CONSTANT UUID := '7f64e3a0-0dc3-43c6-912a-4ba71d9b2c03';
  v_intake_appointment_type_id UUID;
  v_consult_appointment_type_id UUID;
  v_provider_clinic_id CONSTANT UUID := '7ea1a12a-41ec-4f00-b8bc-714dffdc4ea6';
  v_monday_logical_availability_id CONSTANT UUID := 'fbb2ea49-fb62-470a-8f87-04f7c918b81b';
  v_thursday_logical_availability_id CONSTANT UUID := 'e96ddcb0-7588-4087-b615-c230ca7784de';

  v_referrer_url_name CONSTANT TEXT := 'autism-clinic';
  v_referrer_title CONSTANT TEXT := 'Pilot: Penn Autism Clinic';
  v_referrer_description CONSTANT TEXT := 'Schedule a call with Autism Clinic staff or visit the website for more information.';
  v_referrer_page_content CONSTANT TEXT := $page_content$
    <section>
      <h2>What is the Penn Autism Clinic?</h2>
      <p>
        The Penn Autism Clinic provides diagnostic assessments and ongoing support for children, teens and adults who have, or may have, autism. Clinicians can perform evaluations, offer treatment recommendations, and coordinate with intervention providers to guide families through diagnosis and long-term care.
      </p>
    </section>
    <section>
      <h2>Who can self-schedule a Penn Autism Clinic appointment?</h2>
      <p><strong>If you are seeking services for yourself:</strong></p>
      <p>You must be a benefits-eligible employee of UPHS or UPenn.</p>
      <p><strong>If you are seeking services for a dependent:</strong></p>
      <p>You must be the legal guardian of the dependent and your dependent must be covered under your UPHS or UPenn health insurance.</p>
    </section>
  $page_content$;
  v_referrer_cta_title CONSTANT TEXT := 'Get started with Penn Autism Clinic';
  v_referrer_cta_description CONSTANT TEXT := 'Check your eligibility to self-schedule a Penn Autism Clinic appointment by clicking below or visit the website for more information.';

  v_flow_name CONSTANT TEXT := 'Penn Autism Clinic Referral Screening Flow';
  v_screening_name CONSTANT TEXT := 'Penn Autism Clinic Assessment';
  v_result_page_path CONSTANT TEXT := '/referrals/autism-clinic/results';
  v_return_to_query_value CONSTANT TEXT := '%2Freferrals%2Fautism-clinic';
  v_booking_feature_id CONSTANT TEXT := 'MENTAL_HEALTH_PROVIDERS';
  v_booking_page_title CONSTANT TEXT := 'Autism Clinic Booking';

  v_question_1_text CONSTANT TEXT := 'Are you a benefits-eligible employee of UPHS or UPenn ?';
  v_question_2_text CONSTANT TEXT := 'Who are you seeking care for?';
  v_question_3_text CONSTANT TEXT := 'Are you currently their legal guardian?';
  v_question_4_text CONSTANT TEXT := 'Are they covered under your Penn insurance (UPHS or UPenn)?';
  v_question_5_text CONSTANT TEXT := 'Which of the following best matches your current needs?';
  v_question_5_option_1_text CONSTANT TEXT := 'I have a diagnosis and need a care plan.';
  v_question_5_option_2_text CONSTANT TEXT := 'A professional (like a teacher or pediatrician) has recommended an evaluation to determine if an autism diagnosis is appropriate.';
  v_question_5_option_3_text CONSTANT TEXT := 'I have concerns and don''t know where to start.';
  v_question_1_footer_html TEXT;
  v_question_4_footer_html TEXT;
  v_insurance_footer_action_metadata CONSTANT TEXT := '{"footerAction":{"actionType":"OPEN_CONTENT_MODAL","label":"View Insurance List","contentSnippetKey":"AUTISM_ACCEPTED_INSURANCES"}}';

  v_scoring_function TEXT;
  v_orchestration_function TEXT := $orchestration$
output.crisisIndicated = false;
output.completed = false;
output.nextScreeningId = null;

const referrerIntakeScreening = input.screeningSessionScreenings[0];

if (input.screeningSessionScreenings.length !== 1) {
  throw "There is an unexpected number of screening session screenings";
}

if (referrerIntakeScreening.completed) {
  output.completed = true;
}
$orchestration$;
  v_results_function TEXT := $results$
output.supportRoleRecommendations = [];
output.recommendLegacyContentAnswerIds = true;
output.legacyContentAnswerIds = [];
$results$;
  v_destination_function TEXT;
  v_referrer_metadata TEXT;

  v_screening_id UUID := uuid_generate_v4();
  v_screening_version_id UUID := uuid_generate_v4();
  v_screening_flow_id UUID := uuid_generate_v4();
  v_screening_flow_version_id UUID := uuid_generate_v4();

  v_question_1_id UUID := uuid_generate_v4();
  v_question_2_id UUID := uuid_generate_v4();
  v_question_3_id UUID := uuid_generate_v4();
  v_question_4_id UUID := uuid_generate_v4();
  v_question_5_id UUID := uuid_generate_v4();

  v_question_1_yes_option_id UUID := uuid_generate_v4();
  v_question_1_no_option_id UUID := uuid_generate_v4();
  v_question_2_myself_option_id UUID := uuid_generate_v4();
  v_question_2_dependent_option_id UUID := uuid_generate_v4();
  v_question_3_yes_option_id UUID := uuid_generate_v4();
  v_question_3_no_option_id UUID := uuid_generate_v4();
  v_question_4_yes_option_id UUID := uuid_generate_v4();
  v_question_4_no_option_id UUID := uuid_generate_v4();
  v_question_5_option_1_id UUID := uuid_generate_v4();
  v_question_5_option_2_id UUID := uuid_generate_v4();
  v_question_5_option_3_id UUID := uuid_generate_v4();
BEGIN
  SELECT account.account_id
  INTO v_created_by_account_id
  FROM account
  WHERE LOWER(TRIM(account.email_address)) = LOWER(TRIM(v_created_by_email_address))
  ORDER BY account.account_id
  LIMIT 1;

  IF v_created_by_account_id IS NULL THEN
    RAISE EXCEPTION 'Account with email "%" was not found', v_created_by_email_address;
  END IF;

  IF EXISTS (
    SELECT 1
    FROM institution_referrer
    WHERE from_institution_id = v_from_institution_id
      AND url_name = v_referrer_url_name
  ) THEN
    RAISE NOTICE 'institution_referrer "%" already exists for institution "%"', v_referrer_url_name, v_from_institution_id;
    RETURN;
  END IF;

  IF EXISTS (
    SELECT 1
    FROM screening_flow
    WHERE institution_id = v_from_institution_id
      AND LOWER(TRIM(name)) = LOWER(TRIM(v_flow_name))
  ) THEN
    RAISE EXCEPTION 'screening flow "%" already exists for institution "%"', v_flow_name, v_from_institution_id;
  END IF;

  v_question_1_footer_html := '<strong>Not sure about your coverage?</strong>';
  v_question_4_footer_html := '<strong>Not sure about your coverage?</strong>';

  SELECT c.clinic_id
  INTO v_clinic_id
  FROM clinic c
  WHERE c.institution_id = v_from_institution_id
    AND LOWER(TRIM(c.description)) = LOWER(TRIM(v_clinic_description))
  ORDER BY c.clinic_id
  LIMIT 1;

  IF v_clinic_id IS NULL THEN
    v_clinic_id := v_placeholder_clinic_id;

    INSERT INTO clinic (
      clinic_id,
      description,
      treatment_description,
      institution_id,
      show_intake_assessment_prompt
    ) VALUES (
      v_clinic_id,
      v_clinic_description,
      v_clinic_treatment_description,
      v_from_institution_id,
      FALSE
    );
  END IF;

  SELECT at.appointment_type_id
  INTO v_intake_appointment_type_id
  FROM appointment_type at
  WHERE LOWER(TRIM(at.name)) = LOWER(TRIM(v_intake_appointment_type_name))
    AND at.scheduling_system_id = 'COBALT'
    AND at.duration_in_minutes = 60
  ORDER BY at.appointment_type_id
  LIMIT 1;

  IF v_intake_appointment_type_id IS NULL THEN
    v_intake_appointment_type_id := v_placeholder_intake_appointment_type_id;

    INSERT INTO appointment_type (
      appointment_type_id,
      acuity_appointment_type_id,
      name,
      duration_in_minutes,
      visit_type_id,
      scheduling_system_id
    ) VALUES (
      v_intake_appointment_type_id,
      NULL,
      v_intake_appointment_type_name,
      60,
      'INITIAL',
      'COBALT'
    );
  END IF;

  SELECT at.appointment_type_id
  INTO v_consult_appointment_type_id
  FROM appointment_type at
  WHERE LOWER(TRIM(at.name)) = LOWER(TRIM(v_consult_appointment_type_name))
    AND at.scheduling_system_id = 'COBALT'
    AND at.duration_in_minutes = 30
  ORDER BY at.appointment_type_id
  LIMIT 1;

  IF v_consult_appointment_type_id IS NULL THEN
    v_consult_appointment_type_id := v_placeholder_consult_appointment_type_id;

    INSERT INTO appointment_type (
      appointment_type_id,
      acuity_appointment_type_id,
      name,
      duration_in_minutes,
      visit_type_id,
      scheduling_system_id
    ) VALUES (
      v_consult_appointment_type_id,
      NULL,
      v_consult_appointment_type_name,
      30,
      'INITIAL',
      'COBALT'
    );
  END IF;

  SELECT p.provider_id
  INTO v_provider_id
  FROM provider p
  WHERE p.institution_id = v_from_institution_id
    AND (
      LOWER(TRIM(p.url_name)) = LOWER(TRIM(v_provider_url_name))
      OR LOWER(TRIM(p.name)) = LOWER(TRIM(v_provider_name))
    )
  ORDER BY p.provider_id
  LIMIT 1;

  IF v_provider_id IS NULL THEN
    v_provider_id := v_placeholder_provider_id;

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
      description
    ) VALUES (
      v_provider_id,
      v_from_institution_id,
      v_provider_name,
      v_provider_title,
      v_provider_email_address,
      'en-US',
      'America/New_York',
      v_clinic_description,
      v_clinic_description,
      'Autism Clinic',
      TRUE,
      'COBALT',
      'COBALT',
      v_provider_url_name,
      v_provider_description
    );
  END IF;

  INSERT INTO provider_clinic (
    provider_clinic_id,
    provider_id,
    clinic_id,
    primary_clinic
  )
  SELECT
    v_provider_clinic_id,
    v_provider_id,
    v_clinic_id,
    TRUE
  WHERE NOT EXISTS (
    SELECT 1
    FROM provider_clinic existing
    WHERE existing.provider_id = v_provider_id
      AND existing.clinic_id = v_clinic_id
  );

  INSERT INTO provider_support_role (
    provider_id,
    support_role_id
  )
  SELECT
    v_provider_id,
    'CARE_MANAGER'
  WHERE NOT EXISTS (
    SELECT 1
    FROM provider_support_role existing
    WHERE existing.provider_id = v_provider_id
      AND existing.support_role_id = 'CARE_MANAGER'
  );

  INSERT INTO provider_appointment_type (
    provider_id,
    appointment_type_id,
    display_order
  )
  SELECT
    v_provider_id,
    v_intake_appointment_type_id,
    COALESCE((
      SELECT MAX(existing.display_order) + 1
      FROM provider_appointment_type existing
      WHERE existing.provider_id = v_provider_id
    ), 1)
  WHERE NOT EXISTS (
    SELECT 1
    FROM provider_appointment_type existing
    WHERE existing.provider_id = v_provider_id
      AND existing.appointment_type_id = v_intake_appointment_type_id
  );

  INSERT INTO provider_appointment_type (
    provider_id,
    appointment_type_id,
    display_order
  )
  SELECT
    v_provider_id,
    v_consult_appointment_type_id,
    COALESCE((
      SELECT MAX(existing.display_order) + 1
      FROM provider_appointment_type existing
      WHERE existing.provider_id = v_provider_id
    ), 1)
  WHERE NOT EXISTS (
    SELECT 1
    FROM provider_appointment_type existing
    WHERE existing.provider_id = v_provider_id
      AND existing.appointment_type_id = v_consult_appointment_type_id
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
  )
  SELECT
    v_monday_logical_availability_id,
    v_provider_id,
    TIMESTAMP '2026-03-30 09:00:00',
    TIMESTAMP '2099-12-31 12:00:00',
    'OPEN',
    'DAILY',
    FALSE,
    TRUE,
    FALSE,
    FALSE,
    FALSE,
    FALSE,
    FALSE,
    v_created_by_account_id,
    v_created_by_account_id
  WHERE NOT EXISTS (
    SELECT 1
    FROM logical_availability existing
    WHERE existing.logical_availability_id = v_monday_logical_availability_id
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
  )
  SELECT
    v_thursday_logical_availability_id,
    v_provider_id,
    TIMESTAMP '2026-03-26 13:00:00',
    TIMESTAMP '2099-12-30 16:00:00',
    'OPEN',
    'DAILY',
    FALSE,
    FALSE,
    FALSE,
    FALSE,
    TRUE,
    FALSE,
    FALSE,
    v_created_by_account_id,
    v_created_by_account_id
  WHERE NOT EXISTS (
    SELECT 1
    FROM logical_availability existing
    WHERE existing.logical_availability_id = v_thursday_logical_availability_id
  );

  INSERT INTO logical_availability_appointment_type (
    logical_availability_id,
    appointment_type_id
  )
  SELECT
    v_monday_logical_availability_id,
    v_intake_appointment_type_id
  WHERE NOT EXISTS (
    SELECT 1
    FROM logical_availability_appointment_type existing
    WHERE existing.logical_availability_id = v_monday_logical_availability_id
      AND existing.appointment_type_id = v_intake_appointment_type_id
  );

  INSERT INTO logical_availability_appointment_type (
    logical_availability_id,
    appointment_type_id
  )
  SELECT
    v_monday_logical_availability_id,
    v_consult_appointment_type_id
  WHERE NOT EXISTS (
    SELECT 1
    FROM logical_availability_appointment_type existing
    WHERE existing.logical_availability_id = v_monday_logical_availability_id
      AND existing.appointment_type_id = v_consult_appointment_type_id
  );

  INSERT INTO logical_availability_appointment_type (
    logical_availability_id,
    appointment_type_id
  )
  SELECT
    v_thursday_logical_availability_id,
    v_intake_appointment_type_id
  WHERE NOT EXISTS (
    SELECT 1
    FROM logical_availability_appointment_type existing
    WHERE existing.logical_availability_id = v_thursday_logical_availability_id
      AND existing.appointment_type_id = v_intake_appointment_type_id
  );

  INSERT INTO logical_availability_appointment_type (
    logical_availability_id,
    appointment_type_id
  )
  SELECT
    v_thursday_logical_availability_id,
    v_consult_appointment_type_id
  WHERE NOT EXISTS (
    SELECT 1
    FROM logical_availability_appointment_type existing
    WHERE existing.logical_availability_id = v_thursday_logical_availability_id
      AND existing.appointment_type_id = v_consult_appointment_type_id
  );

  v_referrer_metadata := format($metadata$
{
  "screening": {
    "fullscreen": true,
    "title": "%s",
    "instructionsHtml": "<p class='mb-0'>Please answer the questions to the best of your knowledge.</p>"
  },
  "page": {
    "contentSnippetSections": [
      {
        "contentSnippetKey": "AUTISM_ACCEPTED_INSURANCES",
        "title": "Accepted Insurances",
        "leadHtml": "<p><strong>You will need to confirm your insurance before booking.</strong></p>"
      }
    ]
  },
  "resultScreens": {
    "INTAKE_CALL": {
      "recommendation": "Schedule a 60 minute intake call with a patient care manager",
      "bodyHtml": "<p class='mb-4'>We'll schedule time for an intake conversation with John Skokowski so the clinic can review your needs and determine the best next step for care.</p><ul class='mb-0 ps-4'><li class='mb-3'>We'll ask you to submit any existing documentation beforehand. More details will be included in your confirmation email.</li><li class='mb-3'>During the call, the patient care manager will collect additional information, including demographics, SSN, primary care details, and a summary of concerns over the past six months.</li><li class='mb-0'>A patient folder will be created and shared with a clinician for review.</li></ul>",
      "noteHtml": "<p class='mb-0'>If you already have reports, school documentation, or prior evaluations, please send them in before the call when possible.</p>",
      "buttonText": "Continue to Scheduling",
      "booking": {
        "path": "/connect-with-support/autism-clinic-booking",
        "featureId": "%s",
        "pageTitle": "%s",
        "pageDescription": "Schedule a 60 minute Penn Autism Clinic intake call with John Skokowski.",
        "clinicIds": ["%s"],
        "providerId": "%s",
        "appointmentTypeIds": ["%s"]
      }
    },
    "CONSULT_EVALUATION": {
      "recommendation": "Schedule a 30 minute consult call with a patient care manager",
      "bodyHtml": "<p class='mb-4'>We'll schedule time for a consult conversation with John Skokowski so you can discuss what is going on and assess the best next step.</p><ul class='mb-0 ps-4'><li class='mb-3'>No preparation is needed for this call.</li><li class='mb-3'>During the call, you'll discuss what is going on with the patient and assess their needs.</li><li class='mb-0'>The next step may be a longer, formal intake call with the patient care manager.</li></ul>",
      "noteHtml": "<p class='mb-0'>While the clinic can start consultation right away, the waitlist for a formal diagnostic evaluation varies depending on age.</p>",
      "buttonText": "Continue to Scheduling",
      "booking": {
        "path": "/connect-with-support/autism-clinic-booking",
        "featureId": "%s",
        "pageTitle": "%s",
        "pageDescription": "Schedule a 30 minute Penn Autism Clinic consult call with John Skokowski.",
        "clinicIds": ["%s"],
        "providerId": "%s",
        "appointmentTypeIds": ["%s"]
      }
    },
    "CONSULT_GENERAL": {
      "recommendation": "Schedule a 30 minute consult call with a patient care manager",
      "bodyHtml": "<p class='mb-4'>We'll schedule time for a consult conversation with John Skokowski so you can talk through concerns and get an expert perspective on the best path forward.</p><ul class='mb-0 ps-4'><li class='mb-3'>No preparation is needed for this call.</li><li class='mb-3'>During the call, you'll discuss any noticed behaviors or milestones that concern you.</li><li class='mb-0'>The goal is to help you understand the best next step for support or evaluation.</li></ul>",
      "buttonText": "Continue to Scheduling",
      "booking": {
        "path": "/connect-with-support/autism-clinic-booking",
        "featureId": "%s",
        "pageTitle": "%s",
        "pageDescription": "Schedule a 30 minute Penn Autism Clinic consult call with John Skokowski.",
        "clinicIds": ["%s"],
        "providerId": "%s",
        "appointmentTypeIds": ["%s"]
      }
    }
  }
}
$metadata$,
    v_screening_name,
    v_booking_feature_id,
    v_booking_page_title,
    v_clinic_id,
    v_provider_id,
    v_intake_appointment_type_id,
    v_booking_feature_id,
    v_booking_page_title,
    v_clinic_id,
    v_provider_id,
    v_consult_appointment_type_id,
    v_booking_feature_id,
    v_booking_page_title,
    v_clinic_id,
    v_provider_id,
    v_consult_appointment_type_id
  );

  v_scoring_function := format($scoring$
output.completed = false;
output.score = { overallScore: 0 };
output.nextScreeningQuestionId = null;

function answerOptionTextForQuestion(questionId) {
  const answerIds = input.screeningAnswerIdsByScreeningQuestionId[questionId] || [];
  if (answerIds.length === 0) {
    return null;
  }

  const answerOption = input.screeningAnswerOptionsByScreeningAnswerId[answerIds[0]];
  return answerOption ? answerOption.answerOptionText : null;
}

const benefitsEligibleAnswer = answerOptionTextForQuestion('%s');
const seekingCareForAnswer = answerOptionTextForQuestion('%s');
const legalGuardianAnswer = answerOptionTextForQuestion('%s');
const dependentCoverageAnswer = answerOptionTextForQuestion('%s');
const currentNeedsAnswer = answerOptionTextForQuestion('%s');

if (benefitsEligibleAnswer === 'No') {
  output.completed = true;
} else if (benefitsEligibleAnswer !== 'Yes') {
  output.completed = false;
} else if (seekingCareForAnswer === 'A dependent' && legalGuardianAnswer === null) {
  output.nextScreeningQuestionId = '%s';
} else if (seekingCareForAnswer === 'A dependent' && legalGuardianAnswer === 'No') {
  output.completed = true;
} else if (seekingCareForAnswer === 'A dependent' && dependentCoverageAnswer === null) {
  output.nextScreeningQuestionId = '%s';
} else if (seekingCareForAnswer === 'A dependent' && dependentCoverageAnswer === 'No') {
  output.completed = true;
} else if ((seekingCareForAnswer === 'Myself') || (seekingCareForAnswer === 'A dependent')) {
  if (currentNeedsAnswer === null) {
    output.nextScreeningQuestionId = '%s';
  } else {
    output.completed = true;
    output.score.overallScore = 1;
  }
}
$scoring$, v_question_1_id, v_question_2_id, v_question_3_id, v_question_4_id, v_question_5_id, v_question_3_id, v_question_4_id, v_question_5_id);

  v_destination_function := format($destination$
const referrerIntakeScreening = input.screeningSessionScreenings[0];
const screeningResults = input.screeningResultsByScreeningSessionScreeningId[referrerIntakeScreening.screeningSessionScreeningId] || [];

output.screeningSessionDestinationId = null;
output.context = {};

function answerOptionTextForQuestion(questionId) {
  const screeningResult = screeningResults.find((result) => result.screeningQuestionId === questionId);

  if (!screeningResult || !screeningResult.screeningResponses || screeningResult.screeningResponses.length === 0) {
    return null;
  }

  const firstResponse = screeningResult.screeningResponses[0];
  return firstResponse.screeningAnswerOption ? firstResponse.screeningAnswerOption.answerOptionText : null;
}

function answerOptionMetadataForQuestion(questionId) {
  const screeningResult = screeningResults.find((result) => result.screeningQuestionId === questionId);

  if (!screeningResult || !screeningResult.screeningResponses || screeningResult.screeningResponses.length === 0) {
    return {};
  }

  const firstResponse = screeningResult.screeningResponses[0];
  return firstResponse.screeningAnswerOption && firstResponse.screeningAnswerOption.metadata
    ? firstResponse.screeningAnswerOption.metadata
    : {};
}

if (input.screeningSession.completed) {
  if (referrerIntakeScreening.scoreAsObject.overallScore === 1) {
    const currentNeedsMetadata = answerOptionMetadataForQuestion('%s');
    const resultKey = currentNeedsMetadata.resultKey;

    if (!resultKey) {
      throw 'Unable to determine Autism Clinic scheduling result type';
    }

    output.screeningSessionDestinationId = 'INSTITUTION_REFERRAL';
    output.context.institutionReferralUrl = '%s/' + resultKey + '?returnTo=%s';
  } else {
    output.screeningSessionDestinationId = 'INSTITUTION_REFERRER_DETAIL';
    output.context.institutionReferrerUrlName = '%s';
  }
}
$destination$,
    v_question_5_id,
    v_result_page_path,
    v_return_to_query_value,
    v_referrer_url_name
  );

  INSERT INTO screening (
    screening_id,
    name,
    active_screening_version_id,
    created_by_account_id,
    created,
    last_updated
  ) VALUES (
    v_screening_id,
    v_screening_name,
    NULL,
    v_created_by_account_id,
    NOW(),
    NOW()
  );

  INSERT INTO screening_version (
    screening_version_id,
    screening_id,
    screening_type_id,
    created_by_account_id,
    version_number,
    scoring_function,
    created,
    last_updated
  ) VALUES (
    v_screening_version_id,
    v_screening_id,
    'CUSTOM',
    v_created_by_account_id,
    1,
    v_scoring_function,
    NOW(),
    NOW()
  );

  UPDATE screening
  SET active_screening_version_id = v_screening_version_id
  WHERE screening_id = v_screening_id;

  INSERT INTO screening_institution (
    screening_id,
    institution_id
  ) VALUES (
    v_screening_id,
    v_from_institution_id
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
    created,
    last_updated,
    footer_text,
    pre_question_screening_confirmation_prompt_id,
    metadata,
    prefer_autosubmit,
    screening_question_submission_style_id,
    supplement_text
  ) VALUES
    (
      v_question_1_id,
      v_screening_version_id,
      'SINGLE_SELECT',
      'NONE',
      NULL,
      v_question_1_text,
      1,
      1,
      1,
      NOW(),
      NOW(),
      v_question_1_footer_html,
      NULL,
      CAST(v_insurance_footer_action_metadata AS JSONB),
      FALSE,
      'NEXT',
      NULL
    ),
    (
      v_question_2_id,
      v_screening_version_id,
      'SINGLE_SELECT',
      'NONE',
      NULL,
      v_question_2_text,
      1,
      1,
      2,
      NOW(),
      NOW(),
      NULL,
      NULL,
      NULL,
      FALSE,
      'NEXT',
      NULL
    ),
    (
      v_question_3_id,
      v_screening_version_id,
      'SINGLE_SELECT',
      'NONE',
      NULL,
      v_question_3_text,
      1,
      1,
      3,
      NOW(),
      NOW(),
      NULL,
      NULL,
      NULL,
      FALSE,
      'NEXT',
      NULL
    ),
    (
      v_question_4_id,
      v_screening_version_id,
      'SINGLE_SELECT',
      'NONE',
      NULL,
      v_question_4_text,
      1,
      1,
      4,
      NOW(),
      NOW(),
      v_question_4_footer_html,
      NULL,
      CAST(v_insurance_footer_action_metadata AS JSONB),
      FALSE,
      'NEXT',
      NULL
    ),
    (
      v_question_5_id,
      v_screening_version_id,
      'SINGLE_SELECT',
      'NONE',
      NULL,
      v_question_5_text,
      1,
      1,
      5,
      NOW(),
      NOW(),
      NULL,
      NULL,
      NULL,
      FALSE,
      'NEXT',
      NULL
    );

  INSERT INTO screening_answer_option (
    screening_answer_option_id,
    screening_question_id,
    answer_option_text,
    score,
    indicates_crisis,
    display_order,
    created,
    last_updated,
    freeform_supplement,
    freeform_supplement_text,
    metadata,
    freeform_supplement_text_auto_show,
    freeform_supplement_content_hint_id
  ) VALUES
    (v_question_1_yes_option_id, v_question_1_id, 'Yes', 1, FALSE, 1, NOW(), NOW(), FALSE, NULL, NULL, FALSE, NULL),
    (v_question_1_no_option_id, v_question_1_id, 'No', 0, FALSE, 2, NOW(), NOW(), FALSE, NULL, NULL, FALSE, NULL),
    (v_question_2_myself_option_id, v_question_2_id, 'Myself', 1, FALSE, 1, NOW(), NOW(), FALSE, NULL, NULL, FALSE, NULL),
    (v_question_2_dependent_option_id, v_question_2_id, 'A dependent', 1, FALSE, 2, NOW(), NOW(), FALSE, NULL, NULL, FALSE, NULL),
    (v_question_3_yes_option_id, v_question_3_id, 'Yes', 1, FALSE, 1, NOW(), NOW(), FALSE, NULL, NULL, FALSE, NULL),
    (v_question_3_no_option_id, v_question_3_id, 'No', 0, FALSE, 2, NOW(), NOW(), FALSE, NULL, NULL, FALSE, NULL),
    (v_question_4_yes_option_id, v_question_4_id, 'Yes', 1, FALSE, 1, NOW(), NOW(), FALSE, NULL, NULL, FALSE, NULL),
    (v_question_4_no_option_id, v_question_4_id, 'No', 0, FALSE, 2, NOW(), NOW(), FALSE, NULL, NULL, FALSE, NULL),
    (v_question_5_option_1_id, v_question_5_id, v_question_5_option_1_text, 1, FALSE, 1, NOW(), NOW(), FALSE, NULL, CAST('{"resultKey":"INTAKE_CALL"}' AS JSONB), FALSE, NULL),
    (v_question_5_option_2_id, v_question_5_id, v_question_5_option_2_text, 1, FALSE, 2, NOW(), NOW(), FALSE, NULL, CAST('{"resultKey":"CONSULT_EVALUATION"}' AS JSONB), FALSE, NULL),
    (v_question_5_option_3_id, v_question_5_id, v_question_5_option_3_text, 1, FALSE, 3, NOW(), NOW(), FALSE, NULL, CAST('{"resultKey":"CONSULT_GENERAL"}' AS JSONB), FALSE, NULL);

  INSERT INTO screening_flow (
    screening_flow_id,
    institution_id,
    active_screening_flow_version_id,
    screening_flow_type_id,
    created_by_account_id,
    name,
    created,
    last_updated,
    analytics_name
  ) VALUES (
    v_screening_flow_id,
    v_from_institution_id,
    NULL,
    'CUSTOM',
    v_created_by_account_id,
    v_flow_name,
    NOW(),
    NOW(),
    NULL
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
    created_by_account_id,
    created,
    last_updated,
    skippable,
    pre_completion_screening_confirmation_prompt_id,
    minutes_until_retake,
    recommendation_expiration_minutes,
    screening_flow_skip_type_id,
    initialization_function
  ) VALUES (
    v_screening_flow_version_id,
    v_screening_flow_id,
    v_screening_id,
    FALSE,
    1,
    v_orchestration_function,
    v_results_function,
    v_destination_function,
    v_created_by_account_id,
    NOW(),
    NOW(),
    FALSE,
    NULL,
    1440,
    8760,
    'EXIT',
    NULL
  );

  UPDATE screening_flow
  SET active_screening_flow_version_id = v_screening_flow_version_id
  WHERE screening_flow_id = v_screening_flow_id;

  INSERT INTO institution_referrer (
    institution_referrer_id,
    from_institution_id,
    to_institution_id,
    institution_feature_id,
    intake_screening_flow_id,
    url_name,
    title,
    description,
    page_content,
    cta_title,
    cta_description,
    metadata
  ) VALUES (
    uuid_generate_v4(),
    v_from_institution_id,
    v_to_institution_id,
    NULL,
    v_screening_flow_id,
    v_referrer_url_name,
    v_referrer_title,
    v_referrer_description,
    v_referrer_page_content,
    v_referrer_cta_title,
    v_referrer_cta_description,
    CAST(v_referrer_metadata AS JSONB)
  );
END $$;

-- Attach the new referrer to the same PENN institution_feature used by TEAM.
WITH team_feature_referrer AS (
  SELECT
    ifir.institution_feature_id,
    ifir.display_order
  FROM institution_referrer ir
  JOIN institution_feature_institution_referrer ifir
    ON ifir.institution_referrer_id = ir.institution_referrer_id
  WHERE ir.from_institution_id = 'COBALT'
    AND ir.to_institution_id = 'COBALT_IC_SELF_REFERRAL'
    AND ir.url_name = 'team-clinic-pilot'
),
autism_referrer AS (
  SELECT institution_referrer_id
  FROM institution_referrer
  WHERE from_institution_id = 'COBALT'
    AND url_name = 'autism-clinic'
)
INSERT INTO institution_feature_institution_referrer (
  institution_feature_institution_referrer_id,
  institution_feature_id,
  institution_referrer_id,
  cta_title,
  cta_description,
  display_order
)
SELECT
  uuid_generate_v4(),
  tfr.institution_feature_id,
  ar.institution_referrer_id,
  'Autism Clinic Pilot: Check Your Eligibility',
  'Eligible Penn employees can now complete a short Autism Clinic eligibility assessment and, if eligible, continue to a clinic-specific booking experience.',
  tfr.display_order + 1
FROM team_feature_referrer tfr
CROSS JOIN autism_referrer ar
WHERE NOT EXISTS (
  SELECT 1
  FROM institution_feature_institution_referrer existing
  WHERE existing.institution_feature_id = tfr.institution_feature_id
    AND existing.institution_referrer_id = ar.institution_referrer_id
);

-- Copy the same Penn location gating used by TEAM.
WITH team_rows AS (
  SELECT
    team_ifir.institution_feature_institution_referrer_id AS team_ifir_id,
    autism_ifir.institution_feature_institution_referrer_id AS autism_ifir_id
  FROM institution_referrer team_ir
  JOIN institution_feature_institution_referrer team_ifir
    ON team_ifir.institution_referrer_id = team_ir.institution_referrer_id
  JOIN institution_referrer autism_ir
    ON autism_ir.from_institution_id = 'COBALT'
   AND autism_ir.url_name = 'autism-clinic'
  JOIN institution_feature_institution_referrer autism_ifir
    ON autism_ifir.institution_referrer_id = autism_ir.institution_referrer_id
  WHERE team_ir.from_institution_id = 'COBALT'
    AND team_ir.url_name = 'team-clinic-pilot'
)
INSERT INTO institution_feature_referrer_location (
  institution_feature_institution_referrer_id,
  institution_location_id
)
SELECT
  tr.autism_ifir_id,
  ifrl.institution_location_id
FROM team_rows tr
JOIN institution_feature_referrer_location ifrl
  ON ifrl.institution_feature_institution_referrer_id = tr.team_ifir_id
WHERE NOT EXISTS (
  SELECT 1
  FROM institution_feature_referrer_location existing
  WHERE existing.institution_feature_institution_referrer_id = tr.autism_ifir_id
    AND existing.institution_location_id = ifrl.institution_location_id
);

-- Copy any TEAM account-source gating if Penn later adds it.
WITH team_rows AS (
  SELECT
    team_ifir.institution_feature_institution_referrer_id AS team_ifir_id,
    autism_ifir.institution_feature_institution_referrer_id AS autism_ifir_id
  FROM institution_referrer team_ir
  JOIN institution_feature_institution_referrer team_ifir
    ON team_ifir.institution_referrer_id = team_ir.institution_referrer_id
  JOIN institution_referrer autism_ir
    ON autism_ir.from_institution_id = 'COBALT'
   AND autism_ir.url_name = 'autism-clinic'
  JOIN institution_feature_institution_referrer autism_ifir
    ON autism_ifir.institution_referrer_id = autism_ir.institution_referrer_id
  WHERE team_ir.from_institution_id = 'COBALT'
    AND team_ir.url_name = 'team-clinic-pilot'
)
INSERT INTO institution_feature_referrer_account_source (
  institution_feature_institution_referrer_id,
  account_source_id
)
SELECT
  tr.autism_ifir_id,
  ifras.account_source_id
FROM team_rows tr
JOIN institution_feature_referrer_account_source ifras
  ON ifras.institution_feature_institution_referrer_id = tr.team_ifir_id
WHERE NOT EXISTS (
  SELECT 1
  FROM institution_feature_referrer_account_source existing
  WHERE existing.institution_feature_institution_referrer_id = tr.autism_ifir_id
    AND existing.account_source_id = ifras.account_source_id
);

DO $$
DECLARE
  v_from_institution_id CONSTANT TEXT := 'COBALT';
  v_referrer_url_name CONSTANT TEXT := 'autism-clinic';
  v_screening_name CONSTANT TEXT := 'Penn Autism Clinic Assessment';
  v_screening_title CONSTANT TEXT := 'Penn Autism Clinic Assessment';
  v_booking_feature_id CONSTANT TEXT := 'MENTAL_HEALTH_PROVIDERS';
  v_booking_page_title CONSTANT TEXT := 'Autism Clinic Booking';
  v_provider_name CONSTANT TEXT := 'John Skokowski';
  v_provider_url_name CONSTANT TEXT := 'john-skokowski';
  v_clinic_description CONSTANT TEXT := 'Penn Autism Clinic';
  v_intake_appointment_type_name CONSTANT TEXT := 'Autism Clinic Intake Call';
  v_consult_appointment_type_name CONSTANT TEXT := 'Autism Clinic Consult Call';
  v_question_1_text CONSTANT TEXT := 'Are you a benefits-eligible employee of UPHS or UPenn ?';
  v_question_4_text CONSTANT TEXT := 'Are they covered under your Penn insurance (UPHS or UPenn)?';
  v_question_footer_html CONSTANT TEXT := '<strong>Not sure about your coverage?</strong>';
  v_insurance_footer_action_metadata CONSTANT TEXT := '{"footerAction":{"actionType":"OPEN_CONTENT_MODAL","label":"View Insurance List","contentSnippetKey":"AUTISM_ACCEPTED_INSURANCES"}}';
  v_content_snippet_key CONSTANT TEXT := 'AUTISM_ACCEPTED_INSURANCES';
  v_content_snippet_id CONSTANT UUID := '0a7f4da2-f7f7-4874-b740-7d7cdd3e25c5';
  v_content_snippet_content CONSTANT TEXT := $content$
{
  "columns": [
    { "key": "healthInsurance", "label": "Health Insurance" },
    { "key": "behavioralHealthInsurance", "label": "Behavioral Health Insurance", "align": "right" }
  ],
  "rows": [
    {
      "healthInsurance": "Aetna Choice Point-of-Service (POS) II",
      "behavioralHealthInsurance": "Aetna Behavioral Health Network"
    },
    {
      "healthInsurance": "Aetna High Deductible Health Plan (HDHP) with Health Savings Account",
      "behavioralHealthInsurance": "Aetna Behavioral Health Network"
    },
    {
      "healthInsurance": "Keystone/Amerihealth Health Maintenance Organization (HMO) administered by Independence Blue Cross (IBX)",
      "behavioralHealthInsurance": "IBX Behavioral Health"
    },
    {
      "healthInsurance": "PennCare/Personal Choice Preferred Provider Organization (PPO) Plan administered by Independence Blue Cross (IBX)",
      "behavioralHealthInsurance": "Quest Behavioral Health"
    },
    {
      "healthInsurance": "PennCare High Deductible Health Plan (HDHP) administered by Independence Blue Cross (IBX)",
      "behavioralHealthInsurance": "Quest Behavioral Health"
    }
  ]
}
$content$;
  v_referrer_page_content CONSTANT TEXT := $page_content$
    <section>
      <h2>What is the Penn Autism Clinic?</h2>
      <p>
        The Penn Autism Clinic provides diagnostic assessments and ongoing support for children, teens and adults who have, or may have, autism. Clinicians can perform evaluations, offer treatment recommendations, and coordinate with intervention providers to guide families through diagnosis and long-term care.
      </p>
    </section>
    <section>
      <h2>Who can self-schedule a Penn Autism Clinic appointment?</h2>
      <p><strong>If you are seeking services for yourself:</strong></p>
      <p>You must be a benefits-eligible employee of UPHS or UPenn.</p>
      <p><strong>If you are seeking services for a dependent:</strong></p>
      <p>You must be the legal guardian of the dependent and your dependent must be covered under your UPHS or UPenn health insurance.</p>
    </section>
  $page_content$;
  v_referrer_metadata TEXT;
  v_clinic_id UUID;
  v_provider_id UUID;
  v_intake_appointment_type_id UUID;
  v_consult_appointment_type_id UUID;
BEGIN
  SELECT c.clinic_id
  INTO v_clinic_id
  FROM clinic c
  WHERE c.institution_id = v_from_institution_id
    AND LOWER(TRIM(c.description)) = LOWER(TRIM(v_clinic_description))
  ORDER BY c.clinic_id
  LIMIT 1;

  IF v_clinic_id IS NULL THEN
    RAISE EXCEPTION 'Unable to find clinic "%" for institution "%"', v_clinic_description, v_from_institution_id;
  END IF;

  SELECT p.provider_id
  INTO v_provider_id
  FROM provider p
  WHERE p.institution_id = v_from_institution_id
    AND (
      LOWER(TRIM(p.url_name)) = LOWER(TRIM(v_provider_url_name))
      OR LOWER(TRIM(p.name)) = LOWER(TRIM(v_provider_name))
    )
  ORDER BY p.provider_id
  LIMIT 1;

  IF v_provider_id IS NULL THEN
    RAISE EXCEPTION 'Unable to find provider "%" for institution "%"', v_provider_name, v_from_institution_id;
  END IF;

  SELECT at.appointment_type_id
  INTO v_intake_appointment_type_id
  FROM appointment_type at
  WHERE LOWER(TRIM(at.name)) = LOWER(TRIM(v_intake_appointment_type_name))
    AND at.scheduling_system_id = 'COBALT'
    AND at.duration_in_minutes = 60
  ORDER BY at.appointment_type_id
  LIMIT 1;

  IF v_intake_appointment_type_id IS NULL THEN
    RAISE EXCEPTION 'Unable to find appointment type "%" (60 minutes)', v_intake_appointment_type_name;
  END IF;

  SELECT at.appointment_type_id
  INTO v_consult_appointment_type_id
  FROM appointment_type at
  WHERE LOWER(TRIM(at.name)) = LOWER(TRIM(v_consult_appointment_type_name))
    AND at.scheduling_system_id = 'COBALT'
    AND at.duration_in_minutes = 30
  ORDER BY at.appointment_type_id
  LIMIT 1;

  IF v_consult_appointment_type_id IS NULL THEN
    RAISE EXCEPTION 'Unable to find appointment type "%" (30 minutes)', v_consult_appointment_type_name;
  END IF;

  INSERT INTO content_snippet (
    content_snippet_id,
    institution_id,
    content_snippet_key,
    content_snippet_type_id,
    title,
    body_html,
    content,
    dismiss_button_text,
    created,
    last_updated
  )
  SELECT
    v_content_snippet_id,
    v_from_institution_id,
    v_content_snippet_key,
    'TABLE',
    'Accepted Insurances',
    NULL,
    CAST(v_content_snippet_content AS JSONB),
    'Done',
    NOW(),
    NOW()
  WHERE NOT EXISTS (
    SELECT 1
    FROM content_snippet existing
    WHERE existing.institution_id = v_from_institution_id
      AND existing.content_snippet_key = v_content_snippet_key
  );

  UPDATE content_snippet
  SET
    content_snippet_type_id = 'TABLE',
    title = 'Accepted Insurances',
    body_html = NULL,
    content = CAST(v_content_snippet_content AS JSONB),
    dismiss_button_text = 'Done',
    last_updated = NOW()
  WHERE institution_id = v_from_institution_id
    AND content_snippet_key = v_content_snippet_key;

  v_referrer_metadata := format($metadata$
{
  "screening": {
    "fullscreen": true,
    "title": "%s",
    "instructionsHtml": "<p class='mb-0'>Please answer the questions to the best of your knowledge.</p>"
  },
  "page": {
    "contentSnippetSections": [
      {
        "contentSnippetKey": "%s",
        "title": "Accepted Insurances",
        "leadHtml": "<p><strong>You will need to confirm your insurance before booking.</strong></p>"
      }
    ]
  },
  "resultScreens": {
    "INTAKE_CALL": {
      "recommendation": "Schedule a 60 minute intake call with a patient care manager",
      "bodyHtml": "<p class='mb-4'>We'll schedule time for an intake conversation with John Skokowski so the clinic can review your needs and determine the best next step for care.</p><ul class='mb-0 ps-4'><li class='mb-3'>We'll ask you to submit any existing documentation beforehand. More details will be included in your confirmation email.</li><li class='mb-3'>During the call, the patient care manager will collect additional information, including demographics, SSN, primary care details, and a summary of concerns over the past six months.</li><li class='mb-0'>A patient folder will be created and shared with a clinician for review.</li></ul>",
      "noteHtml": "<p class='mb-0'>If you already have reports, school documentation, or prior evaluations, please send them in before the call when possible.</p>",
      "buttonText": "Continue to Scheduling",
      "booking": {
        "path": "/connect-with-support/autism-clinic-booking",
        "featureId": "%s",
        "pageTitle": "%s",
        "pageDescription": "Schedule a 60 minute Penn Autism Clinic intake call with John Skokowski.",
        "clinicIds": ["%s"],
        "providerId": "%s",
        "appointmentTypeIds": ["%s"]
      }
    },
    "CONSULT_EVALUATION": {
      "recommendation": "Schedule a 30 minute consult call with a patient care manager",
      "bodyHtml": "<p class='mb-4'>We'll schedule time for a consult conversation with John Skokowski so you can discuss what is going on and assess the best next step.</p><ul class='mb-0 ps-4'><li class='mb-3'>No preparation is needed for this call.</li><li class='mb-3'>During the call, you'll discuss what is going on with the patient and assess their needs.</li><li class='mb-0'>The next step may be a longer, formal intake call with the patient care manager.</li></ul>",
      "noteHtml": "<p class='mb-0'>While the clinic can start consultation right away, the waitlist for a formal diagnostic evaluation varies depending on age.</p>",
      "buttonText": "Continue to Scheduling",
      "booking": {
        "path": "/connect-with-support/autism-clinic-booking",
        "featureId": "%s",
        "pageTitle": "%s",
        "pageDescription": "Schedule a 30 minute Penn Autism Clinic consult call with John Skokowski.",
        "clinicIds": ["%s"],
        "providerId": "%s",
        "appointmentTypeIds": ["%s"]
      }
    },
    "CONSULT_GENERAL": {
      "recommendation": "Schedule a 30 minute consult call with a patient care manager",
      "bodyHtml": "<p class='mb-4'>We'll schedule time for a consult conversation with John Skokowski so you can talk through concerns and get an expert perspective on the best path forward.</p><ul class='mb-0 ps-4'><li class='mb-3'>No preparation is needed for this call.</li><li class='mb-3'>During the call, you'll discuss any noticed behaviors or milestones that concern you.</li><li class='mb-0'>The goal is to help you understand the best next step for support or evaluation.</li></ul>",
      "buttonText": "Continue to Scheduling",
      "booking": {
        "path": "/connect-with-support/autism-clinic-booking",
        "featureId": "%s",
        "pageTitle": "%s",
        "pageDescription": "Schedule a 30 minute Penn Autism Clinic consult call with John Skokowski.",
        "clinicIds": ["%s"],
        "providerId": "%s",
        "appointmentTypeIds": ["%s"]
      }
    }
  }
}
$metadata$,
    v_screening_title,
    v_content_snippet_key,
    v_booking_feature_id,
    v_booking_page_title,
    v_clinic_id,
    v_provider_id,
    v_intake_appointment_type_id,
    v_booking_feature_id,
    v_booking_page_title,
    v_clinic_id,
    v_provider_id,
    v_consult_appointment_type_id,
    v_booking_feature_id,
    v_booking_page_title,
    v_clinic_id,
    v_provider_id,
    v_consult_appointment_type_id
  );

  UPDATE institution_referrer
  SET
    page_content = v_referrer_page_content,
    metadata = CAST(v_referrer_metadata AS JSONB)
  WHERE from_institution_id = v_from_institution_id
    AND url_name = v_referrer_url_name;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'Unable to find institution_referrer "%" for institution "%"', v_referrer_url_name, v_from_institution_id;
  END IF;

  UPDATE screening_question sq
  SET
    footer_text = v_question_footer_html,
    metadata = CAST(v_insurance_footer_action_metadata AS JSONB),
    last_updated = NOW()
  FROM screening_version sv,
       screening s
  WHERE sq.screening_version_id = sv.screening_version_id
    AND sv.screening_id = s.screening_id
    AND s.name = v_screening_name
    AND sq.question_text IN (v_question_1_text, v_question_4_text);
END $$;

COMMIT;
