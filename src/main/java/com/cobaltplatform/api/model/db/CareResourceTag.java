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

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class CareResourceTag {
	@Nullable
	private String careResourceTagId;
	@Nullable
	private String name;
	@Nullable
	private CareResourceTagGroupId careResourceTagGroupId;
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	public enum CareResourceTagGroupId {
		PAYORS,
		SPECIALTIES,
		THERAPY_TYPES,
		GENDERS,
		ETHNICITIES,
		LANGUAGES,
		POPULATION_SERVED
	}

	@Nullable
	public String getCareResourceTagId() {
		return careResourceTagId;
	}

	public void setCareResourceTagId(@Nullable String careResourceTagId) {
		this.careResourceTagId = careResourceTagId;
	}

	@Nullable
	public String getName() {
		return name;
	}

	public void setName(@Nullable String name) {
		this.name = name;
	}

	@Nullable
	public CareResourceTagGroupId getCareResourceTagGroupId() {
		return careResourceTagGroupId;
	}

	public void setCareResourceTagGroupId(@Nullable CareResourceTagGroupId careResourceTagGroupId) {
		this.careResourceTagGroupId = careResourceTagGroupId;
	}

	public Instant getCreated() {
		return created;
	}

	public void setCreated(Instant created) {
		this.created = created;
	}

	@Nullable
	public Instant getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(@Nullable Instant lastUpdated) {
		this.lastUpdated = lastUpdated;
	}
}
