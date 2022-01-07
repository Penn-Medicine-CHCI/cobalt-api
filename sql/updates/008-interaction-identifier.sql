BEGIN;
SELECT _v.register_patch('008-interaction-identifier', NULL, NULL);

CREATE SEQUENCE interaction_instance_case_number START 100;

ALTER TABLE interaction_instance ADD COLUMN  case_number VARCHAR NOT NULL DEFAULT 'CASE-' || nextval('interaction_instance_case_number');

END;