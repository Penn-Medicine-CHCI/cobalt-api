BEGIN;
SELECT _v.register_patch('053-account-tags', NULL, NULL);

-- Relates tags directly to an account.
--
-- Short-term, this is to store off old per-account content assessment answers so we can bring them forward
-- to fit into the new tagging scheme (our content recommendation engine can query this table).
--
-- Long-term, in a future release, we might want to let users specify "I always want to see SLEEP content"
-- and this could help drive that.
CREATE TABLE tag_account (
	tag_account_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	tag_id VARCHAR NOT NULL REFERENCES tag,
	account_id UUID NOT NULL REFERENCES account,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON tag_account FOR EACH ROW EXECUTE PROCEDURE set_last_updated();
CREATE UNIQUE INDEX tag_account_unique_idx ON tag_account USING btree (tag_id, account_id);

COMMIT;