BEGIN;
SELECT _v.register_patch('143-content-admin', NULL, NULL);

ALTER TABLE file_upload ADD COLUMN filesize NUMERIC;

COMMIT;
