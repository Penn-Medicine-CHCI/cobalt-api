BEGIN;
SELECT _v.register_patch('052-account-capabilities', NULL, NULL);

-- No longer have the concept of "super administrator"
UPDATE account SET role_id='ADMINISTRATOR' WHERE role_id='SUPER_ADMINISTRATOR';
DELETE FROM role WHERE role_id IN ('SUPER_ADMINISTRATOR', 'BHS');

CREATE TABLE capability (
  capability_id VARCHAR PRIMARY KEY,
  description VARCHAR NOT NULL
);

INSERT INTO capability VALUES ('CONTENT_EDITOR', 'Content Editor');
INSERT INTO capability VALUES ('CONTENT_APPROVER', 'Content Approver');
INSERT INTO capability VALUES ('REPORT_PROVIDER_SLOT', 'Provider Slot Reporting');

-- Relates capabilities to accounts
CREATE TABLE account_capability (
	account_id UUID NOT NULL REFERENCES account,
	capability_id VARCHAR NOT NULL REFERENCES capability,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	PRIMARY KEY (account_id, capability_id)
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON account_capability FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

COMMIT;