BEGIN;
SELECT _v.register_patch('150-tag-content', NULL, NULL);

ALTER TABLE tag_content
DROP COLUMN institution_id;

COMMIT;