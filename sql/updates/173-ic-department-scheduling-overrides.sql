BEGIN;
SELECT _v.register_patch('173-ic-department-scheduling-overrides', NULL, NULL);

-- To handle scenario where an order comes in for department A but scheduling happens in department B.
-- The epic_department record for A should have its scheduling_override_epic_department_id set to the ID for department B
ALTER TABLE epic_department ADD COLUMN scheduling_override_epic_department_id UUID REFERENCES epic_department(epic_department_id);

COMMIT;