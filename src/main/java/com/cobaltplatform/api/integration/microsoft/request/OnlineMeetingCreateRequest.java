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
public class OnlineMeetingCreateRequest {
	@Nullable
	private String userId;
	@Nullable
	private String subject;
	@Nullable
	private ZonedDateTime startDateTime;
	@Nullable
	private ZonedDateTime endDateTime;

	@Nullable
	public String getUserId() {
		return this.userId;
	}

	public void setUserId(@Nullable String userId) {
		this.userId = userId;
	}

	@Nullable
	public String getSubject() {
		return this.subject;
	}

	public void setSubject(@Nullable String subject) {
		this.subject = subject;
	}

	@Nullable
	public ZonedDateTime getStartDateTime() {
		return this.startDateTime;
	}

	public void setStartDateTime(@Nullable ZonedDateTime startDateTime) {
		this.startDateTime = startDateTime;
	}

	@Nullable
	public ZonedDateTime getEndDateTime() {
		return this.endDateTime;
	}

	public void setEndDateTime(@Nullable ZonedDateTime endDateTime) {
		this.endDateTime = endDateTime;
	}
}