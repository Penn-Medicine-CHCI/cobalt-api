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
		// * responseBody (String, the URL that was navigated away from, may be null for initial loads/external referrals)
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
		ACCOUNT_SIGNED_OUT,
		// On the web, when the "sign-in" page is rendered.
		// There is no additional data associated with this event type.
		PAGE_VIEW_SIGN_IN,
		// On the web, when the "sign-in with email" page is rendered.
		// There is no additional data associated with this event type.
		PAGE_VIEW_SIGN_IN_EMAIL,
		// On the web, when the "home" page is rendered (default landing screen after sign-in).
		// There is no additional data associated with this event type.
		PAGE_VIEW_HOME,
		// On the web, when a "topic center" page is rendered.
		// Additional data:
		// * topicCenterId (UUID)
		PAGE_VIEW_TOPIC_CENTER,
		// On the web, when the toplevel Resource Library page is rendered.
		// * mode: (String, one of DEFAULT, SEARCH, or RECOMMENDED based on how page is displayed)
		// * searchQuery: (String, if in SEARCH mode, when the page is filtered by a search query)
		// * totalCount: (Integer, if in SEARCH or RECOMMENDED mode, how many results exist for the mode. Not all results may be shown)
		PAGE_VIEW_RESOURCE_LIBRARY,
		// On the web, when a Resource Library Tag Group page is rendered.
		// Additional data:
		// * tagGroupId (String)
		// * tagIds: (String[], if the page is filtered by tag IDs)
		// * contentTypeIds: (String[], if the page is filtered by content type IDs)
		// * contentDurationIds: (String[], if the page is filtered by content duration IDs),
		// * searchQuery: (String, if the page is filtered by a search query)
		// * totalCount: (Integer, how many results exist. Not all results may be shown)
		PAGE_VIEW_RESOURCE_LIBRARY_TAG_GROUP,
		// On the web, when a Resource Library Tag page is rendered.
		// Additional data:
		// * tagId (String)
		// * contentTypeIds: (String[], if the page is filtered by content type IDs)
		// * contentDurationIds: (String[], if the page is filtered by content duration IDs)
		PAGE_VIEW_RESOURCE_LIBRARY_TAG,
		// On the web, when a Resource Library Detail page is rendered.
		// Additional data:
		// * contentId (UUID)
		PAGE_VIEW_RESOURCE_LIBRARY_DETAIL,
		// When a piece of content is explicitly viewed (clickthrough on CTA or play button pressed for embedded media).
		// Additional data:
		// * contentId (UUID)
		CONTENT_VIEWED,
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