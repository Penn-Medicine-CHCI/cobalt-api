BEGIN;
SELECT _v.register_patch('257-provider-booking-contact-ownership-cleanup', ARRAY['256-provider-booking-screening'], NULL);

-- Booking V2 contact information belongs to provider/clinic owner records.
-- This cleanup preserves branch-local values from the earlier location-level
-- schema before removing those accidental columns.
ALTER TABLE provider ADD COLUMN IF NOT EXISTS website_url TEXT;
ALTER TABLE clinic ADD COLUMN IF NOT EXISTS email_address TEXT;

DO $$
BEGIN
	IF EXISTS (
		SELECT 1
		FROM information_schema.columns
		WHERE table_schema=CURRENT_SCHEMA()
		AND table_name='provider_location'
		AND column_name='phone_number'
	) THEN
		EXECUTE $SQL$
			UPDATE provider
			SET phone_number=provider_contact.phone_number
			FROM (
				SELECT DISTINCT ON (provider_location.provider_id)
					provider_location.provider_id,
					NULLIF(BTRIM(provider_location.phone_number), '') AS phone_number
				FROM provider_location
				WHERE NULLIF(BTRIM(provider_location.phone_number), '') IS NOT NULL
				ORDER BY provider_location.provider_id, provider_location.display_order, provider_location.name, provider_location.provider_location_id
			) provider_contact
			WHERE provider.provider_id=provider_contact.provider_id
			AND NULLIF(BTRIM(provider.phone_number), '') IS NULL
		$SQL$;
	END IF;

	IF EXISTS (
		SELECT 1
		FROM information_schema.columns
		WHERE table_schema=CURRENT_SCHEMA()
		AND table_name='provider_location'
		AND column_name='website_url'
	) THEN
		EXECUTE $SQL$
			UPDATE provider
			SET website_url=provider_contact.website_url
			FROM (
				SELECT DISTINCT ON (provider_location.provider_id)
					provider_location.provider_id,
					NULLIF(BTRIM(provider_location.website_url), '') AS website_url
				FROM provider_location
				WHERE NULLIF(BTRIM(provider_location.website_url), '') IS NOT NULL
				ORDER BY provider_location.provider_id, provider_location.display_order, provider_location.name, provider_location.provider_location_id
			) provider_contact
			WHERE provider.provider_id=provider_contact.provider_id
			AND NULLIF(BTRIM(provider.website_url), '') IS NULL
		$SQL$;
	END IF;

	IF EXISTS (
		SELECT 1
		FROM information_schema.columns
		WHERE table_schema=CURRENT_SCHEMA()
		AND table_name='clinic_location'
		AND column_name='phone_number'
	) THEN
		EXECUTE $SQL$
			UPDATE clinic
			SET phone_number=clinic_contact.phone_number
			FROM (
				SELECT DISTINCT ON (clinic_location.clinic_id)
					clinic_location.clinic_id,
					NULLIF(BTRIM(clinic_location.phone_number), '') AS phone_number
				FROM clinic_location
				WHERE NULLIF(BTRIM(clinic_location.phone_number), '') IS NOT NULL
				ORDER BY clinic_location.clinic_id, clinic_location.display_order, clinic_location.name, clinic_location.clinic_location_id
			) clinic_contact
			WHERE clinic.clinic_id=clinic_contact.clinic_id
			AND NULLIF(BTRIM(clinic.phone_number), '') IS NULL
		$SQL$;
	END IF;

	IF EXISTS (
		SELECT 1
		FROM information_schema.columns
		WHERE table_schema=CURRENT_SCHEMA()
		AND table_name='clinic_location'
		AND column_name='website_url'
	) THEN
		EXECUTE $SQL$
			UPDATE clinic
			SET website_url=clinic_contact.website_url
			FROM (
				SELECT DISTINCT ON (clinic_location.clinic_id)
					clinic_location.clinic_id,
					NULLIF(BTRIM(clinic_location.website_url), '') AS website_url
				FROM clinic_location
				WHERE NULLIF(BTRIM(clinic_location.website_url), '') IS NOT NULL
				ORDER BY clinic_location.clinic_id, clinic_location.display_order, clinic_location.name, clinic_location.clinic_location_id
			) clinic_contact
			WHERE clinic.clinic_id=clinic_contact.clinic_id
			AND NULLIF(BTRIM(clinic.website_url), '') IS NULL
		$SQL$;
	END IF;

	IF EXISTS (
		SELECT 1
		FROM information_schema.columns
		WHERE table_schema=CURRENT_SCHEMA()
		AND table_name='clinic_location'
		AND column_name='email_address'
	) THEN
		EXECUTE $SQL$
			UPDATE clinic
			SET email_address=clinic_contact.email_address
			FROM (
				SELECT DISTINCT ON (clinic_location.clinic_id)
					clinic_location.clinic_id,
					NULLIF(BTRIM(clinic_location.email_address), '') AS email_address
				FROM clinic_location
				WHERE NULLIF(BTRIM(clinic_location.email_address), '') IS NOT NULL
				ORDER BY clinic_location.clinic_id, clinic_location.display_order, clinic_location.name, clinic_location.clinic_location_id
			) clinic_contact
			WHERE clinic.clinic_id=clinic_contact.clinic_id
			AND NULLIF(BTRIM(clinic.email_address), '') IS NULL
		$SQL$;
	END IF;
END $$;

UPDATE provider
SET website_url=NULLIF(BTRIM(bio_url), '')
WHERE NULLIF(BTRIM(website_url), '') IS NULL
AND NULLIF(BTRIM(bio_url), '') IS NOT NULL;

ALTER TABLE provider_location DROP COLUMN IF EXISTS phone_number;
ALTER TABLE provider_location DROP COLUMN IF EXISTS website_url;
ALTER TABLE provider_location DROP COLUMN IF EXISTS email_address;

ALTER TABLE clinic_location DROP COLUMN IF EXISTS phone_number;
ALTER TABLE clinic_location DROP COLUMN IF EXISTS website_url;
ALTER TABLE clinic_location DROP COLUMN IF EXISTS email_address;

ALTER TABLE institution_location DROP COLUMN IF EXISTS address_id;
ALTER TABLE institution_location DROP COLUMN IF EXISTS phone_number;
ALTER TABLE institution_location DROP COLUMN IF EXISTS website_url;
ALTER TABLE institution_location DROP COLUMN IF EXISTS email_address;

COMMIT;
