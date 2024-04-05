BEGIN;
SELECT _v.register_patch('161-tableau', NULL, NULL);

ALTER TABLE institution ADD COLUMN tableau_view_name TEXT;
ALTER TABLE institution ADD COLUMN tableau_report_name TEXT;

COMMIT;