BEGIN;
SELECT _v.register_patch('122-institution-adjustments', NULL, NULL);

ALTER TABLE institution ADD COLUMN tech_support_phone_number TEXT;

COMMIT;