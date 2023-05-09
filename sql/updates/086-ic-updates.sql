BEGIN;
SELECT _v.register_patch('086-ic-updates', NULL, NULL);

ALTER TABLE institution ADD COLUMN mychart_default_url TEXT;

COMMIT;