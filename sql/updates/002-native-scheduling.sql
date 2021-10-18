BEGIN;
SELECT _v.register_patch('002-native-scheduling', ARRAY['000-base-creates'], NULL);

-- Track whether we've viewed the scheduling tutorial
ALTER TABLE account ADD COLUMN cobalt_scheduling_tutorial_viewed BOOLEAN NOT NULL DEFAULT FALSE;

-- Allow people to interact with (view/edit/etc.) each other's calendars
CREATE TABLE calendar_permission (
	calendar_permission_id VARCHAR PRIMARY KEY,
	description VARCHAR NOT NULL
);

INSERT INTO calendar_permission (calendar_permission_id, description) VALUES ('OWNER', 'Owner');
INSERT INTO calendar_permission (calendar_permission_id, description) VALUES ('EDITOR', 'Editor');
INSERT INTO calendar_permission (calendar_permission_id, description) VALUES ('VIEWER', 'Viewer');

CREATE TABLE account_calendar_permission (
	owner_account_id UUID NOT NULL REFERENCES account(account_id),
	granted_to_account_id UUID NOT NULL REFERENCES account(account_id),
	calendar_permission_id VARCHAR NOT NULL REFERENCES calendar_permission,
	created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
	created_by_account_id UUID NOT NULL REFERENCES account(account_id),
	last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
	last_updated_by_account_id UUID NOT NULL REFERENCES account(account_id),
	PRIMARY KEY(owner_account_id, granted_to_account_id, calendar_permission_id)
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

COMMIT;