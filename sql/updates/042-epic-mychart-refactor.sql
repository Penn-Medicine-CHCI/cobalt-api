BEGIN;
SELECT _v.register_patch('042-epic-mychart-refactor', NULL, NULL);

-- Institution-level changes

ALTER TABLE institution ADD COLUMN integrated_care_enabled BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE institution ADD COLUMN epic_client_id TEXT;
ALTER TABLE institution ADD COLUMN epic_user_id TEXT; -- e.g. COBALT
ALTER TABLE institution ADD COLUMN epic_user_id_type TEXT; -- e.g. EXTERNAL
ALTER TABLE institution ADD COLUMN epic_username TEXT;
ALTER TABLE institution ADD COLUMN epic_password TEXT;
ALTER TABLE institution ADD COLUMN epic_base_url TEXT;
ALTER TABLE institution ADD COLUMN mychart_client_id TEXT;
ALTER TABLE institution ADD COLUMN mychart_scope TEXT;
ALTER TABLE institution ADD COLUMN mychart_response_type TEXT;
ALTER TABLE institution ADD COLUMN mychart_token_url TEXT;
ALTER TABLE institution ADD COLUMN mychart_authorize_url TEXT;
ALTER TABLE institution ADD COLUMN mychart_callback_url TEXT;

INSERT INTO account_source (account_source_id, description) VALUES ('MYCHART', 'MyChart');

-- Account-level changes

CREATE TABLE gender (
	gender_id TEXT PRIMARY KEY,
	description TEXT NOT NULL
);

-- See https://www.csusm.edu/ipa/surveys/inclusive-language-guidelines.html
INSERT INTO gender (gender_id, description) VALUES ('UNSPECIFIED', 'Unspecified');
INSERT INTO gender (gender_id, description) VALUES ('MALE', 'Male');
INSERT INTO gender (gender_id, description) VALUES ('FEMALE', 'Female');
INSERT INTO gender (gender_id, description) VALUES ('TRANSGENDER', 'Transgender');
INSERT INTO gender (gender_id, description) VALUES ('NONBINARY', 'Non-binary/non-conforming');
INSERT INTO gender (gender_id, description) VALUES ('PRIVATE', 'Prefer not to respond');

ALTER TABLE account ADD COLUMN gender_id TEXT REFERENCES gender NOT NULL DEFAULT 'UNSPECIFIED';

COMMIT;