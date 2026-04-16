BEGIN;
SELECT _v.register_patch('255-custom-row-column-content-order', NULL, NULL);

ALTER TABLE page_row_column
  ADD COLUMN content_order_id TEXT NOT NULL DEFAULT 'IMAGE_THEN_TEXT',
  ADD CONSTRAINT page_row_column_content_order_id_check
    CHECK (content_order_id IN ('IMAGE_THEN_TEXT', 'TEXT_THEN_IMAGE'));

COMMIT;
