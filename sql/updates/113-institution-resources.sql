BEGIN;
SELECT _v.register_patch('113-institution-resources', NULL, NULL);

-- Groups institution resources for display purposes
CREATE TABLE institution_resource_group (
	institution_resource_group_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	institution_id VARCHAR NOT NULL REFERENCES institution,
	color_id VARCHAR NOT NULL REFERENCES color DEFAULT 'BRAND_PRIMARY',
	name VARCHAR NOT NULL,
	url_name VARCHAR NOT NULL,
	description VARCHAR,
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
	image_url VARCHAR NOT NULL,
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

COMMIT;