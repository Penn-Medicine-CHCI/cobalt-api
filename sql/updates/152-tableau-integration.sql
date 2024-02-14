BEGIN;
SELECT _v.register_patch('152-tableau-integration', NULL, NULL);

ALTER TABLE institution ADD COLUMN tableau_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE institution ADD COLUMN tableau_client_id TEXT;
ALTER TABLE institution ADD COLUMN tableau_api_base_url TEXT; -- e.g. https://prod-useast-a.online.tableau.com
ALTER TABLE institution ADD COLUMN tableau_content_url TEXT;
ALTER TABLE institution ADD COLUMN tableau_email_address TEXT;

-- Note: Tableau secret ID and secret value are stored in Secrets Manager

COMMIT;