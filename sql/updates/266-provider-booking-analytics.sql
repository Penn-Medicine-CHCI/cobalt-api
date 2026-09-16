BEGIN;
SELECT _v.register_patch('266-provider-booking-analytics', ARRAY['261-provider-booking-database'], NULL);

-- Provider Booking V2 funnel events. Payloads intentionally contain only
-- identifiers and controlled enum values; never contact details, free text, or
-- screening answers.
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES
	('CLICKTHROUGH_PROVIDER_SEARCH_RESULT', 'Clickthrough (Provider Search Result)'),
	('EVENT_PROVIDER_APPOINTMENT_SELECTION_VIEWED', 'Event (Provider Appointment Selection Viewed)'),
	('EVENT_PROVIDER_APPOINTMENT_SELECTED', 'Event (Provider Appointment Selected)'),
	('PAGE_VIEW_PROVIDER_BOOKING_COMPLETE', 'Page View (Provider Booking Complete)');

COMMIT;
