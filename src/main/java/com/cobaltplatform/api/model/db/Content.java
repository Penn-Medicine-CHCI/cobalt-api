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

import com.cobaltplatform.api.model.db.ContentType.ContentTypeId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Content {
	@Nonnull
	private UUID contentId;
	@Nonnull
	private ContentTypeId contentTypeId;
	@Nonnull
	private String title;
	@Nullable
	private String url;
	@Nullable
	private String imageUrl;
	@Nullable
	private String description;
	@Nullable
	private String author;
	@Nullable
	private Instant created;
	@Nonnull
	private String createdDescription;
	@Nullable
	private Instant lastUpdated;
	@Nullable
	private String lastUpdatedDescription;
	@Nonnull
	private InstitutionId ownerInstitutionId;
	@Nonnull
	private Integer durationInMinutes;

	//TODO: re-think this
	//Additional contentType attributes
	@Nullable
	private String contentTypeDescription;
	@Nullable
	private String callToAction;

	@Nullable
	private String contentTypeLabel;

	//TODO: re-think this
	// Determines if the content is new to the current user
	@Nullable
	private Boolean newFlag;

	// This is stored separately in the DB, but in practice it's needed everywhere we see content in the UI
	@Nullable
	private List<Tag> tags;

	@Nullable
	private LocalDate publishStartDate;
	@Nullable
	private LocalDate publishEndDate;
	@Nullable
	private Boolean publishRecurring;
	@Nullable
	private String searchTerms;
	@Nullable
	private Boolean sharedFlag;
	@Nullable
	private ContentStatus.ContentStatusId contentStatusId;

	@Nullable
	private String contentStatusDescription;
	@Nullable
	private Integer totalCount;
	@Nullable
	private LocalDate dateCreated;

	@Nonnull
	public UUID getContentId() {
		return contentId;
	}

	public void setContentId(@Nonnull UUID contentId) {
		this.contentId = contentId;
	}

	@Nonnull
	public ContentTypeId getContentTypeId() {
		return contentTypeId;
	}

	public void setContentTypeId(@Nonnull ContentTypeId contentTypeId) {
		this.contentTypeId = contentTypeId;
	}

	@Nonnull
	public String getTitle() {
		return title;
	}

	public void setTitle(@Nonnull String title) {
		this.title = title;
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
	public String getDescription() {
		return description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
	}

	@Nullable
	public String getAuthor() {
		return author;
	}

	public void setAuthor(@Nullable String author) {
		this.author = author;
	}

	@Nullable
	public Instant getCreated() {
		return created;
	}

	public void setCreated(@Nullable Instant created) {
		this.created = created;
	}

	@Nonnull
	public String getCreatedDescription() {
		return createdDescription;
	}

	public void setCreatedDescription(@Nonnull String createdDescription) {
		this.createdDescription = createdDescription;
	}

	@Nullable
	public Instant getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(@Nullable Instant lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	@Nullable
	public String getLastUpdatedDescription() {
		return lastUpdatedDescription;
	}

	public void setLastUpdatedDescription(@Nullable String lastUpdatedDescription) {
		this.lastUpdatedDescription = lastUpdatedDescription;
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
	public Boolean getNewFlag() {
		return newFlag;
	}

	public void setNewFlag(@Nullable Boolean newFlag) {
		this.newFlag = newFlag;
	}

	@Nonnull
	public InstitutionId getOwnerInstitutionId() {
		return ownerInstitutionId;
	}

	public void setOwnerInstitutionId(@Nonnull InstitutionId ownerInstitutionId) {
		this.ownerInstitutionId = ownerInstitutionId;
	}

	@Nonnull
	public Integer getDurationInMinutes() {
		return durationInMinutes;
	}

	public void setDurationInMinutes(@Nonnull Integer durationInMinutes) {
		this.durationInMinutes = durationInMinutes;
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
	public ContentStatus.ContentStatusId getContentStatusId() {
		return contentStatusId;
	}

	public void setContentStatusId(@Nullable ContentStatus.ContentStatusId contentStatusId) {
		this.contentStatusId = contentStatusId;
	}

	@Nullable
	public Integer getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(@Nullable Integer totalCount) {
		this.totalCount = totalCount;
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
	public String getContentTypeLabel() {
		return contentTypeLabel;
	}

	public void setContentTypeLabel(@Nullable String contentTypeLabel) {
		this.contentTypeLabel = contentTypeLabel;
	}
}