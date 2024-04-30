BEGIN;
SELECT _v.register_patch('170-study-first-check-in-behavior', NULL, NULL);

ALTER TABLE study ADD COLUMN leave_first_check_in_open_until_started BOOLEAN NOT NULL DEFAULT FALSE;

COMMIT;