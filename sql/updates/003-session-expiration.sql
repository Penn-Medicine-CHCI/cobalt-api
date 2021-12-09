BEGIN;
SELECT _v.register_patch('003-session-expiration', NULL, NULL);

ALTER TABLE institution
ADD COLUMN access_token_expiration_in_minutes INT8 NOT NULL DEFAULT 43200,
ADD COLUMN access_token_short_expiration_in_minutes INT8 NOT NULL DEFAULT 43200,
ADD COLUMN anon_access_token_expiration_in_minutes INT8 NOT NULL DEFAULT 525600,
ADD COLUMN anon_access_token_short_expiration_in_minutes INT8 NOT NULL DEFAULT 525600;

ALTER TABLE account
ADD COLUMN access_token_expiration_in_minutes INT8 NULL,
ADD COLUMN access_token_short_expiration_in_minutes INT8 NULL;

ALTER TABLE account_login_rule
ADD COLUMN access_token_expiration_in_minutes INT8 NULL,
ADD COLUMN access_token_short_expiration_in_minutes INT8 NULL,
ADD COLUMN login_rule_executed BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN login_rule_execution_time timestamptz NULL;

END;