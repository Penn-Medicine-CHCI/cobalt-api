BEGIN;
SELECT _v.register_patch('259-page-row-padding-none', NULL, NULL);

ALTER TABLE page_row
  DROP CONSTRAINT IF EXISTS page_row_padding_id_check;

ALTER TABLE page_row
  ADD CONSTRAINT page_row_padding_id_check CHECK (padding_id IN ('NONE', 'SMALL', 'MEDIUM', 'LARGE'));

COMMIT;
