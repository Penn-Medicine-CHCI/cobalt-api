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

package com.cobaltplatform.api.integration.microsoft.request;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.time.ZonedDateTime;

/**
 * See https://learn.microsoft.com/en-us/azure/active-directory/develop/v2-oauth2-auth-code-flow.
 *
 * @author Transmogrify, LLC.
 */
@Immutable
public class SubscriptionCreateRequest {
	@Nullable
	private String changeType;
	@Nullable
	private String notificationUrl;
	@Nullable
	private String resource;
	@Nullable
	private ZonedDateTime expirationDateTime;
	@Nullable
	private String clientState;

	// TODO: additional fields

	@Nullable
	public String getChangeType() {
		return this.changeType;
	}

	public void setChangeType(@Nullable String changeType) {
		this.changeType = changeType;
	}

	@Nullable
	public String getNotificationUrl() {
		return this.notificationUrl;
	}

	public void setNotificationUrl(@Nullable String notificationUrl) {
		this.notificationUrl = notificationUrl;
	}

	@Nullable
	public String getResource() {
		return this.resource;
	}

	public void setResource(@Nullable String resource) {
		this.resource = resource;
	}

	@Nullable
	public ZonedDateTime getExpirationDateTime() {
		return this.expirationDateTime;
	}

	public void setExpirationDateTime(@Nullable ZonedDateTime expirationDateTime) {
		this.expirationDateTime = expirationDateTime;
	}

	@Nullable
	public String getClientState() {
		return this.clientState;
	}

	public void setClientState(@Nullable String clientState) {
		this.clientState = clientState;
	}
}