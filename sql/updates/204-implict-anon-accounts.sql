BEGIN;
SELECT _v.register_patch('204-implicit-anon-accounts', NULL, NULL);

INSERT INTO account_source (account_source_id, description) VALUES ('ANONYMOUS_IMPLICIT', 'Anonymous (Implicitly Created)');

INSERT INTO institution_account_source (institution_account_source_id, institution_id, account_source_id, account_source_display_style_id, display_order, authentication_description, visible)
VALUES (uuid_generate_v4(), 'COBALT', 'ANONYMOUS_IMPLICIT', 'TERTIARY', 5, 'Anonymous (Implicitly Created)', FALSE);

INSERT INTO footprint_event_group_type VALUES ('ACCOUNT_CREATE_ANONYMOUS_IMPLICIT', 'Account Create (Anonymous - Implicitly Created)');

COMMIT;