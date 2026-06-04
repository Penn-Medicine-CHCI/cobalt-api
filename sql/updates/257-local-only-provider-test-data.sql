BEGIN;
SELECT _v.register_patch('257-local-only-provider-test-data', NULL, NULL);

UPDATE clinic
SET bookable_as_provider=TRUE
WHERE clinic_id='ab629384-400a-4688-8465-04636ec2eaa2'
AND institution_id='COBALT';

DELETE FROM provider_institution_location
WHERE provider_id IN (
  'dc7aeafd-0fc8-4c4d-b09a-09d4dc3079c1',
  '2d6b7032-0145-4273-84f5-94e7238bc331',
  '360d46c4-2ee9-4031-aab6-aa6a16f398d7',
  '31633b9d-651b-402b-9314-7def6af811b6'
);

INSERT INTO provider_institution_location (
  provider_id,
  institution_location_id
)
SELECT provider_location.provider_id, il.institution_location_id
FROM (
  VALUES
    ('dc7aeafd-0fc8-4c4d-b09a-09d4dc3079c1'::uuid, 'Cobalt General'),
    ('2d6b7032-0145-4273-84f5-94e7238bc331'::uuid, 'Cobalt General'),
    ('360d46c4-2ee9-4031-aab6-aa6a16f398d7'::uuid, 'Cobalt Health System'),
    ('31633b9d-651b-402b-9314-7def6af811b6'::uuid, 'Cobalt Health System')
) AS provider_location(provider_id, institution_location_name)
JOIN provider p ON p.provider_id=provider_location.provider_id
JOIN institution_location il ON il.institution_id=p.institution_id
  AND il.name=provider_location.institution_location_name
WHERE p.institution_id='COBALT';

COMMIT;
