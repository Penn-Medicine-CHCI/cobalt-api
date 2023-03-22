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
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class PatientOrderScheduledScreening {
	@Nullable
	private UUID patientOrderScheduledScreeningId;
	@Nullable
	private UUID patientOrderId;
	@Nullable
	private UUID accountId;
	@Nullable
	private LocalDateTime scheduledDateTime;
	@Nullable
	private String calendarUrl;
	@Nullable
	private Boolean canceled;
	@Nullable
	private Instant canceledAt;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nullable
	public UUID getPatientOrderScheduledScreeningId() {
		return this.patientOrderScheduledScreeningId;
	}

	public void setPatientOrderScheduledScreeningId(@Nullable UUID patientOrderScheduledScreeningId) {
		this.patientOrderScheduledScreeningId = patientOrderScheduledScreeningId;
	}

	@Nullable
	public UUID getPatientOrderId() {
		return this.patientOrderId;
	}

	public void setPatientOrderId(@Nullable UUID patientOrderId) {
		this.patientOrderId = patientOrderId;
	}

	@Nullable
	public UUID getAccountId() {
		return this.accountId;
	}

	public void setAccountId(@Nullable UUID accountId) {
		this.accountId = accountId;
	}

	@Nullable
	public LocalDateTime getScheduledDateTime() {
		return this.scheduledDateTime;
	}

	public void setScheduledDateTime(@Nullable LocalDateTime scheduledDateTime) {
		this.scheduledDateTime = scheduledDateTime;
	}

	@Nullable
	public String getCalendarUrl() {
		return this.calendarUrl;
	}

	public void setCalendarUrl(@Nullable String calendarUrl) {
		this.calendarUrl = calendarUrl;
	}

	@Nullable
	public Boolean getCanceled() {
		return this.canceled;
	}

	public void setCanceled(@Nullable Boolean canceled) {
		this.canceled = canceled;
	}

	@Nullable
	public Instant getCanceledAt() {
		return this.canceledAt;
	}

	public void setCanceledAt(@Nullable Instant canceledAt) {
		this.canceledAt = canceledAt;
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