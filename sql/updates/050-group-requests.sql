BEGIN;
SELECT _v.register_patch('050-group-requests', NULL, NULL);

CREATE TABLE group_topic (
	group_topic_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	institution_id TEXT NOT NULL REFERENCES institution,
	name TEXT NOT NULL,
	description TEXT NOT NULL,
	display_order INTEGER NOT NULL DEFAULT 1,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON group_topic FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE group_request (
	group_request_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	requestor_account_id UUID NOT NULL REFERENCES account(account_id),
	requestor_name TEXT NOT NULL,
	requestor_email_address TEXT NOT NULL,
	preferred_date_description TEXT,
	preferred_time_description TEXT,
	additional_description TEXT,
	other_group_topics_description TEXT,
	minimum_attendee_count INTEGER NOT NULL,
	maximum_attendee_count INTEGER,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON group_request FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE group_request_topic (
	group_request_id UUID NOT NULL REFERENCES group_request,
	group_topic_id UUID NOT NULL REFERENCES group_topic,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	PRIMARY KEY (group_request_id, group_topic_id)
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON group_request_topic FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE group_request_institution_contact (
	institution_id TEXT NOT NULL REFERENCES institution,
	email_address TEXT NOT NULL,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	PRIMARY KEY (institution_id, email_address)
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON group_request_institution_contact FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

COMMIT;