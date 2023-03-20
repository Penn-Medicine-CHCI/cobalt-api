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
import com.cobaltplatform.api.model.db.PatientOrderStatus.PatientOrderStatusId;
import com.cobaltplatform.api.model.service.PatientOrderPanelTypeId;

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
	private PatientOrderPanelTypeId patientOrderPanelTypeId;
	@Nullable
	private Set<PatientOrderStatusId> patientOrderStatusIds;
	@Nullable
	private UUID panelAccountId;
	@Nullable
	private String patientMrn;
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
	public PatientOrderPanelTypeId getPatientOrderPanelTypeId() {
		return this.patientOrderPanelTypeId;
	}

	public void setPatientOrderPanelTypeId(@Nullable PatientOrderPanelTypeId patientOrderPanelTypeId) {
		this.patientOrderPanelTypeId = patientOrderPanelTypeId;
	}

	@Nullable
	public Set<PatientOrderStatusId> getPatientOrderStatusIds() {
		return this.patientOrderStatusIds;
	}

	public void setPatientOrderStatusIds(@Nullable Set<PatientOrderStatusId> patientOrderStatusIds) {
		this.patientOrderStatusIds = patientOrderStatusIds;
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