BEGIN;
SELECT _v.register_patch('047-user-submitted-flags', NULL, NULL);

ALTER TABLE institution ADD COLUMN user_submitted_content_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE institution ADD COLUMN user_submitted_group_session_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE institution ADD COLUMN user_submitted_group_session_request_enabled BOOLEAN NOT NULL DEFAULT FALSE;

COMMIT;