BEGIN;
SELECT _v.register_patch('101-ic-updates', NULL, NULL);

INSERT INTO scheduling_system (scheduling_system_id, description) VALUES ('EPIC_FHIR', 'Epic (FHIR)');

-- Useful for institutions that want providers gated behind MyChart
CREATE TABLE provider_scheduling_strategy (
  provider_scheduling_strategy_id TEXT PRIMARY KEY,
  description TEXT NOT NULL
);

INSERT INTO provider_scheduling_strategy VALUES ('DEFAULT', 'Default');
INSERT INTO provider_scheduling_strategy VALUES ('MYCHART_ONLY', 'MyChart Only');

ALTER TABLE institution ADD COLUMN provider_scheduling_strategy_id TEXT NOT NULL REFERENCES provider_scheduling_strategy DEFAULT 'DEFAULT';

-- Is this account still active in the system?
ALTER TABLE account ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;

-- Active flag means we need to recreate idx_account_email_address so it only applies to active accounts
DROP INDEX idx_account_email_address;
CREATE UNIQUE INDEX idx_account_email_address ON account USING btree (lower((email_address)::text)) WHERE ((account_source_id)::text = 'EMAIL_PASSWORD'::text AND active=TRUE);

DROP VIEW v_account;

-- Add in WHERE active=TRUE to reflect new active column
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

-- TODO for IC: get rid of the insurance tracking, replace with just text fields

COMMIT;