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
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.PageStatus.PageStatusId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;


/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Page {
	@Nullable
	private UUID pageId;
	@Nullable
	private String name;
	@Nullable
	private String urlName;
	@Nullable
	private PageStatusId pageStatusId;
	@Nullable
	private String headline;
	@Nullable
	private String description;
	@Nullable
	private UUID imageFileUploadId;
	@Nullable
	private String imageAltText;
	@Nullable
	private String imageUrl;
	@Nullable
	private LocalDate publishedDate;
	@Nullable
	private Boolean deletedFlag;
	@Nullable
	private InstitutionId institutionId;
	@Nullable
	private UUID createdByAccountId;
	@Nullable
	private UUID parentPageId;
	@Nullable
	private UUID pageGroupId;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;
	@Nullable
	private Instant originalCreateDate;

	@Nullable
	public UUID getPageId() {
		return pageId;
	}

	public void setPageId(@Nullable UUID pageId) {
		this.pageId = pageId;
	}

	@Nullable
	public String getName() {
		return name;
	}

	public void setName(@Nullable String name) {
		this.name = name;
	}

	@Nullable
	public String getUrlName() {
		return urlName;
	}

	public void setUrlName(@Nullable String urlName) {
		this.urlName = urlName;
	}

	@Nullable
	public PageStatusId getPageStatusId() {
		return pageStatusId;
	}

	public void setPageStatusId(@Nullable PageStatusId pageStatusId) {
		this.pageStatusId = pageStatusId;
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
	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(@Nullable String imageUrl) {
		this.imageUrl = imageUrl;
	}

	@Nullable
	public LocalDate getPublishedDate() {
		return publishedDate;
	}

	public void setPublishedDate(@Nullable LocalDate publishedDate) {
		this.publishedDate = publishedDate;
	}

	@Nullable
	public Boolean getDeletedFlag() {
		return deletedFlag;
	}

	public void setDeletedFlag(@Nullable Boolean deletedFlag) {
		this.deletedFlag = deletedFlag;
	}

	@Nullable
	public InstitutionId getInstitutionId() {
		return institutionId;
	}

	public void setInstitutionId(@Nullable InstitutionId institutionId) {
		this.institutionId = institutionId;
	}

	@Nullable
	public UUID getCreatedByAccountId() {
		return createdByAccountId;
	}

	public void setCreatedByAccountId(@Nullable UUID createdByAccountId) {
		this.createdByAccountId = createdByAccountId;
	}

	@Nullable
	public UUID getParentPageId() {
		return parentPageId;
	}

	public void setParentPageId(@Nullable UUID parentPageId) {
		this.parentPageId = parentPageId;
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

	@Nullable
	public UUID getPageGroupId() {
		return pageGroupId;
	}

	public void setPageGroupId(@Nullable UUID pageGroupId) {
		this.pageGroupId = pageGroupId;
	}

	@Nullable
	public Instant getOriginalCreateDate() {
		return originalCreateDate;
	}

	public void setOriginalCreateDate(@Nullable Instant originalCreateDate) {
		this.originalCreateDate = originalCreateDate;
	}
}