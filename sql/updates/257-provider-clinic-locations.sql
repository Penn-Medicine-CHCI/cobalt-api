BEGIN;
SELECT _v.register_patch('257-provider-clinic-locations', NULL, NULL);

CREATE TABLE IF NOT EXISTS provider_location (
	provider_location_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	provider_id UUID NOT NULL REFERENCES provider,
	address_id UUID REFERENCES address,
	name TEXT NOT NULL,
	short_name TEXT,
	display_order INTEGER NOT NULL,
	phone_number TEXT,
	website_url TEXT,
	email_address TEXT,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	CONSTRAINT provider_location_nonempty_name CHECK (LENGTH(BTRIM(name)) > 0)
);

DO $$
BEGIN
	IF NOT EXISTS (
		SELECT 1
		FROM pg_trigger
		WHERE tgrelid='provider_location'::REGCLASS
		AND tgname='set_last_updated'
	) THEN
		CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON provider_location FOR EACH ROW EXECUTE PROCEDURE set_last_updated();
	END IF;
END $$;

CREATE INDEX IF NOT EXISTS provider_location_provider_id_display_order_idx
ON provider_location(provider_id, display_order, name, provider_location_id);

CREATE TABLE IF NOT EXISTS clinic_location (
	clinic_location_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	clinic_id UUID NOT NULL REFERENCES clinic,
	address_id UUID REFERENCES address,
	name TEXT NOT NULL,
	short_name TEXT,
	display_order INTEGER NOT NULL,
	phone_number TEXT,
	website_url TEXT,
	email_address TEXT,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	CONSTRAINT clinic_location_nonempty_name CHECK (LENGTH(BTRIM(name)) > 0)
);

DO $$
BEGIN
	IF NOT EXISTS (
		SELECT 1
		FROM pg_trigger
		WHERE tgrelid='clinic_location'::REGCLASS
		AND tgname='set_last_updated'
	) THEN
		CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON clinic_location FOR EACH ROW EXECUTE PROCEDURE set_last_updated();
	END IF;
END $$;

CREATE INDEX IF NOT EXISTS clinic_location_clinic_id_display_order_idx
ON clinic_location(clinic_id, display_order, name, clinic_location_id);

COMMIT;
