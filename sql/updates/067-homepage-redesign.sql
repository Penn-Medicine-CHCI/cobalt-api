BEGIN;
SELECT _v.register_patch('067-homepage-redesign', NULL, NULL);

CREATE TABLE navigation_header (
  navigation_header_id VARCHAR PRIMARY KEY,
  name VARCHAR NOT NULL,
  created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW());

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON navigation_header FOR EACH ROW EXECUTE PROCEDURE set_last_updated(); 


--Avaialble Cobalt features to be highlighted on the hoempage
CREATE TABLE feature (
  feature_id VARCHAR PRIMARY KEY,
  navigation_header_id VARCHAR REFERENCES navigation_header,
  name VARCHAR NOT NULL,
  url_name VARCHAR NOT NULL,
  created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON feature FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE feature_support_role (
  feature_support_role_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  feature_id VARCHAR NOT NULL REFERENCES feature,
  support_role_id VARCHAR REFERENCES support_role,
  created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX feature_support_role_id_unique_idx ON feature_support_role USING btree (feature_id, support_role_id);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON feature_support_role FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

--Filters available when filtering feature options
CREATE TABLE filter (
	filter_id VARCHAR PRIMARY KEY,
	name VARCHAR NOT NULL,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON filter FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

--Determines which filters will be shown for a feature
CREATE TABLE feature_filter (
	feature_filter_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	feature_id VARCHAR NOT NULL REFERENCES feature,
	filter_id VARCHAR NOT NULL REFERENCES filter,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX feature_filter_unique_idx ON feature_filter USING btree (feature_id, filter_id);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON feature_filter FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

--Associates the available features to an institution
CREATE TABLE institution_feature (
	institution_feature_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	institution_id VARCHAR NOT NULL REFERENCES institution,
	feature_id VARCHAR NOT NULL REFERENCES feature,
	nav_description VARCHAR NOT NULL,
	description VARCHAR NOT NULL,
	display_order INTEGER NOT NULL,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX institution_feature_unique_idx ON institution_feature USING btree (institution_id, feature_id);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON institution_feature FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

--When a feature is recommended to a user we write a record to this table
CREATE TABLE screening_session_feature_recommendation (
	screening_session_feature_recommendation_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	screening_session_id UUID NOT NULL REFERENCES screening_session,
	feature_id VARCHAR NOT NULL REFERENCES feature,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
 );

CREATE UNIQUE INDEX screening_session_feature_recommendation_unique_idx ON screening_session_feature_recommendation USING btree (screening_session_id, feature_id);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON screening_session_feature_recommendation FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

--Stores the locations for an institution
CREATE TABLE institution_location (
	institution_location_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	institution_id VARCHAR NOT NULL REFERENCES institution,
	name VARCHAR NOT NULL,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW());

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON institution_location FOR EACH ROW EXECUTE PROCEDURE set_last_updated(); 

ALTER TABLE account ADD COLUMN institution_location_id UUID REFERENCES institution_location;
ALTER TABLE account ADD COLUMN prompted_for_institution_location BOOLEAN NOT NULL DEFAULT false;

--Available appointment time ranges
CREATE TABLE appointment_time (
	appointment_time_id VARCHAR PRIMARY KEY,
	name VARCHAR NOT NULL,
	description VARCHAR NOT NULL,
	start_time TIMETZ NOT NULL,
	end_time TIMETZ NOT NULL,
	display_order INTEGER NOT NULL,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW());

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON appointment_time FOR EACH ROW EXECUTE PROCEDURE set_last_updated(); 

CREATE TABLE provider_institution_location (
  provider_location_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  provider_id UUID NOT NULL REFERENCES provider,
  institution_location_id UUID NOT NULL REFERENCES institution_location,
  created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW());

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON provider_institution_location FOR EACH ROW EXECUTE PROCEDURE set_last_updated(); 

ALTER TABLE screening_flow_version ADD COLUMN minutes_until_retake INTEGER NOT NULL DEFAULT 1440;
ALTER TABLE screening_flow_version ADD COLUMN recommendation_expiration_minutes INTEGER NOT NULL DEFAULT 8760;


ALTER TABLE clinic ADD COLUMN image_url VARCHAR;
ALTER TABLE clinic ADD COLUMN phone_number VARCHAR;
ALTER TABLE clinic ADD COLUMN locale VARCHAR NOT NULL DEFAULT 'en-US'::character varying;

ALTER TABLE provider ADD COLUMN phone_number VARCHAR;
ALTER TABLE provider ADD COLUMN display_phone_number_only_for_booking BOOLEAN DEFAULT false;

INSERT INTO navigation_header
  (navigation_header_id, name)
VALUES
  ('CONNECT_WITH_SUPPORT', 'Connect with Support'),
  ('BROWSE_RESOURCES', 'Browse Resources');

INSERT INTO feature 
  (feature_id, name, url_name, navigation_header_id)
VALUES
  ('THERAPY', 'Therapy', '/connect-with-support/therapy', 'CONNECT_WITH_SUPPORT'),
  ('MEDICATION_PRESCRIBER', 'Medication Prescriber', '/connect-with-support/medication-prescriber', 'CONNECT_WITH_SUPPORT'),
  ('GROUP_SESSIONS', 'Group Sessions', '/group-sessions', 'CONNECT_WITH_SUPPORT'),
  ('COACHING', 'Coaching', '/connect-with-support/coaching', 'CONNECT_WITH_SUPPORT'),
  ('SELF_HELP_RESOURCES', 'Self-Help Resources', '/resource-library', 'BROWSE_RESOURCES'),
  ('SPIRITUAL_SUPPORT', 'Spiritual Support', '/connect-with-support/spiritual-support', 'CONNECT_WITH_SUPPORT'),
  ('CRISIS_SUPPORT', 'Crisis Support', '/in-crisis', 'CONNECT_WITH_SUPPORT');

INSERT INTO filter
  (filter_id, name)
VALUES 
  ('DATE', 'Date'),
  ('TIME_OF_DAY', 'Time of day'),
  ('LOCATION', 'Location');

INSERT INTO feature_filter
  (feature_id, filter_id)
VALUES
  ('THERAPY', 'DATE'),
  ('THERAPY', 'TIME_OF_DAY'),
  ('THERAPY', 'LOCATION'),
  ('COACHING', 'TIME_OF_DAY'),
  ('COACHING', 'LOCATION'),
  ('SPIRITUAL_SUPPORT', 'DATE');

INSERT INTO institution_feature
  (institution_id, feature_id, nav_description, description, display_order)
VALUES
  ('COBALT', 'THERAPY', 'Connect to a therapist through your Employee Assistance Program or TEAM Clinic','If you''d like to talk to a therapist, you can schedule with your Employee Assistance Program (EAP) or the TEAM Clinic. EAP offers 8 free and confidential counseling sessions and visits are not documented in electronic medical records (EMR). TEAM Clinic is based in Cobalt Psychiatry and diagnoses and treats patients as part of a 4-month long, outpatient program. TEAM clinic bills insurance for your visits.', 1),
  ('COBALT', 'MEDICATION_PRESCRIBER', 'Discuss medication prescription options through the TEAM Clinic', 'If you’re looking to find a provider to discuss medication options to address mental health symptoms, please connect with the TEAM Clinic. Based in Cobalt Psychiatry, the Time Efficient, Accessible, Multidisciplinary (TEAM) Clinic uses an evidence-based and collaborative approach to diagnose and treat mental health conditions as part of a 4-month long, outpatient program.',2),
  ('COBALT', 'GROUP_SESSIONS', 'Register for topical group sessions led by experts', 'Virtual sessions led by experts and designed to foster connection and provide support for people experiencing similar issues or concerns. Topics range from managing anxiety to healthy living and mindfulness.', 3),
  ('COBALT', 'COACHING', 'Get 1:1 confidential emotional support from trained volunteers', 'If you’d like to connect with a coach, Coping First Aid is a free resource for you. Flexible appointments are available now with lay health coaches supervised by licensed mental health professionals. You can receive one-to-one support with a trained coach to work with you on coping and resilience strategies.', 4),
  ('COBALT', 'SELF_HELP_RESOURCES', 'Digital articles, podcasts, apps, videos, worksheets, and more', 'A variety of self-directed digital resources, including articles, podcasts, apps, videos, worksheets and more, that help support general wellness and mental health education.', 5),
  ('COBALT', 'SPIRITUAL_SUPPORT', 'Receive confidential, non-judgmental support from multi-faith chaplains', 'If you’re looking for spiritual support, chaplains are available for 30-minute anonymous and confidential appointments. Chaplains respect your experiences, understand the complexities inherent in belief systems, and help you to uncover your strengths. A part of the healthcare team, chaplains are professionally trained to offer emotional and spiritual support.' ,6),
  ('COBALT', 'CRISIS_SUPPORT', 'Get contact information for immediate help', 'If you are in crisis, contact one of the listed resources for immediate help or go to your nearest emergency department or crisis center.', 7);

INSERT INTO institution_location 
  (institution_id, name)
VALUES
  ('COBALT', 'Cobalt Health System'),
  ('COBALT', 'Cobalt General');

INSERT INTO appointment_time
  (appointment_time_id, name, description, start_time, end_time, display_order)
VALUES
  ('EARLY_MORNING', 'Early Morning', 'Starts before 10am', '00:00', '10:00', 1),
  ('MORNING', 'Morning', 'Starts before 12pm', '10:00', '12:00', 2),
  ('AFTERNOON', 'Afternoon', 'Starts after 12pm', '12:00', '17:00', 3),
  ('EVENING', 'Evening', 'Starts after 5pm', '17:00', '23:59:59', 4);

 INSERT INTO feature_support_role
   (feature_id, support_role_id)
 VALUES
   ('THERAPY', 'CLINICIAN'),
   ('COACHING', 'COACH'),
   ('SPIRITUAL_SUPPORT', 'CHAPLAIN');

INSERT INTO screening_flow_type
    (screening_flow_type_id, description)
VALUES
    ('FEATURE', 'Feature');

ALTER TABLE institution ADD COLUMN feature_screening_flow_id UUID REFERENCES screening_flow(screening_flow_id);
ALTER TABLE institution ADD COLUMN features_enabled BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE institution SET features_enabled=TRUE WHERE institution_id='COBALT';

COMMIT;



