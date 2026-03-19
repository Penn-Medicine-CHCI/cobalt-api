BEGIN;
SELECT _v.register_patch('248-ip-geolocation', NULL, NULL);

CREATE TABLE ip_geolocation_status (
  ip_geolocation_status_id TEXT PRIMARY KEY,
  description TEXT NOT NULL
);

INSERT INTO ip_geolocation_status VALUES ('PENDING', 'Pending');
INSERT INTO ip_geolocation_status VALUES ('IN_PROGRESS', 'In Progress');
INSERT INTO ip_geolocation_status VALUES ('SUCCEEDED', 'Succeeded');
INSERT INTO ip_geolocation_status VALUES ('FAILED', 'Failed');
INSERT INTO ip_geolocation_status VALUES ('SKIPPED_INVALID', 'Skipped - Invalid');
INSERT INTO ip_geolocation_status VALUES ('SKIPPED_PRIVATE', 'Skipped - Private/Reserved');

CREATE TABLE ip_geolocation (
  ip_address INET PRIMARY KEY,
  ip_geolocation_status_id TEXT NOT NULL REFERENCES ip_geolocation_status DEFAULT 'PENDING',
  provider_name TEXT NOT NULL DEFAULT 'IPSTACK',
  ip_type TEXT,
  continent_code TEXT,
  continent_name TEXT,
  country_code TEXT,
  country_name TEXT,
  region_code TEXT,
  region_name TEXT,
  city TEXT,
  postal_code TEXT,
  latitude DOUBLE PRECISION,
  longitude DOUBLE PRECISION,
  msa TEXT,
  dma TEXT,
  radius INTEGER,
  ip_routing_type TEXT,
  connection_type TEXT,
  location_geoname_id BIGINT,
  location_capital TEXT,
  location_languages JSONB,
  location_country_flag TEXT,
  location_country_flag_emoji TEXT,
  location_country_flag_emoji_unicode TEXT,
  location_calling_code TEXT,
  location_is_eu BOOLEAN,
  time_zone_id TEXT,
  time_zone_current_time TEXT,
  time_zone_gmt_offset INTEGER,
  time_zone_code TEXT,
  time_zone_is_daylight_saving BOOLEAN,
  currency_code TEXT,
  currency_name TEXT,
  currency_plural TEXT,
  currency_symbol TEXT,
  currency_symbol_native TEXT,
  connection_asn BIGINT,
  connection_isp TEXT,
  connection_sld TEXT,
  connection_tld TEXT,
  connection_carrier TEXT,
  connection_home BOOLEAN,
  connection_organization_type TEXT,
  connection_isic_code TEXT,
  connection_naics_code TEXT,
  hostname TEXT,
  security_is_proxy BOOLEAN,
  security_proxy_type TEXT,
  security_is_crawler BOOLEAN,
  security_crawler_name TEXT,
  security_crawler_type TEXT,
  security_is_tor BOOLEAN,
  security_threat_level TEXT,
  security_threat_types JSONB,
  security_proxy_last_detected TEXT,
  security_proxy_level TEXT,
  security_vpn_service TEXT,
  security_anonymizer_status TEXT,
  security_hosting_facility BOOLEAN,
  provider_error_code INTEGER,
  provider_error_type TEXT,
  provider_error_message TEXT,
  provider_raw_json JSONB,
  last_lookup_attempted_at TIMESTAMPTZ,
  last_lookup_succeeded_at TIMESTAMPTZ,
  created TIMESTAMPTZ NOT NULL DEFAULT now(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON ip_geolocation FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE INDEX ip_geolocation_status_idx ON ip_geolocation(ip_geolocation_status_id);
CREATE INDEX ip_geolocation_last_lookup_attempted_at_idx ON ip_geolocation(last_lookup_attempted_at);

COMMIT;
