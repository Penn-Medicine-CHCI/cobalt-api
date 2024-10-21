BEGIN;
SELECT _v.register_patch('190-resource-packet', NULL, NULL);

CREATE TABLE population_served (
	population_served_id TEXT PRIMARY KEY,
	name TEXT NOT NULL,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON population_served FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

INSERT INTO population_served 
VALUES
('ADULTS', 'Adults'),
('CHILDREN', 'Children'),
('ADOLESCENTS', 'Adolescents');

CREATE TABLE accredidation (
	accredidation_id TEXT PRIMARY KEY,
	name TEXT NOT NULL,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON accredidation FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE language (
	language_id TEXT PRIMARY KEY,
	name TEXT NOT NULL,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON language FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE care_resource_group (
	care_resource_group_id TEXT PRIMARY KEY,
	name TEXT NOT NULL,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON care_resource_group FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE care_resource (
	care_resource_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	name TEXT NOT NULL,
  	notes TEXT NULL,
  	phone_number TEXT NULL,
  	website_url TEXT NULL,  	  	
  	care_resource_group_id TEXT NULL REFERENCES care_resource_group,
  	import_reference_number INTEGER,
  	resource_available BOOLEAN NOT NULL,
  	deleted BOOLEAN NOT NULL DEFAULT FALSE,
  	created_by_account_id UUID NOT NULL REFERENCES account,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON care_resource FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE care_resource_population_served (
	care_resource_population_served_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	care_resource_id UUID NOT NULL REFERENCES care_resource,
	population_served_id TEXT NOT NULL REFERENCES population_served,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON care_resource_population_served FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE care_resource_accredidation (
	care_resource_accredidation_id TEXT PRIMARY KEY,
	care_resource_id UUID NOT NULL REFERENCES care_resource,
	accredidation_id TEXT NOT NULL REFERENCES accredidation,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON care_resource_accredidation FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE care_resource_institution (
	care_resource_institution_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	care_resource_id UUID NOT NULL REFERENCES care_resource,
	institution_id TEXT NOT NULL REFERENCES institution,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON care_resource_institution FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE care_resource_location (
	care_resource_location_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	care_resource_id UUID NOT NULL REFERENCES care_resource,
	address_id UUID NOT NULL REFERENCES address,
	phone_number TEXT NULL,
	wheelchair_access BOOLEAN NOT NULL DEFAULT FALSE,
	notes TEXT NULL,
	accepting_new_patients BOOLEAN NOT NULL DEFAULT TRUE,
	created_by_account_id UUID NOT NULL REFERENCES account,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON care_resource_location FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE care_resource_location_language (
	care_resource_location_language_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	care_resource_location_id UUID NOT NULL REFERENCES care_resource_location,
	language_id TEXT NOT NULL REFERENCES language,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON care_resource_location_language FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE payor (
	payor_id TEXT PRIMARY KEY,
	name TEXT NOT NULL,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON payor FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE care_resource_location_payor (
	care_resource_location_payor_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	care_resource_location_id UUID NOT NULL REFERENCES care_resource_location,
	payor_id TEXT NOT NULL REFERENCES payor,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);	
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON care_resource_location_payor FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE care_resource_focus_type (
	care_resource_focus_type_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	care_resource_id UUID NOT NULL REFERENCES care_resource,
	focus_type_id TEXT NOT NULL REFERENCES patient_order_focus_type(patient_order_focus_type_id),
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON care_resource_focus_type FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE care_resource_location_support_role (
	care_resource_location_support_role_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	care_resource_location_id UUID NOT NULL REFERENCES care_resource_location,
	support_role_id TEXT NOT NULL REFERENCES support_role,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON care_resource_location_support_role FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE care_resource_specialty_group (
	care_resource_specialty_group_id TEXT PRIMARY KEY,
	name TEXT NOT NULL,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON care_resource_specialty_group FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE care_resource_specialty (
	care_resource_specialty_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	name TEXT NOT NULL,
	care_resource_specialty_group_id TEXT NOT NULL REFERENCES care_resource_specialty_group,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON care_resource_specialty FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE care_resource_location_specialty (
	care_resource_location_specialty_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	care_resource_specialty_id UUID NOT NULL REFERENCES care_resource_specialty,
	care_resource_location_id UUID NOT NULL REFERENCES care_resource_location,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON care_resource_location_specialty FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE patient_order_resource_packet (
	patient_order_resource_packet_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	patient_order_id UUID NOT NULL REFERENCES patient_order,
	current_flag BOOLEAN NOT NULL DEFAULT true,
	payor_id TEXT NOT NULL REFERENCES payor,
	postal_code TEXT NOT NULL,
	travel_radius INTEGER NOT NULL,
	travel_radius_distance_unit_id TEXT NOT NULL REFERENCES distance_unit(distance_unit_id) DEFAULT 'MILE',
	support_role_id TEXT NOT NULL REFERENCES support_role,
	intro_text TEXT NOT NULL,
	end_text TEXT NULL,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON patient_order_resource_packet FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE patient_order_resource_packet_resource (
	patient_order_resource_packet_resource_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	patient_order_resource_packet_id UUID REFERENCES patient_order_resource_packet,
	care_resource_id UUID NOT NULL REFERENCES care_resource,
	included BOOLEAN NOT NULL DEFAULT false,
	display_order INTEGER NOT NULL,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON patient_order_resource_packet_resource FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

-- If a resource packet is send this column will contain a link to the patient_order_resource_packet that was sent
ALTER TABLE patient_order ADD COLUMN patient_order_resource_packet_id UUID NULL REFERENCES patient_order_resource_packet;

ALTER TABLE address 
ADD COLUMN google_maps_url TEXT NULL,
ADD COLUMN google_place_id TEXT NULL,
ADD COLUMN latitude NUMERIC NULL,
ADD COLUMN longitude NUMERIC NULL,
ADD COLUMN premise TEXT NULL,
ADD COLUMN subpremise TEXT NULL,
ADD COLUMN region_subdivision TEXT NULL,
ADD COLUMN postal_code_suffix TEXT NULL,
ADD COLUMN formatted_address TEXT NULL;

COMMIT;