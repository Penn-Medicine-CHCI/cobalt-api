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

import static java.lang.String.format;

/**
 * @author Transmogrify LLC.
 */
@NotThreadSafe
public class ScreeningType {
	@Nullable
	private ScreeningTypeId screeningTypeId;
	@Nullable
	private String description;
	@Nullable
	private Integer overallScoreMaximum;

	public enum ScreeningTypeId {
		CUSTOM,
		GAD_2,
		GAD_7,
		PHQ_4,
		PHQ_8,
		PHQ_9,
		WHO_5,
		PC_PTSD_5,
		CAGE_ALCOHOL,
		TICS,
		ISI,
		ASRM,
		C_SSRS_3,
		C_SSRS_8,
		DAST_10,
		BPI,
		AUDIT_C,
		MBI_9,
		@Deprecated IC_INTRO,  // Replaced by IC_INTAKE
		@Deprecated IC_INTRO_CONDITIONS, // Replaced by IC_PATIENT_SELF_REPORTING
		@Deprecated IC_INTRO_SYMPTOMS, // Replaced by IC_PATIENT_SELF_REPORTING
		IC_INTAKE,
		IC_PATIENT_SELF_REPORTING,
		IC_DRUG_USE_FREQUENCY,
		IC_DRUG_USE_OPIOID,
		BPI_1,
		PRIME_5,
		PSYCHOSIS,
		SUDS_THERMOMETER,
		UCLA_LONELINESS,
		CONTENT_SATISFACTION,
		SCREENING_READINESS,
		CARD_SORT
	}

	@Override
	public String toString() {
		return format("%s{screeningTypeId=%s, description=%s}", getClass().getSimpleName(), getScreeningTypeId().name(), getDescription());
	}

	@Nullable
	public ScreeningTypeId getScreeningTypeId() {
		return this.screeningTypeId;
	}

	public void setScreeningTypeId(@Nullable ScreeningTypeId screeningTypeId) {
		this.screeningTypeId = screeningTypeId;
	}

	@Nullable
	public String getDescription() {
		return this.description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
	}

	@Nullable
	public Integer getOverallScoreMaximum() {
		return this.overallScoreMaximum;
	}

	public void setOverallScoreMaximum(@Nullable Integer overallScoreMaximum) {
		this.overallScoreMaximum = overallScoreMaximum;
	}
}