BEGIN;
SELECT _v.register_patch('189-analytics', NULL, NULL);

-- Force fingerprints to UUID for faster access/consistency.
-- In practice, they are UUID strings currently.
ALTER TABLE client_device ALTER COLUMN fingerprint TYPE uuid USING fingerprint::uuid;

INSERT INTO client_device_type VALUES ('WEB_CRAWLER', 'Web Crawler');
INSERT INTO client_device_type VALUES ('UNKNOWN', 'Unknown');

-- Cobalt native analytics event types
CREATE TABLE analytics_native_event_type (
	analytics_native_event_type_id TEXT PRIMARY KEY,
  description VARCHAR NOT NULL
);

-- The start of a web "session" (lifespan of a browser tab).
-- Fired when a new browser tab opens.
-- When the browser tab is closed, the session ends (there is no corresponding SESSION_ENDED event for this currently).
-- See https://developer.mozilla.org/en-US/docs/Web/API/Window/sessionStorage for details
-- In a native app, this would be when the app first launches
-- There is no additional data associated with this event type.
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('SESSION_STARTED', 'Session Started');
-- When the user brings the browser tab into focus (unminimizes the window, switches into it from another tab)
-- See https://developer.mozilla.org/en-US/docs/Web/API/Document/visibilitychange_event for details
-- In a native app, this would be fired when the app is brought to the foreground
-- There is no additional data associated with this event type.
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('BROUGHT_TO_FOREGROUND', 'Brought to Foreground');
-- When the user brings the browser tab out of focus (minimizes the window, switches to another tab)
-- See https://developer.mozilla.org/en-US/docs/Web/API/Document/visibilitychange_event for details
-- In a native app, this would be fired when the app is sent to the background
-- There is no additional data associated with this event type.
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('SENT_TO_BACKGROUND', 'Sent to Background');
-- When the browser's URL changes, either from initial page load or SPA client-side routing
-- Additional data:
-- * url (String, the URL that was changed to)
-- * previousUrl (String, the URL that was navigated away from, may be null for initial loads/external referrals)
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('URL_CHANGED', 'URL Changed');
-- When an API call attempt fails (returns a status >= 400, terminated client-side, etc.)
-- Additional data:
-- * request (Object)
--    * method (String, e.g. GET, POST)
--    * URL (String, starts with '/')
--    * body (JSON data, if present)
-- * response (Object)
--    * status (Integer, if present)
--    * body (JSON data, if present)
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('API_CALL_ERROR', 'API Call Error');
-- When an account successfully authenticates with the backend.
-- Additional data:
-- * accountId (UUID)
-- * redirectUrl (String, if the system plans to redirect the user immediately after sign-in)
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('ACCOUNT_SIGNED_IN', 'Account Signed In');
-- When an account explicitly chooses to sign-out via the UI.
-- Additional data:
-- * accountId (UUID)
-- * source (String, indicates in what part of the system the sign-out occurred)
--    CONSENT_FORM: When viewing a consent form and revoking consent
--    PATIENT_HEADER: In the header navigation of the patient experience
--    ADMIN_HEADER: In the header navigation of the admin experience
--    MHIC_HEADER: In the header navigation of the MHIC experience
--    ACCESS_TOKEN_EXPIRED: If the backend reports that the access token is invalid/expired
--    STUDY_ONBOARDING: If necessary as part of the study onboarding process
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('ACCOUNT_SIGNED_OUT', 'Account Signed Out');
-- On the web, when the "sign-in" page is rendered.
-- There is no additional data associated with this event type.
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('PAGE_VIEW_SIGN_IN', 'Page View (Sign In)');
-- On the web, when the "sign-in with email" page is rendered.
-- There is no additional data associated with this event type.
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('PAGE_VIEW_SIGN_IN_EMAIL', 'Page View (Sign In - Email)');
-- On the web, when the "home" page is rendered (default landing screen after sign-in).
-- There is no additional data associated with this event type.
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('PAGE_VIEW_HOME', 'Page View (Home)');
-- On the web, when the Group Sessions Overview page is rendered.
-- Additional data:
-- * searchQuery: (String, if the page is filtered by a search query)
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('PAGE_VIEW_GROUP_SESSIONS', 'Page View (Group Sessions)');
-- On the web, when a Group Session Detail page is rendered.
-- Additional data:
-- * groupSessionId (UUID)
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('PAGE_VIEW_GROUP_SESSION_DETAIL', 'Page View (Group Session Detail)');
-- On the web, when a Provider List page is rendered.
-- Additional data:
-- * featureId: (String, the selected feature ID for this group of providers)
-- * supportRoleIds: (String[], the support roles associated with the feature ID)
-- * startDate: (Date in YYYY-MM-DD format, start of availability filter range)
-- * endDate: (Date in YYYY-MM-DD format, end of availability filter range)
-- * appointmentTimeIds: (String[], if the results are filtered by logical time values)
-- * institutionLocationId (UUID, if user has chosen an institution location)
-- * patientOrderId (UUID, if user is viewing providers available for a particular order)
-- * availabilitySections (Object[], the detailed day-by-day provider availability/timeslots shown to the patient)
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('PAGE_VIEW_PROVIDERS', 'Page View (Providers)');
-- On the web, when a Provider Appointment Confirmation page is rendered.
-- Additional data:
-- * providerId (UUID, the provider ID who will be booked)
-- * appointmentTypeId (UUID, the appointment type ID that will be booked)
-- * date (Date in YYYY-MM-DD format, the date of the appointment in the provider's time zone)
-- * time (Time in HH:MM format (0-23 hours), the time of the appointment in the provider's time zone)
-- * intakeAssessmentId (UUID, if there was an intake assessment taken prior to booking)
-- * patientOrderId (UUID, if this appointment is booked for a particular order)
-- * epicAppointmentFhirId (String, if this appointment is associated with an Epic FHIR slot)
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('PAGE_VIEW_PROVIDER_APPOINTMENT_CONFIRMATION', 'Page View (Provider Appointment Confirmation)');
-- On the web, when the special "Medication Prescriber" feature page is rendered.
-- There is no additional data associated with this event type.
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('PAGE_VIEW_MEDICATION_PRESCRIBER', 'Page View (Medication Prescriber)');
-- On the web, when a "topic center" page is rendered.
-- Additional data:
-- * topicCenterId (UUID)
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('PAGE_VIEW_TOPIC_CENTER', 'Page View (Topic Center)');
-- On the web, when the toplevel Resource Library page is rendered.
-- * mode: (String, one of DEFAULT, SEARCH, or RECOMMENDED based on how page is displayed)
-- * searchQuery: (String, if in SEARCH mode, when the page is filtered by a search query)
-- * totalCount: (Integer, if in SEARCH or RECOMMENDED mode, how many results exist for the mode. Not all results may be shown)
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('PAGE_VIEW_RESOURCE_LIBRARY', 'Page View (Resource Library)');
-- On the web, when a Resource Library Tag Group page is rendered.
-- Additional data:
-- * tagGroupId (String)
-- * tagIds: (String[], if the page is filtered by tag IDs)
-- * contentTypeIds: (String[], if the page is filtered by content type IDs)
-- * contentDurationIds: (String[], if the page is filtered by content duration IDs),
-- * searchQuery: (String, if the page is filtered by a search query)
-- * totalCount: (Integer, how many results exist. Not all results may be shown)
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('PAGE_VIEW_RESOURCE_LIBRARY_TAG_GROUP', 'Page View (Resource Library Tag Group)');
-- On the web, when a Resource Library Tag page is rendered.
-- Additional data:
-- * tagId (String)
-- * contentTypeIds: (String[], if the page is filtered by content type IDs)
-- * contentDurationIds: (String[], if the page is filtered by content duration IDs)
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('PAGE_VIEW_RESOURCE_LIBRARY_TAG', 'Page View (Resource Library Tag)');
-- On the web, when a Resource Library Detail page is rendered.
-- Additional data:
-- * contentId (UUID)
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('PAGE_VIEW_RESOURCE_LIBRARY_DETAIL', 'Page View (Resource Library Detail)');
-- On the web, when the "my events" page is rendered.
-- Additional data:
-- * appointmentId (UUID, if this page view is intended to highlight a specific appointment, e.g. post-booking or via email link)
-- * groupSessionReservationId (UUID, if this page view is intended to highlight a specific group session reservation, e.g. post-booking or via email link)
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('PAGE_VIEW_MY_EVENTS', 'Page View (My Events)');
-- On the web, when the "contact us" page is rendered.
-- There is no additional data associated with this event type.
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('PAGE_VIEW_CONTACT_US', 'Page View (Contact Us)');
-- On the web, when the "in crisis" page is rendered.
-- There is no additional data associated with this event type.
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('PAGE_VIEW_IN_CRISIS', 'Page View (In Crisis)');
-- On the web, when the "in crisis" overlay is rendered.
-- Additional data:
-- * source (String)
--    CONTACT_US: From the "contact us" page.
--    CALL_TO_ACTION: From a CTA recommendation for crisis resources
--    ERROR_OVERLAY: From an error message popup
--    PATIENT_HEADER: From the header navigation
--    TRIAGE_RESULTS: From patient-facing triage results, where triage indicated SI (only for IC currently)
--    SCREENING_SESSION: If an answer to a question during a screening session indicated SI (only for IC currently; patient flow triggers PAGE_VIEW_IN_CRISIS instead)
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('OVERLAY_VIEW_IN_CRISIS', 'Overlay View (In Crisis)');
-- When a piece of content is explicitly viewed (clickthrough on CTA or play button pressed for embedded media).
-- Additional data:
-- * contentId (UUID)
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('CONTENT_VIEWED', 'Content Viewed');

-- Cobalt native analytics events
CREATE TABLE analytics_native_event (
  analytics_native_event_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  analytics_native_event_type_id TEXT NOT NULL REFERENCES analytics_native_event_type,
  institution_id VARCHAR NOT NULL REFERENCES institution,
  client_device_id UUID NOT NULL REFERENCES client_device,
  account_id UUID,
  -- A session-specific identifier that follows the lifecycle defined by JS sessionStorage.
  -- For example, a session is created when a browser tab is first opened and destroyed once it's closed.
  -- Session storage will survive navigations/reloads/etc.
  -- See https://developer.mozilla.org/en-US/docs/Web/API/Window/sessionStorage for details
  -- In a native app, this would be generated and stored when the app first launches
  session_id UUID NOT NULL,
  -- Set when this event occurs in a session that is initiated by or involves clicking a link from a message
  -- represented in the message_log table (e.g. an email or SMS the system sends).
  -- This value follows the lifecycle rules governing session_id - that is, if set, it should persist for
  -- all events in the session until the session's life has ended.
  -- This can be optionally combined with referring_campaign_id.
  -- On the web, a referring message means the "a.m=[messageId]" query parameter was detected
  referring_message_id UUID REFERENCES message_log(message_id),
  -- Set when this event occurs in a session that is initiated by or involves clicking a link associated with
  -- a marketing campaign.
  -- This value follows the lifecycle rules governing session_id - that is, if set, it should persist for
  -- all events in the session until the session's life has ended.
  -- This can be optionally combined with referring_message_id.
  -- On the web, a referring campaign means the "a.c=[campaignId]" query parameter was detected
  referring_campaign_id TEXT,
  -- The client-specified timestamp for this event.
  -- Postgres does not natively store high precision times, e.g. nanos.  It's possible to receive events for the same millisecond,
  -- so use timestamp_epoch_second and timestamp_epoch_second_nano_offset if full precision is needed
  timestamp TIMESTAMPTZ NOT NULL,
  -- The client-specified timestamp represented as seconds since epoch.
  -- Combine with timestamp_epoch_second_nano_offset below for high-resolution time.
  timestamp_epoch_second INTEGER NOT NULL, -- Careful, will wrap in January 2038 when we hit 2147483647 seconds
  -- The client-specified timestamp's nanosecond offset from timestamp_epoch_second
  timestamp_epoch_second_nano_offset INTEGER NOT NULL,
  -- The value of the browser's URL bar at the time this event was captured (window.location.href)
  url TEXT,
  -- Bag of data that corresponds to the event type.
  data JSONB NOT NULL DEFAULT '{}'::JSONB,
  -- Explicitly specified by client. Example for web: "Cobalt Website". Example for native app: "Cobalt App XYZ"
  app_name TEXT NOT NULL,
  -- Explicitly specified by client. Example for web: "cbf1b9a1be984a9f61b79a05f23b19f66d533537" (git commit hash). Example for native app: "1.2.3 (4)"
  app_version TEXT NOT NULL,
  -- Client device OS name at this moment in time.  May be different from client_device record - which only shows current device state - if user updates their OS
  -- If explicitly specified by client.  Example for native app: "iOS"
  client_device_operating_system_name TEXT,
  -- Client device OS version at this moment in time.  May be different from client_device record - which only shows current device state - if user updates their OS
  -- If explicitly specified by client.  Example for native app: "17.6"
  client_device_operating_system_version TEXT,
  -- The value of window.navigator.userAgent
  user_agent TEXT,
  -- Parsed from User-Agent
  user_agent_device_family TEXT,
  -- Parsed from User-Agent
  user_agent_browser_family TEXT,
  -- Parsed from User-Agent
  user_agent_browser_version TEXT,
  -- Parsed from User-Agent
  user_agent_operating_system_name TEXT,
  -- Parsed from User-Agent
  user_agent_operating_system_version TEXT,
  -- Provided by JS window.screen object on web
	screen_color_depth INTEGER,
	-- Provided by JS window.screen object on web
	screen_pixel_depth INTEGER,
	-- Provided by JS window.screen object on web
	screen_width NUMERIC(8,2),
	-- Provided by JS window.screen object on web
	screen_height NUMERIC(8,2),
	-- Provided by JS window.screen object on web
	screen_orientation TEXT,
	-- Provided by JS window object on web
	window_device_pixel_ratio NUMERIC(4,2),
	-- Provided by JS window object on web
	window_width NUMERIC(8,2),
	-- Provided by JS window object on web
	window_height NUMERIC(8,2),
	-- Provided by document.visibilityState on web
	document_visibility_state TEXT,
  created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON analytics_native_event FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

COMMIT;