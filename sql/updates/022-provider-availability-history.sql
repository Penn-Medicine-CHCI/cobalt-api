BEGIN;
SELECT _v.register_patch('022-provider-availability-history', NULL, NULL);

CREATE TABLE provider_availability_history (
	provider_availability_history_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	provider_id UUID NOT NULL REFERENCES provider,
	scheduling_system_id TEXT NOT NULL REFERENCES scheduling_system,
	name TEXT NOT NULL,
	slot_date_time timestamp NOT NULL,
	time_zone text NOT NULL, -- e.g. 'America/New_York'
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	UNIQUE (provider_id, slot_date_time, time_zone)
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON provider_availability_history FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE provider_availability_appointment_type_history (
	provider_availability_appointment_type_history_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	provider_availability_history_id UUID NOT NULL REFERENCES provider_availability_history,
	appointment_type_id UUID NOT NULL REFERENCES appointment_type,
	visit_type_id TEXT NOT NULL REFERENCES visit_type,
	name TEXT NOT NULL,
	duration_in_minutes INTEGER NOT NULL,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	UNIQUE (provider_availability_history_id, appointment_type_id)
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON provider_availability_appointment_type_history FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

COMMIT;