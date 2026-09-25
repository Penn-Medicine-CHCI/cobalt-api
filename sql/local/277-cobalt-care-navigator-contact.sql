BEGIN;
SELECT _v.register_patch(
  '277-local-only-cobalt-care-navigator-contact',
  ARRAY['263-local-only-care-navigator-seed'],
  NULL
);

UPDATE institution
SET metadata=jsonb_set(COALESCE(metadata, '{}'::JSONB),
                      '{careNavigatorCrisisPhoneNumber}',
                      '"1-888-321-4433"'::JSONB, TRUE)
WHERE institution_id='COBALT';

UPDATE provider
SET details_html=REPLACE(
  details_html,
  'Penn Medicine employees may also contact the EAP 24/7 Crisis Line.',
  'Penn Medicine employees may also contact the EAP 24/7 Crisis Line at 1-888-321-4433.'
)
WHERE institution_id='COBALT'
  AND url_name='cobalt-care-navigator'
  AND details_html LIKE '%Penn Medicine employees may also contact the EAP 24/7 Crisis Line.%';

COMMIT;
