BEGIN;
SELECT _v.register_patch('160-study-reset', NULL, NULL);

ALTER TABLE study ADD COLUMN reset_after_final_check_in BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE account_study ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;

DROP VIEW v_account_study;

CREATE OR REPLACE VIEW v_account_study
AS
SELECT a.*, ac.institution_id, ac.time_zone, s.send_check_in_reminder_notification, s.check_in_reminder_notification_minutes,
s.check_in_reminder_notification_message_title, s.check_in_reminder_notification_message_body, ac.password_reset_required
FROM account_study a, study s, account ac
WHERE a.study_id = s.study_id
AND a.account_id = ac.account_id
AND deleted = false;

DROP VIEW v_account_check_in;

CREATE OR REPLACE VIEW v_account_check_in
AS
SELECT ac.*, a.account_id, sc.study_id, sc.check_in_number, cis.description as check_in_status_description,
cis.check_in_status_group_id
FROM account_check_in ac, account_study a, study_check_in sc, check_in_status cis
WHERE ac.account_study_id = a.account_study_id 
AND ac.study_check_in_id = sc.study_check_in_id
AND ac.check_in_status_id = cis.check_in_status_id
AND a.deleted = false
ORDER BY sc.check_in_number ASC;

DROP VIEW v_account_check_in_action;

CREATE OR REPLACE VIEW v_account_check_in_action
AS
SELECT ac.*, a.account_id, sci.study_id, sc.check_in_type_id,
cit.description as check_in_type_description, cis.description as check_in_action_status_description,
ss.screening_session_id, sc.screening_flow_id, sc.video_prompt, sc.video_script, video_intro, min_video_time_seconds, max_video_time_seconds,
sc.send_followup_notification, sc.followup_notification_message_title, sc.followup_notification_message_body,
sc.followup_notification_minutes
FROM account_check_in_action ac
LEFT OUTER JOIN screening_session ss ON ac.account_check_in_action_id = ss.account_check_in_action_id,
study_check_in_action sc, study_check_in sci, account_check_in aci,
check_in_type cit, check_in_action_status cis, account_study a
WHERE ac.study_check_in_action_id = sc.study_check_in_action_id
AND sc.study_check_in_id = sci.study_check_in_id
AND ac.account_check_in_id = aci.account_check_in_id
AND sc.check_in_type_id = cit.check_in_type_id
AND ac.check_in_action_status_id = cis.check_in_action_status_id
AND aci.account_study_id = a.account_study_id
AND a.deleted = false
ORDER BY aci.check_in_start_date_time, sc.action_order ASC;

DROP INDEX idx_account_study_account_id;

CREATE UNIQUE INDEX IF NOT EXISTS idx_tag_content_content_id_tag_id ON tag_content (tag_id, content_id);

COMMIT;