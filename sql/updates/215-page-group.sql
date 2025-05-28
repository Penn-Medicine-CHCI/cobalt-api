BEGIN;

ALTER TABLE page ADD COLUMN page_group_id UUID NULL;

WITH RECURSIVE page_hierarchy AS (
  -- start with all root pages (no parent)
  SELECT
    page_id,
    parent_page_id,
    page_id AS root_page_id
  FROM cobalt.page
  WHERE parent_page_id IS NULL
  UNION ALL
  -- walk down: each child inherits its ancestor’s root_page_id
  SELECT
    p.page_id,
    p.parent_page_id,
    ph.root_page_id
  FROM cobalt.page p
  JOIN page_hierarchy ph
    ON p.parent_page_id = ph.page_id
)
-- now update every page to point at its root_page_id
UPDATE cobalt.page AS tgt
SET page_group_id = ph.root_page_id
FROM page_hierarchy ph
WHERE tgt.page_id = ph.page_id;

ALTER TABLE page ALTER COLUMN page_group_id SET NOT NULL;

DROP VIEW v_page;

CREATE OR REPLACE VIEW v_page AS
SELECT
  p.page_id,
  p.name,
  p.url_name,
  p.page_status_id,
  p.headline,
  p.description,
  p.image_file_upload_id,
  p.image_alt_text,
  p.published_date,
  p.deleted_flag,
  p.institution_id,
  p.parent_page_id,
  p.created_by_account_id,
  p.created,
  p.last_updated,
  p.page_group_id,
  MIN(p.created) OVER (PARTITION BY p.page_group_id) AS original_create_date,
  fu.url AS image_url
FROM page p
LEFT JOIN file_upload fu
  ON p.image_file_upload_id = fu.file_upload_id
WHERE p.deleted_flag = false;

COMMIT;