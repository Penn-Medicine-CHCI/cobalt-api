BEGIN;
SELECT _v.register_patch('089-messaging-confirmation', NULL, NULL);

-- Rename; we might support other kinds of AWS messaging services in the future
DELETE FROM message_vendor WHERE message_vendor_id='AMAZON_AWS';
INSERT INTO message_vendor VALUES ('AMAZON_SES', 'Amazon SES');

ALTER TABLE message_log ADD COLUMN vendor_assigned_id VARCHAR;  -- e.g. the ID we get from AWS or Twilio at send time

-- Vendor IDs must be unique per-vendor
CREATE UNIQUE INDEX message_log_message_vendor_vendor_assigned_id_unique_idx ON message_log USING btree (message_vendor_id, vendor_assigned_id);

-- Maintains a history of webhooks (and potentially other events) that we receive for a message after it leaves our system.
-- For example, we might send an email and then later get a bounce or delivery notification from a Twilio or SES webhook.
-- This way we have a permanent record of everything we know about a message after it leaves our system.
--
-- Format of event_data:
--
-- {
--   "webhookHeaders": { "{headerName}" : "{headerValue}", ... },
--   "webhookRequestBody": "{raw request body as string}"
--   "webhookPayload": { /* pure JSON representation of all relevant webhook data for ease of querying */ }
-- }
CREATE TABLE message_log_event (
  message_log_event_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  message_id UUID NOT NULL REFERENCES message_log(message_id),
  event_data JSONB NOT NULL, -- different vendors/message types will have different data.  Understood to be a JSON object, not array
  created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON message_log_event FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

COMMIT;