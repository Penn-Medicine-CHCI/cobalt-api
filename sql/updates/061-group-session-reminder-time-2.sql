BEGIN;
SELECT _v.register_patch('061-group-session-reminder-time-2', NULL, NULL);

-- This gets run after production deploy finishes successfully
ALTER TABLE institution DROP COLUMN group_session_reservation_default_reminder_time_of_day;
ALTER TABLE institution DROP COLUMN group_session_reservation_default_reminder_day_offset;

COMMIT;