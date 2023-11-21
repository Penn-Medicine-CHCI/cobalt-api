BEGIN;
SELECT _v.register_patch('133-suds-thermometer', NULL, NULL);

INSERT INTO screening_type (screening_type_id, description, overall_score_maximum) VALUES ('SUDS_THERMOMETER', 'SUDS Thermometer', 100);

COMMIT;