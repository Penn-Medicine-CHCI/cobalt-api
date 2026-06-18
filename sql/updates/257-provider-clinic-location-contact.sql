BEGIN;
SELECT _v.register_patch('257-provider-clinic-location-contact', NULL, NULL);

ALTER TABLE clinic ADD COLUMN IF NOT EXISTS website_url TEXT;

ALTER TABLE institution_location ADD COLUMN IF NOT EXISTS address_id UUID NULL REFERENCES address;
ALTER TABLE institution_location ADD COLUMN IF NOT EXISTS phone_number TEXT;
ALTER TABLE institution_location ADD COLUMN IF NOT EXISTS website_url TEXT;
ALTER TABLE institution_location ADD COLUMN IF NOT EXISTS email_address TEXT;

CREATE INDEX IF NOT EXISTS institution_location_institution_id_display_order_idx
ON institution_location(institution_id, display_order, name, institution_location_id);

CREATE INDEX IF NOT EXISTS provider_institution_location_provider_id_idx
ON provider_institution_location(provider_id, institution_location_id);

CREATE INDEX IF NOT EXISTS provider_institution_location_institution_location_id_idx
ON provider_institution_location(institution_location_id, provider_id);

COMMIT;
