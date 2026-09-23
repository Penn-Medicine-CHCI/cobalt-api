BEGIN;
SELECT _v.register_patch(
  '271-local-only-cobalt-ic-ease',
  ARRAY['262-care-navigator', '264-provider-institution-referrer', '269-ease-clinic-assessment-types'],
  NULL
);

-- EASE is a separate integrated-care tenant.  Keeping it separate from
-- COBALT_IC_SELF_REFERRAL prevents TEAM and EASE orders, assessments, Epic configuration,
-- and crisis handling from being conflated.
INSERT INTO institution (institution_id, name)
SELECT 'COBALT_IC_EASE', 'EASE Clinic'
WHERE NOT EXISTS (
  SELECT 1 FROM institution WHERE institution_id='COBALT_IC_EASE'
);

-- Reuse the local TEAM fixture's Epic/MyChart credentials and patient-ID
-- contracts without copying TEAM's clinical flows or scheduling records.
UPDATE institution ease
SET epic_client_id=team.epic_client_id,
    epic_user_id=team.epic_user_id,
    epic_user_id_type=team.epic_user_id_type,
    epic_username=team.epic_username,
    epic_password=team.epic_password,
    epic_base_url=team.epic_base_url,
    epic_token_url=team.epic_token_url,
    epic_authorize_url=team.epic_authorize_url,
    epic_backend_service_auth_type_id=team.epic_backend_service_auth_type_id,
    epic_fhir_enabled=team.epic_fhir_enabled,
    epic_patient_mrn_type_name=team.epic_patient_mrn_type_name,
    epic_patient_unique_id_type=team.epic_patient_unique_id_type,
    epic_patient_unique_id_system=team.epic_patient_unique_id_system,
    epic_patient_mrn_system=team.epic_patient_mrn_system,
    epic_patient_mrn_type_alternate_name=team.epic_patient_mrn_type_alternate_name,
    epic_patient_encounter_csn_system=team.epic_patient_encounter_csn_system,
    epic_provider_slot_booking_sync_enabled=team.epic_provider_slot_booking_sync_enabled,
    epic_provider_slot_booking_sync_contact_id_type=team.epic_provider_slot_booking_sync_contact_id_type,
    epic_provider_slot_booking_sync_department_id_type=team.epic_provider_slot_booking_sync_department_id_type,
    epic_provider_slot_booking_sync_visit_type_id_type=team.epic_provider_slot_booking_sync_visit_type_id_type,
    mychart_client_id=team.mychart_client_id,
    mychart_scope=team.mychart_scope,
    mychart_response_type=team.mychart_response_type,
    mychart_callback_url=team.mychart_callback_url,
    mychart_default_url=team.mychart_default_url,
    mychart_aud=team.mychart_aud
FROM institution team
WHERE ease.institution_id='COBALT_IC_EASE'
AND team.institution_id='COBALT_IC_SELF_REFERRAL';

UPDATE institution
SET name='EASE Clinic',
    integrated_care_enabled=TRUE,
    integrated_care_availability_description='(TBD: hours for EASE Clinic go here)',
    integrated_care_phone_number='+12155551212',
    clinical_support_phone_number='+12155551212',
    integrated_care_outreach_followup_day_offset=4,
    integrated_care_program_name='EASE Clinic',
    integrated_care_primary_care_name='Hall Mercer Psychiatry',
    integrated_care_call_center_name='EASE Clinic',
    integrated_care_patient_demographics_required=TRUE,
    integrated_care_patient_care_preference_visible=FALSE,
    integrated_care_patient_intro_override='Thank you for your interest in EASE Clinic. Follow the steps below to connect your MyChart account, tell us about your insurance, complete a brief assessment, and self-schedule a consultation.',
    integrated_care_mhp_triage_overview_override='Thank you for completing the assessment. <strong>You may continue to self-schedule an EASE Clinic consultation.</strong>',
    integrated_care_booking_insurance_requirements='<p>Staff will confirm insurance prior to your appointment, and you may be contacted if any issues are identified. The Department of Psychiatry and/or Cobalt are not responsible for fees that may arise from incomplete or inaccurate insurance information.</p>',
    integrated_care_clinical_report_disclaimer='EASE Clinic assessment responses are intended to support clinical evaluation and do not replace an evaluation by a qualified clinician.',
    landing_page_tagline_override='Connect MyChart to complete your intake and self-schedule',
    email_signup_enabled=FALSE,
    anonymous_enabled=FALSE,
    metadata='{"defaultCrisisInteractionId": null, "way2HealthIncidentTrackingConfigs": []}'::JSONB,
    booking_v2_enabled=FALSE
WHERE institution_id='COBALT_IC_EASE';

INSERT INTO institution_patient_order_referral_source (
  institution_id,
  patient_order_referral_source_id
) VALUES ('COBALT_IC_EASE', 'SELF')
ON CONFLICT DO NOTHING;

INSERT INTO institution_feature (
  institution_feature_id,
  institution_id,
  feature_id,
  nav_description,
  description,
  display_order,
  nav_visible,
  landing_page_visible,
  name_override
) VALUES (
  'c6509c49-c7f1-4caa-a31d-66ee2be34489',
  'COBALT_IC_EASE',
  'MHP',
  'Schedule an EASE Clinic Consultation',
  'Select an available time for a 60-minute telehealth EASE Clinic consultation.',
  1,
  TRUE,
  TRUE,
  'EASE Clinic'
)
ON CONFLICT (institution_id, feature_id) DO UPDATE
SET nav_description=EXCLUDED.nav_description,
    description=EXCLUDED.description,
    display_order=EXCLUDED.display_order,
    nav_visible=EXCLUDED.nav_visible,
    landing_page_visible=EXCLUDED.landing_page_visible,
    name_override=EXCLUDED.name_override;

INSERT INTO appointment_reason (
  appointment_reason_id,
  appointment_reason_type_id,
  institution_id,
  description,
  color,
  display_order
) VALUES (
  '72df2273-1212-4830-9fef-3a1ef7082e44',
  'NOT_SPECIFIED',
  'COBALT_IC_EASE',
  'Not Specified',
  '#53123A',
  1
);

CREATE SEQUENCE IF NOT EXISTS po_reference_number_seq_COBALT_IC_EASE START 1;

-- Create the local referral confirmation flow with fixture-specific IDs.
DO $$
DECLARE
  v_created_by_account_id UUID;
  v_screening_id UUID := 'c5e19848-190c-4c2f-890b-a90f447bd7f8';
  v_screening_version_id UUID := '713a6391-f9e7-49b3-b004-faef73b51278';
  v_question_id UUID := '5e20e6b2-8737-4a37-9e76-b459d82510bb';
  v_flow_id CONSTANT UUID := 'ead3af5f-fd00-412a-8cf2-b5c4de556545';
  v_flow_version_id UUID := '02e5170d-1104-4248-a774-e7c5f4ec56d5';
BEGIN
  SELECT account_id INTO STRICT v_created_by_account_id
  FROM account
  WHERE email_address='admin@cobaltinnovations.org'
  AND institution_id='COBALT';

  IF NOT EXISTS (SELECT 1 FROM screening_flow WHERE screening_flow_id=v_flow_id) THEN
    INSERT INTO screening (
      screening_id,
      name,
      created_by_account_id
    ) VALUES (
      v_screening_id,
      'EASE Clinic Referral Screening',
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
      'console.log("screening scoring");'
    );

    UPDATE screening
    SET active_screening_version_id=v_screening_version_id
    WHERE screening_id=v_screening_id;

    INSERT INTO screening_institution (screening_id, institution_id)
    VALUES (v_screening_id, 'COBALT');

    INSERT INTO screening_question (
      screening_question_id,
      screening_version_id,
      screening_answer_format_id,
      question_text,
      minimum_answer_count,
      maximum_answer_count,
      display_order
    ) VALUES (
      v_question_id,
      v_screening_version_id,
      'SINGLE_SELECT',
      'This self-scheduling pilot requires linking to your MyChart account. Next, you will be taken to the EASE Clinic booking website to link to MyChart and perform a self-assessment in order to connect you to care. Is that OK?',
      1,
      1,
      1
    );

    INSERT INTO screening_answer_option (
      screening_question_id,
      answer_option_text,
      score,
      display_order
    ) VALUES
      (v_question_id, 'Yes, Continue', 1, 1),
      (v_question_id, 'No, Cancel', 0, 2);

    INSERT INTO screening_flow (
      screening_flow_id,
      institution_id,
      screening_flow_type_id,
      created_by_account_id,
      name
    ) VALUES (
      v_flow_id,
      'COBALT',
      'CUSTOM',
      v_created_by_account_id,
      'EASE Clinic Referral Screening Flow'
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
      skippable,
      minutes_until_retake,
      recommendation_expiration_minutes,
      screening_flow_skip_type_id
    ) VALUES (
      v_flow_version_id,
      v_flow_id,
      v_screening_id,
      FALSE,
      1,
      'console.log("flow orchestration");',
      'console.log("flow results");',
      'console.log("flow destination");',
      v_created_by_account_id,
      FALSE,
      1440,
      8760,
      'SKIP'
    );

    UPDATE screening_flow
    SET active_screening_flow_version_id=v_flow_version_id
    WHERE screening_flow_id=v_flow_id;
  END IF;
END $$;

UPDATE screening
SET name='EASE Clinic Referral Screening'
WHERE screening_id=(
  SELECT screening_flow_version.initial_screening_id
  FROM screening_flow
  JOIN screening_flow_version
    ON screening_flow_version.screening_flow_version_id=screening_flow.active_screening_flow_version_id
  WHERE screening_flow.screening_flow_id='ead3af5f-fd00-412a-8cf2-b5c4de556545'
);

UPDATE screening_flow
SET name='EASE Clinic Referral Screening Flow'
WHERE screening_flow_id='ead3af5f-fd00-412a-8cf2-b5c4de556545';

UPDATE screening_version
SET scoring_function=$jscode$
const answeredQuestionIds = [];

input.screeningAnswers.forEach(function (screeningAnswer) {
  if (!answeredQuestionIds.includes(screeningAnswer.screeningSessionAnsweredScreeningQuestionId)) {
    answeredQuestionIds.push(screeningAnswer.screeningSessionAnsweredScreeningQuestionId);
  }
});

output.completed = answeredQuestionIds.length === input.screeningQuestionsWithAnswerOptions.length;
output.score = { overallScore: 0 };

input.screeningAnswers.forEach(function (screeningAnswer) {
  output.score.overallScore += input.screeningAnswerOptionsByScreeningAnswerId[screeningAnswer.screeningAnswerId].score;
});
$jscode$
WHERE screening_version_id=(
  SELECT screening.active_screening_version_id
  FROM screening_flow
  JOIN screening_flow_version
    ON screening_flow_version.screening_flow_version_id=screening_flow.active_screening_flow_version_id
  JOIN screening
    ON screening.screening_id=screening_flow_version.initial_screening_id
  WHERE screening_flow.screening_flow_id='ead3af5f-fd00-412a-8cf2-b5c4de556545'
);

UPDATE screening_question
SET question_text='This self-scheduling pilot requires linking to your MyChart account. Next, you will be taken to the EASE Clinic booking website to link to MyChart and perform a self-assessment in order to connect you to care. Is that OK?'
WHERE screening_version_id=(
  SELECT screening.active_screening_version_id
  FROM screening_flow
  JOIN screening_flow_version
    ON screening_flow_version.screening_flow_version_id=screening_flow.active_screening_flow_version_id
  JOIN screening
    ON screening.screening_id=screening_flow_version.initial_screening_id
  WHERE screening_flow.screening_flow_id='ead3af5f-fd00-412a-8cf2-b5c4de556545'
)
AND display_order=1;

UPDATE screening_flow_version
SET orchestration_function=$jscode$
output.crisisIndicated = false;
output.completed = false;
output.nextScreeningId = null;

const referralScreening = input.screeningSessionScreenings[0];
if (input.screeningSessionScreenings.length !== 1) {
  throw "There is an unexpected number of screening session screenings";
}

if (referralScreening.completed) {
  output.completed = true;
}
$jscode$,
    results_function=$jscode$
output.supportRoleRecommendations = [];
output.recommendLegacyContentAnswerIds = true;
output.legacyContentAnswerIds = [];
$jscode$
WHERE screening_flow_id='ead3af5f-fd00-412a-8cf2-b5c4de556545'
AND screening_flow_version_id=(
  SELECT active_screening_flow_version_id
  FROM screening_flow
  WHERE screening_flow_id='ead3af5f-fd00-412a-8cf2-b5c4de556545'
);

-- Create or retarget the local /referrals/ease-clinic page to the distinct
-- EASE institution.
UPDATE institution_referrer
SET to_institution_id='COBALT_IC_EASE',
    institution_feature_id=NULL,
    intake_screening_flow_id='ead3af5f-fd00-412a-8cf2-b5c4de556545',
    title='EASE Clinic Pilot for Employees',
    description='We''re excited to partner with Hall Mercer Psychiatry to allow UPHS employees to self-schedule an expedited EASE Clinic consultation.',
    page_content=$ease_page_content$
      <h4 class="mb-2">What is EASE Clinic?</h4>
      <p class="mb-8">EASE (Rapid Mental Health Support for UPHS Employees) provides expedited support when workplace stress, acute anxiety, low mood, burnout, or other crises are actively interfering with your daily functioning, well-being, or ability to perform at work.</p>
      <hr class="mb-8">
      <h4 class="mb-2">Who can self-schedule an EASE Clinic appointment?</h4>
      <ul class="mb-8">
        <li>This self-scheduling pilot is currently intended for UPHS employees.</li>
        <li>You will be asked to connect your MyChart account and complete a brief intake before booking.</li>
        <li>Your insurance information will be collected so staff can confirm coverage before the appointment.</li>
      </ul>
      <hr class="mb-8">
      <h4 class="mb-2">How does EASE Clinic work?</h4>
      <ul class="mb-8">
        <li>Self-scheduled consultations are typically available within 7 days.</li>
        <li>The pilot begins with 60-minute telehealth consultations.</li>
        <li>Consultations are led by a licensed, board-certified psychiatrist.</li>
        <li>EASE offers quick symptom evaluation, immediate psychiatric intervention when needed, brief psychotherapeutic support, and warm hand-off scheduling to longer-term psychiatric or therapeutic care.</li>
        <li>Follow-up planning may be provided until you are bridged to ongoing care, if needed.</li>
        <li>Care is documented in PennChart (EMR).</li>
      </ul>
      <hr class="mb-8">
      <h4 class="mb-2">What is NOT included in EASE Clinic services?</h4>
      <p class="mb-2">This service is typically not appropriate for:</p>
      <ul>
        <li>Administrative paperwork, including FMLA, disability, workers' compensation, or ESA requests</li>
        <li>Legal or forensic evaluations</li>
        <li>Neuropsychological testing (for example, ADHD evaluations)</li>
      </ul>
    $ease_page_content$,
    cta_title='Get started with EASE Clinic',
    cta_description='Connect your MyChart account, complete a brief intake and assessment, and self-schedule an expedited EASE Clinic consultation.'
WHERE from_institution_id='COBALT'
AND url_name='ease-clinic';

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
  cta_description
)
SELECT
  '790937a5-cd5d-475b-a621-45d8037efa93',
  'COBALT',
  'COBALT_IC_EASE',
  NULL,
  'ead3af5f-fd00-412a-8cf2-b5c4de556545',
  'ease-clinic',
  'EASE Clinic Pilot for Employees',
  'We''re excited to partner with Hall Mercer Psychiatry to allow UPHS employees to self-schedule an expedited EASE Clinic consultation.',
  $ease_page_content$
    <h4 class="mb-2">What is EASE Clinic?</h4>
    <p class="mb-8">EASE (Rapid Mental Health Support for UPHS Employees) provides expedited support when workplace stress, acute anxiety, low mood, burnout, or other crises are actively interfering with your daily functioning, well-being, or ability to perform at work.</p>
    <hr class="mb-8">
    <h4 class="mb-2">Who can self-schedule an EASE Clinic appointment?</h4>
    <ul class="mb-8">
      <li>This self-scheduling pilot is currently intended for UPHS employees.</li>
      <li>You will be asked to connect your MyChart account and complete a brief intake before booking.</li>
      <li>Your insurance information will be collected so staff can confirm coverage before the appointment.</li>
    </ul>
    <hr class="mb-8">
    <h4 class="mb-2">How does EASE Clinic work?</h4>
    <ul class="mb-8">
      <li>Self-scheduled consultations are typically available within 7 days.</li>
      <li>The pilot begins with 60-minute telehealth consultations.</li>
      <li>Consultations are led by a licensed, board-certified psychiatrist.</li>
      <li>EASE offers quick symptom evaluation, immediate psychiatric intervention when needed, brief psychotherapeutic support, and warm hand-off scheduling to longer-term psychiatric or therapeutic care.</li>
      <li>Follow-up planning may be provided until you are bridged to ongoing care, if needed.</li>
      <li>Care is documented in PennChart (EMR).</li>
    </ul>
    <hr class="mb-8">
    <h4 class="mb-2">What is NOT included in EASE Clinic services?</h4>
    <p class="mb-2">This service is typically not appropriate for:</p>
    <ul>
      <li>Administrative paperwork, including FMLA, disability, workers' compensation, or ESA requests</li>
      <li>Legal or forensic evaluations</li>
      <li>Neuropsychological testing (for example, ADHD evaluations)</li>
    </ul>
  $ease_page_content$,
  'Get started with EASE Clinic',
  'Connect your MyChart account, complete a brief intake and assessment, and self-schedule an expedited EASE Clinic consultation.'
WHERE NOT EXISTS (
  SELECT 1
  FROM institution_referrer
  WHERE from_institution_id='COBALT'
  AND url_name='ease-clinic'
);

-- EASE is intentionally not represented by a referral-backed provider in the
-- local provider directory.
DELETE FROM provider_institution_referrer
WHERE institution_referrer_id IN (
  SELECT institution_referrer_id
  FROM institution_referrer
  WHERE from_institution_id='COBALT'
  AND url_name='ease-clinic'
);

-- Informational intake.  Every answer has score 0 and all six questions are
-- required, so employment or insurance answers never screen a patient out.
INSERT INTO screening (
  screening_id,
  name,
  created_by_account_id
)
SELECT
  '78ae9db8-e6a5-4df7-a4ca-110b529b60da',
  'EASE Clinic Intake',
  account_id
FROM account
WHERE email_address='admin@cobaltinnovations.org'
AND institution_id='COBALT';

INSERT INTO screening_version (
  screening_version_id,
  screening_id,
  screening_type_id,
  created_by_account_id,
  version_number,
  scoring_function
)
SELECT
  'f9c47b13-419f-45d0-a6cb-03d7a060052f',
  '78ae9db8-e6a5-4df7-a4ca-110b529b60da',
  'IC_INTAKE',
  account_id,
  1,
  $jscode$
output.completed = input.answeredScreeningQuestionCount === input.screeningQuestionsWithAnswerOptions.length;
output.score = { overallScore: 0 };
output.belowScoringThreshold = true;
$jscode$
FROM account
WHERE email_address='admin@cobaltinnovations.org'
AND institution_id='COBALT';

UPDATE screening
SET active_screening_version_id='f9c47b13-419f-45d0-a6cb-03d7a060052f'
WHERE screening_id='78ae9db8-e6a5-4df7-a4ca-110b529b60da';

INSERT INTO screening_institution (screening_id, institution_id)
VALUES ('78ae9db8-e6a5-4df7-a4ca-110b529b60da', 'COBALT_IC_EASE');

INSERT INTO screening_question (
  screening_version_id,
  screening_answer_format_id,
  intro_text,
  question_text,
  minimum_answer_count,
  maximum_answer_count,
  display_order
) VALUES
  ('f9c47b13-419f-45d0-a6cb-03d7a060052f', 'SINGLE_SELECT', 'Employment status', 'This pilot program is currently intended for UPHS employees. Are you a UPHS employee?', 1, 1, 1),
  ('f9c47b13-419f-45d0-a6cb-03d7a060052f', 'SINGLE_SELECT', 'What EASE Clinic provides', 'EASE Clinic provides quick symptom evaluation, immediate psychiatric intervention when needed, brief psychotherapeutic support, warm hand-off scheduling to longer-term care, and follow-up planning until you are bridged to ongoing care, if needed.', 1, 1, 2),
  ('f9c47b13-419f-45d0-a6cb-03d7a060052f', 'SINGLE_SELECT', 'What EASE Clinic does not provide', 'This service is typically not appropriate for administrative paperwork (including FMLA, disability, workers'' compensation, or ESA requests), legal or forensic evaluations, or neuropsychological testing (for example, ADHD evaluations).', 1, 1, 3),
  ('f9c47b13-419f-45d0-a6cb-03d7a060052f', 'SINGLE_SELECT', 'Insurance', 'How do you currently get your health insurance?', 1, 1, 4),
  ('f9c47b13-419f-45d0-a6cb-03d7a060052f', 'SINGLE_SELECT', 'Insurance', 'Select your current behavioral health insurance plan from the list below.', 1, 1, 5),
  ('f9c47b13-419f-45d0-a6cb-03d7a060052f', 'SINGLE_SELECT', 'Insurance confirmation', 'Staff will confirm insurance prior to your appointment, and you may be contacted if any issues are identified.<br/><br/>The Department of Psychiatry and/or Cobalt are not responsible for fees that may arise from incomplete or inaccurate insurance information.', 1, 1, 6);

INSERT INTO screening_answer_option (
  screening_question_id,
  answer_option_text,
  score,
  display_order,
  freeform_supplement,
  freeform_supplement_text
)
SELECT q.screening_question_id, option.answer_text, 0, option.answer_order, option.freeform, option.freeform_text
FROM screening_question q
JOIN (VALUES
  (1, 1, 'Yes', FALSE, NULL),
  (1, 2, 'No', FALSE, NULL),
  (2, 1, 'I understand', FALSE, NULL),
  (3, 1, 'I understand', FALSE, NULL),
  (4, 1, 'Through UPHS', FALSE, NULL),
  (4, 2, 'Through a family member''s employer', FALSE, NULL),
  (4, 3, 'Through the marketplace or privately purchased', FALSE, NULL),
  (4, 4, 'I don''t currently have health insurance', FALSE, NULL),
  (5, 1, 'Aetna', FALSE, NULL),
  (5, 2, 'Independence Personal Choice', FALSE, NULL),
  (5, 3, 'Quest Behavioral Health (listed on the back of your PennCare PPO card)', FALSE, NULL),
  (5, 4, 'Keystone Health Plan East', FALSE, NULL),
  (5, 5, 'Blue Cross Blue Shield', FALSE, NULL),
  (5, 6, 'Horizon', FALSE, NULL),
  (5, 7, 'Tricare', FALSE, NULL),
  (5, 8, 'Medicaid (Philadelphia County)', FALSE, NULL),
  (5, 9, 'Not applicable — I don''t currently have health insurance', FALSE, NULL),
  (5, 10, 'Other', TRUE, 'Enter the name of your insurance plan.'),
  (6, 1, 'I understand', FALSE, NULL)
) AS option(question_order, answer_order, answer_text, freeform, freeform_text)
  ON option.question_order=q.display_order
WHERE q.screening_version_id='f9c47b13-419f-45d0-a6cb-03d7a060052f';

INSERT INTO screening_flow (
  screening_flow_id,
  institution_id,
  screening_flow_type_id,
  created_by_account_id,
  name
)
SELECT
  'f947fb9e-be69-427e-b6d8-8f02f7b66a09',
  'COBALT_IC_EASE',
  'INTEGRATED_CARE_INTAKE',
  account_id,
  'EASE Clinic Intake Flow'
FROM account
WHERE email_address='admin@cobaltinnovations.org'
AND institution_id='COBALT';

INSERT INTO screening_flow_version (
  screening_flow_version_id,
  screening_flow_id,
  initial_screening_id,
  phone_number_required,
  skippable,
  orchestration_function,
  results_function,
  destination_function,
  created_by_account_id
)
SELECT
  'e0398333-d2a0-49e3-87cc-5091c1aed5b8',
  'f947fb9e-be69-427e-b6d8-8f02f7b66a09',
  '78ae9db8-e6a5-4df7-a4ca-110b529b60da',
  FALSE,
  FALSE,
  $jscode$
output.crisisIndicated = false;
output.completed = input.screeningSessionScreenings.length === 1 && input.screeningSessionScreenings[0].completed;
output.nextScreeningId = null;
$jscode$,
  $jscode$
output.supportRoleRecommendations = [];
$jscode$,
  $jscode$
output.screeningSessionDestinationId = null;
output.context = {};

if (input.screeningSession.completed) {
  output.screeningSessionDestinationId = input.selfAdministered ? 'IC_PATIENT_CLINICAL_SCREENING' : 'IC_MHIC_CLINICAL_SCREENING';
  output.context = { patientOrderId: input.additionalContext.patientOrderId };
}
$jscode$,
  account_id
FROM account
WHERE email_address='admin@cobaltinnovations.org'
AND institution_id='COBALT';

UPDATE screening_flow
SET active_screening_flow_version_id='e0398333-d2a0-49e3-87cc-5091c1aed5b8'
WHERE screening_flow_id='f947fb9e-be69-427e-b6d8-8f02f7b66a09';

UPDATE institution
SET integrated_care_intake_screening_flow_id='f947fb9e-be69-427e-b6d8-8f02f7b66a09'
WHERE institution_id='COBALT_IC_EASE';

-- EASE-specific PHQ-2 branching to PHQ-9.  A PHQ-2 raw score below 3
-- completes this screening after two questions; otherwise all nine questions
-- are presented.  A non-zero Q9 marks crisis but does not stop the flow.
INSERT INTO screening (
  screening_id,
  name,
  created_by_account_id
)
SELECT
  '84c18d18-86f5-4031-b6ad-8c8e7b1c9b7d',
  'EASE Clinic PHQ-9',
  account_id
FROM account
WHERE email_address='admin@cobaltinnovations.org'
AND institution_id='COBALT';

INSERT INTO screening_version (
  screening_version_id,
  screening_id,
  screening_type_id,
  created_by_account_id,
  version_number,
  scoring_function
)
SELECT
  'aa86808c-2b07-4a76-aae0-b82d33cfdd07',
  '84c18d18-86f5-4031-b6ad-8c8e7b1c9b7d',
  'PHQ_9',
  account_id,
  1,
  $jscode$
let rawScore = 0;
input.screeningAnswers.forEach(function (screeningAnswer) {
  rawScore += input.screeningAnswerOptionsByScreeningAnswerId[screeningAnswer.screeningAnswerId].score;
});

const answeredCount = input.answeredScreeningQuestionCount;
output.completed = answeredCount === 9 || (answeredCount === 2 && rawScore < 3);
output.score = { overallScore: rawScore };
output.belowScoringThreshold = rawScore < 5;
$jscode$
FROM account
WHERE email_address='admin@cobaltinnovations.org'
AND institution_id='COBALT';

UPDATE screening
SET active_screening_version_id='aa86808c-2b07-4a76-aae0-b82d33cfdd07'
WHERE screening_id='84c18d18-86f5-4031-b6ad-8c8e7b1c9b7d';

INSERT INTO screening_institution (screening_id, institution_id)
VALUES ('84c18d18-86f5-4031-b6ad-8c8e7b1c9b7d', 'COBALT_IC_EASE');

INSERT INTO screening_question (
  screening_version_id,
  screening_answer_format_id,
  intro_text,
  question_text,
  minimum_answer_count,
  maximum_answer_count,
  display_order
) VALUES
  ('aa86808c-2b07-4a76-aae0-b82d33cfdd07', 'SINGLE_SELECT', 'Over the last two weeks, how often have you been bothered by...', 'Little interest or pleasure in doing things', 1, 1, 1),
  ('aa86808c-2b07-4a76-aae0-b82d33cfdd07', 'SINGLE_SELECT', 'Over the last two weeks, how often have you been bothered by...', 'Feeling down, depressed, or hopeless', 1, 1, 2),
  ('aa86808c-2b07-4a76-aae0-b82d33cfdd07', 'SINGLE_SELECT', 'Over the last two weeks, how often have you been bothered by...', 'Trouble falling or staying asleep, or sleeping too much', 1, 1, 3),
  ('aa86808c-2b07-4a76-aae0-b82d33cfdd07', 'SINGLE_SELECT', 'Over the last two weeks, how often have you been bothered by...', 'Feeling tired or having little energy', 1, 1, 4),
  ('aa86808c-2b07-4a76-aae0-b82d33cfdd07', 'SINGLE_SELECT', 'Over the last two weeks, how often have you been bothered by...', 'Poor appetite or overeating', 1, 1, 5),
  ('aa86808c-2b07-4a76-aae0-b82d33cfdd07', 'SINGLE_SELECT', 'Over the last two weeks, how often have you been bothered by...', 'Feeling bad about yourself - or that you are a failure or have let yourself or your family down', 1, 1, 6),
  ('aa86808c-2b07-4a76-aae0-b82d33cfdd07', 'SINGLE_SELECT', 'Over the last two weeks, how often have you been bothered by...', 'Trouble concentrating on things, such as reading the newspaper or watching television', 1, 1, 7),
  ('aa86808c-2b07-4a76-aae0-b82d33cfdd07', 'SINGLE_SELECT', 'Over the last two weeks, how often have you been bothered by...', 'Moving or speaking so slowly that other people could have noticed? Or the opposite - being so fidgety or restless that you have been moving around a lot more than usual', 1, 1, 8),
  ('aa86808c-2b07-4a76-aae0-b82d33cfdd07', 'SINGLE_SELECT', 'Over the last two weeks, how often have you been bothered by...', 'Thoughts that you would be better off dead, or thoughts of hurting yourself in some way', 1, 1, 9);

INSERT INTO screening_answer_option (
  screening_question_id,
  answer_option_text,
  score,
  indicates_crisis,
  display_order
)
SELECT
  question.screening_question_id,
  answer.answer_text,
  answer.score,
  question.display_order=9 AND answer.score>0,
  answer.answer_order
FROM screening_question question
CROSS JOIN (VALUES
  (1, 'Not at all', 0),
  (2, 'Several days', 1),
  (3, 'More than half the days', 2),
  (4, 'Nearly every day', 3)
) AS answer(answer_order, answer_text, score)
WHERE question.screening_version_id='aa86808c-2b07-4a76-aae0-b82d33cfdd07';

-- PROMIS Satisfaction with Participation in Social Roles 8a v1.0.
INSERT INTO screening (
  screening_id,
  name,
  created_by_account_id
)
SELECT
  '05458418-619f-423c-bd38-49a269d8f398',
  'EASE PROMIS Social Roles 8a v1.0',
  account_id
FROM account
WHERE email_address='admin@cobaltinnovations.org'
AND institution_id='COBALT';

INSERT INTO screening_version (
  screening_version_id,
  screening_id,
  screening_type_id,
  created_by_account_id,
  version_number,
  scoring_function
)
SELECT
  '7c866e7c-3dda-4318-adf2-fa31400c6b62',
  '05458418-619f-423c-bd38-49a269d8f398',
  'PROMIS_PARTICIPATION_SOCIAL_ROLES_8A_V1',
  account_id,
  1,
  $jscode$
let rawScore = 0;
input.screeningAnswers.forEach(function (screeningAnswer) {
  rawScore += input.screeningAnswerOptionsByScreeningAnswerId[screeningAnswer.screeningAnswerId].score;
});

const conversionByRawScore = {
  8: [26.9, 4.1], 9: [30.8, 2.5], 10: [32.5, 2.1], 11: [33.8, 1.9],
  12: [34.9, 1.8], 13: [35.8, 1.7], 14: [36.7, 1.7], 15: [37.5, 1.6],
  16: [38.3, 1.6], 17: [39.1, 1.6], 18: [39.9, 1.6], 19: [40.6, 1.6],
  20: [41.4, 1.6], 21: [42.2, 1.7], 22: [43.0, 1.7], 23: [43.9, 1.7],
  24: [44.7, 1.7], 25: [45.5, 1.7], 26: [46.4, 1.7], 27: [47.3, 1.7],
  28: [48.2, 1.7], 29: [49.1, 1.7], 30: [50.0, 1.7], 31: [51.0, 1.7],
  32: [52.0, 1.7], 33: [53.0, 1.7], 34: [54.0, 1.7], 35: [55.1, 1.7],
  36: [56.2, 1.8], 37: [57.4, 1.9], 38: [58.9, 2.2], 39: [61.0, 2.7],
  40: [66.1, 4.9]
};
const conversion = conversionByRawScore[rawScore] || null;

output.completed = input.answeredScreeningQuestionCount === 8;
output.score = {
  overallScore: rawScore,
  rawScore: rawScore,
  tScore: conversion ? conversion[0] : null,
  standardError: conversion ? conversion[1] : null
};
output.belowScoringThreshold = false;
$jscode$
FROM account
WHERE email_address='admin@cobaltinnovations.org'
AND institution_id='COBALT';

UPDATE screening
SET active_screening_version_id='7c866e7c-3dda-4318-adf2-fa31400c6b62'
WHERE screening_id='05458418-619f-423c-bd38-49a269d8f398';

INSERT INTO screening_institution (screening_id, institution_id)
VALUES ('05458418-619f-423c-bd38-49a269d8f398', 'COBALT_IC_EASE');

INSERT INTO screening_question (
  screening_version_id,
  screening_answer_format_id,
  intro_text,
  question_text,
  minimum_answer_count,
  maximum_answer_count,
  display_order
) VALUES
  ('7c866e7c-3dda-4318-adf2-fa31400c6b62', 'SINGLE_SELECT', 'In the past 7 days...', 'I am satisfied with how much work I can do (include work at home)', 1, 1, 1),
  ('7c866e7c-3dda-4318-adf2-fa31400c6b62', 'SINGLE_SELECT', 'In the past 7 days...', 'I am satisfied with my ability to work (include work at home)', 1, 1, 2),
  ('7c866e7c-3dda-4318-adf2-fa31400c6b62', 'SINGLE_SELECT', 'In the past 7 days...', 'I am satisfied with my ability to do regular personal and household responsibilities', 1, 1, 3),
  ('7c866e7c-3dda-4318-adf2-fa31400c6b62', 'SINGLE_SELECT', NULL, 'I am satisfied with my ability to perform my daily routines', 1, 1, 4),
  ('7c866e7c-3dda-4318-adf2-fa31400c6b62', 'SINGLE_SELECT', 'In the past 7 days...', 'I am satisfied with my ability to meet the needs of those who depend on me', 1, 1, 5),
  ('7c866e7c-3dda-4318-adf2-fa31400c6b62', 'SINGLE_SELECT', 'In the past 7 days...', 'I am satisfied with my ability to do household chores/tasks', 1, 1, 6),
  ('7c866e7c-3dda-4318-adf2-fa31400c6b62', 'SINGLE_SELECT', NULL, 'I am satisfied with my ability to do things for my family', 1, 1, 7),
  ('7c866e7c-3dda-4318-adf2-fa31400c6b62', 'SINGLE_SELECT', 'In the past 7 days...', 'I am satisfied with the amount of time I spend performing my daily routines', 1, 1, 8);

INSERT INTO screening_answer_option (
  screening_question_id,
  answer_option_text,
  score,
  display_order
)
SELECT
  question.screening_question_id,
  answer.answer_text,
  answer.score,
  answer.score
FROM screening_question question
CROSS JOIN (VALUES
  (1, 'Not at all satisfied'),
  (2, 'A little bit satisfied'),
  (3, 'Somewhat satisfied'),
  (4, 'Quite satisfied'),
  (5, 'Very satisfied')
) AS answer(score, answer_text)
WHERE question.screening_version_id='7c866e7c-3dda-4318-adf2-fa31400c6b62';

INSERT INTO screening_flow (
  screening_flow_id,
  institution_id,
  screening_flow_type_id,
  created_by_account_id,
  name
)
SELECT
  '52af99c9-ffac-4320-8170-3162bb0a6e35',
  'COBALT_IC_EASE',
  'INTEGRATED_CARE',
  account_id,
  'EASE Clinic Clinical Assessment Flow'
FROM account
WHERE email_address='admin@cobaltinnovations.org'
AND institution_id='COBALT';

INSERT INTO screening_flow_version (
  screening_flow_version_id,
  screening_flow_id,
  initial_screening_id,
  phone_number_required,
  skippable,
  orchestration_function,
  results_function,
  destination_function,
  created_by_account_id
)
SELECT
  '1d7f6dcb-35cc-4f98-90f3-a60b4698a766',
  '52af99c9-ffac-4320-8170-3162bb0a6e35',
  '84c18d18-86f5-4031-b6ad-8c8e7b1c9b7d',
  FALSE,
  FALSE,
  $jscode$
output.crisisIndicated = false;
output.completed = false;
output.nextScreeningId = null;
output.hardStop = false;

const phq9 = input.screeningSessionScreenings[0];
const promis = input.screeningSessionScreenings.length > 1 ? input.screeningSessionScreenings[1] : null;
const phq9Questions = input.screeningResultsByScreeningSessionScreeningId[phq9.screeningSessionScreeningId] || [];
const phq9Question9 = phq9Questions.length > 8 ? phq9Questions[8] : null;
const phq9Question9Response = phq9Question9 && phq9Question9.screeningResponses.length > 0
  ? phq9Question9.screeningResponses[0]
  : null;

if (phq9Question9Response && phq9Question9Response.screeningAnswerOption.score > 0) {
  output.crisisIndicated = true;
}

if (!promis && phq9.completed) {
  output.nextScreeningId = input.screeningsByName['EASE PROMIS Social Roles 8a v1.0'].screeningId;
} else if (promis && promis.completed) {
  output.completed = true;
}
$jscode$,
  $jscode$
output.supportRoleRecommendations = [{ supportRoleId: 'MHP', weight: 1 }];
output.recommendedTagIds = [];
output.integratedCareTriages = [{
  patientOrderFocusTypeId: 'EVALUATION',
  patientOrderCareTypeId: 'SPECIALTY',
  reason: 'EASE Clinic consultation requested; assessment results do not screen the patient out.'
}];
output.integratedCareTriagedCareTypeId = 'SPECIALTY';
$jscode$,
  $jscode$
output.screeningSessionDestinationId = null;
output.context = {};

if (input.screeningSession.completed) {
  output.screeningSessionDestinationId = input.selfAdministered ? 'IC_PATIENT_SCREENING_SESSION_RESULTS' : 'IC_MHIC_SCREENING_SESSION_RESULTS';
  output.context = {
    patientOrderId: input.additionalContext.patientOrderId,
    screeningSessionId: input.screeningSession.screeningSessionId
  };
}
$jscode$,
  account_id
FROM account
WHERE email_address='admin@cobaltinnovations.org'
AND institution_id='COBALT';

UPDATE screening_flow
SET active_screening_flow_version_id='1d7f6dcb-35cc-4f98-90f3-a60b4698a766'
WHERE screening_flow_id='52af99c9-ffac-4320-8170-3162bb0a6e35';

UPDATE institution
SET integrated_care_screening_flow_id='52af99c9-ffac-4320-8170-3162bb0a6e35'
WHERE institution_id='COBALT_IC_EASE';

-- Epic scheduling: telehealth-only with Dr. Olga Barg.
INSERT INTO epic_department (
  epic_department_id,
  institution_id,
  department_id,
  department_id_type,
  name
) VALUES (
  '1f0fd2c8-bfb9-4191-af02-e250e99e8c1a',
  'COBALT_IC_EASE',
  '1366',
  'EXTERNAL',
  'Hall Mercer Psychiatry EASE Clinic'
);

INSERT INTO appointment_type (
  appointment_type_id,
  name,
  description,
  duration_in_minutes,
  scheduling_system_id,
  epic_visit_type_id,
  epic_visit_type_id_type,
  visit_type_id,
  hex_color
) VALUES (
  'b5a6bd83-6a12-45f2-9289-3bd754c95987',
  'EASE Clinic Consultation',
  'New Patient Telehealth Consultation',
  60,
  'EPIC',
  '3137',
  'EXTERNAL',
  'INITIAL',
  '3494369'
);

INSERT INTO provider (
  provider_id,
  institution_id,
  name,
  email_address,
  scheduling_system_id,
  epic_provider_id,
  epic_provider_id_type,
  videoconference_platform_id,
  epic_appointment_filter_id,
  system_affinity_id,
  scheduling_lead_time_in_hours,
  url_name,
  virtual_appointments_only
) VALUES (
  '72d08bd0-4f35-4faa-acb8-b1ed01ed6993',
  'COBALT_IC_EASE',
  'Dr. Olga Barg',
  NULL,
  'EPIC',
  '17000617',
  'EXTERNAL',
  'EXTERNAL',
  'VISIT_TYPE',
  'COBALT',
  16,
  'olga-barg-ease-clinic',
  TRUE
);

INSERT INTO provider_support_role (provider_id, support_role_id)
VALUES ('72d08bd0-4f35-4faa-acb8-b1ed01ed6993', 'MHP');

INSERT INTO provider_appointment_type (
  provider_id,
  appointment_type_id,
  display_order
) VALUES (
  '72d08bd0-4f35-4faa-acb8-b1ed01ed6993',
  'b5a6bd83-6a12-45f2-9289-3bd754c95987',
  1
);

INSERT INTO provider_epic_department (
  provider_id,
  epic_department_id,
  display_order
) VALUES (
  '72d08bd0-4f35-4faa-acb8-b1ed01ed6993',
  '1f0fd2c8-bfb9-4191-af02-e250e99e8c1a',
  1
);

-- Writable question rows only.  Epic-calculated raw/scale/total rows are
-- intentionally omitted.
INSERT INTO flowsheet (
  flowsheet_type_id,
  institution_id,
  epic_flowsheet_id,
  epic_flowsheet_id_type,
  epic_flowsheet_template_id,
  epic_flowsheet_template_id_type,
  permitted_values
) VALUES
  ('PHQ9_QUESTION_1', 'COBALT_IC_EASE', '3375', 'INTERNAL', '310', 'INTERNAL', '[0, 1, 2, 3]'::JSONB),
  ('PHQ9_QUESTION_2', 'COBALT_IC_EASE', '3376', 'INTERNAL', '310', 'INTERNAL', '[0, 1, 2, 3]'::JSONB),
  ('PHQ9_QUESTION_3', 'COBALT_IC_EASE', '3377', 'INTERNAL', '310', 'INTERNAL', '["0 - Not at all", "1 - Several days", "2 - More than half the days", "3 - Nearly every day"]'::JSONB),
  ('PHQ9_QUESTION_4', 'COBALT_IC_EASE', '5777', 'INTERNAL', '310', 'INTERNAL', '["0 - Not at all", "1 - Several days", "2 - More than half the days", "3 - Nearly every day"]'::JSONB),
  ('PHQ9_QUESTION_5', 'COBALT_IC_EASE', '3379', 'INTERNAL', '310', 'INTERNAL', '["0 - Not at all", "1 - Several days", "2 - More than half the days", "3 - Nearly every day"]'::JSONB),
  ('PHQ9_QUESTION_6', 'COBALT_IC_EASE', '3380', 'INTERNAL', '310', 'INTERNAL', '["0 - Not at all", "1 - Several days", "2 - More than half the days", "3 - Nearly every day"]'::JSONB),
  ('PHQ9_QUESTION_7', 'COBALT_IC_EASE', '3381', 'INTERNAL', '310', 'INTERNAL', '["0 - Not at all", "1 - Several days", "2 - More than half the days", "3 - Nearly every day"]'::JSONB),
  ('PHQ9_QUESTION_8', 'COBALT_IC_EASE', '3382', 'INTERNAL', '310', 'INTERNAL', '["0 - Not at all", "1 - Several days", "2 - More than half the days", "3 - Nearly every day"]'::JSONB),
  ('PHQ9_QUESTION_9', 'COBALT_IC_EASE', '3383', 'INTERNAL', '310', 'INTERNAL', '["0 - Not at all", "1 - Several days", "2 - More than half the days", "3 - Nearly every day"]'::JSONB),
  ('PROMIS_SOCIAL_ROLES_8A_V1_QUESTION_1', 'COBALT_IC_EASE', '1021500376', 'INTERNAL', '375', 'INTERNAL', '[1, 2, 3, 4, 5]'::JSONB),
  ('PROMIS_SOCIAL_ROLES_8A_V1_QUESTION_2', 'COBALT_IC_EASE', '1021500377', 'INTERNAL', '375', 'INTERNAL', '[1, 2, 3, 4, 5]'::JSONB),
  ('PROMIS_SOCIAL_ROLES_8A_V1_QUESTION_3', 'COBALT_IC_EASE', '1021500378', 'INTERNAL', '375', 'INTERNAL', '[1, 2, 3, 4, 5]'::JSONB),
  ('PROMIS_SOCIAL_ROLES_8A_V1_QUESTION_4', 'COBALT_IC_EASE', '1021500379', 'INTERNAL', '375', 'INTERNAL', '[1, 2, 3, 4, 5]'::JSONB),
  ('PROMIS_SOCIAL_ROLES_8A_V1_QUESTION_5', 'COBALT_IC_EASE', '1021500380', 'INTERNAL', '375', 'INTERNAL', '[1, 2, 3, 4, 5]'::JSONB),
  ('PROMIS_SOCIAL_ROLES_8A_V1_QUESTION_6', 'COBALT_IC_EASE', '1021500381', 'INTERNAL', '375', 'INTERNAL', '[1, 2, 3, 4, 5]'::JSONB),
  ('PROMIS_SOCIAL_ROLES_8A_V1_QUESTION_7', 'COBALT_IC_EASE', '1021500382', 'INTERNAL', '375', 'INTERNAL', '[1, 2, 3, 4, 5]'::JSONB),
  ('PROMIS_SOCIAL_ROLES_8A_V1_QUESTION_8', 'COBALT_IC_EASE', '1021500383', 'INTERNAL', '375', 'INTERNAL', '[1, 2, 3, 4, 5]'::JSONB);


-- Local mirrors expose MyChart plus email/password so the EASE plugin can
-- exercise patient test-account creation and staff fixture sign-in.
UPDATE institution
SET email_signup_enabled=TRUE
WHERE institution_id='COBALT_IC_EASE';

INSERT INTO institution_account_source (
  institution_account_source_id,
  institution_id,
  account_source_id,
  account_source_display_style_id,
  authentication_description,
  display_order,
  requires_user_experience_type_id,
  visible
) VALUES
  (uuid_generate_v4(), 'COBALT_IC_EASE', 'MYCHART', 'PRIMARY', 'Sign In With MyChart', 1, 'PATIENT', TRUE),
  (uuid_generate_v4(), 'COBALT_IC_EASE', 'EMAIL_PASSWORD', 'SECONDARY', 'Sign In With Email', 2, NULL, TRUE)
ON CONFLICT (institution_id, account_source_id) DO UPDATE
SET account_source_display_style_id=EXCLUDED.account_source_display_style_id,
    authentication_description=EXCLUDED.authentication_description,
    display_order=EXCLUDED.display_order,
    requires_user_experience_type_id=EXCLUDED.requires_user_experience_type_id,
    visible=EXCLUDED.visible;

INSERT INTO institution_url (
  institution_id,
  url,
  hostname,
  preferred,
  user_experience_type_id
) VALUES
  ('COBALT_IC_EASE', 'http://ease-staff.cobalt.local:3000', 'ease-staff.cobalt.local', TRUE, 'STAFF'),
  ('COBALT_IC_EASE', 'http://ease.cobalt.local:3000', 'ease.cobalt.local', TRUE, 'PATIENT');

UPDATE screening_flow_version
SET destination_function=$jscode$
const referralScreening = input.screeningSessionScreenings[0];

output.screeningSessionDestinationId = null;
output.context = {};

if (input.screeningSession.completed) {
  if (referralScreening.scoreAsObject.overallScore === 1) {
    output.screeningSessionDestinationId = 'INSTITUTION_REFERRAL';
    output.context.institutionReferralUrl = 'http://ease.cobalt.local:3000/sign-in?a.c=ir-ease-clinic-' + input.accountId;
  } else {
    output.screeningSessionDestinationId = 'INSTITUTION_REFERRER_DETAIL';
    output.context.institutionReferrerUrlName = 'ease-clinic';
  }
}
$jscode$
WHERE screening_flow_id='ead3af5f-fd00-412a-8cf2-b5c4de556545'
AND screening_flow_version_id=(
  SELECT active_screening_flow_version_id
  FROM screening_flow
  WHERE screening_flow_id='ead3af5f-fd00-412a-8cf2-b5c4de556545'
);

INSERT INTO crisis_contact (
  crisis_contact_id,
  institution_id,
  email_address,
  active,
  locale,
  time_zone
) VALUES
  ('dea14a56-c678-44f5-8777-85096c6927ca', 'COBALT_IC_EASE', 'maa@xmog.com', TRUE, 'en-US', 'America/New_York'),
  ('184c5e3d-567e-445d-861e-1c01b5df8454', 'COBALT_IC_EASE', 'mark@xmog.com', TRUE, 'en-US', 'America/New_York');

-- TEAM-style local staff fixture.  The password is "test1234".
INSERT INTO account (
  account_id,
  role_id,
  institution_id,
  account_source_id,
  first_name,
  last_name,
  display_name,
  email_address,
  password
) VALUES (
  'd21fc1ef-4589-44f8-b6cd-6501901985d7',
  'MHIC',
  'COBALT_IC_EASE',
  'EMAIL_PASSWORD',
  'Mark',
  'Allen',
  'Mark Allen',
  'maa+mhic@xmog.com',
  '$2a$10$M2tPoJ8eQr55OW4iOfpbBOpgqFWt0LxnvVBnW1a/1LhKNA6SuUN42'
);

INSERT INTO account_capability (account_id, account_capability_type_id) VALUES
  ('d21fc1ef-4589-44f8-b6cd-6501901985d7', 'MHIC_REPORT_VIEWER'),
  ('d21fc1ef-4589-44f8-b6cd-6501901985d7', 'MHIC_DEPARTMENT_ADMIN'),
  ('d21fc1ef-4589-44f8-b6cd-6501901985d7', 'MHIC_ORDER_SERVICER');

COMMIT;
