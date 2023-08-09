BEGIN;
SELECT _v.register_patch('111-group-session-updates', NULL, NULL);

CREATE TABLE group_session_collection (
  group_session_collection_id UUID NOT NULL PRIMARY KEY,
  institution_id VARCHAR NOT NULL REFERENCES institution,
  description VARCHAR NOT NULL,
  display_order INTEGER NOT NULL,
  created timestamptz NOT NULL DEFAULT now(),
  last_updated timestamptz NOT NULL 
);

create trigger set_last_updated before
insert or update on group_session_collection for each row execute procedure set_last_updated();

ALTER TABLE group_session 
ADD COLUMN screening_flow_id UUID NULL REFERENCES screening_flow,
ADD COLUMN visible_flag BOOLEAN NOT NULL DEFAULT true,
ADD COLUMN group_session_collection_id UUID NULL REFERENCES group_session_collection,
ADD COLUMN followup_time_of_day TIME NULL,
ADD COLUMN followup_day_offset INTEGER NULL,
ADD COLUMN send_reminder_email BOOLEAN NOT NULL DEFAULT false,
ADD COLUMN reminder_email_content TEXT NULL,
ADD COLUMN single_session_flag BOOLEAN NOT NULL DEFAULT true,
ADD COLUMN date_time_description TEXT,
ALTER COLUMN start_date_time DROP NOT NULL,
ALTER COLUMN end_date_time DROP NOT NULL;

ALTER TABLE tag_group_session RENAME COLUMN tag_content_id TO tag_group_session_id;

COMMIT;