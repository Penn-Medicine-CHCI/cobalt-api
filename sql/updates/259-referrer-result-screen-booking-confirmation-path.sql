BEGIN;
SELECT _v.register_patch('259-referrer-result-screen-booking-confirmation-path', NULL, NULL);

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

WITH updated_result_screens AS (
	SELECT
		ir.institution_referrer_id,
		JSONB_OBJECT_AGG(
			result_screen.key,
			CASE
				WHEN booking_parameter.provider_id IS NOT NULL
						AND booking_parameter.clinic_id IS NOT NULL
						AND booking_parameter.appointment_type_id IS NOT NULL THEN
					JSONB_SET(
						result_screen.value,
						'{booking,path}',
						TO_JSONB(FORMAT('/provider-confirm-appointment-time?providerId=%s&clinicId=%s&appointmentTypeId=%s',
							pg_temp.url_encode(booking_parameter.provider_id),
							pg_temp.url_encode(booking_parameter.clinic_id),
							pg_temp.url_encode(booking_parameter.appointment_type_id))),
						TRUE
					)
				ELSE result_screen.value
			END
		) AS result_screens
	FROM
		institution_referrer ir
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
			END AS appointment_type_id
	) booking_parameter
	GROUP BY
		ir.institution_referrer_id
)
UPDATE institution_referrer ir
SET metadata = JSONB_SET(ir.metadata, '{resultScreens}', updated_result_screens.result_screens, FALSE)
FROM updated_result_screens
WHERE ir.institution_referrer_id = updated_result_screens.institution_referrer_id
AND ir.metadata->'resultScreens' IS DISTINCT FROM updated_result_screens.result_screens;

COMMIT;
