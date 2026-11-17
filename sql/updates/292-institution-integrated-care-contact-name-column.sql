BEGIN;
SELECT _v.register_patch('292-institution-integrated-care-contact-name-column', ARRAY['291-page-directory-locations'], NULL);

ALTER TABLE institution ADD COLUMN integrated_care_contact_name TEXT;

COMMIT;
