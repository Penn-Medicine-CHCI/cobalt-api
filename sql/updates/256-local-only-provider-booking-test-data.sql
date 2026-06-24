BEGIN;
SELECT _v.register_patch('256-local-only-provider-booking-test-data', ARRAY['256-provider-booking-screening'], NULL);

-- Local-only provider booking seed data for QA and developer databases.
-- This script depends on bootstrap fixture rows and the consolidated production
-- provider-booking/location schema, and is intentionally run only by
-- sql/recreate-local after initial/bootstrap.sql and 256-provider-booking-screening.
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
	display_phone_number_only_for_booking=FALSE,
	details_html=FORMAT($details_html$
<section class="mb-8">
  <h2 class="mb-4">About</h2>
  <p class="mb-0 fs-large">%s</p>
</section>
<section class="mb-8">
  <h2 class="mb-4">Who can self-schedule a fixture appointment?</h2>
  <p class="mb-2 fs-large"><strong>If you are testing provider search:</strong></p>
  <p class="mb-4 fs-large">Use this local fixture profile to verify provider details, contact information, and scheduling behavior.</p>
  <p class="mb-2 fs-large"><strong>If you are testing booking:</strong></p>
  <p class="mb-0 fs-large">Select an available appointment to confirm the provider-level booking flow renders consistently.</p>
</section>
<section>
  <h2 class="mb-4">Fixture Details</h2>
  <div class="table-responsive">
    <table class="table table-bordered align-middle mb-0">
      <thead class="table-light">
        <tr>
          <th scope="col">Field</th>
          <th scope="col">Fixture Value</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td>Provider</td>
          <td>%s</td>
        </tr>
        <tr>
          <td>Scheduling Platform</td>
          <td>%s</td>
        </tr>
        <tr>
          <td>Phone</td>
          <td>%s</td>
        </tr>
      </tbody>
    </table>
  </div>
</section>
$details_html$, provider_fixture.description, provider.name, provider_fixture.videoconference_platform_id, provider_fixture.phone_number)
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
	appointment_booking_level_id=clinic_fixture.appointment_booking_level_id,
	details_html=FORMAT($details_html$
<section class="mb-8">
  <h2 class="mb-4">About</h2>
  <p class="mb-0 fs-large">%s</p>
</section>
<section class="mb-8">
  <h2 class="mb-4">Who can self-schedule a fixture appointment?</h2>
  <p class="mb-2 fs-large"><strong>If you are testing clinic search:</strong></p>
  <p class="mb-4 fs-large">Use this local fixture clinic to verify clinic details, images, contact information, and provider-search grouping.</p>
  <p class="mb-2 fs-large"><strong>If you are testing booking:</strong></p>
  <p class="mb-0 fs-large">Confirm the appointment flow respects the clinic booking level and available provider appointment types.</p>
</section>
<section>
  <h2 class="mb-4">Fixture Details</h2>
  <div class="table-responsive">
    <table class="table table-bordered align-middle mb-0">
      <thead class="table-light">
        <tr>
          <th scope="col">Field</th>
          <th scope="col">Fixture Value</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td>Clinic</td>
          <td>%s</td>
        </tr>
        <tr>
          <td>Booking Level</td>
          <td>%s</td>
        </tr>
        <tr>
          <td>Phone</td>
          <td>%s</td>
        </tr>
      </tbody>
    </table>
  </div>
</section>
$details_html$, clinic_fixture.treatment_description, clinic_fixture.description, clinic_fixture.appointment_booking_level_id, clinic_fixture.phone_number)
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

UPDATE clinic
SET details_html=$details_html$
<section class="mb-8">
  <h2 class="mb-4">About</h2>
  <p class="mb-0 fs-large">
    The Penn Autism Clinic provides diagnostic assessments and ongoing support for children, teens and adults who have, or may have, autism. Clinicians can perform evaluations, offer treatment recommendations, and coordinate with intervention providers to guide families through diagnosis and long-term care.
  </p>
</section>
<section class="mb-8">
  <h2 class="mb-4">Who can self-schedule a Penn Autism Clinic appointment?</h2>
  <p class="mb-2 fs-large"><strong>If you are seeking services for yourself:</strong></p>
  <p class="mb-4 fs-large">You must be a benefits-eligible employee of UPHS or UPenn.</p>
  <p class="mb-2 fs-large"><strong>If you are seeking services for a dependent:</strong></p>
  <p class="mb-0 fs-large">You must be the legal guardian of the dependent and your dependent must be covered under your UPHS or UPenn health insurance.</p>
</section>
<section>
  <h2 class="mb-4">Accepted Insurances</h2>
  <p class="mb-4 fs-large"><strong>You will need to confirm your insurance before booking.</strong></p>
  <div class="table-responsive">
    <table class="table table-bordered align-middle mb-0">
      <thead class="table-light">
        <tr>
          <th scope="col">Health Insurance</th>
          <th scope="col">Behavioral Health Insurance</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td>Aetna Choice Point-of-Service (POS) II</td>
          <td>Aetna Behavioral Health Network</td>
        </tr>
        <tr>
          <td>Aetna High Deductible Health Plan (HDHP) with Health Savings Account</td>
          <td>Aetna Behavioral Health Network</td>
        </tr>
        <tr>
          <td>Keystone/Amerihealth Health Maintenance Organization (HMO) administered by Independence Blue Cross (IBX)</td>
          <td>IBX Behavioral Health</td>
        </tr>
        <tr>
          <td>PennCare/Personal Choice Preferred Provider Organization (PPO) Plan administered by Independence Blue Cross (IBX)</td>
          <td>Quest Behavioral Health</td>
        </tr>
        <tr>
          <td>PennCare High Deductible Health Plan (HDHP) administered by Independence Blue Cross (IBX)</td>
          <td>Quest Behavioral Health</td>
        </tr>
      </tbody>
    </table>
  </div>
</section>
$details_html$
WHERE institution_id='COBALT'
AND LOWER(TRIM(description))=LOWER(TRIM('Penn Autism Clinic'));

UPDATE provider
SET bio='John Skokowski coordinates autism clinic intake and consult calls for fixture testing.',
	phone_number='+12155551012',
	description=$provider_description$
		<p class="mb-0">
			John Skokowski is the Penn Autism Clinic patient care manager. He leads the initial intake or consult call,
			gathers the information needed for clinical review, and helps determine the most appropriate next step for care.
		</p>
	$provider_description$,
	details_html=$details_html$
<section class="mb-8">
  <h2 class="mb-4">About</h2>
  <p class="mb-0 fs-large">John Skokowski coordinates Penn Autism Clinic intake and consult calls for local fixture testing.</p>
</section>
<section class="mb-8">
  <h2 class="mb-4">Who can self-schedule a Penn Autism Clinic appointment?</h2>
  <p class="mb-2 fs-large"><strong>If you are testing intake scheduling:</strong></p>
  <p class="mb-4 fs-large">Use this fixture provider to verify the clinic-level appointment flow and fullscreen screening handoff.</p>
  <p class="mb-2 fs-large"><strong>If you are testing consult scheduling:</strong></p>
  <p class="mb-0 fs-large">Confirm appointment selection works when multiple Penn Autism Clinic appointment types are available.</p>
</section>
<section>
  <h2 class="mb-4">Fixture Details</h2>
  <div class="table-responsive">
    <table class="table table-bordered align-middle mb-0">
      <thead class="table-light">
        <tr>
          <th scope="col">Field</th>
          <th scope="col">Fixture Value</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td>Provider</td>
          <td>John Skokowski</td>
        </tr>
        <tr>
          <td>Clinic</td>
          <td>Penn Autism Clinic</td>
        </tr>
        <tr>
          <td>Phone</td>
          <td>+12155551012</td>
        </tr>
      </tbody>
    </table>
  </div>
</section>
$details_html$
WHERE institution_id='COBALT'
AND name='John Skokowski';


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
		OR clinic.description='Penn Autism Clinic'
	)
)
UPDATE clinic
SET website_url=FORMAT('https://fixtures.cobalt.care/clinics/%s', fixture_clinic.clinic_id)
FROM fixture_clinic
WHERE clinic.clinic_id=fixture_clinic.clinic_id;

WITH fixture_institution_location AS (
	SELECT
		institution_location.institution_location_id,
		institution_location.name,
		ROW_NUMBER() OVER (ORDER BY institution_location.display_order, institution_location.name, institution_location.institution_location_id) AS fixture_order
	FROM institution_location
	WHERE institution_location.institution_id='COBALT'
	AND institution_location.name IN (
		'Cobalt Health System',
		'Cobalt General',
		'Cobalt Downtown',
		'Cobalt Virtual Care'
	)
),
fixture_institution_location_address AS (
	SELECT
		UUID_GENERATE_V5('c65b62f1-b132-4d1b-ad43-26eae272a8b7'::UUID, FORMAT('institution-location-address:%s', institution_location_id)) AS address_id,
		FORMAT('%s Fixture Location', name) AS postal_name,
		FORMAT('%s Cobalt Location Way', 3000 + fixture_order) AS street_address_1,
		'Philadelphia' AS locality,
		'PA' AS region,
		FORMAT('193%s', LPAD(((fixture_order - 1) % 100)::TEXT, 2, '0')) AS postal_code,
		FORMAT('%s Cobalt Location Way, Philadelphia, PA %s', 3000 + fixture_order, FORMAT('193%s', LPAD(((fixture_order - 1) % 100)::TEXT, 2, '0'))) AS formatted_address
	FROM fixture_institution_location
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
FROM fixture_institution_location_address
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
		institution_location.institution_location_id,
		institution_location.name,
		ROW_NUMBER() OVER (ORDER BY institution_location.display_order, institution_location.name, institution_location.institution_location_id) AS fixture_order
	FROM institution_location
	WHERE institution_location.institution_id='COBALT'
	AND institution_location.name IN (
		'Cobalt Health System',
		'Cobalt General',
		'Cobalt Downtown',
		'Cobalt Virtual Care'
	)
)
UPDATE institution_location
SET address_id=UUID_GENERATE_V5('c65b62f1-b132-4d1b-ad43-26eae272a8b7'::UUID, FORMAT('institution-location-address:%s', fixture_institution_location.institution_location_id)),
	phone_number=FORMAT('+12155553%s', LPAD(fixture_institution_location.fixture_order::TEXT, 3, '0')),
	website_url=FORMAT('https://fixtures.cobalt.care/institution-locations/%s', LOWER(REGEXP_REPLACE(fixture_institution_location.name, '[^a-zA-Z0-9]+', '-', 'g'))),
	email_address=FORMAT('institution-location-%s@example.com', LOWER(REGEXP_REPLACE(fixture_institution_location.name, '[^a-zA-Z0-9]+', '-', 'g')))
FROM fixture_institution_location
WHERE institution_location.institution_location_id=fixture_institution_location.institution_location_id;


-- Add owner-specific provider, clinic, and institution location fixtures. These
-- deterministic rows exercise the production location schema without leaking
-- QA-only addresses, phone numbers, or fixture URLs into the functional
-- migration.
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
