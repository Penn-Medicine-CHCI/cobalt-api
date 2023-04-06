BEGIN;
SELECT _v.register_patch('075-provider-description', NULL, NULL);

ALTER TABLE provider ADD COLUMN description VARCHAR;

COMMIT;