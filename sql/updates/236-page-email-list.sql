BEGIN;
SELECT _v.register_patch('236-page-email-list', NULL, NULL);

INSERT INTO footprint_event_group_type VALUES ('MAILING_LIST_ENTRY_CREATE', 'Mailing List Entry Create');
INSERT INTO footprint_event_group_type VALUES ('MAILING_LIST_ENTRY_UPDATE', 'Mailing List Entry Update');

-- General "mailing list" concept to gather email addresses.
-- For now, we are only exposing these to SUBSCRIBE-type page rows, but we could attach these to other constructs in the future.
-- We might also support non-email contact types in the future (e.g. phone numbers for SMS) - but keeping it simple for now.
CREATE TABLE mailing_list (
  mailing_list_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  institution_id TEXT NOT NULL REFERENCES institution,
  created_by_account_id UUID NOT NULL REFERENCES account(account_id),
  created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX ON mailing_list(institution_id);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON mailing_list FOR EACH ROW EXECUTE PROCEDURE set_last_updated();
CREATE TRIGGER mailing_list_footprint AFTER INSERT OR UPDATE OR DELETE ON mailing_list FOR EACH ROW EXECUTE PROCEDURE perform_footprint();

-- What kind of data can go into a mailing list?
CREATE TABLE mailing_list_entry_type (
  mailing_list_entry_type_id TEXT PRIMARY KEY,
  description TEXT NOT NULL
);

INSERT INTO mailing_list_entry_type VALUES ('EMAIL_ADDRESS', 'Email Address');
INSERT INTO mailing_list_entry_type VALUES ('SMS', 'SMS');

-- Status of this entry, e.g. active
CREATE TABLE mailing_list_entry_status (
  mailing_list_entry_status_id TEXT PRIMARY KEY,
  description TEXT NOT NULL
);

INSERT INTO mailing_list_entry_status VALUES ('SUBSCRIBED', 'Subscribed');
INSERT INTO mailing_list_entry_status VALUES ('UNSUBSCRIBED', 'Unsubscribed');

CREATE TABLE mailing_list_entry (
  mailing_list_entry_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  mailing_list_entry_type_id TEXT NOT NULL REFERENCES mailing_list_entry_type,
  mailing_list_entry_status_id TEXT NOT NULL REFERENCES mailing_list_entry_status DEFAULT 'SUBSCRIBED',
  mailing_list_id UUID NOT NULL REFERENCES mailing_list,
  account_id UUID NOT NULL REFERENCES account, -- the account whose email/phone this is
  created_by_account_id UUID NOT NULL REFERENCES account(account_id), -- the account who added this entry
  value TEXT NOT NULL, -- might be normalized email or E.164 phone number depending on mailing_list_entry_type_id
  created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX ON mailing_list_entry(mailing_list_id);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON mailing_list_entry FOR EACH ROW EXECUTE PROCEDURE set_last_updated();
CREATE TRIGGER mailing_list_entry_footprint AFTER INSERT OR UPDATE OR DELETE ON mailing_list_entry FOR EACH ROW EXECUTE PROCEDURE perform_footprint();

-- Ensure `value` column is lowercased (e.g. email addresses)
CREATE OR REPLACE FUNCTION normalize_mailing_list_entry_value()
RETURNS TRIGGER AS $$
BEGIN
  -- Trim whitespace first
  IF NEW.value IS NOT NULL THEN
    NEW.value := btrim(NEW.value);
  END IF;

  -- Reject null or empty values
  IF NEW.value IS NULL OR NEW.value = '' THEN
    RAISE EXCEPTION 'mailing_list_entry.value cannot be null or empty'
      USING ERRCODE = '23514';  -- check_violation
  END IF;

  -- Normalize to lowercase
  NEW.value := lower(NEW.value);

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_normalize_mailing_list_entry_value
BEFORE INSERT OR UPDATE OF value
ON mailing_list_entry
FOR EACH ROW
EXECUTE FUNCTION normalize_mailing_list_entry_value();

-- Don't permit duplicate values
ALTER TABLE mailing_list_entry
ADD CONSTRAINT mailing_list_entry_value_unique_idx
UNIQUE (value, mailing_list_id, mailing_list_entry_type_id);

-- New "mailing list" row type
INSERT INTO row_type (row_type_id, description) VALUES ('MAILING_LIST', 'Mailing List');

-- Ability to tie mailing lists to MAILING_LIST page rows
CREATE TABLE page_row_mailing_list (
  page_row_mailing_list_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  page_row_id UUID NOT NULL REFERENCES page_row,
  mailing_list_id UUID NOT NULL REFERENCES mailing_list,
  title TEXT NOT NULL,
  description TEXT NOT NULL, -- Can contain HTML
  created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_page_row_mailing_list_ml ON page_row_mailing_list(page_row_id, mailing_list_id);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON page_row_mailing_list FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE VIEW v_page_row_mailing_list AS
SELECT
  prml.page_row_mailing_list_id,
  pr.page_row_id,
  pr.display_order,
  pr.page_section_id,
  pr.row_type_id,
  prml.mailing_list_id,
  pr.created_by_account_id,
  prml.title,
  prml.description,
  pr.created,
  pr.last_updated
FROM
	page_row_mailing_list prml,
	page_row pr
WHERE
	prml.page_row_id = pr.page_row_id
	AND pr.deleted_flag = FALSE;

COMMIT;