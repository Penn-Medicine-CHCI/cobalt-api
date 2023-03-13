BEGIN;
SELECT _v.register_patch('063-patient-order-screening-session', NULL, NULL);

-- Alternative to target_account_id, you can tie a screening session to a patient order
-- which might or might not have an account tied to it.
-- This permits an MHIC to perform a screening without relying on the target account existing -
-- for example, it's possible the MHIC could screen someone who never logs in to Cobalt
ALTER TABLE screening_session ADD COLUMN patient_order_id UUID REFERENCES patient_order;

UPDATE
  screening_session ss
SET
  patient_order_id = poss.patient_order_id
FROM
  patient_order_screening_session poss
WHERE
  ss.screening_session_id = poss.screening_session_id;

DROP TABLE patient_order_screening_session;

COMMIT;