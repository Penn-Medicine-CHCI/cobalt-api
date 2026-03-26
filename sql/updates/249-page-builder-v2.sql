BEGIN;
SELECT _v.register_patch('249-page-builder-v2', NULL, NULL);

ALTER TABLE page_row
  ADD COLUMN name TEXT NULL,
  ADD COLUMN background_color_id TEXT NOT NULL REFERENCES background_color DEFAULT 'WHITE';

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
  pr.background_color_id
FROM page_row pr, page_section ps, page p
WHERE pr.page_section_id = ps.page_section_id
AND ps.page_id = p.page_id
AND pr.deleted_flag = false;

INSERT INTO row_type (row_type_id, description)
VALUES
  ('ONE_COLUMN_TEXT', 'One Column Text'),
  ('TWO_COLUMN_TEXT', 'Two Column Text'),
  ('ONE_COLUMN_IMAGE_RIGHT', 'One Column Image Right');

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
keeper_sections AS (
  SELECT
    os.page_id,
    os.page_section_id AS keeper_section_id
  FROM ordered_sections os
  WHERE os.section_rank = 1
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
  JOIN keeper_sections ks
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
  JOIN keeper_sections ks
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
  FROM keeper_sections ks
  WHERE ps.page_section_id = ks.keeper_section_id
  RETURNING ps.page_section_id
)
DELETE FROM page_section ps
USING keeper_sections ks
WHERE ps.page_id = ks.page_id
  AND ps.page_section_id <> ks.keeper_section_id;

COMMIT;
