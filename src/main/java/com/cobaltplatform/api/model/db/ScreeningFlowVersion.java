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

import com.cobaltplatform.api.model.db.ScreeningFlowSkipType.ScreeningFlowSkipTypeId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class ScreeningFlowVersion {
	@Nullable
	private UUID screeningFlowVersionId;
	@Nullable
	private UUID screeningFlowId;
	@Nullable
	private UUID initialScreeningId;
	@Nullable
	private UUID preCompletionScreeningConfirmationPromptId;
	@Nullable
	private ScreeningFlowSkipTypeId screeningFlowSkipTypeId;
	@Nullable
	private Boolean phoneNumberRequired;
	@Nullable
	private Boolean skippable;
	@Nullable
	private Integer versionNumber;
	@Nullable
	private String initializationFunction;
	@Nullable
	private String orchestrationFunction;
	@Nullable
	private String resultsFunction;
	@Nullable
	private String destinationFunction;
	@Nullable
	private UUID createdByAccountId;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;
	@Nullable
	private Integer minutesUntilRetake;
	@Nullable
	private Integer recommendationExpirationMinutes;

	@Nullable
	public UUID getScreeningFlowVersionId() {
		return this.screeningFlowVersionId;
	}

	public void setScreeningFlowVersionId(@Nullable UUID screeningFlowVersionId) {
		this.screeningFlowVersionId = screeningFlowVersionId;
	}

	@Nullable
	public UUID getScreeningFlowId() {
		return this.screeningFlowId;
	}

	public void setScreeningFlowId(@Nullable UUID screeningFlowId) {
		this.screeningFlowId = screeningFlowId;
	}

	@Nullable
	public UUID getInitialScreeningId() {
		return this.initialScreeningId;
	}

	public void setInitialScreeningId(@Nullable UUID initialScreeningId) {
		this.initialScreeningId = initialScreeningId;
	}

	@Nullable
	public UUID getPreCompletionScreeningConfirmationPromptId() {
		return this.preCompletionScreeningConfirmationPromptId;
	}

	public void setPreCompletionScreeningConfirmationPromptId(@Nullable UUID preCompletionScreeningConfirmationPromptId) {
		this.preCompletionScreeningConfirmationPromptId = preCompletionScreeningConfirmationPromptId;
	}

	@Nullable
	public ScreeningFlowSkipTypeId getScreeningFlowSkipTypeId() {
		return this.screeningFlowSkipTypeId;
	}

	public void setScreeningFlowSkipTypeId(@Nullable ScreeningFlowSkipTypeId screeningFlowSkipTypeId) {
		this.screeningFlowSkipTypeId = screeningFlowSkipTypeId;
	}

	@Nullable
	public Boolean getPhoneNumberRequired() {
		return this.phoneNumberRequired;
	}

	public void setPhoneNumberRequired(@Nullable Boolean phoneNumberRequired) {
		this.phoneNumberRequired = phoneNumberRequired;
	}

	@Nullable
	public Boolean getSkippable() {
		return this.skippable;
	}

	public void setSkippable(@Nullable Boolean skippable) {
		this.skippable = skippable;
	}

	@Nullable
	public Integer getVersionNumber() {
		return this.versionNumber;
	}

	public void setVersionNumber(@Nullable Integer versionNumber) {
		this.versionNumber = versionNumber;
	}

	@Nullable
	public String getInitializationFunction() {
		return this.initializationFunction;
	}

	public void setInitializationFunction(@Nullable String initializationFunction) {
		this.initializationFunction = initializationFunction;
	}

	@Nullable
	public String getOrchestrationFunction() {
		return this.orchestrationFunction;
	}

	public void setOrchestrationFunction(@Nullable String orchestrationFunction) {
		this.orchestrationFunction = orchestrationFunction;
	}

	@Nullable
	public String getResultsFunction() {
		return this.resultsFunction;
	}

	public void setResultsFunction(@Nullable String resultsFunction) {
		this.resultsFunction = resultsFunction;
	}

	@Nullable
	public String getDestinationFunction() {
		return this.destinationFunction;
	}

	public void setDestinationFunction(@Nullable String destinationFunction) {
		this.destinationFunction = destinationFunction;
	}

	@Nullable
	public UUID getCreatedByAccountId() {
		return this.createdByAccountId;
	}

	public void setCreatedByAccountId(@Nullable UUID createdByAccountId) {
		this.createdByAccountId = createdByAccountId;
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
	public Integer getMinutesUntilRetake() {
		return minutesUntilRetake;
	}

	public void setMinutesUntilRetake(@Nullable Integer minutesUntilRetake) {
		this.minutesUntilRetake = minutesUntilRetake;
	}

	@Nullable
	public Integer getRecommendationExpirationMinutes() {
		return recommendationExpirationMinutes;
	}

	public void setRecommendationExpirationMinutes(@Nullable Integer recommendationExpirationMinutes) {
		this.recommendationExpirationMinutes = recommendationExpirationMinutes;
	}
}
