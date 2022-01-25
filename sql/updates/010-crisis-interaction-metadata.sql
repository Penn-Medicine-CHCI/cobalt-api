BEGIN;
SELECT _v.register_patch('010-crisis-interaction-metadata', NULL, NULL);

ALTER TABLE interaction_instance ADD COLUMN hipaa_compliant_metadata JSONB NULL;

END;