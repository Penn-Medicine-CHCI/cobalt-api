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

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Clinic {
	@Nullable
	private UUID clinicId;
	@Nullable
	private InstitutionId institutionId;
	@Nullable
	private UUID intakeAssessmentId;
	@Nullable
	private String description;
	@Nullable
	private String treatmentDescription;
	@Nullable
	private Boolean showIntakeAssessmentPrompt;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nullable
	public UUID getClinicId() {
		return clinicId;
	}

	public void setClinicId(@Nullable UUID clinicId) {
		this.clinicId = clinicId;
	}

	@Nullable
	public InstitutionId getInstitutionId() {
		return institutionId;
	}

	public void setInstitutionId(@Nullable InstitutionId institutionId) {
		this.institutionId = institutionId;
	}

	@Nullable
	public UUID getIntakeAssessmentId() {
		return intakeAssessmentId;
	}

	public void setIntakeAssessmentId(@Nullable UUID intakeAssessmentId) {
		this.intakeAssessmentId = intakeAssessmentId;
	}

	@Nullable
	public String getDescription() {
		return description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
	}

	@Nullable
	public String getTreatmentDescription() {
		return treatmentDescription;
	}

	public void setTreatmentDescription(@Nullable String treatmentDescription) {
		this.treatmentDescription = treatmentDescription;
	}

	@Nullable
	public Boolean getShowIntakeAssessmentPrompt() {
		return showIntakeAssessmentPrompt;
	}

	public void setShowIntakeAssessmentPrompt(@Nullable Boolean showIntakeAssessmentPrompt) {
		this.showIntakeAssessmentPrompt = showIntakeAssessmentPrompt;
	}

	@Nullable
	public Instant getCreated() {
		return created;
	}

	public void setCreated(@Nullable Instant created) {
		this.created = created;
	}

	@Nullable
	public Instant getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(@Nullable Instant lastUpdated) {
		this.lastUpdated = lastUpdated;
	}
}