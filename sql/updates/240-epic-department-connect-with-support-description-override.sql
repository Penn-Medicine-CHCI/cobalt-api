BEGIN;
SELECT _v.register_patch('240-epic-department-connect-with-support-description-override', NULL, NULL);

-- Add optional override copy for /connect-with-support/mhp messaging
ALTER TABLE epic_department ADD COLUMN connect_with_support_description_override TEXT;

COMMIT;