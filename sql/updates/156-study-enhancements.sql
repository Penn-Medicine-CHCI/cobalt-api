BEGIN;
SELECT _v.register_patch('156-study-enhancements', NULL, NULL);

-- Now stored in Secrets Manager
ALTER TABLE institution DROP COLUMN google_fcm_service_account_private_key;
ALTER TABLE institution ADD COLUMN google_fcm_push_notifications_enabled BOOLEAN NOT NULL DEFAULT FALSE;

-- Ability to track events that occur on a client device
CREATE TABLE client_device_activity_type (
	client_device_activity_type_id TEXT NOT NULL PRIMARY KEY,
	description TEXT NOT NULL
);

-- e.g. on a native app, entering the foreground or being sent to background
INSERT INTO client_device_activity_type VALUES ('ENTERED_FOREGROUND', 'Entered Foreground');
INSERT INTO client_device_activity_type VALUES ('ENTERED_BACKGROUND', 'Entered Background');

CREATE TABLE client_device_activity (
	client_device_activity_id UUID NOT NULL PRIMARY KEY DEFAULT uuid_generate_v4(),
	client_device_id UUID NOT NULL REFERENCES client_device,
	client_device_activity_type_id TEXT NOT NULL REFERENCES client_device_activity_type,
	account_id UUID REFERENCES account, -- OK to be null, e.g. app is brought to the foreground or background before user signs in
	created TIMESTAMPTZ NOT NULL DEFAULT now(),
	last_updated TIMESTAMPTZ NOT NULL
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON client_device_activity FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

COMMIT;