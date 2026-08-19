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

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.LocalDate;
import java.util.UUID;

@NotThreadSafe
public class FindCareEncountersRequest {
	@Nullable
	private UUID providerId;
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
	@Nullable
	private OrderBy orderBy;

	public enum OrderBy {
		APPOINTMENT_START_TIME_ASC,
		APPOINTMENT_START_TIME_DESC,
		PATIENT_NAME_ASC,
		PATIENT_NAME_DESC,
		STATUS_ASC,
		STATUS_DESC,
		LAST_UPDATED_ASC,
		LAST_UPDATED_DESC
	}

	@Nullable
	public UUID getProviderId() {
		return this.providerId;
	}

	public void setProviderId(@Nullable UUID providerId) {
		this.providerId = providerId;
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

	@Nullable
	public OrderBy getOrderBy() {
		return this.orderBy;
	}

	public void setOrderBy(@Nullable OrderBy orderBy) {
		this.orderBy = orderBy;
	}
}
