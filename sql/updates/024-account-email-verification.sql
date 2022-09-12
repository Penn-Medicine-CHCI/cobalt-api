BEGIN;
SELECT _v.register_patch('024-account-email-verification', NULL, NULL);

CREATE TABLE account_email_verification (
	account_email_verification_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	account_id UUID NOT NULL REFERENCES account,
	code TEXT NOT NULL, -- for verification, e.g. a 6-digit random number
	email_address TEXT NOT NULL,
	verified BOOLEAN NOT NULL DEFAULT FALSE,
	expiration TIMESTAMPTZ NOT NULL DEFAULT NOW() + (10 ||' minutes')::INTERVAL,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON account_email_verification FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

COMMIT;