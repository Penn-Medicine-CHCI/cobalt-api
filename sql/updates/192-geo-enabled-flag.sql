BEGIN;
SELECT _v.register_patch('192-geo-enabled-flag', NULL, NULL);

ALTER TABLE institution ADD COLUMN google_geo_enabled BOOLEAN NOT NULL DEFAULT FALSE;

-- Just for testing
UPDATE institution SET google_geo_enabled = TRUE WHERE institution_id='COBALT_IC';

COMMIT;