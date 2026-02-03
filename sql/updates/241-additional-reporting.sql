BEGIN;
SELECT _v.register_patch('241-additional-reporting', NULL, NULL);

INSERT INTO report_type (report_type_id, description, display_order)
VALUES ('SIGN_IN_PAGEVIEW_NO_ACCOUNT', 'Analytics - Sign In Pageviews (No Account)', 114);

INSERT INTO report_type (report_type_id, description, display_order)
VALUES ('ACCOUNT_SIGNUP_UNVERIFIED', 'Analytics - Account Signups (Unverified)', 115);

INSERT INTO report_type (report_type_id, description, display_order)
VALUES ('ACCOUNT_ONBOARDING_INCOMPLETE', 'Analytics - Onboarding Incomplete Accounts', 116);

COMMIT;
