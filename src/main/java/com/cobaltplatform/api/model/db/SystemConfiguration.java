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
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class SystemConfiguration {
	@Nullable
	private Integer databaseVersion;
	@Nullable
	private Instant createdTimestamp;
	@Nullable
	private UUID createdAccountId;
	@Nullable
	private Instant lastUpdatedTimestamp;
	@Nullable
	private UUID lastUpdatedAccountId;

	@Nullable
	public Integer getDatabaseVersion() {
		return databaseVersion;
	}

	public void setDatabaseVersion(@Nullable Integer databaseVersion) {
		this.databaseVersion = databaseVersion;
	}

	@Nullable
	public Instant getCreatedTimestamp() {
		return createdTimestamp;
	}

	public void setCreatedTimestamp(@Nullable Instant createdTimestamp) {
		this.createdTimestamp = createdTimestamp;
	}

	@Nullable
	public UUID getCreatedAccountId() {
		return createdAccountId;
	}

	public void setCreatedAccountId(@Nullable UUID createdAccountId) {
		this.createdAccountId = createdAccountId;
	}

	@Nullable
	public Instant getLastUpdatedTimestamp() {
		return lastUpdatedTimestamp;
	}

	public void setLastUpdatedTimestamp(@Nullable Instant lastUpdatedTimestamp) {
		this.lastUpdatedTimestamp = lastUpdatedTimestamp;
	}

	@Nullable
	public UUID getLastUpdatedAccountId() {
		return lastUpdatedAccountId;
	}

	public void setLastUpdatedAccountId(@Nullable UUID lastUpdatedAccountId) {
		this.lastUpdatedAccountId = lastUpdatedAccountId;
	}
}
