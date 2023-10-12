BEGIN;
SELECT _v.register_patch('126-google-bigquery', NULL, NULL);

ALTER TABLE institution ADD COLUMN google_reporting_service_account_private_key VARCHAR;
ALTER TABLE institution ADD COLUMN google_ga4_property_id VARCHAR;
ALTER TABLE institution ADD COLUMN google_bigquery_resource_id VARCHAR;
ALTER TABLE institution ADD COLUMN google_bigquery_sync_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE institution ADD COLUMN google_bigquery_sync_starts_at DATE;
ALTER TABLE institution ADD COLUMN mixpanel_project_id INTEGER;
ALTER TABLE institution ADD COLUMN mixpanel_service_account_username VARCHAR;
ALTER TABLE institution ADD COLUMN mixpanel_service_account_secret VARCHAR;
ALTER TABLE institution ADD COLUMN mixpanel_sync_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE institution ADD COLUMN mixpanel_sync_starts_at DATE;

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

INSERT INTO analytics_sync_status VALUES ('SYNCED', 'Synced');
INSERT INTO analytics_sync_status VALUES ('BUSY_SYNCING', 'Busy Syncing');
INSERT INTO analytics_sync_status VALUES ('SYNC_FAILED', 'Sync Failed');

-- Analytics data is synced by date (e.g. asking BigQuery for all events for 2023-10-31 for institution ABC).
-- Keep track of each date's sync status by institution so know what's already synced and what still needs to be synced.
-- Actual event data is stored in analytics_mixpanel_event and analytics_google_bigquery_event tables.
CREATE TABLE analytics_event_date_sync (
  institution_id VARCHAR NOT NULL REFERENCES institution,
  analytics_vendor_id VARCHAR NOT NULL REFERENCES analytics_vendor,
  analytics_sync_status_id VARCHAR NOT NULL REFERENCES analytics_sync_status,
	date DATE NOT NULL,
	sync_started_at timestamptz NOT NULL,
	sync_ended_at timestamptz,
	CONSTRAINT analytics_event_date_sync_pk PRIMARY KEY (institution_id, analytics_vendor_id, date)
);

CREATE TABLE analytics_google_bigquery_event (
  analytics_google_bigquery_event_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  institution_id VARCHAR NOT NULL REFERENCES institution,
  account_id UUID, -- duplicates user->userId for easy access. see note below about why we do not include 'REFERENCES account' here
  user_pseudo_id TEXT, -- duplicates user->userPseudoId for easy access
  event_bundle_sequence_id TEXT, -- duplicates event->bundleSequenceId for easy access
	name VARCHAR NOT NULL, -- duplicates event->name for easy access
	date DATE NOT NULL, -- duplicates event->date for easy access
	timestamp TIMESTAMPTZ NOT NULL, -- event_timestamp field
	timestamp_parameter TIMESTAMPTZ, --  duplicates event->timestamp (can be different from previous field)
	event JSONB NOT NULL,
	bigquery_user JSONB NOT NULL, -- "user" is a reserved word in PG and has to be double-quoted to reference, hence `bigquery_user`
	traffic_source JSONB NOT NULL,
	collected_traffic_source JSONB NOT NULL,
	geo JSONB NOT NULL,
	device JSONB NOT NULL,
	-- In BigQuery world, the combination of "event_name, event_timestamp, user_pseudo_id, event_bundle_sequence_id" is in theory a unique identifier.
	-- In practice, it also needs event_server_timestamp_offset because event_timestamp can be duplicated if events are batched together on the client.
	-- GA4 does not populate event_server_timestamp_offset, so we cannot apply this unique constraint as-is.
	-- However, events have a "timestamp" parameter field (timestamp_parameter in our table), which appears to be sufficient to uniquely identify in combination with the other fields.
	-- See https://issuetracker.google.com/issues/246937506
	UNIQUE (institution_id, name, timestamp, timestamp_parameter, user_pseudo_id, event_bundle_sequence_id)
);

CREATE TABLE analytics_mixpanel_event (
  analytics_mixpanel_event_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  institution_id VARCHAR NOT NULL REFERENCES institution,
  account_id UUID, -- duplicates properties->$user_id for easy access. see note below about why we do not include 'REFERENCES account' here
  distinct_id VARCHAR NOT NULL, -- this is not really distinct - just preserving what Mixpanel calls it
  anon_id VARCHAR,
  device_id VARCHAR NOT NULL,
	name VARCHAR NOT NULL,
	date DATE NOT NULL, -- duplicate data for easy access
	timestamp TIMESTAMPTZ NOT NULL,
	properties JSONB NOT NULL,
	-- In Mixpanel world, the combination of "distinct_id, name, timestamp" is a unique identifier.
	UNIQUE (institution_id, distinct_id, name, timestamp)
);

-- Run these statements only in nonlocal environments.
-- Locally, we might pull down dev or prod data for experimenting and won't have corresponding account records, so
-- having this constraint present locally would cause inserts to fail.
--
-- ALTER TABLE analytics_bigquery_event ADD CONSTRAINT account_fk FOREIGN KEY (account_id) REFERENCES account (account_id);
-- ALTER TABLE analytics_mixpanel_event ADD CONSTRAINT account_fk FOREIGN KEY (account_id) REFERENCES account (account_id);

COMMIT;