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

package com.cobaltplatform.api.model.api.response;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.cobaltplatform.api.model.db.ActivityAction;
import com.cobaltplatform.api.model.db.ActivityTracking;
import com.cobaltplatform.api.model.db.ActivityType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class ActivityTrackingApiResponse {
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

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface ActivityTrackingApiResponseFactory {
		@Nonnull
		ActivityTrackingApiResponse create(@Nonnull ActivityTracking activityTracking);
	}

	@AssistedInject
	public ActivityTrackingApiResponse(@Assisted @Nonnull ActivityTracking activityTracking) {
		requireNonNull(activityTracking);

		this.activityTrackingId = activityTracking.getActivityTrackingId();
		this.accountId = activityTracking.getAccountId();
		this.activityTypeId = activityTracking.getActivityTypeId();
		this.activityActionId = activityTracking.getActivityActionId();
		this.activityKey = activityTracking.getActivityKey();
	}

	@Nullable
	public UUID getActivityTrackingId() {
		return activityTrackingId;
	}

	@Nullable
	public UUID getAccountId() {
		return accountId;
	}

	@Nullable
	public ActivityType.ActivityTypeId getActivityTypeId() {
		return activityTypeId;
	}

	@Nullable
	public ActivityAction.ActivityActionId getActivityActionId() {
		return activityActionId;
	}

	@Nullable
	public UUID getActivityKey() {
		return activityKey;
	}
}