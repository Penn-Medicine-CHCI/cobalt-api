BEGIN;
SELECT _v.register_patch('205-page-tag', NULL, NULL);

ALTER TABLE page_row_tag_group ADD COLUMN tag_id VARCHAR NULL REFERENCES tag;

COMMIT;