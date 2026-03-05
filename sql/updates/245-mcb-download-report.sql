BEGIN;
SELECT _v.register_patch('245-mcb-download-report', NULL, NULL);

INSERT INTO report_type (report_type_id, description, display_order)
VALUES ('COURSE_MCB_DOWNLOAD', 'Analytics - MCB Download', 118);

COMMIT;
