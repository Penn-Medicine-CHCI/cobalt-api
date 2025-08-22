BEGIN;
SELECT _v.register_patch('231-data-driven-reports', NULL, NULL);

-- TODO: add institution flag for legacy analytics vs. x-ray so we can leave old institutions as-is

-- TODO: don't model legacy ADMIN_ANALYTICS_OVERVIEW etc.  Instead, add new ADMIN_ANALYTICS_V2_SITE_ACTIVE_ACCOUNTS, ADMIN_ANALYTICS_V2_SITE_CREATED_ACCOUNTS, ...
INSERT INTO site_location (site_location_id, description) VALUES
  ('ADMIN_REPORTS', 'Admin Reports'),
  ('ADMIN_ANALYTICS_OVERVIEW', 'Admin Analytics - Overview'),
  ('ADMIN_ANALYTICS_ASSESSMENTS_APPOINTMENTS', 'Admin Analytics - Assessments & Appointments'),
  ('ADMIN_ANALYTICS_GROUP_SESSIONS', 'Admin Analytics - Group Sessions'),
  ('ADMIN_ANALYTICS_RESOURCES_TOPICS', 'Admin Analytics - Resources & Topics'),
  ('ADMIN_ANALYTICS_TABLEAU', 'Admin Analytics - Tableau');

-- Reports are now customized per-institution for non-legacy-flag institutions.
CREATE TABLE institution_report_type ()

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON institution_report_type FOR EACH ROW EXECUTE PROCEDURE set_last_updated();
CREATE UNIQUE INDEX idx_institution_report_type_i_rt ON institution_report_type (institution_id, report_type_id);

CREATE TABLE report_site_location (
	report_site_location_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	institution_report_type_id UUID NOT NULL REFERENCES institution_report_type,
	site_location_id TEXT REFERENCES site_location,
	display_order SMALLINT NOT NULL,
	created_by_account_id UUID NOT NULL REFERENCES account(account_id),
	created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
	last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON report_site_location FOR EACH ROW EXECUTE PROCEDURE set_last_updated();
CREATE UNIQUE INDEX idx_report_site_location_rt_sl ON report_site_location (institution_report_type_id, site_location_id);

END;