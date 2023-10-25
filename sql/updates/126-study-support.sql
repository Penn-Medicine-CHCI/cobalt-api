BEGIN;
SELECT _v.register_patch('126-study-support', NULL, NULL);

--Base table for study metadata
CREATE TABLE study
(study_id UUID NOT NULL PRIMARY KEY,
 institution_id VARCHAR NOT NULL REFERENCES institution,
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

CREATE TABLE check_in_status
(check_in_status_id VARCHAR NOT NULL PRIMARY KEY,
description VARCHAR NOT NULL);

CREATE TABLE account_check_in
(account_check_in_id UUID NOT NULL PRIMARY KEY,
 account_study_id UUID NOT NULL REFERENCES account_study,
 study_check_in_id UUID NOT NULL REFERENCES study_check_in,
 check_in_start_date_time timestamp NOT NULL,
 check_in_end_date_time timestamp NOT NULL,
 check_in_status_id VARCHAR NOT NULL REFERENCES check_in_status DEFAULT 'NOT_STARTED',
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

ALTER TABLE account ADD COLUMN username VARCHAR NULL;

ALTER TABLE account ADD COLUMN password_reset_required BOOLEAN NOT NULL DEFAULT FALSE;

INSERT INTO account_source
(account_source_id, description)
VALUES
('USERNAME', 'Username');

INSERT INTO institution_account_source
(institution_account_source_id, institution_id, account_source_id, account_source_display_style_id, display_order, authentication_description, visible)
VALUES
(uuid_generate_v4(), 'COBALT', 'USERNAME', 'TERTIARY', 3, 'Username', false);

INSERT INTO check_in_type
(check_in_type_id, description)
VALUES
('VIDEO','Record Video'),
('SCREENING', 'Take the Assessment');

INSERT INTO check_in_status
(check_in_status_id, description)
VALUES
('NOT_STARTED', 'Not Started'),
('IN_PROGRESS', 'In Progress'),
('COMPLETE', 'Complete'),
('EXPIRED', 'Expired');

INSERT INTO check_in_action_status
(check_in_action_status_id, description)
VALUES
('INCOMPLETE', 'Incomplete'),
('IN_PROGRESS', 'In Progress'),
('FAILED', 'Failed'),
('COMPLETE', 'Complete');

INSERT INTO screening_flow_type
(screening_flow_type_id, description)
VALUES
('STUDY', 'Study');

CREATE OR REPLACE VIEW v_account_check_in
AS
SELECT ac.*, a.account_id, sc.study_id, sc.check_in_number, cis.description as check_in_status_description
FROM account_check_in ac, account_study a, study_check_in sc, check_in_status cis 
WHERE ac.account_study_id = a.account_study_id 
AND ac.study_check_in_id = sc.study_check_in_id
AND ac.check_in_status_id = cis.check_in_status_id
ORDER BY sc.check_in_number ASC;

CREATE OR REPLACE VIEW v_account_check_in_action
AS
SELECT ac.*, a.account_id, sci.study_id, sc.check_in_type_id,
cit.description as check_in_type_description, cis.description as check_in_action_status_description,
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

DROP VIEW v_account;

-- Pick-up new column on account table
CREATE VIEW v_account AS
WITH account_capabilities_query AS (
	 -- Collect the capability types for each account
	 SELECT
			 account_id,
			 jsonb_agg(account_capability_type_id) as account_capability_type_ids
	 FROM
			 account_capability
  GROUP BY account_id
)
SELECT a.*, acq.account_capability_type_ids
FROM account a LEFT OUTER JOIN account_capabilities_query acq on a.account_id=acq.account_id
WHERE active=TRUE;

COMMIT;




