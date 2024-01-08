BEGIN;
SELECT _v.register_patch('141-analytics', NULL, NULL);



-- Additional information at the study level for push and coordinator
ALTER TABLE study ADD COLUMN coordinator_availability TEXT; -- e.g. "Monday-Friday, 8am-5pm"

COMMIT;