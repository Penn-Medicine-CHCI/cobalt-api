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

import com.cobaltplatform.api.model.db.PatientOrderOutreachType.PatientOrderOutreachTypeId;
import com.cobaltplatform.api.model.db.PatientOrderScheduledOutreachReason.PatientOrderScheduledOutreachReasonId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class UpdatePatientOrderScheduledOutreachRequest {
	@Nullable
	private UUID patientOrderScheduledOutreachId;
	@Nullable
	private UUID updatedByAccountId;
	@Nullable
	private PatientOrderOutreachTypeId patientOrderOutreachTypeId;
	@Nullable
	private PatientOrderScheduledOutreachReasonId patientOrderScheduledOutreachReasonId;
	@Nullable
	private LocalDate scheduledAtDate;
	@Nullable
	private LocalTime scheduledAtTime;
	@Nullable
	private String message;

	@Nullable
	public UUID getPatientOrderScheduledOutreachId() {
		return this.patientOrderScheduledOutreachId;
	}

	public void setPatientOrderScheduledOutreachId(@Nullable UUID patientOrderScheduledOutreachId) {
		this.patientOrderScheduledOutreachId = patientOrderScheduledOutreachId;
	}

	@Nullable
	public UUID getUpdatedByAccountId() {
		return this.updatedByAccountId;
	}

	public void setUpdatedByAccountId(@Nullable UUID updatedByAccountId) {
		this.updatedByAccountId = updatedByAccountId;
	}

	@Nullable
	public PatientOrderOutreachTypeId getPatientOrderOutreachTypeId() {
		return this.patientOrderOutreachTypeId;
	}

	public void setPatientOrderOutreachTypeId(@Nullable PatientOrderOutreachTypeId patientOrderOutreachTypeId) {
		this.patientOrderOutreachTypeId = patientOrderOutreachTypeId;
	}

	@Nullable
	public PatientOrderScheduledOutreachReasonId getPatientOrderScheduledOutreachReasonId() {
		return this.patientOrderScheduledOutreachReasonId;
	}

	public void setPatientOrderScheduledOutreachReasonId(@Nullable PatientOrderScheduledOutreachReasonId patientOrderScheduledOutreachReasonId) {
		this.patientOrderScheduledOutreachReasonId = patientOrderScheduledOutreachReasonId;
	}

	@Nullable
	public LocalDate getScheduledAtDate() {
		return this.scheduledAtDate;
	}

	public void setScheduledAtDate(@Nullable LocalDate scheduledAtDate) {
		this.scheduledAtDate = scheduledAtDate;
	}

	@Nullable
	public LocalTime getScheduledAtTime() {
		return this.scheduledAtTime;
	}

	public void setScheduledAtTime(@Nullable LocalTime scheduledAtTime) {
		this.scheduledAtTime = scheduledAtTime;
	}

	@Nullable
	public String getMessage() {
		return this.message;
	}

	public void setMessage(@Nullable String message) {
		this.message = message;
	}
}
