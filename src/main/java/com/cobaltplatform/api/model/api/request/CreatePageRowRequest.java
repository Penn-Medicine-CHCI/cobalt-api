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

import com.cobaltplatform.api.model.db.PageStatus;
import com.cobaltplatform.api.model.db.RowType.RowTypeId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class CreatePageRowRequest {
	@Nullable
	private UUID pageSectionId;
	@Nullable
	private RowTypeId rowTypeId;
	@Nullable
	private Integer displayOrder;
	@Nullable
	private UUID createdByAccountId;
	@Nullable
	private PageStatus.PageStatusId pageStatusId;
	@Nullable
	public UUID getPageSectionId() {
		return pageSectionId;
	}

	public void setPageSectionId(@Nullable UUID pageSectionId) {
		this.pageSectionId = pageSectionId;
	}

	@Nullable
	public RowTypeId getRowTypeId() {
		return rowTypeId;
	}

	public void setRowTypeId(@Nullable RowTypeId rowTypeId) {
		this.rowTypeId = rowTypeId;
	}

	@Nullable
	public UUID getCreatedByAccountId() {
		return createdByAccountId;
	}

	public void setCreatedByAccountId(@Nullable UUID createdByAccountId) {
		this.createdByAccountId = createdByAccountId;
	}

	@Nullable
	public PageStatus.PageStatusId getPageStatusId() {
		return pageStatusId;
	}

	public void setPageStatusId(@Nullable PageStatus.PageStatusId pageStatusId) {
		this.pageStatusId = pageStatusId;
	}

	@Nullable
	public Integer getDisplayOrder() {
		return displayOrder;
	}

	public void setDisplayOrder(@Nullable Integer displayOrder) {
		this.displayOrder = displayOrder;
	}
}
