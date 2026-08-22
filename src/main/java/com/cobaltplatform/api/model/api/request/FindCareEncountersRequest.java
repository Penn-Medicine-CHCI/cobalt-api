/*
 * Copyright 2021 The University of Pennsylvania and Penn Medicine
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package com.cobaltplatform.api.model.api.request;

import com.cobaltplatform.api.model.db.CareEncounterStatus.CareEncounterStatusId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.service.SortDirectionId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.LocalDate;
import java.util.UUID;

@NotThreadSafe
public class FindCareEncountersRequest {
	@Nullable
	private InstitutionId institutionId;
	@Nullable
	private Integer pageNumber;
	@Nullable
	private Integer pageSize;
	@Nullable
	private LocalDate startDate;
	@Nullable
	private LocalDate endDate;
	@Nullable
	private String searchQuery;
	@Nullable
	private CareEncounterStatusId careEncounterStatusId;
	@Nonnull
	private CareEncounterAssignmentScopeId careEncounterAssignmentScopeId = CareEncounterAssignmentScopeId.ALL;
	@Nullable
	private UUID careNavigatorAccountId;
	@Nullable
	private CareEncounterSortColumnId careEncounterSortColumnId;
	@Nullable
	private SortDirectionId sortDirectionId;

	public enum CareEncounterSortColumnId {
		APPOINTMENT_DATE,
		PATIENT_NAME,
		STATUS,
		CREATED,
		LAST_UPDATED
	}

	public enum CareEncounterAssignmentScopeId {
		ALL,
		SELF,
		UNASSIGNED
	}

	@Nullable
	public InstitutionId getInstitutionId() {
		return this.institutionId;
	}

	public void setInstitutionId(@Nullable InstitutionId institutionId) {
		this.institutionId = institutionId;
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

	@Nullable
	public LocalDate getStartDate() {
		return this.startDate;
	}

	public void setStartDate(@Nullable LocalDate startDate) {
		this.startDate = startDate;
	}

	@Nullable
	public LocalDate getEndDate() {
		return this.endDate;
	}

	public void setEndDate(@Nullable LocalDate endDate) {
		this.endDate = endDate;
	}

	@Nullable
	public String getSearchQuery() {
		return this.searchQuery;
	}

	public void setSearchQuery(@Nullable String searchQuery) {
		this.searchQuery = searchQuery;
	}

	@Nullable
	public CareEncounterStatusId getCareEncounterStatusId() {
		return this.careEncounterStatusId;
	}

	public void setCareEncounterStatusId(@Nullable CareEncounterStatusId careEncounterStatusId) {
		this.careEncounterStatusId = careEncounterStatusId;
	}

	@Nonnull
	public CareEncounterAssignmentScopeId getCareEncounterAssignmentScopeId() {
		return this.careEncounterAssignmentScopeId;
	}

	public void setCareEncounterAssignmentScopeId(@Nullable CareEncounterAssignmentScopeId careEncounterAssignmentScopeId) {
		this.careEncounterAssignmentScopeId = careEncounterAssignmentScopeId == null
				? CareEncounterAssignmentScopeId.ALL
				: careEncounterAssignmentScopeId;
	}

	@Nullable
	public UUID getCareNavigatorAccountId() {
		return this.careNavigatorAccountId;
	}

	public void setCareNavigatorAccountId(@Nullable UUID careNavigatorAccountId) {
		this.careNavigatorAccountId = careNavigatorAccountId;
	}

	@Nullable
	public CareEncounterSortColumnId getCareEncounterSortColumnId() {
		return this.careEncounterSortColumnId;
	}

	public void setCareEncounterSortColumnId(@Nullable CareEncounterSortColumnId careEncounterSortColumnId) {
		this.careEncounterSortColumnId = careEncounterSortColumnId;
	}

	@Nullable
	public SortDirectionId getSortDirectionId() {
		return this.sortDirectionId;
	}

	public void setSortDirectionId(@Nullable SortDirectionId sortDirectionId) {
		this.sortDirectionId = sortDirectionId;
	}
}
