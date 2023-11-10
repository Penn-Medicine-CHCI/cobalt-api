BEGIN;
SELECT _v.register_patch('132-study-presigned-uploads', NULL, NULL);

CREATE TABLE file_upload (
  file_upload_id UUID PRIMARY KEY,
  account_id UUID NOT NULL REFERENCES account,
  url TEXT NOT NULL, -- Fully qualified URL to uploaded file
  storage_key TEXT NOT NULL, -- Storage key component of URL (in practice, the path, including filename)
  filename TEXT NOT NULL, -- e.g. "123.csv"
  content_type TEXT NOT NULL, -- e.g. "application/csv"
  created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX file_upload_url_unique_idx ON file_upload USING btree (url);
CREATE UNIQUE INDEX file_upload_storage_key_unique_idx ON file_upload USING btree (storage_key);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON file_upload FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE account_check_in_action_file_upload (
  account_check_in_action_id UUID NOT NULL REFERENCES account_check_in_action,
  file_upload_id UUID NOT NULL REFERENCES account,
  PRIMARY KEY (account_check_in_action_id, file_upload_id)
);

CREATE TABLE study_file_upload (
  study_id UUID NOT NULL REFERENCES study,
  file_upload_id UUID NOT NULL REFERENCES account,
  PRIMARY KEY (study_id, file_upload_id)
);

COMMIT;