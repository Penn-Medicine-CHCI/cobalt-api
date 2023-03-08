BEGIN;
SELECT _v.register_patch('061-group-session-reminder-time-1', NULL, NULL);

-- This is to replace these fields (to be deleted in 061-group-session-reminder-time-2 after prod deploy) -
-- group session reminders are now offset in hours before the session, not same-day at 9:30 AM.
--
-- group_session_reservation_default_reminder_time_of_day TIME NOT NULL DEFAULT '09:30';
-- group_session_reservation_default_reminder_day_offset INTEGER NOT NULL DEFAULT 0;

ALTER TABLE institution ADD COLUMN group_session_reservation_default_reminder_minutes_offset INTEGER NOT NULL DEFAULT 60;

COMMIT;