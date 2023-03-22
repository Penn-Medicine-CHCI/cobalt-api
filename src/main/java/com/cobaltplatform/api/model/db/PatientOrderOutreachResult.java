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

import com.cobaltplatform.api.model.db.PatientOrderOutreachResultStatus.PatientOrderOutreachResultStatusId;
import com.cobaltplatform.api.model.db.PatientOrderOutreachResultType.PatientOrderOutreachResultTypeId;
import com.cobaltplatform.api.model.db.PatientOrderOutreachType.PatientOrderOutreachTypeId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.UUID;

/**
 * @author Transmogrify LLC.
 */
@NotThreadSafe
public class PatientOrderOutreachResult {
	@Nullable
	private UUID patientOrderOutreachResultId;
	@Nullable
	private PatientOrderOutreachTypeId patientOrderOutreachTypeId;
	@Nullable
	private PatientOrderOutreachResultTypeId patientOrderOutreachResultTypeId;
	@Nullable
	private Integer displayOrder;

	// Joined in by v_patient_order_outreach_result

	@Nullable
	private String patientOrderOutreachTypeDescription;
	@Nullable
	private String patientOrderOutreachResultTypeDescription;
	@Nullable
	private PatientOrderOutreachResultStatusId patientOrderOutreachResultStatusId;
	@Nullable
	private String patientOrderOutreachResultStatusDescription;

	@Nullable
	public UUID getPatientOrderOutreachResultId() {
		return this.patientOrderOutreachResultId;
	}

	public void setPatientOrderOutreachResultId(@Nullable UUID patientOrderOutreachResultId) {
		this.patientOrderOutreachResultId = patientOrderOutreachResultId;
	}

	@Nullable
	public PatientOrderOutreachTypeId getPatientOrderOutreachTypeId() {
		return this.patientOrderOutreachTypeId;
	}

	public void setPatientOrderOutreachTypeId(@Nullable PatientOrderOutreachTypeId patientOrderOutreachTypeId) {
		this.patientOrderOutreachTypeId = patientOrderOutreachTypeId;
	}

	@Nullable
	public PatientOrderOutreachResultTypeId getPatientOrderOutreachResultTypeId() {
		return this.patientOrderOutreachResultTypeId;
	}

	public void setPatientOrderOutreachResultTypeId(@Nullable PatientOrderOutreachResultTypeId patientOrderOutreachResultTypeId) {
		this.patientOrderOutreachResultTypeId = patientOrderOutreachResultTypeId;
	}

	@Nullable
	public Integer getDisplayOrder() {
		return this.displayOrder;
	}

	public void setDisplayOrder(@Nullable Integer displayOrder) {
		this.displayOrder = displayOrder;
	}

	@Nullable
	public String getPatientOrderOutreachTypeDescription() {
		return this.patientOrderOutreachTypeDescription;
	}

	public void setPatientOrderOutreachTypeDescription(@Nullable String patientOrderOutreachTypeDescription) {
		this.patientOrderOutreachTypeDescription = patientOrderOutreachTypeDescription;
	}

	@Nullable
	public String getPatientOrderOutreachResultTypeDescription() {
		return this.patientOrderOutreachResultTypeDescription;
	}

	public void setPatientOrderOutreachResultTypeDescription(@Nullable String patientOrderOutreachResultTypeDescription) {
		this.patientOrderOutreachResultTypeDescription = patientOrderOutreachResultTypeDescription;
	}

	@Nullable
	public PatientOrderOutreachResultStatusId getPatientOrderOutreachResultStatusId() {
		return this.patientOrderOutreachResultStatusId;
	}

	public void setPatientOrderOutreachResultStatusId(@Nullable PatientOrderOutreachResultStatusId patientOrderOutreachResultStatusId) {
		this.patientOrderOutreachResultStatusId = patientOrderOutreachResultStatusId;
	}

	@Nullable
	public String getPatientOrderOutreachResultStatusDescription() {
		return this.patientOrderOutreachResultStatusDescription;
	}

	public void setPatientOrderOutreachResultStatusDescription(@Nullable String patientOrderOutreachResultStatusDescription) {
		this.patientOrderOutreachResultStatusDescription = patientOrderOutreachResultStatusDescription;
	}
}