BEGIN;
SELECT _v.register_patch('226-screening-flow-version-account-source', NULL, NULL);

-- If records exist in this table, it means the screening flow version requires an account with one of the specified account sources.
-- This allows us to gatekeep screening flows, e.g. require non-anonymous accounts.
CREATE TABLE screening_flow_version_account_source (
	screening_flow_version_id UUID NOT NULL REFERENCES screening_flow_version,
	account_source_id TEXT NOT NULL REFERENCES account_source,
	display_order SMALLINT NOT NULL,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	PRIMARY KEY (screening_flow_version_id, account_source_id),
	UNIQUE (screening_flow_version_id, display_order)
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON screening_flow_version_account_source FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

COMMIT;