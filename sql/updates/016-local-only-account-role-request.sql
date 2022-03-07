BEGIN;
SELECT _v.register_patch('016-local-only-account-role-request', NULL, NULL);

-- Add the role request interaction ID to our COBALT institution admin's list of interaction IDs
UPDATE account
SET metadata = jsonb_set(
  metadata::jsonb,
  array['interactionIds'],
  (metadata->'interactionIds')::jsonb || '["f2d728b1-076d-433e-9166-df2a1237d2ef"]'::jsonb)
WHERE email_address='admin@cobaltinnovations.org';

END;