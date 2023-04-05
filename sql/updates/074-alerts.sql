BEGIN;
SELECT _v.register_patch('074-alerts', NULL, NULL);

CREATE TABLE alert_type (
  alert_type_id VARCHAR PRIMARY KEY,
  description VARCHAR NOT NULL,
  severity INTEGER NOT NULL
);

INSERT INTO alert_type VALUES ('INFORMATION', 'Information', 100);
INSERT INTO alert_type VALUES ('WARNING', 'Warning', 300);
INSERT INTO alert_type VALUES ('ERROR', 'Error', 400);

-- "Banner" style alert bars
CREATE TABLE alert (
  alert_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  alert_type_id VARCHAR NOT NULL REFERENCES alert_type,
  title VARCHAR NOT NULL,
  message VARCHAR NOT NULL,
  created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON alert FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

-- When an account dismisses an alert, keep track so we don't show the alert again
CREATE TABLE alert_dismissal (
  alert_dismissal_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  alert_id UUID NOT NULL REFERENCES alert,
  account_id UUID NOT NULL REFERENCES account,
  created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX alert_account_unique_idx ON alert_dismissal USING btree (alert_id, account_id);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON alert_dismissal FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE institution_alert (
  institution_alert_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	institution_id VARCHAR NOT NULL REFERENCES institution,
	alert_id UUID NOT NULL REFERENCES alert,
	active BOOLEAN NOT NULL DEFAULT FALSE,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX institution_alert_unique_idx ON institution_alert USING btree (institution_id, alert_id);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON institution_alert FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

COMMIT;