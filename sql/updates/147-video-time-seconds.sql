BEGIN;
SELECT _v.register_patch('147-video-time-seconds', NULL, NULL);

ALTER TABLE study_check_in_action ADD COLUMN min_video_time_seconds INTEGER NULL;
ALTER TABLE study_check_in_action ADD COLUMN max_video_time_seconds INTEGER NULL;

UPDATE study_check_in_action SET min_video_time_seconds = min_video_time_minutes * 60, max_video_time_seconds = max_video_time_minutes * 60;

DROP VIEW v_account_check_in_action;

ALTER TABLE study_check_in_action DROP COLUMN min_video_time_minutes;
ALTER TABLE study_check_in_action DROP COLUMN max_video_time_minutes;

CREATE OR REPLACE VIEW v_account_check_in_action
AS
SELECT ac.*, a.account_id, sci.study_id, sc.check_in_type_id,
cit.description as check_in_type_description, cis.description as check_in_action_status_description,
ss.screening_session_id, sc.screening_flow_id, sc.video_prompt, sc.video_script, video_intro, min_video_time_seconds, max_video_time_seconds
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
ORDER BY aci.check_in_start_date_time, sc.action_order ASC;

COMMIT;