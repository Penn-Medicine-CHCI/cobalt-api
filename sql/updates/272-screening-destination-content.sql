BEGIN;
SELECT _v.register_patch('272-screening-destination-content', ARRAY['021-screening'], NULL);

-- Flow-owned copy for screening destinations. Updating this row does not
-- require publishing another screening flow version.
CREATE TABLE screening_destination_content (
  screening_flow_id UUID NOT NULL REFERENCES screening_flow(screening_flow_id),
  screening_session_destination_id TEXT NOT NULL,
  title TEXT NOT NULL,
  message TEXT NOT NULL,
  action_url TEXT,
  action_text TEXT,
  contact_name TEXT,
  contact_phone TEXT,
  created TIMESTAMPTZ NOT NULL DEFAULT now(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (screening_flow_id, screening_session_destination_id),
  CONSTRAINT screening_destination_content_action_pair CHECK
    ((action_url IS NULL AND action_text IS NULL) OR
     (action_url IS NOT NULL AND action_text IS NOT NULL)),
  CONSTRAINT screening_destination_content_action_https CHECK
    (action_url IS NULL OR action_url ~ '^https://[^[:space:]]+$')
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON screening_destination_content
  FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

COMMIT;
