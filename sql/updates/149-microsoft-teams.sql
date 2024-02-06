BEGIN;
SELECT _v.register_patch('149-microsoft-teams', NULL, NULL);

INSERT INTO videoconference_platform (videoconference_platform_id, description) VALUES ('MICROSOFT_TEAMS','Microsoft Teams');

CREATE TABLE microsoft_teams_meeting (
  microsoft_teams_meeting_id UUID NOT NULL PRIMARY KEY DEFAULT uuid_generate_v4(),
  institution_id TEXT NOT NULL REFERENCES institution,
  created_by_account_id UUID NOT NULL REFERENCES account,
  online_meeting_id TEXT NOT NULL, -- Microsoft's unique identifier
  join_url TEXT NOT NULL,
  start_date_time TIMESTAMP NOT NULL,
  end_date_time TIMESTAMP NOT NULL,
  time_zone TEXT NOT NULL,
  api_response JSONB NOT NULL,
  created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON microsoft_teams_meeting FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

ALTER TABLE appointment ADD COLUMN microsoft_teams_meeting_id UUID REFERENCES microsoft_teams_meeting;

ALTER TABLE institution ADD COLUMN microsoft_teams_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE institution ADD COLUMN microsoft_teams_tenant_id TEXT;
ALTER TABLE institution ADD COLUMN microsoft_teams_client_id TEXT;
ALTER TABLE institution ADD COLUMN microsoft_teams_user_id TEXT;

COMMIT;