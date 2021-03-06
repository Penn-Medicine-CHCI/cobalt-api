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
public class Way2HealthIncident {
	@Nullable
	private UUID way2HealthIncidentTrackingId;
	@Nullable
	private InstitutionId institutionId;
	@Nullable
	private Long incidentId;
	@Nullable
	private Long studyId;
	@Nullable
	private String rawJson;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nullable
	public UUID getWay2HealthIncidentTrackingId() {
		return way2HealthIncidentTrackingId;
	}

	public void setWay2HealthIncidentTrackingId(@Nullable UUID way2HealthIncidentTrackingId) {
		this.way2HealthIncidentTrackingId = way2HealthIncidentTrackingId;
	}

	@Nullable
	public InstitutionId getInstitutionId() {
		return institutionId;
	}

	public void setInstitutionId(@Nullable InstitutionId institutionId) {
		this.institutionId = institutionId;
	}

	@Nullable
	public Long getIncidentId() {
		return incidentId;
	}

	public void setIncidentId(@Nullable Long incidentId) {
		this.incidentId = incidentId;
	}

	@Nullable
	public Long getStudyId() {
		return studyId;
	}

	public void setStudyId(@Nullable Long studyId) {
		this.studyId = studyId;
	}

	@Nullable
	public String getRawJson() {
		return rawJson;
	}

	public void setRawJson(@Nullable String rawJson) {
		this.rawJson = rawJson;
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
}