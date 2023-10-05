BEGIN;
SELECT _v.register_patch('126-google-bigquery', NULL, NULL);

ALTER TABLE institution ADD COLUMN google_reporting_service_account_private_key TEXT;
ALTER TABLE institution ADD COLUMN ga4_property_id TEXT;
ALTER TABLE institution ADD COLUMN bigquery_resource_id TEXT;
ALTER TABLE institution ADD COLUMN mixpanel_project_id INTEGER;
ALTER TABLE institution ADD COLUMN mixpanel_service_account_username TEXT;
ALTER TABLE institution ADD COLUMN mixpanel_service_account_secret TEXT;
ALTER TABLE institution ADD COLUMN mixpanel_event_history_starts_at DATE;

CREATE TABLE mixpanel_event (
  institution_id TEXT NOT NULL REFERENCES institution,
  distinct_id TEXT NOT NULL, -- this is not really distinct - just preserving what Mixpanel calls it
	event varchar NOT NULL,
	date DATE NOT NULL, -- duplicate data (we also store timestamp as 'time') for ease of querying
	time TIMESTAMPTZ NOT NULL,
	properties JSONB NOT NULL,
	PRIMARY KEY (institution_id, distinct_id, event, time)
);

COMMIT;