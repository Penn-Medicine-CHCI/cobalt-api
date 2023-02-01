BEGIN;
SELECT _v.register_patch('056-group-session-requests-disable', NULL, NULL);

ALTER TABLE institution ADD COLUMN group_session_requests_enabled BOOLEAN NOT NULL DEFAULT FALSE;

COMMIT;