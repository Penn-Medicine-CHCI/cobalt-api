BEGIN;
SELECT _v.register_patch('230-institution-about', NULL, NULL);

ALTER TABLE institution ADD COLUMN about_page_enabled BOOLEAN NOT NULL DEFAULT FALSE;

END;