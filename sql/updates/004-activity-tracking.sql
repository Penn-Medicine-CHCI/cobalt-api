BEGIN;
SELECT _v.register_patch('004-activity-tracking', NULL, NULL);

ALTER TABLE activity_tracking ALTER COLUMN account_id DROP NOT NULL;

END;