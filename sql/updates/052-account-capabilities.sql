BEGIN;
SELECT _v.register_patch('052-account-capabilities', NULL, NULL);

CREATE TABLE capability (
  capability_id VARCHAR PRIMARY KEY,
  description VARCHAR NOT NULL
);

INSERT INTO capability VALUES ('CONTENT_EDITOR', 'Content Editor');
INSERT INTO capability VALUES ('CONTENT_APPROVER', 'Content Approver');
INSERT INTO capability VALUES ('REPORT_PROVIDER_SLOT', 'Provider Slot Reporting');

-- Relates capabilities to accounts
CREATE TABLE account_capability (
	account_id UUID NOT NULL REFERENCES tag,
	capability_id VARCHAR NOT NULL REFERENCES capability,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	PRIMARY KEY (account_id, capability_id)
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON account_capability FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

COMMIT;