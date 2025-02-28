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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.pyranid.DatabaseColumn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class ScreeningSession {
	@Nonnull
	private static final Gson GSON;

	static {
		GsonBuilder gsonBuilder = new GsonBuilder()
				.setPrettyPrinting()
				.disableHtmlEscaping();

		GSON = gsonBuilder.create();
	}

	@Nullable
	private UUID screeningSessionId;
	@Nullable
	private UUID screeningFlowVersionId;
	@Nullable
	private UUID targetAccountId;
	@Nullable
	private UUID patientOrderId;
	@Nullable
	private UUID groupSessionId;
	@Nullable
	private UUID courseSessionId;
	@Nullable
	private UUID createdByAccountId;
	@Nullable
	private Boolean completed;
	@Nullable
	private Instant completedAt;
	@Nullable
	private Boolean skipped;
	@Nullable
	private Instant skippedAt;
	@Nullable
	private Boolean crisisIndicated;
	@Nullable
	private Instant crisisIndicatedAt;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;
	@Nullable
	private UUID accountCheckInActionId;
	@Nullable
	@DatabaseColumn("metadata")
	private String metadataAsJson;

	@Nonnull
	public static Optional<String> metadataToJson(@Nullable Map<String, Object> metadata) {
		if (metadata == null)
			return Optional.empty();

		return Optional.of(GSON.toJson(metadata));
	}

	@Nonnull
	public Map<String, Object> getMetadata() {
		return this.metadataAsJson == null ? Map.of() : GSON.fromJson(this.metadataAsJson, new TypeToken<Map<String, Object>>() {
		}.getType());
	}

	@Nullable
	public UUID getScreeningSessionId() {
		return this.screeningSessionId;
	}

	public void setScreeningSessionId(@Nullable UUID screeningSessionId) {
		this.screeningSessionId = screeningSessionId;
	}

	@Nullable
	public UUID getScreeningFlowVersionId() {
		return this.screeningFlowVersionId;
	}

	public void setScreeningFlowVersionId(@Nullable UUID screeningFlowVersionId) {
		this.screeningFlowVersionId = screeningFlowVersionId;
	}

	@Nullable
	public UUID getTargetAccountId() {
		return this.targetAccountId;
	}

	public void setTargetAccountId(@Nullable UUID targetAccountId) {
		this.targetAccountId = targetAccountId;
	}

	@Nullable
	public UUID getPatientOrderId() {
		return this.patientOrderId;
	}

	public void setPatientOrderId(@Nullable UUID patientOrderId) {
		this.patientOrderId = patientOrderId;
	}

	@Nullable
	public UUID getGroupSessionId() {
		return this.groupSessionId;
	}

	public void setGroupSessionId(@Nullable UUID groupSessionId) {
		this.groupSessionId = groupSessionId;
	}

	@Nullable
	public UUID getCourseSessionId() {
		return this.courseSessionId;
	}

	public void setCourseSessionId(@Nullable UUID courseSessionId) {
		this.courseSessionId = courseSessionId;
	}

	@Nullable
	public UUID getCreatedByAccountId() {
		return this.createdByAccountId;
	}

	public void setCreatedByAccountId(@Nullable UUID createdByAccountId) {
		this.createdByAccountId = createdByAccountId;
	}

	@Nullable
	public Boolean getCompleted() {
		return this.completed;
	}

	public void setCompleted(@Nullable Boolean completed) {
		this.completed = completed;
	}

	@Nullable
	public Instant getCompletedAt() {
		return this.completedAt;
	}

	public void setCompletedAt(@Nullable Instant completedAt) {
		this.completedAt = completedAt;
	}

	@Nullable
	public Boolean getSkipped() {
		return this.skipped;
	}

	public void setSkipped(@Nullable Boolean skipped) {
		this.skipped = skipped;
	}

	@Nullable
	public Instant getSkippedAt() {
		return this.skippedAt;
	}

	public void setSkippedAt(@Nullable Instant skippedAt) {
		this.skippedAt = skippedAt;
	}

	@Nullable
	public Boolean getCrisisIndicated() {
		return this.crisisIndicated;
	}

	public void setCrisisIndicated(@Nullable Boolean crisisIndicated) {
		this.crisisIndicated = crisisIndicated;
	}

	@Nullable
	public Instant getCrisisIndicatedAt() {
		return this.crisisIndicatedAt;
	}

	public void setCrisisIndicatedAt(@Nullable Instant crisisIndicatedAt) {
		this.crisisIndicatedAt = crisisIndicatedAt;
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

	@Nullable
	public UUID getAccountCheckInActionId() {
		return accountCheckInActionId;
	}

	public void setAccountCheckInActionId(@Nullable UUID accountCheckInActionId) {
		this.accountCheckInActionId = accountCheckInActionId;
	}

	@Nullable
	public String getMetadataAsJson() {
		return this.metadataAsJson;
	}

	public void setMetadataAsJson(@Nullable String metadataAsJson) {
		this.metadataAsJson = metadataAsJson;
	}
}
