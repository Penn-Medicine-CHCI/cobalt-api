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
public class ActivityType {
	@Nullable
	private ActivityTypeId activityTypeId;
	@Nullable
	private String description;

	public enum ActivityTypeId {
		CONTENT,
		APPOINTMENT,
		URL,
		ACCOUNT
	}

	@Override
	public String toString() {
		return format("%s{activityTypeId=%s, description=%s}", getClass().getSimpleName(), getActivityTypeId(), getDescription());
	}

	@Nullable
	public ActivityTypeId getActivityTypeId() {
		return activityTypeId;
	}

	public void setActivityTypeId(@Nullable ActivityTypeId activityTypeId) {
		this.activityTypeId = activityTypeId;
	}

	@Nullable
	public String getDescription() {
		return description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
	}
}