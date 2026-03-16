BEGIN;
SELECT _v.register_patch('247-account-timeline-report', NULL, NULL);

INSERT INTO report_type (report_type_id, description, display_order)
VALUES ('ACCOUNT_TIMELINE', 'Analytics - Account Timeline', 119);

COMMIT;
