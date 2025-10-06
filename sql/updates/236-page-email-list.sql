BEGIN;
SELECT _v.register_patch('236-page-email-list', NULL, NULL);

-- General "mailing list" concept to gather email addresses.
-- For now, we are only exposing these to SUBSCRIBE-type page rows, but we could attach these to other constructs in the future.
-- We might also support non-email contact types in the future (e.g. phone numbers for SMS) - but keeping it simple for now.
CREATE TABLE mailing_list (
  mailing_list_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  institution_id TEXT NOT NULL REFERENCES institution,
  created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON mailing_list FOR EACH ROW EXECUTE PROCEDURE set_last_updated();
CREATE INDEX ON mailing_list(institution_id);

-- What kind of data can go into a mailing list?
CREATE TABLE mailing_list_entry_type (
  mailing_list_entry_type_id TEXT PRIMARY KEY,
  description TEXT NOT NULL
);

INSERT INTO mailing_list_entry_type VALUES ('EMAIL_ADDRESS', 'Email Address');
INSERT INTO mailing_list_entry_type VALUES ('SMS', 'SMS');

CREATE TABLE mailing_list_entry (
  mailing_list_entry_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  mailing_list_entry_type_id TEXT NOT NULL REFERENCES mailing_list_entry_type,
  mailing_list_id UUID NOT NULL REFERENCES mailing_list,
  account_id UUID NOT NULL REFERENCES account, -- the account whose email/phone this is
  value TEXT NOT NULL, -- might be normalized email or E.164 phone number depending on mailing_list_entry_type_id
  created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX ON mailing_list_entry(mailing_list_id);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON mailing_list_entry FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

-- New "subscription" row type
INSERT INTO row_type (row_type_id, description) VALUES ('SUBSCRIBE', 'Subscribe');

-- Ability to tie mailing lists to SUBSCRIBE page rows
CREATE TABLE page_row_mailing_list (
  page_row_mailing_list_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  page_row_id UUID NOT NULL REFERENCES page_row,
  mailing_list_id UUID NOT NULL REFERENCES mailing_list,
  created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_page_row_mailing_list_ml ON page_row_mailing_list(page_row_id, mailing_list_id);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON page_row_mailing_list FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

COMMIT;