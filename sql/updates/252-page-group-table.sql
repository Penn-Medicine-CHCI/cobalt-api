BEGIN;
SELECT _v.register_patch('252-page-group-table', NULL, NULL);

CREATE TABLE page_group (
  page_group_id UUID PRIMARY KEY,
  analytics_campaign_key TEXT
);

CREATE TABLE page_group_email_group_session (
  page_group_email_group_session_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  page_group_id UUID NOT NULL REFERENCES page_group,
  group_session_id UUID NOT NULL REFERENCES group_session,
  description_override TEXT,
  display_order SMALLINT NOT NULL,
  created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX idx_page_group_email_group_session_group_session ON page_group_email_group_session(page_group_id, group_session_id);
CREATE UNIQUE INDEX idx_page_group_email_group_session_display_order ON page_group_email_group_session(page_group_id, display_order);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON page_group_email_group_session FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE page_group_email_content (
  page_group_email_content_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  page_group_id UUID NOT NULL REFERENCES page_group,
  content_id UUID NOT NULL REFERENCES content,
  display_order SMALLINT NOT NULL,
  created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX idx_page_group_email_content_content ON page_group_email_content(page_group_id, content_id);
CREATE UNIQUE INDEX idx_page_group_email_content_display_order ON page_group_email_content(page_group_id, display_order);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON page_group_email_content FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

INSERT INTO page_group (page_group_id)
SELECT DISTINCT page_group_id
FROM page
ON CONFLICT (page_group_id) DO NOTHING;

ALTER TABLE page
  ADD CONSTRAINT page_page_group_id_fkey
  FOREIGN KEY (page_group_id) REFERENCES page_group(page_group_id);

COMMIT;
