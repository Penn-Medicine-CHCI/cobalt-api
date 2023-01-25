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

import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.InstitutionBlurbType.InstitutionBlurbTypeId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class InstitutionBlurb {
	@Nullable
	private UUID institutionBlurbId;
	@Nullable
	private InstitutionId institutionId;
	@Nullable
	private InstitutionBlurbTypeId institutionBlurbTypeId;
	@Nullable
	private String title;
	@Nullable
	private String description;
	@Nullable
	private String shortDescription;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nullable
	public UUID getInstitutionBlurbId() {
		return this.institutionBlurbId;
	}

	public void setInstitutionBlurbId(@Nullable UUID institutionBlurbId) {
		this.institutionBlurbId = institutionBlurbId;
	}

	@Nullable
	public InstitutionId getInstitutionId() {
		return this.institutionId;
	}

	public void setInstitutionId(@Nullable InstitutionId institutionId) {
		this.institutionId = institutionId;
	}

	@Nullable
	public InstitutionBlurbTypeId getInstitutionBlurbTypeId() {
		return this.institutionBlurbTypeId;
	}

	public void setInstitutionBlurbTypeId(@Nullable InstitutionBlurbTypeId institutionBlurbTypeId) {
		this.institutionBlurbTypeId = institutionBlurbTypeId;
	}

	@Nullable
	public String getTitle() {
		return this.title;
	}

	public void setTitle(@Nullable String title) {
		this.title = title;
	}

	@Nullable
	public String getDescription() {
		return this.description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
	}

	@Nullable
	public String getShortDescription() {
		return this.shortDescription;
	}

	public void setShortDescription(@Nullable String shortDescription) {
		this.shortDescription = shortDescription;
	}

	@Nullable
	public Instant getCreated() {
		return this.created;
	}

	public void setCreated(@Nullable Instant created) {
		this.created = created;
	}

	@Nullable
	public Instant getLastUpdated() {
		return this.lastUpdated;
	}

	public void setLastUpdated(@Nullable Instant lastUpdated) {
		this.lastUpdated = lastUpdated;
	}
}