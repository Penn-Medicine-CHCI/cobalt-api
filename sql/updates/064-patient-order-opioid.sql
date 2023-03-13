BEGIN;
SELECT _v.register_patch('064-patient-order-opioid', NULL, NULL);

INSERT INTO patient_order_focus_type VALUES ('OPIOID_USE_DISORDER', 'Opioid Use Disorder');

COMMIT;