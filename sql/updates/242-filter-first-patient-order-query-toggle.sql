BEGIN;
SELECT _v.register_patch('242-filter-first-patient-order-query-toggle', NULL, NULL);

ALTER TABLE institution ADD COLUMN integrated_care_filter_first_patient_order_query_enabled BOOLEAN NOT NULL DEFAULT FALSE;

COMMIT;
