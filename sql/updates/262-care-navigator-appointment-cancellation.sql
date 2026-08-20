BEGIN;
SELECT _v.register_patch('262-care-navigator-appointment-cancellation', ARRAY['261-care-encounters'], NULL);

ALTER TABLE appointment
ADD COLUMN cancellation_reason TEXT;

COMMIT;
