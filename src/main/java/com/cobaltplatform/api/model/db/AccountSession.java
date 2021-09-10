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

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.UUID;

/**
 * @author Transmogrify LLC.
 */
public class AccountSession {
	@Nonnull
	private UUID accountSessionId;
	@Nonnull
	private UUID accountId;
	@Nonnull
	private UUID assessmentId;
	@Nonnull
	private Boolean completeFlag;
	@Nonnull
	private Boolean currentFlag;
	@Nonnull
	private Instant created;
	@Nonnull
	private Instant lastUpdated;

	@Nonnull
	public UUID getAccountSessionId() {
		return accountSessionId;
	}

	public void setAccountSessionId(@Nonnull UUID accountSessionId) {
		this.accountSessionId = accountSessionId;
	}

	@Nonnull
	public UUID getAccountId() {
		return accountId;
	}

	public void setAccountId(@Nonnull UUID accountId) {
		this.accountId = accountId;
	}

	@Nonnull
	public UUID getAssessmentId() {
		return assessmentId;
	}

	public void setAssessmentId(@Nonnull UUID assessmentId) {
		this.assessmentId = assessmentId;
	}

	@Nonnull
	public Boolean getCompleteFlag() {
		return completeFlag;
	}

	public void setCompleteFlag(@Nonnull Boolean completeFlag) {
		this.completeFlag = completeFlag;
	}

	@Nonnull
	public Boolean getCurrentFlag() {
		return currentFlag;
	}

	public void setCurrentFlag(@Nonnull Boolean currentFlag) {
		this.currentFlag = currentFlag;
	}

	@Nonnull
	public Instant getCreated() {
		return created;
	}

	public void setCreated(@Nonnull Instant created) {
		this.created = created;
	}

	@Nonnull
	public Instant getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(@Nonnull Instant lastUpdated) {
		this.lastUpdated = lastUpdated;
	}
}
