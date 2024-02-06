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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * @author Transmogrify LLC.
 */
@NotThreadSafe
public class MicrosoftTeamsMeeting {
	@Nullable
	private UUID microsoftTeamsMeetingId;
	@Nullable
	private Institution.InstitutionId institutionId;
	@Nullable
	private UUID createdByAccountId;
	@Nullable
	private String onlineMeetingId; // Microsoft identifier
	@Nullable
	private String joinUrl;
	@Nullable
	private LocalDateTime startDateTime;
	@Nullable
	private LocalDateTime endDateTime;
	@Nullable
	private ZoneId timeZone;
	@Nullable
	private String apiResponse; // JSON
	@Nonnull
	private Instant created;
	@Nonnull
	private Instant lastUpdated;

	@Nullable
	public UUID getMicrosoftTeamsMeetingId() {
		return this.microsoftTeamsMeetingId;
	}

	public void setMicrosoftTeamsMeetingId(@Nullable UUID microsoftTeamsMeetingId) {
		this.microsoftTeamsMeetingId = microsoftTeamsMeetingId;
	}

	@Nullable
	public Institution.InstitutionId getInstitutionId() {
		return this.institutionId;
	}

	public void setInstitutionId(@Nullable Institution.InstitutionId institutionId) {
		this.institutionId = institutionId;
	}

	@Nullable
	public UUID getCreatedByAccountId() {
		return this.createdByAccountId;
	}

	public void setCreatedByAccountId(@Nullable UUID createdByAccountId) {
		this.createdByAccountId = createdByAccountId;
	}

	@Nullable
	public String getOnlineMeetingId() {
		return this.onlineMeetingId;
	}

	public void setOnlineMeetingId(@Nullable String onlineMeetingId) {
		this.onlineMeetingId = onlineMeetingId;
	}

	@Nullable
	public String getJoinUrl() {
		return this.joinUrl;
	}

	public void setJoinUrl(@Nullable String joinUrl) {
		this.joinUrl = joinUrl;
	}

	@Nullable
	public LocalDateTime getStartDateTime() {
		return this.startDateTime;
	}

	public void setStartDateTime(@Nullable LocalDateTime startDateTime) {
		this.startDateTime = startDateTime;
	}

	@Nullable
	public LocalDateTime getEndDateTime() {
		return this.endDateTime;
	}

	public void setEndDateTime(@Nullable LocalDateTime endDateTime) {
		this.endDateTime = endDateTime;
	}

	@Nullable
	public ZoneId getTimeZone() {
		return this.timeZone;
	}

	public void setTimeZone(@Nullable ZoneId timeZone) {
		this.timeZone = timeZone;
	}

	@Nullable
	public String getApiResponse() {
		return this.apiResponse;
	}

	public void setApiResponse(@Nullable String apiResponse) {
		this.apiResponse = apiResponse;
	}

	@Nonnull
	public Instant getCreated() {
		return this.created;
	}

	public void setCreated(@Nonnull Instant created) {
		this.created = created;
	}

	@Nonnull
	public Instant getLastUpdated() {
		return this.lastUpdated;
	}

	public void setLastUpdated(@Nonnull Instant lastUpdated) {
		this.lastUpdated = lastUpdated;
	}
}