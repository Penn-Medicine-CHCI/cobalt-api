BEGIN;
SELECT _v.register_patch('291-page-directory-locations', ARRAY['215-page-group'], NULL);

-- Directory membership is independent of scheduled homepage and navigation placements.
CREATE TABLE page_location (
  page_location_id TEXT PRIMARY KEY,
  description TEXT NOT NULL,
  directory_relative_url TEXT,
  display_order SMALLINT NOT NULL UNIQUE
);

INSERT INTO page_location (page_location_id, description, directory_relative_url, display_order)
VALUES
  ('FEATURED_TOPIC', 'Featured Topics', '/featured-topics', 0),
  ('COMMUNITY', 'Communities', '/communities', 1),
  ('UNCATEGORIZED', 'Uncategorized', NULL, 2);

ALTER TABLE page ADD COLUMN page_location_id TEXT NOT NULL DEFAULT 'UNCATEGORIZED' REFERENCES page_location;
-- Preserve publication history when the current publication date is cleared.
ALTER TABLE page ADD COLUMN has_been_published BOOLEAN NOT NULL DEFAULT FALSE;

-- Both backfills must preserve the editor-visible modified timestamp.
ALTER TABLE page DISABLE TRIGGER set_last_updated;
UPDATE page p
SET page_location_id = inferred.page_location_id
FROM (
  SELECT page_id, MAX(site_location_id) AS page_location_id
  FROM page_site_location
  WHERE site_location_id IN ('FEATURED_TOPIC', 'COMMUNITY')
  GROUP BY page_id
  HAVING COUNT(DISTINCT site_location_id) = 1
) inferred
WHERE inferred.page_id = p.page_id;

UPDATE page
SET has_been_published = TRUE
WHERE page_status_id IN ('LIVE', 'COPY_FOR_EDITING') OR published_date IS NOT NULL;
ALTER TABLE page ENABLE TRIGGER set_last_updated;

UPDATE page_status SET description = 'Published' WHERE page_status_id = 'LIVE';
UPDATE page_status SET description = 'Unpublished' WHERE page_status_id = 'DRAFT';

-- Keep the existing view columns in order and append the new response metadata.
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
  fu.url AS image_url,
  p.page_location_id,
  pl.description AS page_location_description,
  pst.description AS page_status_description,
  p.has_been_published
FROM page p
LEFT JOIN file_upload fu ON p.image_file_upload_id = fu.file_upload_id
JOIN page_location pl ON p.page_location_id = pl.page_location_id
JOIN page_status pst ON p.page_status_id = pst.page_status_id
WHERE p.deleted_flag = FALSE;

CREATE INDEX page_published_directory_name_idx
ON page (institution_id, page_location_id, LOWER(name), page_id)
WHERE deleted_flag = FALSE AND page_status_id = 'LIVE';

COMMIT;
