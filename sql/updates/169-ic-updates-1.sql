BEGIN;
SELECT _v.register_patch('169-ic-updates-1', NULL, NULL);

-- Optionally restrict, for example, late-night orders coming through.
-- This way patients don't get texts at midnight if a doctor is working late.
-- Any order imports would queue up and fire once the start time rolls around.
ALTER TABLE institution ADD COLUMN integrated_care_order_import_start_time_window TIME;
ALTER TABLE institution ADD COLUMN integrated_care_order_import_end_time_window TIME;

-- Set a default of 8AM-9PM for IC institutions
UPDATE
  institution
SET
  integrated_care_order_import_start_time_window='8:00',
  integrated_care_order_import_end_time_window='21:00'
WHERE
  integrated_care_enabled=TRUE;

-- MHICs can now schedule an outreach in the future.
CREATE TABLE patient_order_scheduled_outreach_reason (
  patient_order_scheduled_outreach_reason_id VARCHAR PRIMARY KEY,
  description VARCHAR NOT NULL,
  display_order INTEGER NOT NULL
);

-- Currently this is just for resource followups, might be other types in the future
INSERT INTO patient_order_scheduled_outreach_reason VALUES ('RESOURCE_FOLLOWUP', 'Resource Followup', 1);
INSERT INTO patient_order_scheduled_outreach_reason VALUES ('OTHER', 'Other', 2);

-- Scheduled outreaches can be either scheduled, completed, or canceled
CREATE TABLE patient_order_scheduled_outreach_status (
  patient_order_scheduled_outreach_status_id VARCHAR PRIMARY KEY,
  description VARCHAR NOT NULL
);

INSERT INTO patient_order_scheduled_outreach_status VALUES ('SCHEDULED', 'Scheduled');
INSERT INTO patient_order_scheduled_outreach_status VALUES ('COMPLETED', 'Completed');
INSERT INTO patient_order_scheduled_outreach_status VALUES ('CANCELED', 'Canceled');

CREATE TABLE patient_order_scheduled_outreach (
  patient_order_scheduled_outreach_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  patient_order_id UUID NOT NULL REFERENCES patient_order,
  patient_order_outreach_type_id VARCHAR NOT NULL REFERENCES patient_order_outreach_type,
  patient_order_scheduled_outreach_reason_id VARCHAR NOT NULL REFERENCES patient_order_scheduled_outreach_reason,
  patient_order_scheduled_outreach_status_id VARCHAR NOT NULL REFERENCES patient_order_scheduled_outreach_status DEFAULT 'SCHEDULED',
  created_by_account_id UUID NOT NULL REFERENCES account(account_id),
  updated_by_account_id UUID REFERENCES account(account_id),
  completed_by_account_id UUID REFERENCES account(account_id),
  canceled_by_account_id UUID REFERENCES account(account_id),
  scheduled_at_date_time TIMESTAMP NOT NULL,
  message TEXT,
  completed_at TIMESTAMPTZ,
  canceled_at TIMESTAMPTZ,
  created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON patient_order_scheduled_outreach FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

-- Patient order outreaches can be tied back to their scheduled outreach (if one exists)
ALTER TABLE patient_order_outreach ADD COLUMN patient_order_scheduled_outreach_id UUID REFERENCES patient_order_scheduled_outreach;

CREATE VIEW v_patient_order_scheduled_outreach AS
SELECT
  poso.*,
  acr.first_name as created_by_account_first_name,
  acr.last_name as created_by_account_last_name,
  aco.first_name as completed_by_account_first_name,
  aco.last_name as completed_by_account_last_name
FROM patient_order_scheduled_outreach poso
LEFT JOIN account acr ON poso.created_by_account_id = acr.account_id
LEFT JOIN account aco ON poso.completed_by_account_id = aco.account_id;

-- Performance index
CREATE INDEX idx_scheduled_message_message ON scheduled_message (message_id);

COMMIT;