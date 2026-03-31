BEGIN;
SELECT _v.register_patch('251-ip-geolocation-radius-double', NULL, NULL);

ALTER TABLE ip_geolocation
ALTER COLUMN radius TYPE DOUBLE PRECISION
USING radius::DOUBLE PRECISION;

COMMIT;
