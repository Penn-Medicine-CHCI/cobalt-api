BEGIN;
SELECT _v.register_patch('138-analytics', NULL, NULL);

-- Account "meaningful interaction" is defined as any of the following:
-- * Appointment booked
-- * Group session booked
-- * Content viewed
create view v_analytics_account_meaningful_interaction as
  select a.account_id, a.institution_id, app.created as activity_timestamp, 'APPOINTMENT_BOOKED' as activity
  from account a, appointment app
  where a.account_id=app.account_id
union
  select a.account_id, a.institution_id, gsr.created as activity_timestamp, 'GROUP_SESSION_BOOKED' as activity
  from account a, group_session_reservation gsr
  where a.account_id=gsr.account_id
union
  select a.account_id, a.institution_id, at.created as activity_timestamp, 'CONTENT_INTERACTION' as activity
  from account a, activity_tracking at
  where at.activity_type_id='CONTENT' and at.account_id=a.account_id;

-- Account interaction is anything at all we see coming from BigQuery
CREATE VIEW v_analytics_account_interaction as
  SELECT
    a.account_id,
    a.institution_id,
    agbe.timestamp AS activity_timestamp,
    agbe.name AS activity,
    agbe.analytics_google_bigquery_event_id,
    agbe.event->'parameters'->'page_location'->>'value' AS url
  FROM account a, analytics_google_bigquery_event agbe
  WHERE a.account_id=(agbe.bigquery_user->>'userId')::UUID;

COMMIT;