BEGIN;
SELECT _v.register_patch('256-custom-row-column-content-order-view', NULL, NULL);

DROP VIEW IF EXISTS v_page_row_column;

CREATE VIEW v_page_row_column AS
SELECT pr.*, fu.url AS image_url
FROM page_row_column pr
LEFT OUTER JOIN file_upload fu ON pr.image_file_upload_id = fu.file_upload_id;

COMMIT;
