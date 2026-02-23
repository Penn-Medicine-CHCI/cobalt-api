BEGIN;
SELECT _v.register_patch('243-appointment-booking-failure', NULL, NULL);

CREATE TABLE appointment_booking_failure (
	appointment_booking_failure_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	account_id UUID,
	created_by_account_id UUID,
	institution_id TEXT,
	provider_id UUID,
	appointment_type_id UUID,
	patient_order_id UUID,
	scheduling_system_id TEXT,
	appointment_date DATE,
	appointment_time TIME,
	failure_type TEXT NOT NULL,
	failure_description TEXT,
	metadata JSONB NOT NULL DEFAULT '{}'::JSONB,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON appointment_booking_failure FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE INDEX appointment_booking_failure_created_desc_idx ON appointment_booking_failure(created DESC);
CREATE INDEX appointment_booking_failure_institution_id_created_desc_idx ON appointment_booking_failure(institution_id, created DESC);
CREATE INDEX appointment_booking_failure_scheduling_system_id_created_desc_idx ON appointment_booking_failure(scheduling_system_id, created DESC);
CREATE INDEX appointment_booking_failure_failure_type_created_desc_idx ON appointment_booking_failure(failure_type, created DESC);

COMMIT;
