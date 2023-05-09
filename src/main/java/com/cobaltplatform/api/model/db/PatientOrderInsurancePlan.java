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

import com.cobaltplatform.api.model.db.PatientOrderInsurancePlanType.PatientOrderInsurancePlanTypeId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class PatientOrderInsurancePlan {
	@Nullable
	private UUID patientOrderInsurancePlanId;
	@Nullable
	private UUID patientOrderInsurancePayorId;
	@Nullable
	private PatientOrderInsurancePlanTypeId patientOrderInsurancePlanTypeId;
	@Nullable
	private String name;
	@Nullable
	private Boolean accepted;
	@Nullable
	private Integer displayOrder;
	@Nullable
	private Boolean deleted;

	@Nullable
	public UUID getPatientOrderInsurancePlanId() {
		return this.patientOrderInsurancePlanId;
	}

	public void setPatientOrderInsurancePlanId(@Nullable UUID patientOrderInsurancePlanId) {
		this.patientOrderInsurancePlanId = patientOrderInsurancePlanId;
	}

	@Nullable
	public UUID getPatientOrderInsurancePayorId() {
		return this.patientOrderInsurancePayorId;
	}

	public void setPatientOrderInsurancePayorId(@Nullable UUID patientOrderInsurancePayorId) {
		this.patientOrderInsurancePayorId = patientOrderInsurancePayorId;
	}

	@Nullable
	public PatientOrderInsurancePlanTypeId getPatientOrderInsurancePlanTypeId() {
		return this.patientOrderInsurancePlanTypeId;
	}

	public void setPatientOrderInsurancePlanTypeId(@Nullable PatientOrderInsurancePlanTypeId patientOrderInsurancePlanTypeId) {
		this.patientOrderInsurancePlanTypeId = patientOrderInsurancePlanTypeId;
	}

	@Nullable
	public String getName() {
		return this.name;
	}

	public void setName(@Nullable String name) {
		this.name = name;
	}

	@Nullable
	public Boolean getAccepted() {
		return this.accepted;
	}

	public void setAccepted(@Nullable Boolean accepted) {
		this.accepted = accepted;
	}

	@Nullable
	public Integer getDisplayOrder() {
		return this.displayOrder;
	}

	public void setDisplayOrder(@Nullable Integer displayOrder) {
		this.displayOrder = displayOrder;
	}

	@Nullable
	public Boolean getDeleted() {
		return this.deleted;
	}

	public void setDeleted(@Nullable Boolean deleted) {
		this.deleted = deleted;
	}
}