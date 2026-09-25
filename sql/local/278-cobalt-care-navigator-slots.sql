BEGIN;
SELECT _v.register_patch(
  '278-local-only-cobalt-care-navigator-slots',
  ARRAY['263-local-only-care-navigator-seed',
        '261-provider-booking-database'],
  NULL
);

-- One clinic booking page combines two providers. Each provider owns its
-- availability and maps to one navigator, so the existing encounter and
-- appointment email paths select the correct staff account without slot routing.
DO $local_care_navigation$
DECLARE
  v_becca_provider_id UUID;
  v_michal_provider_id CONSTANT UUID :=
    uuid_generate_v5(uuid_ns_url(), 'cobalt-local-care-navigation-michal-provider');
  v_clinic_id CONSTANT UUID :=
    uuid_generate_v5(uuid_ns_url(), 'cobalt-local-care-navigation-clinic');
  v_appointment_type_id UUID;
  v_becca_account_id UUID;
  v_michal_account_id UUID;
BEGIN
  SELECT provider_id INTO STRICT v_becca_provider_id
  FROM provider
  WHERE institution_id='COBALT' AND url_name='cobalt-care-navigator' AND active=TRUE;

  SELECT pat.appointment_type_id INTO STRICT v_appointment_type_id
  FROM provider_appointment_type pat
  JOIN appointment_type apt ON apt.appointment_type_id=pat.appointment_type_id
  WHERE pat.provider_id=v_becca_provider_id AND apt.deleted=FALSE
    AND apt.duration_in_minutes=30 AND apt.scheduling_system_id='COBALT';

  SELECT account_id INTO STRICT v_becca_account_id
  FROM account
  WHERE institution_id='COBALT' AND active=TRUE
    AND LOWER(email_address)='care-navigator@cobaltinnovations.org';
  SELECT account_id INTO v_michal_account_id
  FROM account
  WHERE institution_id='COBALT'
    AND LOWER(email_address)='care-navigator-michal@example.com'
  LIMIT 1;
  IF v_michal_account_id IS NULL THEN
    v_michal_account_id :=
      uuid_generate_v5(uuid_ns_url(), 'cobalt-local-care-navigation-michal-account');
    INSERT INTO account (
      account_id, role_id, institution_id, account_source_id,
      email_address, password, first_name, last_name, display_name,
      provider_id, locale, time_zone, active, test_account
    ) VALUES (
      v_michal_account_id, 'ADMINISTRATOR', 'COBALT', 'EMAIL_PASSWORD',
      'care-navigator-michal@example.com',
      '$2a$10$M2tPoJ8eQr55OW4iOfpbBOpgqFWt0LxnvVBnW1a/1LhKNA6SuUN42',
      'Michal', 'Weiss', 'Michal Weiss', v_becca_provider_id,
      'en-US', 'America/New_York', TRUE, TRUE
    );
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM account
    WHERE account_id=v_michal_account_id AND active=TRUE
      AND role_id IN ('ADMINISTRATOR', 'PROVIDER')
  ) THEN
    RAISE EXCEPTION 'Local Michal fixture account must be active and authorized.';
  END IF;

  IF EXISTS (SELECT 1 FROM provider
             WHERE provider_id=v_michal_provider_id AND institution_id<>'COBALT') THEN
    RAISE EXCEPTION 'The Michal Care Navigator provider ID is already in use.';
  END IF;

  INSERT INTO provider (
    provider_id, institution_id, name, title, entity, clinic, specialty,
    email_address, locale, time_zone, active, scheduling_system_id,
    videoconference_platform_id, system_affinity_id, url_name, bio,
    description, tags, display_phone_number_only_for_booking, details_html
  )
  SELECT v_michal_provider_id, institution_id, name, title, entity, clinic,
    specialty, 'care-navigator-michal@example.com', locale, time_zone,
    TRUE, scheduling_system_id, videoconference_platform_id,
    system_affinity_id, 'cobalt-care-navigator-michal', bio, description, tags,
    display_phone_number_only_for_booking, details_html
  FROM provider WHERE provider_id=v_becca_provider_id
  ON CONFLICT (provider_id) DO UPDATE
  SET name=EXCLUDED.name, title=EXCLUDED.title, entity=EXCLUDED.entity,
      clinic=EXCLUDED.clinic, specialty=EXCLUDED.specialty,
      email_address=EXCLUDED.email_address, locale=EXCLUDED.locale,
      time_zone=EXCLUDED.time_zone, active=EXCLUDED.active,
      scheduling_system_id=EXCLUDED.scheduling_system_id,
      videoconference_platform_id=EXCLUDED.videoconference_platform_id,
      system_affinity_id=EXCLUDED.system_affinity_id, bio=EXCLUDED.bio,
      description=EXCLUDED.description, tags=EXCLUDED.tags,
      details_html=EXCLUDED.details_html;

  UPDATE account
  SET provider_id=v_michal_provider_id
  WHERE account_id=v_michal_account_id
    AND provider_id IS DISTINCT FROM v_michal_provider_id;

  INSERT INTO provider_support_role (provider_id, support_role_id)
  VALUES (v_michal_provider_id, 'CARE_NAVIGATOR')
  ON CONFLICT (provider_id, support_role_id) DO NOTHING;

  INSERT INTO provider_institution_location (provider_id, institution_location_id)
  SELECT v_michal_provider_id, source.institution_location_id
  FROM provider_institution_location source
  WHERE source.provider_id=v_becca_provider_id
    AND NOT EXISTS (
      SELECT 1 FROM provider_institution_location existing
      WHERE existing.provider_id=v_michal_provider_id
        AND existing.institution_location_id=source.institution_location_id
    );

  INSERT INTO provider_payment_type (provider_id, payment_type_id)
  SELECT v_michal_provider_id, payment_type_id
  FROM provider_payment_type WHERE provider_id=v_becca_provider_id
  ON CONFLICT (provider_id, payment_type_id) DO NOTHING;

  INSERT INTO provider_appointment_type
    (provider_id, appointment_type_id, display_order)
  SELECT v_michal_provider_id, v_appointment_type_id, 1
  WHERE NOT EXISTS (
    SELECT 1 FROM provider_appointment_type
    WHERE provider_id=v_michal_provider_id
      AND appointment_type_id=v_appointment_type_id
  );

  INSERT INTO account_capability (account_id, account_capability_type_id)
  VALUES (v_becca_account_id, 'NAVIGATOR'), (v_michal_account_id, 'NAVIGATOR')
  ON CONFLICT (account_id, account_capability_type_id) DO NOTHING;

  INSERT INTO care_navigator_provider_account (provider_id, account_id, display_order)
  VALUES (v_becca_provider_id, v_becca_account_id, 1),
         (v_michal_provider_id, v_michal_account_id, 1)
  ON CONFLICT (provider_id, account_id) DO UPDATE
  SET display_order=EXCLUDED.display_order;

  -- Remove the earlier local-preview mapping if this script is reapplied there.
  DELETE FROM care_navigator_provider_account
  WHERE provider_id=v_becca_provider_id AND account_id=v_michal_account_id;

  IF NOT care_navigator_account_can_serve_provider(v_becca_account_id, v_becca_provider_id)
    OR NOT care_navigator_account_can_serve_provider(v_michal_account_id, v_michal_provider_id) THEN
    RAISE EXCEPTION 'Becca and Michal must both be eligible Care Navigators before slots are published.';
  END IF;

  INSERT INTO clinic (
    clinic_id, description, treatment_description, institution_id,
    show_intake_assessment_prompt, appointment_booking_level_id, details_html,
    image_url
  )
  SELECT v_clinic_id, 'Care Navigator', 'Care navigation consultations',
    'COBALT', FALSE, 'CLINIC', details_html,
    'https://cdn-prod.cobalt.care/providers/penn/penn-provider-placeholder-centered.png'
  FROM provider WHERE provider_id=v_becca_provider_id
  ON CONFLICT (clinic_id) DO UPDATE
  SET description=EXCLUDED.description,
      treatment_description=EXCLUDED.treatment_description,
      show_intake_assessment_prompt=EXCLUDED.show_intake_assessment_prompt,
      appointment_booking_level_id=EXCLUDED.appointment_booking_level_id,
      details_html=EXCLUDED.details_html,
      image_url=EXCLUDED.image_url;

  IF EXISTS (
    SELECT 1 FROM provider_clinic pc
    JOIN clinic c ON c.clinic_id=pc.clinic_id
    WHERE pc.provider_id IN (v_becca_provider_id, v_michal_provider_id)
      AND pc.clinic_id<>v_clinic_id
      AND c.appointment_booking_level_id='CLINIC'
  ) THEN
    RAISE EXCEPTION 'Care Navigator providers already belong to another bookable clinic.';
  END IF;

  INSERT INTO provider_clinic (provider_clinic_id, provider_id, clinic_id, primary_clinic)
  SELECT uuid_generate_v5(uuid_ns_url(), 'cobalt-local-care-navigation-clinic-' || p.provider_id),
    p.provider_id, v_clinic_id, TRUE
  FROM provider p
  WHERE p.provider_id IN (v_becca_provider_id, v_michal_provider_id)
    AND NOT EXISTS (
      SELECT 1 FROM provider_clinic existing
      WHERE existing.provider_id=p.provider_id AND existing.clinic_id=v_clinic_id
    );

  -- Retire the broad legacy schedule without changing past appointments.
  DELETE FROM logical_availability_appointment_type laat
  USING logical_availability la
  WHERE laat.logical_availability_id=la.logical_availability_id
    AND la.provider_id IN (v_becca_provider_id, v_michal_provider_id)
    AND laat.appointment_type_id=v_appointment_type_id;

  INSERT INTO logical_availability (
    logical_availability_id, provider_id, start_date_time, end_date_time,
    logical_availability_type_id, recurrence_type_id,
    recur_sunday, recur_monday, recur_tuesday, recur_wednesday,
    recur_thursday, recur_friday, recur_saturday,
    created_by_account_id, last_updated_by_account_id
  )
  SELECT uuid_generate_v5(uuid_ns_url(),
           'cobalt-local-care-navigation-clinic-' || slot.iso_day_of_week || '-' || slot.local_start_time),
         CASE WHEN slot.navigator='MICHAL' THEN v_michal_provider_id
              ELSE v_becca_provider_id END,
         DATE '2026-09-28' + slot.local_start_time,
         DATE '2099-12-31' + slot.local_end_time,
         'OPEN', 'DAILY',
         FALSE, slot.iso_day_of_week=1, slot.iso_day_of_week=2, FALSE,
         slot.iso_day_of_week=4, FALSE, FALSE,
         CASE WHEN slot.navigator='MICHAL' THEN v_michal_account_id
              ELSE v_becca_account_id END,
         CASE WHEN slot.navigator='MICHAL' THEN v_michal_account_id
              ELSE v_becca_account_id END
  FROM (VALUES
    (1, TIME '12:15', TIME '12:45', 'BECCA'),
    (1, TIME '13:15', TIME '13:45', 'BECCA'),
    (1, TIME '14:15', TIME '14:45', 'BECCA'),
    (2, TIME '09:15', TIME '09:45', 'MICHAL'),
    (2, TIME '10:15', TIME '10:45', 'BECCA'),
    (2, TIME '11:15', TIME '11:45', 'BECCA'),
    (4, TIME '09:15', TIME '09:45', 'MICHAL'),
    (4, TIME '10:15', TIME '10:45', 'MICHAL'),
    (4, TIME '11:15', TIME '11:45', 'BECCA'),
    (4, TIME '12:15', TIME '12:45', 'BECCA'),
    (4, TIME '13:15', TIME '13:45', 'BECCA'),
    (4, TIME '14:15', TIME '14:45', 'BECCA')
  ) AS slot(iso_day_of_week, local_start_time, local_end_time, navigator)
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

  INSERT INTO logical_availability_appointment_type
    (logical_availability_id, appointment_type_id)
  SELECT la.logical_availability_id, v_appointment_type_id
  FROM logical_availability la
  WHERE la.provider_id IN (v_becca_provider_id, v_michal_provider_id)
    AND la.logical_availability_id IN (
      SELECT uuid_generate_v5(uuid_ns_url(),
               'cobalt-local-care-navigation-clinic-' || slot.iso_day_of_week || '-' || slot.local_start_time)
      FROM (VALUES
        (1, TIME '12:15'), (1, TIME '13:15'), (1, TIME '14:15'),
        (2, TIME '09:15'), (2, TIME '10:15'), (2, TIME '11:15'),
        (4, TIME '09:15'), (4, TIME '10:15'), (4, TIME '11:15'),
        (4, TIME '12:15'), (4, TIME '13:15'), (4, TIME '14:15')
      ) AS slot(iso_day_of_week, local_start_time)
    )
  ON CONFLICT DO NOTHING;

  IF (SELECT COUNT(*) FROM logical_availability_appointment_type laat
      JOIN logical_availability la USING (logical_availability_id)
      WHERE la.provider_id IN (v_becca_provider_id, v_michal_provider_id)
        AND laat.appointment_type_id=v_appointment_type_id) <> 12
    OR (SELECT COUNT(*) FROM logical_availability_appointment_type laat
      JOIN logical_availability la USING (logical_availability_id)
      WHERE la.provider_id=v_michal_provider_id
        AND laat.appointment_type_id=v_appointment_type_id) <> 3
    OR (SELECT COUNT(*) FROM provider_clinic
      WHERE clinic_id=v_clinic_id
        AND provider_id IN (v_becca_provider_id, v_michal_provider_id)) <> 2 THEN
    RAISE EXCEPTION 'COBALT Care Navigator clinic must have two providers and the requested 9/3 slot split.';
  END IF;

  UPDATE institution_feature
  SET nav_visible=TRUE, landing_page_visible=TRUE
  WHERE institution_id='COBALT' AND feature_id='RESOURCE_NAVIGATOR'
    AND provider_id=v_becca_provider_id;
  UPDATE institution SET booking_v2_enabled=TRUE WHERE institution_id='COBALT';
END $local_care_navigation$;

COMMIT;
