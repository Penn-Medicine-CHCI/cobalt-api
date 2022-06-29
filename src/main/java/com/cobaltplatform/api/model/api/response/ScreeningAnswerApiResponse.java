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

import com.cobaltplatform.api.model.db.ScreeningAnswer;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class ScreeningAnswerApiResponse {
	@Nonnull
	private final UUID screeningAnswerId;
	@Nonnull
	private final UUID screeningAnswerOptionId;
	@Nonnull
	private final UUID screeningSessionAnsweredScreeningQuestionId;
	@Nonnull
	private final UUID createdByAccountId;
	@Nullable
	private final String text;
	@Nonnull
	private final Instant created;
	@Nonnull
	private final String createdDescription;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface ScreeningAnswerApiResponseFactory {
		@Nonnull
		ScreeningAnswerApiResponse create(@Nonnull ScreeningAnswer screeningAnswer);
	}

	@AssistedInject
	public ScreeningAnswerApiResponse(@Nonnull Formatter formatter,
																		@Nonnull Strings strings,
																		@Assisted @Nonnull ScreeningAnswer screeningAnswer) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(screeningAnswer);

		this.screeningAnswerId = screeningAnswer.getScreeningAnswerId();
		this.screeningAnswerOptionId = screeningAnswer.getScreeningAnswerOptionId();
		this.screeningSessionAnsweredScreeningQuestionId = screeningAnswer.getScreeningSessionAnsweredScreeningQuestionId();
		this.createdByAccountId = screeningAnswer.getCreatedByAccountId();
		this.text = screeningAnswer.getText();
		this.created = screeningAnswer.getCreated();
		this.createdDescription = formatter.formatTimestamp(screeningAnswer.getCreated());
	}

	@Nonnull
	public UUID getScreeningAnswerId() {
		return this.screeningAnswerId;
	}

	@Nonnull
	public UUID getScreeningAnswerOptionId() {
		return this.screeningAnswerOptionId;
	}

	@Nonnull
	public UUID getScreeningSessionAnsweredScreeningQuestionId() {
		return this.screeningSessionAnsweredScreeningQuestionId;
	}

	@Nonnull
	public UUID getCreatedByAccountId() {
		return this.createdByAccountId;
	}

	@Nullable
	public String getText() {
		return this.text;
	}

	@Nonnull
	public Instant getCreated() {
		return this.created;
	}

	@Nonnull
	public String getCreatedDescription() {
		return this.createdDescription;
	}
}