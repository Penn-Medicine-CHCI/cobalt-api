BEGIN;
SELECT _v.register_patch('171-study-check-in-windows-fixed', NULL, NULL);

-- Used to determine if the study has a set of fixed check in start/end dates or if the start/end dates
-- are rescheduled after a check in is completed.
ALTER TABLE study ADD COLUMN check_in_windows_fixed BOOLEAN NOT NULL DEFAULT TRUE;

COMMIT;