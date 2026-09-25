BEGIN;
SELECT _v.register_patch('271-care-encounter-follow-up-link-clicks', ARRAY['262-care-navigator'], NULL);

CREATE TABLE care_encounter_follow_up_link (
  care_encounter_follow_up_link_id UUID PRIMARY KEY,
  care_encounter_id UUID NOT NULL REFERENCES care_encounter(care_encounter_id),
  message_id UUID NOT NULL,
  revision_id UUID NOT NULL,
  destination_url TEXT NOT NULL CHECK (destination_url ~ '^https://[^[:space:]]+$'),
  click_count INTEGER NOT NULL DEFAULT 0 CHECK (click_count >= 0),
  first_clicked_at TIMESTAMPTZ,
  last_clicked_at TIMESTAMPTZ,
  created TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX care_encounter_follow_up_link_message_idx
  ON care_encounter_follow_up_link(message_id);

COMMIT;
