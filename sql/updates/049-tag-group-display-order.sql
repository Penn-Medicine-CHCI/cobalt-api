BEGIN;
SELECT _v.register_patch('049-tag-group-display-order', NULL, NULL);

ALTER TABLE tag_group ADD COLUMN display_order SMALLINT NOT NULL DEFAULT 1;

COMMIT;