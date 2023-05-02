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
import com.cobaltplatform.api.model.db.PatientOrderDisposition.PatientOrderDispositionId;
import com.cobaltplatform.api.model.db.PatientOrderTriageStatus.PatientOrderTriageStatusId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Set;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class FindPatientOrdersRequest {
	@Nullable
	private InstitutionId institutionId;
	@Nullable
	private PatientOrderDispositionId patientOrderDispositionId;
	@Nullable
	private Set<PatientOrderTriageStatusId> patientOrderTriageStatusIds;
	@Nullable
	private UUID panelAccountId;
	@Nullable
	private String patientMrn;
	@Nullable
	private String searchQuery;
	@Nullable
	private Integer pageNumber;
	@Nullable
	private Integer pageSize;

	@Nullable
	public InstitutionId getInstitutionId() {
		return this.institutionId;
	}

	public void setInstitutionId(@Nullable InstitutionId institutionId) {
		this.institutionId = institutionId;
	}

	@Nullable
	public PatientOrderDispositionId getPatientOrderDispositionId() {
		return this.patientOrderDispositionId;
	}

	public void setPatientOrderDispositionId(@Nullable PatientOrderDispositionId patientOrderDispositionId) {
		this.patientOrderDispositionId = patientOrderDispositionId;
	}

	@Nullable
	public Set<PatientOrderTriageStatusId> getPatientOrderTriageStatusIds() {
		return this.patientOrderTriageStatusIds;
	}

	public void setPatientOrderTriageStatusIds(@Nullable Set<PatientOrderTriageStatusId> patientOrderTriageStatusIds) {
		this.patientOrderTriageStatusIds = patientOrderTriageStatusIds;
	}

	@Nullable
	public UUID getPanelAccountId() {
		return this.panelAccountId;
	}

	public void setPanelAccountId(@Nullable UUID panelAccountId) {
		this.panelAccountId = panelAccountId;
	}

	@Nullable
	public String getPatientMrn() {
		return this.patientMrn;
	}

	public void setPatientMrn(@Nullable String patientMrn) {
		this.patientMrn = patientMrn;
	}

	@Nullable
	public String getSearchQuery() {
		return this.searchQuery;
	}

	public void setSearchQuery(@Nullable String searchQuery) {
		this.searchQuery = searchQuery;
	}

	@Nullable
	public Integer getPageNumber() {
		return this.pageNumber;
	}

	public void setPageNumber(@Nullable Integer pageNumber) {
		this.pageNumber = pageNumber;
	}

	@Nullable
	public Integer getPageSize() {
		return this.pageSize;
	}

	public void setPageSize(@Nullable Integer pageSize) {
		this.pageSize = pageSize;
	}
}