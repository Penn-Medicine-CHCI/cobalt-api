BEGIN;
SELECT _v.register_patch('117-fhir-institution-updates', NULL, NULL);

ALTER TABLE institution ADD COLUMN external_contact_us_url TEXT;
ALTER TABLE institution ADD COLUMN mychart_instructions_url TEXT;

COMMIT;