BEGIN;
SELECT _v.register_patch('198-data-sync', NULL, NULL);

CREATE OR REPLACE VIEW v_remote_tag
AS
SELECT rt.*
FROM remote_tag rt;

ALTER TABLE institution_content ADD COLUMN remote_data_flag BOOLEAN NOT NULL DEFAULT false;

COMMIT;


