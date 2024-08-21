BEGIN;
SELECT _v.register_patch('181-data-sync', NULL, NULL);

ALTER TABLE content ADD COLUMN remote_data_flag BOOLEAN NOT NULL DEFAULT false;

COMMIT;