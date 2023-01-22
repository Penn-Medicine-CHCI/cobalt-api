BEGIN;
SELECT _v.register_patch('052-reporting', NULL, NULL);

-- No longer have the concept of "super administrator"
UPDATE account SET role_id='ADMINISTRATOR' WHERE role_id='SUPER_ADMINISTRATOR';
DELETE FROM role WHERE role_id IN ('SUPER_ADMINISTRATOR', 'BHS');

CREATE TABLE report_type (
  report_type_id VARCHAR PRIMARY KEY,
  description VARCHAR NOT NULL,
  display_order INTEGER NOT NULL
);

INSERT INTO report_type VALUES ('PROVIDER_UNUSED_AVAILABILITY', 'Provider Availability', 1);
INSERT INTO report_type VALUES ('PROVIDER_APPOINTMENTS', 'Provider Appointments', 2);
INSERT INTO report_type VALUES ('PROVIDER_APPOINTMENT_CANCELATIONS', 'Provider Appointment Cancelations', 3);

-- What reports an account is permitted to see (administrators can implicitly see all reports)
CREATE TABLE account_report_type (
	account_id UUID NOT NULL REFERENCES account,
	report_type_id VARCHAR NOT NULL REFERENCES report_type,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	PRIMARY KEY (account_id, report_type_id)
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON account_report_type FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

COMMIT;