BEGIN;
SELECT _v.register_patch('199-message-status-sending', NULL, NULL);

INSERT INTO message_status (message_status_id, description) VALUES ('SENDING', 'Sending');

COMMIT;


