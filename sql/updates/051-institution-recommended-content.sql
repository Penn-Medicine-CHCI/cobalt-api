BEGIN;
SELECT _v.register_patch('051-institution-recommended-content', NULL, NULL);

ALTER TABLE institution ADD COLUMN recommended_content_enabled BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE institution SET recommended_content_enabled=TRUE where institution_id='COBALT';

COMMIT;