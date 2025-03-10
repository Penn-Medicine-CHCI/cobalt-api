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

import com.cobaltplatform.api.model.db.ContentAudienceType.ContentAudienceTypeId;
import com.cobaltplatform.api.model.db.ContentType.ContentTypeId;
import com.cobaltplatform.api.model.db.ContentVisibilityType;
import com.cobaltplatform.api.model.db.ContentVisibilityType.ContentVisibilityTypeId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.service.ContentDurationId;
import com.cobaltplatform.api.model.service.ResourceLibrarySortColumnId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Set;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class FindResourceLibraryContentRequest {
	@Nullable
	private InstitutionId institutionId;
	@Nullable
	private String searchQuery;
	@Nullable
	private Integer pageNumber;
	@Nullable
	private Integer pageSize;
	@Nullable
	private String tagGroupId;
	@Nullable
	private Set<String> tagIds;
	@Nullable
	private Set<ContentTypeId> contentTypeIds;
	@Nullable
	private Set<ContentDurationId> contentDurationIds;
	@Nullable
	private Set<ContentAudienceTypeId> contentAudienceTypeIds;
	@Nullable
	private ResourceLibrarySortColumnId resourceLibrarySortColumnId;
	@Nullable
	private UUID prioritizeUnviewedForAccountId;
	@Nullable
	private ContentVisibilityTypeId contentVisibilityTypeId;

	@Nullable
	public InstitutionId getInstitutionId() {
		return this.institutionId;
	}

	public void setInstitutionId(@Nullable InstitutionId institutionId) {
		this.institutionId = institutionId;
	}

	@Nullable
	public String getSearchQuery() {
		return this.searchQuery;
	}

	public void setSearchQuery(@Nullable String searchQuery) {
		this.searchQuery = searchQuery;
	}

	@Nullable
	public Integer getPageNumber() {
		return this.pageNumber;
	}

	public void setPageNumber(@Nullable Integer pageNumber) {
		this.pageNumber = pageNumber;
	}

	@Nullable
	public Integer getPageSize() {
		return this.pageSize;
	}

	public void setPageSize(@Nullable Integer pageSize) {
		this.pageSize = pageSize;
	}

	@Nullable
	public String getTagGroupId() {
		return this.tagGroupId;
	}

	public void setTagGroupId(@Nullable String tagGroupId) {
		this.tagGroupId = tagGroupId;
	}

	@Nullable
	public Set<String> getTagIds() {
		return this.tagIds;
	}

	public void setTagIds(@Nullable Set<String> tagIds) {
		this.tagIds = tagIds;
	}

	@Nullable
	public Set<ContentTypeId> getContentTypeIds() {
		return this.contentTypeIds;
	}

	public void setContentTypeIds(@Nullable Set<ContentTypeId> contentTypeIds) {
		this.contentTypeIds = contentTypeIds;
	}

	@Nullable
	public Set<ContentDurationId> getContentDurationIds() {
		return this.contentDurationIds;
	}

	public void setContentDurationIds(@Nullable Set<ContentDurationId> contentDurationIds) {
		this.contentDurationIds = contentDurationIds;
	}

	@Nullable
	public Set<ContentAudienceTypeId> getContentAudienceTypeIds() {
		return this.contentAudienceTypeIds;
	}

	public void setContentAudienceTypeIds(@Nullable Set<ContentAudienceTypeId> contentAudienceTypeIds) {
		this.contentAudienceTypeIds = contentAudienceTypeIds;
	}

	@Nullable
	public UUID getPrioritizeUnviewedForAccountId() {
		return this.prioritizeUnviewedForAccountId;
	}

	public void setPrioritizeUnviewedForAccountId(@Nullable UUID prioritizeUnviewedForAccountId) {
		this.prioritizeUnviewedForAccountId = prioritizeUnviewedForAccountId;
	}

	@Nullable
	public ResourceLibrarySortColumnId getResourceLibrarySortColumnId() {
		return this.resourceLibrarySortColumnId;
	}

	public void setResourceLibrarySortColumnId(@Nullable ResourceLibrarySortColumnId resourceLibrarySortColumnId) {
		this.resourceLibrarySortColumnId = resourceLibrarySortColumnId;
	}

	@Nullable
	public ContentVisibilityTypeId getContentVisibilityTypeId() {
		return this.contentVisibilityTypeId;
	}

	public void setContentVisibilityTypeId(@Nullable ContentVisibilityTypeId contentVisibilityTypeId) {
		this.contentVisibilityTypeId = contentVisibilityTypeId;
	}
}