BEGIN;
SELECT _v.register_patch('194-ic-data-driven-order-assignment', NULL, NULL);

-- Should content audience filtering be exposed in the patient-facing UI?
ALTER TABLE institution ADD COLUMN content_audiences_enabled BOOLEAN NOT NULL DEFAULT TRUE;

-- What kind of a crisis handler is this (e.g. one who is notified of all crises, or only for crises in specific departments)?
CREATE TABLE patient_order_crisis_handler_type (
	patient_order_crisis_handler_type_id TEXT PRIMARY KEY,
  description VARCHAR NOT NULL
);

INSERT INTO patient_order_crisis_handler_type (patient_order_crisis_handler_type_id, description) VALUES ('ALL_ORDERS', 'All Orders');
INSERT INTO patient_order_crisis_handler_type (patient_order_crisis_handler_type_id, description) VALUES ('SPECIFIC_DEPARTMENTS_ONLY', 'Specific Departments Only');

ALTER TABLE patient_order_crisis_handler ADD COLUMN patient_order_crisis_handler_type_id TEXT NOT NULL REFERENCES patient_order_crisis_handler_type DEFAULT 'ALL_ORDERS';

-- Associate crisis handlers with specific Epic departments
CREATE TABLE patient_order_crisis_handler_epic_department (
  patient_order_crisis_handler_id UUID NOT NULL REFERENCES patient_order_crisis_handler,
  epic_department_id UUID NOT NULL REFERENCES epic_department,
  created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (patient_order_crisis_handler_id, epic_department_id)
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON patient_order_crisis_handler_epic_department FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

-- If a department-specific safety planning manager is present, prefer it to institution.integrated_care_safety_planning_manager_account_id when auto-assigning crisis orders.
-- If there are multiple safety planning managers for the same department, application code should pick one according to whatever heuristics it likes.
CREATE TABLE epic_department_safety_planning_manager (
  epic_department_id UUID NOT NULL REFERENCES epic_department,
  safety_planning_manager_account_id UUID NOT NULL REFERENCES account(account_id),
  created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (epic_department_id, safety_planning_manager_account_id)
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON epic_department_safety_planning_manager FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

-- When an order is imported into the system, allow it to be auto-assigned to an MHIC based on its department.
-- If there are multiple auto-assigned MHIC accounts for the same department, application code should pick one according to whatever heuristics it likes.
CREATE TABLE epic_department_order_auto_assigned_account (
  epic_department_id UUID NOT NULL REFERENCES epic_department,
  auto_assigned_account_id UUID NOT NULL REFERENCES account(account_id),
  created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (epic_department_id, auto_assigned_account_id)
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON epic_department_order_auto_assigned_account FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

COMMIT;