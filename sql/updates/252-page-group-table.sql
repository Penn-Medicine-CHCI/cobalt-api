BEGIN;
SELECT _v.register_patch('252-page-group-table', NULL, NULL);

CREATE TABLE page_group (
  page_group_id UUID PRIMARY KEY,
  analytics_campaign_key TEXT
);

INSERT INTO page_group (page_group_id)
SELECT DISTINCT page_group_id
FROM page
ON CONFLICT (page_group_id) DO NOTHING;

ALTER TABLE page
  ADD CONSTRAINT page_page_group_id_fkey
  FOREIGN KEY (page_group_id) REFERENCES page_group(page_group_id);

COMMIT;
