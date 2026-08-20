BEGIN;
SELECT _v.register_patch(
	'261-local-only-care-encounter-seed',
	ARRAY[
		'260-local-only-care-navigator-seed',
		'261-care-encounters'
	],
	NULL
);

-- Local/bootstrap-only Care Encounter fixtures. Dynamic dates keep the admin
-- screen populated with recent, upcoming, and canceled examples after every
-- database recreation.
DO $$
DECLARE
	v_institution_id CONSTANT TEXT := 'COBALT';
	v_provider_id CONSTANT UUID := 'ca4e0000-0000-4000-8000-000000000002';
	v_navigator_account_id CONSTANT UUID := 'ca4e0000-0000-4000-8000-000000000001';
	v_appointment_type_id CONSTANT UUID := 'ca4e0000-0000-4000-8000-000000000003';
	v_screening_version_id CONSTANT UUID := 'ca4e0000-0000-4000-8000-000000000006';
	v_navigation_question_id CONSTANT UUID := 'ca4e0000-0000-4000-8000-000000000007';
	v_navigation_answer_option_id CONSTANT UUID := 'ca4e0000-0000-4000-8000-000000000008';
	v_screening_flow_version_id CONSTANT UUID := 'ca4e0000-0000-4000-8000-00000000000c';
	v_support_question_id CONSTANT UUID := 'ca4e0000-0000-4000-8000-00000000000f';
	v_support_provider_option_id CONSTANT UUID := 'ca4e0000-0000-4000-8000-000000000010';
	v_support_benefits_option_id CONSTANT UUID := 'ca4e0000-0000-4000-8000-000000000011';
	v_follow_up_question_id CONSTANT UUID := 'ca4e0000-0000-4000-8000-000000000013';
	v_follow_up_email_option_id CONSTANT UUID := 'ca4e0000-0000-4000-8000-000000000014';
	v_context_question_id CONSTANT UUID := 'ca4e0000-0000-4000-8000-000000000017';
	v_context_answer_option_id CONSTANT UUID := 'ca4e0000-0000-4000-8000-000000000018';
	v_patient_one_id CONSTANT UUID := 'ca4e1000-0000-4000-8000-000000000001';
	v_patient_two_id CONSTANT UUID := 'ca4e1000-0000-4000-8000-000000000002';
	v_patient_three_id CONSTANT UUID := 'ca4e1000-0000-4000-8000-000000000003';
	v_patient_four_id CONSTANT UUID := 'ca4e1000-0000-4000-8000-000000000004';
	v_completed_appointment_id CONSTANT UUID := 'ca4e2000-0000-4000-8000-000000000001';
	v_upcoming_appointment_id CONSTANT UUID := 'ca4e2000-0000-4000-8000-000000000002';
	v_canceled_appointment_id CONSTANT UUID := 'ca4e2000-0000-4000-8000-000000000003';
	v_rebooked_appointment_id CONSTANT UUID := 'ca4e2000-0000-4000-8000-000000000004';
	v_patient_canceled_appointment_id CONSTANT UUID := 'ca4e2000-0000-4000-8000-000000000005';
	v_upcoming_screening_session_id CONSTANT UUID := 'ca4e3000-0000-4000-8000-000000000001';
	v_upcoming_session_screening_id CONSTANT UUID := 'ca4e3000-0000-4000-8000-000000000002';
	v_upcoming_navigation_response_id CONSTANT UUID := 'ca4e3000-0000-4000-8000-000000000003';
	v_upcoming_support_response_id CONSTANT UUID := 'ca4e3000-0000-4000-8000-000000000004';
	v_upcoming_follow_up_response_id CONSTANT UUID := 'ca4e3000-0000-4000-8000-000000000005';
	v_upcoming_context_response_id CONSTANT UUID := 'ca4e3000-0000-4000-8000-000000000006';
	v_upcoming_navigation_answer_id CONSTANT UUID := 'ca4e3000-0000-4000-8000-000000000007';
	v_upcoming_support_provider_answer_id CONSTANT UUID := 'ca4e3000-0000-4000-8000-000000000008';
	v_upcoming_support_benefits_answer_id CONSTANT UUID := 'ca4e3000-0000-4000-8000-000000000009';
	v_upcoming_follow_up_answer_id CONSTANT UUID := 'ca4e3000-0000-4000-8000-00000000000a';
	v_upcoming_context_answer_id CONSTANT UUID := 'ca4e3000-0000-4000-8000-00000000000b';
	v_upcoming_context_text CONSTANT TEXT := 'I would like help finding an in-network therapist with evening availability.';
	v_appointment_reason_id UUID;
	v_today TIMESTAMP := DATE_TRUNC('day', NOW() AT TIME ZONE 'America/New_York');
BEGIN
	UPDATE provider
	SET virtual_appointments_only=TRUE
	WHERE provider_id=v_provider_id;

	SELECT appointment_reason_id
	INTO v_appointment_reason_id
	FROM appointment_reason
	WHERE institution_id=v_institution_id
	AND appointment_reason_type_id='NOT_SPECIFIED'
	ORDER BY appointment_reason_id
	LIMIT 1;

	IF v_appointment_reason_id IS NULL THEN
		RAISE EXCEPTION 'A NOT_SPECIFIED appointment reason is required for the Care Encounter fixture';
	END IF;

	INSERT INTO account (
		account_id,
		role_id,
		institution_id,
		account_source_id,
		email_address,
		first_name,
		last_name,
		display_name,
		locale,
		time_zone,
		active,
		test_account
	) VALUES
		(v_patient_one_id, 'PATIENT', v_institution_id, 'EMAIL_PASSWORD', 'care-encounter.alex@example.com', 'Alex', 'Morgan', 'Alex Morgan', 'en-US', 'America/New_York', TRUE, TRUE),
		(v_patient_two_id, 'PATIENT', v_institution_id, 'EMAIL_PASSWORD', 'care-encounter.jordan@example.com', 'Jordan', 'Lee', 'Jordan Lee', 'en-US', 'America/New_York', TRUE, TRUE),
		(v_patient_three_id, 'PATIENT', v_institution_id, 'EMAIL_PASSWORD', 'care-encounter.taylor@example.com', 'Taylor', 'Rivera', 'Taylor Rivera', 'en-US', 'America/New_York', TRUE, TRUE),
		(v_patient_four_id, 'PATIENT', v_institution_id, 'EMAIL_PASSWORD', 'care-encounter.casey@example.com', 'Casey', 'Nguyen', 'Casey Nguyen', 'en-US', 'America/New_York', TRUE, TRUE)
	ON CONFLICT (account_id) DO UPDATE
	SET first_name=EXCLUDED.first_name,
		last_name=EXCLUDED.last_name,
		display_name=EXCLUDED.display_name,
		active=EXCLUDED.active,
		test_account=EXCLUDED.test_account;

	INSERT INTO screening_session (
		screening_session_id,
		screening_flow_version_id,
		target_account_id,
		created_by_account_id,
		completed,
		crisis_indicated,
		completed_at
	) VALUES (
		v_upcoming_screening_session_id,
		v_screening_flow_version_id,
		v_patient_two_id,
		v_patient_two_id,
		TRUE,
		FALSE,
		NOW()
	)
	ON CONFLICT (screening_session_id) DO UPDATE
	SET screening_flow_version_id=EXCLUDED.screening_flow_version_id,
		target_account_id=EXCLUDED.target_account_id,
		created_by_account_id=EXCLUDED.created_by_account_id,
		completed=EXCLUDED.completed,
		crisis_indicated=EXCLUDED.crisis_indicated,
		completed_at=EXCLUDED.completed_at;

	INSERT INTO screening_session_screening (
		screening_session_screening_id,
		screening_session_id,
		screening_version_id,
		screening_order,
		completed,
		score,
		below_scoring_threshold
	) VALUES (
		v_upcoming_session_screening_id,
		v_upcoming_screening_session_id,
		v_screening_version_id,
		1,
		TRUE,
		'{"overallScore":1}'::JSONB,
		FALSE
	)
	ON CONFLICT (screening_session_screening_id) DO UPDATE
	SET screening_session_id=EXCLUDED.screening_session_id,
		screening_version_id=EXCLUDED.screening_version_id,
		screening_order=EXCLUDED.screening_order,
		completed=EXCLUDED.completed,
		score=EXCLUDED.score,
		below_scoring_threshold=EXCLUDED.below_scoring_threshold;

	INSERT INTO screening_session_answered_screening_question (
		screening_session_answered_screening_question_id,
		screening_session_screening_id,
		screening_question_id
	) VALUES
		(v_upcoming_navigation_response_id, v_upcoming_session_screening_id, v_navigation_question_id),
		(v_upcoming_support_response_id, v_upcoming_session_screening_id, v_support_question_id),
		(v_upcoming_follow_up_response_id, v_upcoming_session_screening_id, v_follow_up_question_id),
		(v_upcoming_context_response_id, v_upcoming_session_screening_id, v_context_question_id)
	ON CONFLICT (screening_session_answered_screening_question_id) DO UPDATE
	SET screening_session_screening_id=EXCLUDED.screening_session_screening_id,
		screening_question_id=EXCLUDED.screening_question_id,
		valid=TRUE;

	INSERT INTO screening_answer (
		screening_answer_id,
		screening_answer_option_id,
		screening_session_answered_screening_question_id,
		created_by_account_id,
		text,
		answer_order
	) VALUES
		(v_upcoming_navigation_answer_id, v_navigation_answer_option_id, v_upcoming_navigation_response_id, v_patient_two_id, NULL, 1),
		(v_upcoming_support_provider_answer_id, v_support_provider_option_id, v_upcoming_support_response_id, v_patient_two_id, NULL, 1),
		(v_upcoming_support_benefits_answer_id, v_support_benefits_option_id, v_upcoming_support_response_id, v_patient_two_id, NULL, 2),
		(v_upcoming_follow_up_answer_id, v_follow_up_email_option_id, v_upcoming_follow_up_response_id, v_patient_two_id, NULL, 1),
		(v_upcoming_context_answer_id, v_context_answer_option_id, v_upcoming_context_response_id, v_patient_two_id, v_upcoming_context_text, 1)
	ON CONFLICT (screening_answer_id) DO UPDATE
	SET screening_answer_option_id=EXCLUDED.screening_answer_option_id,
		screening_session_answered_screening_question_id=EXCLUDED.screening_session_answered_screening_question_id,
		created_by_account_id=EXCLUDED.created_by_account_id,
		text=EXCLUDED.text,
		answer_order=EXCLUDED.answer_order,
		valid=TRUE;

	INSERT INTO appointment (
		appointment_id,
		provider_id,
		account_id,
		created_by_account_id,
		first_name,
		last_name,
		email_address,
		contact_phone_number,
		appointment_type_id,
		title,
		start_time,
		end_time,
		duration_in_minutes,
		time_zone,
		videoconference_url,
		videoconference_platform_id,
		scheduling_system_id,
		appointment_reason_id,
		attendance_status_id,
		screening_session_id,
		canceled,
		canceled_at,
		canceled_by_account_id
	) VALUES
		(v_completed_appointment_id, v_provider_id, v_patient_one_id, v_patient_one_id, 'Alex', 'Morgan', 'care-encounter.alex@example.com', '+12155553001', v_appointment_type_id, 'Care Navigation Consultation', v_today - INTERVAL '1 day' + INTERVAL '10 hours', v_today - INTERVAL '1 day' + INTERVAL '10 hours 30 minutes', 30, 'America/New_York', 'https://fixtures.cobalt.care/care-encounters/completed', 'SWITCHBOARD', 'COBALT', v_appointment_reason_id, 'ATTENDED', NULL, FALSE, NULL, NULL),
		(v_upcoming_appointment_id, v_provider_id, v_patient_two_id, v_patient_two_id, 'Jordan', 'Lee', 'care-encounter.jordan@example.com', '+12155553002', v_appointment_type_id, 'Care Navigation Consultation', v_today + INTERVAL '1 day 11 hours', v_today + INTERVAL '1 day 11 hours 30 minutes', 30, 'America/New_York', 'https://fixtures.cobalt.care/care-encounters/upcoming', 'SWITCHBOARD', 'COBALT', v_appointment_reason_id, 'UNKNOWN', v_upcoming_screening_session_id, FALSE, NULL, NULL),
		(v_canceled_appointment_id, v_provider_id, v_patient_three_id, v_patient_three_id, 'Taylor', 'Rivera', 'care-encounter.taylor@example.com', '+12155553003', v_appointment_type_id, 'Care Navigation Consultation', v_today + INTERVAL '2 days 14 hours', v_today + INTERVAL '2 days 14 hours 30 minutes', 30, 'America/New_York', 'https://fixtures.cobalt.care/care-encounters/canceled', 'SWITCHBOARD', 'COBALT', v_appointment_reason_id, 'CANCELED', NULL, TRUE, NOW(), v_navigator_account_id),
		(v_rebooked_appointment_id, v_provider_id, v_patient_three_id, v_patient_three_id, 'Taylor', 'Rivera', 'care-encounter.taylor@example.com', '+12155553003', v_appointment_type_id, 'Care Navigation Consultation', v_today + INTERVAL '4 days 14 hours', v_today + INTERVAL '4 days 14 hours 30 minutes', 30, 'America/New_York', 'https://fixtures.cobalt.care/care-encounters/rebooked', 'SWITCHBOARD', 'COBALT', v_appointment_reason_id, 'UNKNOWN', NULL, FALSE, NULL, NULL),
		(v_patient_canceled_appointment_id, v_provider_id, v_patient_four_id, v_patient_four_id, 'Casey', 'Nguyen', 'care-encounter.casey@example.com', '+12155553004', v_appointment_type_id, 'Care Navigation Consultation', v_today + INTERVAL '3 days 9 hours', v_today + INTERVAL '3 days 9 hours 30 minutes', 30, 'America/New_York', 'https://fixtures.cobalt.care/care-encounters/patient-canceled', 'SWITCHBOARD', 'COBALT', v_appointment_reason_id, 'CANCELED', NULL, TRUE, NOW(), v_patient_four_id)
	ON CONFLICT (appointment_id) DO UPDATE
	SET provider_id=EXCLUDED.provider_id,
		account_id=EXCLUDED.account_id,
		created_by_account_id=EXCLUDED.created_by_account_id,
		first_name=EXCLUDED.first_name,
		last_name=EXCLUDED.last_name,
		email_address=EXCLUDED.email_address,
		contact_phone_number=EXCLUDED.contact_phone_number,
		appointment_type_id=EXCLUDED.appointment_type_id,
		title=EXCLUDED.title,
		start_time=EXCLUDED.start_time,
		end_time=EXCLUDED.end_time,
		duration_in_minutes=EXCLUDED.duration_in_minutes,
		time_zone=EXCLUDED.time_zone,
		videoconference_url=EXCLUDED.videoconference_url,
		videoconference_platform_id=EXCLUDED.videoconference_platform_id,
		scheduling_system_id=EXCLUDED.scheduling_system_id,
		appointment_reason_id=EXCLUDED.appointment_reason_id,
		attendance_status_id=EXCLUDED.attendance_status_id,
		screening_session_id=EXCLUDED.screening_session_id,
		canceled=EXCLUDED.canceled,
		canceled_at=EXCLUDED.canceled_at,
		canceled_by_account_id=EXCLUDED.canceled_by_account_id;

	UPDATE care_encounter
	SET notes='Discussed provider preferences; awaiting navigator closure after the completed appointment.',
		care_encounter_status_id='OPEN',
		closed_at=NULL,
		closed_by_account_id=NULL,
		last_updated_by_account_id=v_navigator_account_id
	WHERE care_encounter_id=(SELECT care_encounter_id FROM appointment WHERE appointment_id=v_completed_appointment_id);

	UPDATE care_encounter
	SET notes='Review intake goals before the scheduled encounter.',
		last_updated_by_account_id=v_navigator_account_id
	WHERE care_encounter_id=(SELECT care_encounter_id FROM appointment WHERE appointment_id=v_upcoming_appointment_id);

	UPDATE care_encounter
	SET notes='The Care Navigator canceled the original appointment; the replacement remains in the same encounter.',
		last_updated_by_account_id=v_navigator_account_id
	WHERE care_encounter_id=(SELECT care_encounter_id FROM appointment WHERE appointment_id=v_rebooked_appointment_id);

	UPDATE care_encounter
	SET notes='The patient canceled their appointment.',
		care_encounter_status_id='CLOSED',
		closed_at=COALESCE(closed_at, NOW()),
		closed_by_account_id=v_patient_four_id,
		last_updated_by_account_id=v_patient_four_id
	WHERE care_encounter_id=(SELECT care_encounter_id FROM appointment WHERE appointment_id=v_patient_canceled_appointment_id);
END $$;

COMMIT;
