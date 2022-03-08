BEGIN;
SELECT _v.register_patch('017-account-session-for-intake', NULL, NULL);

ALTER TABLE appointment ADD intake_account_session_id UUID NULL REFERENCES account_session; 

END;