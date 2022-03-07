BEGIN;
SELECT _v.register_patch('001-cobalt-institution', ARRAY['000-base-creates'], NULL);

INSERT INTO institution (institution_id, name, crisis_content, privacy_content, covid_content, subdomain) VALUES
('COBALT', 'Cobalt', '', '', '', 'cobalt');

COMMIT;