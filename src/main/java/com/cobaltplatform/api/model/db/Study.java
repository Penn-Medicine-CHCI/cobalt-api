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

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Study {
	@Nullable
	private UUID studyId;
	@Nullable
	private InstitutionId institutionId;
	@Nullable
	private String name;
	@Nullable
	private String urlName;
	@Nullable
	private String onboardingDestinationUrl;
	@Nullable
	private Integer minutesBetweenCheckIns;
	@Nullable
	private Integer gracePeriodInMinutes;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nullable
	public UUID getStudyId() {
		return studyId;
	}

	public void setStudyId(@Nullable UUID studyId) {
		this.studyId = studyId;
	}

	@Nullable
	public String getName() {
		return this.name;
	}

	public void setName(@Nullable String name) {
		this.name = name;
	}

	@Nullable
	public String getUrlName() {
		return this.urlName;
	}

	public void setUrlName(@Nullable String urlName) {
		this.urlName = urlName;
	}

	@Nullable
	public String getOnboardingDestinationUrl() {
		return this.onboardingDestinationUrl;
	}

	public void setOnboardingDestinationUrl(@Nullable String onboardingDestinationUrl) {
		this.onboardingDestinationUrl = onboardingDestinationUrl;
	}

	@Nullable
	public Integer getMinutesBetweenCheckIns() {
		return minutesBetweenCheckIns;
	}

	public void setMinutesBetweenCheckIns(@Nullable Integer minutesBetweenCheckIns) {
		this.minutesBetweenCheckIns = minutesBetweenCheckIns;
	}

	@Nullable
	public Integer getGracePeriodInMinutes() {
		return gracePeriodInMinutes;
	}

	public void setGracePeriodInMinutes(@Nullable Integer gracePeriodInMinutes) {
		this.gracePeriodInMinutes = gracePeriodInMinutes;
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
	public InstitutionId getInstitutionId() {
		return institutionId;
	}

	public void setInstitutionId(@Nullable InstitutionId institutionId) {
		this.institutionId = institutionId;
	}
}