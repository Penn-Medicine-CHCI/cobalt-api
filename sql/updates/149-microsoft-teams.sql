BEGIN;
SELECT _v.register_patch('149-microsoft-teams', NULL, NULL);

INSERT INTO videoconference_platform (videoconference_platform_id, description) VALUES ('MICROSOFT_TEAMS','Microsoft Teams');

COMMIT;