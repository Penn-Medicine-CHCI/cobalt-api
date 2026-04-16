BEGIN;
SELECT _v.register_patch('258-page-row-padding', NULL, NULL);

ALTER TABLE page_row
  ADD COLUMN padding_id TEXT NOT NULL DEFAULT 'MEDIUM',
  ADD CONSTRAINT page_row_padding_id_check CHECK (padding_id IN ('SMALL', 'MEDIUM', 'LARGE'));

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
  pr.padding_id
FROM page_row pr, page_section ps, page p
WHERE pr.page_section_id = ps.page_section_id
AND ps.page_id = p.page_id
AND pr.deleted_flag = false;

COMMIT;
