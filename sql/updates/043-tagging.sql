BEGIN;
SELECT _v.register_patch('043-tagging', NULL, NULL);

-- Groups tags for display purposes
CREATE TABLE tag_group (
	tag_group_id VARCHAR PRIMARY KEY,
	name VARCHAR NOT NULL,
	description VARCHAR NOT NULL,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON tag_group FOR EACH ROW EXECUTE PROCEDURE set_last_updated();
CREATE UNIQUE INDEX tag_group_unique_idx ON tag_group USING btree (LOWER(name));

-- Tags can be associated to content and group sessions and then used for recommendations in screening logic
CREATE TABLE tag (
	tag_id VARCHAR PRIMARY KEY,
	name VARCHAR NOT NULL,
	description VARCHAR NOT NULL,
	tag_group_id VARCHAR NOT NULL REFERENCES tag_group,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON tag FOR EACH ROW EXECUTE PROCEDURE set_last_updated();
CREATE UNIQUE INDEX tag_unique_idx ON tag USING btree (LOWER(name));

-- Relates tags to content
CREATE TABLE tag_content (
	tag_content_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	tag_id VARCHAR NOT NULL REFERENCES tag,
	content_id UUID NOT NULL REFERENCES content,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON tag_content FOR EACH ROW EXECUTE PROCEDURE set_last_updated();
CREATE UNIQUE INDEX tag_content_unique_idx ON tag_content USING btree (tag_id, content_id);

-- Relates tags to group sessions
CREATE TABLE tag_group_session (
	tag_content_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	tag_id VARCHAR NOT NULL REFERENCES tag,
	group_session_id UUID NOT NULL REFERENCES group_session,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON tag_group_session FOR EACH ROW EXECUTE PROCEDURE set_last_updated();
CREATE UNIQUE INDEX tag_group_session_unique_idx ON tag_group_session USING btree (tag_id, group_session_id);

-- Relates tags to group sessions by request
CREATE TABLE tag_group_session_request (
	tag_content_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	tag_id VARCHAR NOT NULL REFERENCES tag,
	group_session_request_id UUID NOT NULL REFERENCES group_session_request,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON tag_group_session_request FOR EACH ROW EXECUTE PROCEDURE set_last_updated();
CREATE UNIQUE INDEX tag_group_session_request_unique_idx ON tag_group_session_request USING btree (tag_id, group_session_request_id);

-- Relates the tags that are recommended as part of a screening session
CREATE TABLE tag_screening_session (
    tag_screening_session_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tag_id VARCHAR NOT NULL REFERENCES tag,
    screening_session_id UUID NOT NULL REFERENCES screening_session,
    created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON tag_screening_session FOR EACH ROW EXECUTE PROCEDURE set_last_updated();
CREATE UNIQUE INDEX tag_screening_session_unique_idx ON tag_screening_session USING btree (tag_id, screening_session_id);

COMMIT;