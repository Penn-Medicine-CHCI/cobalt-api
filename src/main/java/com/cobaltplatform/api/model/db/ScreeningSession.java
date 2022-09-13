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
public class ScreeningSession {
	@Nullable
	private UUID screeningSessionId;
	@Nullable
	private UUID screeningFlowVersionId;
	@Nullable
	private UUID targetAccountId;
	@Nullable
	private UUID createdByAccountId;
	@Nullable
	private Boolean completed;
	@Nullable
	private Instant completedAt;
	@Nullable
	private Boolean skipped;
	@Nullable
	private Instant skippedAt;
	@Nullable
	private Boolean crisisIndicated;
	@Nullable
	private Instant crisisIndicatedAt;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nullable
	public UUID getScreeningSessionId() {
		return this.screeningSessionId;
	}

	public void setScreeningSessionId(@Nullable UUID screeningSessionId) {
		this.screeningSessionId = screeningSessionId;
	}

	@Nullable
	public UUID getScreeningFlowVersionId() {
		return this.screeningFlowVersionId;
	}

	public void setScreeningFlowVersionId(@Nullable UUID screeningFlowVersionId) {
		this.screeningFlowVersionId = screeningFlowVersionId;
	}

	@Nullable
	public UUID getTargetAccountId() {
		return this.targetAccountId;
	}

	public void setTargetAccountId(@Nullable UUID targetAccountId) {
		this.targetAccountId = targetAccountId;
	}

	@Nullable
	public UUID getCreatedByAccountId() {
		return this.createdByAccountId;
	}

	public void setCreatedByAccountId(@Nullable UUID createdByAccountId) {
		this.createdByAccountId = createdByAccountId;
	}

	@Nullable
	public Boolean getCompleted() {
		return this.completed;
	}

	public void setCompleted(@Nullable Boolean completed) {
		this.completed = completed;
	}

	@Nullable
	public Instant getCompletedAt() {
		return this.completedAt;
	}

	public void setCompletedAt(@Nullable Instant completedAt) {
		this.completedAt = completedAt;
	}

	@Nullable
	public Boolean getSkipped() {
		return this.skipped;
	}

	public void setSkipped(@Nullable Boolean skipped) {
		this.skipped = skipped;
	}

	@Nullable
	public Instant getSkippedAt() {
		return this.skippedAt;
	}

	public void setSkippedAt(@Nullable Instant skippedAt) {
		this.skippedAt = skippedAt;
	}

	@Nullable
	public Boolean getCrisisIndicated() {
		return this.crisisIndicated;
	}

	public void setCrisisIndicated(@Nullable Boolean crisisIndicated) {
		this.crisisIndicated = crisisIndicated;
	}

	@Nullable
	public Instant getCrisisIndicatedAt() {
		return this.crisisIndicatedAt;
	}

	public void setCrisisIndicatedAt(@Nullable Instant crisisIndicatedAt) {
		this.crisisIndicatedAt = crisisIndicatedAt;
	}

	@Nullable
	public Instant getCreated() {
		return this.created;
	}

	public void setCreated(@Nullable Instant created) {
		this.created = created;
	}

	@Nullable
	public Instant getLastUpdated() {
		return this.lastUpdated;
	}

	public void setLastUpdated(@Nullable Instant lastUpdated) {
		this.lastUpdated = lastUpdated;
	}
}
