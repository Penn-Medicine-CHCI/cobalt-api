BEGIN;
SELECT _v.register_patch('265-page-row-split-padding', NULL, NULL);

ALTER TABLE page_row
  ADD COLUMN padding_top_id TEXT NOT NULL DEFAULT 'MEDIUM',
  ADD COLUMN padding_bottom_id TEXT NOT NULL DEFAULT 'MEDIUM',
  ADD CONSTRAINT page_row_padding_top_id_check CHECK (padding_top_id IN ('NONE', 'SMALL', 'MEDIUM', 'LARGE')),
  ADD CONSTRAINT page_row_padding_bottom_id_check CHECK (padding_bottom_id IN ('NONE', 'SMALL', 'MEDIUM', 'LARGE'));

UPDATE page_row
SET padding_top_id = padding_id,
    padding_bottom_id = padding_id;

DROP VIEW IF EXISTS v_page_row;

CREATE VIEW v_page_row AS
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
  pr.padding_bottom_id
FROM page_row pr, page_section ps, page p
WHERE pr.page_section_id = ps.page_section_id
AND ps.page_id = p.page_id
AND pr.deleted_flag = false;

COMMIT;
