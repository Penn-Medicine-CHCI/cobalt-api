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

package com.cobaltplatform.api.model.api.response;

import com.cobaltplatform.api.model.db.ScreeningSession;
import com.cobaltplatform.api.model.service.ScreeningQuestionContext;
import com.cobaltplatform.api.model.service.ScreeningQuestionContextId;
import com.cobaltplatform.api.model.service.ScreeningSessionDestination;
import com.cobaltplatform.api.service.ScreeningService;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class ScreeningSessionApiResponse {
	@Nonnull
	private final UUID screeningSessionId;
	@Nonnull
	private final UUID screeningFlowVersionId;
	@Nonnull
	private final UUID targetAccountId;
	@Nonnull
	private final UUID createdByAccountId;
	@Nonnull
	private final Boolean completed;
	@Nonnull
	private final Instant completedAt;
	@Nonnull
	private final String completedAtDescription;
	@Nonnull
	private final Boolean skipped;
	@Nonnull
	private final Instant skippedAt;
	@Nonnull
	private final String skippedAtDescription;
	@Nonnull
	private final Boolean crisisIndicated;
	@Nonnull
	private final Instant crisisIndicatedAt;
	@Nonnull
	private final String crisisIndicatedAtDescription;
	@Nonnull
	private final Instant created;
	@Nonnull
	private final String createdDescription;

	@Nullable
	private ScreeningQuestionContextId nextScreeningQuestionContextId;
	@Nullable
	private ScreeningSessionDestination screeningSessionDestination;

	public enum ScreeningSessionApiResponseSupplement {
		NEXT_QUESTION,
		RESULTS
	}

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface ScreeningSessionApiResponseFactory {
		@Nonnull
		ScreeningSessionApiResponse create(@Nonnull ScreeningSession screeningSession);

		@Nonnull
		ScreeningSessionApiResponse create(@Nonnull ScreeningSession screeningSession,
																			 @Nullable Set<ScreeningSessionApiResponseSupplement> supplements);
	}

	@AssistedInject
	public ScreeningSessionApiResponse(@Nonnull ScreeningService screeningService,
																		 @Nonnull Formatter formatter,
																		 @Nonnull Strings strings,
																		 @Assisted @Nonnull ScreeningSession screeningSession) {
		this(screeningService, formatter, strings, screeningSession, null);
	}

	@AssistedInject
	public ScreeningSessionApiResponse(@Nonnull ScreeningService screeningService,
																		 @Nonnull Formatter formatter,
																		 @Nonnull Strings strings,
																		 @Assisted @Nonnull ScreeningSession screeningSession,
																		 @Assisted @Nullable Set<ScreeningSessionApiResponseSupplement> supplements) {
		requireNonNull(screeningService);
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(screeningSession);

		if (supplements == null)
			supplements = Set.of();

		this.screeningSessionId = screeningSession.getScreeningSessionId();
		this.screeningFlowVersionId = screeningSession.getScreeningFlowVersionId();
		this.createdByAccountId = screeningSession.getCreatedByAccountId();
		this.targetAccountId = screeningSession.getTargetAccountId();
		this.completed = screeningSession.getCompleted();
		this.completedAt = screeningSession.getCompletedAt();
		this.completedAtDescription = screeningSession.getCompletedAt() == null ? null : formatter.formatTimestamp(screeningSession.getCompletedAt());
		this.skipped = screeningSession.getSkipped();
		this.skippedAt = screeningSession.getSkippedAt();
		this.skippedAtDescription = screeningSession.getSkippedAt() == null ? null : formatter.formatTimestamp(screeningSession.getSkippedAt());
		this.crisisIndicated = screeningSession.getCrisisIndicated();
		this.crisisIndicatedAt = screeningSession.getCrisisIndicatedAt();
		this.crisisIndicatedAtDescription = screeningSession.getCrisisIndicatedAt() == null ? null : formatter.formatTimestamp(screeningSession.getCrisisIndicatedAt());
		this.created = screeningSession.getCreated();
		this.createdDescription = formatter.formatTimestamp(screeningSession.getCreated());

		if (supplements.contains(ScreeningSessionApiResponseSupplement.NEXT_QUESTION)) {
			ScreeningQuestionContext nextScreeningQuestionContext =
					screeningService.findNextUnansweredScreeningQuestionContextByScreeningSessionId(screeningSessionId).orElse(null);

			this.nextScreeningQuestionContextId = nextScreeningQuestionContext == null ? null
					: nextScreeningQuestionContext.getScreeningQuestionContextId();

			this.screeningSessionDestination = screeningService.determineDestinationForScreeningSessionId(screeningSessionId).orElse(null);
		}

		if (supplements.contains(ScreeningSessionApiResponseSupplement.RESULTS)) {
			// TODO
		}
	}

	@Nonnull
	public UUID getScreeningSessionId() {
		return this.screeningSessionId;
	}

	@Nonnull
	public UUID getScreeningFlowVersionId() {
		return this.screeningFlowVersionId;
	}

	@Nonnull
	public UUID getTargetAccountId() {
		return this.targetAccountId;
	}

	@Nonnull
	public UUID getCreatedByAccountId() {
		return this.createdByAccountId;
	}

	@Nonnull
	public Boolean getCompleted() {
		return this.completed;
	}

	@Nonnull
	public Instant getCompletedAt() {
		return this.completedAt;
	}

	@Nonnull
	public String getCompletedAtDescription() {
		return this.completedAtDescription;
	}

	@Nonnull
	public Boolean getSkipped() {
		return this.skipped;
	}

	@Nonnull
	public Instant getSkippedAt() {
		return this.skippedAt;
	}

	@Nonnull
	public String getSkippedAtDescription() {
		return this.skippedAtDescription;
	}

	@Nonnull
	public Boolean getCrisisIndicated() {
		return this.crisisIndicated;
	}

	@Nonnull
	public Instant getCrisisIndicatedAt() {
		return this.crisisIndicatedAt;
	}

	@Nonnull
	public String getCrisisIndicatedAtDescription() {
		return this.crisisIndicatedAtDescription;
	}

	@Nonnull
	public Instant getCreated() {
		return this.created;
	}

	@Nonnull
	public String getCreatedDescription() {
		return this.createdDescription;
	}

	@Nullable
	public ScreeningQuestionContextId getNextScreeningQuestionContextId() {
		return this.nextScreeningQuestionContextId;
	}

	@Nullable
	public ScreeningSessionDestination getScreeningSessionDestination() {
		return this.screeningSessionDestination;
	}
}