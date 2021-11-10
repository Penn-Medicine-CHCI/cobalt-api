BEGIN;
SELECT _v.register_patch('002-click-tracking', NULL, NULL);

ALTER TABLE activity_tracking 
ADD COLUMN session_tracking_id UUID,
ADD COLUMN context JSONB;

INSERT INTO activity_action
VALUES ('CREATE', 'Create');

INSERT INTO activity_type
VALUES
('APPOINTMENT', 'Appointment'),
('URL', 'URL');

END;