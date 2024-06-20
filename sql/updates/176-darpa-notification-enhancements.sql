BEGIN;
SELECT _v.register_patch('176-darpa-notification-enhancements', NULL, NULL);

DROP VIEW v_account_check_in;

CREATE OR REPLACE VIEW v_account_check_in
AS
SELECT ac.*, a.account_id, sc.study_id, sc.check_in_number, cis.description as check_in_status_description,
cis.check_in_status_group_id, a.study_started, acc.institution_id, acc.time_zone
FROM account_check_in ac, account_study a, study_check_in sc, check_in_status cis, account acc
WHERE ac.account_study_id = a.account_study_id 
AND ac.study_check_in_id = sc.study_check_in_id
AND ac.check_in_status_id = cis.check_in_status_id
AND a.account_id = acc.account_id
AND a.deleted = false
ORDER BY sc.check_in_number ASC;

DROP VIEW v_account_study;

CREATE OR REPLACE VIEW v_account_study
AS
SELECT a.*, ac.institution_id, ac.time_zone,  ac.password_reset_required
FROM account_study a, study s, account ac
WHERE a.study_id = s.study_id
AND a.account_id = ac.account_id
AND deleted = false;

ALTER TABLE account_study_scheduled_message RENAME TO account_study_scheduled_message_depricated;

CREATE TABLE study_check_in_reminder
(study_check_in_reminder_id UUID NOT NULL PRIMARY KEY DEFAULT uuid_generate_v4(),
 study_id UUID NOT NULL REFERENCES study,
 check_in_reminder_notification_minutes INTEGER NOT NULL,
 check_in_reminder_notification_message_title VARCHAR NOT NULL, 
 check_in_reminder_notification_message_body  VARCHAR NOT NULL, 
 created timestamptz NOT NULL DEFAULT now(),
 last_updated timestamptz NOT NULL
);

create trigger set_last_updated before
insert or update on study_check_in_reminder for each row execute procedure set_last_updated();

CREATE TABLE account_check_in_reminder
(account_check_in_reminder_id UUID NOT NULL PRIMARY KEY DEFAULT uuid_generate_v4(),
account_check_in_id UUID NOT NULL REFERENCES account_check_in,
study_check_in_reminder_id UUID NOT NULL REFERENCES study_check_in_reminder,
scheduled_message_id UUID NOT NULL REFERENCES scheduled_message,
created timestamptz NOT NULL DEFAULT now(),
last_updated timestamptz NOT NULL
);

create trigger set_last_updated before
insert or update on account_check_in_reminder for each row execute procedure set_last_updated();

ALTER TABLE study
DROP COLUMN check_in_reminder_notification_minutes,
DROP COLUMN check_in_reminder_notification_message_title,
DROP COLUMN check_in_reminder_notification_message_body,
DROP COLUMN max_check_in_reminder;

CREATE OR REPLACE VIEW v_study_check_in_reminder
AS
SELECT sc.*, s.check_in_windows_fixed, s.send_check_in_reminder_notification
FROM study s, study_check_in_reminder sc 
WHERE s.study_id = sc.study_id;


CREATE OR REPLACE FUNCTION delete_study_check_ins(studyId UUID,
												 checkInNumbers INTEGER[],
												 deleteStudy BOOLEAN)
RETURNS VOID AS '
BEGIN

UPDATE screening_session SET account_check_in_action_id = null
WHERE account_check_in_action_id IN
(SELECT aci.account_check_in_action_id
FROM account_check_in_action aci 
WHERE account_check_in_id IN
(SELECT ac.account_check_in_id 
FROM account_check_in ac 
WHERE study_check_in_id  IN
(SELECT sc.study_check_in_id
FROM study_check_in sc
WHERE study_id=studyId
AND check_in_number = ANY (checkInNumbers))));

UPDATE screening_session SET account_check_in_action_id = null
WHERE account_check_in_action_id IN
(SELECT aci.account_check_in_action_id
FROM account_check_in_action aci 
WHERE account_check_in_id IN
(SELECT ac.account_check_in_id 
FROM account_check_in ac 
WHERE study_check_in_id  IN
(SELECT sc.study_check_in_id
FROM study_check_in sc
WHERE study_id=studyId
AND check_in_number = ANY (checkInNumbers))));

DELETE FROM account_check_in_action_file_upload
WHERE account_check_in_action_id IN
(SELECT aci.account_check_in_action_id
FROM account_check_in_action aci 
WHERE account_check_in_id IN
(SELECT ac.account_check_in_id 
FROM account_check_in ac 
WHERE study_check_in_id  IN
(SELECT sc.study_check_in_id
FROM study_check_in sc
WHERE study_id=studyId
AND check_in_number = ANY (checkInNumbers))));

DELETE FROM account_check_in_action_scheduled_message
WHERE account_check_in_action_id IN
(SELECT aci.account_check_in_action_id
FROM account_check_in_action aci 
WHERE account_check_in_id IN
(SELECT ac.account_check_in_id 
FROM account_check_in ac 
WHERE study_check_in_id  IN
(SELECT sc.study_check_in_id
FROM study_check_in sc
WHERE study_id=studyId
AND check_in_number = ANY (checkInNumbers))));

DELETE FROM account_check_in_action
WHERE account_check_in_id IN
(SELECT ac.account_check_in_id 
FROM account_check_in ac 
WHERE study_check_in_id  IN
(SELECT sc.study_check_in_id
FROM study_check_in sc
WHERE study_id=studyId
AND check_in_number = ANY (checkInNumbers)));

DELETE FROM account_check_in
WHERE study_check_in_id IN
(SELECT sc.study_check_in_id
FROM study_check_in sc
WHERE study_id=studyId
AND check_in_number = ANY (checkInNumbers));

DELETE FROM study_check_in_action
WHERE study_check_in_id IN
(SELECT sc.study_check_in_id
FROM study_check_in sc
WHERE study_id=studyId
AND check_in_number = ANY (checkInNumbers));

DELETE FROM study_check_in
WHERE study_id=studyId
AND check_in_number = ANY (checkInNumbers);

IF (deleteStudy) THEN
	DELETE FROM account_check_in_reminder
	WHERE account_study_id IN 
	(SELECT a.account_study_id 
	FROM account_study a
	WHERE study_id = studyId);

	DELETE FROM study_file_upload
	WHERE study_id = studyId;
	
	DELETE FROM study_beiwe_config
	WHERE study_id=studyId;

	DELETE FROM study_account_source
	WHERE study_id=studyId;

	DELETE FROM account_study 
	WHERE study_id=studyId;

	DELETE FROM study 
	WHERE study_id=studyId;
END IF;

END;
' LANGUAGE plpgsql;

END;

COMMIT;