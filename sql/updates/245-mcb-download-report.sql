BEGIN;
SELECT _v.register_patch('245-mcb-download-report', NULL, NULL);

INSERT INTO report_type (report_type_id, description, display_order)
VALUES ('COURSE_MCB_DOWNLOAD', 'Analytics - MCB Download', 118);

ALTER TABLE course
	ADD COLUMN reporting_key TEXT;

ALTER TABLE course_unit
	ADD COLUMN reporting_key TEXT;

CREATE INDEX course_reporting_key_idx
	ON course (reporting_key)
	WHERE reporting_key IS NOT NULL;

CREATE INDEX course_unit_reporting_key_idx
	ON course_unit (reporting_key)
	WHERE reporting_key IS NOT NULL;

COMMIT;
