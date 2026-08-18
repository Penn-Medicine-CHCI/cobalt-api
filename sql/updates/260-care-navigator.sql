BEGIN;
SELECT _v.register_patch('260-care-navigator', ARRAY['259-provider-booking-database'], NULL);

-- Home-page features may link directly to a specific provider details view.
-- Existing features remain unchanged because this association is optional.
ALTER TABLE institution_feature
ADD COLUMN IF NOT EXISTS provider_id UUID REFERENCES provider(provider_id);

-- RESOURCE_NAVIGATOR is a legacy internal identifier. Use Care Navigator in
-- every user-facing feature response.
UPDATE feature
SET name='Connect with a Care Navigator'
WHERE feature_id='RESOURCE_NAVIGATOR';

-- Care Navigators use the existing provider login role. Their provider-search
-- identity and signed-in experience are modeled separately so future
-- administrative capabilities do not require another account role.
INSERT INTO support_role (
	support_role_id,
	description,
	display_order
)
VALUES ('CARE_NAVIGATOR', 'Care Navigator', 12)
ON CONFLICT (support_role_id) DO UPDATE
SET description=EXCLUDED.description,
	display_order=EXCLUDED.display_order;

INSERT INTO account_capability_type (
	account_capability_type_id,
	description
)
VALUES ('NAVIGATOR', 'Care Navigator')
ON CONFLICT (account_capability_type_id) DO UPDATE
SET description=EXCLUDED.description;

-- RESOURCE_NAVIGATOR already exists as a global feature. Connect it to the new
-- provider support role without changing its route or tenant visibility.
INSERT INTO feature_support_role (
	feature_support_role_id,
	feature_id,
	support_role_id
)
SELECT
	'67ab92eb-fb06-4d83-9103-5b97fdb10007'::UUID,
	'RESOURCE_NAVIGATOR',
	'CARE_NAVIGATOR'
WHERE EXISTS (
	SELECT 1
	FROM feature
	WHERE feature_id='RESOURCE_NAVIGATOR'
)
ON CONFLICT (feature_id, support_role_id) DO NOTHING;

COMMIT;
