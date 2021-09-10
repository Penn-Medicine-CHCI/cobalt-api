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

package com.cobaltplatform.api.model.service;

import com.cobaltplatform.api.model.db.GroupSessionStatus.GroupSessionStatusId;

import javax.annotation.Nullable;

/**
 * @author Transmogrify, LLC.
 */
public class GroupSessionStatusWithCount {
	@Nullable
	private GroupSessionStatusId groupSessionStatusId;
	@Nullable
	private String groupSessionStatusIdDescription;
	@Nullable
	private Integer totalCount;

	@Nullable
	public GroupSessionStatusId getGroupSessionStatusId() {
		return groupSessionStatusId;
	}

	public void setGroupSessionStatusId(@Nullable GroupSessionStatusId groupSessionStatusId) {
		this.groupSessionStatusId = groupSessionStatusId;
	}

	@Nullable
	public String getGroupSessionStatusIdDescription() {
		return groupSessionStatusIdDescription;
	}

	public void setGroupSessionStatusIdDescription(@Nullable String groupSessionStatusIdDescription) {
		this.groupSessionStatusIdDescription = groupSessionStatusIdDescription;
	}

	@Nullable
	public Integer getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(@Nullable Integer totalCount) {
		this.totalCount = totalCount;
	}
}
