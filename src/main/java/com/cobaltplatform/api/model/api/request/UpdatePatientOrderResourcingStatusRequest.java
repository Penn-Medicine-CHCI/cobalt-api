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

import com.cobaltplatform.api.model.db.PatientOrderResourcingStatus.PatientOrderResourcingStatusId;
import com.cobaltplatform.api.model.db.PatientOrderResourcingType.PatientOrderResourcingTypeId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.LocalDate;
import java.util.UUID;

/**
 * @author Transmogrify LLC.
 */
@NotThreadSafe
public class UpdatePatientOrderResourcingStatusRequest {
	@Nullable
	private UUID patientOrderId;
	@Nullable
	private UUID accountId;
	@Nullable
	private PatientOrderResourcingStatusId patientOrderResourcingStatusId;
	@Nullable
	private PatientOrderResourcingTypeId patientOrderResourcingTypeId;
	@Nullable
	private LocalDate resourcesSentAtDate;
	@Nullable
	private String resourcesSentAtTime; // Manually parse string from the UI
	@Nullable
	private String resourcesSentNote;

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
	public PatientOrderResourcingStatusId getPatientOrderResourcingStatusId() {
		return this.patientOrderResourcingStatusId;
	}

	public void setPatientOrderResourcingStatusId(@Nullable PatientOrderResourcingStatusId patientOrderResourcingStatusId) {
		this.patientOrderResourcingStatusId = patientOrderResourcingStatusId;
	}

	@Nullable
	public PatientOrderResourcingTypeId getPatientOrderResourcingTypeId() {
		return this.patientOrderResourcingTypeId;
	}

	public void setPatientOrderResourcingTypeId(@Nullable PatientOrderResourcingTypeId patientOrderResourcingTypeId) {
		this.patientOrderResourcingTypeId = patientOrderResourcingTypeId;
	}

	@Nullable
	public LocalDate getResourcesSentAtDate() {
		return this.resourcesSentAtDate;
	}

	public void setResourcesSentAtDate(@Nullable LocalDate resourcesSentAtDate) {
		this.resourcesSentAtDate = resourcesSentAtDate;
	}

	@Nullable
	public String getResourcesSentAtTime() {
		return this.resourcesSentAtTime;
	}

	public void setResourcesSentAtTime(@Nullable String resourcesSentAtTime) {
		this.resourcesSentAtTime = resourcesSentAtTime;
	}

	@Nullable
	public String getResourcesSentNote() {
		return this.resourcesSentNote;
	}

	public void setResourcesSentNote(@Nullable String resourcesSentNote) {
		this.resourcesSentNote = resourcesSentNote;
	}
}
