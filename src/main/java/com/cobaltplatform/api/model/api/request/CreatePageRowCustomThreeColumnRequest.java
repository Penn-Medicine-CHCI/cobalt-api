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


import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class CreatePageRowCustomThreeColumnRequest {
	@Nullable
	private UUID pageSectionId;
	@Nullable
	private CreatePageRowColumnRequest columnOne;
	@Nullable
	private CreatePageRowColumnRequest columnTwo;
	@Nullable
	private CreatePageRowColumnRequest columnThree;
	@Nullable
	private UUID createdByAccountId;

	@Nullable
	public UUID getPageSectionId() {
		return pageSectionId;
	}

	public void setPageSectionId(@Nullable UUID pageSectionId) {
		this.pageSectionId = pageSectionId;
	}

	@Nullable
	public CreatePageRowColumnRequest getColumnOne() {
		return columnOne;
	}

	public void setColumnOne(@Nullable CreatePageRowColumnRequest columnOne) {
		this.columnOne = columnOne;
	}

	@Nullable
	public CreatePageRowColumnRequest getColumnTwo() {
		return columnTwo;
	}

	public void setColumnTwo(@Nullable CreatePageRowColumnRequest columnTwo) {
		this.columnTwo = columnTwo;
	}

	@Nullable
	public CreatePageRowColumnRequest getColumnThree() {
		return columnThree;
	}

	public void setColumnThree(@Nullable CreatePageRowColumnRequest columnThree) {
		this.columnThree = columnThree;
	}

	@Nullable
	public UUID getCreatedByAccountId() {
		return createdByAccountId;
	}

	public void setCreatedByAccountId(@Nullable UUID createdByAccountId) {
		this.createdByAccountId = createdByAccountId;
	}
}
