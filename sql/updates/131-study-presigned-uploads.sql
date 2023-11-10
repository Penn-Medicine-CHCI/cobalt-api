BEGIN;
SELECT _v.register_patch('131-study-presigned-uploads', NULL, NULL);

CREATE TABLE account_check_in_action_upload (
  account_check_in_action_upload_id UUID PRIMARY KEY,
  account_check_in_action_id UUID NOT NULL REFERENCES account_check_in_action,
  account_id UUID NOT NULL REFERENCES account,
  url TEXT NOT NULL, -- Fully qualified URL to uploaded file
  storage_key TEXT NOT NULL, -- Storage key component of URL (in practice, the path, including filename)
  filename TEXT NOT NULL, -- e.g. "123.csv"
  content_type TEXT NOT NULL, -- e.g. "application/csv"
  created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX account_check_in_action_upload_url_unique_idx ON account_check_in_action_upload USING btree (url);
CREATE UNIQUE INDEX account_check_in_action_upload_storage_key_unique_idx ON account_check_in_action_upload USING btree (storage_key);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON account_check_in_action_upload FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE study_upload (
  study_upload_id UUID PRIMARY KEY,
  study_id UUID NOT NULL REFERENCES study,
  account_id UUID NOT NULL REFERENCES account,
  url TEXT NOT NULL, -- Fully qualified URL to uploaded file
  storage_key TEXT NOT NULL, -- Storage key component of URL (in practice, the path, including filename)
  filename TEXT NOT NULL, -- e.g. "123.csv"
  content_type TEXT NOT NULL, -- e.g. "application/csv"
  created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX study_upload_url_unique_idx ON study_upload USING btree (url);
CREATE UNIQUE INDEX study_upload_storage_key_unique_idx ON study_upload USING btree (storage_key);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON study_upload FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

COMMIT;