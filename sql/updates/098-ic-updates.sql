BEGIN;
SELECT _v.register_patch('098-ic-updates', NULL, NULL);

-- Adds a convenience "scheduled_at_date_time_has_passed" based on institution timezone.
-- These kinds of scheduled message groups should be treated as immutable, because messages might have already gone out
CREATE VIEW v_patient_order_scheduled_message_group AS
SELECT
	posmg.*,
  posmg.scheduled_at_date_time AT TIME ZONE i.time_zone < NOW() AS scheduled_at_date_time_has_passed
FROM
  patient_order_scheduled_message_group posmg,
  patient_order po,
  institution i
WHERE
  posmg.patient_order_id=po.patient_order_id
  AND po.institution_id=i.institution_id
  AND posmg.deleted = FALSE;

COMMIT;