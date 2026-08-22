BEGIN;
SELECT _v.register_patch(
	'259-cobalt-provider-booking-configuration',
	ARRAY[
		'250-autism-clinic-referrer',
		'259-provider-booking-database'
	],
	NULL
);

-- This patch prepares Cobalt-specific configuration but deliberately leaves
-- booking v2 disabled. Production rollout is controlled separately by setting
-- institution.booking_v2_enabled only when the tenant is ready to opt in.

-- The Penn Autism Clinic referrer owns a fullscreen screening flow that routes
-- eligible users to a clinic-specific booking page. Reuse that flow for booking
-- requirements on the concrete appointment types shown in provider search.
WITH autism_referrer AS (
	SELECT intake_screening_flow_id
	FROM institution_referrer
	WHERE from_institution_id='COBALT'
	AND url_name='autism-clinic'
),
autism_appointment_type AS (
	SELECT DISTINCT appointment_type.appointment_type_id
	FROM appointment_type
	JOIN provider_appointment_type
		ON provider_appointment_type.appointment_type_id=appointment_type.appointment_type_id
	JOIN provider
		ON provider.provider_id=provider_appointment_type.provider_id
	WHERE provider.institution_id='COBALT'
	AND appointment_type.scheduling_system_id='COBALT'
	AND appointment_type.name IN ('Autism Clinic Intake Call', 'Autism Clinic Consult Call')
)
UPDATE appointment_type
SET screening_flow_id=autism_referrer.intake_screening_flow_id
FROM autism_referrer, autism_appointment_type
WHERE appointment_type.appointment_type_id=autism_appointment_type.appointment_type_id
AND autism_referrer.intake_screening_flow_id IS NOT NULL;

-- Appointment-booking eligibility is satisfied only by an explicit successful
-- screening result. Preserve the Autism Clinic's referral destinations while
-- marking its eligible and ineligible branches accordingly.
UPDATE screening_flow_version sfv
SET destination_function = REPLACE(
	REPLACE(
		sfv.destination_function,
		$eligible$output.screeningSessionDestinationId = 'INSTITUTION_REFERRAL';$eligible$,
		$eligible$output.screeningSessionDestinationId = 'INSTITUTION_REFERRAL';
    output.context.result = 'SUCCESS';$eligible$
	),
	$ineligible$output.screeningSessionDestinationId = 'INSTITUTION_REFERRER_DETAIL';$ineligible$,
	$ineligible$output.screeningSessionDestinationId = 'INSTITUTION_REFERRER_DETAIL';
    output.context.result = 'FAILURE';$ineligible$
)
FROM screening_flow sf
WHERE sfv.screening_flow_id=sf.screening_flow_id
AND sf.institution_id='COBALT'
AND sf.screening_flow_id=(
	SELECT intake_screening_flow_id
	FROM institution_referrer
	WHERE from_institution_id='COBALT'
	AND url_name='autism-clinic'
)
AND sfv.destination_function NOT LIKE '%output.context.result%';

UPDATE clinic
SET appointment_booking_level_id='CLINIC'
WHERE institution_id='COBALT'
AND description='Penn Autism Clinic';

-- Finalize appointment-linked screening destinations after tenant booking
-- levels and flow associations are configured.
UPDATE screening_flow_version sfv
SET destination_function = REPLACE(
	REPLACE(sfv.destination_function,
		'''PROVIDER_APPOINTMENT_BOOKING''',
		'''APPOINTMENT_BOOKING_CONFIRMATION'''),
	'"PROVIDER_APPOINTMENT_BOOKING"',
	'"APPOINTMENT_BOOKING_CONFIRMATION"')
FROM screening_flow sf
WHERE sfv.screening_flow_id=sf.screening_flow_id
AND sf.institution_id='COBALT'
AND EXISTS (
	SELECT 1
	FROM appointment_type
	WHERE appointment_type.screening_flow_id=sf.screening_flow_id
)
AND sfv.destination_function LIKE '%PROVIDER_APPOINTMENT_BOOKING%';

-- Preserve each referrer's legacy booking.path and write the v2-specific route
-- alongside it. The web application selects v2Path only while the tenant flag
-- is enabled, so disabling booking v2 immediately restores the legacy route.
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
	FROM institution_referrer ir
	LEFT OUTER JOIN institution_feature_institution_referrer ifir
		ON ifir.institution_referrer_id=ir.institution_referrer_id
	LEFT OUTER JOIN institution_feature_referrer_location ifrl
		ON ifrl.institution_feature_institution_referrer_id=ifir.institution_feature_institution_referrer_id
	WHERE ir.from_institution_id='COBALT'
	GROUP BY ir.institution_referrer_id
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
							'{booking,v2Path}',
							TO_JSONB(FORMAT('/provider-confirm-appointment-time?providerSearchResultTypeId=%s&%s=%s&appointmentTypeId=%s&institutionLocationId=%s&featureId=%s',
								pg_temp.url_encode(booking_route.provider_search_result_type_id),
								CASE
									WHEN booking_route.provider_search_result_type_id='PROVIDER' THEN 'providerId'
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
	FROM institution_referrer ir
	LEFT OUTER JOIN referrer_location
		ON referrer_location.institution_referrer_id=ir.institution_referrer_id
	CROSS JOIN LATERAL JSONB_EACH(
		CASE
			WHEN JSONB_TYPEOF(ir.metadata->'resultScreens')='object' THEN ir.metadata->'resultScreens'
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
				WHEN JSONB_TYPEOF(booking_context.booking->'clinicIds')='array'
						AND JSONB_ARRAY_LENGTH(booking_context.booking->'clinicIds')=1 THEN
					NULLIF(BTRIM(booking_context.booking->'clinicIds'->>0), '')
				ELSE NULL
			END AS clinic_id,
			CASE
				WHEN NULLIF(BTRIM(booking_context.booking->>'appointmentTypeId'), '') IS NOT NULL THEN
					NULLIF(BTRIM(booking_context.booking->>'appointmentTypeId'), '')
				WHEN JSONB_TYPEOF(booking_context.booking->'appointmentTypeIds')='array'
						AND JSONB_ARRAY_LENGTH(booking_context.booking->'appointmentTypeIds')=1 THEN
					NULLIF(BTRIM(booking_context.booking->'appointmentTypeIds'->>0), '')
				ELSE NULL
			END AS appointment_type_id,
			NULLIF(BTRIM(booking_context.booking->>'featureId'), '') AS feature_id,
			COALESCE(NULLIF(BTRIM(booking_context.booking->>'institutionLocationId'), ''), referrer_location.institution_location_id, 'na') AS institution_location_id
	) booking_parameter
	LEFT OUTER JOIN clinic booking_clinic
		ON booking_parameter.clinic_id ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
		AND booking_clinic.clinic_id=booking_parameter.clinic_id::UUID
	CROSS JOIN LATERAL (
		SELECT COALESCE(
			booking_parameter.provider_search_result_type_id,
			CASE
				WHEN booking_parameter.clinic_id IS NOT NULL
						AND booking_clinic.appointment_booking_level_id='CLINIC' THEN 'CLINIC'
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
				WHEN booking_type.provider_search_result_type_id='PROVIDER' THEN booking_parameter.provider_id
				WHEN booking_type.provider_search_result_type_id='CLINIC' THEN booking_parameter.clinic_id
				ELSE NULL
			END AS provider_search_result_id,
			booking_parameter.appointment_type_id,
			booking_parameter.institution_location_id,
			booking_parameter.feature_id
	) booking_route
	WHERE ir.from_institution_id='COBALT'
	GROUP BY ir.institution_referrer_id
)
UPDATE institution_referrer ir
SET metadata=JSONB_SET(ir.metadata, '{resultScreens}', updated_result_screens.result_screens, FALSE)
FROM updated_result_screens
WHERE ir.institution_referrer_id=updated_result_screens.institution_referrer_id
AND ir.from_institution_id='COBALT'
AND ir.metadata->'resultScreens' IS DISTINCT FROM updated_result_screens.result_screens;

COMMIT;
