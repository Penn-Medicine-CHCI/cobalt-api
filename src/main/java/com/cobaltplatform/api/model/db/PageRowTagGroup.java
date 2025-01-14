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

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class PageRowTagGroup {
	@Nullable
	private UUID pageRowTagGroupId;
	@Nullable
	private UUID pageRowId;
	@Nullable
	private String tagGroupId;
	@Nullable
	private Boolean deletedFlag;
	@Nullable
	private Integer displayOrder;
	@Nullable
	private UUID createdByAccountId;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nullable
	public UUID getPageRowTagGroupId() {
		return pageRowTagGroupId;
	}

	public void setPageRowTagGroupId(@Nullable UUID pageRowTagGroupId) {
		this.pageRowTagGroupId = pageRowTagGroupId;
	}

	@Nullable
	public UUID getPageRowId() {
		return pageRowId;
	}

	public void setPageRowId(@Nullable UUID pageRowId) {
		this.pageRowId = pageRowId;
	}

	@Nullable
	public String getTagGroupId() {
		return tagGroupId;
	}

	public void setTagGroupId(@Nullable String tagGroupId) {
		this.tagGroupId = tagGroupId;
	}

	@Nullable
	public Boolean getDeletedFlag() {
		return deletedFlag;
	}

	public void setDeletedFlag(@Nullable Boolean deletedFlag) {
		this.deletedFlag = deletedFlag;
	}

	@Nullable
	public Integer getDisplayOrder() {
		return displayOrder;
	}

	public void setDisplayOrder(@Nullable Integer displayOrder) {
		this.displayOrder = displayOrder;
	}

	@Nullable
	public UUID getCreatedByAccountId() {
		return createdByAccountId;
	}

	public void setCreatedByAccountId(@Nullable UUID createdByAccountId) {
		this.createdByAccountId = createdByAccountId;
	}

	@Nullable
	public Instant getCreated() {
		return created;
	}

	public void setCreated(@Nullable Instant created) {
		this.created = created;
	}

	@Nullable
	public Instant getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(@Nullable Instant lastUpdated) {
		this.lastUpdated = lastUpdated;
	}
}