BEGIN;
SELECT _v.register_patch('170-study-first-check-in-behavior', NULL, NULL);

ALTER TABLE study ADD COLUMN leave_first_check_in_open_until_started BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE account_study ADD COLUMN study_started BOOLEAN DEFAULT false;

DROP VIEW v_account_study;

CREATE OR REPLACE VIEW v_account_study
AS
SELECT a.*, ac.institution_id, ac.time_zone, s.send_check_in_reminder_notification, s.check_in_reminder_notification_minutes,
s.check_in_reminder_notification_message_title, s.check_in_reminder_notification_message_body, ac.password_reset_required
FROM account_study a, study s, account ac
WHERE a.study_id = s.study_id
AND a.account_id = ac.account_id
AND deleted = false;


COMMIT;