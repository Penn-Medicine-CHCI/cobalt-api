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
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class PatientOrderReferralReason {
	@Nullable
	private PatientOrderReferralReasonId patientOrderReferralReasonId;
	@Nullable
	private String description;

	public enum PatientOrderReferralReasonId {
		UNKNOWN,
		ADJUSTMENT_DISORDERS,
		ALCOHOL_MISUSE_OR_ADDICTION,
		ANXIETY_SYMPTOMS,
		DRUG_MISUSE_OR_ADDICTION,
		FEEDING_OR_EATING_DISORDERS,
		IMPULSE_CONTROL_AND_CONDUCT_DISORDERS,
		MOOD_OR_DEPRESSION_SYMPTOMS,
		NEUROCOGNITIVE_DISORDERS,
		NEURODEVELOPMENTAL_DISORDERS,
		OBSESSIVE_COMPULSIVE_DISORDERS,
		OPIOID_USE_DISORDER,
		PERSONALITY_DISORDERS,
		PSYCHOPHARMACOLOGY_MANAGEMENT,
		PSYCHOSIS,
		PTSD_OR_TRAUMA_RELATED_SYMPTOMS,
		SEXUAL_INTEREST_OR_DISFUNCTION_OR_GENDER_DYSPHORIA,
		SLEEP_WAKE_CYCLE_DISORDERS,
		TREATMENT_ENGAGEMENT,
		SELF
	}

	@Override
	public String toString() {
		return format("%s{patientOrderReferralReasonId=%s, description=%s}", getClass().getSimpleName(),
				getPatientOrderReferralReasonId(), getDescription());
	}

	@Nullable
	public PatientOrderReferralReasonId getPatientOrderReferralReasonId() {
		return this.patientOrderReferralReasonId;
	}

	public void setPatientOrderReferralReasonId(@Nullable PatientOrderReferralReasonId patientOrderReferralReasonId) {
		this.patientOrderReferralReasonId = patientOrderReferralReasonId;
	}

	@Nullable
	public String getDescription() {
		return this.description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
	}
}
