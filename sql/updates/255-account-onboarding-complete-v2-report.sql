BEGIN;
SELECT _v.register_patch('255-account-onboarding-complete-v2-report', NULL, NULL);

-- Deploy API first, then run this script so we don't have "missing enum value" errors
UPDATE report_type
SET display_order = display_order + 2
WHERE display_order >= 118;

INSERT INTO report_type (report_type_id, description, display_order)
VALUES ('ACCOUNT_ONBOARDING_COMPLETE_V2', 'Analytics - Onboarding Complete Accounts V2', 118);

INSERT INTO report_type (report_type_id, description, display_order)
VALUES ('ACCOUNT_GEOLOCATION', 'Analytics - Account Geolocation', 119);

COMMIT;
