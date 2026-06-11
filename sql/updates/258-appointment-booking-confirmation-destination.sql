BEGIN;
SELECT _v.register_patch('258-appointment-booking-confirmation-destination', NULL, NULL);

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

COMMIT;
