BEGIN;
SELECT _v.register_patch('038-institution-account-source', NULL, NULL);

CREATE TABLE account_source_display_style (
	account_source_display_style_id TEXT PRIMARY KEY,
	description TEXT NOT NULL
);

INSERT INTO account_source_display_style (account_source_display_style_id, description) VALUES ('PRIMARY', 'Primary');
INSERT INTO account_source_display_style (account_source_display_style_id, description) VALUES ('SECONDARY', 'Secondary');
INSERT INTO account_source_display_style (account_source_display_style_id, description) VALUES ('TERTIARY', 'Tertiary');

ALTER TABLE institution_account_source ADD COLUMN account_source_display_style_id TEXT REFERENCES account_source_display_style NOT NULL DEFAULT 'PRIMARY';
ALTER TABLE institution_account_source ADD COLUMN display_order SMALLINT NOT NULL DEFAULT 1;
ALTER TABLE institution_account_source ADD COLUMN authentication_description TEXT;

UPDATE institution_account_source
SET authentication_description=a.description
FROM account_source a
WHERE institution_account_source.account_source_id=a.account_source_id;

COMMIT;