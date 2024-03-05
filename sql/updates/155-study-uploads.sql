BEGIN;
SELECT _v.register_patch('155-study-uploads', NULL, NULL);

-- These 2 tables were not set up correctly and need to be recreated.
-- They have no data in them yet, so this is safe.

DROP TABLE account_check_in_action_file_upload;

CREATE TABLE account_check_in_action_file_upload (
  account_check_in_action_id UUID NOT NULL REFERENCES account_check_in_action,
  file_upload_id UUID NOT NULL REFERENCES file_upload,
  created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (account_check_in_action_id, file_upload_id)
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON account_check_in_action_file_upload FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

DROP TABLE study_file_upload;

CREATE TABLE study_file_upload (
  study_id UUID NOT NULL REFERENCES study,
  file_upload_id UUID NOT NULL REFERENCES file_upload,
  created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (study_id, file_upload_id)
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON study_file_upload FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

COMMIT;