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

import com.cobaltplatform.api.model.db.PatientOrderScheduledFollowupStatus.PatientOrderScheduledFollowupStatusId;
import com.cobaltplatform.api.model.db.PatientOrderScheduledFollowupType.PatientOrderScheduledFollowupTypeId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class PatientOrderScheduledFollowup {
	@Nullable
	private UUID patientOrderScheduledFollowupId;
	@Nullable
	private UUID patientOrderId;
	@Nullable
	private PatientOrderScheduledFollowupStatusId patientOrderScheduledFollowupStatusId;
	@Nullable
	private PatientOrderScheduledFollowupTypeId patientOrderScheduledFollowupTypeId;
	@Nullable
	private UUID createdByAccountId;
	@Nullable
	private UUID completedByAccountId;
	@Nullable
	private UUID canceledByAccountId;
	@Nonnull
	private LocalDateTime scheduledAtDateTime;
	@Nullable
	private String comment;
	@Nullable
	private Instant completedAt;
	@Nullable
	private Instant deletedAt;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nullable
	public UUID getPatientOrderScheduledFollowupId() {
		return this.patientOrderScheduledFollowupId;
	}

	public void setPatientOrderScheduledFollowupId(@Nullable UUID patientOrderScheduledFollowupId) {
		this.patientOrderScheduledFollowupId = patientOrderScheduledFollowupId;
	}

	@Nullable
	public UUID getPatientOrderId() {
		return this.patientOrderId;
	}

	public void setPatientOrderId(@Nullable UUID patientOrderId) {
		this.patientOrderId = patientOrderId;
	}

	@Nullable
	public PatientOrderScheduledFollowupStatusId getPatientOrderScheduledFollowupStatusId() {
		return this.patientOrderScheduledFollowupStatusId;
	}

	public void setPatientOrderScheduledFollowupStatusId(@Nullable PatientOrderScheduledFollowupStatusId patientOrderScheduledFollowupStatusId) {
		this.patientOrderScheduledFollowupStatusId = patientOrderScheduledFollowupStatusId;
	}

	@Nullable
	public PatientOrderScheduledFollowupTypeId getPatientOrderScheduledFollowupTypeId() {
		return this.patientOrderScheduledFollowupTypeId;
	}

	public void setPatientOrderScheduledFollowupTypeId(@Nullable PatientOrderScheduledFollowupTypeId patientOrderScheduledFollowupTypeId) {
		this.patientOrderScheduledFollowupTypeId = patientOrderScheduledFollowupTypeId;
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
	public UUID getCanceledByAccountId() {
		return this.canceledByAccountId;
	}

	public void setCanceledByAccountId(@Nullable UUID canceledByAccountId) {
		this.canceledByAccountId = canceledByAccountId;
	}

	@Nonnull
	public LocalDateTime getScheduledAtDateTime() {
		return this.scheduledAtDateTime;
	}

	public void setScheduledAtDateTime(@Nonnull LocalDateTime scheduledAtDateTime) {
		this.scheduledAtDateTime = scheduledAtDateTime;
	}

	@Nullable
	public String getComment() {
		return this.comment;
	}

	public void setComment(@Nullable String comment) {
		this.comment = comment;
	}

	@Nullable
	public Instant getCompletedAt() {
		return this.completedAt;
	}

	public void setCompletedAt(@Nullable Instant completedAt) {
		this.completedAt = completedAt;
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
}