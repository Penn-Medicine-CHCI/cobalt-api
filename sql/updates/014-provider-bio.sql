BEGIN;
SELECT _v.register_patch('014-provider-bio', NULL, NULL);

ALTER TABLE provider ADD bio VARCHAR NULL;

END;