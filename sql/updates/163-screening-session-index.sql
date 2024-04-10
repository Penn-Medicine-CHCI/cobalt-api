BEGIN;
SELECT _v.register_patch('162-event-tracking', NULL, NULL);

--Unique index to prevent more than one screening session for a study check-in
CREATE UNIQUE INDEX idx_screening_session_account_check_in_action_id ON screening_session(account_check_in_action_id);

--Adding missing foreign key constraint
ALTER TABLE study_check_in_action ADD CONSTRAINT screening_flow_id_fkey FOREIGN KEY (screening_flow_id) REFERENCES screening_flow;

CREATE TABLE analytics_cobalt_event_type (
  analytics_cobalt_event_type_id VARCHAR PRIMARY KEY,
  description VARCHAR NOT NULL
);

INSERT INTO analytics_cobalt_event_type VALUES ('PAGE_VIEW', 'Page View');

CREATE TABLE analytics_cobalt_event (
  analytics_cobalt_event_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  analytics_cobalt_event_type_id VARCHAR NOT NULL REFERENCES analytics_cobalt_event_type, -- What kind of event this is
  institution_id VARCHAR NOT NULL REFERENCES institution,
  client_device_id UUID NOT NULL REFERENCES client_device,
  account_id UUID REFERENCES account, -- Nullable; we might not yet have an account
  properties JSONB NOT NULL DEFAULT '{}'::jsonb,
  client_timestamp TIMESTAMPTZ NOT NULL, -- Instant the event occurred from the client device's perspective
  created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON analytics_cobalt_event FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

COMMIT;