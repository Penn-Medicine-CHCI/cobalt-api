BEGIN;
SELECT _v.register_patch('033-mbi-screening-type', NULL, NULL);

INSERT INTO screening_type (screening_type_id, description) VALUES ('MBI_9', 'MBI-9');

COMMIT;