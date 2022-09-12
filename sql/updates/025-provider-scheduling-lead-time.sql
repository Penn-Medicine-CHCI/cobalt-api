BEGIN;
SELECT _v.register_patch('025-provider-scheduling-lead-time', NULL, NULL);

ALTER TABLE provider ADD COLUMN scheduling_lead_time_in_hours INTEGER NOT NULL DEFAULT 48;

COMMIT;