BEGIN;
SELECT _v.register_patch('257-provider-clinic-location-contact', NULL, NULL);

ALTER TABLE clinic ADD COLUMN IF NOT EXISTS website_url TEXT;

CREATE TABLE IF NOT EXISTS provider_location (
	provider_location_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	provider_id UUID NOT NULL REFERENCES provider,
	address_id UUID NULL REFERENCES address,
	name TEXT NULL,
	phone_number TEXT NULL,
	website_url TEXT NULL,
	email_address TEXT NULL,
	display_order INTEGER NOT NULL DEFAULT 1,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

DO $$
BEGIN
	IF NOT EXISTS (
		SELECT 1
		FROM pg_trigger
		WHERE tgname='provider_location_set_last_updated'
		AND tgrelid='provider_location'::REGCLASS
	) THEN
		CREATE TRIGGER provider_location_set_last_updated
		BEFORE INSERT OR UPDATE ON provider_location
		FOR EACH ROW EXECUTE PROCEDURE set_last_updated();
	END IF;
END $$;

CREATE INDEX IF NOT EXISTS provider_location_provider_id_display_order_idx
ON provider_location(provider_id, display_order, name, provider_location_id);

CREATE TABLE IF NOT EXISTS clinic_location (
	clinic_location_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	clinic_id UUID NOT NULL REFERENCES clinic,
	address_id UUID NULL REFERENCES address,
	name TEXT NULL,
	phone_number TEXT NULL,
	website_url TEXT NULL,
	email_address TEXT NULL,
	display_order INTEGER NOT NULL DEFAULT 1,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

DO $$
BEGIN
	IF NOT EXISTS (
		SELECT 1
		FROM pg_trigger
		WHERE tgname='clinic_location_set_last_updated'
		AND tgrelid='clinic_location'::REGCLASS
	) THEN
		CREATE TRIGGER clinic_location_set_last_updated
		BEFORE INSERT OR UPDATE ON clinic_location
		FOR EACH ROW EXECUTE PROCEDURE set_last_updated();
	END IF;
END $$;

CREATE INDEX IF NOT EXISTS clinic_location_clinic_id_display_order_idx
ON clinic_location(clinic_id, display_order, name, clinic_location_id);

COMMIT;
