BEGIN;
SELECT _v.register_patch('040-epic-mychart-refactor', NULL, NULL);

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

insert into account_source (account_source_id, description) values ('MYCHART', 'MyChart');

COMMIT;