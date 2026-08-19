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
	v_patient_one_id CONSTANT UUID := 'ca4e1000-0000-4000-8000-000000000001';
	v_patient_two_id CONSTANT UUID := 'ca4e1000-0000-4000-8000-000000000002';
	v_patient_three_id CONSTANT UUID := 'ca4e1000-0000-4000-8000-000000000003';
	v_completed_appointment_id CONSTANT UUID := 'ca4e2000-0000-4000-8000-000000000001';
	v_upcoming_appointment_id CONSTANT UUID := 'ca4e2000-0000-4000-8000-000000000002';
	v_canceled_appointment_id CONSTANT UUID := 'ca4e2000-0000-4000-8000-000000000003';
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
		(v_patient_three_id, 'PATIENT', v_institution_id, 'EMAIL_PASSWORD', 'care-encounter.taylor@example.com', 'Taylor', 'Rivera', 'Taylor Rivera', 'en-US', 'America/New_York', TRUE, TRUE)
	ON CONFLICT (account_id) DO UPDATE
	SET first_name=EXCLUDED.first_name,
		last_name=EXCLUDED.last_name,
		display_name=EXCLUDED.display_name,
		active=EXCLUDED.active,
		test_account=EXCLUDED.test_account;

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
		canceled,
		canceled_at
	) VALUES
		(v_completed_appointment_id, v_provider_id, v_patient_one_id, v_patient_one_id, 'Alex', 'Morgan', 'care-encounter.alex@example.com', '+12155553001', v_appointment_type_id, 'Care Navigation Consultation', v_today - INTERVAL '1 day' + INTERVAL '10 hours', v_today - INTERVAL '1 day' + INTERVAL '10 hours 30 minutes', 30, 'America/New_York', 'https://fixtures.cobalt.care/care-encounters/completed', 'SWITCHBOARD', 'COBALT', v_appointment_reason_id, 'ATTENDED', FALSE, NULL),
		(v_upcoming_appointment_id, v_provider_id, v_patient_two_id, v_patient_two_id, 'Jordan', 'Lee', 'care-encounter.jordan@example.com', '+12155553002', v_appointment_type_id, 'Care Navigation Consultation', v_today + INTERVAL '1 day 11 hours', v_today + INTERVAL '1 day 11 hours 30 minutes', 30, 'America/New_York', 'https://fixtures.cobalt.care/care-encounters/upcoming', 'SWITCHBOARD', 'COBALT', v_appointment_reason_id, 'UNKNOWN', FALSE, NULL),
		(v_canceled_appointment_id, v_provider_id, v_patient_three_id, v_patient_three_id, 'Taylor', 'Rivera', 'care-encounter.taylor@example.com', '+12155553003', v_appointment_type_id, 'Care Navigation Consultation', v_today + INTERVAL '2 days 14 hours', v_today + INTERVAL '2 days 14 hours 30 minutes', 30, 'America/New_York', 'https://fixtures.cobalt.care/care-encounters/canceled', 'SWITCHBOARD', 'COBALT', v_appointment_reason_id, 'CANCELED', TRUE, NOW())
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
		canceled=EXCLUDED.canceled,
		canceled_at=EXCLUDED.canceled_at;

	UPDATE care_encounter
	SET notes=CASE appointment_id
			WHEN v_completed_appointment_id THEN 'Discussed provider preferences and shared next-step options.'
			WHEN v_upcoming_appointment_id THEN 'Review intake goals before the scheduled encounter.'
			WHEN v_canceled_appointment_id THEN 'Appointment canceled by the Care Navigator.'
		END,
		care_encounter_status_id=CASE appointment_id
			WHEN v_completed_appointment_id THEN 'CLOSED'
			WHEN v_upcoming_appointment_id THEN 'OPEN'
			WHEN v_canceled_appointment_id THEN 'CANCELED'
		END,
		closed_at=CASE
			WHEN appointment_id=v_upcoming_appointment_id THEN NULL
			ELSE COALESCE(closed_at, NOW())
		END,
		canceled_by_account_id=CASE
			WHEN appointment_id=v_canceled_appointment_id THEN v_navigator_account_id
			ELSE NULL
		END,
		deleted=FALSE,
		last_updated_by_account_id=v_navigator_account_id
	WHERE appointment_id IN (
		v_completed_appointment_id,
		v_upcoming_appointment_id,
		v_canceled_appointment_id
	);
END $$;

COMMIT;
