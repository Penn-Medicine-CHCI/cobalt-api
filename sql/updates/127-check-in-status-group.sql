BEGIN;
SELECT _v.register_patch('127-check-in-status-group', NULL, NULL);

CREATE TABLE check_in_status_group
(check_in_status_group_id VARCHAR NOT NULL PRIMARY KEY,
description VARCHAR NOT NULL);

INSERT INTO check_in_status_group
(check_in_status_group_id, description)
VALUES
('TO_DO', 'To-Do'),
('PAST', 'Past');

ALTER TABLE check_in_status
ADD COLUMN check_in_status_group_id VARCHAR REFERENCES check_in_status_group;

UPDATE check_in_status
SET check_in_status_group_id = 'TO_DO';

UPDATE check_in_status
SET check_in_status_group_id = 'PAST'
WHERE check_in_status_id IN ('COMPLETE', 'EXPIRED');

ALTER TABLE check_in_status
ALTER COLUMN check_in_status_group_id SET NOT NULL;

ALTER TABLE study_check_in_action ADD COLUMN video_prompt VARCHAR NULL;
ALTER TABLE study_check_in_action ADD COLUMN video_script VARCHAR NULL;
ALTER TABLE study_check_in_action ADD COLUMN video_intro VARCHAR NULL;
ALTER TABLE study_check_in_action ADD COLUMN min_video_time_minutes INTEGER NULL;
ALTER TABLE study_check_in_action ADD COLUMN max_video_time_minutes INTEGER NULL;

UPDATE study_check_in_action SET video_prompt = 'Describe a stressful situation or event from the past week.' WHERE check_in_type_id = 'VIDEO';
UPDATE study_check_in_action SET video_script = 'Do you like amusement parks? Well, I sure do. To amuse myself, I went twice last spring. My most MEMORABLE moment was riding on the Caterpillar, which is a gigantic rollercoaster high above the ground. When I saw how high the Caterpillar rose into the bright blue sky I knew it was for me. After waiting in line for thirty minutes, I made it to the front where the man measured my height to see if I was tall enough. I gave the man my coins, asked for change, and jumped on the cart. Tick, tick, tick, the Caterpillar climbed slowly up the tracks. It went SO high I could see the parking lot. Boy was I SCARED! I thought to myself, “There’s no turning back now.” People were so scared they screamed as we swiftly zoomed fast, fast, and faster along the tracks. As quickly as it started, the Caterpillar came to a stop. Unfortunately, it was time to pack the car and drive home. That night I dreamt of the wild ride on the Caterpillar. Taking a trip to the amusement park and riding on the Caterpillar was my MOST memorable moment ever!' WHERE check_in_type_id = 'VIDEO';
UPDATE study_check_in_action SET video_intro = 'Please make sure your face is visible to the camera and that you are in a place without a lot of background noise before you begin. You will have five minutes to complete two tasks. Your recording must be at least three minutes and will automatically stop after 5 min.' WHERE check_in_type_id = 'VIDEO';
UPDATE study_check_in_action SET min_video_time_minutes = 3 WHERE check_in_type_id = 'VIDEO';
UPDATE study_check_in_action SET max_video_time_minutes = 5 WHERE check_in_type_id = 'VIDEO';

CREATE OR REPLACE VIEW v_account_check_in
AS
SELECT ac.*, a.account_id, sc.study_id, sc.check_in_number, cis.description as check_in_status_description,
cis.check_in_status_group_id
FROM account_check_in ac, account_study a, study_check_in sc, check_in_status cis
WHERE ac.account_study_id = a.account_study_id 
AND ac.study_check_in_id = sc.study_check_in_id
AND ac.check_in_status_id = cis.check_in_status_id
ORDER BY sc.check_in_number ASC;

CREATE OR REPLACE VIEW v_account_check_in_action
AS
SELECT ac.*, a.account_id, sci.study_id, sc.check_in_type_id,
cit.description as check_in_type_description, cis.description as check_in_action_status_description,
ss.screening_session_id, sc.screening_flow_id, sc.video_prompt, sc.video_script, video_intro, min_video_time_minutes, max_video_time_minutes
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
