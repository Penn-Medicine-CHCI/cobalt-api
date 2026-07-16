BEGIN;

SELECT _v.register_patch('257-page-builder-v2', NULL, NULL);

-- DEPLOYMENT NOTE: this data reshape introduces row-type values that pre-V2 API
-- instances cannot deserialize. Follow docs/page-builder-v2-deployment.md and
-- run it only after draining old instances during the maintenance window.

-- Preserve pre-migration page data for audit and manual recovery after the section-to-row reshape
CREATE TABLE page_builder_v2_257_backup_page AS
SELECT NOW() AS backed_up_at, p.*
FROM page p;

CREATE TABLE page_builder_v2_257_backup_page_site_location AS
SELECT NOW() AS backed_up_at, psl.*
FROM page_site_location psl;

CREATE TABLE page_builder_v2_257_backup_page_section AS
SELECT NOW() AS backed_up_at, ps.*
FROM page_section ps;

CREATE TABLE page_builder_v2_257_backup_page_row AS
SELECT NOW() AS backed_up_at, pr.*
FROM page_row pr;

CREATE TABLE page_builder_v2_257_backup_page_row_column AS
SELECT NOW() AS backed_up_at, prc.*
FROM page_row_column prc;

CREATE TABLE page_builder_v2_257_backup_page_row_group_session AS
SELECT NOW() AS backed_up_at, prgs.*
FROM page_row_group_session prgs;

CREATE TABLE page_builder_v2_257_backup_page_row_content AS
SELECT NOW() AS backed_up_at, prc.*
FROM page_row_content prc;

CREATE TABLE page_builder_v2_257_backup_page_row_tag_group AS
SELECT NOW() AS backed_up_at, prtg.*
FROM page_row_tag_group prtg;

CREATE TABLE page_builder_v2_257_backup_page_row_tag AS
SELECT NOW() AS backed_up_at, prt.*
FROM page_row_tag prt;

CREATE TABLE page_builder_v2_257_backup_page_row_mailing_list AS
SELECT NOW() AS backed_up_at, prml.*
FROM page_row_mailing_list prml;

-- Define reusable page-row padding values instead of inline CHECK constraints
CREATE TABLE page_row_padding (
  page_row_padding_id TEXT PRIMARY KEY,
  description TEXT NOT NULL
);

INSERT INTO page_row_padding (page_row_padding_id, description)
VALUES
  ('NONE', 'None'),
  ('SMALL', 'Small'),
  ('MEDIUM', 'Medium'),
  ('LARGE', 'Large');

-- Define reusable custom-row column content order values instead of inline CHECK constraints
CREATE TABLE page_row_column_content_order (
  page_row_column_content_order_id TEXT PRIMARY KEY,
  description TEXT NOT NULL
);

INSERT INTO page_row_column_content_order (page_row_column_content_order_id, description)
VALUES
  ('IMAGE_THEN_TEXT', 'Image Then Text'),
  ('TEXT_THEN_IMAGE', 'Text Then Image');

-- Add page-row metadata used by the page builder editor and renderer
ALTER TABLE page_row
  ADD COLUMN name TEXT NULL,
  ADD COLUMN background_color_id TEXT NOT NULL REFERENCES background_color DEFAULT 'WHITE',
  ADD COLUMN padding_id TEXT NOT NULL REFERENCES page_row_padding DEFAULT 'MEDIUM',
  ADD COLUMN padding_top_id TEXT NOT NULL REFERENCES page_row_padding DEFAULT 'MEDIUM',
  ADD COLUMN padding_bottom_id TEXT NOT NULL REFERENCES page_row_padding DEFAULT 'MEDIUM',
  ADD COLUMN page_row_anchor_id UUID NULL;

-- A row's primary key changes whenever a live page is copied for editing. Give
-- existing rows a separate logical identity so public fragment links can remain
-- stable across that versioning cycle. Using the original primary key makes the
-- backfill deterministic; newly-created rows receive a fresh UUID by default.
UPDATE page_row
SET page_row_anchor_id = page_row_id;

ALTER TABLE page_row
  ALTER COLUMN page_row_anchor_id SET DEFAULT uuid_generate_v4(),
  ALTER COLUMN page_row_anchor_id SET NOT NULL,
  ADD CONSTRAINT page_row_page_section_id_page_row_anchor_id_key
    UNIQUE (page_section_id, page_row_anchor_id);

-- Add custom-row column presentation settings
ALTER TABLE page_row_column
  ADD COLUMN content_order_id TEXT NOT NULL REFERENCES page_row_column_content_order DEFAULT 'IMAGE_THEN_TEXT',
  ADD COLUMN use_placeholder_image BOOLEAN NOT NULL DEFAULT FALSE;

-- Normalize any historical gaps or duplicate display positions before enforcing the
-- invariant relied on by custom-row add/edit/reorder operations.
WITH ordered_columns AS (
  SELECT
    prc.page_row_column_id,
    row_number() OVER (
      PARTITION BY prc.page_row_id
      ORDER BY prc.column_display_order, prc.page_row_column_id
    ) - 1 AS normalized_display_order
  FROM page_row_column prc
)
UPDATE page_row_column prc
SET column_display_order = oc.normalized_display_order
FROM ordered_columns oc
WHERE prc.page_row_column_id = oc.page_row_column_id
  AND prc.column_display_order <> oc.normalized_display_order;

ALTER TABLE page_row_column
  ADD CONSTRAINT page_row_column_page_row_id_column_display_order_key
  UNIQUE (page_row_id, column_display_order)
  DEFERRABLE INITIALLY IMMEDIATE;

-- Serialize column creation on the parent row and enforce the four-column
-- product limit even for callers outside PageService.
CREATE FUNCTION page_row_column_enforce_limit() RETURNS TRIGGER AS $$
BEGIN
  IF TG_OP = 'UPDATE' AND NEW.page_row_id = OLD.page_row_id THEN
    RETURN NEW;
  END IF;

  PERFORM 1
  FROM page_row
  WHERE page_row_id = NEW.page_row_id
  FOR UPDATE;

  IF (
    SELECT count(*)
    FROM page_row_column
    WHERE page_row_id = NEW.page_row_id
  ) >= 4 THEN
    RAISE EXCEPTION 'A page row cannot contain more than four columns'
      USING ERRCODE = 'check_violation';
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER page_row_column_enforce_limit
BEFORE INSERT OR UPDATE OF page_row_id ON page_row_column
FOR EACH ROW EXECUTE PROCEDURE page_row_column_enforce_limit();

-- Register the new page builder row types
INSERT INTO row_type (row_type_id, description)
VALUES
  ('ONE_COLUMN_TEXT', 'One Column Text'),
  ('TWO_COLUMN_TEXT', 'Two Column Text'),
  ('ONE_COLUMN_IMAGE_RIGHT', 'One Column Image Right'),
  ('CUSTOM_ROW', 'Custom Row'),
  ('CALL_TO_ACTION_BLOCK', 'Call-to-Action (Block)'),
  ('CALL_TO_ACTION_FULL_WIDTH', 'Call-to-Action (Full Width)');

-- Ensure each existing page has at least one content section to hold rows
INSERT INTO page_section (
  page_section_id,
  page_id,
  name,
  headline,
  description,
  background_color_id,
  created_by_account_id,
  display_order
)
SELECT
  uuid_generate_v4(),
  p.page_id,
  'Content',
  NULL,
  NULL,
  'WHITE',
  p.created_by_account_id,
  0
FROM page p
WHERE p.deleted_flag = FALSE
  AND NOT EXISTS (
    SELECT 1
    FROM page_section ps
    WHERE ps.page_id = p.page_id
      AND ps.deleted_flag = FALSE
  );

-- Materialize the keeper choice so every subsequent reshape step, including
-- cleanup of inactive rows, uses the exact same section for each page.
CREATE TEMPORARY TABLE page_builder_v2_257_keeper_section ON COMMIT DROP AS
SELECT page_id, page_section_id AS keeper_section_id
FROM (
  SELECT
    ps.page_id,
    ps.page_section_id,
    row_number() OVER (
      PARTITION BY ps.page_id
      ORDER BY ps.display_order, ps.page_section_id
    ) AS section_rank
  FROM page_section ps
  WHERE ps.deleted_flag = FALSE
) ranked_sections
WHERE section_rank = 1;

CREATE UNIQUE INDEX page_builder_v2_257_keeper_section_page_id_key
ON page_builder_v2_257_keeper_section(page_id);

-- Move legacy section headline/description content into text rows and collapse each page to one content section
WITH ordered_sections AS (
  SELECT
    ps.page_id,
    ps.page_section_id,
    ps.name,
    ps.headline,
    ps.description,
    ps.background_color_id,
    ps.created_by_account_id,
    ps.display_order,
    row_number() OVER (PARTITION BY ps.page_id ORDER BY ps.display_order, ps.page_section_id) AS section_rank
  FROM page_section ps
  WHERE ps.deleted_flag = FALSE
),
ordered_items AS (
  SELECT
    os.page_id,
    os.page_section_id,
    ks.keeper_section_id,
    NULL::UUID AS existing_page_row_id,
    os.name AS row_name,
    'ONE_COLUMN_TEXT'::TEXT AS row_type_id,
    os.background_color_id,
    os.created_by_account_id,
    os.headline AS column_headline,
    os.description AS column_description,
    os.display_order AS section_order,
    0 AS item_order,
    0 AS row_order
  FROM ordered_sections os
  JOIN page_builder_v2_257_keeper_section ks
    ON ks.page_id = os.page_id
  WHERE COALESCE(NULLIF(BTRIM(os.headline), ''), NULLIF(BTRIM(os.description), '')) IS NOT NULL

  UNION ALL

  SELECT
    os.page_id,
    os.page_section_id,
    ks.keeper_section_id,
    pr.page_row_id AS existing_page_row_id,
    COALESCE(NULLIF(BTRIM(pr.name), ''), CASE
      WHEN pr.row_type_id IN ('ONE_COLUMN_TEXT', 'TWO_COLUMN_TEXT') THEN 'Text'
      WHEN pr.row_type_id = 'RESOURCES' THEN 'Resources'
      WHEN pr.row_type_id = 'GROUP_SESSIONS' THEN 'Group Sessions'
      WHEN pr.row_type_id = 'TAG_GROUP' THEN 'Tag Group'
      WHEN pr.row_type_id = 'TAG' THEN 'Tag'
      WHEN pr.row_type_id = 'MAILING_LIST' THEN 'Subscribe'
      ELSE 'Text & Image'
    END) AS row_name,
    pr.row_type_id::TEXT AS row_type_id,
    os.background_color_id,
    pr.created_by_account_id,
    NULL::TEXT AS column_headline,
    NULL::TEXT AS column_description,
    os.display_order AS section_order,
    1 AS item_order,
    pr.display_order AS row_order
  FROM ordered_sections os
  JOIN page_builder_v2_257_keeper_section ks
    ON ks.page_id = os.page_id
  JOIN page_row pr
    ON pr.page_section_id = os.page_section_id
   AND pr.deleted_flag = FALSE
),
ordered_rows AS (
  SELECT
    oi.*,
    row_number() OVER (
      PARTITION BY oi.page_id
      ORDER BY oi.section_order, oi.item_order, oi.row_order, oi.page_section_id, oi.existing_page_row_id
    ) - 1 AS new_display_order
  FROM ordered_items oi
),
new_text_rows AS (
  SELECT
    uuid_generate_v4() AS page_row_id,
    orw.keeper_section_id AS page_section_id,
    orw.row_name,
    orw.background_color_id,
    orw.created_by_account_id,
    orw.column_headline,
    orw.column_description,
    orw.new_display_order
  FROM ordered_rows orw
  WHERE orw.existing_page_row_id IS NULL
),
inserted_text_rows AS (
  INSERT INTO page_row (
    page_row_id,
    page_section_id,
    row_type_id,
    deleted_flag,
    display_order,
    created_by_account_id,
    name,
    background_color_id
  )
  SELECT
    ntr.page_row_id,
    ntr.page_section_id,
    'ONE_COLUMN_TEXT',
    FALSE,
    ntr.new_display_order,
    ntr.created_by_account_id,
    ntr.row_name,
    ntr.background_color_id
  FROM new_text_rows ntr
  RETURNING page_row_id
),
inserted_text_row_columns AS (
  INSERT INTO page_row_column (
    page_row_id,
    headline,
    description,
    image_file_upload_id,
    image_alt_text,
    column_display_order
  )
  SELECT
    ntr.page_row_id,
    ntr.column_headline,
    ntr.column_description,
    NULL,
    NULL,
    0
  FROM new_text_rows ntr
  RETURNING page_row_id
),
updated_existing_rows AS (
  UPDATE page_row pr
  SET
    page_section_id = orw.keeper_section_id,
    name = orw.row_name,
    background_color_id = orw.background_color_id,
    display_order = orw.new_display_order
  FROM ordered_rows orw
  WHERE pr.page_row_id = orw.existing_page_row_id
  RETURNING pr.page_row_id
),
updated_keeper_sections AS (
  UPDATE page_section ps
  SET
    name = 'Content',
    headline = NULL,
    description = NULL,
    background_color_id = 'WHITE',
    display_order = 0
  FROM page_builder_v2_257_keeper_section ks
  WHERE ps.page_section_id = ks.keeper_section_id
  RETURNING ps.page_section_id
)
SELECT COUNT(*) AS updated_keeper_section_count
FROM updated_keeper_sections;

-- The display reshape above intentionally ignores inactive rows. Reparent every
-- residual row as well so deleting old/deleted sections cannot violate the
-- non-cascading page_row.page_section_id foreign key.
UPDATE page_row pr
SET page_section_id = ks.keeper_section_id
FROM page_section source_section
JOIN page_builder_v2_257_keeper_section ks
  ON ks.page_id = source_section.page_id
WHERE pr.page_section_id = source_section.page_section_id
  AND pr.page_section_id <> ks.keeper_section_id;

DELETE FROM page_section ps
USING page_builder_v2_257_keeper_section ks
WHERE ps.page_id = ks.page_id
  AND ps.page_section_id <> ks.keeper_section_id;

-- Preserve intended placeholder image display for custom rows without uploaded images
UPDATE page_row_column
SET use_placeholder_image = TRUE
WHERE image_file_upload_id IS NULL
  AND page_row_id IN (
    SELECT page_row_id
    FROM page_row
    WHERE row_type_id = 'CUSTOM_ROW'
  );

-- Store call-to-action row content separately from the generic page row metadata
CREATE TABLE page_row_call_to_action (
  page_row_call_to_action_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  page_row_id UUID NOT NULL REFERENCES page_row,
  headline TEXT NOT NULL,
  description TEXT NOT NULL,
  button_text TEXT NOT NULL,
  button_url TEXT NOT NULL,
  image_file_upload_id UUID REFERENCES file_upload,
  created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Enforce one call-to-action record per row and keep audit timestamps current
CREATE UNIQUE INDEX idx_page_row_call_to_action_row ON page_row_call_to_action(page_row_id);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON page_row_call_to_action FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

-- Replace the page-row view with the final row metadata exposed to application code
CREATE OR REPLACE VIEW v_page_row AS
SELECT
  pr.page_row_id,
  pr.page_section_id,
  pr.row_type_id,
  pr.deleted_flag,
  pr.display_order,
  pr.created_by_account_id,
  pr.created,
  pr.last_updated,
  p.institution_id,
  pr.name,
  pr.background_color_id,
  pr.padding_top_id,
  pr.padding_bottom_id,
  pr.page_row_anchor_id
FROM page_row pr, page_section ps, page p
WHERE pr.page_section_id = ps.page_section_id
AND ps.page_id = p.page_id
AND pr.deleted_flag = false;

-- Recreate the row-column view so it exposes the new columns plus resolved image URL
DROP VIEW IF EXISTS v_page_row_column;

CREATE VIEW v_page_row_column AS
SELECT pr.*, fu.url AS image_url
FROM page_row_column pr
LEFT OUTER JOIN file_upload fu ON pr.image_file_upload_id = fu.file_upload_id;

-- Expose call-to-action rows with row ordering and resolved image URL
CREATE VIEW v_page_row_call_to_action AS
SELECT
  prcta.page_row_call_to_action_id,
  pr.page_row_id,
  pr.page_section_id,
  pr.display_order,
  pr.row_type_id,
  prcta.headline,
  prcta.description,
  prcta.button_text,
  prcta.button_url,
  prcta.image_file_upload_id,
  fu.url AS image_url,
  pr.created,
  pr.last_updated
FROM
  page_row_call_to_action prcta
  JOIN page_row pr ON prcta.page_row_id = pr.page_row_id
  LEFT OUTER JOIN file_upload fu ON prcta.image_file_upload_id = fu.file_upload_id
WHERE
  pr.deleted_flag = FALSE;

-- When a Page viewer clicks a Page Builder call-to-action button.
--
-- Additional data:
-- * pageId (UUID)
-- * pageRowId (UUID, the CTA row containing the clicked button)
-- * rowTypeId (String, either CALL_TO_ACTION_BLOCK or CALL_TO_ACTION_FULL_WIDTH)
-- * linkUrl (String, the browser-normalized destination URL)
-- * linkText (String, the CTA button text)
-- * siteLocationIds (String[], where this page "lives" on the site at the moment this event occurred)
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description)
VALUES ('CLICKTHROUGH_PAGE_CALL_TO_ACTION', 'Clickthrough (Page Call to Action)');

COMMIT;
