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

import com.cobaltplatform.api.model.db.PageStatus.PageStatusId;
import com.cobaltplatform.api.model.db.PageType.PageTypeId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class CreatePageRequest {

	@Nullable
	private String name;
	@Nullable
	private String urlName;
	@Nullable
	private PageTypeId pageTypeId;
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
	private UUID createdByAccountId;

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
	public PageTypeId getPageTypeId() {
		return pageTypeId;
	}

	public void setPageTypeId(@Nullable PageTypeId pageTypeId) {
		this.pageTypeId = pageTypeId;
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
	public UUID getCreatedByAccountId() {
		return createdByAccountId;
	}

	public void setCreatedByAccountId(@Nullable UUID createdByAccountId) {
		this.createdByAccountId = createdByAccountId;
	}
}
