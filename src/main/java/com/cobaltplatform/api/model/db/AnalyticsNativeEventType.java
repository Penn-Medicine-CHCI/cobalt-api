/*
 * Copyright 2021 The University of Pennsylvania and Penn Medicine
 *
 * Originally created at the University of Pennsylvania and Penn Medicine by:
 * Dr. David Asch; Dr. Lisa Bellini; Dr. Cecilia Livesey; Kelley Kugler; and Dr. Matthew Press.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cobaltplatform.api.model.db;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import static java.lang.String.format;

/**
 * @author Transmogrify LLC.
 */
@NotThreadSafe
public class AnalyticsNativeEventType {
	@Nullable
	private AnalyticsNativeEventTypeId analyticsNativeEventTypeId;
	@Nullable
	private String description;

	public enum AnalyticsNativeEventTypeId {
		// When the user brings the browser tab into focus (unminimizes the window, switches into it from another tab)
		// See https://developer.mozilla.org/en-US/docs/Web/API/Document/visibilitychange_event for details
		// In a native app, this would be fired when the app is brought to the foreground
		// There is no additional data associated with this event type.
		SESSION_STARTED,
		// Fired if, due to a browser bug regarding session storage, we have to manually "restore" a session as a workaround.
		// See https://issues.chromium.org/issues/40940701
		// There is no additional data associated with this event type.
		SESSION_RESTORED,
		// Fired periodically when the website/app is in the foreground (that is, being actively used).
		// For example, in a web browser, if the user switches to a different browser tab or minimizes the browser window, heartbeats
		// should cease until the application tab is re-focused or the window is unminimized.
		// Additional data:
		// * intervalInMilliseconds (Integer, the recurring interval duration in millis, only present if this heartbeat was fired by an automated interval as opposed to an explicit one-off)
		HEARTBEAT,
		// When the user brings the browser tab out of focus (minimizes the window, switches to another tab)
		// See https://developer.mozilla.org/en-US/docs/Web/API/Document/visibilitychange_event for details
		// In a native app, this would be fired when the app is sent to the background
		// There is no additional data associated with this event type.
		BROUGHT_TO_FOREGROUND,
		// When the user brings the browser tab out of focus (minimizes the window, switches to another tab)
		// See https://developer.mozilla.org/en-US/docs/Web/API/Document/visibilitychange_event for details
		// In a native app, this would be fired when the app is sent to the background
		// There is no additional data associated with this event type.
		SENT_TO_BACKGROUND,
		// When the browser's URL changes, either from initial page load or SPA client-side routing
		// Additional data:
		// * url (String, the URL that was changed to)
		// * previousUrl (String, the URL that was navigated away from, may be null for initial loads/external referrals)
		URL_CHANGED,
		// When an API call attempt fails (returns a status >= 400, terminated client-side, etc.)
		// Additional data:
		// * request (Object)
		//    * method (String, e.g. GET, POST)
		//    * URL (String, starts with '/')
		//    * body (JSON data, if present)
		// * response (Object)
		//    * status (Integer, if present)
		//    * body (JSON data, if present)
		API_CALL_ERROR,
		// When an account successfully authenticates with the backend.
		// There is no additional data associated with this event type.
		// * accountId (UUID)
		// * redirectUrl (String, if the system plans to redirect the user immediately after sign-in)
		ACCOUNT_SIGNED_IN,
		// When an account explicitly chooses to sign-out via the UI.
		// Additional data:
		// * accountId (UUID)
		// * source (String, indicates in what part of the system the sign-out occurred)
		//    CONSENT_FORM: When viewing a consent form and revoking consent
		//    PATIENT_HEADER: In the header navigation of the patient experience
		//    ADMIN_HEADER: In the header navigation of the admin experience
		//    MHIC_HEADER: In the header navigation of the MHIC experience
		//    ACCESS_TOKEN_EXPIRED: If the backend reports that the access token is invalid/expired
		//    STUDY_ONBOARDING: If necessary as part of the study onboarding process
		ACCOUNT_SIGNED_OUT,
		// When a MyChart handshake has succeeded (prior to linking to an account).
		// There is no additional data associated with this event type.
		MYCHART_AUTHENTICATION_SUCCEEDED,
		// When a MyChart handshake has failed (e.g. user declines to link their account to Cobalt on the MyChart side)
		// Additional data:
		// * myChartError (String with the MyChart `error` value, e.g. `access_denied`)
		// * myChartErrorDescription (String, if available, with the MyChart `error_description` value, e.g. `User refused`)
		MYCHART_AUTHENTICATION_FAILED,
		// On the web, when the "sign-in" page is rendered.
		// There is no additional data associated with this event type.
		PAGE_VIEW_SIGN_IN,
		// On the web, when the "sign-in with email" page is rendered.
		// There is no additional data associated with this event type.
		PAGE_VIEW_SIGN_IN_EMAIL,
		// On the web, when the "home" page is rendered (default landing screen after sign-in).
		// There is no additional data associated with this event type.
		PAGE_VIEW_HOME,
		// On the web, when the Group Sessions Overview page is rendered.
		// Additional data:
		// * searchQuery: (String, if the page is filtered by a search query)
		PAGE_VIEW_GROUP_SESSIONS,
		// On the web, when a Group Session Detail page is rendered.
		// Additional data:
		// * groupSessionId (UUID)
		PAGE_VIEW_GROUP_SESSION_DETAIL,
		// On the web, when a Provider List page is rendered.
		// Additional data:
		// * featureId: (String, the selected feature ID for this group of providers)
		// * supportRoleIds: (String[], the support roles associated with the feature ID)
		// * startDate: (Date in YYYY-MM-DD format, start of availability filter range)
		// * endDate: (Date in YYYY-MM-DD format, end of availability filter range)
		// * appointmentTimeIds: (String[], if the results are filtered by logical time values)
		// * institutionLocationId (UUID, if user has chosen an institution location)
		// * patientOrderId (UUID, if user is viewing providers available for a particular order)
		// * availabilitySections (Object[], the detailed day-by-day provider availability/timeslots shown to the patient)
		PAGE_VIEW_PROVIDERS,
		// On the web, when a Provider Appointment Confirmation page is rendered.
		// Additional data:
		// * providerId (UUID, the provider ID who will be booked)
		// * appointmentTypeId (UUID, the appointment type ID that will be booked)
		// * date (Date in YYYY-MM-DD format, the date of the appointment in the provider's time zone)
		// * time (Time in HH:MM format (0-23 hours), the time of the appointment in the provider's time zone)
		// * intakeAssessmentId (UUID, if there was an intake assessment taken prior to booking)
		// * patientOrderId (UUID, if this appointment is booked for a particular order)
		// * epicAppointmentFhirId (String, if this appointment is associated with an Epic FHIR slot)
		PAGE_VIEW_PROVIDER_APPOINTMENT_CONFIRMATION,
		// On the web, when the special "Medication Prescriber" feature page is rendered.
		// There is no additional data associated with this event type.
		PAGE_VIEW_MEDICATION_PRESCRIBER,
		// On the web, when a "topic center" page is rendered.
		// Additional data:
		// * topicCenterId (UUID)
		@Deprecated
		PAGE_VIEW_TOPIC_CENTER,
		// On the web, when a custom Page is rendered.
		// Additional data:
		// * pageId (UUID)
		// * siteLocationIds (String[], where this page "lives" on the site at the moment this event occurred. Values are defined by PK of site_location table, reproduced below)
		//   FEATURED_TOPIC
		//   COMMUNITY
		PAGE_VIEW_PAGE,
		// On the web, when the toplevel Resource Library page is rendered.
		// * mode: (String, one of DEFAULT, FILTERED, or RECOMMENDED based on how page is displayed)
		// * searchQuery: (String, if in FILTERED mode, when the page is filtered by a search query)
		// * tagIds: (String[], if in FILTERED mode and the page is filtered by tag IDs)
		// * contentTypeIds: (String[], if in FILTERED mode and the page is filtered by content type IDs)
		// * contentDurationIds: (String[], if in FILTERED mode and the page is filtered by content duration IDs),
		// * contentAudienceTypeIds: (String[], if in FILTERED mode and the page is filtered by content audience type IDs),
		// * totalCount: (Integer, if in FILTERED or RECOMMENDED mode, how many results exist for the mode. Not all results may be shown)
		PAGE_VIEW_RESOURCE_LIBRARY,
		// On the web, when a Resource Library Tag Group page is rendered.
		// Additional data:
		// * tagGroupId (String)
		// * tagIds: (String[], if the page is filtered by tag IDs)
		// * contentTypeIds: (String[], if the page is filtered by content type IDs)
		// * contentDurationIds: (String[], if the page is filtered by content duration IDs),
		// * contentAudienceTypeIds: (String[], if in FILTERED mode and the page is filtered by content audience type IDs),
		// * searchQuery: (String, if the page is filtered by a search query)
		// * totalCount: (Integer, how many results exist. Not all results may be shown)
		PAGE_VIEW_RESOURCE_LIBRARY_TAG_GROUP,
		// On the web, when a Resource Library Tag page is rendered.
		// Additional data:
		// * tagId (String)
		// * contentTypeIds: (String[], if the page is filtered by content type IDs)
		// * contentDurationIds: (String[], if the page is filtered by content duration IDs)
		// * contentAudienceTypeIds: (String[], if in FILTERED mode and the page is filtered by content audience type IDs),
		PAGE_VIEW_RESOURCE_LIBRARY_TAG,
		// On the web, when a Resource Library Detail page is rendered.
		// Additional data:
		// * contentId (UUID)
		PAGE_VIEW_RESOURCE_LIBRARY_DETAIL,
		// On the web, when the "my events" page is rendered.
		// Additional data:
		// * appointmentId (UUID, if this page view is intended to highlight a specific appointment, e.g. post-booking or via email link)
		// * groupSessionReservationId (UUID, if this page view is intended to highlight a specific group session reservation, e.g. post-booking or via email link)
		PAGE_VIEW_MY_EVENTS,
		// On the web, when the "contact us" page is rendered.
		// There is no additional data associated with this event type.
		PAGE_VIEW_CONTACT_US,
		// On the web, when the Resource Navigator page is rendered.
		// There is no additional data associated with this event type.
		PAGE_VIEW_RESOURCE_NAVIGATOR,
		// On the web, when the FAQ overview page is rendered.
		// There is no additional data associated with this event type.
		PAGE_VIEW_FAQS,
		// On the web, when a FAQ detail page is rendered.
		// Additional data:
		// * faqId (UUID)
		PAGE_VIEW_FAQ_DETAIL,
		// On the web, when the Institution Resources overview page is rendered.
		// There is no additional data associated with this event type.
		PAGE_VIEW_INSTITUTION_RESOURCES,
		// On the web, when an Institution Resource Group detail page is rendered.
		// Additional data:
		// * institutionResourceGroupId (UUID)
		PAGE_VIEW_INSTITUTION_RESOURCE_GROUP_DETAIL,
		// On the web, when the "in crisis" page is rendered.
		// There is no additional data associated with this event type.
		PAGE_VIEW_IN_CRISIS,
		//-- On the web, when the "courses" page is rendered.
		//-- There is no additional data associated with this event type.
		PAGE_VIEW_COURSES,
		// On the web, when the "course detail" page is rendered.
		// Additional data:
		// * courseId (UUID)
		// * courseSessionId (UUID) - optional, if a session has been started for this course
		// * mode (String, one of OVERVIEW or ADDITIONAL_RESOURCES based on how page is displayed)
		PAGE_VIEW_COURSE_DETAIL,
		// On the web, when the "course unit" page is rendered.
		// Additional data:
		// * courseUnitId (UUID)
		// * courseSessionId (UUID) - optional, if a session has been started for this course
		PAGE_VIEW_COURSE_UNIT,
		// On the web, when the MHIC "Priorities" page is rendered.
		// If a priority group tab on the page is selected, another event should be fired.
		// Additional data:
		// * priorityGroupId (String, which tab is selected)
		//    OUTREACH_REVIEW
		//    VOICEMAIL_TASK
		//    OUTREACH_FOLLOWUP_NEEDED
		//    SCHEDULED_ASSESSMENT
		//    NEED_RESOURCES
		//    SAFETY_PLANNING
		// * totalCount (Integer, how many orders are shown in the selected tab)
		PAGE_VIEW_MHIC_PRIORITIES,
		// On the web, when the MHIC "Assigned Orders" page is rendered.
		// If a filter or "sort by" option is selected, another event should be fired.
		// Additional data:
		// * patientOrderSortColumnId: (PatientOrderSortColumnId, if explicitly selected)
		// * sortDirectionId: (SortDirectionId, if explicitly selected)
		// * patientOrderOutreachStatusId: (PatientOrderOutreachStatusId, if explicitly specified)
		// * patientOrderScreeningStatusId: (PatientOrderScreeningStatusId, if explicitly specified)
		// * patientOrderScheduledScreeningScheduledDate (Date, e.g. 2024-10-11, if explicitly specified)
		// * patientOrderResourcingStatusId: (PatientOrderResourcingStatusId, if explicitly specified)
		// * PatientOrderResourceCheckInResponseStatusId: (PatientOrderResourceCheckInResponseStatusId, if explicitly specified)
		// * patientOrderAssignmentStatusId: (PatientOrderAssignmentStatusId, if explicitly specified)
		// * patientOrderDispositionId: (PatientOrderAssignmentStatusId, if explicitly specified)
		// * referringPracticeIds: (String[], if explicitly specified)
		// * patientOrderFilterFlagTypeIds: (PatientOrderFilterFlagTypeId[], if explicitly specified)
		// * pageNumber (Integer, which page of the results we're on, 0-indexed)
		// * pageSize (Integer, the maximum number of results visible per page)
		// * totalCount (Integer, how many results exist. Not all results may be shown)
		PAGE_VIEW_MHIC_ASSIGNED_ORDERS,
		// On the web, when an MHIC patient order "panel" page is rendered.
		// If a "sort by" option is selected, another event should be fired.
		// Additional data:
		// * patientOrderViewTypeId (PatientOrderViewTypeId, which panel we're looking at)
		// * patientOrderSortColumnId (PatientOrderSortColumnId, if explicitly specified)
		// * sortDirectionId (SortDirectionId, if explicitly specified)
		// * pageNumber (Integer, which page of the results we're on, 0-indexed)
		// * pageSize (Integer, the maximum number of results visible per page)
		// * totalCount (Integer, how many results exist. Not all results may be shown)
		PAGE_VIEW_MHIC_ASSIGNED_ORDERS_VIEW,
		// On the web, when the MHIC "All Orders" page is rendered.
		// If a filter or "sort by" option is selected, another event should be fired.
		// Additional data:
		// * patientOrderSortColumnId: (PatientOrderSortColumnId, if explicitly selected)
		// * sortDirectionId: (SortDirectionId, if explicitly selected)
		// * patientOrderOutreachStatusId: (PatientOrderOutreachStatusId, if explicitly specified)
		// * patientOrderScreeningStatusId: (PatientOrderScreeningStatusId, if explicitly specified)
		// * patientOrderScheduledScreeningScheduledDate (Date, e.g. 2024-10-11, if explicitly specified)
		// * patientOrderResourcingStatusId: (PatientOrderResourcingStatusId, if explicitly specified)
		// * PatientOrderResourceCheckInResponseStatusId: (PatientOrderResourceCheckInResponseStatusId, if explicitly specified)
		// * patientOrderAssignmentStatusId: (PatientOrderAssignmentStatusId, if explicitly specified)
		// * patientOrderDispositionId: (PatientOrderAssignmentStatusId, if explicitly specified)
		// * panelAccountIds: (UUID[], if explicitly specified)
		// * referringPracticeIds: (String[], if explicitly specified)
		// * patientOrderFilterFlagTypeIds: (PatientOrderFilterFlagTypeId[], if explicitly specified)
		// * pageNumber (Integer, which page of the results we're on, 0-indexed)
		// * pageSize (Integer, the maximum number of results visible per page)
		// * totalCount (Integer, how many results exist. Not all results may be shown)
		PAGE_VIEW_MHIC_ALL_ORDERS,
		// On the web, when the patient order "shelf" is summoned in any context.
		// If another section of the page is selected (e.g. Contact History or Comments), another event should be fired.
		// Additional data:
		// * patientOrderId: (UUID)
		// * sectionId: (String)
		//    ORDER_DETAILS
		//    CONTACT_HISTORY
		//    COMMENTS
		PAGE_VIEW_MHIC_ORDER_DETAIL,
		// On the web, when the MHIC "Assessment Review" page is rendered.
		// Additional data:
		// * patientOrderId: (UUID)
		PAGE_VIEW_MHIC_ORDER_ASSESSMENT_RESULTS,
		// On the web, when the MHIC "Search Results" page is rendered.
		// Additional data:
		// * searchQuery: (String)
		// * patientMrn: (String, if present)
		// * pageNumber (Integer, which page of the results we're on, 0-indexed)
		// * pageSize (Integer, the maximum number of results visible per page)
		// * totalCount (Integer, how many results exist. Not all results may be shown)
		PAGE_VIEW_MHIC_ORDER_SEARCH_RESULTS,
		// When an MHIC attempts to search over orders via autocompleter UI.
		// Additional data:
		// * searchQuery (String)
		// * patientOrderIds (UUID[], the ordered set of autocompleted patient order identifiers, or the empty list if none)
		MHIC_ORDER_AUTOCOMPLETE,
		// On the web, when the "in crisis" overlay is triggered.
		// Additional data:
		// * source (String)
		//    CONTACT_US: From the "contact us" page.
		//    CALL_TO_ACTION: From a CTA recommendation for crisis resources
		//    ERROR_OVERLAY: From an error message popup
		//    PATIENT_HEADER: From the header navigation
		//    TRIAGE_RESULTS: From patient-facing triage results, where triage indicated SI (only for IC currently)
		//    SCREENING_SESSION: If an answer to a question during a screening session indicated SI (only for IC currently; patient flow triggers PAGE_VIEW_IN_CRISIS instead)
		OVERLAY_VIEW_IN_CRISIS,
		// When a click occurs to access a topic center.
		// Additional data:
		// * topicCenterId (UUID)
		// * source (String)
		//    HOME_FEATURE: When clicked through from the homepage in the primary featured area
		//    HOME_SECONDARY_FEATURE: When clicked through from the homepage in the secondary featured area
		//    NAV_FEATURE: When clicked through from the navigation featured area
		//    NAV: When clicked through from the navigation (not in the featured area)
		@Deprecated
		CLICKTHROUGH_TOPIC_CENTER,
		// When a click occurs to access a custom Page.
		// Additional data:
		// * pageId (UUID)
		// * siteLocationId (String, the context (featured or community) in which the clickthrough occurred. Values are defined by PK of site_location table, reproduced below)
		//    FEATURED_TOPIC
		//    COMMUNITY
		// * source (String)
		//    HOME_FEATURE: When clicked through from the homepage in one of the featured areas
		//    NAV_FEATURE: When clicked through from the navigation featured area
		//    NAV: When clicked through from the navigation (not in the featured area)
		CLICKTHROUGH_PAGE,
		// When a piece of content is explicitly viewed (clickthrough on CTA or play button pressed for embedded media).
		// Additional data:
		// * contentId (UUID)
		CLICKTHROUGH_CONTENT,
		// When a click occurs to access a feature.
		// Additional data:
		// * featureId (String)
		// * source (String)
		//    HOME: When clicked through from the homepage
		//    NAV: When clicked through from the navigation
		CLICKTHROUGH_FEATURE,
		// When a click occurs on an account source to select login mode (e.g. anonymous, email/password, MyChart, ...)
		// Additional data:
		// * accountSourceId (String)
		CLICKTHROUGH_ACCOUNT_SOURCE,
		// When an MHIC clicks on the "Retake Assessment" button on the assessment review page.
		// Additional data:
		// * patientOrderId (UUID)
		CLICKTHROUGH_MHIC_RETAKE_ORDER_ASSESSMENT,
		// When an MHIC clicks on the "Export Results" button on the assessment review page.
		// Additional data:
		// * patientOrderId (UUID)
		CLICKTHROUGH_MHIC_EXPORT_ORDER_ASSESSMENT_RESULTS,
		// When an MHIC clicks on the "Review" button on the order detail page's assessment section.
		// Additional data:
		// * patientOrderId (UUID)
		CLICKTHROUGH_MHIC_ORDER_ASSESSMENT_RESULTS,
		// When a Topic Center page viewer clicks through on a group session to view its detail page.
		// Additional data:
		// * topicCenterId (UUID)
		// * groupSessionId (UUID)
		@Deprecated
		CLICKTHROUGH_TOPIC_CENTER_GROUP_SESSION,
		// When a Topic Center page viewer clicks through on a piece of content to view its detail page.
		// Additional data:
		// * topicCenterId (UUID)
		// * contentId (UUID)
		@Deprecated
		CLICKTHROUGH_TOPIC_CENTER_CONTENT,
		// When a Topic Center page viewer clicks through on a tag group to view its Resource Library page.
		// Additional data:
		// * topicCenterId (UUID)
		// * tagGroupId (String)
		@Deprecated
		CLICKTHROUGH_TOPIC_CENTER_TAG_GROUP,
		// When a Topic Center page viewer clicks through on a tag to view its Resource Library page.
		// Additional data:
		// * topicCenterId (UUID)
		// * tagId (String)
		@Deprecated
		CLICKTHROUGH_TOPIC_CENTER_TAG,
		// When a Topic Center page viewer clicks through on a link contained within a "pinboard note" component.
		// Additional data:
		// * topicCenterId (UUID)
		// * pinboardNoteId (UUID)
		// * linkUrl (String, the URL linked in the anchor tag)
		// * linkText (String, the text component of the anchor tag)
		@Deprecated
		CLICKTHROUGH_TOPIC_CENTER_PINBOARD_NOTE_LINK,
		// When a Page viewer clicks through on a group session to view its detail page.
		// Additional data:
		// * pageId (UUID)
		// * groupSessionId (UUID)
		// * siteLocationIds (String[], where this page "lives" on the site at the moment this event occurred. Values are defined by PK of site_location table, reproduced below)
		//   FEATURED_TOPIC
		//   COMMUNITY
		CLICKTHROUGH_PAGE_GROUP_SESSION,
		// When a Page viewer clicks through on a piece of content to view its detail page.
		// Additional data:
		// * pageId (UUID)
		// * contentId (UUID)
		// * siteLocationIds (String[], where this page "lives" on the site at the moment this event occurred. Values are defined by PK of site_location table, reproduced below)
		//   FEATURED_TOPIC
		//   COMMUNITY
		CLICKTHROUGH_PAGE_CONTENT,
		// When a Page viewer clicks through on a tag group to view its Resource Library page.
		// Additional data:
		// * pageId (UUID)
		// * tagGroupId (String)
		// * siteLocationIds (String[], where this page "lives" on the site at the moment this event occurred. Values are defined by PK of site_location table, reproduced below)
		//   FEATURED_TOPIC
		//   COMMUNITY
		CLICKTHROUGH_PAGE_TAG_GROUP,
		// When a Page viewer clicks through on a tag to view its Resource Library page.
		// Additional data:
		// * pageId (UUID)
		// * tagId (String)
		// * siteLocationIds (String[], where this page "lives" on the site at the moment this event occurred. Values are defined by PK of site_location table, reproduced below)
		//   FEATURED_TOPIC
		//   COMMUNITY
		CLICKTHROUGH_PAGE_TAG,
		// When a Page viewer clicks through on a link contained within any WYSIWYG page component.
		// Additional data:
		// * pageId (UUID)
		// * linkUrl (String, the URL linked in the anchor tag)
		// * linkText (String, the text component of the anchor tag)
		// * siteLocationIds (String[], where this page "lives" on the site at the moment this event occurred. Values are defined by PK of site_location table, reproduced below)
		//   FEATURED_TOPIC
		//   COMMUNITY
		CLICKTHROUGH_PAGE_LINK,
		// When the user clicks to download a file that's part of a course unit.
		// Additional data:
		// * courseUnitId (UUID)
		// * courseSessionId (UUID) - optional, if a session has been started for this course
		// * courseUnitDownloadableFileId (UUID) - the file for which click-to-download was initiated
		CLICKTHROUGH_COURSE_UNIT_DOWNLOADABLE_FILE,
		// When the user clicks to view a piece of content associated with a course.
		// Additional data:
		// * courseId (UUID)
		// * courseSessionId (UUID) - optional, if a session has been started for this course
		// * contentId (UUID) - the content that was clicked
		CLICKTHROUGH_COURSE_CONTENT,
		// When the user clicks directly on a course unit to access it, e.g. from the course detail page or from the the left nav when viewing another course unit.
		// Additional data:
		// * courseId (UUID)
		// * courseSessionId (UUID) - optional, if a session has been started for this course
		// * courseUnitId (UUID) - the course unit that was clicked
		// * source (String, one of COURSE_DETAIL or COURSE_UNIT based on where the click happened)
		// * sourceCourseUnitId (UUID) - optional, only specified if source == COURSE_UNIT. This value is the course unit the user was currently viewing
		CLICKTHROUGH_COURSE_UNIT,
		// When the user clicks the button to skip a course unit.
		// Additional data:
		// * courseId (UUID)
		// * courseSessionId (UUID) - optional, if a session has been started for this course
		// * courseUnitId (UUID) - the course unit that was skipped
		CLICKTHROUGH_COURSE_UNIT_SKIP,

		// When a video that's part of a course unit fires off an event that we listen for, e.g. 'playerReady' or 'playerPaused'.
		// Additional data:
		// * courseUnitId (UUID)
		// * courseSessionId (UUID) - optional, if a session has been started for this course
		// * videoId (UUID)
		// * eventName (String) - the name of the event, e.g. 'playerReady', which is specific to the type of video (Kaltura, YouTube, ...)
		//     If the video fails to embed/load entirely, specify the special `eventName` INITIALIZATION_ERROR regardless of what type of video it is.
		// * eventPayload (any) - optional, a payload for the event specific to the type of video (Kaltura, YouTube, ...)
		EVENT_COURSE_UNIT_VIDEO,
	}

	@Override
	public String toString() {
		return format("%s{analyticsNativeEventTypeId=%s, description=%s}", getClass().getSimpleName(),
				getAnalyticsNativeEventTypeId(), getDescription());
	}

	@Nullable
	public AnalyticsNativeEventTypeId getAnalyticsNativeEventTypeId() {
		return this.analyticsNativeEventTypeId;
	}

	public void setAnalyticsNativeEventTypeId(@Nullable AnalyticsNativeEventTypeId analyticsNativeEventTypeId) {
		this.analyticsNativeEventTypeId = analyticsNativeEventTypeId;
	}

	@Nullable
	public String getDescription() {
		return description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
	}
}