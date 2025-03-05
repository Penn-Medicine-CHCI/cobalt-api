BEGIN;
SELECT _v.register_patch('203-ic-safety-planning-report', NULL, NULL);

insert into report_type (report_type_id, description, display_order) values ('IC_SAFETY_PLANNING', 'Safety Planning', 26);

COMMIT;