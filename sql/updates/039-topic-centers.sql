BEGIN;
SELECT _v.register_patch('039-topic-centers', NULL, NULL);

CREATE TABLE topic_center (
	topic_center_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	name TEXT NOT NULL, -- e.g. Spaces of Color
	description TEXT,
	url_name TEXT, -- e.g. 'spaces-of-color', to be accessible via /topic-centers/spaces-of-color
	created_by_account_id UUID NOT NULL REFERENCES account (account_id),
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON topic_center FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE topic_center_row (
	topic_center_row_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	topic_center_id UUID NOT NULL REFERENCES topic_center,
	title TEXT, -- e.g. Community Connections
	description TEXT, -- e.g. Sources of connection, community, and healing for individuals
	display_order INTEGER NOT NULL DEFAULT 1,
	created_by_account_id UUID NOT NULL REFERENCES account (account_id),
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON topic_center_row FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE pinboard_note (
	pinboard_note_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	title TEXT NOT NULL,
	description TEXT NOT NULL,
	url TEXT,
	image_url TEXT,
	created_by_account_id UUID NOT NULL REFERENCES account (account_id),
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON pinboard_note FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE topic_center_row_pinboard_note (
	topic_center_row_id UUID NOT NULL REFERENCES topic_center_row,
	pinboard_note_id UUID NOT NULL REFERENCES pinboard_note,
	display_order INTEGER NOT NULL DEFAULT 1,
	created_by_account_id UUID NOT NULL REFERENCES account (account_id),
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	PRIMARY KEY (topic_center_row_id, pinboard_note_id)
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON topic_center_row_pinboard_note FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE topic_center_row_group_session (
	topic_center_row_id UUID NOT NULL REFERENCES topic_center_row,
	group_session_id UUID NOT NULL REFERENCES group_session,
	display_order INTEGER NOT NULL DEFAULT 1,
	created_by_account_id UUID NOT NULL REFERENCES account (account_id),
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	PRIMARY KEY (topic_center_row_id, group_session_id)
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON topic_center_row_group_session FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE topic_center_row_group_session_request (
	topic_center_row_id UUID NOT NULL REFERENCES topic_center_row,
	group_session_request_id UUID NOT NULL REFERENCES group_session_request,
	display_order INTEGER NOT NULL DEFAULT 1,
	created_by_account_id UUID NOT NULL REFERENCES account (account_id),
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	PRIMARY KEY (topic_center_row_id, group_session_request_id)
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON topic_center_row_group_session_request FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE topic_center_row_content (
	topic_center_row_id UUID NOT NULL REFERENCES topic_center_row,
	content_id UUID NOT NULL REFERENCES content,
	display_order INTEGER NOT NULL DEFAULT 1,
	created_by_account_id UUID NOT NULL REFERENCES account (account_id),
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	PRIMARY KEY (topic_center_row_id, content_id)
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON topic_center_row_content FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE institution_topic_center (
  institution_id TEXT NOT NULL REFERENCES institution,
	topic_center_id UUID NOT NULL REFERENCES topic_center,
	navigation_item_enabled BOOLEAN NOT NULL DEFAULT FALSE,
	navigation_item_name TEXT,
	navigation_icon_name TEXT,
	navigation_display_order INTEGER,
	PRIMARY KEY (institution_id, topic_center_id)
);

COMMIT;