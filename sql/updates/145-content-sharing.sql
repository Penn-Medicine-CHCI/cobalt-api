BEGIN;
SELECT _v.register_patch('145-content-sharing', NULL, NULL);

ALTER TABLE institution ADD COLUMN sharing_content BOOLEAN NOT NULL DEFAULT TRUE;

COMMIT;