BEGIN;
SELECT _v.register_patch('189-resource-packet', NULL, NULL);

CREATE TABLE population_served (
	population_served_id VARCHAR PRIMARY KEY,
	name VARCHAR NOT NULL,
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
	accredidation_id VARCHAR PRIMARY KEY,
	name VARCHAR NOT NULL,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON accredidation FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE language (
	language_id VARCHAR PRIMARY KEY,
	name VARCHAR NOT NULL,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON language FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE care_resource_group (
	care_resource_group_id VARCHAR PRIMARY KEY,
	name VARCHAR NOT NULL,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON care_resource_group FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE care_resource (
	care_resource_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	name VARCHAR NOT NULL,
  	notes VARCHAR NULL,
  	phone_number VARCHAR NULL,
  	website_url VARCHAR NULL,
  	care_resource_available BOOLEAN NOT NULL,
  	created_by_account_id UUID NOT NULL REFERENCES account,
  	gender_identity_id VARCHAR NULL REFERENCES gender_identity,
  	ethnicity_id VARCHAR NULL REFERENCES ethnicity,
  	wheelchair_access BOOLEAN NOT NULL DEFAULT FALSE,
  	accepting_new_patients BOOLEAN NOT NULL DEFAULT TRUE,
  	care_resource_group_id VARCHAR NOT NULL REFERENCES care_resource_group,
  	raw_contact_information VARCHAR NULL,
  	import_reference_number INTEGER,
  	deleted BOOLEAN NOT NULL DEFAULT FALSE,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON care_resource FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE care_resource_population_served (
	care_resource_population_served_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	care_resource_id UUID NOT NULL REFERENCES care_resource,
	population_served_id VARCHAR NOT NULL REFERENCES population_served,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON care_resource_population_served FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE care_resource_language (
	care_resource_language_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	care_resource_id UUID NOT NULL REFERENCES care_resource,
	language_id VARCHAR NOT NULL REFERENCES language,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON care_resource_language FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE care_resource_accredidation (
	care_resource_accredidation_id VARCHAR PRIMARY KEY,
	care_resource_id UUID NOT NULL REFERENCES care_resource,
	accredidation_id VARCHAR NOT NULL REFERENCES accredidation,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON care_resource_accredidation FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE care_resource_institution (
	care_resource_institution_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	care_resource_id UUID NOT NULL REFERENCES care_resource,
	institution_id VARCHAR NOT NULL REFERENCES institution,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON care_resource_institution FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE care_resource_location (
	care_resource_location_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	care_resource_id UUID NOT NULL REFERENCES care_resource,
	address VARCHAR NOT NULL,
	phone_number VARCHAR NULL,
	notes VARCHAR NULL,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON care_resource_location FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE payor (
	payor_id VARCHAR PRIMARY KEY,
	name VARCHAR NOT NULL,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON payor FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE care_resource_payor (
	care_resource_payor_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	care_resource_id UUID NOT NULL REFERENCES care_resource,
	payor_id VARCHAR NOT NULL REFERENCES payor,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);	
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON care_resource_payor FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE care_resource_focus_type (
	care_resource_focus_type_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	care_resource_id UUID NOT NULL REFERENCES care_resource,
	focus_type_id VARCHAR NOT NULL REFERENCES patient_order_focus_type(patient_order_focus_type_id),
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON care_resource_focus_type FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE care_resource_support_role (
	care_resource_support_role_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	care_resource_id UUID NOT NULL REFERENCES care_resource,
	support_role_id VARCHAR NOT NULL REFERENCES support_role,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON care_resource_support_role FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE care_resource_specialty_group (
	care_resource_specialty_group_id VARCHAR PRIMARY KEY,
	name VARCHAR NOT NULL,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON care_resource_specialty_group FOR EACH ROW EXECUTE PROCEDURE set_last_updated();


CREATE TABLE care_resource_specialty (
	care_resource_specialty_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	name VARCHAR NOT NULL,
	care_resource_specialty_group_id VARCHAR NOT NULL REFERENCES care_resource_specialty_group,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON care_resource_specialty FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE care_resource_specialty_resource (
	care_resource_specialty_resource_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	care_resource_specialty_id UUID NOT NULL REFERENCES care_resource_specialty,
	care_resource_id UUID NOT NULL REFERENCES care_resource,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON care_resource_specialty_resource FOR EACH ROW EXECUTE PROCEDURE set_last_updated();


CREATE TABLE patient_order_resource_packet (
	patient_order_resource_packet_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	patient_order_id UUID NOT NULL REFERENCES patient_order,
	current_flag BOOLEAN NOT NULL DEFAULT true,
	payor_id VARCHAR NOT NULL REFERENCES payor,
	postal_code VARCHAR NOT NULL,
	travel_radius INTEGER NOT NULL,
	travel_radius_distance_unit_id VARCHAR NOT NULL REFERENCES distance_unit(distance_unit_id) DEFAULT 'MILE',
	support_role_id VARCHAR NOT NULL REFERENCES support_role,
	intro_text VARCHAR NOT NULL,
	end_text VARCHAR NULL,
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

--SELECT DISTINCT 'INSERT INTO payor VALUES ('||''''|| UPPER(TRIM(REPLACE(primary_payor_name,' ','_')))||''''||','||'''' ||primary_payor_name||''''||');'
--from patient_order 
--where lower(primary_payor_name) not like 'zzz%'
--and test_patient_order = false;

INSERT INTO care_resource_specialty_group 
VALUES
('HOME_LIFE', 'Home Life'),
('IDENTITY', 'Identity'),
('LANGUAGES', 'Languages');

INSERT INTO care_resource_specialty VALUES ('8787ba6e-19ee-4c73-a2b5-91f8807a139e', 'Family', 'HOME_LIFE');
INSERT INTO care_resource_specialty VALUES ('9fbcd9f0-1f5f-4cb2-b9d6-f751a84fef62', 'Marriage', 'HOME_LIFE');
INSERT INTO care_resource_specialty VALUES ('30d229c6-796d-4e0b-b80a-9b0fd57b7878', 'LGBTQ', 'IDENTITY');
INSERT INTO care_resource_specialty VALUES ('bc01e110-9d92-4d64-bec4-1d594a9d5041', 'Transgender', 'IDENTITY');
INSERT INTO care_resource_specialty VALUES ('fa50751d-c8e3-46b2-bc3f-fb27db359266', 'Spanish', 'LANGUAGES');
INSERT INTO care_resource_specialty VALUES ('38ba3468-7d67-409a-8404-f137781ffb9c', 'French', 'LANGUAGES');

INSERT INTO payor VALUES ('ALLSTATE','ALLSTATE');
INSERT INTO payor VALUES ('AMERIHEALTH','AMERIHEALTH');
INSERT INTO payor VALUES ('THIRD_PARTY_LIABILITY','THIRD PARTY LIABILITY');
INSERT INTO payor VALUES ('HIGHMARK_BC_BS_OF_PA_MANAGED_CARE','HIGHMARK BC BS OF PA MANAGED CARE');
INSERT INTO payor VALUES ('MULTIPLAN','MULTIPLAN');
INSERT INTO payor VALUES ('CIGNA_MEDICARE','CIGNA MEDICARE');
INSERT INTO payor VALUES ('MGD_MA_OTHER','MGD MA OTHER');
INSERT INTO payor VALUES ('MGD_CARE_OTHER','MGD CARE OTHER');
INSERT INTO payor VALUES ('MGD_MEDICARE_OTHER','MGD MEDICARE OTHER');
INSERT INTO payor VALUES ('AETNA','AETNA');
INSERT INTO payor VALUES ('BLUE_CROSS','BLUE CROSS');
INSERT INTO payor VALUES ('KEYSTONE_FIRST_MEDICAID','KEYSTONE FIRST MEDICAID');
INSERT INTO payor VALUES ('PATIENT_ASSISTANCE_PROGRAM-HB_ONLY','PATIENT ASSISTANCE PROGRAM-HB ONLY');
INSERT INTO payor VALUES ('HIGHMARK_WHOLECARE_HEALTH_MEDICARE','HIGHMARK WHOLECARE HEALTH MEDICARE');
INSERT INTO payor VALUES ('AETNA_MEDICARE','AETNA MEDICARE');
INSERT INTO payor VALUES ('HORIZON','HORIZON');
INSERT INTO payor VALUES ('STATE_FARM','STATE FARM');
INSERT INTO payor VALUES ('HUMANA','HUMANA');
INSERT INTO payor VALUES ('NON_PAR_PAYOR_MANAGED_CARE','NON PAR PAYOR MANAGED CARE');
INSERT INTO payor VALUES ('VSP','VSP');
INSERT INTO payor VALUES ('HEALTH_PARTNERS_MEDICAID','HEALTH PARTNERS MEDICAID');
INSERT INTO payor VALUES ('INDEPENDENCE_ADMIN','INDEPENDENCE ADMIN');
INSERT INTO payor VALUES ('COMM_BEHAVIOR_HEALTH','COMM BEHAVIOR HEALTH');
INSERT INTO payor VALUES ('CLOVER_HEALTH','CLOVER HEALTH');
INSERT INTO payor VALUES ('AMERIHEALTH_ADMIN','AMERIHEALTH ADMIN');
INSERT INTO payor VALUES ('PA_HEALTH_AND_WELLNESS_MEDICAID','PA HEALTH AND WELLNESS MEDICAID');
INSERT INTO payor VALUES ('OPTUM','OPTUM');
INSERT INTO payor VALUES ('OSCAR_HEALTH','OSCAR HEALTH');
INSERT INTO payor VALUES ('COMM','COMM');
INSERT INTO payor VALUES ('PA_HEALTH_AND_WELLNESS_EXCHANGE','PA HEALTH AND WELLNESS EXCHANGE');
INSERT INTO payor VALUES ('CIGNA','CIGNA');
INSERT INTO payor VALUES ('DELTA_DENTAL','DELTA DENTAL');
INSERT INTO payor VALUES ('TRICARE','TRICARE');
INSERT INTO payor VALUES ('MAGELLAN','MAGELLAN');
INSERT INTO payor VALUES ('MEDICARE','MEDICARE');
INSERT INTO payor VALUES ('PA_MA','PA MA');
INSERT INTO payor VALUES ('NON_PAR_PAYOR','NON PAR PAYOR');
INSERT INTO payor VALUES ('AMERIHEALTH_65','AMERIHEALTH 65');
INSERT INTO payor VALUES ('BLUE_SHIELD','BLUE SHIELD');
INSERT INTO payor VALUES ('WORKERS_COMP','WORKERS COMP');
INSERT INTO payor VALUES ('RAILROAD_MEDICARE','RAILROAD MEDICARE');
INSERT INTO payor VALUES ('UNKNOWN_PAYOR','UNKNOWN PAYOR');
INSERT INTO payor VALUES ('UNITED_HEALTHCARE','UNITED HEALTHCARE');
INSERT INTO payor VALUES ('IMAGINE_HEALTH/IMAGINE_HEALTH_360','IMAGINE HEALTH/IMAGINE HEALTH 360');
INSERT INTO payor VALUES ('PA_HEALTH_AND_WELLNESS_MEDICARE_ADV','PA HEALTH AND WELLNESS MEDICARE ADV');
INSERT INTO payor VALUES ('GEISINGER','GEISINGER');
INSERT INTO payor VALUES ('IBC_MEDICARE','IBC MEDICARE');
INSERT INTO payor VALUES ('NON_PAR_PAYOR-MANAGED_MEDICAID','NON PAR PAYOR-MANAGED MEDICAID');
INSERT INTO payor VALUES ('KEYSTONE_FIRST_MEDICARE','KEYSTONE FIRST MEDICARE');
INSERT INTO payor VALUES ('CLAIM_WATCHER','CLAIM WATCHER');
INSERT INTO payor VALUES ('FIRST_HEALTH_NETWORK','FIRST HEALTH NETWORK');
INSERT INTO payor VALUES ('HEALTHPARTNERS_MC','HEALTHPARTNERS MC');
INSERT INTO payor VALUES ('GREAT_WEST_HEALTH_CARE','GREAT WEST HEALTH CARE');
INSERT INTO payor VALUES ('BLUE_CROSS_MEDICARE','BLUE CROSS MEDICARE');
INSERT INTO payor VALUES ('MGD_CARE','MGD CARE');
INSERT INTO payor VALUES ('HUMANA_MEDICARE','HUMANA MEDICARE');
INSERT INTO payor VALUES ('DAVIS_VISION','DAVIS VISION');
INSERT INTO payor VALUES ('IBC','IBC');
INSERT INTO payor VALUES ('GEISINGER_MEDICAID','GEISINGER MEDICAID');
INSERT INTO payor VALUES ('BAMS_WORKERS_COMP','BAMS WORKERS COMP');
INSERT INTO payor VALUES ('UNITED_HEALTH_CARE_COMMUNITY_PLAN','UNITED HEALTH CARE COMMUNITY PLAN');
INSERT INTO payor VALUES ('VETERANS_ADMINISTRATION','VETERANS ADMINISTRATION');

COMMIT;