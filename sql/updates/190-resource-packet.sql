BEGIN;
SELECT _v.register_patch('190-resource-packet', NULL, NULL);

CREATE TABLE care_resource_tag_group(
	care_resource_tag_group_id TEXT PRIMARY KEY,
	name TEXT NOT NULL,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON care_resource_tag_group FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE care_resource_tag(
	care_resource_tag_id TEXT PRIMARY KEY,
	care_resource_tag_group_id TEXT NOT NULL REFERENCES care_resource_tag_group,
	name TEXT NOT NULL,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON care_resource_tag FOR EACH ROW EXECUTE PROCEDURE set_last_updated();
CREATE UNIQUE INDEX idx_care_resource_tag_id_group ON care_resource_tag(care_resource_tag_id, care_resource_tag_group_id);

CREATE TABLE care_resource (
	care_resource_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	name TEXT NOT NULL,
  import_reference_number INTEGER,  	
  insurance_notes VARCHAR,
  notes VARCHAR,
  phone_number TEXT NULL,
  website_url TEXT NULL,  
  email_address TEXT NULL,  	  	
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  created_by_account_id UUID NOT NULL REFERENCES account,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON care_resource FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE care_resource_care_resource_tag(
	care_resource_care_resource_tag_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	care_resource_id UUID NOT NULL REFERENCES care_resource,
	care_resource_tag_id VARCHAR NOT NULL REFERENCES care_resource_tag,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON care_resource_care_resource_tag FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

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
	address_id UUID NULL REFERENCES address,
	name TEXT NOT NULL,	
	phone_number TEXT NULL,
	wheelchair_access BOOLEAN NOT NULL DEFAULT FALSE,
	insurance_notes VARCHAR,
	website_url VARCHAR,
  notes VARCHAR,
  internal_notes VARCHAR,
  email_address VARCHAR,
	accepting_new_patients BOOLEAN NOT NULL DEFAULT TRUE,
	created_by_account_id UUID NOT NULL REFERENCES account,
	override_payors BOOLEAN NOT NULL DEFAULT FALSE,
  override_specialties BOOLEAN NOT NULL DEFAULT FALSE,
	appointment_type_in_person BOOLEAN NOT NULL DEFAULT FALSE,
	appointment_type_online BOOLEAN NOT NULL DEFAULT FALSE,
	deleted BOOLEAN NOT NULL DEFAULT FALSE,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON care_resource_location FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE care_resource_location_care_resource_tag(
	care_resource_location_care_resource_tag_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	care_resource_location_id UUID NOT NULL REFERENCES care_resource_location,
	care_resource_tag_id VARCHAR NOT NULL REFERENCES care_resource_tag,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON care_resource_location_care_resource_tag FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE resource_packet (
	resource_packet_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	patient_order_id UUID NOT NULL REFERENCES patient_order,
	address_id UUID NULL REFERENCES address,
	travel_radius INTEGER  NULL,
	travel_radius_distance_unit_id TEXT NULL REFERENCES distance_unit(distance_unit_id) DEFAULT 'MILE',
	current_flag BOOLEAN NOT NULL DEFAULT true,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON resource_packet FOR EACH ROW EXECUTE PROCEDURE set_last_updated();
CREATE INDEX idx_resource_packet_patient_order_id ON resource_packet(patient_order_id);

CREATE TABLE resource_packet_care_resource_location (
	resource_packet_care_resource_location_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	resource_packet_id UUID REFERENCES resource_packet,
	care_resource_location_id UUID NOT NULL REFERENCES care_resource_location,
	created_by_account_id UUID NOT NULL REFERENCES account,
	display_order INTEGER NOT NULL,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON resource_packet_care_resource_location FOR EACH ROW EXECUTE PROCEDURE set_last_updated();
CREATE UNIQUE INDEX idx_resource_packet_care_resource_location_ak1 ON resource_packet_care_resource_location(resource_packet_id, care_resource_location_id);

-- If resources are needed this column will contain a link to the resource_packet that was sent
ALTER TABLE patient_order ADD COLUMN resource_packet_id UUID NULL REFERENCES resource_packet;

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

CREATE VIEW v_care_resource_institution 
AS
SELECT cr.*, cri.institution_id
FROM care_resource cr, care_resource_institution cri
WHERE cr.care_resource_id = cri.care_resource_id
AND cr.deleted = false;

CREATE VIEW v_care_resource_location_institution 
AS
SELECT 
    crl.*, 
    cr.name AS resource_name, 
    cr.notes AS resource_notes, 
    cr.insurance_notes AS resource_insurance_notes, 
    cri.institution_id,
    addr.latitude,
    addr.longitude,
    addr.google_place_id
FROM 
    care_resource_location crl
JOIN 
    care_resource cr 
    ON cr.care_resource_id = crl.care_resource_id
JOIN 
    care_resource_institution cri 
    ON cr.care_resource_id = cri.care_resource_id
LEFT OUTER JOIN 
    address addr 
    ON crl.address_id = addr.address_id
WHERE 
    cr.deleted = false;

INSERT INTO account_capability_type
VALUES
('MHIC_RESOURCE_MANAGER', 'MHIC Resource Manager');

COMMIT;