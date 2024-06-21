BEGIN;
SELECT _v.register_patch('177-epic-provider-blocks', NULL, NULL);

-- Set to true to enable Epic provider block syncing.
ALTER TABLE institution ADD COLUMN epic_provider_slot_booking_sync_enabled BOOLEAN NOT NULL DEFAULT FALSE;

-- Contact ID to key on, e.g. 'CSN'
ALTER TABLE institution ADD COLUMN epic_provider_slot_booking_sync_contact_id_type TEXT;

 -- Department ID to key on, e.g. 'INTERNAL'
ALTER TABLE institution ADD COLUMN epic_provider_slot_booking_sync_department_id_type TEXT;

 -- Visit type ID to key on, e.g. 'INTERNAL'
ALTER TABLE institution ADD COLUMN epic_provider_slot_booking_sync_visit_type_id_type TEXT;

-- We likely want to enforce scheduling limits for Epic appointments.
-- For example, don't allow more than 1 NPV per schedule (morning or afternoon), where morning is 8-12 and afternoon is 1-5.
CREATE TABLE epic_provider_schedule (
	epic_provider_schedule_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	institution_id TEXT NOT NULL REFERENCES institution,
	name TEXT NOT NULL,
	start_time DATE NOT NULL,
	end_time DATE NOT NULL,
	maximum_npv_count INTEGER,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON epic_provider_schedule FOR EACH ROW EXECUTE PROCEDURE set_last_updated();
CREATE UNIQUE INDEX epic_provider_schedule_name_unique_idx ON epic_provider_schedule USING btree (institution_id, LOWER(name));

-- Synced-in booked slots from Epic for a given provider.
-- This gives us sufficient information to enforce rules defined in epic_provider_schedule, e.g. "1 NPV max for the morning schedule"
CREATE TABLE epic_provider_slot_booking (
  epic_provider_slot_booking_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  provider_id UUID NOT NULL REFERENCES provider,
  contact_id TEXT NOT NULL, -- Epic's 'appointment ID'
  contact_id_type TEXT NOT NULL, -- e.g. 'CSN'
  department_id TEXT NOT NULL, -- Appointment's Epic department ID
  department_id_type TEXT NOT NULL, -- e.g. 'INTERNAL'
  visit_type_id TEXT NOT NULL, -- Appointment's Epic visit type ID
  visit_type_id_type TEXT NOT NULL, -- e.g. 'INTERNAL'
  start_date_time TIMESTAMP WITHOUT TIME ZONE NOT NULL, -- Understood to be in provider's time zone
  end_date_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,  -- Understood to be in provider's time zone
  created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON epic_provider_slot_booking FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

COMMIT;