BEGIN;
SELECT _v.register_patch('012-fingerprinting', NULL, NULL);

CREATE TABLE account_fingerprint (
	account_id UUID NOT NULL REFERENCES account,
	fingerprint_id VARCHAR NOT NULL,
	last_updated timestamptz NOT NULL DEFAULT now(),
	CONSTRAINT account_fingerprint_key PRIMARY KEY(account_id, fingerprint_id)
);

END;