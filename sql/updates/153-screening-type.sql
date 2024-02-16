BEGIN;
SELECT _v.register_patch('153-screening-type', NULL, NULL);

INSERT INTO screening_type (screening_type_id, description) VALUES ('CONTENT_SATISFACTION', 'Content Satisfaction');
INSERT INTO screening_type (screening_type_id, description) VALUES ('SCREENING_READINESS', 'Screening Readiness');

COMMIT;