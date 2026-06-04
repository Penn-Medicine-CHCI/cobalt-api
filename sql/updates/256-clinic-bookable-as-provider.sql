BEGIN;
SELECT _v.register_patch('256-clinic-bookable-as-provider', NULL, NULL);

ALTER TABLE clinic ADD COLUMN IF NOT EXISTS bookable_as_provider BOOLEAN NOT NULL DEFAULT FALSE;

COMMIT;
