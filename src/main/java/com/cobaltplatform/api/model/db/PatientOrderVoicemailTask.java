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
public class PatientOrderVoicemailTask {
	@Nullable
	private UUID patientOrderVoicemailTaskId;
	@Nullable
	private UUID patientOrderId;
	@Nullable
	private UUID createdByAccountId;
	@Nullable
	private UUID completedByAccountId;
	@Nullable
	private String message;
	@Nullable
	private Boolean completed;
	@Nullable
	private Instant completedAt;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nullable
	public UUID getPatientOrderVoicemailTaskId() {
		return this.patientOrderVoicemailTaskId;
	}

	public void setPatientOrderVoicemailTaskId(@Nullable UUID patientOrderVoicemailTaskId) {
		this.patientOrderVoicemailTaskId = patientOrderVoicemailTaskId;
	}

	@Nullable
	public UUID getPatientOrderId() {
		return this.patientOrderId;
	}

	public void setPatientOrderId(@Nullable UUID patientOrderId) {
		this.patientOrderId = patientOrderId;
	}

	@Nullable
	public UUID getCreatedByAccountId() {
		return this.createdByAccountId;
	}

	public void setCreatedByAccountId(@Nullable UUID createdByAccountId) {
		this.createdByAccountId = createdByAccountId;
	}

	@Nullable
	public UUID getCompletedByAccountId() {
		return this.completedByAccountId;
	}

	public void setCompletedByAccountId(@Nullable UUID completedByAccountId) {
		this.completedByAccountId = completedByAccountId;
	}

	@Nullable
	public String getMessage() {
		return this.message;
	}

	public void setMessage(@Nullable String message) {
		this.message = message;
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