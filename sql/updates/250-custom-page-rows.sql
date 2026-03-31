BEGIN;
SELECT _v.register_patch('250-custom-page-rows', NULL, NULL);

INSERT INTO row_type (row_type_id, description)
VALUES ('CUSTOM_ROW', 'Custom Row');

COMMIT;
