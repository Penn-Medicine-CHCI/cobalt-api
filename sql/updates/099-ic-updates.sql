BEGIN;
SELECT _v.register_patch('099-ic-updates', NULL, NULL);

-- Recreate view to include new message fields:
-- sms_to_number
-- email_to_addresses
DROP VIEW v_patient_order_scheduled_message;
CREATE VIEW v_patient_order_scheduled_message AS
SELECT
  posmg.patient_order_scheduled_message_group_id,
  posmg.patient_order_id,
  posmg.patient_order_scheduled_message_type_id,
  posm.patient_order_scheduled_message_id,
  posm.scheduled_message_id,
  posmt.description AS patient_order_scheduled_message_type_description,
  posm.created,
  posm.last_updated,
  sm.institution_id,
  sm.scheduled_message_status_id,
  sm.scheduled_by_account_id,
  sm.scheduled_message_source_id,
  sm.message_type_id,
  mt.description AS message_type_description,
  sm.scheduled_at,
  sm.time_zone,
  sm.processed_at,
  sm.canceled_at,
  sm.errored_at,
  sm.message_id,
  COALESCE (ml.message_status_id, 'ENQUEUED') as message_status_id,
  COALESCE (ms.description, 'Enqueued') as message_status_description,
  ml.processed AS sent_at,
  ml.delivered AS delivered_at,
  ml.delivery_failed AS delivery_failed_at,
  ml.delivery_failed_reason,
  ml.complaint_registered AS complaint_registered_at,
  ml.serialized_message->>'toNumber' AS sms_to_number, -- SMS number, if applicable
  ml.serialized_message->'toAddresses' AS email_to_addresses -- Email addresses, if applicable (JSON array of string)
FROM
  patient_order_scheduled_message posm,
  patient_order_scheduled_message_type posmt,
  patient_order_scheduled_message_group posmg,
  message_type mt,
  scheduled_message sm
  LEFT OUTER JOIN message_log ml ON sm.message_id=ml.message_id
  LEFT JOIN message_status ms ON ml.message_status_id=ms.message_status_id
WHERE
  posm.scheduled_message_id=sm.scheduled_message_id
  AND posmg.patient_order_scheduled_message_type_id=posmt.patient_order_scheduled_message_type_id
  AND posmg.patient_order_scheduled_message_group_id=posm.patient_order_scheduled_message_group_id
  AND posmg.deleted = FALSE
  AND sm.message_type_id=mt.message_type_id;

COMMIT;