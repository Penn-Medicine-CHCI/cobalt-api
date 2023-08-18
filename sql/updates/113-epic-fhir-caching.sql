BEGIN;
SELECT _v.register_patch('113-epic-fhir-caching', NULL, NULL);

-- Caches responses for Appointment $find (STU3).
--
-- Note: assumes data is not parameterized in any way other than specifying a date,
-- IOW _all_ provider slots are included for the date, not filtered on specialty, visit type, ...
-- This is due to difficulty configuring rules in Cadence - simpler for institutions to just provide
-- a firehose of all data for a date and we filter by provider/support role/etc. on our end.
--
-- See https://fhir.epic.com/Specifications?api=840
CREATE TABLE epic_fhir_appointment_find_cache (
  institution_id TEXT NOT NULL REFERENCES institution,
  date DATE NOT NULL,
  api_response TEXT NOT NULL,
  last_updated timestamptz NOT NULL DEFAULT NOW()
  PRIMARY KEY(institution_id, date)
);

ALTER TABLE institution ADD COLUMN epic_fhir_appointment_find_cache_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE institution ADD COLUMN epic_fhir_appointment_find_cache_expiration_in_seconds INTEGER NOT NULL DEFAULT 60;

COMMIT;