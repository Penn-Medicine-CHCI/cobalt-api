BEGIN;
SELECT _v.register_patch('066-patient-order-search', NULL, NULL);

-- These trigram indices permit efficient autocomplete-like searching, e.g.
--
--   SELECT *
--   FROM patient_order
--   WHERE patient_first_name ILIKE '%mike%';

CREATE INDEX patient_order_patient_first_name_trgm_idx
ON patient_order
USING GIN (patient_first_name gin_trgm_ops);

CREATE INDEX patient_order_patient_last_name_trgm_idx
ON patient_order
USING GIN (patient_last_name gin_trgm_ops);

CREATE INDEX patient_order_patient_mrn_trgm_idx
ON patient_order
USING GIN (patient_mrn gin_trgm_ops);

COMMIT;