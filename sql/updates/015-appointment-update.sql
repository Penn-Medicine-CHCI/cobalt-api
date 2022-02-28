BEGIN;
SELECT _v.register_patch('015-appointment-update', NULL, NULL);

INSERT INTO audit_log_event VALUES ('APPOINTMENT_UPDATE', 'Update an appointment');

ALTER TABLE appointment ADD COLUMN canceled_for_reschedule BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE appointment ADD COLUMN rescheduled_appointment_id UUID NULL;

END;