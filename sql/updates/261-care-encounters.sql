BEGIN;
SELECT _v.register_patch('261-care-encounters', ARRAY['260-care-navigator'], NULL);

-- A provider may publish a contact phone number without offering phone
-- appointments. Care Navigators use this flag because every encounter is
-- delivered online.
ALTER TABLE provider
ADD COLUMN IF NOT EXISTS virtual_appointments_only BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE provider
SET virtual_appointments_only=TRUE
WHERE EXISTS (
	SELECT 1
	FROM provider_support_role
	WHERE provider_support_role.provider_id=provider.provider_id
	AND provider_support_role.support_role_id='CARE_NAVIGATOR'
);

-- A Care Encounter is the navigator-owned administrative record for an
-- appointment. Appointment data remains the source of truth for scheduling,
-- attendance, and patient contact details. CANCELED is reserved for an action
-- taken by the Care Navigator so it remains distinguishable from other closed
-- appointment outcomes.
CREATE TABLE care_encounter_status (
	care_encounter_status_id TEXT PRIMARY KEY,
	description TEXT NOT NULL,
	terminal BOOLEAN NOT NULL
);

INSERT INTO care_encounter_status VALUES ('OPEN', 'Open', FALSE);
INSERT INTO care_encounter_status VALUES ('CLOSED', 'Closed', TRUE);
INSERT INTO care_encounter_status VALUES ('CANCELED', 'Canceled by Care Navigator', TRUE);

CREATE TABLE care_encounter_cancellation_reason (
	care_encounter_cancellation_reason_id TEXT PRIMARY KEY,
	description TEXT NOT NULL,
	display_order INTEGER NOT NULL,
	freeform_text_required BOOLEAN NOT NULL DEFAULT FALSE
);

INSERT INTO care_encounter_cancellation_reason VALUES
	('PATIENT_REQUESTED', 'Patient requested cancellation', 1, FALSE),
	('NO_LONGER_NEEDED', 'Care navigation is no longer needed', 2, FALSE),
	('UNABLE_TO_REACH_PATIENT', 'Unable to reach patient', 3, FALSE),
	('SCHEDULING_CONFLICT', 'Scheduling conflict', 4, FALSE),
	('DUPLICATE_BOOKING', 'Duplicate booking', 5, FALSE),
	('OTHER', 'Other', 6, TRUE);

CREATE TABLE care_encounter (
	care_encounter_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	appointment_id UUID NOT NULL UNIQUE REFERENCES appointment(appointment_id),
	account_id UUID NOT NULL REFERENCES account(account_id),
	care_encounter_status_id TEXT NOT NULL REFERENCES care_encounter_status(care_encounter_status_id) DEFAULT 'OPEN',
	notes TEXT,
	closed_at TIMESTAMPTZ,
	canceled_by_account_id UUID REFERENCES account(account_id),
	care_encounter_cancellation_reason_id TEXT REFERENCES care_encounter_cancellation_reason(care_encounter_cancellation_reason_id),
	care_encounter_cancellation_reason_other_text TEXT,
	deleted BOOLEAN NOT NULL DEFAULT FALSE,
	created_by_account_id UUID NOT NULL REFERENCES account(account_id),
	last_updated_by_account_id UUID NOT NULL REFERENCES account(account_id),
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	CONSTRAINT care_encounter_cancellation_reason_required_check CHECK (
		care_encounter_status_id<>'CANCELED'
		OR care_encounter_cancellation_reason_id IS NOT NULL
	),
	CONSTRAINT care_encounter_cancellation_reason_other_text_check CHECK (
		(care_encounter_cancellation_reason_id='OTHER'
			AND NULLIF(BTRIM(care_encounter_cancellation_reason_other_text), '') IS NOT NULL)
		OR (care_encounter_cancellation_reason_id IS DISTINCT FROM 'OTHER'
			AND care_encounter_cancellation_reason_other_text IS NULL)
	)
);

CREATE TRIGGER set_last_updated
BEFORE INSERT OR UPDATE ON care_encounter
FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

-- A patient may have only one active Care Navigator relationship at a time,
-- regardless of which Care Navigator owns the appointment. Keeping the
-- account ID on the encounter permits PostgreSQL to enforce this under
-- concurrent and non-HTTP booking/import paths.
CREATE UNIQUE INDEX care_encounter_one_open_per_account_idx
ON care_encounter(account_id)
WHERE care_encounter_status_id='OPEN';

-- Ensure providers assigned the Care Navigator support role after this patch
-- receive the same modality restriction and have their existing appointments
-- backfilled automatically.
CREATE OR REPLACE FUNCTION apply_care_navigator_provider_defaults()
RETURNS TRIGGER AS $$
BEGIN
	IF NEW.support_role_id='CARE_NAVIGATOR' THEN
		UPDATE provider
		SET virtual_appointments_only=TRUE
		WHERE provider_id=NEW.provider_id;

		INSERT INTO care_encounter (
			appointment_id,
			account_id,
			care_encounter_status_id,
			closed_at,
			created_by_account_id,
			last_updated_by_account_id,
			created,
			last_updated
		)
		SELECT
			appointment.appointment_id,
			appointment.account_id,
			CASE
				WHEN appointment.canceled=TRUE OR appointment.attendance_status_id IN ('ATTENDED', 'MISSED', 'CANCELED') THEN 'CLOSED'
				ELSE 'OPEN'
			END,
			CASE
				WHEN appointment.canceled=TRUE OR appointment.attendance_status_id IN ('ATTENDED', 'MISSED', 'CANCELED')
					THEN COALESCE(appointment.canceled_at, appointment.last_updated)
				ELSE NULL
			END,
			appointment.created_by_account_id,
			appointment.created_by_account_id,
			appointment.created,
			appointment.last_updated
		FROM appointment
		WHERE appointment.provider_id=NEW.provider_id
		ON CONFLICT (appointment_id) DO NOTHING;
	END IF;

	RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS apply_care_navigator_provider_defaults ON provider_support_role;
CREATE TRIGGER apply_care_navigator_provider_defaults
AFTER INSERT OR UPDATE OF support_role_id ON provider_support_role
FOR EACH ROW EXECUTE PROCEDURE apply_care_navigator_provider_defaults();

CREATE INDEX IF NOT EXISTS appointment_provider_start_time_idx
ON appointment(provider_id, start_time DESC);

-- Create the administrative record in the same transaction as every Care
-- Navigator appointment, including appointments created by sync/import paths.
CREATE OR REPLACE FUNCTION create_care_encounter_for_appointment()
RETURNS TRIGGER AS $$
BEGIN
	IF NEW.provider_id IS NOT NULL AND EXISTS (
		SELECT 1
		FROM provider_support_role
		WHERE provider_support_role.provider_id=NEW.provider_id
		AND provider_support_role.support_role_id='CARE_NAVIGATOR'
	) THEN
		INSERT INTO care_encounter (
			appointment_id,
			account_id,
			care_encounter_status_id,
			closed_at,
			created_by_account_id,
			last_updated_by_account_id
		) VALUES (
			NEW.appointment_id,
			NEW.account_id,
			CASE
				WHEN NEW.canceled=TRUE OR NEW.attendance_status_id IN ('ATTENDED', 'MISSED', 'CANCELED') THEN 'CLOSED'
				ELSE 'OPEN'
			END,
			CASE
				WHEN NEW.canceled=TRUE OR NEW.attendance_status_id IN ('ATTENDED', 'MISSED', 'CANCELED')
					THEN COALESCE(NEW.canceled_at, NOW())
				ELSE NULL
			END,
			NEW.created_by_account_id,
			NEW.created_by_account_id
		)
		ON CONFLICT (appointment_id) DO UPDATE
		SET account_id=EXCLUDED.account_id;
	END IF;

	RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS create_care_encounter_for_appointment ON appointment;
CREATE TRIGGER create_care_encounter_for_appointment
AFTER INSERT OR UPDATE OF provider_id, account_id ON appointment
FOR EACH ROW EXECUTE PROCEDURE create_care_encounter_for_appointment();

-- An appointment with a terminal attendance outcome closes its encounter.
-- This transition never overwrites CANCELED, which is set explicitly by the
-- Care Navigator cancellation operation.
CREATE OR REPLACE FUNCTION close_care_encounter_for_terminal_appointment()
RETURNS TRIGGER AS $$
BEGIN
	IF NEW.canceled=TRUE OR NEW.attendance_status_id IN ('ATTENDED', 'MISSED', 'CANCELED') THEN
		UPDATE care_encounter
		SET care_encounter_status_id='CLOSED',
			closed_at=COALESCE(closed_at, NEW.canceled_at, NOW())
		WHERE appointment_id=NEW.appointment_id
		AND care_encounter_status_id='OPEN';
	END IF;

	RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS close_care_encounter_for_terminal_appointment ON appointment;
CREATE TRIGGER close_care_encounter_for_terminal_appointment
AFTER INSERT OR UPDATE OF attendance_status_id, canceled ON appointment
FOR EACH ROW EXECUTE PROCEDURE close_care_encounter_for_terminal_appointment();

-- Backfill appointments that predate the Care Encounter data structure.
INSERT INTO care_encounter (
	appointment_id,
	account_id,
	care_encounter_status_id,
	closed_at,
	created_by_account_id,
	last_updated_by_account_id,
	created,
	last_updated
)
SELECT
	appointment.appointment_id,
	appointment.account_id,
	CASE
		WHEN appointment.canceled=TRUE OR appointment.attendance_status_id IN ('ATTENDED', 'MISSED', 'CANCELED') THEN 'CLOSED'
		ELSE 'OPEN'
	END,
	CASE
		WHEN appointment.canceled=TRUE OR appointment.attendance_status_id IN ('ATTENDED', 'MISSED', 'CANCELED')
			THEN COALESCE(appointment.canceled_at, appointment.last_updated)
		ELSE NULL
	END,
	appointment.created_by_account_id,
	appointment.created_by_account_id,
	appointment.created,
	appointment.last_updated
FROM appointment
JOIN provider_support_role
	ON provider_support_role.provider_id=appointment.provider_id
	AND provider_support_role.support_role_id='CARE_NAVIGATOR'
ON CONFLICT (appointment_id) DO NOTHING;

COMMIT;
