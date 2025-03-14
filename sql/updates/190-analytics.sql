BEGIN;
SELECT _v.register_patch('190-analytics', NULL, NULL);

-- Allows us to persist arbitrarily-large sets of claims for the MyChart OAuth flow.
-- This is necessary because OAuth state values have small size limits -
-- if we were to bundle up a large JSON object in OAuth state, the flow would error out.
-- This table enables us to provide a fixed-size state that only includes a myChartAuthenticationClaimsId value,
-- which points to a record with the serialized "real" claims which can include as much data as we like.
CREATE TABLE mychart_authentication_claims (
	mychart_authentication_claims_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	institution_id TEXT NOT NULL REFERENCES institution,
	claims JSONB NOT NULL DEFAULT '{}'::JSONB,
	consumed_at TIMESTAMPTZ, -- Set when these claims been "consumed" by an assertion from MyChart
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON mychart_authentication_claims FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

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
-- Additional data:
-- * referringUrl (String, in a webapp this is the value of document.referrer, if available)
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('SESSION_STARTED', 'Session Started');
-- Fired if, due to a browser bug regarding session storage, we have to manually "restore" a session as a workaround.
-- See https://issues.chromium.org/issues/40940701
-- There is no additional data associated with this event type.
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('SESSION_RESTORED', 'Session Restored');
-- Fired periodically when the website/app is in the foreground (that is, being actively used).
-- For example, in a web browser, if the user switches to a different browser tab or minimizes the browser window, heartbeats
-- should cease until the application tab is re-focused or the window is unminimized.
-- Additional data:
-- * intervalInMilliseconds (Integer, the recurring interval duration in millis, only present if this heartbeat was fired by an automated interval as opposed to an explicit one-off)
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('HEARTBEAT', 'Heartbeat');
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
-- When a MyChart handshake has succeeded (prior to linking to an account).
-- There is no additional data associated with this event type.
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('MYCHART_AUTHENTICATION_SUCCEEDED', 'MyChart Authentication Succeeded');
-- When a MyChart handshake has failed (e.g. user declines to link their account to Cobalt on the MyChart side)
-- Additional data:
-- * myChartError (String with the MyChart `error` value, e.g. `access_denied`)
-- * myChartErrorDescription (String, if available, with the MyChart `error_description` value, e.g. `User refused`)
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('MYCHART_AUTHENTICATION_FAILED', 'MyChart Authentication Failed');
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
-- On the web, when the Resource Navigator page is rendered.
-- There is no additional data associated with this event type.
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('PAGE_VIEW_RESOURCE_NAVIGATOR', 'Page View (Resource Navigator)');
-- On the web, when the FAQ overview page is rendered.
-- There is no additional data associated with this event type.
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('PAGE_VIEW_FAQS', 'Page View (FAQs)');
-- On the web, when a FAQ detail page is rendered.
-- Additional data:
-- * faqId (UUID)
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('PAGE_VIEW_FAQ_DETAIL', 'Page View (FAQ Detail)');
-- On the web, when the Institution Resources overview page is rendered.
-- There is no additional data associated with this event type.
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('PAGE_VIEW_INSTITUTION_RESOURCES', 'Page View (Institution Resources)');
-- On the web, when an Institution Resource Group detail page is rendered.
-- Additional data:
-- * institutionResourceGroupId (UUID)
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('PAGE_VIEW_INSTITUTION_RESOURCE_GROUP_DETAIL', 'Page View (Institution Resource Detail)');
-- On the web, when the "in crisis" page is rendered.
-- There is no additional data associated with this event type.
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('PAGE_VIEW_IN_CRISIS', 'Page View (In Crisis)');
-- On the web, when the MHIC "Priorities" page is rendered.
-- If a priority group tab on the page is selected, another event should be fired.
-- Additional data:
-- * priorityGroupId (String, which tab is selected)
--    OUTREACH_REVIEW
--    VOICEMAIL_TASK
--    OUTREACH_FOLLOWUP_NEEDED
--    SCHEDULED_ASSESSMENT
--    NEED_RESOURCES
--    SAFETY_PLANNING
-- * totalCount (Integer, how many orders are shown in the selected tab)
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('PAGE_VIEW_MHIC_PRIORITIES', 'Page View (MHIC Priorities)');
-- On the web, when the MHIC "Assigned Orders" page is rendered.
-- If a filter or "sort by" option is selected, another event should be fired.
-- Additional data:
-- * patientOrderSortColumnId: (PatientOrderSortColumnId, if explicitly selected)
-- * sortDirectionId: (SortDirectionId, if explicitly selected)
-- * patientOrderOutreachStatusId: (PatientOrderOutreachStatusId, if explicitly specified)
-- * patientOrderScreeningStatusId: (PatientOrderScreeningStatusId, if explicitly specified)
-- * patientOrderScheduledScreeningScheduledDate (Date, e.g. 2024-10-11, if explicitly specified)
-- * patientOrderResourcingStatusId: (PatientOrderResourcingStatusId, if explicitly specified)
-- * PatientOrderResourceCheckInResponseStatusId: (PatientOrderResourceCheckInResponseStatusId, if explicitly specified)
-- * patientOrderAssignmentStatusId: (PatientOrderAssignmentStatusId, if explicitly specified)
-- * patientOrderDispositionId: (PatientOrderAssignmentStatusId, if explicitly specified)
-- * referringPracticeIds: (String[], if explicitly specified)
-- * patientOrderFilterFlagTypeIds: (PatientOrderFilterFlagTypeId[], if explicitly specified)
-- * pageNumber (Integer, which page of the results we're on, 0-indexed)
-- * pageSize (Integer, the maximum number of results visible per page)
-- * totalCount (Integer, how many results exist. Not all results may be shown)
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('PAGE_VIEW_MHIC_ASSIGNED_ORDERS', 'Page View (MHIC Assigned Orders)');
-- On the web, when an MHIC patient order "panel" page is rendered.
-- If a "sort by" option is selected, another event should be fired.
-- Additional data:
-- * patientOrderViewTypeId (PatientOrderViewTypeId, which panel we're looking at)
-- * patientOrderSortColumnId (PatientOrderSortColumnId, if explicitly specified)
-- * sortDirectionId (SortDirectionId, if explicitly specified)
-- * pageNumber (Integer, which page of the results we're on, 0-indexed)
-- * pageSize (Integer, the maximum number of results visible per page)
-- * totalCount (Integer, how many results exist. Not all results may be shown)
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('PAGE_VIEW_MHIC_ASSIGNED_ORDERS_VIEW', 'Page View (MHIC Assigned Orders View)');
-- On the web, when the MHIC "All Orders" page is rendered.
-- If a filter or "sort by" option is selected, another event should be fired.
-- Additional data:
-- * patientOrderSortColumnId: (PatientOrderSortColumnId, if explicitly selected)
-- * sortDirectionId: (SortDirectionId, if explicitly selected)
-- * patientOrderOutreachStatusId: (PatientOrderOutreachStatusId, if explicitly specified)
-- * patientOrderScreeningStatusId: (PatientOrderScreeningStatusId, if explicitly specified)
-- * patientOrderScheduledScreeningScheduledDate (Date, e.g. 2024-10-11, if explicitly specified)
-- * patientOrderResourcingStatusId: (PatientOrderResourcingStatusId, if explicitly specified)
-- * PatientOrderResourceCheckInResponseStatusId: (PatientOrderResourceCheckInResponseStatusId, if explicitly specified)
-- * patientOrderAssignmentStatusId: (PatientOrderAssignmentStatusId, if explicitly specified)
-- * patientOrderDispositionId: (PatientOrderAssignmentStatusId, if explicitly specified)
-- * panelAccountIds: (UUID[], if explicitly specified)
-- * referringPracticeIds: (String[], if explicitly specified)
-- * patientOrderFilterFlagTypeIds: (PatientOrderFilterFlagTypeId[], if explicitly specified)
-- * pageNumber (Integer, which page of the results we're on, 0-indexed)
-- * pageSize (Integer, the maximum number of results visible per page)
-- * totalCount (Integer, how many results exist. Not all results may be shown)
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('PAGE_VIEW_MHIC_ALL_ORDERS', 'Page View (MHIC All Orders)');
-- On the web, when the patient order "shelf" is summoned in any context.
-- If another section of the page is selected (e.g. Contact History or Comments), another event should be fired.
-- Additional data:
-- * patientOrderId: (UUID)
-- * sectionId: (String)
--    ORDER_DETAILS
--    CONTACT_HISTORY
--    COMMENTS
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('PAGE_VIEW_MHIC_ORDER_DETAIL', 'Page View (MHIC Order Detail)');
-- On the web, when the MHIC "Assessment Review" page is rendered.
-- Additional data:
-- * patientOrderId: (UUID)
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('PAGE_VIEW_MHIC_ORDER_ASSESSMENT_RESULTS', 'Page View (MHIC Order Assessment Results)');
-- On the web, when the MHIC "Search Results" page is rendered.
-- Additional data:
-- * searchQuery: (String)
-- * patientMrn: (String, if present)
-- * pageNumber (Integer, which page of the results we're on, 0-indexed)
-- * pageSize (Integer, the maximum number of results visible per page)
-- * totalCount (Integer, how many results exist. Not all results may be shown)
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('PAGE_VIEW_MHIC_ORDER_SEARCH_RESULTS', 'Page View (MHIC Order Search Results)');
-- When an MHIC attempts to search over orders via autocompleter UI.
-- Additional data:
-- * searchQuery (String)
-- * patientOrderIds (UUID[], the ordered set of autocompleted patient order identifiers, or the empty list if none)
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('MHIC_ORDER_AUTOCOMPLETE', 'MHIC Order Autocomplete');
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
-- When a click occurs to access a topic center.
-- Additional data:
-- * topicCenterId (UUID)
-- * source (String)
--    HOME_FEATURE: When clicked through from the homepage in the primary featured area
--    HOME_SECONDARY_FEATURE: When clicked through from the homepage in the secondary featured area
--    NAV_FEATURE: When clicked through from the navigation featured area
--    NAV: When clicked through from the navigation (not in the featured area)
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('CLICKTHROUGH_TOPIC_CENTER', 'Clickthrough (Topic Center)');
-- When a piece of content is explicitly viewed (clickthrough on CTA or play button pressed for embedded media).
-- Additional data:
-- * contentId (UUID)
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('CLICKTHROUGH_CONTENT', 'Clickthrough (Content)');
-- When a click occurs to access a feature.
-- Additional data:
-- * featureId (String)
-- * source (String)
--    HOME: When clicked through from the homepage
--    NAV: When clicked through from the navigation
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('CLICKTHROUGH_FEATURE', 'Clickthrough (Feature)');
-- When a click occurs on an account source to select login mode (e.g. anonymous, email/password, MyChart, ...)
-- Additional data:
-- * accountSourceId (String)
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('CLICKTHROUGH_ACCOUNT_SOURCE', 'Clickthrough (Account Source)');
-- When an MHIC clicks on the "Retake Assessment" button.
-- Additional data:
-- * patientOrderId (UUID)
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('CLICKTHROUGH_MHIC_RETAKE_ORDER_ASSESSMENT', 'Clickthrough (MHIC Retake Order Assessment)');
-- When an MHIC clicks on the "Export Results" button on the assessment review page.
-- Additional data:
-- * patientOrderId (UUID)
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('CLICKTHROUGH_MHIC_EXPORT_ORDER_ASSESSMENT_RESULTS', 'Clickthrough (MHIC Export Order Assessment Results)');
-- When an MHIC clicks on the "Review" button on the order detail page's assessment section.
-- Additional data:
-- * patientOrderId (UUID)
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('CLICKTHROUGH_MHIC_ORDER_ASSESSMENT_RESULTS', 'Clickthrough (MHIC Order Assessment Results)');

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
  -- On the web, a referring campaign means the "a.c=[campaign]" query parameter was detected
  referring_campaign TEXT,
  -- The client-specified timestamp for this event.
  -- Postgres does not natively store high precision times, e.g. nanos.  It's possible to receive events for the same millisecond,
  -- so use timestamp_epoch_second and timestamp_epoch_second_nano_offset if full precision is needed
  timestamp TIMESTAMPTZ NOT NULL,
  -- The client-specified timestamp represented as seconds since epoch.
  -- Combine with timestamp_epoch_second_nano_offset below for high-resolution time.
  timestamp_epoch_second INTEGER NOT NULL, -- Careful, will wrap in January 2038 when we hit 2147483647 seconds
  -- The client-specified timestamp's nanosecond offset from timestamp_epoch_second
  timestamp_epoch_second_nano_offset INTEGER NOT NULL,
  -- For webapps, the value of the browser's URL bar at the time this event was captured (window.location.href)
  webapp_url TEXT,
  -- Bag of data that corresponds to the event type.
  data JSONB NOT NULL DEFAULT '{}'::JSONB,
  -- Explicitly specified by client. Example for web: "Cobalt Webapp". Example for native app: "Cobalt App XYZ"
  app_name TEXT NOT NULL,
  -- Explicitly specified by client. Example for web: "cbf1b9a1be984a9f61b79a05f23b19f66d533537" (git commit hash). Example for native app: "1.2.3 (4)"
  app_version TEXT NOT NULL,
  -- Client device OS name at this moment in time.  May be different from client_device record - which only shows current device state - if user updates their OS
  -- If explicitly specified by client.  Example for native app: "iOS"
  client_device_operating_system_name TEXT,
  -- Client device OS version at this moment in time.  May be different from client_device record - which only shows current device state - if user updates their OS
  -- If explicitly specified by client.  Example for native app: "17.6"
  client_device_operating_system_version TEXT,
  -- Client device's supported locales at this moment in time, e.g. ["en-US","en","fr-CA"]
  client_device_supported_locales JSONB NOT NULL DEFAULT '[]'::JSONB,
  -- Client device's preferred locale at this moment in time, e.g. 'en-US'
  client_device_locale TEXT,
  -- Client device's timezone at this moment in time, e.g. 'America/New_York'
  client_device_time_zone TEXT,
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
	-- Provided by navigator.maxTouchPoints on web
	navigator_max_touch_points INTEGER,
	-- Provided by document.visibilityState on web
	document_visibility_state TEXT,
  created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON analytics_native_event FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

-- Add some performance indices
CREATE INDEX analytics_native_event_event_type_id_idx ON analytics_native_event(analytics_native_event_type_id);
CREATE INDEX analytics_native_event_account_id_idx ON analytics_native_event(account_id);
CREATE INDEX analytics_native_event_referring_message_id_idx ON analytics_native_event(referring_message_id);
CREATE INDEX analytics_native_event_referring_campaign_idx ON analytics_native_event(referring_campaign);
CREATE INDEX analytics_native_event_timestamp_asc_idx ON analytics_native_event(timestamp ASC);
CREATE INDEX analytics_native_event_timestamp_desc_idx ON analytics_native_event(timestamp DESC);
CREATE INDEX analytics_native_event_timestamp_epoch_second_asc_idx ON analytics_native_event(timestamp_epoch_second ASC);
CREATE INDEX analytics_native_event_timestamp_epoch_second_desc_idx ON analytics_native_event(timestamp_epoch_second DESC);

-- Add some performance indices
CREATE INDEX footprint_event_group_account_idx ON footprint_event_group(account_id);
CREATE INDEX footprint_event_group_type_idx ON footprint_event_group(footprint_event_group_type_id);
CREATE INDEX footprint_event_group_created_asc_idx ON footprint_event_group(created ASC);
CREATE INDEX footprint_event_group_created_desc_idx ON footprint_event_group(created DESC);

CREATE INDEX footprint_event_group_idx ON footprint_event(footprint_event_group_id);

COMMIT;