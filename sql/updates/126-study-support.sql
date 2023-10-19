BEGIN;
SELECT _v.register_patch('126-study-support', NULL, NULL);

--Base table for study metadata
CREATE TABLE study
(study_id UUID NOT NULL PRIMARY KEY,
 name VARCHAR NOT NULL,
 minutes_between_check_ins INTEGER NOT NULL,
 grace_period_in_minutes INTEGER NOT NULL,
 created timestamptz NOT NULL DEFAULT now(),
 last_updated timestamptz NOT NULL
);

create trigger set_last_updated before
insert or update on study for each row execute procedure set_last_updated();


CREATE TABLE study_check_in
(study_check_in_id UUID NOT NULL PRIMARY KEY,
 study_id UUID NOT NULL REFERENCES study,
 check_in_number INTEGER NOT NULL,
 created timestamptz NOT NULL DEFAULT now(),
 last_updated timestamptz NOT NULL
);

create trigger set_last_updated before
insert or update on study_check_in for each row execute procedure set_last_updated();

CREATE TABLE check_in_type
(check_in_type_id VARCHAR NOT NULL PRIMARY KEY,
 description VARCHAR NOT NULL);

CREATE TABLE study_check_in_action
(study_check_in_action_id UUID NOT NULL PRIMARY KEY,
 study_check_in_id UUID NOT NULL REFERENCES study_check_in,
 check_in_type_id VARCHAR NOT NULL REFERENCES check_in_type,
 action_order INTEGER NOT NULL,
 screening_flow_id UUID NULL,
 created timestamptz NOT NULL DEFAULT now(),
 last_updated timestamptz NOT NULL
);

create trigger set_last_updated before
insert or update on study_check_in_action for each row execute procedure set_last_updated();

CREATE TABLE account_study
(account_study_id UUID NOT NULL PRIMARY KEY,
 account_id UUID NOT NULL REFERENCES account,
 study_id UUID NOT NULL REFERENCES study,
 created timestamptz NOT NULL DEFAULT now(),
 last_updated timestamptz NOT NULL
 );

create trigger set_last_updated before
insert or update on account_study for each row execute procedure set_last_updated();

CREATE UNIQUE INDEX idx_account_study_account_id
ON account_study (account_id, study_id);

CREATE TABLE account_check_in
(account_check_in_id UUID NOT NULL PRIMARY KEY,
 account_study_id UUID NOT NULL REFERENCES account_study,
 study_check_in_id UUID NOT NULL REFERENCES study_check_in,
 check_in_start_date_time timestamp NOT NULL,
 check_in_end_date_time timestamp NOT NULL,
 completed_flag BOOLEAN NOT NULL DEFAULT false,
 expired_flag BOOLEAN NOT NULL DEFAULT false,
 completed_date timestamp NULL,
 created timestamptz NOT NULL DEFAULT now(),
 last_updated timestamptz NOT NULL
);

create trigger set_last_updated before
insert or update on account_check_in for each row execute procedure set_last_updated();

CREATE TABLE check_in_action_status
(check_in_action_status_id VARCHAR NOT NULL PRIMARY KEY DEFAULT 'INCOMPLETE',
description VARCHAR NOT NULL);

CREATE TABLE account_check_in_action
(account_check_in_action_id UUID NOT NULL PRIMARY KEY,
 account_check_in_id UUID NOT NULL REFERENCES account_check_in,
 study_check_in_action_id UUID NOT NULL REFERENCES study_check_in_action,
 check_in_action_status_id VARCHAR NOT NULL REFERENCES check_in_action_status,
 created timestamptz NOT NULL DEFAULT now(),
 last_updated timestamptz NOT NULL
);

create trigger set_last_updated before
insert or update on account_check_in_action for each row execute procedure set_last_updated();

ALTER TABLE screening_session ADD COLUMN account_check_in_action_id UUID NULL REFERENCES account_check_in_action;

INSERT INTO check_in_type
(check_in_type_id, description)
VALUES
('VIDEO','Record Video'),
('SCREENING', 'Take the assessment');

INSERT INTO check_in_action_status
(check_in_action_status_id, description)
VALUES
('INCOMPLETE', 'Incomplete'),
('IN_PROGRESS', 'In progress'),
('FAILED', 'Failed'),
('COMPLETE', 'Complete');

CREATE OR REPLACE VIEW v_account_check_in
AS
SELECT ac.*, a.account_id, sc.study_id, sc.check_in_number
FROM account_check_in ac, account_study a, study_check_in sc 
WHERE ac.account_study_id = a.account_study_id 
AND ac.study_check_in_id = sc.study_check_in_id
ORDER BY ac.check_in_start_date_time ASC;

CREATE OR REPLACE VIEW v_account_check_in_action
AS
SELECT ac.*, a.account_id, sci.study_id, sc.check_in_type_id,
cit.description as check_in_type_description, cis.description as check_in_status_description,
ss.screening_session_id, sc.screening_flow_id
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




