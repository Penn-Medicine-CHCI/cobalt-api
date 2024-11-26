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

import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.PatientOrderCrisisHandlerType.PatientOrderCrisisHandlerTypeId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class PatientOrderCrisisHandler {
	@Nullable
	private UUID patientOrderCrisisHandlerId;
	@Nullable
	private PatientOrderCrisisHandlerTypeId patientOrderCrisisHandlerTypeId;
	@Nullable
	private InstitutionId institutionId;
	@Nullable
	private String name;
	@Nullable
	private String phoneNumber;
	@Nullable
	private Boolean enabled;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nullable
	public UUID getPatientOrderCrisisHandlerId() {
		return this.patientOrderCrisisHandlerId;
	}

	public void setPatientOrderCrisisHandlerId(@Nullable UUID patientOrderCrisisHandlerId) {
		this.patientOrderCrisisHandlerId = patientOrderCrisisHandlerId;
	}

	@Nullable
	public PatientOrderCrisisHandlerTypeId getPatientOrderCrisisHandlerTypeId() {
		return this.patientOrderCrisisHandlerTypeId;
	}

	public void setPatientOrderCrisisHandlerTypeId(@Nullable PatientOrderCrisisHandlerTypeId patientOrderCrisisHandlerTypeId) {
		this.patientOrderCrisisHandlerTypeId = patientOrderCrisisHandlerTypeId;
	}

	@Nullable
	public InstitutionId getInstitutionId() {
		return this.institutionId;
	}

	public void setInstitutionId(@Nullable InstitutionId institutionId) {
		this.institutionId = institutionId;
	}

	@Nullable
	public String getPhoneNumber() {
		return this.phoneNumber;
	}

	public void setPhoneNumber(@Nullable String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	@Nullable
	public String getName() {
		return this.name;
	}

	public void setName(@Nullable String name) {
		this.name = name;
	}

	@Nullable
	public Boolean getEnabled() {
		return this.enabled;
	}

	public void setEnabled(@Nullable Boolean enabled) {
		this.enabled = enabled;
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
