BEGIN;
SELECT _v.register_patch('125-admin-capabilities', NULL, NULL);

-- Going to use Capabilities for this instead - it's currently unused anyway, safe to delete
DROP TABLE account_report_type;

ALTER TABLE institution ADD COLUMN secure_filesharing_platform_name TEXT;
ALTER TABLE institution ADD COLUMN secure_filesharing_platform_url TEXT;

-- New report type just for EAP
INSERT INTO report_type VALUES ('PROVIDER_APPOINTMENTS_EAP', 'Provider Appointments (EAP)', 8);

INSERT INTO account_capability_type VALUES ('GROUP_SESSION_ADMIN', 'Group Session Admin');
INSERT INTO account_capability_type VALUES ('CONTENT_ADMIN', 'Content Admin');
INSERT INTO account_capability_type VALUES ('ANALYTICS_VIEWER', 'Analytics Viewer');
INSERT INTO account_capability_type VALUES ('PROVIDER_REPORT_ADMIN', 'Provider Report Admin');
INSERT INTO account_capability_type VALUES ('PROVIDER_REPORT_UNUSED_AVAILABILITY_VIEWER', 'Provider Report Viewer - Unused Availability');
INSERT INTO account_capability_type VALUES ('PROVIDER_REPORT_APPOINTMENTS_VIEWER', 'Provider Report Viewer - Appointments');
INSERT INTO account_capability_type VALUES ('PROVIDER_REPORT_APPOINTMENT_CANCELATIONS_VIEWER', 'Provider Report Viewer - Appointment Cancelations');
INSERT INTO account_capability_type VALUES ('PROVIDER_REPORT_APPOINTMENTS_EAP_VIEWER', 'Provider Report Viewer - Appointments (EAP)');

-- For non-IC institutions, give all existing admins group session and content admin by default.
-- We can make one-off updates to add or remove capabilities to specific accounts as-needed.
INSERT INTO account_capability(account_id, account_capability_type_id)
SELECT a.account_id, 'GROUP_SESSION_ADMIN'
FROM account a, institution i
WHERE a.institution_id=i.institution_id
AND a.role_id='ADMINISTRATOR'
AND i.integrated_care_enabled=FALSE;

INSERT INTO account_capability(account_id, account_capability_type_id)
SELECT a.account_id, 'CONTENT_ADMIN'
FROM account a, institution i
WHERE a.institution_id=i.institution_id
AND a.role_id='ADMINISTRATOR'
AND i.integrated_care_enabled=FALSE;

COMMIT;