BEGIN;
SELECT _v.register_patch('289-care-encounter-follow-up-without-attendance', ARRAY['262-care-navigator'], NULL);

-- Navigator outreach may be scheduled before a meeting or after a missed,
-- canceled, or rescheduled appointment. Attendance changes must not cancel it.
-- Closing, canceling, or deleting the encounter still cancels pending messages.
DROP TRIGGER cancel_pending_messages_for_invalidated_care_attendance ON appointment;
DROP FUNCTION cancel_pending_messages_for_invalidated_care_attendance();

COMMIT;
