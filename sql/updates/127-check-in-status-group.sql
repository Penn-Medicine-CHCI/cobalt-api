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

CREATE OR REPLACE VIEW v_account_check_in
AS
SELECT ac.*, a.account_id, sc.study_id, sc.check_in_number, cis.description as check_in_status_description,
cis.check_in_status_group_id
FROM account_check_in ac, account_study a, study_check_in sc, check_in_status cis
WHERE ac.account_study_id = a.account_study_id 
AND ac.study_check_in_id = sc.study_check_in_id
AND ac.check_in_status_id = cis.check_in_status_id
ORDER BY sc.check_in_number ASC;

COMMIT;
