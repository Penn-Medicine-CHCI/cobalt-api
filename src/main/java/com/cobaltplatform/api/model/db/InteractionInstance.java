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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * @author Transmogrify LLC.
 */
@NotThreadSafe
public class InteractionInstance {
	@Nullable
	private UUID interactionInstanceId;
	@Nullable
	private UUID interactionId;
	@Nullable
	private UUID accountId;
	@Nullable
	private LocalDateTime startDateTime;
	@Nullable
	private ZoneId timeZone;
	@Nullable
	private String metadata;
	@Nullable
	private Boolean completedFlag;
	@Nullable
	private Instant completedDate;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;


	@Nullable
	public UUID getInteractionInstanceId() {
		return interactionInstanceId;
	}

	public void setInteractionInstanceId(@Nullable UUID interactionInstanceId) {
		this.interactionInstanceId = interactionInstanceId;
	}

	@Nullable
	public UUID getInteractionId() {
		return interactionId;
	}

	public void setInteractionId(@Nullable UUID interactionId) {
		this.interactionId = interactionId;
	}

	@Nullable
	public UUID getAccountId() {
		return accountId;
	}

	public void setAccountId(@Nullable UUID accountId) {
		this.accountId = accountId;
	}

	@Nullable
	public LocalDateTime getStartDateTime() {
		return startDateTime;
	}

	public void setStartDateTime(@Nullable LocalDateTime startDateTime) {
		this.startDateTime = startDateTime;
	}

	@Nullable
	public ZoneId getTimeZone() {
		return timeZone;
	}

	public void setTimeZone(@Nullable ZoneId timeZone) {
		this.timeZone = timeZone;
	}

	@Nullable
	public String getMetadata() {
		return metadata;
	}

	public void setMetadata(@Nullable String metadata) {
		this.metadata = metadata;
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

	@Nullable
	public Boolean getCompletedFlag() {
		return completedFlag;
	}

	public void setCompletedFlag(@Nullable Boolean completedFlag) {
		this.completedFlag = completedFlag;
	}

	@Nullable
	public Instant getCompletedDate() {
		return completedDate;
	}

	public void setCompletedDate(@Nullable Instant completedDate) {
		this.completedDate = completedDate;
	}
}