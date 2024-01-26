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

import com.cobaltplatform.api.model.db.ContentStatus;
import com.cobaltplatform.api.model.db.ContentType.ContentTypeId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class UpdateContentRequest {
	@Nullable
	private UUID contentId;
	@Nullable
	private ContentTypeId contentTypeId;
	@Nonnull
	private String title;
	@Nullable
	private String author;
	@Nullable
	private String url;
	@Nullable
	private String durationInMinutes;
	@Nullable
	private String description;
	@Nullable
	private LocalDate publishStartDate;
	@Nullable
	private LocalDate publishEndDate;
	@Nullable
	private Boolean publishRecurring;
	@Nullable
	private Set<String> tagIds;
	@Nullable
	private String searchTerms;
	@Nullable
	private Boolean sharedFlag;
	@Nullable
	private UUID fileUploadId;
	@Nullable
	private UUID imageFileUploadId;

	@Nullable
	public UUID getContentId() {
		return contentId;
	}

	public void setContentId(@Nullable UUID contentId) {
		this.contentId = contentId;
	}

	@Nullable
	public ContentTypeId getContentTypeId() {
		return contentTypeId;
	}

	public void setContentTypeId(@Nullable ContentTypeId contentTypeId) {
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
	public String getAuthor() {
		return author;
	}

	public void setAuthor(@Nullable String author) {
		this.author = author;
	}

	@Nullable
	public String getUrl() {
		return url;
	}

	public void setUrl(@Nullable String url) {
		this.url = url;
	}

	@Nullable
	public String getDurationInMinutes() {
		return durationInMinutes;
	}

	public void setDurationInMinutes(@Nullable String durationInMinutes) {
		this.durationInMinutes = durationInMinutes;
	}

	@Nullable
	public String getDescription() {
		return description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
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
	public Set<String> getTagIds() {
		return tagIds;
	}

	public void setTagIds(@Nullable Set<String> tagIds) {
		this.tagIds = tagIds;
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
	public UUID getFileUploadId() {
		return fileUploadId;
	}

	public void setFileUploadId(@Nullable UUID fileUploadId) {
		this.fileUploadId = fileUploadId;
	}

	@Nullable
	public UUID getImageFileUploadId() {
		return imageFileUploadId;
	}

	public void setImageFileUploadId(@Nullable UUID imageFileUploadId) {
		this.imageFileUploadId = imageFileUploadId;
	}
}
