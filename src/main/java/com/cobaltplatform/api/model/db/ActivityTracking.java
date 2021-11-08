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

import java.time.Instant;
import java.util.UUID;


/**
 * @author Transmogrify LLC.
 */
@NotThreadSafe
public class ActivityTracking {
	@Nullable
	private UUID activityTrackingId;
	@Nullable
	private UUID accountId;
	@Nullable
	private ActivityType.ActivityTypeId activityTypeId;
	@Nullable
	private ActivityAction.ActivityActionId activityActionId;
	@Nullable
	private UUID activityKey;
	@Nullable
	private UUID sessionTrackingId;
	@Nullable
	private String context;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nullable
	public UUID getActivityTrackingId() {
		return activityTrackingId;
	}

	public void setActivityTrackingId(@Nullable UUID activityTrackingId) {
		this.activityTrackingId = activityTrackingId;
	}

	@Nullable
	public UUID getAccountId() {
		return accountId;
	}

	public void setAccountId(@Nullable UUID accountId) {
		this.accountId = accountId;
	}

	@Nullable
	public ActivityType.ActivityTypeId getActivityTypeId() {
		return activityTypeId;
	}

	public void setActivityTypeId(@Nullable ActivityType.ActivityTypeId activityTypeId) {
		this.activityTypeId = activityTypeId;
	}

	@Nullable
	public ActivityAction.ActivityActionId getActivityActionId() {
		return activityActionId;
	}

	public void setActivityActionId(@Nullable ActivityAction.ActivityActionId activityActionId) {
		this.activityActionId = activityActionId;
	}

	@Nullable
	public UUID getActivityKey() {
		return activityKey;
	}

	public void setActivityKey(@Nullable UUID activityKey) {
		this.activityKey = activityKey;
	}

	@Nullable
	public UUID getSessionTrackingId() {
		return sessionTrackingId;
	}

	public void setSessionTrackingId(@Nullable UUID sessionTrackingId) {
		this.sessionTrackingId = sessionTrackingId;
	}

	@Nullable
	public String getContext() {
		return context;
	}

	public void setContext(@Nullable String context) {
		this.context = context;
	}

	@Nullable
	public Instant getCreated() {
		return created;
	}

	public void setCreated(@Nullable Instant created) {
		this.created = created;
	}

	@Nullable
	public Instant getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(@Nullable Instant lastUpdated) {
		this.lastUpdated = lastUpdated;
	}
}