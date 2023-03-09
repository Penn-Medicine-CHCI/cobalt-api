BEGIN;
SELECT _v.register_patch('062-patient-order-scheduled-messages', NULL, NULL);

-- What was the source of this scheduled message?  Currently system-generated or manually scheduled by a user, e.g. an MHIC
CREATE TABLE scheduled_message_source (
  scheduled_message_source_id VARCHAR PRIMARY KEY,
  description VARCHAR NOT NULL
);

INSERT INTO scheduled_message_source VALUES ('SYSTEM', 'System-generated');
INSERT INTO scheduled_message_source VALUES ('MANUAL', 'Manually Scheduled');

ALTER TABLE scheduled_message ADD COLUMN scheduled_message_source_id VARCHAR REFERENCES scheduled_message_source NOT NULL DEFAULT 'SYSTEM';

-- Nullable; most messages are scheduled automatically by the system, not by an account
ALTER TABLE scheduled_message ADD COLUMN scheduled_by_account_id UUID REFERENCES account(account_id);

-- Keep track of scheduled messages for this order
CREATE TABLE patient_order_scheduled_message (
  patient_order_scheduled_message_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  patient_order_id UUID NOT NULL REFERENCES patient_order,
  scheduled_message_id UUID NOT NULL REFERENCES scheduled_message,
  created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX patient_order_scheduled_message_unique_idx ON patient_order_scheduled_message USING btree (patient_order_id, scheduled_message_id);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON patient_order_scheduled_message FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

COMMIT;