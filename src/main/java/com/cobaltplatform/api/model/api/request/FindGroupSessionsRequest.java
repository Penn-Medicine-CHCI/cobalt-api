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

import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.GroupSessionSchedulingSystem.GroupSessionSchedulingSystemId;
import com.cobaltplatform.api.model.db.GroupSessionStatus.GroupSessionStatusId;
import com.cobaltplatform.api.model.db.GroupSessionVisibilityType.GroupSessionVisibilityTypeId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class FindGroupSessionsRequest {
	@Nullable
	private Integer pageNumber;
	@Nullable
	private Integer pageSize;
	@Nullable
	private GroupSessionStatusId groupSessionStatusId;
	@Nullable
	private String urlName;
	@Nullable
	private String searchQuery;
	@Nullable
	private InstitutionId institutionId;
	@Nullable
	private OrderBy orderBy;
	@Nullable
	private FilterBehavior filterBehavior;
	@Nullable
	private Account account;
	@Nullable
	private UUID groupSessionCollectionId;
	@Nonnull
	private GroupSessionSchedulingSystemId groupSessionSchedulingSystemId;
	@Nonnull
	private GroupSessionVisibilityTypeId groupSessionVisibilityTypeId;

	public enum FilterBehavior {
		DEFAULT,
		ONLY_MY_SESSIONS
	}

	public enum OrderBy {
		START_TIME_ASCENDING,
		START_TIME_DESCENDING,
		DATE_ADDED_ASCENDING,
		DATE_ADDED_DESCENDING,
		REGISTERED_ASCENDING,
		REGISTERED_DESCENDING,
		CAPACITY_ASCENDING,
		CAPACITY_DESCENDING
	}

	@Nullable
	public Integer getPageNumber() {
		return pageNumber;
	}

	public void setPageNumber(@Nullable Integer pageNumber) {
		this.pageNumber = pageNumber;
	}

	@Nullable
	public Integer getPageSize() {
		return pageSize;
	}

	public void setPageSize(@Nullable Integer pageSize) {
		this.pageSize = pageSize;
	}

	@Nullable
	public String getUrlName() {
		return urlName;
	}

	public void setUrlName(@Nullable String urlName) {
		this.urlName = urlName;
	}

	@Nullable
	public String getSearchQuery() {
		return this.searchQuery;
	}

	public void setSearchQuery(@Nullable String searchQuery) {
		this.searchQuery = searchQuery;
	}

	@Nullable
	public InstitutionId getInstitutionId() {
		return institutionId;
	}

	public void setInstitutionId(@Nullable InstitutionId institutionId) {
		this.institutionId = institutionId;
	}

	@Nullable
	public GroupSessionStatusId getGroupSessionStatusId() {
		return groupSessionStatusId;
	}

	public void setGroupSessionStatusId(@Nullable GroupSessionStatusId groupSessionStatusId) {
		this.groupSessionStatusId = groupSessionStatusId;
	}

	@Nullable
	public OrderBy getOrderBy() {
		return orderBy;
	}

	public void setOrderBy(@Nullable OrderBy orderBy) {
		this.orderBy = orderBy;
	}

	@Nullable
	public FilterBehavior getFilterBehavior() {
		return filterBehavior;
	}

	public void setFilterBehavior(@Nullable FilterBehavior filterBehavior) {
		this.filterBehavior = filterBehavior;
	}

	@Nullable
	public Account getAccount() {
		return account;
	}

	public void setAccount(@Nullable Account account) {
		this.account = account;
	}

	@Nullable
	public UUID getGroupSessionCollectionId() {
		return groupSessionCollectionId;
	}

	public void setGroupSessionCollectionId(@Nullable UUID groupSessionCollectionId) {
		this.groupSessionCollectionId = groupSessionCollectionId;
	}

	@Nonnull
	public GroupSessionSchedulingSystemId getGroupSessionSchedulingSystemId() {
		return groupSessionSchedulingSystemId;
	}

	public void setGroupSessionSchedulingSystemId(@Nonnull GroupSessionSchedulingSystemId groupSessionSchedulingSystemId) {
		this.groupSessionSchedulingSystemId = groupSessionSchedulingSystemId;
	}

	@Nonnull
	public GroupSessionVisibilityTypeId getGroupSessionVisibilityTypeId() {
		return this.groupSessionVisibilityTypeId;
	}

	public void setGroupSessionVisibilityTypeId(@Nonnull GroupSessionVisibilityTypeId groupSessionVisibilityTypeId) {
		this.groupSessionVisibilityTypeId = groupSessionVisibilityTypeId;
	}
}