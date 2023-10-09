BEGIN;
SELECT _v.register_patch('126-google-bigquery', NULL, NULL);

ALTER TABLE institution ADD COLUMN google_reporting_service_account_private_key VARCHAR;
ALTER TABLE institution ADD COLUMN google_ga4_property_id VARCHAR;
ALTER TABLE institution ADD COLUMN google_bigquery_resource_id VARCHAR;
ALTER TABLE institution ADD COLUMN google_bigquery_event_date_sync_starts_at DATE;
ALTER TABLE institution ADD COLUMN mixpanel_project_id INTEGER;
ALTER TABLE institution ADD COLUMN mixpanel_service_account_username VARCHAR;
ALTER TABLE institution ADD COLUMN mixpanel_service_account_secret VARCHAR;
ALTER TABLE institution ADD COLUMN mixpanel_event_date_sync_starts_at DATE;

CREATE TABLE analytics_vendor (
	analytics_vendor_id VARCHAR PRIMARY KEY,
	description VARCHAR NOT NULL
);

INSERT INTO analytics_vendor VALUES ('GOOGLE_BIGQUERY', 'Google BigQuery');
INSERT INTO analytics_vendor VALUES ('GOOGLE_GA4', 'Google GA4');
INSERT INTO analytics_vendor VALUES ('MIXPANEL', 'MixPanel');
INSERT INTO analytics_vendor VALUES ('AMPLITUDE', 'Amplitude');

CREATE TABLE analytics_sync_status (
	analytics_sync_status_id VARCHAR PRIMARY KEY,
	description VARCHAR NOT NULL
);

INSERT INTO analytics_sync_status VALUES ('UNSYNCED', 'Unsynced');
INSERT INTO analytics_sync_status VALUES ('SYNCED', 'Synced');
INSERT INTO analytics_sync_status VALUES ('BUSY_SYNCING', 'Busy Syncing');
INSERT INTO analytics_sync_status VALUES ('SYNC_FAILED', 'Sync Failed');

-- Analytics data is synced by date (e.g. asking BigQuery for all events for 2023-10-31 for institution ABC).
-- Keep track of each date's sync status by institution so know what's already synced and what still needs to be synced.
-- Actual event data is stored in analytics_mixpanel_event and analytics_bigquery_event tables.
CREATE TABLE analytics_event_date_sync (
  institution_id VARCHAR NOT NULL REFERENCES institution,
  analytics_vendor_id VARCHAR NOT NULL REFERENCES analytics_vendor,
  analytics_sync_status_id VARCHAR NOT NULL REFERENCES analytics_sync_status,
	date DATE NOT NULL,
	sync_started_at timestamptz NOT NULL,
	sync_ended_at timestamptz,
	PRIMARY KEY (institution_id, analytics_vendor_id, date)
);

CREATE TABLE analytics_google_bigquery_event (
  analytics_google_bigquery_event_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  institution_id VARCHAR NOT NULL REFERENCES institution,
  account_id UUID, -- see note below about why we do not include 'REFERENCES account' here
  user_pseudo_id TEXT, -- duplicates user->userPseudoId for easy access
  event_bundle_sequence_id TEXT, -- duplicates event->bundleSequenceId for easy access
	name VARCHAR NOT NULL, -- duplicates event->name for easy access
	date DATE NOT NULL, -- duplicates event->date for easy access
	timestamp TIMESTAMPTZ NOT NULL, --  duplicates event->timestamp for easy access
	event JSONB NOT NULL,
	"user" JSONB NOT NULL,
	traffic_source JSONB NOT NULL,
	collected_traffic_source JSONB NOT NULL,
	geo JSONB NOT NULL,
	device JSONB NOT NULL,
	-- In BigQuery world, the combination of "event_name, event_timestamp, user_pseudo_id, event_bundle_sequence_id" is a unique identifier.
	UNIQUE (institution_id, name, timestamp, user_pseudo_id, event_bundle_sequence_id)
);

-- Run this separately in nonlocal environments.
-- Locally, we might pull down dev or prod data for experimenting and won't have corresponding account records, so
-- having this constraint present locally would cause inserts to fail.
-- ALTER TABLE analytics_bigquery_event ADD CONSTRAINT account_fk FOREIGN KEY (account_id) REFERENCES account (account_id);

CREATE TABLE analytics_mixpanel_event (
  analytics_mixpanel_event_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  institution_id VARCHAR NOT NULL REFERENCES institution,
  distinct_id VARCHAR NOT NULL, -- this is not really distinct - just preserving what Mixpanel calls it
	name VARCHAR NOT NULL,
	date DATE NOT NULL, -- duplicate data for easy access
	timestamp TIMESTAMPTZ NOT NULL,
	properties JSONB NOT NULL,
	-- In MixPanel world, the combination of "distinct_id, name, timestamp" is a unique identifier.
	UNIQUE (institution_id, distinct_id, name, timestamp)
);

COMMIT;