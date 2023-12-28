BEGIN;
SELECT _v.register_patch('140-fcm', NULL, NULL);

CREATE TABLE client_device_type (
	client_device_type_id TEXT NOT NULL PRIMARY KEY,
	description TEXT NOT NULL
);

INSERT INTO client_device_type VALUES ('WEB_BROWSER', 'Web Browser');
INSERT INTO client_device_type VALUES ('IOS_APP', 'iOS App');
INSERT INTO client_device_type VALUES ('ANDROID_APP', 'Android App');

CREATE TABLE client_device_push_token_type (
	client_device_push_token_type_id TEXT NOT NULL PRIMARY KEY,
	description TEXT NOT NULL
);

INSERT INTO client_device_push_token_type VALUES ('NATIVE', 'Native');
INSERT INTO client_device_push_token_type VALUES ('GOOGLE_FCM', 'Google Firebase Cloud Messaging (FCM)');

CREATE TABLE client_device (
	client_device_id UUID NOT NULL PRIMARY KEY DEFAULT uuid_generate_v4(),
	client_device_type_id TEXT NOT NULL REFERENCES client_device_type,
	fingerprint TEXT NOT NULL, -- Unique device identifier
	model_name TEXT,
	operating_system_name TEXT,
	operating_system_version TEXT,
	created TIMESTAMPTZ NOT NULL DEFAULT now(),
	last_updated TIMESTAMPTZ NOT NULL
);

-- Devices must have unique fingerprints
CREATE UNIQUE INDEX client_device_unique_idx ON client_device USING btree (fingerprint);

-- Explicit constraint on table so we can upsert and say ON CONFLICT (client_device_fingerprint_unique)
ALTER TABLE client_device ADD CONSTRAINT client_device_unique_idx UNIQUE USING INDEX client_device_unique_idx;

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON client_device FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE client_device_push_token (
  client_device_push_token_id UUID NOT NULL PRIMARY KEY DEFAULT uuid_generate_v4(),
  client_device_id UUID NOT NULL REFERENCES client_device,
  client_device_push_token_type_id TEXT NOT NULL REFERENCES client_device_push_token_type,
  push_token TEXT NOT NULL,
  valid BOOLEAN NOT NULL DEFAULT TRUE,
  created TIMESTAMPTZ NOT NULL DEFAULT now(),
  last_updated TIMESTAMPTZ NOT NULL
);

-- Push tokens must be unique for the combination of device/token/type
CREATE UNIQUE INDEX client_device_push_token_unique_idx ON client_device_push_token USING btree (client_device_id, push_token, client_device_push_token_type_id);

-- Explicit constraint on table so we can upsert and say ON CONFLICT (client_device_fingerprint_unique)
ALTER TABLE client_device_push_token ADD CONSTRAINT client_device_push_token_unique_idx UNIQUE USING INDEX client_device_push_token_unique_idx;

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON client_device_push_token FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE account_client_device (
	account_id UUID NOT NULL REFERENCES account,
	client_device_id UUID NOT NULL REFERENCES client_device,
	created TIMESTAMPTZ NOT NULL DEFAULT now(),
	last_updated TIMESTAMPTZ NOT NULL,
	CONSTRAINT account_client_device_unique_idx PRIMARY KEY (account_id, client_device_id)
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON account_client_device FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

INSERT INTO message_vendor VALUES ('GOOGLE_FCM', 'Google Firebase Cloud Messaging (FCM)');

-- FCM is configurable per-institution
-- TODO: move this and other keys into their own table separate from `institution` so we can more tightly restrict access
ALTER TABLE institution ADD COLUMN google_fcm_service_account_private_key VARCHAR;

-- Additional information at the study level for push and coordinator
ALTER TABLE study ADD COLUMN coordinator_name TEXT;
ALTER TABLE study ADD COLUMN coordinator_email_address TEXT;
ALTER TABLE study ADD COLUMN coordinator_phone_number TEXT; -- E.164 format, e.g. +12155551212

CREATE TABLE file_upload_type (
	file_upload_type_id TEXT NOT NULL PRIMARY KEY,
	description TEXT NOT NULL
);

INSERT INTO file_upload_type VALUES ('UNSPECIFIED', 'Unspecified');
INSERT INTO file_upload_type VALUES ('CONTENT', 'Content');
INSERT INTO file_upload_type VALUES ('IMAGE', 'Image');
INSERT INTO file_upload_type VALUES ('VIDEO', 'Video');
INSERT INTO file_upload_type VALUES ('AUDIO', 'Audio');
INSERT INTO file_upload_type VALUES ('ACCELEROMETER', 'Accelerometer');
INSERT INTO file_upload_type VALUES ('GPS', 'GPS');
INSERT INTO file_upload_type VALUES ('STEPS', 'Steps');
INSERT INTO file_upload_type VALUES ('PHONE_CALL', 'Phone Call');
INSERT INTO file_upload_type VALUES ('TEXT_MESSAGE', 'Text Message');
INSERT INTO file_upload_type VALUES ('PROXIMITY', 'Proximity');
INSERT INTO file_upload_type VALUES ('MAGNETOMETER', 'Magnetometer');
INSERT INTO file_upload_type VALUES ('DEVICE_MOTION', 'Device Motion');
INSERT INTO file_upload_type VALUES ('REACHABILITY', 'Reachability');
INSERT INTO file_upload_type VALUES ('WIFI', 'Wi-Fi');
INSERT INTO file_upload_type VALUES ('BLUETOOTH', 'Bluetooth');
INSERT INTO file_upload_type VALUES ('POWER_STATE', 'Power State');

ALTER TABLE file_upload ADD COLUMN file_upload_type_id TEXT NOT NULL REFERENCES file_upload_type DEFAULT 'UNSPECIFIED';

COMMIT;