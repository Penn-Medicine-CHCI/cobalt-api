BEGIN;
SELECT _v.register_patch('114-institution-resources', NULL, NULL);

-- Groups institution resources for display purposes
CREATE TABLE institution_resource_group (
	institution_resource_group_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	institution_id VARCHAR NOT NULL REFERENCES institution,
	color_id VARCHAR NOT NULL REFERENCES color DEFAULT 'BRAND_PRIMARY',
	name VARCHAR NOT NULL,
	url_name VARCHAR NOT NULL,
	description VARCHAR,
	display_order INTEGER NOT NULL,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON institution_resource_group FOR EACH ROW EXECUTE PROCEDURE set_last_updated();
CREATE UNIQUE INDEX institution_resource_group_name_unique_idx ON institution_resource_group USING btree (institution_id, LOWER(name));
CREATE UNIQUE INDEX institution_resource_group_url_name_unique_idx ON institution_resource_group USING btree (institution_id, LOWER(url_name));

CREATE TABLE institution_resource (
	institution_resource_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	institution_id VARCHAR NOT NULL REFERENCES institution,
	name VARCHAR NOT NULL,
	url_name VARCHAR NOT NULL,
	description VARCHAR,
	url VARCHAR,
	image_url VARCHAR,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON institution_resource FOR EACH ROW EXECUTE PROCEDURE set_last_updated();
CREATE UNIQUE INDEX institution_resource_url_name_unique_idx ON institution_resource USING btree (institution_id, LOWER(url_name));

CREATE TABLE institution_resource_group_institution_resource (
  institution_resource_group_id UUID NOT NULL,
  institution_resource_id UUID NOT NULL,
  display_order INTEGER NOT NULL,
  PRIMARY KEY(institution_resource_group_id, institution_resource_id)
);

ALTER TABLE institution ADD COLUMN resource_groups_title TEXT;
ALTER TABLE institution ADD COLUMN resource_groups_description TEXT;

ALTER TABLE institution_feature ADD COLUMN name_override TEXT;
ALTER TABLE institution_feature ADD COLUMN landing_page_visible BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE institution_feature ADD COLUMN treatment_description TEXT;

UPDATE institution_feature SET landing_page_visible=nav_visible;

INSERT INTO feature
  (feature_id, name, url_name, navigation_header_id)
VALUES
  ('RESOURCE_NAVIGATOR', 'Connect with a Resource Navigator', '/feedback', 'CONNECT_WITH_SUPPORT');

INSERT INTO support_role (support_role_id, description, display_order) VALUES ('MSW', 'Master''s Level Social Worker', 11);

-- Clean up old data.  The PSYCHOTHERAPIST feature is now psychologists and LCSWs
DELETE FROM feature_support_role WHERE feature_id='PSYCHOTHERAPIST' AND support_role_id='PSYCHIATRIST';
INSERT INTO feature_support_role(feature_id, support_role_id) VALUES ('PSYCHOTHERAPIST', 'LCSW');

-- Introduce MSW feature
INSERT INTO feature (feature_id, navigation_header_id, name, url_name) VALUES
	('MSW', 'CONNECT_WITH_SUPPORT', 'Master''s Level Social Worker', '/connect-with-support/msw');

INSERT INTO feature_support_role(feature_id, support_role_id) VALUES
	('MSW', 'MSW');

INSERT INTO feature_filter(feature_id, filter_id) VALUES
	('MSW', 'DATE'),
	('MSW', 'TIME_OF_DAY');

-- No longer have an LCSW feature
UPDATE screening_session_feature_recommendation SET feature_id='MSW' WHERE feature_id='LCSW';

DELETE FROM feature_filter WHERE feature_id='LCSW';
DELETE FROM feature_support_role WHERE feature_id='LCSW';
DELETE FROM institution_feature WHERE feature_id='LCSW';
DELETE FROM feature WHERE feature_id='LCSW';

COMMIT;