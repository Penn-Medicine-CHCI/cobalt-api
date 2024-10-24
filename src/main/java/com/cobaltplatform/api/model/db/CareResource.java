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

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class CareResource {
	@Nullable
	private UUID careResourceId;
	@Nullable
	private String name;
	@Nullable
	private String notes;
	@Nullable
	private String insuranceNotes;
	@Nullable
	private String phoneNumber;
	@Nullable
	private String websiteUrl;
	@Nullable
	private Boolean resourceAvailable;
	@Nullable
	private UUID createdByAccountId;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;


	@Nullable
	public UUID getCareResourceId() {
		return careResourceId;
	}

	public void setCareResourceId(@Nullable UUID careResourceId) {
		this.careResourceId = careResourceId;
	}

	@Nullable
	public String getName() {
		return name;
	}

	public void setName(@Nullable String name) {
		this.name = name;
	}

	@Nullable
	public String getNotes() {
		return notes;
	}

	public void setNotes(@Nullable String notes) {
		this.notes = notes;
	}

	@Nullable
	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(@Nullable String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	@Nullable
	public String getWebsiteUrl() {
		return websiteUrl;
	}

	public void setWebsiteUrl(@Nullable String websiteUrl) {
		this.websiteUrl = websiteUrl;
	}

	@Nullable
	public Boolean getResourceAvailable() {
		return resourceAvailable;
	}

	public void setResourceAvailable(@Nullable Boolean resourceAvailable) {
		this.resourceAvailable = resourceAvailable;
	}

	@Nullable
	public Instant getCreated() {
		return created;
	}

	public void setCreated(@Nullable Instant created) {
		this.created = created;
	}

	@Nullable
	public Instant getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(@Nullable Instant lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	@Nullable
	public UUID getCreatedByAccountId() {
		return createdByAccountId;
	}

	public void setCreatedByAccountId(@Nullable UUID createdByAccountId) {
		this.createdByAccountId = createdByAccountId;
	}

	@Nullable
	public String getInsuranceNotes() {
		return insuranceNotes;
	}

	public void setInsuranceNotes(@Nullable String insuranceNotes) {
		this.insuranceNotes = insuranceNotes;
	}
}