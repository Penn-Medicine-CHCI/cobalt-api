BEGIN;
SELECT _v.register_patch('142-content-feedback', NULL, NULL);

CREATE TABLE content_feedback_type (
	content_feedback_type_id VARCHAR NOT NULL PRIMARY KEY,
	description VARCHAR NOT NULL
);

INSERT INTO content_feedback_type VALUES ('THUMBS_UP', 'Thumbs Up');
INSERT INTO content_feedback_type VALUES ('THUMBS_DOWN', 'Thumbs Down');

CREATE TABLE content_feedback (
  content_feedback_id UUID NOT NULL PRIMARY KEY DEFAULT uuid_generate_v4(),
  content_feedback_type_id VARCHAR NOT NULL REFERENCES content_feedback_type,
  content_id UUID NOT NULL REFERENCES content,
  account_id UUID NOT NULL REFERENCES account,
  message VARCHAR,
  created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON content_feedback FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

-- TODO: revisit the concept of "subscribers"/batch messaging in a future release
-- CREATE TABLE content_feedback_subscriber (
--   content_feedback_subscriber_id UUID NOT NULL PRIMARY KEY DEFAULT uuid_generate_v4(),
--   institution_id VARCHAR NOT NULL REFERENCES institution,
--   email_address VARCHAR NOT NULL,
--   active BOOLEAN NOT NULL DEFAULT TRUE,
--   created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
--   last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
-- );

-- CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON content_feedback_subscriber FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

ALTER TABLE content ADD COLUMN never_embed BOOLEAN NOT NULL DEFAULT FALSE;

COMMIT;