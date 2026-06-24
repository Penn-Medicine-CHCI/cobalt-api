BEGIN;
SELECT _v.register_patch('256-autism-clinic-penn-updates', NULL, NULL);

-- Content updates for the Autism Clinic referrer seeded in
-- 250-autism-clinic-referrer.sql.
--
-- The appointment confirmation copy is appointment-type-specific because the
-- requested preparation language applies to the 60-minute intake call, not to
-- the 30-minute consult calls with the same provider.
ALTER TABLE appointment_type
  ADD COLUMN IF NOT EXISTS appointment_created_patient_email_body_html TEXT;

DO $$
DECLARE
  v_institution_id CONSTANT TEXT := 'COBALT';
  v_referrer_url_name CONSTANT TEXT := 'autism-clinic';
  v_screening_name CONSTANT TEXT := 'Penn Autism Clinic Assessment';
  v_screening_title CONSTANT TEXT := 'Penn Autism Clinic Eligibility Check';
  v_content_snippet_key CONSTANT TEXT := 'AUTISM_ACCEPTED_INSURANCES';
  v_booking_feature_id CONSTANT TEXT := 'MENTAL_HEALTH_PROVIDERS';
  v_booking_page_title CONSTANT TEXT := 'Book An Autism Care Consultation';
  v_booking_page_description CONSTANT TEXT := 'Find a convenient time to discuss your needs with a patient care manager.';
  v_clinic_description CONSTANT TEXT := 'Penn Autism Clinic';
  v_clinic_description_normalized CONSTANT TEXT := LOWER(REGEXP_REPLACE(TRIM(REPLACE(v_clinic_description, CHR(160), ' ')), '[[:space:]]+', ' ', 'g'));
  v_provider_name CONSTANT TEXT := 'John Skokowski';
  v_provider_url_name CONSTANT TEXT := 'john-skokowski';
  v_intake_appointment_type_name CONSTANT TEXT := 'Autism Clinic Intake Call';
  v_consult_appointment_type_name CONSTANT TEXT := 'Autism Clinic Consult Call';

  v_old_question_1_text CONSTANT TEXT := 'Are you a benefits-eligible employee of UPHS or UPenn ?';
  v_new_question_1_text CONSTANT TEXT := 'Are you a benefits-eligible UPHS or UPenn employee enrolled in a Penn-sponsored health insurance plan?';
  v_old_question_4_text CONSTANT TEXT := 'Are they covered under your Penn insurance (UPHS or UPenn)?';
  v_new_question_4_text CONSTANT TEXT := 'Are they covered under your Penn-sponsored health insurance plan?';

  v_patient_name_question_text CONSTANT TEXT := 'What is your first and last name?';
  v_patient_phone_question_text CONSTANT TEXT := 'What is your phone number?';

  v_intake_assessment_id CONSTANT UUID := '063866ee-4714-471a-a9bc-1e2d3392b754';
  v_intake_name_question_id CONSTANT UUID := 'edffa806-12d2-4857-b390-a9a68774d023';
  v_intake_phone_question_id CONSTANT UUID := '53a2a75f-30d2-48eb-b94e-519572c3cc0d';
  v_intake_name_answer_id CONSTANT UUID := 'f79df3fc-e38b-4bb1-9dfb-47dbbcf957a3';
  v_intake_phone_answer_id CONSTANT UUID := '2c811686-7a84-407a-94bc-de616f1c49cb';

  v_consult_assessment_id CONSTANT UUID := '40948871-cc58-4338-a1f2-0d8fedef3588';
  v_consult_name_question_id CONSTANT UUID := '274de921-4961-416e-9e9a-57324acd9ada';
  v_consult_phone_question_id CONSTANT UUID := '7d43cd2f-0a41-4d5a-bc1e-a4e502983518';
  v_consult_name_answer_id CONSTANT UUID := '56861da4-e0cd-4aa3-8137-0f150257d411';
  v_consult_phone_answer_id CONSTANT UUID := '570d76cd-68e6-422c-9760-10285905e302';

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
      <p>You must be a benefits-eligible UPHS or UPenn employee enrolled in a Penn-sponsored health insurance plan.</p>
      <p><strong>If you are seeking services for a dependent:</strong></p>
      <p>You must be the legal guardian of the dependent and your dependent must be covered under your Penn-sponsored health insurance plan.</p>
    </section>
  $page_content$;

  v_intake_email_body_html CONSTANT TEXT := $email_body$
    <p><b>Preparing for your intake appointment</b></p>
    <p>Please set aside 60 minutes in a private location for this phone intake session and have the following items ready and on hand:</p>
    <ul>
      <li style="margin-bottom: 5px;">Social Security numbers</li>
      <li style="margin-bottom: 5px;">Penn insurance ID number</li>
      <li style="margin-bottom: 5px;">PCP contact information</li>
      <li style="margin-bottom: 5px;">List of allergies (if any)</li>
      <li style="margin-bottom: 5px;">List of medications (if any)</li>
      <li style="margin-bottom: 5px;">History of past &amp; ongoing support services</li>
      <li style="margin-bottom: 5px;">Descriptions of current communication, behavioral, social &amp; educational concerns with the child</li>
    </ul>
    <p>Before the appointment, please email the following documents to <b><a href="mailto:John.Skokowski@pennmedicine.upenn.edu">John.Skokowski@pennmedicine.upenn.edu</a></b> for review by <b>Dr. Keiran Rump</b>, Director and Evaluator of the Penn Medicine Autism Clinic:</p>
    <ol>
      <li style="margin-bottom: 5px;">Initial ASD diagnostic evaluation report (if any)</li>
      <li style="margin-bottom: 5px;">Developmental Pediatric or other specialist follow up visit summary report (if any)</li>
      <li style="margin-bottom: 5px;">Up to date ABA medical orders (if any)</li>
      <li style="margin-bottom: 5px;">Current Individualized Education Plan - IEP, 504 Plan or Individualized Family Services Plan - IFSP (if any)</li>
    </ol>
  $email_body$;

  v_metadata TEXT;
  v_clinic_id UUID;
  v_provider_id UUID;
  v_intake_appointment_type_id UUID;
  v_consult_appointment_type_id UUID;
BEGIN
  SELECT c.clinic_id
  INTO v_clinic_id
  FROM clinic c
  WHERE c.institution_id = v_institution_id
    AND LOWER(REGEXP_REPLACE(TRIM(REPLACE(c.description, CHR(160), ' ')), '[[:space:]]+', ' ', 'g')) = v_clinic_description_normalized
  ORDER BY c.clinic_id
  LIMIT 1;

  IF v_clinic_id IS NULL THEN
    SELECT pc.clinic_id
    INTO v_clinic_id
    FROM provider p
    JOIN provider_clinic pc
      ON pc.provider_id = p.provider_id
    WHERE p.institution_id = v_institution_id
      AND (
        LOWER(TRIM(p.url_name)) = LOWER(TRIM(v_provider_url_name))
        OR LOWER(TRIM(p.name)) = LOWER(TRIM(v_provider_name))
      )
    ORDER BY pc.primary_clinic DESC,
             pc.provider_clinic_id
    LIMIT 1;
  END IF;

  IF v_clinic_id IS NULL THEN
    RAISE EXCEPTION 'Unable to find clinic "%" for institution "%"', v_clinic_description, v_institution_id;
  END IF;

  UPDATE clinic
  SET
    description = v_clinic_description,
    last_updated = NOW()
  WHERE clinic_id = v_clinic_id
    AND description IS DISTINCT FROM v_clinic_description;

  SELECT p.provider_id
  INTO v_provider_id
  FROM provider p
  WHERE p.institution_id = v_institution_id
    AND (
      LOWER(TRIM(p.url_name)) = LOWER(TRIM(v_provider_url_name))
      OR LOWER(TRIM(p.name)) = LOWER(TRIM(v_provider_name))
    )
  ORDER BY p.provider_id
  LIMIT 1;

  IF v_provider_id IS NULL THEN
    RAISE EXCEPTION 'Unable to find provider "%" for institution "%"', v_provider_name, v_institution_id;
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

  v_metadata := format($metadata$
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
      "title": "Recommended Next Step",
      "recommendation": "Schedule a time to discuss your needs with a patient care manager and determine the best next steps for care.",
      "bodyHtml": "<ul class='mb-0 ps-4'><li class='mb-3'>During the call, the patient care manager will collect additional information, including demographics, SSN, primary care details, and a summary of concerns over the past six months.</li><li class='mb-0'>They will also ask you to submit any existing documentation beforehand. More details will be included in your confirmation email.</li></ul>",
      "buttonText": "Continue to Scheduling",
      "booking": {
        "path": "/connect-with-support/autism-clinic-booking",
        "featureId": "%s",
        "pageTitle": "%s",
        "pageDescription": "%s",
        "clinicIds": ["%s"],
        "providerId": "%s",
        "appointmentTypeIds": ["%s"]
      }
    },
    "CONSULT_EVALUATION": {
      "title": "Recommended Next Step",
      "recommendation": "Schedule a time to discuss your needs with a patient care manager and determine the best next steps for care.",
      "bodyHtml": "<ul class='mb-0 ps-4'><li class='mb-3'>No preparation is needed for this call.</li><li class='mb-3'>During the call, you'll discuss what is going on with the patient and assess their needs.</li><li class='mb-0'>The next step may be a longer, formal intake call with the patient care manager.</li></ul>",
      "noteHtml": "<p class='mb-0'><strong>Note on Timing:</strong> While the clinic can start consultation right away, please be aware the current waitlist for a formal diagnostic evaluation varies, depending on age.</p>",
      "buttonText": "Continue to Scheduling",
      "booking": {
        "path": "/connect-with-support/autism-clinic-booking",
        "featureId": "%s",
        "pageTitle": "%s",
        "pageDescription": "%s",
        "clinicIds": ["%s"],
        "providerId": "%s",
        "appointmentTypeIds": ["%s"]
      }
    },
    "CONSULT_GENERAL": {
      "title": "Recommended Next Step",
      "recommendation": "Schedule a time to discuss your needs with a patient care manager and determine the best next steps for care.",
      "bodyHtml": "<ul class='mb-0 ps-4'><li class='mb-3'>No preparation is needed for this call.</li><li class='mb-3'>During the call, you'll discuss any noticed behaviors or milestones that concern you.</li><li class='mb-0'>The goal is to help you understand the best next step for support or evaluation.</li></ul>",
      "buttonText": "Continue to Scheduling",
      "booking": {
        "path": "/connect-with-support/autism-clinic-booking",
        "featureId": "%s",
        "pageTitle": "%s",
        "pageDescription": "%s",
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
    v_booking_page_description,
    v_clinic_id,
    v_provider_id,
    v_intake_appointment_type_id,
    v_booking_feature_id,
    v_booking_page_title,
    v_booking_page_description,
    v_clinic_id,
    v_provider_id,
    v_consult_appointment_type_id,
    v_booking_feature_id,
    v_booking_page_title,
    v_booking_page_description,
    v_clinic_id,
    v_provider_id,
    v_consult_appointment_type_id
  );

  UPDATE institution_referrer
  SET
    title = 'Schedule An Autism Care Consultation',
    description = 'Check your plan eligibility and book an appointment to discuss your needs.',
    page_content = v_referrer_page_content,
    cta_title = 'Get started with the Penn Autism Clinic',
    cta_description = 'Check your eligibility to book an appointment by clicking below or call (215) 746-8103 to schedule a phone intake.',
    metadata = CAST(v_metadata AS JSONB),
    last_updated = NOW()
  WHERE from_institution_id = v_institution_id
    AND url_name = v_referrer_url_name;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'Unable to find institution_referrer "%" for institution "%"', v_referrer_url_name, v_institution_id;
  END IF;

  UPDATE screening_question sq
  SET
    question_text = v_new_question_1_text,
    last_updated = NOW()
  FROM screening_version sv,
       screening s
  WHERE sq.screening_version_id = sv.screening_version_id
    AND sv.screening_id = s.screening_id
    AND s.name = v_screening_name
    AND sq.question_text = v_old_question_1_text;

  UPDATE screening_question sq
  SET
    question_text = v_new_question_4_text,
    last_updated = NOW()
  FROM screening_version sv,
       screening s
  WHERE sq.screening_version_id = sv.screening_version_id
    AND sv.screening_id = s.screening_id
    AND s.name = v_screening_name
    AND sq.question_text = v_old_question_4_text;

  UPDATE appointment_type
  SET
    appointment_created_patient_email_body_html = v_intake_email_body_html,
    last_updated = NOW()
  WHERE appointment_type_id = v_intake_appointment_type_id;

  UPDATE appointment_type
  SET
    appointment_created_patient_email_body_html = NULL,
    last_updated = NOW()
  WHERE appointment_type_id = v_consult_appointment_type_id;

  UPDATE appointment_type_assessment
  SET
    active = FALSE,
    last_updated = NOW()
  WHERE appointment_type_id IN (v_intake_appointment_type_id, v_consult_appointment_type_id)
    AND active = TRUE;

  INSERT INTO assessment (
    assessment_id,
    assessment_type_id,
    minimum_eligibility_score,
    answers_may_contain_pii
  ) VALUES
    (v_intake_assessment_id, 'INTAKE', 2, TRUE),
    (v_consult_assessment_id, 'INTAKE', 2, TRUE)
  ON CONFLICT (assessment_id) DO UPDATE
  SET
    minimum_eligibility_score = EXCLUDED.minimum_eligibility_score,
    answers_may_contain_pii = EXCLUDED.answers_may_contain_pii,
    last_updated = NOW();

  INSERT INTO question (
    question_id,
    assessment_id,
    question_type_id,
    font_size_id,
    question_content_hint_id,
    question_text,
    display_order,
    is_root_question,
    answer_required
  ) VALUES
    (v_intake_name_question_id, v_intake_assessment_id, 'TEXT', 'DEFAULT', 'NONE', v_patient_name_question_text, 1, TRUE, TRUE),
    (v_intake_phone_question_id, v_intake_assessment_id, 'TEXT', 'DEFAULT', 'PHONE_NUMBER', v_patient_phone_question_text, 2, TRUE, TRUE),
    (v_consult_name_question_id, v_consult_assessment_id, 'TEXT', 'DEFAULT', 'NONE', v_patient_name_question_text, 1, TRUE, TRUE),
    (v_consult_phone_question_id, v_consult_assessment_id, 'TEXT', 'DEFAULT', 'PHONE_NUMBER', v_patient_phone_question_text, 2, TRUE, TRUE)
  ON CONFLICT (question_id) DO UPDATE
  SET
    question_type_id = EXCLUDED.question_type_id,
    font_size_id = EXCLUDED.font_size_id,
    question_content_hint_id = EXCLUDED.question_content_hint_id,
    question_text = EXCLUDED.question_text,
    display_order = EXCLUDED.display_order,
    is_root_question = EXCLUDED.is_root_question,
    answer_required = EXCLUDED.answer_required,
    last_updated = NOW();

  INSERT INTO answer (
    answer_id,
    question_id,
    answer_text,
    display_order,
    answer_value,
    crisis,
    call,
    next_question_id
  ) VALUES
    (v_intake_name_answer_id, v_intake_name_question_id, 'Type here', 1, 1, FALSE, FALSE, v_intake_phone_question_id),
    (v_intake_phone_answer_id, v_intake_phone_question_id, 'Type here', 1, 1, FALSE, FALSE, NULL),
    (v_consult_name_answer_id, v_consult_name_question_id, 'Type here', 1, 1, FALSE, FALSE, v_consult_phone_question_id),
    (v_consult_phone_answer_id, v_consult_phone_question_id, 'Type here', 1, 1, FALSE, FALSE, NULL)
  ON CONFLICT (answer_id) DO UPDATE
  SET
    answer_text = EXCLUDED.answer_text,
    display_order = EXCLUDED.display_order,
    answer_value = EXCLUDED.answer_value,
    crisis = EXCLUDED.crisis,
    call = EXCLUDED.call,
    next_question_id = EXCLUDED.next_question_id,
    last_updated = NOW();

  INSERT INTO appointment_type_assessment (
    appointment_type_id,
    assessment_id,
    active
  ) VALUES
    (v_intake_appointment_type_id, v_intake_assessment_id, TRUE),
    (v_consult_appointment_type_id, v_consult_assessment_id, TRUE)
  ON CONFLICT (assessment_id, appointment_type_id) DO UPDATE
  SET
    active = EXCLUDED.active,
    last_updated = NOW();
END $$;

COMMIT;
