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
public class PatientOrderStatus {
	@Nullable
	private PatientOrderStatusId patientOrderStatusId;
	@Nullable
	private String description;
	@Nullable
	private Integer displayOrder;

	public enum PatientOrderStatusId {
		PENDING, // Unassigned
		NEEDS_ASSESSMENT, // Assigned, but none of the below apply.  Also note, we are in this state if unscheduled but "screening in progress"
		SAFETY_PLANNING, // Screening completed, most severe level of care type triage is SAFETY_PLANNING
		SPECIALTY_CARE, // Screening completed, most severe level of care type triage is SPECIALTY
		SUBCLINICAL, // Screening completed, most severe level of care type triage is SUBCLINICAL
		BHP // Screening completed, most severe level of care type triage is COLLABORATIVE.  Patient or MHIC can schedule with a provider
	}

	@Override
	public String toString() {
		return format("%s{patientOrderStatusId=%s, description=%s}", getClass().getSimpleName(),
				getPatientOrderStatusId(), getDescription());
	}

	@Nullable
	public PatientOrderStatusId getPatientOrderStatusId() {
		return this.patientOrderStatusId;
	}

	public void setPatientOrderStatusId(@Nullable PatientOrderStatusId patientOrderStatusId) {
		this.patientOrderStatusId = patientOrderStatusId;
	}

	@Nullable
	public String getDescription() {
		return this.description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
	}

	@Nullable
	public Integer getDisplayOrder() {
		return this.displayOrder;
	}

	public void setDisplayOrder(@Nullable Integer displayOrder) {
		this.displayOrder = displayOrder;
	}
}