BEGIN;
SELECT _v.register_patch('256-local-only-provider-booking-test-data', NULL, NULL);

UPDATE institution
SET booking_v2_enabled=TRUE
WHERE institution_id='COBALT';

-- Local-only provider booking seed data for QA and developer databases.
-- This script depends on bootstrap fixture rows and is intentionally run only by
-- sql/recreate-local and sql/recreate-bootstrap after initial/bootstrap.sql.
-- Do not run this as a production functional migration.

-- Refresh Cobalt provider-search fixture rows so local and test databases have
-- varied provider bio/description/phone/modality data, clinic
-- description/treatment-description/phone/image data, one clinic-level booking
-- aggregate, native availability, and feature/location-specific provider rows.
-- These UUID-scoped updates are no-ops in environments without the fixture rows.
UPDATE provider
SET bio=provider_fixture.bio,
	description=provider_fixture.description,
	phone_number=provider_fixture.phone_number,
	videoconference_platform_id=provider_fixture.videoconference_platform_id,
	display_phone_number_only_for_booking=FALSE
FROM (VALUES
	('15f9711d-38e1-44a1-a933-f1522ddd2c81'::UUID, 'Dr. Allen is a phone-booked therapy fixture with intentionally ambiguous appointment types.', 'Perinatal therapy and psychiatry consults with appointment type selection handled by phone.', '+12155551001', 'TELEPHONE'),
	('31633b9d-651b-402b-9314-7def6af811b6'::UUID, 'Dr. Spence treats anxiety and trauma concerns with a structured psychiatry approach.', 'Psychiatry visits for anxiety, panic, and trauma-related symptoms.', '+12155551002', 'SWITCHBOARD'),
	('360d46c4-2ee9-4031-aab6-aa6a16f398d7'::UUID, 'Dr. Fritz provides cognitive therapy with practical goal setting between sessions.', 'Cognitive therapy appointments for mood, stress, and behavior change.', '+12155551003', 'SWITCHBOARD'),
	('dc7aeafd-0fc8-4c4d-b09a-09d4dc3079c1'::UUID, 'Rabbi Grayson offers spiritual support for identity, grief, and major transitions.', 'Spiritual care appointments for reflection, meaning, grief, and transition support.', '+12155551004', 'SWITCHBOARD'),
	('2d6b7032-0145-4273-84f5-94e7238bc331'::UUID, 'Dr. Watson coaches patients who are navigating substance use goals and recovery supports.', 'Coaching sessions for substance use goals and treatment navigation.', '+12155551005', 'SWITCHBOARD'),
	('ed461fc4-0436-4880-b340-b075d56a06f4'::UUID, 'Dr. Shaaban supports patients working on eating patterns, weight concerns, and motivation.', 'Coaching visits for eating, weight, and behavior change goals.', '+12155551006', 'SWITCHBOARD'),
	('9dcc6e07-821e-4b64-8975-aee5fcd5ca8b'::UUID, 'Dr. Jones works with veterans and families on short-term coping plans.', 'Coaching for military families, veterans, and related transition stress.', '+12155551007', 'SWITCHBOARD'),
	('a865013e-d50c-46fc-b828-0e5ccdac41b6'::UUID, 'Dr. Behavioral Sleep provides psychiatry support for insomnia and sleep routines.', 'Behavioral sleep appointments for insomnia and circadian rhythm concerns.', '+12155551008', 'SWITCHBOARD'),
	('eb19c43f-c452-407f-92c1-3602695bceb2'::UUID, 'Dr. Attention Deficit evaluates attention concerns and treatment planning needs.', 'Psychiatry visits for attention, focus, and executive functioning concerns.', '+12155551009', 'SWITCHBOARD'),
	('b988f30d-11a6-4818-9c34-ac6f7c429ee1'::UUID, 'Dr. EAP Clinic offers short-term psychiatry support for workplace stress.', 'Brief EAP psychiatry appointments for acute stress and work-related concerns.', '+12155551010', 'SWITCHBOARD'),
	('11e02870-30bc-4178-8614-16caf5fe8996'::UUID, 'Dr. No Intake is used to test direct booking without legacy intake requirements.', 'Direct booking psychiatry appointments without a clinic intake assessment.', '+12155551011', 'SWITCHBOARD'),
	('9d692393-f613-4c6f-8d7c-9272af495f4a'::UUID, 'Dr. Adam Grayson is a military family clinic fixture for one-to-one support visits.', 'One-to-one support appointments for military family stress and transition needs.', '+12155551013', 'SWITCHBOARD')
) AS provider_fixture(provider_id, bio, description, phone_number, videoconference_platform_id)
WHERE provider.provider_id=provider_fixture.provider_id;

INSERT INTO provider_support_role (
	provider_id,
	support_role_id
)
SELECT
	'15f9711d-38e1-44a1-a933-f1522ddd2c81'::UUID,
	'CLINICIAN'
WHERE EXISTS (
	SELECT 1
	FROM provider
	WHERE provider_id='15f9711d-38e1-44a1-a933-f1522ddd2c81'::UUID
)
AND NOT EXISTS (
	SELECT 1
	FROM provider_support_role
	WHERE provider_id='15f9711d-38e1-44a1-a933-f1522ddd2c81'::UUID
	AND support_role_id='CLINICIAN'
);

UPDATE clinic
SET description=clinic_fixture.description,
	treatment_description=clinic_fixture.treatment_description,
	phone_number=clinic_fixture.phone_number,
	image_url=clinic_fixture.image_url,
	appointment_booking_level_id=clinic_fixture.appointment_booking_level_id
FROM (VALUES
	('d789dbdb-6756-4293-836d-91b7329fb49c'::UUID, 'Fixture Behavioral Wellness Clinic', 'Clinic fixture profile for reproductive mental health therapy and consult routing.', '+12155552001', 'https://www.fillmurray.com/640/360', 'PROVIDER'),
	('b6c5e9a3-6018-473d-86d4-2861a328e537'::UUID, 'Fixture Stress and Anxiety Clinic', 'Clinic fixture profile for anxiety stabilization and stress planning.', '+12155552002', 'https://www.fillmurray.com/641/360', 'PROVIDER'),
	('7872559f-b5f6-449f-892d-3f312cd691ff'::UUID, 'Fixture Cognitive Therapy Clinic', 'Clinic fixture profile for structured cognitive therapy appointment testing.', '+12155552003', 'https://www.fillmurray.com/642/360', 'PROVIDER'),
	('ab629384-400a-4688-8465-04636ec2eaa2'::UUID, 'Fixture Adult Autism Services Clinic', 'Clinic fixture profile for autism services booking through clinic-level routing.', '+12155552004', 'https://www.fillmurray.com/643/360', 'CLINIC'),
	('03283875-eb33-42ff-8d14-2acb4a67b300'::UUID, 'Fixture Eating and Weight Clinic', 'Clinic fixture profile for eating pattern and weight concern coaching.', '+12155552005', 'https://www.fillmurray.com/644/360', 'PROVIDER'),
	('3eeb5b48-4c9c-4601-a091-09af03abe3ef'::UUID, 'Fixture Substance Use Support Clinic', 'Clinic fixture profile for substance use consultation and recovery coaching.', '+12155552006', 'https://www.fillmurray.com/645/360', 'PROVIDER'),
	('25fd7117-3013-4462-b7b4-63a9bf808f10'::UUID, 'Fixture Military Family Clinic', 'Clinic fixture profile for veteran and military family coaching workflows.', '+12155552007', 'https://www.fillmurray.com/646/360', 'PROVIDER'),
	('b1f16a29-66ed-484f-a4ed-110fd8bdded5'::UUID, 'Fixture Behavioral Sleep Clinic', 'Clinic fixture profile for insomnia and behavioral sleep appointment searches.', '+12155552008', 'https://www.fillmurray.com/647/360', 'PROVIDER'),
	('8a385c20-dec8-4535-8c6d-684f0e70bfc0'::UUID, 'Fixture Attention Clinic', 'Clinic fixture profile for attention, focus, and executive function consults.', '+12155552009', 'https://www.fillmurray.com/648/360', 'PROVIDER'),
	('adab724f-2de7-4824-a56f-50fe8554f730'::UUID, 'Fixture EAP Clinic', 'Clinic fixture profile for employee assistance psychiatry booking coverage.', '+12155552010', 'https://www.fillmurray.com/649/360', 'PROVIDER'),
	('af1bb3fc-f5ab-49e2-8276-9727b58e9a93'::UUID, 'Fixture Direct Booking Clinic', 'Clinic fixture profile for direct booking without legacy intake prompts.', '+12155552011', 'https://www.fillmurray.com/650/360', 'PROVIDER')
) AS clinic_fixture(clinic_id, description, treatment_description, phone_number, image_url, appointment_booking_level_id)
WHERE clinic.clinic_id=clinic_fixture.clinic_id;

WITH fixture_feature (
	institution_feature_id,
	feature_id,
	nav_description,
	description,
	treatment_description,
	display_order
) AS (VALUES
	('4ef4d2e5-2491-4e77-bf95-1fef36c10001'::UUID, 'THERAPY', 'Therapy provider-search fixture coverage.', 'Therapy fixture feature for provider search testing.', 'Therapy appointments', 1),
	('4ef4d2e5-2491-4e77-bf95-1fef36c10002'::UUID, 'MEDICATION_PRESCRIBER', 'Medication provider-search fixture coverage.', 'Medication prescriber fixture feature for provider search testing.', 'Medication appointments', 2),
	('4ef4d2e5-2491-4e77-bf95-1fef36c10003'::UUID, 'PSYCHIATRIST', 'Psychiatry provider-search fixture coverage.', 'Psychiatry fixture feature for provider search testing.', 'Psychiatry appointments', 3),
	('4ef4d2e5-2491-4e77-bf95-1fef36c10004'::UUID, 'MENTAL_HEALTH_PROVIDERS', 'Mental health provider-search fixture coverage.', 'Mental health provider fixture feature for provider search testing.', 'Mental health appointments', 4),
	('4ef4d2e5-2491-4e77-bf95-1fef36c10005'::UUID, 'COACHING', 'Wellness coaching provider-search fixture coverage.', 'Wellness coaching fixture feature for provider search testing.', 'Wellness coaching appointments', 5),
	('4ef4d2e5-2491-4e77-bf95-1fef36c10006'::UUID, 'SPIRITUAL_SUPPORT', 'Spiritual support provider-search fixture coverage.', 'Spiritual support fixture feature for provider search testing.', 'Spiritual support appointments', 6)
)
INSERT INTO institution_feature (
	institution_feature_id,
	institution_id,
	feature_id,
	nav_description,
	description,
	display_order,
	nav_visible,
	landing_page_visible,
	treatment_description
)
SELECT
	fixture_feature.institution_feature_id,
	'COBALT',
	fixture_feature.feature_id,
	fixture_feature.nav_description,
	fixture_feature.description,
	fixture_feature.display_order,
	TRUE,
	TRUE,
	fixture_feature.treatment_description
FROM fixture_feature
JOIN feature
	ON feature.feature_id=fixture_feature.feature_id
ON CONFLICT (institution_id, feature_id) DO UPDATE
SET nav_description=EXCLUDED.nav_description,
	description=EXCLUDED.description,
	display_order=EXCLUDED.display_order,
	nav_visible=EXCLUDED.nav_visible,
	landing_page_visible=EXCLUDED.landing_page_visible,
	treatment_description=EXCLUDED.treatment_description;

INSERT INTO feature_support_role (
	feature_support_role_id,
	feature_id,
	support_role_id
)
SELECT
	feature_support_role_fixture.feature_support_role_id,
	feature_support_role_fixture.feature_id,
	feature_support_role_fixture.support_role_id
FROM (VALUES
	('67ab92eb-fb06-4d83-9103-5b97fdb10001'::UUID, 'MEDICATION_PRESCRIBER', 'PSYCHIATRIST'),
	('67ab92eb-fb06-4d83-9103-5b97fdb10002'::UUID, 'MENTAL_HEALTH_PROVIDERS', 'CARE_MANAGER'),
	('67ab92eb-fb06-4d83-9103-5b97fdb10003'::UUID, 'MENTAL_HEALTH_PROVIDERS', 'CHAPLAIN'),
	('67ab92eb-fb06-4d83-9103-5b97fdb10004'::UUID, 'MENTAL_HEALTH_PROVIDERS', 'CLINICIAN'),
	('67ab92eb-fb06-4d83-9103-5b97fdb10005'::UUID, 'MENTAL_HEALTH_PROVIDERS', 'COACH'),
	('67ab92eb-fb06-4d83-9103-5b97fdb10006'::UUID, 'MENTAL_HEALTH_PROVIDERS', 'PSYCHIATRIST')
) AS feature_support_role_fixture(feature_support_role_id, feature_id, support_role_id)
JOIN feature
	ON feature.feature_id=feature_support_role_fixture.feature_id
JOIN support_role
	ON support_role.support_role_id=feature_support_role_fixture.support_role_id
ON CONFLICT (feature_id, support_role_id) DO NOTHING;

UPDATE institution_location
SET short_name=location_fixture.short_name,
	display_order=location_fixture.display_order
FROM (VALUES
	('Cobalt Health System', 'Health System', 1),
	('Cobalt General', 'General', 2)
) AS location_fixture(name, short_name, display_order)
WHERE institution_location.institution_id='COBALT'
AND institution_location.name=location_fixture.name;

INSERT INTO institution_location (
	institution_location_id,
	institution_id,
	name,
	short_name,
	display_order
)
SELECT
	location_fixture.institution_location_id,
	'COBALT',
	location_fixture.name,
	location_fixture.short_name,
	location_fixture.display_order
FROM (VALUES
	('4f11d582-78e8-4559-a1b7-faa53e33f2f1'::UUID, 'Cobalt Downtown', 'Downtown', 3),
	('565e7f20-8f63-4be1-86e6-8c63eedcb277'::UUID, 'Cobalt Virtual Care', 'Virtual Care', 4)
) AS location_fixture(institution_location_id, name, short_name, display_order)
WHERE NOT EXISTS (
	SELECT 1
	FROM institution_location
	WHERE institution_id='COBALT'
	AND name=location_fixture.name
);

UPDATE account
SET institution_location_id=NULL,
	prompted_for_institution_location=FALSE
WHERE account_id='07b6f7c6-1d6d-4886-a6eb-1bcfd0139e53'::UUID
AND institution_id='COBALT';

INSERT INTO provider_institution_location (
	provider_id,
	institution_location_id
)
SELECT
	provider.provider_id,
	institution_location.institution_location_id
FROM (VALUES
	('15f9711d-38e1-44a1-a933-f1522ddd2c81'::UUID, 'Cobalt General'),
	('31633b9d-651b-402b-9314-7def6af811b6'::UUID, 'Cobalt Health System'),
	('360d46c4-2ee9-4031-aab6-aa6a16f398d7'::UUID, 'Cobalt Health System'),
	('dc7aeafd-0fc8-4c4d-b09a-09d4dc3079c1'::UUID, 'Cobalt General'),
	('2d6b7032-0145-4273-84f5-94e7238bc331'::UUID, 'Cobalt General'),
	('ed461fc4-0436-4880-b340-b075d56a06f4'::UUID, 'Cobalt Health System'),
	('9dcc6e07-821e-4b64-8975-aee5fcd5ca8b'::UUID, 'Cobalt General'),
	('a865013e-d50c-46fc-b828-0e5ccdac41b6'::UUID, 'Cobalt General'),
	('eb19c43f-c452-407f-92c1-3602695bceb2'::UUID, 'Cobalt Health System'),
	('b988f30d-11a6-4818-9c34-ac6f7c429ee1'::UUID, 'Cobalt Health System'),
	('11e02870-30bc-4178-8614-16caf5fe8996'::UUID, 'Cobalt General'),
	('f56acf8f-3d10-431d-8f65-4c379658bfcc'::UUID, 'Cobalt Health System'),
	('f56acf8f-3d10-431d-8f65-4c379658bfcc'::UUID, 'Cobalt Virtual Care'),
	('15f9711d-38e1-44a1-a933-f1522ddd2c81'::UUID, 'Cobalt Virtual Care'),
	('360d46c4-2ee9-4031-aab6-aa6a16f398d7'::UUID, 'Cobalt Downtown'),
	('dc7aeafd-0fc8-4c4d-b09a-09d4dc3079c1'::UUID, 'Cobalt Downtown'),
	('2d6b7032-0145-4273-84f5-94e7238bc331'::UUID, 'Cobalt Downtown'),
	('31633b9d-651b-402b-9314-7def6af811b6'::UUID, 'Cobalt Downtown'),
	('9d692393-f613-4c6f-8d7c-9272af495f4a'::UUID, 'Cobalt Downtown'),
	('9dcc6e07-821e-4b64-8975-aee5fcd5ca8b'::UUID, 'Cobalt Virtual Care'),
	('ed461fc4-0436-4880-b340-b075d56a06f4'::UUID, 'Cobalt Virtual Care'),
	('eb19c43f-c452-407f-92c1-3602695bceb2'::UUID, 'Cobalt Virtual Care'),
	('11e02870-30bc-4178-8614-16caf5fe8996'::UUID, 'Cobalt Virtual Care')
) AS provider_location(provider_id, institution_location_name)
JOIN provider
	ON provider.provider_id=provider_location.provider_id
JOIN institution_location
	ON institution_location.institution_id=provider.institution_id
	AND institution_location.name=provider_location.institution_location_name
WHERE NOT EXISTS (
	SELECT 1
	FROM provider_institution_location existing_provider_location
	WHERE existing_provider_location.provider_id=provider.provider_id
	AND existing_provider_location.institution_location_id=institution_location.institution_location_id
);

WITH fixture_availability (
	logical_availability_id,
	provider_id,
	start_date_time,
	end_date_time
) AS (VALUES
	('409b6b18-78b4-4a0b-bb03-6c77ff100001'::UUID, '15f9711d-38e1-44a1-a933-f1522ddd2c81'::UUID, TIMESTAMP '2026-01-05 09:00:00', TIMESTAMP '2099-12-31 17:00:00'),
	('409b6b18-78b4-4a0b-bb03-6c77ff100002'::UUID, '31633b9d-651b-402b-9314-7def6af811b6'::UUID, TIMESTAMP '2026-01-05 09:00:00', TIMESTAMP '2099-12-31 17:00:00'),
	('409b6b18-78b4-4a0b-bb03-6c77ff100003'::UUID, '360d46c4-2ee9-4031-aab6-aa6a16f398d7'::UUID, TIMESTAMP '2026-01-05 09:00:00', TIMESTAMP '2099-12-31 17:00:00'),
	('409b6b18-78b4-4a0b-bb03-6c77ff100004'::UUID, 'dc7aeafd-0fc8-4c4d-b09a-09d4dc3079c1'::UUID, TIMESTAMP '2026-01-05 09:00:00', TIMESTAMP '2099-12-31 17:00:00'),
	('409b6b18-78b4-4a0b-bb03-6c77ff100005'::UUID, '2d6b7032-0145-4273-84f5-94e7238bc331'::UUID, TIMESTAMP '2026-01-05 09:00:00', TIMESTAMP '2099-12-31 17:00:00'),
	('409b6b18-78b4-4a0b-bb03-6c77ff100006'::UUID, 'ed461fc4-0436-4880-b340-b075d56a06f4'::UUID, TIMESTAMP '2026-01-05 09:00:00', TIMESTAMP '2099-12-31 17:00:00'),
	('409b6b18-78b4-4a0b-bb03-6c77ff100007'::UUID, '9dcc6e07-821e-4b64-8975-aee5fcd5ca8b'::UUID, TIMESTAMP '2026-01-05 09:00:00', TIMESTAMP '2099-12-31 17:00:00'),
	('409b6b18-78b4-4a0b-bb03-6c77ff100008'::UUID, 'a865013e-d50c-46fc-b828-0e5ccdac41b6'::UUID, TIMESTAMP '2026-01-05 09:00:00', TIMESTAMP '2099-12-31 17:00:00'),
	('409b6b18-78b4-4a0b-bb03-6c77ff100009'::UUID, 'eb19c43f-c452-407f-92c1-3602695bceb2'::UUID, TIMESTAMP '2026-01-05 09:00:00', TIMESTAMP '2099-12-31 17:00:00'),
	('409b6b18-78b4-4a0b-bb03-6c77ff100010'::UUID, 'b988f30d-11a6-4818-9c34-ac6f7c429ee1'::UUID, TIMESTAMP '2026-01-05 09:00:00', TIMESTAMP '2099-12-31 17:00:00'),
	('409b6b18-78b4-4a0b-bb03-6c77ff100011'::UUID, '11e02870-30bc-4178-8614-16caf5fe8996'::UUID, TIMESTAMP '2026-01-05 09:00:00', TIMESTAMP '2099-12-31 17:00:00')
)
INSERT INTO logical_availability (
	logical_availability_id,
	provider_id,
	start_date_time,
	end_date_time,
	logical_availability_type_id,
	recurrence_type_id,
	recur_sunday,
	recur_monday,
	recur_tuesday,
	recur_wednesday,
	recur_thursday,
	recur_friday,
	recur_saturday,
	created_by_account_id,
	last_updated_by_account_id
)
SELECT
	fixture_availability.logical_availability_id,
	fixture_availability.provider_id,
	fixture_availability.start_date_time,
	fixture_availability.end_date_time,
	'OPEN',
	'DAILY',
	FALSE,
	TRUE,
	TRUE,
	TRUE,
	TRUE,
	TRUE,
	FALSE,
	account.account_id,
	account.account_id
FROM fixture_availability
JOIN provider
	ON provider.provider_id=fixture_availability.provider_id
JOIN account
	ON account.account_id='f3d6c7b8-ac74-4679-b788-502a27804474'::UUID
ON CONFLICT (logical_availability_id) DO UPDATE
SET provider_id=EXCLUDED.provider_id,
	start_date_time=EXCLUDED.start_date_time,
	end_date_time=EXCLUDED.end_date_time,
	logical_availability_type_id=EXCLUDED.logical_availability_type_id,
	recurrence_type_id=EXCLUDED.recurrence_type_id,
	recur_sunday=EXCLUDED.recur_sunday,
	recur_monday=EXCLUDED.recur_monday,
	recur_tuesday=EXCLUDED.recur_tuesday,
	recur_wednesday=EXCLUDED.recur_wednesday,
	recur_thursday=EXCLUDED.recur_thursday,
	recur_friday=EXCLUDED.recur_friday,
	recur_saturday=EXCLUDED.recur_saturday,
	last_updated_by_account_id=EXCLUDED.last_updated_by_account_id;

WITH fixture_availability (
	logical_availability_id,
	provider_id
) AS (VALUES
	('409b6b18-78b4-4a0b-bb03-6c77ff100001'::UUID, '15f9711d-38e1-44a1-a933-f1522ddd2c81'::UUID),
	('409b6b18-78b4-4a0b-bb03-6c77ff100002'::UUID, '31633b9d-651b-402b-9314-7def6af811b6'::UUID),
	('409b6b18-78b4-4a0b-bb03-6c77ff100003'::UUID, '360d46c4-2ee9-4031-aab6-aa6a16f398d7'::UUID),
	('409b6b18-78b4-4a0b-bb03-6c77ff100004'::UUID, 'dc7aeafd-0fc8-4c4d-b09a-09d4dc3079c1'::UUID),
	('409b6b18-78b4-4a0b-bb03-6c77ff100005'::UUID, '2d6b7032-0145-4273-84f5-94e7238bc331'::UUID),
	('409b6b18-78b4-4a0b-bb03-6c77ff100006'::UUID, 'ed461fc4-0436-4880-b340-b075d56a06f4'::UUID),
	('409b6b18-78b4-4a0b-bb03-6c77ff100007'::UUID, '9dcc6e07-821e-4b64-8975-aee5fcd5ca8b'::UUID),
	('409b6b18-78b4-4a0b-bb03-6c77ff100008'::UUID, 'a865013e-d50c-46fc-b828-0e5ccdac41b6'::UUID),
	('409b6b18-78b4-4a0b-bb03-6c77ff100009'::UUID, 'eb19c43f-c452-407f-92c1-3602695bceb2'::UUID),
	('409b6b18-78b4-4a0b-bb03-6c77ff100010'::UUID, 'b988f30d-11a6-4818-9c34-ac6f7c429ee1'::UUID),
	('409b6b18-78b4-4a0b-bb03-6c77ff100011'::UUID, '11e02870-30bc-4178-8614-16caf5fe8996'::UUID)
)
INSERT INTO logical_availability_appointment_type (
	logical_availability_id,
	appointment_type_id
)
SELECT
	fixture_availability.logical_availability_id,
	provider_appointment_type.appointment_type_id
FROM fixture_availability
JOIN provider_appointment_type
	ON provider_appointment_type.provider_id=fixture_availability.provider_id
JOIN appointment_type
	ON appointment_type.appointment_type_id=provider_appointment_type.appointment_type_id
	AND COALESCE(appointment_type.deleted, FALSE)=FALSE
ON CONFLICT (logical_availability_id, appointment_type_id) DO NOTHING;


-- Enrich the Penn Autism Clinic bootstrap fixture with display data used by
-- provider-search and appointment-booking QA flows.
UPDATE clinic
SET phone_number='+12155552012'
WHERE institution_id='COBALT'
AND description='Penn Autism Clinic';

UPDATE provider
SET bio='John Skokowski coordinates autism clinic intake and consult calls for fixture testing.',
	phone_number='+12155551012',
	description=$provider_description$
		<p class="mb-0">
			John Skokowski is the Penn Autism Clinic patient care manager. He leads the initial intake or consult call,
			gathers the information needed for clinical review, and helps determine the most appropriate next step for care.
		</p>
	$provider_description$
WHERE institution_id='COBALT'
AND name='John Skokowski';


-- Provider search needs concrete, bookable rows for every screening type so QA
-- can run provider search -> screening -> appointment booking confirmation for
-- each screening taxonomy value without relying on legacy assessment fixtures.
DO $$
DECLARE
	v_created_by_account_id UUID;
	v_fixture_clinic_id UUID := UUID_GENERATE_V5('c65b62f1-b132-4d1b-ad43-26eae272a8b7'::UUID, 'provider-search-screening-type-fixture-clinic');
	v_institution_id TEXT := 'COBALT';
	v_screening_type RECORD;
BEGIN
	SELECT account_id
	INTO v_created_by_account_id
	FROM account
	WHERE institution_id=v_institution_id
	ORDER BY
		CASE WHEN role_id IN ('ADMINISTRATOR', 'SUPER_ADMINISTRATOR') THEN 0 ELSE 1 END,
		created,
		account_id
	LIMIT 1;

	IF v_created_by_account_id IS NULL THEN
		RAISE EXCEPTION 'Unable to create provider-search screening type fixtures because institution % has no account to own generated screening records.',
			v_institution_id;
	END IF;

	INSERT INTO clinic (
		clinic_id,
		description,
		treatment_description,
		phone_number,
		institution_id,
		show_intake_assessment_prompt,
		appointment_booking_level_id
	) VALUES (
		v_fixture_clinic_id,
		'Fixture Screening Type Coverage Clinic',
		'Provider-search fixture rows for screening type coverage.',
		'+12155553000',
		v_institution_id,
		FALSE,
		'PROVIDER'
	)
	ON CONFLICT (clinic_id) DO UPDATE
	SET description=EXCLUDED.description,
		treatment_description=EXCLUDED.treatment_description,
		phone_number=EXCLUDED.phone_number,
		institution_id=EXCLUDED.institution_id,
		show_intake_assessment_prompt=EXCLUDED.show_intake_assessment_prompt,
		appointment_booking_level_id=EXCLUDED.appointment_booking_level_id;

	FOR v_screening_type IN
		SELECT
			screening_type_id,
			description,
			ROW_NUMBER() OVER (ORDER BY screening_type_id)::INTEGER AS fixture_order
		FROM screening_type
		ORDER BY screening_type_id
	LOOP
		DECLARE
			v_appointment_type_id UUID := UUID_GENERATE_V5('c65b62f1-b132-4d1b-ad43-26eae272a8b7'::UUID, 'provider-search-screening-type-fixture-appointment-type:' || v_screening_type.screening_type_id);
			v_logical_availability_id UUID := UUID_GENERATE_V5('c65b62f1-b132-4d1b-ad43-26eae272a8b7'::UUID, 'provider-search-screening-type-fixture-logical-availability:' || v_screening_type.screening_type_id);
			v_provider_appointment_type_id UUID := UUID_GENERATE_V5('c65b62f1-b132-4d1b-ad43-26eae272a8b7'::UUID, 'provider-search-screening-type-fixture-provider-appointment-type:' || v_screening_type.screening_type_id);
			v_provider_clinic_id UUID := UUID_GENERATE_V5('c65b62f1-b132-4d1b-ad43-26eae272a8b7'::UUID, 'provider-search-screening-type-fixture-provider-clinic:' || v_screening_type.screening_type_id);
			v_provider_id UUID := UUID_GENERATE_V5('c65b62f1-b132-4d1b-ad43-26eae272a8b7'::UUID, 'provider-search-screening-type-fixture-provider:' || v_screening_type.screening_type_id);
			v_question_id UUID := UUID_GENERATE_V5('c65b62f1-b132-4d1b-ad43-26eae272a8b7'::UUID, 'provider-search-screening-type-fixture-question:' || v_screening_type.screening_type_id);
			v_screening_flow_id UUID := UUID_GENERATE_V5('c65b62f1-b132-4d1b-ad43-26eae272a8b7'::UUID, 'provider-search-screening-type-fixture-flow:' || v_screening_type.screening_type_id);
			v_screening_flow_version_id UUID := UUID_GENERATE_V5('c65b62f1-b132-4d1b-ad43-26eae272a8b7'::UUID, 'provider-search-screening-type-fixture-flow-version:' || v_screening_type.screening_type_id);
			v_screening_id UUID := UUID_GENERATE_V5('c65b62f1-b132-4d1b-ad43-26eae272a8b7'::UUID, 'provider-search-screening-type-fixture-screening:' || v_screening_type.screening_type_id);
			v_screening_version_id UUID := UUID_GENERATE_V5('c65b62f1-b132-4d1b-ad43-26eae272a8b7'::UUID, 'provider-search-screening-type-fixture-screening-version:' || v_screening_type.screening_type_id);
			v_yes_option_id UUID := UUID_GENERATE_V5('c65b62f1-b132-4d1b-ad43-26eae272a8b7'::UUID, 'provider-search-screening-type-fixture-answer-yes:' || v_screening_type.screening_type_id);
			v_no_option_id UUID := UUID_GENERATE_V5('c65b62f1-b132-4d1b-ad43-26eae272a8b7'::UUID, 'provider-search-screening-type-fixture-answer-no:' || v_screening_type.screening_type_id);
			v_name_suffix TEXT := REPLACE(LOWER(v_screening_type.screening_type_id), '_', '-');
		BEGIN
			INSERT INTO provider (
				provider_id,
				institution_id,
				name,
				title,
				email_address,
				locale,
				time_zone,
				entity,
				clinic,
				specialty,
				active,
				scheduling_system_id,
				system_affinity_id,
				url_name,
				videoconference_platform_id,
				phone_number,
				display_phone_number_only_for_booking,
				description
			) VALUES (
				v_provider_id,
				v_institution_id,
				FORMAT('Screening Fixture %s', v_screening_type.screening_type_id),
				'Screening Fixture',
				FORMAT('provider-search-screening-%s@example.com', v_name_suffix),
				'en-US',
				'America/New_York',
				'Fixture Screening Type Coverage Clinic',
				'Fixture Screening Type Coverage Clinic',
				v_screening_type.description,
				TRUE,
				'COBALT',
				'COBALT',
				FORMAT('provider-search-screening-%s', v_name_suffix),
				'SWITCHBOARD',
				'+12155553000',
				FALSE,
				FORMAT('Provider-search fixture for %s screening coverage.', v_screening_type.screening_type_id)
			)
			ON CONFLICT (provider_id) DO UPDATE
			SET institution_id=EXCLUDED.institution_id,
				name=EXCLUDED.name,
				title=EXCLUDED.title,
				email_address=EXCLUDED.email_address,
				locale=EXCLUDED.locale,
				time_zone=EXCLUDED.time_zone,
				entity=EXCLUDED.entity,
				clinic=EXCLUDED.clinic,
				specialty=EXCLUDED.specialty,
				active=EXCLUDED.active,
				scheduling_system_id=EXCLUDED.scheduling_system_id,
				system_affinity_id=EXCLUDED.system_affinity_id,
				url_name=EXCLUDED.url_name,
				videoconference_platform_id=EXCLUDED.videoconference_platform_id,
				phone_number=EXCLUDED.phone_number,
				display_phone_number_only_for_booking=EXCLUDED.display_phone_number_only_for_booking,
				description=EXCLUDED.description;

			INSERT INTO provider_support_role (
				provider_id,
				support_role_id
			) VALUES (
				v_provider_id,
				'COACH'
			)
			ON CONFLICT (provider_id, support_role_id) DO NOTHING;

			INSERT INTO provider_clinic (
				provider_clinic_id,
				provider_id,
				clinic_id,
				primary_clinic
			) VALUES (
				v_provider_clinic_id,
				v_provider_id,
				v_fixture_clinic_id,
				TRUE
			)
			ON CONFLICT (provider_clinic_id) DO UPDATE
			SET provider_id=EXCLUDED.provider_id,
				clinic_id=EXCLUDED.clinic_id,
				primary_clinic=EXCLUDED.primary_clinic;

			INSERT INTO screening (
				screening_id,
				name,
				active_screening_version_id,
				created_by_account_id
			) VALUES (
				v_screening_id,
				FORMAT('Provider Search Screening Type Fixture: %s', v_screening_type.screening_type_id),
				NULL,
				v_created_by_account_id
			)
			ON CONFLICT (screening_id) DO UPDATE
			SET name=EXCLUDED.name,
				created_by_account_id=EXCLUDED.created_by_account_id;

			INSERT INTO screening_version (
				screening_version_id,
				screening_id,
				screening_type_id,
				created_by_account_id,
				version_number,
				scoring_function
			) VALUES (
				v_screening_version_id,
				v_screening_id,
				v_screening_type.screening_type_id,
				v_created_by_account_id,
				1,
				$scoring$
const questionsWithAnswerOptions = input.screeningQuestionsWithAnswerOptions || [];
const question = questionsWithAnswerOptions.length > 0 ? questionsWithAnswerOptions[0].screeningQuestion : null;
const answeredQuestionIds = new Set((input.answeredScreeningQuestionIds || []).map(String));

output.completed = question ? answeredQuestionIds.has(String(question.screeningQuestionId)) : true;
output.score = { overallScore: 0 };
output.belowScoringThreshold = false;

if (!output.completed && question) {
  output.nextScreeningQuestionId = question.screeningQuestionId;
}
$scoring$
			)
			ON CONFLICT (screening_version_id) DO UPDATE
			SET screening_id=EXCLUDED.screening_id,
				screening_type_id=EXCLUDED.screening_type_id,
				created_by_account_id=EXCLUDED.created_by_account_id,
				version_number=EXCLUDED.version_number,
				scoring_function=EXCLUDED.scoring_function;

			UPDATE screening
			SET active_screening_version_id=v_screening_version_id
			WHERE screening_id=v_screening_id;

			INSERT INTO screening_institution (
				screening_id,
				institution_id
			) VALUES (
				v_screening_id,
				v_institution_id
			)
			ON CONFLICT (screening_id, institution_id) DO NOTHING;

			INSERT INTO screening_question (
				screening_question_id,
				screening_version_id,
				screening_answer_format_id,
				screening_answer_content_hint_id,
				intro_text,
				question_text,
				minimum_answer_count,
				maximum_answer_count,
				display_order,
				metadata,
				prefer_autosubmit,
				screening_question_submission_style_id
			) VALUES (
				v_question_id,
				v_screening_version_id,
				'SINGLE_SELECT',
				'NONE',
				NULL,
				FORMAT('Fixture question for %s booking screening coverage.', v_screening_type.screening_type_id),
				1,
				1,
				1,
				JSONB_BUILD_OBJECT('providerSearchScreeningTypeFixture', TRUE, 'screeningTypeId', v_screening_type.screening_type_id),
				TRUE,
				'NEXT'
			)
			ON CONFLICT (screening_question_id) DO UPDATE
			SET screening_version_id=EXCLUDED.screening_version_id,
				screening_answer_format_id=EXCLUDED.screening_answer_format_id,
				screening_answer_content_hint_id=EXCLUDED.screening_answer_content_hint_id,
				intro_text=EXCLUDED.intro_text,
				question_text=EXCLUDED.question_text,
				minimum_answer_count=EXCLUDED.minimum_answer_count,
				maximum_answer_count=EXCLUDED.maximum_answer_count,
				display_order=EXCLUDED.display_order,
				metadata=EXCLUDED.metadata,
				prefer_autosubmit=EXCLUDED.prefer_autosubmit,
				screening_question_submission_style_id=EXCLUDED.screening_question_submission_style_id;

			INSERT INTO screening_answer_option (
				screening_answer_option_id,
				screening_question_id,
				answer_option_text,
				score,
				indicates_crisis,
				display_order,
				metadata
			) VALUES
				(
					v_yes_option_id,
					v_question_id,
					'Continue',
					0,
					FALSE,
					1,
					JSONB_BUILD_OBJECT('providerSearchScreeningTypeFixture', TRUE)
				),
				(
					v_no_option_id,
					v_question_id,
					'Continue',
					0,
					FALSE,
					2,
					JSONB_BUILD_OBJECT('providerSearchScreeningTypeFixture', TRUE)
				)
			ON CONFLICT (screening_answer_option_id) DO UPDATE
			SET screening_question_id=EXCLUDED.screening_question_id,
				answer_option_text=EXCLUDED.answer_option_text,
				score=EXCLUDED.score,
				indicates_crisis=EXCLUDED.indicates_crisis,
				display_order=EXCLUDED.display_order,
				metadata=EXCLUDED.metadata;

			INSERT INTO screening_flow (
				screening_flow_id,
				institution_id,
				active_screening_flow_version_id,
				screening_flow_type_id,
				created_by_account_id,
				name
			) VALUES (
				v_screening_flow_id,
				v_institution_id,
				NULL,
				'PROVIDER_INTAKE',
				v_created_by_account_id,
				FORMAT('Provider Search Screening Type Fixture: %s', v_screening_type.screening_type_id)
			)
			ON CONFLICT (screening_flow_id) DO UPDATE
			SET institution_id=EXCLUDED.institution_id,
				screening_flow_type_id=EXCLUDED.screening_flow_type_id,
				created_by_account_id=EXCLUDED.created_by_account_id,
				name=EXCLUDED.name;

			INSERT INTO screening_flow_version (
				screening_flow_version_id,
				screening_flow_id,
				initial_screening_id,
				phone_number_required,
				version_number,
				orchestration_function,
				results_function,
				destination_function,
				created_by_account_id
			) VALUES (
				v_screening_flow_version_id,
				v_screening_flow_id,
				v_screening_id,
				FALSE,
				1,
				$orchestration$
const screeningSessionScreening = (input.screeningSessionScreenings || [])[0];

output.completed = screeningSessionScreening ? Boolean(screeningSessionScreening.completed) : false;
output.crisisIndicated = false;
$orchestration$,
				$results$
output.supportRoleRecommendations = [];
output.recommendLegacyContentAnswerIds = false;
output.legacyContentAnswerIds = [];
output.recommendedTagIds = [];
output.recommendedFeatureIds = [];
output.integratedCareTriages = [];
$results$,
				$destination$
output.screeningSessionDestinationId = null;
output.context = {};

if (input.screeningSession.completed) {
  output.screeningSessionDestinationId = 'APPOINTMENT_BOOKING_CONFIRMATION';
  output.context.result = 'SUCCESS';
}
$destination$,
				v_created_by_account_id
			)
			ON CONFLICT (screening_flow_version_id) DO UPDATE
			SET screening_flow_id=EXCLUDED.screening_flow_id,
				initial_screening_id=EXCLUDED.initial_screening_id,
				phone_number_required=EXCLUDED.phone_number_required,
				version_number=EXCLUDED.version_number,
				orchestration_function=EXCLUDED.orchestration_function,
				results_function=EXCLUDED.results_function,
				destination_function=EXCLUDED.destination_function,
				created_by_account_id=EXCLUDED.created_by_account_id;

			UPDATE screening_flow
			SET active_screening_flow_version_id=v_screening_flow_version_id
			WHERE screening_flow_id=v_screening_flow_id;

			INSERT INTO appointment_type (
				appointment_type_id,
				acuity_appointment_type_id,
				name,
				description,
				duration_in_minutes,
				deleted,
				scheduling_system_id,
				visit_type_id,
				screening_flow_id
			) VALUES (
				v_appointment_type_id,
				NULL,
				FORMAT('Screening Fixture %s Appointment', v_screening_type.screening_type_id),
				FORMAT('%s provider-search booking fixture.', v_screening_type.description),
				30,
				FALSE,
				'COBALT',
				'INITIAL',
				v_screening_flow_id
			)
			ON CONFLICT (appointment_type_id) DO UPDATE
			SET name=EXCLUDED.name,
				description=EXCLUDED.description,
				duration_in_minutes=EXCLUDED.duration_in_minutes,
				deleted=EXCLUDED.deleted,
				scheduling_system_id=EXCLUDED.scheduling_system_id,
				visit_type_id=EXCLUDED.visit_type_id,
				screening_flow_id=EXCLUDED.screening_flow_id;

			INSERT INTO provider_appointment_type (
				provider_appointment_type_id,
				provider_id,
				display_order,
				appointment_type_id
			) VALUES (
				v_provider_appointment_type_id,
				v_provider_id,
				31000 + v_screening_type.fixture_order,
				v_appointment_type_id
			)
			ON CONFLICT (provider_appointment_type_id) DO UPDATE
			SET provider_id=EXCLUDED.provider_id,
				display_order=EXCLUDED.display_order,
				appointment_type_id=EXCLUDED.appointment_type_id;

			INSERT INTO logical_availability (
				logical_availability_id,
				provider_id,
				start_date_time,
				end_date_time,
				logical_availability_type_id,
				recurrence_type_id,
				recur_sunday,
				recur_monday,
				recur_tuesday,
				recur_wednesday,
				recur_thursday,
				recur_friday,
				recur_saturday,
				created_by_account_id,
				last_updated_by_account_id
			) VALUES (
				v_logical_availability_id,
				v_provider_id,
				TIMESTAMP '2026-01-05 09:00:00',
				TIMESTAMP '2099-12-31 17:00:00',
				'OPEN',
				'DAILY',
				FALSE,
				TRUE,
				TRUE,
				TRUE,
				TRUE,
				TRUE,
				FALSE,
				v_created_by_account_id,
				v_created_by_account_id
			)
			ON CONFLICT (logical_availability_id) DO UPDATE
			SET provider_id=EXCLUDED.provider_id,
				start_date_time=EXCLUDED.start_date_time,
				end_date_time=EXCLUDED.end_date_time,
				logical_availability_type_id=EXCLUDED.logical_availability_type_id,
				recurrence_type_id=EXCLUDED.recurrence_type_id,
				recur_sunday=EXCLUDED.recur_sunday,
				recur_monday=EXCLUDED.recur_monday,
				recur_tuesday=EXCLUDED.recur_tuesday,
				recur_wednesday=EXCLUDED.recur_wednesday,
				recur_thursday=EXCLUDED.recur_thursday,
				recur_friday=EXCLUDED.recur_friday,
				recur_saturday=EXCLUDED.recur_saturday,
				last_updated_by_account_id=EXCLUDED.last_updated_by_account_id;

			INSERT INTO logical_availability_appointment_type (
				logical_availability_id,
				appointment_type_id
			) VALUES (
				v_logical_availability_id,
				v_appointment_type_id
			)
			ON CONFLICT (logical_availability_id, appointment_type_id) DO NOTHING;
		END;
	END LOOP;
END $$;


-- Populate direct provider/clinic website and location response fixture data
-- with distinct values. These rows are local-only and deterministic so QA and
-- developer DB recreates can assert response parity without hand setup.
WITH fixture_provider AS (
	SELECT
		provider.provider_id,
		provider.name,
		COALESCE(NULLIF(provider.url_name, ''), provider.provider_id::TEXT) AS url_name
	FROM provider
	WHERE provider.institution_id='COBALT'
	AND (
		provider.provider_id IN (
			'15f9711d-38e1-44a1-a933-f1522ddd2c81'::UUID,
			'31633b9d-651b-402b-9314-7def6af811b6'::UUID,
			'360d46c4-2ee9-4031-aab6-aa6a16f398d7'::UUID,
			'dc7aeafd-0fc8-4c4d-b09a-09d4dc3079c1'::UUID,
			'2d6b7032-0145-4273-84f5-94e7238bc331'::UUID,
			'ed461fc4-0436-4880-b340-b075d56a06f4'::UUID,
			'9dcc6e07-821e-4b64-8975-aee5fcd5ca8b'::UUID,
			'a865013e-d50c-46fc-b828-0e5ccdac41b6'::UUID,
			'eb19c43f-c452-407f-92c1-3602695bceb2'::UUID,
			'b988f30d-11a6-4818-9c34-ac6f7c429ee1'::UUID,
			'11e02870-30bc-4178-8614-16caf5fe8996'::UUID,
			'9d692393-f613-4c6f-8d7c-9272af495f4a'::UUID
		)
		OR provider.name='John Skokowski'
		OR provider.email_address LIKE 'provider-search-screening-%@example.com'
	)
)
UPDATE provider
SET bio_url=FORMAT('https://fixtures.cobalt.care/providers/%s', fixture_provider.url_name)
FROM fixture_provider
WHERE provider.provider_id=fixture_provider.provider_id;

WITH fixture_clinic AS (
	SELECT
		clinic.clinic_id,
		clinic.description
	FROM clinic
	WHERE clinic.institution_id='COBALT'
	AND (
		clinic.clinic_id IN (
			'd789dbdb-6756-4293-836d-91b7329fb49c'::UUID,
			'b6c5e9a3-6018-473d-86d4-2861a328e537'::UUID,
			'7872559f-b5f6-449f-892d-3f312cd691ff'::UUID,
			'ab629384-400a-4688-8465-04636ec2eaa2'::UUID,
			'03283875-eb33-42ff-8d14-2acb4a67b300'::UUID,
			'3eeb5b48-4c9c-4601-a091-09af03abe3ef'::UUID,
			'25fd7117-3013-4462-b7b4-63a9bf808f10'::UUID,
			'b1f16a29-66ed-484f-a4ed-110fd8bdded5'::UUID,
			'8a385c20-dec8-4535-8c6d-684f0e70bfc0'::UUID,
			'adab724f-2de7-4824-a56f-50fe8554f730'::UUID,
			'af1bb3fc-f5ab-49e2-8276-9727b58e9a93'::UUID
		)
		OR clinic.description IN ('Penn Autism Clinic', 'Fixture Screening Type Coverage Clinic')
	)
)
UPDATE clinic
SET website_url=FORMAT('https://fixtures.cobalt.care/clinics/%s', fixture_clinic.clinic_id)
FROM fixture_clinic
WHERE clinic.clinic_id=fixture_clinic.clinic_id;

WITH fixture_provider AS (
	SELECT
		provider.provider_id,
		provider.name,
		COALESCE(NULLIF(provider.url_name, ''), provider.provider_id::TEXT) AS url_name,
		ROW_NUMBER() OVER (ORDER BY provider.name, provider.provider_id) AS fixture_order
	FROM provider
	WHERE provider.institution_id='COBALT'
	AND (
		provider.provider_id IN (
			'15f9711d-38e1-44a1-a933-f1522ddd2c81'::UUID,
			'31633b9d-651b-402b-9314-7def6af811b6'::UUID,
			'360d46c4-2ee9-4031-aab6-aa6a16f398d7'::UUID,
			'dc7aeafd-0fc8-4c4d-b09a-09d4dc3079c1'::UUID,
			'2d6b7032-0145-4273-84f5-94e7238bc331'::UUID,
			'ed461fc4-0436-4880-b340-b075d56a06f4'::UUID,
			'9dcc6e07-821e-4b64-8975-aee5fcd5ca8b'::UUID,
			'a865013e-d50c-46fc-b828-0e5ccdac41b6'::UUID,
			'eb19c43f-c452-407f-92c1-3602695bceb2'::UUID,
			'b988f30d-11a6-4818-9c34-ac6f7c429ee1'::UUID,
			'11e02870-30bc-4178-8614-16caf5fe8996'::UUID,
			'9d692393-f613-4c6f-8d7c-9272af495f4a'::UUID
		)
		OR provider.name='John Skokowski'
		OR provider.email_address LIKE 'provider-search-screening-%@example.com'
	)
),
fixture_provider_location AS (
	SELECT
		UUID_GENERATE_V5('c65b62f1-b132-4d1b-ad43-26eae272a8b7'::UUID, FORMAT('provider-location-address:%s', provider_id)) AS address_id,
		FORMAT('%s Provider Location', name) AS postal_name,
		FORMAT('%s Provider Plaza', 1000 + fixture_order) AS street_address_1,
		'Philadelphia' AS locality,
		'PA' AS region,
		FORMAT('191%s', LPAD(((fixture_order - 1) % 100)::TEXT, 2, '0')) AS postal_code,
		FORMAT('%s Provider Plaza, Philadelphia, PA %s', 1000 + fixture_order, FORMAT('191%s', LPAD(((fixture_order - 1) % 100)::TEXT, 2, '0'))) AS formatted_address
	FROM fixture_provider
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

WITH fixture_provider AS (
	SELECT
		provider.provider_id,
		provider.name,
		COALESCE(NULLIF(provider.url_name, ''), provider.provider_id::TEXT) AS url_name,
		ROW_NUMBER() OVER (ORDER BY provider.name, provider.provider_id) AS fixture_order
	FROM provider
	WHERE provider.institution_id='COBALT'
	AND (
		provider.provider_id IN (
			'15f9711d-38e1-44a1-a933-f1522ddd2c81'::UUID,
			'31633b9d-651b-402b-9314-7def6af811b6'::UUID,
			'360d46c4-2ee9-4031-aab6-aa6a16f398d7'::UUID,
			'dc7aeafd-0fc8-4c4d-b09a-09d4dc3079c1'::UUID,
			'2d6b7032-0145-4273-84f5-94e7238bc331'::UUID,
			'ed461fc4-0436-4880-b340-b075d56a06f4'::UUID,
			'9dcc6e07-821e-4b64-8975-aee5fcd5ca8b'::UUID,
			'a865013e-d50c-46fc-b828-0e5ccdac41b6'::UUID,
			'eb19c43f-c452-407f-92c1-3602695bceb2'::UUID,
			'b988f30d-11a6-4818-9c34-ac6f7c429ee1'::UUID,
			'11e02870-30bc-4178-8614-16caf5fe8996'::UUID,
			'9d692393-f613-4c6f-8d7c-9272af495f4a'::UUID
		)
		OR provider.name='John Skokowski'
		OR provider.email_address LIKE 'provider-search-screening-%@example.com'
	)
)
INSERT INTO provider_location (
	provider_location_id,
	provider_id,
	address_id,
	name,
	phone_number,
	website_url,
	email_address,
	display_order
)
SELECT
	UUID_GENERATE_V5('c65b62f1-b132-4d1b-ad43-26eae272a8b7'::UUID, FORMAT('provider-location:%s', provider_id)),
	provider_id,
	UUID_GENERATE_V5('c65b62f1-b132-4d1b-ad43-26eae272a8b7'::UUID, FORMAT('provider-location-address:%s', provider_id)),
	FORMAT('%s Office', name),
	FORMAT('+12155554%s', LPAD(fixture_order::TEXT, 3, '0')),
	FORMAT('https://fixtures.cobalt.care/provider-locations/%s', url_name),
	FORMAT('provider-location-%s@example.com', LOWER(REGEXP_REPLACE(url_name, '[^a-zA-Z0-9]+', '-', 'g'))),
	fixture_order
FROM fixture_provider
ON CONFLICT (provider_location_id) DO UPDATE
SET provider_id=EXCLUDED.provider_id,
	address_id=EXCLUDED.address_id,
	name=EXCLUDED.name,
	phone_number=EXCLUDED.phone_number,
	website_url=EXCLUDED.website_url,
	email_address=EXCLUDED.email_address,
	display_order=EXCLUDED.display_order;

WITH fixture_clinic AS (
	SELECT
		clinic.clinic_id,
		clinic.description,
		ROW_NUMBER() OVER (ORDER BY clinic.description, clinic.clinic_id) AS fixture_order
	FROM clinic
	WHERE clinic.institution_id='COBALT'
	AND (
		clinic.clinic_id IN (
			'd789dbdb-6756-4293-836d-91b7329fb49c'::UUID,
			'b6c5e9a3-6018-473d-86d4-2861a328e537'::UUID,
			'7872559f-b5f6-449f-892d-3f312cd691ff'::UUID,
			'ab629384-400a-4688-8465-04636ec2eaa2'::UUID,
			'03283875-eb33-42ff-8d14-2acb4a67b300'::UUID,
			'3eeb5b48-4c9c-4601-a091-09af03abe3ef'::UUID,
			'25fd7117-3013-4462-b7b4-63a9bf808f10'::UUID,
			'b1f16a29-66ed-484f-a4ed-110fd8bdded5'::UUID,
			'8a385c20-dec8-4535-8c6d-684f0e70bfc0'::UUID,
			'adab724f-2de7-4824-a56f-50fe8554f730'::UUID,
			'af1bb3fc-f5ab-49e2-8276-9727b58e9a93'::UUID
		)
		OR clinic.description IN ('Penn Autism Clinic', 'Fixture Screening Type Coverage Clinic')
	)
),
fixture_clinic_location AS (
	SELECT
		UUID_GENERATE_V5('c65b62f1-b132-4d1b-ad43-26eae272a8b7'::UUID, FORMAT('clinic-location-address:%s', clinic_id)) AS address_id,
		FORMAT('%s Clinic Location', description) AS postal_name,
		FORMAT('%s Clinic Way', 2000 + fixture_order) AS street_address_1,
		'Philadelphia' AS locality,
		'PA' AS region,
		FORMAT('192%s', LPAD(((fixture_order - 1) % 100)::TEXT, 2, '0')) AS postal_code,
		FORMAT('%s Clinic Way, Philadelphia, PA %s', 2000 + fixture_order, FORMAT('192%s', LPAD(((fixture_order - 1) % 100)::TEXT, 2, '0'))) AS formatted_address
	FROM fixture_clinic
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

WITH fixture_clinic AS (
	SELECT
		clinic.clinic_id,
		clinic.description,
		ROW_NUMBER() OVER (ORDER BY clinic.description, clinic.clinic_id) AS fixture_order
	FROM clinic
	WHERE clinic.institution_id='COBALT'
	AND (
		clinic.clinic_id IN (
			'd789dbdb-6756-4293-836d-91b7329fb49c'::UUID,
			'b6c5e9a3-6018-473d-86d4-2861a328e537'::UUID,
			'7872559f-b5f6-449f-892d-3f312cd691ff'::UUID,
			'ab629384-400a-4688-8465-04636ec2eaa2'::UUID,
			'03283875-eb33-42ff-8d14-2acb4a67b300'::UUID,
			'3eeb5b48-4c9c-4601-a091-09af03abe3ef'::UUID,
			'25fd7117-3013-4462-b7b4-63a9bf808f10'::UUID,
			'b1f16a29-66ed-484f-a4ed-110fd8bdded5'::UUID,
			'8a385c20-dec8-4535-8c6d-684f0e70bfc0'::UUID,
			'adab724f-2de7-4824-a56f-50fe8554f730'::UUID,
			'af1bb3fc-f5ab-49e2-8276-9727b58e9a93'::UUID
		)
		OR clinic.description IN ('Penn Autism Clinic', 'Fixture Screening Type Coverage Clinic')
	)
)
INSERT INTO clinic_location (
	clinic_location_id,
	clinic_id,
	address_id,
	name,
	phone_number,
	website_url,
	email_address,
	display_order
)
SELECT
	UUID_GENERATE_V5('c65b62f1-b132-4d1b-ad43-26eae272a8b7'::UUID, FORMAT('clinic-location:%s', clinic_id)),
	clinic_id,
	UUID_GENERATE_V5('c65b62f1-b132-4d1b-ad43-26eae272a8b7'::UUID, FORMAT('clinic-location-address:%s', clinic_id)),
	FORMAT('%s Front Desk', description),
	FORMAT('+12155555%s', LPAD(fixture_order::TEXT, 3, '0')),
	FORMAT('https://fixtures.cobalt.care/clinic-locations/%s', clinic_id),
	FORMAT('clinic-location-%s@example.com', clinic_id),
	fixture_order
FROM fixture_clinic
ON CONFLICT (clinic_location_id) DO UPDATE
SET clinic_id=EXCLUDED.clinic_id,
	address_id=EXCLUDED.address_id,
	name=EXCLUDED.name,
	phone_number=EXCLUDED.phone_number,
	website_url=EXCLUDED.website_url,
	email_address=EXCLUDED.email_address,
	display_order=EXCLUDED.display_order;

COMMIT;
