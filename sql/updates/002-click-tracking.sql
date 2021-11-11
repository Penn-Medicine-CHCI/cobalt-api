BEGIN;
SELECT _v.register_patch('002-click-tracking', NULL, NULL);

ALTER TABLE activity_tracking 
ADD COLUMN session_tracking_id UUID,
ADD COLUMN context JSONB;

UPDATE activity_tracking SET context = CAST ('{ "contentId": "' ||activity_key||'"}' AS JSONB)
WHERE activity_type_id = 'CONTENT';

ALTER TABLE activity_tracking 
DROP COLUMN activity_key;

INSERT INTO activity_action
VALUES 
('CREATE', 'Create'),
('SIGN_IN', 'Sign In'),
('CANCEL', 'Cancel');

INSERT INTO activity_type
VALUES
('APPOINTMENT', 'Appointment'),
('URL', 'URL'),
('ACCOUNT', 'Account');

END;