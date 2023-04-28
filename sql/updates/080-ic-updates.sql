BEGIN;
SELECT _v.register_patch('080-ic-updates', NULL, NULL);

ALTER TABLE institution ADD COLUMN integrated_care_phone_number TEXT;
ALTER TABLE institution ADD COLUMN integrated_care_availability_description TEXT;

COMMIT;