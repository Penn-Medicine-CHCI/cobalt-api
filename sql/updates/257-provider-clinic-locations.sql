CREATE TABLE provider_location (
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

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON provider_location FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE INDEX provider_location_provider_id_display_order_idx
ON provider_location(provider_id, display_order, name, provider_location_id);

CREATE TABLE clinic_location (
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

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON clinic_location FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE INDEX clinic_location_clinic_id_display_order_idx
ON clinic_location(clinic_id, display_order, name, clinic_location_id);
