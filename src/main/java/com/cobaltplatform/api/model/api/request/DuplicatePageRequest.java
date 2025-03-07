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

import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.PageStatus.PageStatusId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class DuplicatePageRequest {
	@Nullable
	private UUID pageId;
	@Nullable
	private InstitutionId institutionId;
	@Nullable
	private String name;
	@Nullable
	private String urlName;
	@Nullable
	private UUID createdByAccountId;
	@Nullable
	private Boolean copyForEditing;
	@Nullable
	private PageStatusId pageStatusId;

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
	public UUID getCreatedByAccountId() {
		return createdByAccountId;
	}

	public void setCreatedByAccountId(@Nullable UUID createdByAccountId) {
		this.createdByAccountId = createdByAccountId;
	}

	@Nullable
	public Boolean getCopyForEditing() {
		return copyForEditing;
	}

	public void setCopyForEditing(@Nullable Boolean copyForEditing) {
		this.copyForEditing = copyForEditing;
	}

	@Nullable
	public InstitutionId getInstitutionId() {
		return institutionId;
	}

	public void setInstitutionId(@Nullable InstitutionId institutionId) {
		this.institutionId = institutionId;
	}

	@Nullable
	public PageStatusId getPageStatusId() {
		return pageStatusId;
	}

	public void setPageStatusId(@Nullable PageStatusId pageStatusId) {
		this.pageStatusId = pageStatusId;
	}
}
