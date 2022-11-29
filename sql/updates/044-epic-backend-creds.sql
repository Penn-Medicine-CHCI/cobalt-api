BEGIN;
SELECT _v.register_patch('044-epic-backend-creds', NULL, NULL);

CREATE TABLE epic_backend_service_auth_type (
	epic_backend_service_auth_type_id TEXT PRIMARY KEY,
	description TEXT NOT NULL
);

INSERT INTO epic_backend_service_auth_type VALUES ('UNSUPPORTED', 'Unsupported');
INSERT INTO epic_backend_service_auth_type VALUES ('OAUTH_20', 'OAuth 2.0');
INSERT INTO epic_backend_service_auth_type VALUES ('EMP_CREDENTIALS', 'EMP Credentials');

ALTER TABLE institution ADD COLUMN epic_backend_service_auth_type_id TEXT NOT NULL DEFAULT 'UNSUPPORTED' REFERENCES epic_backend_service_auth_type;

ALTER TABLE institution RENAME COLUMN mychart_token_url TO epic_token_url;
ALTER TABLE institution RENAME COLUMN mychart_authorize_url TO epic_authorize_url;

COMMIT;