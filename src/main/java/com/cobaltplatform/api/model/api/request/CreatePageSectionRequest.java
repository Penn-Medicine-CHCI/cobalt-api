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

import com.cobaltplatform.api.model.db.BackgroundColor.BackgroundColorId;
import com.cobaltplatform.api.model.db.Institution;


import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class CreatePageSectionRequest {
	@Nullable
	private UUID pageId;
	@Nullable
	private String name;
	@Nullable
	private String headline;
	@Nullable
	private String description;
	@Nullable
	private BackgroundColorId backgroundColorId;
	@Nullable
	private UUID createdByAccountId;
	@Nullable
	private Institution.InstitutionId institutionId;
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
	public BackgroundColorId getBackgroundColorId() {
		return backgroundColorId;
	}

	public void setBackgroundColorId(@Nullable BackgroundColorId backgroundColorId) {
		this.backgroundColorId = backgroundColorId;
	}

	@Nullable
	public UUID getCreatedByAccountId() {
		return createdByAccountId;
	}

	public void setCreatedByAccountId(@Nullable UUID createdByAccountId) {
		this.createdByAccountId = createdByAccountId;
	}

	@Nullable
	public Institution.InstitutionId getInstitutionId() {
		return institutionId;
	}

	public void setInstitutionId(@Nullable Institution.InstitutionId institutionId) {
		this.institutionId = institutionId;
	}
}
