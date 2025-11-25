BEGIN;
SELECT _v.register_patch('238-patient-order-study-autoassign', NULL, NULL);

-- When an order is imported into the system, allow it to be auto-assigned to an MHIC based on its study (or studies).
-- If there are multiple auto-assigned MHIC account candidates, application code should pick one according to whatever heuristics it likes.
CREATE TABLE study_order_auto_assigned_account (
  study_id UUID NOT NULL REFERENCES study,
  auto_assigned_account_id UUID NOT NULL REFERENCES account(account_id),
  created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (study_id, auto_assigned_account_id)
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON study_order_auto_assigned_account FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

COMMIT;