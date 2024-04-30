BEGIN;

ALTER TABLE study ADD COLUMN leave_first_check_in_open_until_started BOOLEAN NOT NULL DEFAULT FALSE;

COMMIT;