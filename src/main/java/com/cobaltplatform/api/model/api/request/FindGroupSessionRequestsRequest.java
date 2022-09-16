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
import com.cobaltplatform.api.model.db.GroupSessionRequestStatus.GroupSessionRequestStatusId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class FindGroupSessionRequestsRequest {
	@Nullable
	private Integer pageNumber;
	@Nullable
	private Integer pageSize;
	@Nullable
	private String urlName;
	@Nullable
	private String searchQuery;
	@Nullable
	private InstitutionId institutionId;
	@Nullable
	private FilterBehavior filterBehavior;
	@Nullable
	private GroupSessionRequestStatusId groupSessionRequestStatusId;
	@Nullable
	private Account account;

	public enum FilterBehavior {
		DEFAULT,
		ONLY_MY_SESSIONS
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
	public FilterBehavior getFilterBehavior() {
		return filterBehavior;
	}

	public void setFilterBehavior(@Nullable FilterBehavior filterBehavior) {
		this.filterBehavior = filterBehavior;
	}

	@Nullable
	public GroupSessionRequestStatusId getGroupSessionRequestStatusId() {
		return groupSessionRequestStatusId;
	}

	public void setGroupSessionRequestStatusId(@Nullable GroupSessionRequestStatusId groupSessionRequestStatusId) {
		this.groupSessionRequestStatusId = groupSessionRequestStatusId;
	}

	@Nullable
	public Account getAccount() {
		return account;
	}

	public void setAccount(@Nullable Account account) {
		this.account = account;
	}
}