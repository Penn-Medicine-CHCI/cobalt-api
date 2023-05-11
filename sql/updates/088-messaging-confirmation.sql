BEGIN;
SELECT _v.register_patch('088-messaging-confirmation', NULL, NULL);

CREATE TABLE message_vendor (
  message_vendor_id VARCHAR PRIMARY KEY,
  description VARCHAR NOT NULL
);

INSERT INTO message_vendor VALUES ('UNSPECIFIED', 'Unspecified');
INSERT INTO message_vendor VALUES ('TWILIO', 'Twilio');
INSERT INTO message_vendor VALUES ('AMAZON_AWS', 'Amazon AWS');

INSERT INTO message_status (message_status_id, description) VALUES ('DELIVERED', 'Delivered');
INSERT INTO message_status (message_status_id, description) VALUES ('DELIVERY_FAILED', 'Delivery Failed');

ALTER TABLE message_log ADD COLUMN message_vendor_id VARCHAR NOT NULL REFERENCES message_vendor DEFAULT 'UNSPECIFIED';
ALTER TABLE message_log ADD COLUMN delivered TIMESTAMP WITH TIME ZONE;
ALTER TABLE message_log ADD COLUMN delivery_failed TIMESTAMP WITH TIME ZONE;

COMMIT;