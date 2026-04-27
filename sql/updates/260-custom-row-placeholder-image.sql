BEGIN;
SELECT _v.register_patch('260-custom-row-placeholder-image', NULL, NULL);

ALTER TABLE page_row_column
  ADD COLUMN use_placeholder_image BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE page_row_column
SET use_placeholder_image = TRUE
WHERE image_file_upload_id IS NULL
  AND page_row_id IN (
    SELECT page_row_id
    FROM page_row
    WHERE row_type_id = 'CUSTOM_ROW'
  );

COMMIT;
