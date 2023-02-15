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
public class PatientOrderMedication {
	@Nullable
	private UUID patientOrderMedicationId;
	@Nullable
	private UUID patientOrderId;
	@Nullable
	private String medicationId;
	@Nullable
	private String medicationIdType;
	@Nullable
	private String medicationName;
	@Nullable
	private Integer displayOrder;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nullable
	public UUID getPatientOrderMedicationId() {
		return this.patientOrderMedicationId;
	}

	public void setPatientOrderMedicationId(@Nullable UUID patientOrderMedicationId) {
		this.patientOrderMedicationId = patientOrderMedicationId;
	}

	@Nullable
	public UUID getPatientOrderId() {
		return this.patientOrderId;
	}

	public void setPatientOrderId(@Nullable UUID patientOrderId) {
		this.patientOrderId = patientOrderId;
	}

	@Nullable
	public String getMedicationId() {
		return this.medicationId;
	}

	public void setMedicationId(@Nullable String medicationId) {
		this.medicationId = medicationId;
	}

	@Nullable
	public String getMedicationIdType() {
		return this.medicationIdType;
	}

	public void setMedicationIdType(@Nullable String medicationIdType) {
		this.medicationIdType = medicationIdType;
	}

	@Nullable
	public String getMedicationName() {
		return this.medicationName;
	}

	public void setMedicationName(@Nullable String medicationName) {
		this.medicationName = medicationName;
	}

	@Nullable
	public Integer getDisplayOrder() {
		return this.displayOrder;
	}

	public void setDisplayOrder(@Nullable Integer displayOrder) {
		this.displayOrder = displayOrder;
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
