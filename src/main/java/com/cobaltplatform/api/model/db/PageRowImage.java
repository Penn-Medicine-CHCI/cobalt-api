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

import com.cobaltplatform.api.model.db.BackgroundColor.BackgroundColorId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class PageRowImage {
	@Nullable
	private UUID pageRowImageId;
	@Nullable
	private UUID pageRowId;
	@Nullable
	private String headline;
	@Nullable
	private String description;
	@Nullable
	private UUID imageFileUploadId;
	@Nullable
	private String imageAltText;
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
	public UUID getPageRowImageId() {
		return pageRowImageId;
	}

	public void setPageRowImageId(@Nullable UUID pageRowImageId) {
		this.pageRowImageId = pageRowImageId;
	}

	@Nullable
	public UUID getPageRowId() {
		return pageRowId;
	}

	public void setPageRowId(@Nullable UUID pageRowId) {
		this.pageRowId = pageRowId;
	}

	@Nullable
	public String getHeadline() {
		return headline;
	}

	public void setHeadline(@Nullable String headline) {
		this.headline = headline;
	}

	@Nullable
	public String getDescription() {
		return description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
	}

	@Nullable
	public UUID getImageFileUploadId() {
		return imageFileUploadId;
	}

	public void setImageFileUploadId(@Nullable UUID imageFileUploadId) {
		this.imageFileUploadId = imageFileUploadId;
	}

	@Nullable
	public String getImageAltText() {
		return imageAltText;
	}

	public void setImageAltText(@Nullable String imageAltText) {
		this.imageAltText = imageAltText;
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