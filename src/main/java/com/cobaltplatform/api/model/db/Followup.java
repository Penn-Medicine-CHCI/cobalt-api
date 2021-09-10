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
import java.time.LocalDate;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Followup {
	@Nullable
	private UUID followupId;
	@Nullable
	private UUID accountId;
	@Nullable
	private UUID createdByAccountId;
	@Nullable
	private UUID providerId;
	@Nullable
	private UUID appointmentReasonId;
	@Nullable
	private LocalDate followupDate;
	@Nullable
	private String comment;
	@Nullable
	private Boolean canceled;
	@Nullable
	private Instant canceledAt;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nullable
	public UUID getFollowupId() {
		return followupId;
	}

	public void setFollowupId(@Nullable UUID followupId) {
		this.followupId = followupId;
	}

	@Nullable
	public UUID getAccountId() {
		return accountId;
	}

	public void setAccountId(@Nullable UUID accountId) {
		this.accountId = accountId;
	}

	@Nullable
	public UUID getCreatedByAccountId() {
		return createdByAccountId;
	}

	public void setCreatedByAccountId(@Nullable UUID createdByAccountId) {
		this.createdByAccountId = createdByAccountId;
	}

	@Nullable
	public UUID getProviderId() {
		return providerId;
	}

	public void setProviderId(@Nullable UUID providerId) {
		this.providerId = providerId;
	}

	@Nullable
	public UUID getAppointmentReasonId() {
		return appointmentReasonId;
	}

	public void setAppointmentReasonId(@Nullable UUID appointmentReasonId) {
		this.appointmentReasonId = appointmentReasonId;
	}

	@Nullable
	public LocalDate getFollowupDate() {
		return followupDate;
	}

	public void setFollowupDate(@Nullable LocalDate followupDate) {
		this.followupDate = followupDate;
	}

	@Nullable
	public String getComment() {
		return comment;
	}

	public void setComment(@Nullable String comment) {
		this.comment = comment;
	}

	@Nullable
	public Boolean getCanceled() {
		return canceled;
	}

	public void setCanceled(@Nullable Boolean canceled) {
		this.canceled = canceled;
	}

	@Nullable
	public Instant getCanceledAt() {
		return canceledAt;
	}

	public void setCanceledAt(@Nullable Instant canceledAt) {
		this.canceledAt = canceledAt;
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
