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
public class PatientOrderDiagnosis {
	@Nullable
	private UUID patientOrderDiagnosisId;
	@Nullable
	private UUID patientImportId;
	@Nullable
	private String diagnosisId;
	@Nullable
	private String diagnosisIdType;
	@Nullable
	private String diagnosisName;
	@Nullable
	private Integer displayOrder;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nullable
	public UUID getPatientOrderDiagnosisId() {
		return this.patientOrderDiagnosisId;
	}

	public void setPatientOrderDiagnosisId(@Nullable UUID patientOrderDiagnosisId) {
		this.patientOrderDiagnosisId = patientOrderDiagnosisId;
	}

	@Nullable
	public UUID getPatientImportId() {
		return this.patientImportId;
	}

	public void setPatientImportId(@Nullable UUID patientImportId) {
		this.patientImportId = patientImportId;
	}

	@Nullable
	public String getDiagnosisId() {
		return this.diagnosisId;
	}

	public void setDiagnosisId(@Nullable String diagnosisId) {
		this.diagnosisId = diagnosisId;
	}

	@Nullable
	public String getDiagnosisIdType() {
		return this.diagnosisIdType;
	}

	public void setDiagnosisIdType(@Nullable String diagnosisIdType) {
		this.diagnosisIdType = diagnosisIdType;
	}

	@Nullable
	public String getDiagnosisName() {
		return this.diagnosisName;
	}

	public void setDiagnosisName(@Nullable String diagnosisName) {
		this.diagnosisName = diagnosisName;
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
