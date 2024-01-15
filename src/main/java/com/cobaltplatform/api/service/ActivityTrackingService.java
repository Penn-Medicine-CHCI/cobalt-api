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

package com.cobaltplatform.api.service;

import com.cobaltplatform.api.model.api.request.CreateActivityTrackingRequest;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.ActivityTracking;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.pyranid.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Transmogrify LLC.
 */
@Singleton
public class ActivityTrackingService {
	@Nonnull
	private final DatabaseProvider databaseProvider;
	@Nonnull
	private final Logger logger;

	@Inject
	public ActivityTrackingService(@Nonnull DatabaseProvider databaseProvider) {
		this.logger = LoggerFactory.getLogger(getClass());
		this.databaseProvider = databaseProvider;
	}

	@Nonnull
	public Optional<ActivityTracking> findActivityTrackingById(@Nullable UUID activityTrackingId) {
		if (activityTrackingId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM activity_tracking WHERE activity_tracking_id = ?",
				ActivityTracking.class, activityTrackingId);
	}

	@Nonnull
	public UUID trackActivity(@Nonnull Optional<Account> account,
														@Nonnull CreateActivityTrackingRequest request) {

		UUID activityTrackingId = UUID.randomUUID();
		UUID accountId = account.isEmpty() ? null : account.get().getAccountId();

		getDatabase().execute("INSERT INTO activity_tracking (activity_tracking_id, account_id, activity_type_id, "
						+ "activity_action_id, session_tracking_id, context) VALUES (?,?,?,?,?,CAST (? AS JSONB))",
				activityTrackingId, accountId, request.getActivityTypeId(), request.getActivityActionId(),
				request.getSessionTrackingId(), request.getContext());

		return activityTrackingId;
	}

	@Nonnull
	protected Database getDatabase() {
		return this.databaseProvider.get();
	}
}
