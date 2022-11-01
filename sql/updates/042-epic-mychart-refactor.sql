BEGIN;
SELECT _v.register_patch('042-epic-mychart-refactor', NULL, NULL);

-- Institution-level changes

ALTER TABLE institution ADD COLUMN integrated_care_enabled BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE institution ADD COLUMN epic_client_id TEXT;
ALTER TABLE institution ADD COLUMN epic_user_id TEXT; -- e.g. COBALT
ALTER TABLE institution ADD COLUMN epic_user_id_type TEXT; -- e.g. EXTERNAL
ALTER TABLE institution ADD COLUMN epic_username TEXT;
ALTER TABLE institution ADD COLUMN epic_password TEXT;
ALTER TABLE institution ADD COLUMN epic_base_url TEXT;
ALTER TABLE institution ADD COLUMN mychart_client_id TEXT;
ALTER TABLE institution ADD COLUMN mychart_scope TEXT;
ALTER TABLE institution ADD COLUMN mychart_response_type TEXT;
ALTER TABLE institution ADD COLUMN mychart_token_url TEXT;
ALTER TABLE institution ADD COLUMN mychart_authorize_url TEXT;
ALTER TABLE institution ADD COLUMN mychart_callback_url TEXT;

INSERT INTO account_source (account_source_id, description) VALUES ('MYCHART', 'MyChart');

-- Account-level changes

CREATE TABLE gender_identity (
	gender_identity_id TEXT PRIMARY KEY,
	description TEXT NOT NULL,
	display_order SMALLINT NOT NULL
);

-- See https://hl7.org/fhir/codesystem-gender-identity.html and http://hl7.org/fhir/StructureDefinition/patient-genderIdentity
-- code | display | definition
--  transgender-female | transgender female | the patient identifies as transgender male-to-female
--  transgender-male | transgender male | the patient identifies as transgender female-to-male
--  non-binary | non-binary | the patient identifies with neither/both female and male
--  male | male | the patient identifies as male
--  female | female | the patient identifies as female
--  other |	other |	other gender identity
--  non-disclose | does not wish to disclose | the patient does not wish to disclose his gender identity
INSERT INTO gender_identity (gender_identity_id, description, display_order) VALUES ('NOT_ASKED', 'Not asked', 1);
INSERT INTO gender_identity (gender_identity_id, description, display_order) VALUES ('MALE', 'Male', 2);
INSERT INTO gender_identity (gender_identity_id, description, display_order) VALUES ('FEMALE', 'Female', 3);
INSERT INTO gender_identity (gender_identity_id, description, display_order) VALUES ('TRANSGENDER_MTF', 'Transgender male-to-female', 4);
INSERT INTO gender_identity (gender_identity_id, description, display_order) VALUES ('TRANSGENDER_FTM', 'Transgender female-to-male', 5);
INSERT INTO gender_identity (gender_identity_id, description, display_order) VALUES ('NON_BINARY', 'Non-binary', 6);
INSERT INTO gender_identity (gender_identity_id, description, display_order) VALUES ('OTHER', 'Other gender identity', 7);
INSERT INTO gender_identity (gender_identity_id, description, display_order) VALUES ('NOT_DISCLOSED', 'Do not wish to disclose', 8);

-- urn:oid:2.16.840.1.113883.6.238
-- 2135-2 Hispanic or Latino
-- 2186-5 Not Hispanic or Latino

CREATE TABLE ethnicity (
	ethnicity_id TEXT PRIMARY KEY,
	description TEXT NOT NULL,
	display_order SMALLINT NOT NULL
);

INSERT INTO ethnicity (ethnicity_id, description, display_order) VALUES ('NOT_ASKED', 'Not asked', 1);
INSERT INTO ethnicity (ethnicity_id, description, display_order) VALUES ('HISPANIC_OR_LATINO', 'Hispanic or Latino', 2);
INSERT INTO ethnicity (ethnicity_id, description, display_order) VALUES ('NOT_HISPANIC_OR_LATINO', 'Not Hispanic or Latino', 3);
INSERT INTO ethnicity (ethnicity_id, description, display_order) VALUES ('NOT_DISCLOSED', 'Do not wish to disclose', 4);

-- LP189793-5   Sex assigned at birth
-- Male LA2-8
-- Female LA3-6
-- Unknown LA4489-6
CREATE TABLE birth_sex (
	birth_sex_id TEXT PRIMARY KEY,
	description TEXT NOT NULL,
	display_order SMALLINT NOT NULL
);

INSERT INTO birth_sex (birth_sex_id, description, display_order) VALUES ('NOT_ASKED', 'Not asked', 1);
INSERT INTO birth_sex (birth_sex_id, description, display_order) VALUES ('MALE', 'Male', 2);
INSERT INTO birth_sex (birth_sex_id, description, display_order) VALUES ('FEMALE', 'Female', 3);
INSERT INTO birth_sex (birth_sex_id, description, display_order) VALUES ('UNKNOWN', 'Unknown', 4);
INSERT INTO birth_sex (birth_sex_id, description, display_order) VALUES ('NOT_DISCLOSED', 'Do not wish to disclose', 5);

-- Race 2.16.840.1.114222.4.11.6065
-- Concept Code - Description
-- 	1002-5	American Indian or Alaska Native
--   	2028-9	Asian
--   	2054-5	Black or African American
--   	2076-8	Native Hawaiian or Other Pacific Islander
--   	2131-1	Other
--   	2106-3	White
CREATE TABLE race (
	race_id TEXT PRIMARY KEY,
	description TEXT NOT NULL,
	display_order SMALLINT NOT NULL
);

INSERT INTO race (race_id, description, display_order) VALUES ('NOT_ASKED', 'Not asked', 1);
INSERT INTO race (race_id, description, display_order) VALUES ('AMERICAN_INDIAN_OR_ALASKA_NATIVE', 'American Indian or Alaska Native', 2);
INSERT INTO race (race_id, description, display_order) VALUES ('ASIAN', 'Asian', 3);
INSERT INTO race (race_id, description, display_order) VALUES ('BLACK_OR_AFRICAN_AMERICAN', 'Black or African American', 4);
INSERT INTO race (race_id, description, display_order) VALUES ('HAWAIIAN_OR_PACIFIC_ISLANDER', 'Native Hawaiian or Other Pacific Islander', 5);
INSERT INTO race (race_id, description, display_order) VALUES ('OTHER', 'Other', 6);
INSERT INTO race (race_id, description, display_order) VALUES ('WHITE', 'White', 7);
INSERT INTO race (race_id, description, display_order) VALUES ('NOT_DISCLOSED', 'Do not wish to disclose', 8);

CREATE TABLE address (
	address_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	postal_name TEXT NOT NULL,
	street_address_1 TEXT NOT NULL, -- First line of street address, e.g. "123 Fake St"
	street_address_2 TEXT,
	street_address_3 TEXT,
	street_address_4 TEXT,
	post_office_box_number TEXT,
	cross_street TEXT,
	suburb TEXT,
	locality TEXT, -- In the US, city name
	region TEXT, -- In the US, state abbreviation
	postal_code TEXT NOT NULL, -- In the US, ZIP Code
	country_subdivision_code TEXT,
	country_code TEXT NOT NULL, -- Two-letter ISO 3166-1 alpha-2 country code
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON address FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

ALTER TABLE account ADD COLUMN gender_identity_id TEXT REFERENCES gender_identity NOT NULL DEFAULT 'NOT_ASKED';
ALTER TABLE account ADD COLUMN ethnicity_id TEXT REFERENCES ethnicity NOT NULL DEFAULT 'NOT_ASKED';
ALTER TABLE account ADD COLUMN birth_sex_id TEXT REFERENCES birth_sex NOT NULL DEFAULT 'NOT_ASKED';
ALTER TABLE account ADD COLUMN race_id TEXT REFERENCES race NOT NULL DEFAULT 'NOT_ASKED';
ALTER TABLE account ADD COLUMN address_id UUID REFERENCES address;
ALTER TABLE account ADD COLUMN birthdate DATE;
ALTER TABLE account ADD COLUMN mychart_patient_record JSONB;
ALTER TABLE account ADD COLUMN mychart_patient_record_last_imported_at TIMESTAMPTZ;

COMMIT;