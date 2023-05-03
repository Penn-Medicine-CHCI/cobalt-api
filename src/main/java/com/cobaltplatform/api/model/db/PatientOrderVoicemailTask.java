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
	private UUID deletedByAccountId;
	@Nullable
	private String message;
	@Nullable
	private Boolean completed;
	@Nullable
	private Instant completedAt;
	@Nullable
	private Boolean deleted;
	@Nullable
	private Instant deletedAt;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	// Joined from v_patient_order_voicemail_task

	@Nullable
	private String createdByAccountFirstName;
	@Nullable
	private String createdByAccountLastName;
	@Nullable
	private String completedByAccountFirstName;
	@Nullable
	private String completedByAccountLastName;

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
	public UUID getDeletedByAccountId() {
		return this.deletedByAccountId;
	}

	public void setDeletedByAccountId(@Nullable UUID deletedByAccountId) {
		this.deletedByAccountId = deletedByAccountId;
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
	public Boolean getDeleted() {
		return this.deleted;
	}

	public void setDeleted(@Nullable Boolean deleted) {
		this.deleted = deleted;
	}

	@Nullable
	public Instant getDeletedAt() {
		return this.deletedAt;
	}

	public void setDeletedAt(@Nullable Instant deletedAt) {
		this.deletedAt = deletedAt;
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

	@Nullable
	public String getCreatedByAccountFirstName() {
		return this.createdByAccountFirstName;
	}

	public void setCreatedByAccountFirstName(@Nullable String createdByAccountFirstName) {
		this.createdByAccountFirstName = createdByAccountFirstName;
	}

	@Nullable
	public String getCreatedByAccountLastName() {
		return this.createdByAccountLastName;
	}

	public void setCreatedByAccountLastName(@Nullable String createdByAccountLastName) {
		this.createdByAccountLastName = createdByAccountLastName;
	}

	@Nullable
	public String getCompletedByAccountFirstName() {
		return this.completedByAccountFirstName;
	}

	public void setCompletedByAccountFirstName(@Nullable String completedByAccountFirstName) {
		this.completedByAccountFirstName = completedByAccountFirstName;
	}

	@Nullable
	public String getCompletedByAccountLastName() {
		return this.completedByAccountLastName;
	}

	public void setCompletedByAccountLastName(@Nullable String completedByAccountLastName) {
		this.completedByAccountLastName = completedByAccountLastName;
	}
}