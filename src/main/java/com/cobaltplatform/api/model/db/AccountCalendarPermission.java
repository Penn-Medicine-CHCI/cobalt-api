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

import com.cobaltplatform.api.model.db.CalendarPermission.CalendarPermissionId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class AccountCalendarPermission {
	@Nullable
	private UUID ownerAccountId;
	@Nullable
	private UUID grantedToAccountId;
	@Nullable
	private CalendarPermissionId calendarPermissionId;
	@Nullable
	private Instant created;
	@Nullable
	private UUID createdByAccountId;
	@Nullable
	private Instant lastUpdated;
	@Nullable
	private UUID lastUpdatedByAccountId;

	@Nullable
	public UUID getOwnerAccountId() {
		return ownerAccountId;
	}

	public void setOwnerAccountId(@Nullable UUID ownerAccountId) {
		this.ownerAccountId = ownerAccountId;
	}

	@Nullable
	public UUID getGrantedToAccountId() {
		return grantedToAccountId;
	}

	public void setGrantedToAccountId(@Nullable UUID grantedToAccountId) {
		this.grantedToAccountId = grantedToAccountId;
	}

	@Nullable
	public CalendarPermissionId getCalendarPermissionId() {
		return calendarPermissionId;
	}

	public void setCalendarPermissionId(@Nullable CalendarPermissionId calendarPermissionId) {
		this.calendarPermissionId = calendarPermissionId;
	}

	@Nullable
	public Instant getCreated() {
		return created;
	}

	public void setCreated(@Nullable Instant created) {
		this.created = created;
	}

	@Nullable
	public UUID getCreatedByAccountId() {
		return createdByAccountId;
	}

	public void setCreatedByAccountId(@Nullable UUID createdByAccountId) {
		this.createdByAccountId = createdByAccountId;
	}

	@Nullable
	public Instant getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(@Nullable Instant lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	@Nullable
	public UUID getLastUpdatedByAccountId() {
		return lastUpdatedByAccountId;
	}

	public void setLastUpdatedByAccountId(@Nullable UUID lastUpdatedByAccountId) {
		this.lastUpdatedByAccountId = lastUpdatedByAccountId;
	}
}
