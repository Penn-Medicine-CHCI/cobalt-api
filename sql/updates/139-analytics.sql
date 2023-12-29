BEGIN;
SELECT _v.register_patch('139-analytics', NULL, NULL);

INSERT INTO report_type VALUES ('ADMIN_ANALYTICS_VISITS', 'Analytics - Visits', 9);
INSERT INTO report_type VALUES ('ADMIN_ANALYTICS_USERS', 'Analytics - Users', 10);
INSERT INTO report_type VALUES ('ADMIN_ANALYTICS_EMPLOYERS', 'Analytics - Employers', 11);
INSERT INTO report_type VALUES ('ADMIN_ANALYTICS_PAGEVIEWS', 'Analytics - Pageviews', 12);
INSERT INTO report_type VALUES ('ADMIN_ANALYTICS_USER_REFERRALS', 'Analytics - User Referrals', 13);
INSERT INTO report_type VALUES ('ADMIN_ANALYTICS_REFERRING_DOMAINS', 'Analytics - Referring Domains', 14);
INSERT INTO report_type VALUES ('ADMIN_ANALYTICS_CLINICAL_ASSESSMENT_COMPLETION', 'Analytics - Assessment Completion', 15);
INSERT INTO report_type VALUES ('ADMIN_ANALYTICS_CLINICAL_ASSESSMENT_SEVERITY', 'Analytics - Assessment Severity', 16);
INSERT INTO report_type VALUES ('ADMIN_ANALYTICS_CRISIS_TRIGGERS', 'Analytics - Crisis Triggers', 17);
INSERT INTO report_type VALUES ('ADMIN_ANALYTICS_APPOINTMENTS_BOOKABLE', 'Analytics - Appointments Bookable', 18);
INSERT INTO report_type VALUES ('ADMIN_ANALYTICS_APPOINTMENTS_CLICK_TO_CALL', 'Analytics - Appointments Click-To-Call', 19);
INSERT INTO report_type VALUES ('ADMIN_ANALYTICS_GROUP_SESSION_REGISTRATIONS', 'Analytics - Group Session Registrations', 20);
INSERT INTO report_type VALUES ('ADMIN_ANALYTICS_GROUP_SESSION_REQUESTS', 'Analytics - Group Session Requests', 21);
INSERT INTO report_type VALUES ('ADMIN_ANALYTICS_GROUP_SESSIONS', 'Analytics - Group Sessions', 22);
INSERT INTO report_type VALUES ('ADMIN_ANALYTICS_RESOURCE_TOPIC_PAGEVIEWS', 'Analytics - Resource Topic Pageviews', 23);
INSERT INTO report_type VALUES ('ADMIN_ANALYTICS_RESOURCE_PAGEVIEWS', 'Analytics - Resource Pageviews', 24);
INSERT INTO report_type VALUES ('ADMIN_ANALYTICS_TOPIC_CENTER_OVERVIEW', 'Analytics - Topic Center Overview', 25);

-- Account interaction is anything at all we see coming from BigQuery.
-- This matches the column format of v_analytics_account_meaningful_interaction so they can be easily swapped for reports
CREATE OR REPLACE VIEW v_analytics_account_interaction AS
  SELECT
    account_id,
    institution_id,
    timestamp AS activity_timestamp,
    name AS activity,
    analytics_google_bigquery_event_id,
    event->'parameters'->'page_location'->>'value' AS url
  FROM analytics_google_bigquery_event;

COMMIT;