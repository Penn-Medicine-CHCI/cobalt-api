BEGIN;
SELECT _v.register_patch(
  '276-local-only-cobalt-ic-ease-finalization',
  ARRAY['273-local-only-cobalt-ic-ease-triage', '272-screening-destination-content'],
  NULL
);

-- Keep the previously published versions for sessions already in progress.
-- New sessions use the PAH intake and the full nine-question PHQ-9.
DO $ease$
DECLARE
  v_intake_screening_id CONSTANT UUID := '78ae9db8-e6a5-4df7-a4ca-110b529b60da';
  v_phq9_screening_id CONSTANT UUID := '84c18d18-86f5-4031-b6ad-8c8e7b1c9b7d';
  v_intake_flow_id CONSTANT UUID := 'f947fb9e-be69-427e-b6d8-8f02f7b66a09';
  v_clinical_flow_id CONSTANT UUID := '52af99c9-ffac-4320-8170-3162bb0a6e35';
  v_intake_version_id CONSTANT UUID := '2f6e5e66-c320-4efa-9481-f486f0f606ba';
  v_phq9_version_id CONSTANT UUID := '40d47bf6-d598-47d7-9100-09e4c11c97d5';
  v_intake_flow_version_id CONSTANT UUID := '4f3427d7-61e2-48b2-aeb0-343155380aa2';
  v_clinical_flow_version_id CONSTANT UUID := '62270fa0-b67d-4178-a8a6-b75bd6119309';
  v_old_intake_version_id UUID;
  v_old_phq9_version_id UUID;
  v_old_intake_flow_version_id UUID;
  v_old_clinical_flow_version_id UUID;
  v_taylor_provider_id CONSTANT UUID := 'c2f2264f-4a35-48a2-9eca-f55befdf1362';
  v_taylor_appointment_type_id CONSTANT UUID := '1520eca8-7e38-4f46-a840-410b3ac88f87';
  v_ease_department_id CONSTANT UUID := '1f0fd2c8-bfb9-4191-af02-e250e99e8c1a';
BEGIN
  IF NOT EXISTS (SELECT 1 FROM institution WHERE institution_id='COBALT_IC_EASE') THEN
    RAISE EXCEPTION 'COBALT_IC_EASE must exist before EASE finalization.';
  END IF;

  SELECT active_screening_version_id INTO STRICT v_old_intake_version_id
  FROM screening WHERE screening_id=v_intake_screening_id;
  SELECT active_screening_version_id INTO STRICT v_old_phq9_version_id
  FROM screening WHERE screening_id=v_phq9_screening_id;
  SELECT active_screening_flow_version_id INTO STRICT v_old_intake_flow_version_id
  FROM screening_flow WHERE screening_flow_id=v_intake_flow_id;
  SELECT active_screening_flow_version_id INTO STRICT v_old_clinical_flow_version_id
  FROM screening_flow WHERE screening_flow_id=v_clinical_flow_id;

  INSERT INTO screening_version (
    screening_version_id, screening_id, screening_type_id,
    created_by_account_id, version_number, scoring_function
  )
  SELECT v_intake_version_id, screening_id, screening_type_id,
         created_by_account_id, version_number + 1,
         $jscode$
const answers = input.screeningAnswers.map((answer) =>
  input.screeningAnswerOptionsByScreeningAnswerId[answer.screeningAnswerId]);
const notEligible = answers.some((option) => option && option.score === 1);
output.completed = notEligible || input.answeredScreeningQuestionCount === input.screeningQuestionsWithAnswerOptions.length;
output.score = { overallScore: notEligible ? 1 : 0 };
output.belowScoringThreshold = !notEligible;
$jscode$
  FROM screening_version WHERE screening_version_id=v_old_intake_version_id;

  INSERT INTO screening_question (
    screening_version_id, screening_answer_format_id, intro_text, question_text,
    minimum_answer_count, maximum_answer_count, display_order
  ) VALUES
    (v_intake_version_id, 'SINGLE_SELECT', 'Employment status',
     'This pilot is currently intended for Pennsylvania Hospital (PAH) employees only. Are you a PAH employee?', 1, 1, 1),
    (v_intake_version_id, 'SINGLE_SELECT', 'What EASE Clinic provides',
     'EASE Clinic provides quick symptom evaluation, immediate psychiatric intervention when needed, brief psychotherapeutic support, warm hand-off scheduling to longer-term care, and follow-up planning until you are bridged to ongoing care, if needed.', 1, 1, 2),
    (v_intake_version_id, 'SINGLE_SELECT', 'What EASE Clinic does not provide',
     'This service is typically not appropriate for administrative paperwork (including FMLA, disability, workers'' compensation, or ESA requests), legal or forensic evaluations, or neuropsychological testing (for example, ADHD evaluations).', 1, 1, 3),
    (v_intake_version_id, 'SINGLE_SELECT', 'Eligibility confirmation',
     'Staff will confirm eligibility prior to your appointment, and you may be contacted if any issues are identified.<br/><br/>The Department of Psychiatry and/or Cobalt are not responsible for fees that may arise from incomplete or inaccurate information.', 1, 1, 4);

  INSERT INTO screening_answer_option (
    screening_question_id, answer_option_text, score, display_order
  )
  SELECT question.screening_question_id, answer.answer_text,
         answer.score, answer.answer_order
  FROM screening_question question
  JOIN (VALUES
    (1, 1, 'Yes', 0), (1, 2, 'No', 1),
    (2, 1, 'I understand', 0), (3, 1, 'I understand', 0),
    (4, 1, 'I understand', 0)
  ) AS answer(question_order, answer_order, answer_text, score)
    ON answer.question_order=question.display_order
  WHERE question.screening_version_id=v_intake_version_id;

  INSERT INTO screening_version (
    screening_version_id, screening_id, screening_type_id,
    created_by_account_id, version_number, scoring_function
  )
  SELECT v_phq9_version_id, screening_id, screening_type_id,
         created_by_account_id, version_number + 1,
         $jscode$
let score = 0;
input.screeningAnswers.forEach((answer) => {
  score += input.screeningAnswerOptionsByScreeningAnswerId[answer.screeningAnswerId].score;
});
output.completed = input.answeredScreeningQuestionCount === 9;
output.score = { overallScore: score };
output.belowScoringThreshold = score < 5;
$jscode$
  FROM screening_version WHERE screening_version_id=v_old_phq9_version_id;

  INSERT INTO screening_question (
    screening_version_id, screening_answer_format_id, screening_answer_content_hint_id,
    intro_text, question_text, minimum_answer_count, maximum_answer_count,
    display_order, footer_text, metadata, prefer_autosubmit,
    screening_question_submission_style_id, supplement_text
  )
  SELECT v_phq9_version_id, screening_answer_format_id, screening_answer_content_hint_id,
         intro_text, question_text, minimum_answer_count, maximum_answer_count,
         display_order, footer_text, metadata, prefer_autosubmit,
         screening_question_submission_style_id, supplement_text
  FROM screening_question WHERE screening_version_id=v_old_phq9_version_id;

  INSERT INTO screening_answer_option (
    screening_question_id, answer_option_text, score, indicates_crisis,
    display_order, freeform_supplement, freeform_supplement_text,
    freeform_supplement_text_auto_show, freeform_supplement_content_hint_id, metadata
  )
  SELECT new_question.screening_question_id, old_option.answer_option_text,
         old_option.score, old_option.indicates_crisis, old_option.display_order,
         old_option.freeform_supplement, old_option.freeform_supplement_text,
         old_option.freeform_supplement_text_auto_show,
         old_option.freeform_supplement_content_hint_id, old_option.metadata
  FROM screening_question old_question
  JOIN screening_answer_option old_option
    ON old_option.screening_question_id=old_question.screening_question_id
  JOIN screening_question new_question
    ON new_question.screening_version_id=v_phq9_version_id
   AND new_question.display_order=old_question.display_order
  WHERE old_question.screening_version_id=v_old_phq9_version_id;

  INSERT INTO screening_flow_version (
    screening_flow_version_id, screening_flow_id, initial_screening_id,
    phone_number_required, skippable, version_number,
    orchestration_function, results_function, destination_function,
    created_by_account_id
  )
  SELECT v_intake_flow_version_id, screening_flow_id, initial_screening_id,
         phone_number_required, skippable, version_number + 1,
         $jscode$
const intake = input.screeningSessionScreenings[0];
output.crisisIndicated = false;
output.completed = !!(intake && intake.completed);
output.nextScreeningId = null;
if (intake && intake.completed && intake.scoreAsObject.overallScore === 1) {
  output.patientOrderClosureReasonId = 'INELIGIBLE_FOR_IC';
}
$jscode$,
         results_function,
         $jscode$
output.screeningSessionDestinationId = null;
output.context = {};

if (input.screeningSession.completed) {
  const intake = input.screeningSessionScreenings[0];
  if (intake.scoreAsObject.overallScore === 1) {
    output.screeningSessionDestinationId = 'IC_PATIENT_ELIGIBILITY_EXIT';
    output.context = {};
  } else {
    output.screeningSessionDestinationId = input.selfAdministered
      ? 'IC_PATIENT_CLINICAL_SCREENING' : 'IC_MHIC_CLINICAL_SCREENING';
    output.context = { patientOrderId: input.additionalContext.patientOrderId };
  }
}
$jscode$,
         created_by_account_id
  FROM screening_flow_version
  WHERE screening_flow_version_id=v_old_intake_flow_version_id;

  INSERT INTO screening_flow_version (
    screening_flow_version_id, screening_flow_id, initial_screening_id,
    phone_number_required, skippable, version_number,
    orchestration_function, results_function, destination_function,
    created_by_account_id
  )
  SELECT v_clinical_flow_version_id, screening_flow_id, initial_screening_id,
         phone_number_required, skippable, version_number + 1,
         orchestration_function,
         $jscode$
output.supportRoleRecommendations = [{ supportRoleId: 'MHP', weight: 1 }];
output.recommendedTagIds = [];
output.integratedCareTriages = [{
  patientOrderFocusTypeId: 'EVALUATION',
  patientOrderCareTypeId: 'COLLABORATIVE',
  reason: 'Completed EASE Clinic assessment; self-booking is available.'
}];
output.integratedCareTriagedCareTypeId = 'COLLABORATIVE';
$jscode$,
         destination_function, created_by_account_id
  FROM screening_flow_version
  WHERE screening_flow_version_id=v_old_clinical_flow_version_id;

  UPDATE screening SET active_screening_version_id=v_intake_version_id
  WHERE screening_id=v_intake_screening_id;
  UPDATE screening SET active_screening_version_id=v_phq9_version_id
  WHERE screening_id=v_phq9_screening_id;
  UPDATE screening_flow SET active_screening_flow_version_id=v_intake_flow_version_id
  WHERE screening_flow_id=v_intake_flow_id;
  UPDATE screening_flow SET active_screening_flow_version_id=v_clinical_flow_version_id
  WHERE screening_flow_id=v_clinical_flow_id;

  UPDATE institution
  SET integrated_care_phone_number='+12158297052',
      clinical_support_phone_number='+12158297052',
      integrated_care_availability_description='',
      integrated_care_patient_intro_override='Thank you for your interest in EASE Clinic. Follow the steps below to connect your MyChart account, complete a brief assessment, and self-schedule an appointment.',
      integrated_care_booking_insurance_requirements='<p>Staff will confirm eligibility prior to your appointment, and you may be contacted if any issues are identified.</p>',
      integrated_care_mhp_triage_overview_override='Thank you for completing the assessment. <strong>You may continue to self-schedule an EASE Clinic consultation.</strong>'
  WHERE institution_id='COBALT_IC_EASE';

  UPDATE institution_feature
  SET description='Select an available time for a 60-minute in-person EASE Clinic consultation at Pennsylvania Hospital''s Hall-Mercer Center.'
  WHERE institution_id='COBALT_IC_EASE' AND feature_id='MHP';

  UPDATE epic_department
  SET department_id='1366', department_id_type='EXTERNAL',
      name='Pennsylvania Hospital Hall-Mercer EASE Clinic'
  WHERE epic_department_id=v_ease_department_id
    AND institution_id='COBALT_IC_EASE';

  INSERT INTO appointment_type (
    appointment_type_id, name, description, duration_in_minutes,
    scheduling_system_id, epic_visit_type_id, epic_visit_type_id_type,
    visit_type_id, hex_color
  ) VALUES (
    v_taylor_appointment_type_id, 'EASE Clinic Initial Consultation',
    'In-person consultation at Pennsylvania Hospital Hall-Mercer Center',
    60, 'COBALT', NULL, NULL, 'INITIAL', '3494369'
  );

  INSERT INTO provider (
    provider_id, institution_id, name, email_address, scheduling_system_id,
    epic_provider_id, epic_provider_id_type, videoconference_platform_id,
    epic_appointment_filter_id, system_affinity_id,
    scheduling_lead_time_in_hours, url_name, virtual_appointments_only
  ) VALUES (
    v_taylor_provider_id, 'COBALT_IC_EASE', 'Taylor Dibble', NULL, 'COBALT',
    NULL, NULL, 'EXTERNAL', 'VISIT_TYPE', 'COBALT',
    16, 'taylor-dibble-ease-clinic', FALSE
  );

  INSERT INTO provider_support_role (provider_id, support_role_id)
  VALUES (v_taylor_provider_id, 'MHP');
  INSERT INTO provider_appointment_type (provider_id, appointment_type_id, display_order)
  VALUES (v_taylor_provider_id, v_taylor_appointment_type_id, 1);
  UPDATE provider SET time_zone='America/New_York'
  WHERE provider_id=v_taylor_provider_id;

  -- The local EASE tenant has no Epic connection. Publish a native schedule so
  -- the booking path can be exercised without mocked Epic availability.
  INSERT INTO logical_availability (
    logical_availability_id, provider_id, start_date_time, end_date_time,
    logical_availability_type_id, recurrence_type_id,
    recur_sunday, recur_monday, recur_tuesday, recur_wednesday,
    recur_thursday, recur_friday, recur_saturday,
    created_by_account_id, last_updated_by_account_id
  ) VALUES (
    '289ab8a5-9b83-4ea2-a639-3ec8e83b7a9f', v_taylor_provider_id,
    TIMESTAMP '2026-09-01 09:00:00', TIMESTAMP '2099-12-31 17:00:00',
    'OPEN', 'DAILY', FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE,
    (SELECT created_by_account_id FROM screening_flow_version
     WHERE screening_flow_version_id=v_clinical_flow_version_id),
    (SELECT created_by_account_id FROM screening_flow_version
     WHERE screening_flow_version_id=v_clinical_flow_version_id)
  );
  INSERT INTO logical_availability_appointment_type (
    logical_availability_id, appointment_type_id
  ) VALUES (
    '289ab8a5-9b83-4ea2-a639-3ec8e83b7a9f', v_taylor_appointment_type_id
  );

  UPDATE provider SET active=FALSE
  WHERE provider_id='72d08bd0-4f35-4faa-acb8-b1ed01ed6993'
    AND institution_id='COBALT_IC_EASE';
  UPDATE appointment_type SET deleted=TRUE
  WHERE appointment_type_id='b5a6bd83-6a12-45f2-9289-3bd754c95987';

  INSERT INTO crisis_contact (
    crisis_contact_id, institution_id, email_address, active, locale, time_zone
  )
  SELECT '69b39cd4-628a-46e8-8178-6b881d128b43', 'COBALT_IC_EASE',
         'maa@xmog.com', TRUE, 'en-US', 'America/New_York'
  WHERE NOT EXISTS (
    SELECT 1 FROM crisis_contact
    WHERE institution_id='COBALT_IC_EASE' AND LOWER(email_address)='maa@xmog.com'
  );
  UPDATE crisis_contact SET active=TRUE
  WHERE institution_id='COBALT_IC_EASE' AND LOWER(email_address)='maa@xmog.com';
END $ease$;

UPDATE institution_referrer
SET title='EASE Clinic Pilot for Employees',
    description='We''re partnering with the Hall-Mercer Community Behavioral Health Center of Pennsylvania Hospital to help PAH employees self-schedule an EASE Clinic consultation.',
    page_content=$page$
      <h4>What is the EASE Clinic?</h4>
      <p>EASE (Expedited Access to Support for Employees) offers rapid mental health support tailored for PAH staff. It provides an immediate bridge to care when workplace stress, anxiety, low mood, burnout, or other crises interfere with daily life.</p>
      <h4>What we provide</h4>
      <ul>
        <li>Quick evaluation of symptoms and immediate next steps</li>
        <li>Psychiatric intervention, including medication changes when needed</li>
        <li>Brief psychotherapeutic support and a warm hand-off to ongoing care</li>
        <li>Follow-up planning until ongoing care is in place, if needed</li>
      </ul>
      <h4>How it works</h4>
      <ul>
        <li>Appointments are typically available within seven days.</li>
        <li>The first visit is in person at Pennsylvania Hospital's Hall-Mercer Center.</li>
        <li>Care is documented in PennChart.</li>
      </ul>
      <p>This pilot is for PAH employees only. If you are not a PAH employee, visit <a href="https://www.penncobalt.com/">penncobalt.com</a> for other support options.</p>
      <p>If you prefer not to continue with self-service, contact Donna Campo at <a href="tel:+12158297052">215-829-7052</a>.</p>
      <p>For a life-threatening emergency, go to the nearest crisis center or emergency room.</p>
      <h4>What EASE does not provide</h4>
      <p>Administrative paperwork (FMLA, disability, workers' compensation, or ESA requests), legal or forensic evaluations, and neuropsychological testing such as ADHD evaluations are generally outside this pilot.</p>
    $page$,
    cta_description='Connect your MyChart account, complete a brief assessment, and self-schedule an in-person EASE Clinic consultation.'
WHERE from_institution_id='COBALT' AND to_institution_id='COBALT_IC_EASE' AND url_name='ease-clinic';

INSERT INTO screening_destination_content (
  screening_flow_id, screening_session_destination_id, title, message,
  action_url, action_text, contact_name, contact_phone
) VALUES (
  'f947fb9e-be69-427e-b6d8-8f02f7b66a09', 'IC_PATIENT_ELIGIBILITY_EXIT', 'EASE Clinic eligibility',
  'This pilot is currently for Pennsylvania Hospital employees only. You can explore other mental health support options for Penn Medicine employees at penncobalt.com.',
  'https://www.penncobalt.com/', 'Explore support options', 'Donna Campo', '215-829-7052'
) ON CONFLICT (screening_flow_id, screening_session_destination_id) DO UPDATE SET
  title=EXCLUDED.title, message=EXCLUDED.message,
  action_url=EXCLUDED.action_url, action_text=EXCLUDED.action_text,
  contact_name=EXCLUDED.contact_name, contact_phone=EXCLUDED.contact_phone;

COMMIT;
