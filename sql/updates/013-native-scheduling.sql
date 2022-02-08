BEGIN;
SELECT _v.register_patch('013-native-scheduling', ARRAY['000-base-creates'], NULL);

ALTER TABLE account ADD COLUMN scheduling_tutorial_viewed BOOLEAN NOT NULL DEFAULT FALSE;

-- Allow people to interact with (view/edit/etc.) each other's calendars
CREATE TABLE calendar_permission (
	calendar_permission_id VARCHAR PRIMARY KEY,
	description VARCHAR NOT NULL
);

INSERT INTO calendar_permission (calendar_permission_id, description) VALUES ('MANAGER', 'Manager');
INSERT INTO calendar_permission (calendar_permission_id, description) VALUES ('VIEWER', 'Viewer');

CREATE TABLE account_calendar_permission (
	provider_id UUID NOT NULL REFERENCES provider,
	granted_to_account_id UUID NOT NULL REFERENCES account(account_id),
	calendar_permission_id VARCHAR NOT NULL REFERENCES calendar_permission,
	created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
	created_by_account_id UUID NOT NULL REFERENCES account(account_id),
	last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
	last_updated_by_account_id UUID NOT NULL REFERENCES account(account_id),
	PRIMARY KEY(provider_id, granted_to_account_id, calendar_permission_id)
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON account_calendar_permission FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

-- Is our logical availability for an "open" chunk of time, or to "block off" a chunk of time (be unavailable, e.g. vacation)
CREATE TABLE logical_availability_type (
	logical_availability_type_id VARCHAR PRIMARY KEY,
	description VARCHAR NOT NULL
);

INSERT INTO logical_availability_type (logical_availability_type_id, description) VALUES ('OPEN', 'Open');
INSERT INTO logical_availability_type (logical_availability_type_id, description) VALUES ('BLOCK', 'Block');

ALTER TABLE logical_availability ADD COLUMN logical_availability_type_id VARCHAR NOT NULL REFERENCES logical_availability_type DEFAULT 'OPEN';

-- Availability recurrence rules (currently just NONE or DAILY, future might include WEEKLY or MONTHLY)
CREATE TABLE recurrence_type (
	recurrence_type_id VARCHAR PRIMARY KEY,
	description VARCHAR NOT NULL
);

INSERT INTO recurrence_type (recurrence_type_id, description) VALUES ('NONE', 'None');
INSERT INTO recurrence_type (recurrence_type_id, description) VALUES ('DAILY', 'Daily');

ALTER TABLE logical_availability ADD COLUMN recurrence_type_id VARCHAR NOT NULL REFERENCES recurrence_type DEFAULT 'NONE';
ALTER TABLE logical_availability ADD COLUMN recur_sunday BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE logical_availability ADD COLUMN recur_monday BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE logical_availability ADD COLUMN recur_tuesday BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE logical_availability ADD COLUMN recur_wednesday BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE logical_availability ADD COLUMN recur_thursday BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE logical_availability ADD COLUMN recur_friday BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE logical_availability ADD COLUMN recur_saturday BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE logical_availability ADD COLUMN created_by_account_id UUID NOT NULL REFERENCES account(account_id);
ALTER TABLE logical_availability ADD COLUMN last_updated_by_account_id UUID NOT NULL REFERENCES account(account_id);

-- Reporting table to capture statistics like “provider X had Y slots open on day Z” since our native-scheduling slots
-- are calculated at runtime and could change over time (a provider might modify recurrence rules, for example).
-- A nightly job will examine a provider's availability for the day in her timezone (just like
-- an end user of the app would see) and writes to this table so we have an immutable record of that day's availability.

CREATE TABLE reporting_provider_availability (
	provider_id UUID NOT NULL REFERENCES provider,
	appointment_id UUID REFERENCES appointment, -- nice-to-have but not strictly necessary
	date_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
	available BOOLEAN NOT NULL,
	created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- 3692510 is "Cobalt Blue" 0x3857DE in decimal
ALTER TABLE appointment_type ADD COLUMN hex_color INTEGER NOT NULL DEFAULT 3692510;

-- Keep track of all intake assessments tied to an appointment type.
-- We want to keep a history for historical reasons, so if an appointment type has a new assessment tied to it,
-- we can trace back previous answers instead of having the old assessment become "free floating".
--
-- Only one assessment can be active per appointment type, see trigger below which enforces this.
CREATE TABLE appointment_type_assessment (
    assessment_id UUID NOT NULL REFERENCES assessment,
    appointment_type_id UUID NOT NULL REFERENCES appointment_type,
    active BOOLEAN NOT NULL,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    PRIMARY KEY(assessment_id, appointment_type_id)
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON appointment_type_assessment FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE OR REPLACE FUNCTION appointment_type_assessment_active_fn()
RETURNS trigger
AS $function$
BEGIN
	IF (NEW.active = TRUE) THEN
  	UPDATE appointment_type_assessment SET active=FALSE WHERE appointment_type_id=NEW.appointment_type_id;
	END IF;

	RETURN NEW;
END;
$function$
LANGUAGE plpgsql;

CREATE TRIGGER appointment_type_assessment_active_tg
BEFORE INSERT OR UPDATE OF active ON appointment_type_assessment
FOR EACH ROW WHEN (NEW.active = true)
EXECUTE PROCEDURE appointment_type_assessment_active_fn();

-- Include active assessment ID on appointment type, filter out deleted appointment types
CREATE VIEW v_appointment_type AS
SELECT app_type.*, ata.assessment_id
FROM appointment_type app_type
LEFT OUTER JOIN appointment_type_assessment ata ON app_type.appointment_type_id = ata.appointment_type_id AND ata.active=TRUE
WHERE app_type.deleted = FALSE;

COMMIT;