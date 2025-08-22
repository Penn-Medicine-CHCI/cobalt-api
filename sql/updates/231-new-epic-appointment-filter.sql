BEGIN;
SELECT _v.register_patch('231-new-epic-appointment-filter', NULL, NULL);

INSERT INTO epic_appointment_filter (epic_appointment_filter_id, description) VALUES ('MANUAL_VISIT_TYPE', 'Manual Visit Type');

END;