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
		// When an API call returns a status >= 400.
		// Additional data:
		// * statusCode (Integer, e.g. 422, 500, ...)
		// * responseBody (String)
		// * errorCode (String, if "errorCode" field is available from API response, e.g. "VALIDATION_FAILED")
		API_CALL_ERROR,
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
		// When an account explicitly chooses to sign-out via the UI.
		// Additional data:
		// * accountId (UUID)
		// * source (String, indicates in what part of the system the sign-out occurred)
		ACCOUNT_SIGNED_OUT,
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