BEGIN;
SELECT _v.register_patch('128-topic-centers', NULL, NULL);

CREATE TABLE topic_center_row_tag_type (
  topic_center_row_tag_type_id TEXT NOT NULL PRIMARY KEY,
  description TEXT NOT NULL
);

INSERT INTO topic_center_row_tag_type VALUES ('CONTENT', 'Content');
INSERT INTO topic_center_row_tag_type VALUES ('GROUP_SESSION', 'Group Session');

-- Ability to associate a set of content/group sessions/(other?) with a particular tag to a topic center row
CREATE TABLE topic_center_row_tag (
	topic_center_row_id UUID NOT NULL REFERENCES topic_center_row,
	topic_center_row_tag_type_id TEXT NOT NULL REFERENCES topic_center_row_tag_type,
	tag_id TEXT NOT NULL REFERENCES tag,
	created_by_account_id UUID NOT NULL REFERENCES account (account_id),
	title TEXT,
	description TEXT,
	cta TEXT,
	cta_url TEXT,
	display_order INTEGER NOT NULL DEFAULT 1,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	PRIMARY KEY (topic_center_row_id, topic_center_row_tag_type_id, tag_id)
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON topic_center_row_tag FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

ALTER TABLE topic_center_row ADD COLUMN group_sessions_title_override TEXT;
ALTER TABLE topic_center_row ADD COLUMN group_sessions_description_override TEXT;
ALTER TABLE topic_center_row ADD COLUMN group_session_requests_title_override TEXT;
ALTER TABLE topic_center_row ADD COLUMN group_session_requests_description_override TEXT;

COMMIT;