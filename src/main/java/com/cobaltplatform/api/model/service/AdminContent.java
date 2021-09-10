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

import com.cobaltplatform.api.model.db.ApprovalStatus;
import com.cobaltplatform.api.model.db.ContentType;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Visibility;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.time.LocalDate;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class AdminContent{
	@Nonnull
	private UUID contentId;
	@Nonnull
	private LocalDate dateCreated;
	@Nonnull
	private String dateCreatedDescription;
	@Nonnull
	private ContentType.ContentTypeId contentTypeId;
	@Nonnull
	private String contentTypeLabelId;
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
	private Boolean approvedFlag;
	@Nonnull
	private Institution.InstitutionId ownerInstitutionId;
	@Nonnull
	private Boolean archivedFlag;
	@Nullable
	private Integer totalCount;
	@Nullable
	private Visibility.VisibilityId visibilityId;
	@Nullable
	private ApprovalStatus.ApprovalStatusId ownerInstitutionApprovalStatusId;
	@Nullable
	private ApprovalStatus.ApprovalStatusId otherInstitutionApprovalStatusId;

	@Nonnull
	public UUID getContentId() {
		return contentId;
	}

	public void setContentId(@Nonnull UUID contentId) {
		this.contentId = contentId;
	}

	@Nonnull
	public String getContentTypeLabelId() {
		return contentTypeLabelId;
	}

	public void setContentTypeLabelId(@Nonnull String contentTypeLabelId) {
		this.contentTypeLabelId = contentTypeLabelId;
	}

	@Nonnull
	public LocalDate getDateCreated() {
		return dateCreated;
	}

	public void setDateCreated(@Nonnull LocalDate dateCreated) {
		this.dateCreated = dateCreated;
	}

	@Nonnull
	public String getDateCreatedDescription() {
		return dateCreatedDescription;
	}

	public void setDateCreatedDescription(@Nonnull String dateCreatedDescription) {
		this.dateCreatedDescription = dateCreatedDescription;
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
	public Boolean getApprovedFlag() {
		return approvedFlag;
	}

	public void setApprovedFlag(@Nonnull Boolean approvedFlag) {
		this.approvedFlag = approvedFlag;
	}

	@Nonnull
	public Institution.InstitutionId getOwnerInstitutionId() {
		return ownerInstitutionId;
	}

	public void setOwnerInstitutionId(@Nonnull Institution.InstitutionId ownerInstitutionId) {
		this.ownerInstitutionId = ownerInstitutionId;
	}

	@Nonnull
	public Boolean getArchivedFlag() {
		return archivedFlag;
	}

	public void setArchivedFlag(@Nonnull Boolean archivedFlag) {
		this.archivedFlag = archivedFlag;
	}

	@Nullable
	public Integer getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(@Nullable Integer totalCount) {
		this.totalCount = totalCount;
	}

	@Nullable
	public Visibility.VisibilityId getVisibilityId() {
		return visibilityId;
	}

	public void setVisibilityId(@Nullable Visibility.VisibilityId visibilityId) {
		this.visibilityId = visibilityId;
	}

	@Nullable
	public ApprovalStatus.ApprovalStatusId getOwnerInstitutionApprovalStatusId() {
		return ownerInstitutionApprovalStatusId;
	}

	public void setOwnerInstitutionApprovalStatusId(@Nullable ApprovalStatus.ApprovalStatusId ownerInstitutionApprovalStatusId) {
		this.ownerInstitutionApprovalStatusId = ownerInstitutionApprovalStatusId;
	}

	@Nullable
	public ApprovalStatus.ApprovalStatusId getOtherInstitutionApprovalStatusId() {
		return otherInstitutionApprovalStatusId;
	}

	public void setOtherInstitutionApprovalStatusId(@Nullable ApprovalStatus.ApprovalStatusId otherInstitutionApprovalStatusId) {
		this.otherInstitutionApprovalStatusId = otherInstitutionApprovalStatusId;
	}
}
