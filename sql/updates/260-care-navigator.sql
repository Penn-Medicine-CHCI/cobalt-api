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

-- The Cobalt Innovations administrator also participates in organization-wide
-- Care Navigator administration. The NAVIGATOR capability is additive to the
-- account's existing ADMINISTRATOR role.
INSERT INTO account_capability (
	account_id,
	account_capability_type_id
)
SELECT
	account.account_id,
	'NAVIGATOR'
FROM account
WHERE account.institution_id='COBALT'
AND account.role_id='ADMINISTRATOR'
AND LOWER(account.email_address)=LOWER('admin@cobaltinnovations.org')
ON CONFLICT (account_id, account_capability_type_id) DO NOTHING;

-- Navigator accounts may serve one or more public Care Navigator booking
-- providers.  account.provider_id remains the account's primary provider
-- identity; this mapping is used for encounter routing and appointment access.
CREATE TABLE care_navigator_provider_account (
	provider_id UUID NOT NULL REFERENCES provider(provider_id),
	account_id UUID NOT NULL REFERENCES account(account_id),
	display_order INTEGER NOT NULL DEFAULT 1 CHECK (display_order > 0),
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	PRIMARY KEY (provider_id, account_id)
);

CREATE INDEX care_navigator_provider_account_account_id_idx
ON care_navigator_provider_account(account_id);

CREATE TRIGGER set_last_updated
BEFORE INSERT OR UPDATE ON care_navigator_provider_account
FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE OR REPLACE FUNCTION validate_care_navigator_provider_account()
RETURNS TRIGGER AS $$
BEGIN
	IF NOT EXISTS (
		SELECT 1
		FROM provider
		JOIN account ON account.account_id=NEW.account_id
		WHERE provider.provider_id=NEW.provider_id
		AND provider.institution_id=account.institution_id
		AND provider.active=TRUE
		AND account.active=TRUE
		AND account.role_id IN ('ADMINISTRATOR', 'PROVIDER')
		AND EXISTS (
			SELECT 1
			FROM provider_support_role
			WHERE provider_support_role.provider_id=provider.provider_id
			AND provider_support_role.support_role_id='CARE_NAVIGATOR'
		)
		AND EXISTS (
			SELECT 1
			FROM account_capability
			WHERE account_capability.account_id=account.account_id
			AND account_capability.account_capability_type_id='NAVIGATOR'
		)
	) THEN
		RAISE EXCEPTION 'Care Navigator provider mappings require an active Care Navigator provider and an active Navigator-capable Administrator or Provider account in the same institution.';
	END IF;

	RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER validate_care_navigator_provider_account
BEFORE INSERT OR UPDATE OF provider_id, account_id ON care_navigator_provider_account
FOR EACH ROW EXECUTE FUNCTION validate_care_navigator_provider_account();

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

-- An institution only has the Care Navigator capability when its feature is
-- connected to an active Care Navigator provider in the same institution.
-- The provider is the booking entity; Navigator staff accounts remain
-- independently assignable through the NAVIGATOR account capability.
CREATE OR REPLACE FUNCTION validate_care_navigator_booking_provider()
RETURNS TRIGGER AS $$
BEGIN
	IF NEW.feature_id='RESOURCE_NAVIGATOR' THEN
		IF NEW.provider_id IS NULL THEN
			RAISE EXCEPTION 'Care Navigator feature requires a booking provider.';
		END IF;

		IF NOT EXISTS (
			SELECT 1
			FROM provider
			JOIN provider_support_role
				ON provider_support_role.provider_id=provider.provider_id
				AND provider_support_role.support_role_id='CARE_NAVIGATOR'
			WHERE provider.provider_id=NEW.provider_id
			AND provider.institution_id=NEW.institution_id
			AND provider.active=TRUE
		) THEN
			RAISE EXCEPTION 'Care Navigator booking provider must be active, belong to the institution, and have the Care Navigator support role.';
		END IF;
	END IF;

	RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER validate_care_navigator_booking_provider
BEFORE INSERT OR UPDATE OF feature_id, institution_id, provider_id ON institution_feature
FOR EACH ROW
EXECUTE FUNCTION validate_care_navigator_booking_provider();

COMMIT;
