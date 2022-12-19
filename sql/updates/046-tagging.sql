BEGIN;
SELECT _v.register_patch('046-tagging', NULL, NULL);

-- cobalt=> select * from content_type;
--  content_type_id |    description     |     call_to_action
-- -----------------+--------------------+------------------------
--  VIDEO           | Video              | Watch the Video
--  AUDIO           | Audio              | Listen
--  PODCAST         | Podcast            | Listen
--  ARTICLE         | Article            | Read the Article
--  WORKSHEET       | Worksheet          | Complete the Worksheet
--  INT_BLOG        | Internal Blog Post | Read Blog Post
--  EXT_BLOG        | External Blog Post | Read Blog Post
--  APP             | App                | Get the App
-- (8 rows)
--
-- cobalt=> select * from content_type_label;
--            content_type_label_id            |                description
-- --------------------------------------------+--------------------------------------------
--  ACTION_GUIDE                               | Action Guide
--  EXTERNAL_BLOG_POST                         | External Blog Post
--  WORKSHEET                                  | Worksheet
--  WEBINAR_SERIES                             | Webinar Series
--  FACT_SHEET                                 | Fact Sheet
--  INTERNAL_BLOG_POST                         | Internal Blog Post
--  PODCAST                                    | Podcast
--  ARTICLE                                    | Article
--  INFORMATIONAL_HANDOUT                      | Informational Handout
--  HANDS_ON_ACTIVITIES_FOR_YOU_AND_YOUR_CHILD | Hands on Activities for you and your child
--  VIDEO                                      | Video
--  RESOURCE_GUIDE                             | Resource Guide
--  APP                                        | App
-- (13 rows)

-- Final content list: App, Article, Podcast, Video, Worksheet
ALTER TABLE content_type ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE content_type SET deleted=TRUE WHERE content_type_id IN ('INT_BLOG', 'EXT_BLOG', 'AUDIO');

-- Migrate over old content to new types
UPDATE content SET content_type_id='PODCAST' WHERE content_type_id='AUDIO';
UPDATE content SET content_type_id='ARTICLE' WHERE content_type_id='INT_BLOG';
UPDATE content SET content_type_id='ARTICLE' WHERE content_type_id='EXT_BLOG';

CREATE TABLE color (
  color_id VARCHAR PRIMARY KEY,
  description VARCHAR NOT NULL
);

INSERT INTO color VALUES ('BRAND_PRIMARY', 'Brand Primary');
INSERT INTO color VALUES ('BRAND_ACCENT', 'Brand Accent');
INSERT INTO color VALUES ('SEMANTIC_DANGER', 'Semantic Danger');
INSERT INTO color VALUES ('SEMANTIC_WARNING', 'Semantic Warning');
INSERT INTO color VALUES ('SEMANTIC_SUCCESS', 'Semantic Success');
INSERT INTO color VALUES ('SEMANTIC_INFO', 'Semantic Info');

-- Groups tags for display purposes
CREATE TABLE tag_group (
	tag_group_id VARCHAR PRIMARY KEY,
	color_id VARCHAR NOT NULL REFERENCES color DEFAULT 'BRAND_PRIMARY',
	name VARCHAR NOT NULL,
	url_name VARCHAR NOT NULL,
	description VARCHAR NOT NULL,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON tag_group FOR EACH ROW EXECUTE PROCEDURE set_last_updated();
CREATE UNIQUE INDEX tag_group_unique_idx ON tag_group USING btree (LOWER(name));
CREATE UNIQUE INDEX tag_group_url_name_unique_idx ON tag_group USING btree (LOWER(url_name));

-- Tags can be associated to content and group sessions and then used for recommendations in screening logic
CREATE TABLE tag (
	tag_id VARCHAR PRIMARY KEY,
	name VARCHAR NOT NULL,
	url_name VARCHAR NOT NULL,
	description VARCHAR NOT NULL,
	tag_group_id VARCHAR NOT NULL REFERENCES tag_group,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON tag FOR EACH ROW EXECUTE PROCEDURE set_last_updated();
CREATE UNIQUE INDEX tag_unique_idx ON tag USING btree (LOWER(name));
CREATE UNIQUE INDEX tag_url_name_unique_idx ON tag USING btree (LOWER(url_name));

-- Relates tags to content
CREATE TABLE tag_content (
	tag_content_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	tag_id VARCHAR NOT NULL REFERENCES tag,
  institution_id TEXT NOT NULL REFERENCES institution,
	content_id UUID NOT NULL REFERENCES content,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON tag_content FOR EACH ROW EXECUTE PROCEDURE set_last_updated();
CREATE UNIQUE INDEX tag_content_unique_idx ON tag_content USING btree (tag_id, institution_id, content_id);

-- Relates tags to group sessions
CREATE TABLE tag_group_session (
	tag_content_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	tag_id VARCHAR NOT NULL REFERENCES tag,
	institution_id TEXT NOT NULL REFERENCES institution,
	group_session_id UUID NOT NULL REFERENCES group_session,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON tag_group_session FOR EACH ROW EXECUTE PROCEDURE set_last_updated();
CREATE UNIQUE INDEX tag_group_session_unique_idx ON tag_group_session USING btree (tag_id, institution_id, group_session_id);

-- Relates tags to group sessions by request
CREATE TABLE tag_group_session_request (
	tag_content_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	tag_id VARCHAR NOT NULL REFERENCES tag,
	institution_id TEXT NOT NULL REFERENCES institution,
	group_session_request_id UUID NOT NULL REFERENCES group_session_request,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON tag_group_session_request FOR EACH ROW EXECUTE PROCEDURE set_last_updated();
CREATE UNIQUE INDEX tag_group_session_request_unique_idx ON tag_group_session_request USING btree (tag_id, institution_id, group_session_request_id);

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