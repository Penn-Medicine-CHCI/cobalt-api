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

package com.cobaltplatform.api.model.api.request;

import com.cobaltplatform.api.model.db.PatientOrderScheduledOutreachReason.PatientOrderScheduledFollowupContactTypeId;
import com.cobaltplatform.api.model.db.PatientOrderScheduledFollowupType.PatientOrderScheduledFollowupTypeId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.LocalDate;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class UpdatePatientOrderScheduledFollowupRequest {
	@Nullable
	private UUID patientOrderScheduledFollowupId;
	@Nullable
	private UUID patientOrderId;
	@Nullable
	private UUID updatedByAccountId;
	@Nullable
	private PatientOrderScheduledFollowupTypeId patientOrderScheduledFollowupTypeId;
	@Nullable
	private PatientOrderScheduledFollowupContactTypeId patientOrderScheduledFollowupContactTypeId;
	@Nullable
	private LocalDate scheduledAtDate;
	@Nullable
	private LocalDate scheduledAtTime;
	@Nullable
	private String comment;

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
	public UUID getUpdatedByAccountId() {
		return this.updatedByAccountId;
	}

	public void setUpdatedByAccountId(@Nullable UUID updatedByAccountId) {
		this.updatedByAccountId = updatedByAccountId;
	}

	@Nullable
	public PatientOrderScheduledFollowupTypeId getPatientOrderScheduledFollowupTypeId() {
		return this.patientOrderScheduledFollowupTypeId;
	}

	public void setPatientOrderScheduledFollowupTypeId(@Nullable PatientOrderScheduledFollowupTypeId patientOrderScheduledFollowupTypeId) {
		this.patientOrderScheduledFollowupTypeId = patientOrderScheduledFollowupTypeId;
	}

	@Nullable
	public PatientOrderScheduledFollowupContactTypeId getPatientOrderScheduledFollowupContactTypeId() {
		return this.patientOrderScheduledFollowupContactTypeId;
	}

	public void setPatientOrderScheduledFollowupContactTypeId(@Nullable PatientOrderScheduledFollowupContactTypeId patientOrderScheduledFollowupContactTypeId) {
		this.patientOrderScheduledFollowupContactTypeId = patientOrderScheduledFollowupContactTypeId;
	}

	@Nullable
	public LocalDate getScheduledAtDate() {
		return this.scheduledAtDate;
	}

	public void setScheduledAtDate(@Nullable LocalDate scheduledAtDate) {
		this.scheduledAtDate = scheduledAtDate;
	}

	@Nullable
	public LocalDate getScheduledAtTime() {
		return this.scheduledAtTime;
	}

	public void setScheduledAtTime(@Nullable LocalDate scheduledAtTime) {
		this.scheduledAtTime = scheduledAtTime;
	}

	@Nullable
	public String getComment() {
		return this.comment;
	}

	public void setComment(@Nullable String comment) {
		this.comment = comment;
	}
}
