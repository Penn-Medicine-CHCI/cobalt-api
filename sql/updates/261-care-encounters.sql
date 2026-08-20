BEGIN;
SELECT _v.register_patch('261-care-encounters', ARRAY['260-care-navigator'], NULL);

-- A provider may publish a contact phone number without offering phone
-- appointments. Care Navigator appointments are always virtual.
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

-- An encounter is the patient-level Care Navigator lifecycle. Appointments
-- point to it so a canceled or missed booking remains part of the history.
CREATE TABLE care_encounter (
	care_encounter_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	account_id UUID NOT NULL REFERENCES account(account_id),
	care_navigator_account_id UUID REFERENCES account(account_id),
	care_encounter_status_id TEXT NOT NULL REFERENCES care_encounter_status(care_encounter_status_id) DEFAULT 'OPEN',
	email_address TEXT,
	notes TEXT,
	closed_at TIMESTAMPTZ,
	closed_by_account_id UUID REFERENCES account(account_id),
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
	),
	CONSTRAINT care_encounter_deleted_terminal_check CHECK (
		deleted=FALSE OR care_encounter_status_id<>'OPEN'
	)
);

CREATE TRIGGER set_last_updated
BEFORE INSERT OR UPDATE ON care_encounter
FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

ALTER TABLE appointment
ADD COLUMN IF NOT EXISTS care_encounter_id UUID REFERENCES care_encounter(care_encounter_id),
ADD COLUMN IF NOT EXISTS canceled_by_account_id UUID REFERENCES account(account_id),
ADD COLUMN IF NOT EXISTS screening_session_id UUID REFERENCES screening_session(screening_session_id);

CREATE UNIQUE INDEX care_encounter_one_open_per_account_idx
ON care_encounter(account_id)
WHERE care_encounter_status_id='OPEN' AND deleted=FALSE;

-- UNKNOWN is the only scheduled/active attendance state. A pending reschedule
-- is excluded so its replacement can be linked in the same transaction.
CREATE UNIQUE INDEX care_encounter_one_active_appointment_idx
ON appointment(care_encounter_id)
WHERE care_encounter_id IS NOT NULL
AND canceled=FALSE
AND canceled_for_reschedule=FALSE
AND attendance_status_id='UNKNOWN';

CREATE INDEX appointment_care_encounter_start_time_idx
ON appointment(care_encounter_id, start_time DESC, appointment_id);

CREATE INDEX IF NOT EXISTS appointment_provider_start_time_idx
ON appointment(provider_id, start_time DESC);

CREATE OR REPLACE FUNCTION care_navigator_account_can_serve_provider(
	p_account_id UUID,
	p_provider_id UUID
)
RETURNS BOOLEAN AS $$
	SELECT EXISTS (
		SELECT 1
		FROM care_navigator_provider_account mapping
		JOIN account ON account.account_id=mapping.account_id
		JOIN provider ON provider.provider_id=mapping.provider_id
		JOIN account_capability
			ON account_capability.account_id=account.account_id
			AND account_capability.account_capability_type_id='NAVIGATOR'
		WHERE mapping.account_id=p_account_id
		AND mapping.provider_id=p_provider_id
		AND account.active=TRUE
		AND account.role_id IN ('ADMINISTRATOR', 'PROVIDER')
		AND provider.active=TRUE
		AND provider.institution_id=account.institution_id
		AND EXISTS (
			SELECT 1
			FROM provider_support_role
			WHERE provider_support_role.provider_id=provider.provider_id
			AND provider_support_role.support_role_id='CARE_NAVIGATOR'
		)
	)
$$ LANGUAGE SQL STABLE;

CREATE OR REPLACE FUNCTION first_care_navigator_account_for_provider(p_provider_id UUID)
RETURNS UUID AS $$
	SELECT mapping.account_id
	FROM care_navigator_provider_account mapping
	WHERE mapping.provider_id=p_provider_id
	AND care_navigator_account_can_serve_provider(mapping.account_id, mapping.provider_id)
	ORDER BY mapping.display_order, mapping.account_id
	LIMIT 1
$$ LANGUAGE SQL STABLE;

-- Attach every Care Navigator appointment, including import/sync inserts, to
-- the patient's open lifecycle or create the lifecycle when none exists.
CREATE OR REPLACE FUNCTION attach_care_navigator_appointment_to_encounter()
RETURNS TRIGGER AS $$
DECLARE
	v_care_encounter_id UUID;
	v_care_navigator_account_id UUID;
	v_care_encounter_status_id TEXT;
	v_new_appointment_is_active BOOLEAN;
BEGIN
	IF NEW.provider_id IS NULL OR NOT EXISTS (
		SELECT 1
		FROM provider_support_role
		WHERE provider_support_role.provider_id=NEW.provider_id
		AND provider_support_role.support_role_id='CARE_NAVIGATOR'
	) THEN
		RETURN NEW;
	END IF;

	PERFORM pg_advisory_xact_lock(hashtextextended(
		FORMAT('care-navigator-appointment|%s', NEW.account_id), 0));

	v_new_appointment_is_active := NEW.canceled=FALSE
		AND NEW.canceled_for_reschedule=FALSE
		AND NEW.attendance_status_id='UNKNOWN';

	IF NEW.care_encounter_id IS NOT NULL THEN
		SELECT care_encounter.care_navigator_account_id,
			care_encounter.care_encounter_status_id
		INTO v_care_navigator_account_id, v_care_encounter_status_id
		FROM care_encounter
		WHERE care_encounter.care_encounter_id=NEW.care_encounter_id
		AND care_encounter.account_id=NEW.account_id
		FOR UPDATE;

		IF NOT FOUND THEN
			RAISE EXCEPTION 'Care Navigator appointment encounter must belong to the appointment account.';
		END IF;

		IF v_care_encounter_status_id<>'OPEN' THEN
			IF TG_OP='INSERT' THEN
				RAISE EXCEPTION 'New Care Navigator appointments cannot be attached to a terminal encounter.';
			ELSIF OLD.care_encounter_id IS DISTINCT FROM NEW.care_encounter_id THEN
				RAISE EXCEPTION 'Care Navigator appointments cannot be moved to a terminal encounter.';
			END IF;
		END IF;

		IF v_new_appointment_is_active AND EXISTS (
			SELECT 1
			FROM appointment
			WHERE appointment.care_encounter_id=NEW.care_encounter_id
			AND appointment.appointment_id<>NEW.appointment_id
			AND appointment.attendance_status_id='ATTENDED'
		) THEN
			RAISE EXCEPTION 'Completed Care Navigator encounter must be closed before another appointment can be booked.';
		END IF;

		IF v_new_appointment_is_active AND EXISTS (
			SELECT 1
			FROM appointment
			WHERE appointment.care_encounter_id=NEW.care_encounter_id
			AND appointment.appointment_id<>NEW.appointment_id
			AND appointment.canceled=FALSE
			AND appointment.canceled_for_reschedule=FALSE
			AND appointment.attendance_status_id='UNKNOWN'
		) THEN
			RAISE EXCEPTION 'Care Navigator encounter already has an active appointment.';
		END IF;

		IF v_care_navigator_account_id IS NULL
			OR NOT care_navigator_account_can_serve_provider(v_care_navigator_account_id, NEW.provider_id) THEN
			UPDATE care_encounter
			SET care_navigator_account_id=first_care_navigator_account_for_provider(NEW.provider_id),
				last_updated_by_account_id=NEW.created_by_account_id
			WHERE care_encounter_id=NEW.care_encounter_id;
		END IF;

		RETURN NEW;
	END IF;

	SELECT care_encounter.care_encounter_id,
		care_encounter.care_navigator_account_id
	INTO v_care_encounter_id, v_care_navigator_account_id
	FROM care_encounter
	WHERE care_encounter.account_id=NEW.account_id
	AND care_encounter.care_encounter_status_id='OPEN'
	AND care_encounter.deleted=FALSE
	FOR UPDATE;

	IF v_care_encounter_id IS NOT NULL THEN
		IF v_new_appointment_is_active AND EXISTS (
			SELECT 1
			FROM appointment
			WHERE appointment.care_encounter_id=v_care_encounter_id
			AND appointment.attendance_status_id='ATTENDED'
		) THEN
			RAISE EXCEPTION 'Completed Care Navigator encounter must be closed before another appointment can be booked.';
		END IF;

		IF v_new_appointment_is_active AND EXISTS (
			SELECT 1
			FROM appointment
			WHERE appointment.care_encounter_id=v_care_encounter_id
			AND appointment.appointment_id<>NEW.appointment_id
			AND appointment.canceled=FALSE
			AND appointment.canceled_for_reschedule=FALSE
			AND appointment.attendance_status_id='UNKNOWN'
		) THEN
			RAISE EXCEPTION 'Care Navigator encounter already has an active appointment.';
		END IF;

		IF v_care_navigator_account_id IS NULL
			OR NOT care_navigator_account_can_serve_provider(v_care_navigator_account_id, NEW.provider_id) THEN
			v_care_navigator_account_id := first_care_navigator_account_for_provider(NEW.provider_id);
		END IF;

		UPDATE care_encounter
		SET care_navigator_account_id=v_care_navigator_account_id,
			last_updated_by_account_id=NEW.created_by_account_id
		WHERE care_encounter_id=v_care_encounter_id;
	ELSE
		INSERT INTO care_encounter (
			account_id,
			care_navigator_account_id,
			email_address,
			created_by_account_id,
			last_updated_by_account_id
		) VALUES (
			NEW.account_id,
			first_care_navigator_account_for_provider(NEW.provider_id),
			NEW.email_address,
			NEW.created_by_account_id,
			NEW.created_by_account_id
		)
		RETURNING care_encounter_id INTO v_care_encounter_id;
	END IF;

	UPDATE appointment
	SET care_encounter_id=v_care_encounter_id
	WHERE appointment_id=NEW.appointment_id;

	RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS attach_care_navigator_appointment_to_encounter ON appointment;
CREATE TRIGGER attach_care_navigator_appointment_to_encounter
AFTER INSERT OR UPDATE OF provider_id, account_id, care_encounter_id ON appointment
FOR EACH ROW EXECUTE PROCEDURE attach_care_navigator_appointment_to_encounter();

-- Only an authenticated patient canceling their own non-reschedule booking
-- automatically closes the encounter. Unknown/external and staff actors leave
-- the lifecycle open for follow-up and rebooking.
CREATE OR REPLACE FUNCTION close_care_encounter_for_patient_cancellation()
RETURNS TRIGGER AS $$
DECLARE
	v_care_encounter_id UUID;
BEGIN
	IF NEW.canceled=TRUE
		AND NEW.canceled_by_account_id=NEW.account_id
		AND NEW.canceled_for_reschedule=FALSE THEN
		SELECT appointment.care_encounter_id
		INTO v_care_encounter_id
		FROM appointment
		WHERE appointment.appointment_id=NEW.appointment_id;

		UPDATE care_encounter
		SET care_encounter_status_id='CLOSED',
			closed_at=COALESCE(closed_at, NEW.canceled_at, NOW()),
			closed_by_account_id=NEW.canceled_by_account_id,
			last_updated_by_account_id=NEW.canceled_by_account_id
		WHERE care_encounter_id=COALESCE(NEW.care_encounter_id, v_care_encounter_id)
		AND care_encounter_status_id='OPEN'
		AND NOT EXISTS (
			SELECT 1
			FROM appointment active_appointment
			WHERE active_appointment.care_encounter_id=care_encounter.care_encounter_id
			AND active_appointment.appointment_id<>NEW.appointment_id
			AND active_appointment.canceled=FALSE
			AND active_appointment.canceled_for_reschedule=FALSE
			AND active_appointment.attendance_status_id='UNKNOWN'
		);
	END IF;

	RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS close_care_encounter_for_patient_cancellation ON appointment;
CREATE TRIGGER close_care_encounter_for_patient_cancellation
AFTER INSERT OR UPDATE OF canceled, canceled_by_account_id, canceled_for_reschedule ON appointment
FOR EACH ROW EXECUTE PROCEDURE close_care_encounter_for_patient_cancellation();

-- Providers assigned the role later receive the same modality default. A
-- provider_id touch routes existing appointments through the attachment trigger.
CREATE OR REPLACE FUNCTION apply_care_navigator_provider_defaults()
RETURNS TRIGGER AS $$
BEGIN
	IF NEW.support_role_id='CARE_NAVIGATOR' THEN
		UPDATE provider
		SET virtual_appointments_only=TRUE
		WHERE provider_id=NEW.provider_id;

		UPDATE appointment
		SET provider_id=appointment.provider_id
		WHERE appointment.provider_id=NEW.provider_id
		AND appointment.care_encounter_id IS NULL;
	END IF;

	RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS apply_care_navigator_provider_defaults ON provider_support_role;
CREATE TRIGGER apply_care_navigator_provider_defaults
AFTER INSERT OR UPDATE OF support_role_id ON provider_support_role
FOR EACH ROW EXECUTE PROCEDURE apply_care_navigator_provider_defaults();

UPDATE appointment
SET provider_id=appointment.provider_id
WHERE appointment.care_encounter_id IS NULL
AND EXISTS (
	SELECT 1
	FROM provider_support_role
	WHERE provider_support_role.provider_id=appointment.provider_id
	AND provider_support_role.support_role_id='CARE_NAVIGATOR'
);

COMMIT;
