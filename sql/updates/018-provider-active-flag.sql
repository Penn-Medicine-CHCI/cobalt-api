BEGIN;
SELECT _v.register_patch('018-provider-active-flag', NULL, NULL);

UPDATE provider SET active=FALSE WHERE active IS NULL;
ALTER TABLE provider ALTER COLUMN active SET NOT NULL;
ALTER TABLE provider ALTER COLUMN active SET DEFAULT TRUE;

END;