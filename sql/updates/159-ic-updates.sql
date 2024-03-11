BEGIN;
SELECT _v.register_patch('159-ic-updates', NULL, NULL);

ALTER TABLE institution ADD COLUMN call_messages_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE institution ADD COLUMN sms_messages_enabled BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE institution ADD COLUMN twilio_account_sid TEXT;
ALTER TABLE institution ADD COLUMN twilio_from_number TEXT; -- E.164 format, e.g. +12155551212

COMMIT;