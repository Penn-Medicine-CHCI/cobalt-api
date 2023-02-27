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

import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.PatientOrderImportType.PatientOrderImportTypeId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class CreatePatientOrderImportRequest {
	@Nullable
	private InstitutionId institutionId;
	@Nullable
	private PatientOrderImportTypeId patientOrderImportTypeId;
	@Nullable
	private UUID accountId;
	@Nullable
	private String csvContent;
	@Nullable
	private Boolean automaticallyAssignToPanelAccounts;

	@Nullable
	public InstitutionId getInstitutionId() {
		return this.institutionId;
	}

	public void setInstitutionId(@Nullable InstitutionId institutionId) {
		this.institutionId = institutionId;
	}

	@Nullable
	public PatientOrderImportTypeId getPatientOrderImportTypeId() {
		return this.patientOrderImportTypeId;
	}

	public void setPatientOrderImportTypeId(@Nullable PatientOrderImportTypeId patientOrderImportTypeId) {
		this.patientOrderImportTypeId = patientOrderImportTypeId;
	}

	@Nullable
	public UUID getAccountId() {
		return this.accountId;
	}

	public void setAccountId(@Nullable UUID accountId) {
		this.accountId = accountId;
	}

	@Nullable
	public String getCsvContent() {
		return this.csvContent;
	}

	public void setCsvContent(@Nullable String csvContent) {
		this.csvContent = csvContent;
	}

	@Nullable
	public Boolean getAutomaticallyAssignToPanelAccounts() {
		return this.automaticallyAssignToPanelAccounts;
	}

	public void setAutomaticallyAssignToPanelAccounts(@Nullable Boolean automaticallyAssignToPanelAccounts) {
		this.automaticallyAssignToPanelAccounts = automaticallyAssignToPanelAccounts;
	}
}
