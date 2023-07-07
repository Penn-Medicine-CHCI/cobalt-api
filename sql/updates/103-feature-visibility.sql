BEGIN;
SELECT _v.register_patch('103-feature-visibility', NULL, NULL);

INSERT INTO feature (feature_id, navigation_header_id, name, url_name) VALUES
	('PSYCHOLOGIST', 'CONNECT_WITH_SUPPORT', 'Psychologist', '/connect-with-support/psychologist'),
	('PSYCHIATRIST', 'CONNECT_WITH_SUPPORT', 'Psychiatrist', '/connect-with-support/psychiatrist'),
	('LCSW', 'CONNECT_WITH_SUPPORT', 'LCSW', '/connect-with-support/lcsw');

INSERT INTO feature_support_role(feature_id, support_role_id) VALUES
	('PSYCHOLOGIST', 'PSYCHOLOGIST'),
	('PSYCHIATRIST', 'PSYCHIATRIST'),
	('LCSW', 'LCSW');

INSERT INTO feature_filter(feature_id, filter_id) VALUES
	('PSYCHOLOGIST', 'DATE'),
	('PSYCHOLOGIST', 'TIME_OF_DAY'),
	('PSYCHIATRIST', 'DATE'),
	('PSYCHIATRIST', 'TIME_OF_DAY'),
	('LCSW', 'DATE'),
	('LCSW', 'TIME_OF_DAY');

ALTER TABLE institution_feature ALTER COLUMN nav_description DROP NOT NULL;
ALTER TABLE institution_feature ADD COLUMN nav_visible BOOLEAN NOT NULL DEFAULT TRUE;

-- Preparation for altering account table
DROP VIEW v_account;

ALTER TABLE account RENAME COLUMN epic_patient_id TO _deprecated_epic_patient_id;
ALTER TABLE account RENAME COLUMN epic_patient_id_type TO _deprecated_epic_patient_id_type;
ALTER TABLE account RENAME COLUMN epic_patient_created_by_cobalt TO _deprecated_epic_patient_created_by_cobalt;

-- Pick up the renamed columns in the view
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

-- Should be populated for EPIC_FHIR providers
ALTER TABLE provider ADD COLUMN epic_practitioner_fhir_id TEXT;

COMMIT;