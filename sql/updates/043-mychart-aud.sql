BEGIN;
SELECT _v.register_patch('043-mychart-aud', NULL, NULL);

ALTER TABLE institution ADD COLUMN mychart_aud TEXT;

COMMIT;