-- Local COBALT preview data for the PENN employer grouping shown in the
-- September 23 provider notes. This does not change the PENN tenant.
BEGIN;
SELECT _v.register_patch(
  '274-local-only-cobalt-penn-location-preview',
  ARRAY['266-local-only-cobalt-employer-onboarding-single-question'],
  NULL
);

DO $check$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM institution WHERE institution_id = 'COBALT') THEN
    RAISE EXCEPTION 'COBALT institution is missing';
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM institution_location
    WHERE institution_id = 'COBALT'
      AND institution_location_id = '2d2aa1cc-97b6-48cf-b4b8-2b74ccbb3574'
  ) OR NOT EXISTS (
    SELECT 1 FROM institution_location
    WHERE institution_id = 'COBALT'
      AND institution_location_id = 'c898aa56-8d57-4555-b241-725e31f2a06c'
  ) THEN
    RAISE EXCEPTION 'Expected local COBALT preview locations are missing';
  END IF;
END
$check$;

INSERT INTO institution_location_group (institution_location_group_id, institution_id, name, display_order)
VALUES
  (uuid_generate_v5(uuid_ns_url(), 'cobalt-local-preview-penn-uphs-group'), 'COBALT', 'University of Pennsylvania Health System (UPHS)', 1),
  (uuid_generate_v5(uuid_ns_url(), 'cobalt-local-preview-penn-upenn-group'), 'COBALT', 'University of Pennsylvania (UPenn)', 2)
ON CONFLICT DO NOTHING;

-- Keep the existing broad-location identities as the two "Other" choices.
UPDATE institution_location
SET name = 'Other', short_name = 'Other', display_order = 10,
    institution_location_group_id = (
      SELECT institution_location_group_id FROM institution_location_group
      WHERE institution_id = 'COBALT' AND name = 'University of Pennsylvania Health System (UPHS)'
    )
WHERE institution_id = 'COBALT'
  AND institution_location_id = '2d2aa1cc-97b6-48cf-b4b8-2b74ccbb3574';

UPDATE institution_location
SET name = 'Other', short_name = 'Other', display_order = 3,
    institution_location_group_id = (
      SELECT institution_location_group_id FROM institution_location_group
      WHERE institution_id = 'COBALT' AND name = 'University of Pennsylvania (UPenn)'
    )
WHERE institution_id = 'COBALT'
  AND institution_location_id = 'c898aa56-8d57-4555-b241-725e31f2a06c';

UPDATE institution_location
SET name = 'Lancaster General Health (LGH)', short_name = 'LGH', display_order = 4
WHERE institution_id = 'COBALT'
  AND institution_location_id = 'f73191c8-be08-43f5-8a90-bd3ed1dd81ce';

UPDATE institution_location
SET name = 'Princeton Medical Center (PMC)', short_name = 'PMC', display_order = 5
WHERE institution_id = 'COBALT'
  AND institution_location_id = '70affd3f-dd74-4a95-99af-5a311c847c6e';

UPDATE institution_location
SET name = 'Doylestown', short_name = 'Doylestown', display_order = 6
WHERE institution_id = 'COBALT'
  AND institution_location_id = 'a2dfa975-877b-4a7c-9a56-f3e0ea2f3bc5';

UPDATE institution_location
SET name = 'Chester County Hospital (CCH)', short_name = 'CCH', display_order = 7
WHERE institution_id = 'COBALT'
  AND institution_location_id = 'd1fe3d12-c8ad-41b6-8f53-66c8ac1df381';

WITH new_location (key, group_name, name, short_name, display_order) AS (
  VALUES
    ('uphs-pah', 'University of Pennsylvania Health System (UPHS)', 'Pennsylvania Hospital (PAH)', 'PAH', 1),
    ('uphs-hup', 'University of Pennsylvania Health System (UPHS)', 'Hospital of the University of Pennsylvania (HUP)', 'HUP', 2),
    ('uphs-ppmc', 'University of Pennsylvania Health System (UPHS)', 'Penn Presbyterian Medical Center (PPMC)', 'PPMC', 3),
    ('uphs-outpatient', 'University of Pennsylvania Health System (UPHS)', 'Outpatient Locations (e.g Cherry Hill, Radnor, Washington Sq.)', 'Outpatient', 8),
    ('uphs-corporate', 'University of Pennsylvania Health System (UPHS)', 'Corporate', 'Corporate', 9),
    ('upenn-university', 'University of Pennsylvania (UPenn)', 'University of Pennsylvania', 'University', 1),
    ('upenn-psom', 'University of Pennsylvania (UPenn)', 'Perelman School of Medicine (PSOM)', 'PSOM', 2)
)
INSERT INTO institution_location (
  institution_location_id, institution_id, name, short_name, display_order, institution_location_group_id
)
SELECT uuid_generate_v5(uuid_ns_url(), 'cobalt-local-preview-penn-' || nl.key),
       'COBALT', nl.name, nl.short_name, nl.display_order, g.institution_location_group_id
FROM new_location nl
JOIN institution_location_group g ON g.institution_id = 'COBALT' AND g.name = nl.group_name
ON CONFLICT (institution_location_id) DO UPDATE
SET name = EXCLUDED.name,
    short_name = EXCLUDED.short_name,
    display_order = EXCLUDED.display_order,
    institution_location_group_id = EXCLUDED.institution_location_group_id;

-- Groups are display metadata, so copy the existing local provider links to
-- each new eligible leaf. The broad UPHS "Other" location has the full local
-- UPHS set. LGH retains its narrower set plus Dr. Fetrow-Keihl.
INSERT INTO provider_institution_location (provider_id, institution_location_id)
SELECT DISTINCT source_link.provider_id, target.institution_location_id
FROM provider_institution_location source_link
JOIN institution_location target ON target.institution_id = 'COBALT'
JOIN institution_location_group g
  ON g.institution_location_group_id = target.institution_location_group_id
WHERE source_link.institution_location_id = '2d2aa1cc-97b6-48cf-b4b8-2b74ccbb3574'
  AND g.name = 'University of Pennsylvania Health System (UPHS)'
  AND target.institution_location_id <> 'f73191c8-be08-43f5-8a90-bd3ed1dd81ce'
  AND NOT EXISTS (
    SELECT 1 FROM provider_institution_location existing
    WHERE existing.provider_id = source_link.provider_id
      AND existing.institution_location_id = target.institution_location_id
  );

INSERT INTO provider_institution_location (provider_id, institution_location_id)
SELECT DISTINCT source_link.provider_id, target.institution_location_id
FROM provider_institution_location source_link
JOIN institution_location target ON target.institution_id = 'COBALT'
JOIN institution_location_group g
  ON g.institution_location_group_id = target.institution_location_group_id
WHERE source_link.institution_location_id = 'c898aa56-8d57-4555-b241-725e31f2a06c'
  AND g.name = 'University of Pennsylvania (UPenn)'
  AND NOT EXISTS (
    SELECT 1 FROM provider_institution_location existing
    WHERE existing.provider_id = source_link.provider_id
      AND existing.institution_location_id = target.institution_location_id
  );

INSERT INTO provider_institution_location (provider_id, institution_location_id)
SELECT p.provider_id, 'f73191c8-be08-43f5-8a90-bd3ed1dd81ce'::uuid
FROM provider p
WHERE p.institution_id = 'COBALT' AND p.url_name = 'steven-fetrow-keihl'
  AND NOT EXISTS (
    SELECT 1 FROM provider_institution_location existing
    WHERE existing.provider_id = p.provider_id
      AND existing.institution_location_id = 'f73191c8-be08-43f5-8a90-bd3ed1dd81ce'
  );

-- Three local UPHS EAP demo clinicians were global (no location rows), which
-- made their UPHS-only cards appear for UPenn. Keep their previous availability
-- at the ungrouped COBALT locations, and assign them to each UPHS leaf.
INSERT INTO provider_institution_location (provider_id, institution_location_id)
SELECT p.provider_id, il.institution_location_id
FROM provider p
JOIN institution_location il ON il.institution_id = 'COBALT'
LEFT JOIN institution_location_group g
  ON g.institution_location_group_id = il.institution_location_group_id
WHERE p.institution_id = 'COBALT'
  AND p.url_name IN ('eap-clinician-9', 'eap-clinician-10', 'eap-clinician-11')
  AND (g.name = 'University of Pennsylvania Health System (UPHS)' OR g.institution_location_group_id IS NULL)
  AND NOT EXISTS (
    SELECT 1 FROM provider_institution_location existing
    WHERE existing.provider_id = p.provider_id
      AND existing.institution_location_id = il.institution_location_id
  );

COMMIT;
