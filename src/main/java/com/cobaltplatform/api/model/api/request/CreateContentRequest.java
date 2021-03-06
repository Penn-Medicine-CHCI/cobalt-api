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

import com.cobaltplatform.api.model.db.ContentType;
import com.cobaltplatform.api.model.db.Visibility;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.LocalDate;
import java.util.List;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class CreateContentRequest {
	@Nullable
	private ContentType.ContentTypeId contentTypeId;
	@Nonnull
	private String title;
	@Nullable
	private String author;
	@Nullable
	private String url;
	@Nullable
	private String imageUrl;
	@Nullable
	private String durationInMinutes;
	@Nullable
	private String description;
	@Nullable
	private String contentTypeLabelId;
	@Nullable
	private LocalDate dateCreated;
	@Nullable
	private Visibility.VisibilityId visibilityId;
	@Nullable
	private List<InstitutionId> institutionIdList;
	@Nullable
	private PersonalizeAssessmentChoicesCommand contentTags;

	@Nullable
	public ContentType.ContentTypeId getContentTypeId() {
		return contentTypeId;
	}

	public void setContentTypeId(@Nullable ContentType.ContentTypeId contentTypeId) {
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
	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(@Nullable String imageUrl) {
		this.imageUrl = imageUrl;
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
	public String getContentTypeLabelId() {
		return contentTypeLabelId;
	}

	public void setContentTypeLabelId(@Nullable String contentTypeLabelId) {
		this.contentTypeLabelId = contentTypeLabelId;
	}

	@Nullable
	public Visibility.VisibilityId getVisibilityId() {
		return visibilityId;
	}

	public void setVisibilityId(@Nullable Visibility.VisibilityId visibilityId) {
		this.visibilityId = visibilityId;
	}

	@Nullable
	public List<InstitutionId> getInstitutionIdList() {
		return institutionIdList;
	}

	public void setInstitutionIdList(@Nullable List<InstitutionId> institutionIdList) {
		this.institutionIdList = institutionIdList;
	}

	@Nullable
	public LocalDate getDateCreated() {
		return dateCreated;
	}

	public void setDateCreated(@Nullable LocalDate dateCreated) {
		this.dateCreated = dateCreated;
	}

	@Nullable
	public PersonalizeAssessmentChoicesCommand getContentTags() {
		return contentTags;
	}

	public void setContentTags(@Nullable PersonalizeAssessmentChoicesCommand contentTags) {
		this.contentTags = contentTags;
	}
}
