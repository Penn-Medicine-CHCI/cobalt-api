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
import java.time.LocalTime;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class EpicProviderSchedule {
	@Nullable
	private UUID epicProviderScheduleId;
	@Nullable
	private InstitutionId institutionId;
	@Nullable
	private String name;
	@Nullable
	private LocalTime startTime;
	@Nullable
	private LocalTime endTime;
	@Nullable
	private Integer npvDurationInMinutes;
	@Nullable
	private Integer maximumNpvCount;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nullable
	public UUID getEpicProviderScheduleId() {
		return this.epicProviderScheduleId;
	}

	public void setEpicProviderScheduleId(@Nullable UUID epicProviderScheduleId) {
		this.epicProviderScheduleId = epicProviderScheduleId;
	}

	@Nullable
	public InstitutionId getInstitutionId() {
		return this.institutionId;
	}

	public void setInstitutionId(@Nullable InstitutionId institutionId) {
		this.institutionId = institutionId;
	}

	@Nullable
	public String getName() {
		return this.name;
	}

	public void setName(@Nullable String name) {
		this.name = name;
	}

	@Nullable
	public LocalTime getStartTime() {
		return this.startTime;
	}

	public void setStartTime(@Nullable LocalTime startTime) {
		this.startTime = startTime;
	}

	@Nullable
	public LocalTime getEndTime() {
		return this.endTime;
	}

	public void setEndTime(@Nullable LocalTime endTime) {
		this.endTime = endTime;
	}

	@Nullable
	public Integer getNpvDurationInMinutes() {
		return this.npvDurationInMinutes;
	}

	public void setNpvDurationInMinutes(@Nullable Integer npvDurationInMinutes) {
		this.npvDurationInMinutes = npvDurationInMinutes;
	}

	@Nullable
	public Integer getMaximumNpvCount() {
		return this.maximumNpvCount;
	}

	public void setMaximumNpvCount(@Nullable Integer maximumNpvCount) {
		this.maximumNpvCount = maximumNpvCount;
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