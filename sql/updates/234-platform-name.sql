BEGIN;
SELECT _v.register_patch('234-platform-name', NULL, NULL);

-- Permit whitelabeling of the platform name, e.g. "{{platformName}}, powered by Cobalt"
ALTER TABLE institution ADD COLUMN platform_name TEXT NOT NULL DEFAULT 'Cobalt';

COMMIT;