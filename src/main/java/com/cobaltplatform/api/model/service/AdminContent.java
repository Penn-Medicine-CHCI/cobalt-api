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

import com.cobaltplatform.api.model.db.ContentStatus.ContentStatusId;
import com.cobaltplatform.api.model.db.ContentType;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Tag;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class AdminContent{
	@Nonnull
	private UUID contentId;
	@Nonnull
	private ContentType.ContentTypeId contentTypeId;
	@Nonnull
	private String title;
	@Nonnull
	private String author;
	@Nonnull
	private String description;
	@Nullable
	private String url;
	@Nullable
	private String imageUrl;
	@Nullable
	private Integer durationInMinutes;
	@Nonnull
	private String ownerInstitution;
	@Nonnull
	private Integer views;
	@Nonnull
	private Institution.InstitutionId ownerInstitutionId;
	@Nullable
	private Integer totalCount;
	@Nullable
	private LocalDate publishStartDate;
	@Nullable
	private LocalDate publishEndDate;
	@Nullable
	private LocalDate dateCreated;
	@Nullable
	private Boolean publishRecurring;
	@Nullable
	private String searchTerms;
	@Nullable
	private Boolean sharedFlag;
	@Nullable
	private ContentStatusId contentStatusId;

	@Nullable
	private String contentStatusDescription;

	// This is stored separately in the DB, but in practice it's needed everywhere we see content in the UI
	@Nullable
	private List<Tag> tags;

	@Nullable
	private String contentTypeDescription;

	@Nullable
	private String callToAction;
	@Nullable
	private UUID fileUploadId;

	@Nullable
	private UUID imageFileUploadId;

	@Nullable
	private String inUseInstitutionDescription;

	@Nullable
	private Integer inUseCount;

	@Nullable
	private String fileUrl;

	@Nullable
	private String filename;

	@Nullable
	private String fileContentType;

	@Nullable
	private LocalDate dateAddedToInstitution;

	@Nonnull
	public UUID getContentId() {
		return contentId;
	}

	public void setContentId(@Nonnull UUID contentId) {
		this.contentId = contentId;
	}
	@Nonnull
	public ContentType.ContentTypeId getContentTypeId() {
		return contentTypeId;
	}

	public void setContentTypeId(@Nonnull ContentType.ContentTypeId contentTypeId) {
		this.contentTypeId = contentTypeId;
	}

	@Nonnull
	public String getTitle() {
		return title;
	}

	public void setTitle(@Nonnull String title) {
		this.title = title;
	}

	@Nonnull
	public String getAuthor() {
		return author;
	}

	public void setAuthor(@Nonnull String author) {
		this.author = author;
	}

	@Nonnull
	public String getDescription() {
		return description;
	}

	public void setDescription(@Nonnull String description) {
		this.description = description;
	}

	@Nullable
	public String getUrl() {
		return url;
	}

	public void setUrl(@Nullable String url) {
		this.url = url;
	}

	@Nullable
	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(@Nullable String imageUrl) {
		this.imageUrl = imageUrl;
	}

	@Nullable
	public Integer getDurationInMinutes() {
		return durationInMinutes;
	}

	public void setDurationInMinutes(@Nullable Integer durationInMinutes) {
		this.durationInMinutes = durationInMinutes;
	}

	@Nonnull
	public String getOwnerInstitution() {
		return ownerInstitution;
	}

	public void setOwnerInstitution(@Nonnull String ownerInstitution) {
		this.ownerInstitution = ownerInstitution;
	}

	@Nonnull
	public Integer getViews() {
		return views;
	}

	public void setViews(@Nonnull Integer views) {
		this.views = views;
	}

	@Nonnull
	public Institution.InstitutionId getOwnerInstitutionId() {
		return ownerInstitutionId;
	}

	public void setOwnerInstitutionId(@Nonnull Institution.InstitutionId ownerInstitutionId) {
		this.ownerInstitutionId = ownerInstitutionId;
	}
	@Nullable
	public Integer getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(@Nullable Integer totalCount) {
		this.totalCount = totalCount;
	}

	@Nullable
	public List<Tag> getTags() {
		return this.tags;
	}

	public void setTags(@Nullable List<Tag> tags) {
		this.tags = tags;
	}

	@Nullable
	public LocalDate getPublishStartDate() {
		return publishStartDate;
	}

	public void setPublishStartDate(@Nullable LocalDate publishStartDate) {
		this.publishStartDate = publishStartDate;
	}

	@Nullable
	public LocalDate getPublishEndDate() {
		return publishEndDate;
	}

	public void setPublishEndDate(@Nullable LocalDate publishEndDate) {
		this.publishEndDate = publishEndDate;
	}

	@Nullable
	public Boolean getPublishRecurring() {
		return publishRecurring;
	}

	public void setPublishRecurring(@Nullable Boolean publishRecurring) {
		this.publishRecurring = publishRecurring;
	}

	@Nullable
	public String getSearchTerms() {
		return searchTerms;
	}

	public void setSearchTerms(@Nullable String searchTerms) {
		this.searchTerms = searchTerms;
	}

	@Nullable
	public Boolean getSharedFlag() {
		return sharedFlag;
	}

	public void setSharedFlag(@Nullable Boolean sharedFlag) {
		this.sharedFlag = sharedFlag;
	}

	@Nullable
	public ContentStatusId getContentStatusId() {
		return contentStatusId;
	}

	public void setContentStatusId(@Nullable ContentStatusId contentStatusId) {
		this.contentStatusId = contentStatusId;
	}

	@Nullable
	public LocalDate getDateCreated() {
		return dateCreated;
	}

	public void setDateCreated(@Nullable LocalDate dateCreated) {
		this.dateCreated = dateCreated;
	}

	@Nullable
	public String getContentStatusDescription() {
		return contentStatusDescription;
	}

	public void setContentStatusDescription(@Nullable String contentStatusDescription) {
		this.contentStatusDescription = contentStatusDescription;
	}

	@Nullable
	public String getContentTypeDescription() {
		return contentTypeDescription;
	}

	public void setContentTypeDescription(@Nullable String contentTypeDescription) {
		this.contentTypeDescription = contentTypeDescription;
	}

	@Nullable
	public String getCallToAction() {
		return callToAction;
	}

	public void setCallToAction(@Nullable String callToAction) {
		this.callToAction = callToAction;
	}

	@Nullable
	public UUID getFileUploadId() {
		return fileUploadId;
	}

	public void setFileUploadId(@Nullable UUID fileUploadId) {
		this.fileUploadId = fileUploadId;
	}

	@Nullable
	public String getInUseInstitutionDescription() {
		return inUseInstitutionDescription;
	}

	public void setInUseInstitutionDescription(@Nullable String inUseInstitutionDescription) {
		this.inUseInstitutionDescription = inUseInstitutionDescription;
	}

	@Nullable
	public Integer getInUseCount() {
		return inUseCount;
	}

	public void setInUseCount(@Nullable Integer inUseCount) {
		this.inUseCount = inUseCount;
	}

	@Nullable
	public String getFileUrl() {
		return fileUrl;
	}

	public void setFileUrl(@Nullable String fileUrl) {
		this.fileUrl = fileUrl;
	}

	@Nullable
	public String getFilename() {
		return filename;
	}

	public void setFilename(@Nullable String filename) {
		this.filename = filename;
	}

	@Nullable
	public String getFileContentType() {
		return fileContentType;
	}

	public void setFileContentType(@Nullable String fileContentType) {
		this.fileContentType = fileContentType;
	}

	@Nullable
	public UUID getImageFileUploadId() {
		return imageFileUploadId;
	}

	public void setImageFileUploadId(@Nullable UUID imageFileUploadId) {
		this.imageFileUploadId = imageFileUploadId;
	}

	@Nullable
	public LocalDate getDateAddedToInstitution() {
		return dateAddedToInstitution;
	}

	public void setDateAddedToInstitution(@Nullable LocalDate dateAddedToInstitution) {
		this.dateAddedToInstitution = dateAddedToInstitution;
	}
}
