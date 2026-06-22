BEGIN;
SELECT _v.register_patch('257-local-only-provider-clinic-location-test-data', ARRAY['257-provider-clinic-locations'], NULL);

-- Local-only owner-specific location seed data for QA and developer databases.
-- This script depends on the provider_location and clinic_location schema from
-- 257-provider-clinic-locations and is intentionally separate from production
-- functional migrations.

WITH fixture_provider_location AS (
	SELECT
		provider.provider_id,
		provider_location_fixture.name,
		provider_location_fixture.short_name,
		provider_location_fixture.display_order,
		provider_location_fixture.phone_number,
		provider_location_fixture.website_url,
		provider_location_fixture.email_address,
		provider_location_fixture.postal_name,
		provider_location_fixture.street_address_1,
		provider_location_fixture.locality,
		provider_location_fixture.region,
		provider_location_fixture.postal_code,
		provider_location_fixture.formatted_address,
		UUID_GENERATE_V5('bbab3b22-2e18-4dd0-ae89-926e0f9f9a31'::UUID, FORMAT('provider-location-address:%s:%s', provider.provider_id, provider_location_fixture.name)) AS address_id
	FROM (VALUES
		('15f9711d-38e1-44a1-a933-f1522ddd2c81'::UUID, NULL::TEXT, 'Dr. Allen Fixture Office', 'Allen Office', 1, '+12155554001', 'https://fixtures.cobalt.care/locations/dr-allen-office', 'dr-allen-office@example.com', 'Dr. Allen Fixture Office', '4101 Fixture Provider Lane', 'Philadelphia', 'PA', '19104', '4101 Fixture Provider Lane, Philadelphia, PA 19104'),
		('31633b9d-651b-402b-9314-7def6af811b6'::UUID, NULL::TEXT, 'Dr. Spence Anxiety Clinic Office', 'Spence Office', 1, '+12155554002', 'https://fixtures.cobalt.care/locations/dr-spence-office', 'dr-spence-office@example.com', 'Dr. Spence Anxiety Clinic Office', '4102 Fixture Provider Lane', 'Philadelphia', 'PA', '19104', '4102 Fixture Provider Lane, Philadelphia, PA 19104'),
		('360d46c4-2ee9-4031-aab6-aa6a16f398d7'::UUID, NULL::TEXT, 'Dr. Fritz Therapy Office', 'Fritz Office', 1, '+12155554003', 'https://fixtures.cobalt.care/locations/dr-fritz-office', 'dr-fritz-office@example.com', 'Dr. Fritz Therapy Office', '4103 Fixture Provider Lane', 'Philadelphia', 'PA', '19104', '4103 Fixture Provider Lane, Philadelphia, PA 19104'),
		(NULL::UUID, 'John Skokowski', 'John Skokowski Autism Intake Office', 'Autism Intake', 1, '+12155554012', 'https://fixtures.cobalt.care/locations/autism-intake-office', 'autism-intake-office@example.com', 'John Skokowski Autism Intake Office', '4112 Fixture Provider Lane', 'Philadelphia', 'PA', '19104', '4112 Fixture Provider Lane, Philadelphia, PA 19104')
	) AS provider_location_fixture(provider_id, provider_name, name, short_name, display_order, phone_number, website_url, email_address, postal_name, street_address_1, locality, region, postal_code, formatted_address)
	JOIN provider
		ON (
			provider_location_fixture.provider_id IS NOT NULL
			AND provider.provider_id=provider_location_fixture.provider_id
		)
		OR (
			provider_location_fixture.provider_name IS NOT NULL
			AND provider.institution_id='COBALT'
			AND provider.name=provider_location_fixture.provider_name
		)
)
INSERT INTO address (
	address_id,
	postal_name,
	street_address_1,
	locality,
	region,
	postal_code,
	country_code,
	formatted_address
)
SELECT
	address_id,
	postal_name,
	street_address_1,
	locality,
	region,
	postal_code,
	'US',
	formatted_address
FROM fixture_provider_location
ON CONFLICT (address_id) DO UPDATE
SET postal_name=EXCLUDED.postal_name,
	street_address_1=EXCLUDED.street_address_1,
	locality=EXCLUDED.locality,
	region=EXCLUDED.region,
	postal_code=EXCLUDED.postal_code,
	country_code=EXCLUDED.country_code,
	formatted_address=EXCLUDED.formatted_address;

WITH fixture_provider_location AS (
	SELECT
		provider.provider_id,
		provider_location_fixture.name,
		provider_location_fixture.short_name,
		provider_location_fixture.display_order,
		provider_location_fixture.phone_number,
		provider_location_fixture.website_url,
		provider_location_fixture.email_address,
		UUID_GENERATE_V5('bbab3b22-2e18-4dd0-ae89-926e0f9f9a31'::UUID, FORMAT('provider-location-address:%s:%s', provider.provider_id, provider_location_fixture.name)) AS address_id,
		UUID_GENERATE_V5('66bf82b8-9411-45ff-a384-b83f1f914d02'::UUID, FORMAT('provider-location:%s:%s', provider.provider_id, provider_location_fixture.name)) AS provider_location_id
	FROM (VALUES
		('15f9711d-38e1-44a1-a933-f1522ddd2c81'::UUID, NULL::TEXT, 'Dr. Allen Fixture Office', 'Allen Office', 1, '+12155554001', 'https://fixtures.cobalt.care/locations/dr-allen-office', 'dr-allen-office@example.com'),
		('31633b9d-651b-402b-9314-7def6af811b6'::UUID, NULL::TEXT, 'Dr. Spence Anxiety Clinic Office', 'Spence Office', 1, '+12155554002', 'https://fixtures.cobalt.care/locations/dr-spence-office', 'dr-spence-office@example.com'),
		('360d46c4-2ee9-4031-aab6-aa6a16f398d7'::UUID, NULL::TEXT, 'Dr. Fritz Therapy Office', 'Fritz Office', 1, '+12155554003', 'https://fixtures.cobalt.care/locations/dr-fritz-office', 'dr-fritz-office@example.com'),
		(NULL::UUID, 'John Skokowski', 'John Skokowski Autism Intake Office', 'Autism Intake', 1, '+12155554012', 'https://fixtures.cobalt.care/locations/autism-intake-office', 'autism-intake-office@example.com')
	) AS provider_location_fixture(provider_id, provider_name, name, short_name, display_order, phone_number, website_url, email_address)
	JOIN provider
		ON (
			provider_location_fixture.provider_id IS NOT NULL
			AND provider.provider_id=provider_location_fixture.provider_id
		)
		OR (
			provider_location_fixture.provider_name IS NOT NULL
			AND provider.institution_id='COBALT'
			AND provider.name=provider_location_fixture.provider_name
		)
)
INSERT INTO provider_location (
	provider_location_id,
	provider_id,
	address_id,
	name,
	short_name,
	display_order,
	phone_number,
	website_url,
	email_address
)
SELECT
	provider_location_id,
	provider_id,
	address_id,
	name,
	short_name,
	display_order,
	phone_number,
	website_url,
	email_address
FROM fixture_provider_location
ON CONFLICT (provider_location_id) DO UPDATE
SET provider_id=EXCLUDED.provider_id,
	address_id=EXCLUDED.address_id,
	name=EXCLUDED.name,
	short_name=EXCLUDED.short_name,
	display_order=EXCLUDED.display_order,
	phone_number=EXCLUDED.phone_number,
	website_url=EXCLUDED.website_url,
	email_address=EXCLUDED.email_address;

WITH fixture_clinic_location AS (
	SELECT
		clinic.clinic_id,
		clinic_location_fixture.name,
		clinic_location_fixture.short_name,
		clinic_location_fixture.display_order,
		clinic_location_fixture.phone_number,
		clinic_location_fixture.website_url,
		clinic_location_fixture.email_address,
		clinic_location_fixture.postal_name,
		clinic_location_fixture.street_address_1,
		clinic_location_fixture.locality,
		clinic_location_fixture.region,
		clinic_location_fixture.postal_code,
		clinic_location_fixture.formatted_address,
		UUID_GENERATE_V5('a4b09f9d-f2e0-48b6-b0cc-9f99187dff96'::UUID, FORMAT('clinic-location-address:%s:%s', clinic.clinic_id, clinic_location_fixture.name)) AS address_id
	FROM (VALUES
		('d789dbdb-6756-4293-836d-91b7329fb49c'::UUID, NULL::TEXT, 'Behavioral Wellness Fixture Front Desk', 'Wellness Front Desk', 1, '+12155555001', 'https://fixtures.cobalt.care/locations/behavioral-wellness-front-desk', 'behavioral-wellness-location@example.com', 'Behavioral Wellness Fixture Front Desk', '4201 Fixture Clinic Avenue', 'Philadelphia', 'PA', '19107', '4201 Fixture Clinic Avenue, Philadelphia, PA 19107'),
		('b6c5e9a3-6018-473d-86d4-2861a328e537'::UUID, NULL::TEXT, 'Stress and Anxiety Fixture Front Desk', 'Anxiety Front Desk', 1, '+12155555002', 'https://fixtures.cobalt.care/locations/stress-anxiety-front-desk', 'stress-anxiety-location@example.com', 'Stress and Anxiety Fixture Front Desk', '4202 Fixture Clinic Avenue', 'Philadelphia', 'PA', '19107', '4202 Fixture Clinic Avenue, Philadelphia, PA 19107'),
		('7872559f-b5f6-449f-892d-3f312cd691ff'::UUID, NULL::TEXT, 'Cognitive Therapy Fixture Front Desk', 'Cognitive Front Desk', 1, '+12155555003', 'https://fixtures.cobalt.care/locations/cognitive-therapy-front-desk', 'cognitive-therapy-location@example.com', 'Cognitive Therapy Fixture Front Desk', '4203 Fixture Clinic Avenue', 'Philadelphia', 'PA', '19107', '4203 Fixture Clinic Avenue, Philadelphia, PA 19107'),
		('ab629384-400a-4688-8465-04636ec2eaa2'::UUID, NULL::TEXT, 'Adult Autism Services Fixture Front Desk', 'Autism Front Desk', 1, '+12155555004', 'https://fixtures.cobalt.care/locations/adult-autism-front-desk', 'adult-autism-location@example.com', 'Adult Autism Services Fixture Front Desk', '4204 Fixture Clinic Avenue', 'Philadelphia', 'PA', '19107', '4204 Fixture Clinic Avenue, Philadelphia, PA 19107'),
		(NULL::UUID, 'Penn Autism Clinic', 'Penn Autism Clinic Fixture Intake Desk', 'Penn Autism Intake', 1, '+12155555012', 'https://fixtures.cobalt.care/locations/penn-autism-intake-desk', 'penn-autism-location@example.com', 'Penn Autism Clinic Fixture Intake Desk', '4212 Fixture Clinic Avenue', 'Philadelphia', 'PA', '19107', '4212 Fixture Clinic Avenue, Philadelphia, PA 19107')
	) AS clinic_location_fixture(clinic_id, clinic_description, name, short_name, display_order, phone_number, website_url, email_address, postal_name, street_address_1, locality, region, postal_code, formatted_address)
	JOIN clinic
		ON (
			clinic_location_fixture.clinic_id IS NOT NULL
			AND clinic.clinic_id=clinic_location_fixture.clinic_id
		)
		OR (
			clinic_location_fixture.clinic_description IS NOT NULL
			AND clinic.institution_id='COBALT'
			AND LOWER(TRIM(clinic.description))=LOWER(TRIM(clinic_location_fixture.clinic_description))
		)
)
INSERT INTO address (
	address_id,
	postal_name,
	street_address_1,
	locality,
	region,
	postal_code,
	country_code,
	formatted_address
)
SELECT
	address_id,
	postal_name,
	street_address_1,
	locality,
	region,
	postal_code,
	'US',
	formatted_address
FROM fixture_clinic_location
ON CONFLICT (address_id) DO UPDATE
SET postal_name=EXCLUDED.postal_name,
	street_address_1=EXCLUDED.street_address_1,
	locality=EXCLUDED.locality,
	region=EXCLUDED.region,
	postal_code=EXCLUDED.postal_code,
	country_code=EXCLUDED.country_code,
	formatted_address=EXCLUDED.formatted_address;

WITH fixture_clinic_location AS (
	SELECT
		clinic.clinic_id,
		clinic_location_fixture.name,
		clinic_location_fixture.short_name,
		clinic_location_fixture.display_order,
		clinic_location_fixture.phone_number,
		clinic_location_fixture.website_url,
		clinic_location_fixture.email_address,
		UUID_GENERATE_V5('a4b09f9d-f2e0-48b6-b0cc-9f99187dff96'::UUID, FORMAT('clinic-location-address:%s:%s', clinic.clinic_id, clinic_location_fixture.name)) AS address_id,
		UUID_GENERATE_V5('011e2f89-67a0-41e8-9cc1-c4214c9b363e'::UUID, FORMAT('clinic-location:%s:%s', clinic.clinic_id, clinic_location_fixture.name)) AS clinic_location_id
	FROM (VALUES
		('d789dbdb-6756-4293-836d-91b7329fb49c'::UUID, NULL::TEXT, 'Behavioral Wellness Fixture Front Desk', 'Wellness Front Desk', 1, '+12155555001', 'https://fixtures.cobalt.care/locations/behavioral-wellness-front-desk', 'behavioral-wellness-location@example.com'),
		('b6c5e9a3-6018-473d-86d4-2861a328e537'::UUID, NULL::TEXT, 'Stress and Anxiety Fixture Front Desk', 'Anxiety Front Desk', 1, '+12155555002', 'https://fixtures.cobalt.care/locations/stress-anxiety-front-desk', 'stress-anxiety-location@example.com'),
		('7872559f-b5f6-449f-892d-3f312cd691ff'::UUID, NULL::TEXT, 'Cognitive Therapy Fixture Front Desk', 'Cognitive Front Desk', 1, '+12155555003', 'https://fixtures.cobalt.care/locations/cognitive-therapy-front-desk', 'cognitive-therapy-location@example.com'),
		('ab629384-400a-4688-8465-04636ec2eaa2'::UUID, NULL::TEXT, 'Adult Autism Services Fixture Front Desk', 'Autism Front Desk', 1, '+12155555004', 'https://fixtures.cobalt.care/locations/adult-autism-front-desk', 'adult-autism-location@example.com'),
		(NULL::UUID, 'Penn Autism Clinic', 'Penn Autism Clinic Fixture Intake Desk', 'Penn Autism Intake', 1, '+12155555012', 'https://fixtures.cobalt.care/locations/penn-autism-intake-desk', 'penn-autism-location@example.com')
	) AS clinic_location_fixture(clinic_id, clinic_description, name, short_name, display_order, phone_number, website_url, email_address)
	JOIN clinic
		ON (
			clinic_location_fixture.clinic_id IS NOT NULL
			AND clinic.clinic_id=clinic_location_fixture.clinic_id
		)
		OR (
			clinic_location_fixture.clinic_description IS NOT NULL
			AND clinic.institution_id='COBALT'
			AND LOWER(TRIM(clinic.description))=LOWER(TRIM(clinic_location_fixture.clinic_description))
		)
)
INSERT INTO clinic_location (
	clinic_location_id,
	clinic_id,
	address_id,
	name,
	short_name,
	display_order,
	phone_number,
	website_url,
	email_address
)
SELECT
	clinic_location_id,
	clinic_id,
	address_id,
	name,
	short_name,
	display_order,
	phone_number,
	website_url,
	email_address
FROM fixture_clinic_location
ON CONFLICT (clinic_location_id) DO UPDATE
SET clinic_id=EXCLUDED.clinic_id,
	address_id=EXCLUDED.address_id,
	name=EXCLUDED.name,
	short_name=EXCLUDED.short_name,
	display_order=EXCLUDED.display_order,
	phone_number=EXCLUDED.phone_number,
	website_url=EXCLUDED.website_url,
	email_address=EXCLUDED.email_address;

WITH fixture_institution_location AS (
	SELECT
		institution.institution_id,
		institution_location_fixture.name,
		institution_location_fixture.short_name,
		institution_location_fixture.display_order,
		institution_location_fixture.phone_number,
		institution_location_fixture.website_url,
		institution_location_fixture.email_address,
		institution_location_fixture.postal_name,
		institution_location_fixture.street_address_1,
		institution_location_fixture.locality,
		institution_location_fixture.region,
		institution_location_fixture.postal_code,
		institution_location_fixture.formatted_address,
		UUID_GENERATE_V5('4215f128-e740-4d85-84a3-f3f0d9d1a480'::UUID, FORMAT('institution-location-address:%s:%s', institution.institution_id, institution_location_fixture.name)) AS address_id
	FROM (VALUES
		('COBALT', 'Cobalt Fixture Testing Center', 'Testing Center', 10, '+12155556001', 'https://fixtures.cobalt.care/locations/cobalt-testing-center', 'cobalt-testing-center@example.com', 'Cobalt Fixture Testing Center', '4301 Fixture Institution Road', 'Philadelphia', 'PA', '19103', '4301 Fixture Institution Road, Philadelphia, PA 19103'),
		('COBALT', 'Cobalt Fixture Telehealth Hub', 'Telehealth Hub', 11, '+12155556002', 'https://fixtures.cobalt.care/locations/cobalt-telehealth-hub', 'cobalt-telehealth-hub@example.com', 'Cobalt Fixture Telehealth Hub', '4302 Fixture Institution Road', 'Philadelphia', 'PA', '19103', '4302 Fixture Institution Road, Philadelphia, PA 19103'),
		('COBALT_IC_SELF_REFERRAL', 'Self Referral Fixture Intake Office', 'Self Referral Intake', 1, '+12155556003', 'https://fixtures.cobalt.care/locations/self-referral-intake-office', 'self-referral-intake-office@example.com', 'Self Referral Fixture Intake Office', '4303 Fixture Institution Road', 'Philadelphia', 'PA', '19103', '4303 Fixture Institution Road, Philadelphia, PA 19103')
	) AS institution_location_fixture(institution_id, name, short_name, display_order, phone_number, website_url, email_address, postal_name, street_address_1, locality, region, postal_code, formatted_address)
	JOIN institution
		ON institution.institution_id=institution_location_fixture.institution_id
)
INSERT INTO address (
	address_id,
	postal_name,
	street_address_1,
	locality,
	region,
	postal_code,
	country_code,
	formatted_address
)
SELECT
	address_id,
	postal_name,
	street_address_1,
	locality,
	region,
	postal_code,
	'US',
	formatted_address
FROM fixture_institution_location
ON CONFLICT (address_id) DO UPDATE
SET postal_name=EXCLUDED.postal_name,
	street_address_1=EXCLUDED.street_address_1,
	locality=EXCLUDED.locality,
	region=EXCLUDED.region,
	postal_code=EXCLUDED.postal_code,
	country_code=EXCLUDED.country_code,
	formatted_address=EXCLUDED.formatted_address;

WITH fixture_institution_location AS (
	SELECT
		institution.institution_id,
		institution_location_fixture.name,
		institution_location_fixture.short_name,
		institution_location_fixture.display_order,
		institution_location_fixture.phone_number,
		institution_location_fixture.website_url,
		institution_location_fixture.email_address,
		UUID_GENERATE_V5('4215f128-e740-4d85-84a3-f3f0d9d1a480'::UUID, FORMAT('institution-location-address:%s:%s', institution.institution_id, institution_location_fixture.name)) AS address_id,
		UUID_GENERATE_V5('878f317f-a5d3-4fd6-b653-e7d95b4f8bbf'::UUID, FORMAT('institution-location:%s:%s', institution.institution_id, institution_location_fixture.name)) AS institution_location_id
	FROM (VALUES
		('COBALT', 'Cobalt Fixture Testing Center', 'Testing Center', 10, '+12155556001', 'https://fixtures.cobalt.care/locations/cobalt-testing-center', 'cobalt-testing-center@example.com'),
		('COBALT', 'Cobalt Fixture Telehealth Hub', 'Telehealth Hub', 11, '+12155556002', 'https://fixtures.cobalt.care/locations/cobalt-telehealth-hub', 'cobalt-telehealth-hub@example.com'),
		('COBALT_IC_SELF_REFERRAL', 'Self Referral Fixture Intake Office', 'Self Referral Intake', 1, '+12155556003', 'https://fixtures.cobalt.care/locations/self-referral-intake-office', 'self-referral-intake-office@example.com')
	) AS institution_location_fixture(institution_id, name, short_name, display_order, phone_number, website_url, email_address)
	JOIN institution
		ON institution.institution_id=institution_location_fixture.institution_id
)
INSERT INTO institution_location (
	institution_location_id,
	institution_id,
	address_id,
	name,
	short_name,
	display_order,
	phone_number,
	website_url,
	email_address
)
SELECT
	institution_location_id,
	institution_id,
	address_id,
	name,
	short_name,
	display_order,
	phone_number,
	website_url,
	email_address
FROM fixture_institution_location
ON CONFLICT (institution_location_id) DO UPDATE
SET institution_id=EXCLUDED.institution_id,
	address_id=EXCLUDED.address_id,
	name=EXCLUDED.name,
	short_name=EXCLUDED.short_name,
	display_order=EXCLUDED.display_order,
	phone_number=EXCLUDED.phone_number,
	website_url=EXCLUDED.website_url,
	email_address=EXCLUDED.email_address;

COMMIT;
