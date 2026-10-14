-- Epic appointment start alignment
--
-- An Epic provider's appointment-type offering can optionally configure
-- provider_appointment_type.scheduling_start_time_interval_in_minutes. NULL
-- preserves the start times returned by Epic; 60 permits only whole-hour starts
-- in the provider's local time zone. Configured intervals must be between 1 and
-- 1440 minutes and are anchored to local midnight, independently of the first
-- available slot or the appointment's duration.
--
-- Apply migration 290-provider-appointment-start-time-alignment before enabling
-- this setting. The migration requires a unique provider/appointment-type pair;
-- existing duplicate mappings must be resolved before it can succeed. Configure
-- only the intended association and resync the provider's future Epic availability
-- to replace cached starts. Booking and rescheduling also validate the current
-- setting, so stale clients cannot book a disallowed start.
--
-- Alignment filters Epic's offered starts and preserves held/unavailable ranges.
-- It does not establish full-duration availability or change the duration booked
-- in Epic. Native, Acuity, and Epic FHIR scheduling do not use this setting.
--
-- The migration tests start a disposable PostgreSQL cluster using initdb and
-- pg_ctl from PATH, or the directory supplied by
-- -Dcobalt.test.postgresBin=/path/to/bin. Those tests skip when the binaries are
-- unavailable; the database-backed booking/sync tests use the regular local test
-- services (Localstack, PostgreSQL, and Redis).

BEGIN;
SELECT _v.register_patch('290-provider-appointment-start-time-alignment', NULL, NULL);

-- Each provider/appointment-type pair has one start-time policy. Existing
-- duplicate mappings must be resolved before applying this patch; do not
-- silently discard potentially different configuration or display order.
ALTER TABLE provider_appointment_type
	ADD CONSTRAINT provider_appointment_type_provider_type_unique
		UNIQUE (provider_id, appointment_type_id);

ALTER TABLE provider_appointment_type
	ADD COLUMN scheduling_start_time_interval_in_minutes INTEGER,
	ADD CONSTRAINT provider_appointment_type_scheduling_start_time_interval_check
		CHECK (scheduling_start_time_interval_in_minutes BETWEEN 1 AND 1440);

COMMENT ON COLUMN provider_appointment_type.scheduling_start_time_interval_in_minutes IS
	'Optional Epic appointment-start alignment in whole minutes from provider-local midnight. NULL leaves returned start times unrestricted; 60 permits whole-hour starts. This does not change appointment duration or establish full-duration availability.';

COMMIT;
