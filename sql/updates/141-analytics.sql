BEGIN;
SELECT _v.register_patch('141-analytics', NULL, NULL);

ALTER TABLE account_source ADD COLUMN short_description TEXT;
ALTER TABLE institution_location ADD COLUMN short_name TEXT;
ALTER TABLE screening_flow ADD COLUMN analytics_name TEXT;

-- Additional information at the study level for push and coordinator
ALTER TABLE study ADD COLUMN coordinator_availability TEXT; -- e.g. "Monday-Friday, 8am-5pm"

COMMIT;