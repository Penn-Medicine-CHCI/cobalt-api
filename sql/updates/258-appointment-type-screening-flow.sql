BEGIN;
SELECT _v.register_patch('258-appointment-type-screening-flow', NULL, NULL);

ALTER TABLE appointment_type
ADD COLUMN screening_flow_id UUID REFERENCES screening_flow(screening_flow_id);

CREATE OR REPLACE VIEW v_appointment_type AS
SELECT
	app_type.appointment_type_id,
	app_type.acuity_appointment_type_id,
	app_type.name,
	app_type.description,
	app_type.duration_in_minutes,
	app_type.deleted,
	app_type.created,
	app_type.last_updated,
	app_type.scheduling_system_id,
	app_type.epic_visit_type_id,
	app_type.epic_visit_type_id_type,
	app_type.visit_type_id,
	app_type.hex_color,
	ata.assessment_id,
	app_type.epic_visit_type_system,
	app_type.screening_flow_id
FROM appointment_type app_type
LEFT OUTER JOIN appointment_type_assessment ata
	ON app_type.appointment_type_id = ata.appointment_type_id
	AND ata.active = TRUE
WHERE app_type.deleted = FALSE;

COMMIT;
