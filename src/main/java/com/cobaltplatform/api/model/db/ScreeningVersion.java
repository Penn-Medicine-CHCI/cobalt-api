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

import com.cobaltplatform.api.model.db.ScreeningType.ScreeningTypeId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class ScreeningVersion {
	@Nullable
	private UUID screeningVersionId;
	@Nullable
	private UUID screeningId;
	@Nullable
	private ScreeningTypeId screeningTypeId;
	@Nullable
	private UUID createdByAccountId;
	@Nullable
	private Integer versionNumber;
	@Nullable
	private String scoringFunction;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nullable
	public UUID getScreeningVersionId() {
		return this.screeningVersionId;
	}

	public void setScreeningVersionId(@Nullable UUID screeningVersionId) {
		this.screeningVersionId = screeningVersionId;
	}

	@Nullable
	public UUID getScreeningId() {
		return this.screeningId;
	}

	public void setScreeningId(@Nullable UUID screeningId) {
		this.screeningId = screeningId;
	}

	@Nullable
	public ScreeningTypeId getScreeningTypeId() {
		return this.screeningTypeId;
	}

	public void setScreeningTypeId(@Nullable ScreeningTypeId screeningTypeId) {
		this.screeningTypeId = screeningTypeId;
	}

	@Nullable
	public UUID getCreatedByAccountId() {
		return this.createdByAccountId;
	}

	public void setCreatedByAccountId(@Nullable UUID createdByAccountId) {
		this.createdByAccountId = createdByAccountId;
	}

	@Nullable
	public Integer getVersionNumber() {
		return this.versionNumber;
	}

	public void setVersionNumber(@Nullable Integer versionNumber) {
		this.versionNumber = versionNumber;
	}

	@Nullable
	public String getScoringFunction() {
		return this.scoringFunction;
	}

	public void setScoringFunction(@Nullable String scoringFunction) {
		this.scoringFunction = scoringFunction;
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
