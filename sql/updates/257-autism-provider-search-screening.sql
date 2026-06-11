BEGIN;
SELECT _v.register_patch('257-autism-provider-search-screening', NULL, NULL);

-- The Penn Autism Clinic referrer owns a fullscreen screening flow that routes
-- eligible users to a clinic-specific booking page. Reuse that flow for booking
-- requirements on the concrete appointment types shown in provider search.
WITH autism_referrer AS (
  SELECT intake_screening_flow_id
  FROM institution_referrer
  WHERE from_institution_id = 'COBALT'
    AND url_name = 'autism-clinic'
),
autism_appointment_type AS (
  SELECT appointment_type_id
  FROM appointment_type
  WHERE scheduling_system_id = 'COBALT'
    AND name IN ('Autism Clinic Intake Call', 'Autism Clinic Consult Call')
)
UPDATE appointment_type
SET screening_flow_id = autism_referrer.intake_screening_flow_id
FROM autism_referrer, autism_appointment_type
WHERE appointment_type.appointment_type_id = autism_appointment_type.appointment_type_id
  AND autism_referrer.intake_screening_flow_id IS NOT NULL;

UPDATE clinic
SET appointment_booking_level_id = 'CLINIC'
WHERE institution_id = 'COBALT'
  AND description = 'Penn Autism Clinic';

COMMIT;
