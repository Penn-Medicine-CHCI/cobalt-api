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

import com.cobaltplatform.api.model.db.ScreeningAnswerContentHint.ScreeningAnswerContentHintId;
import com.cobaltplatform.api.model.db.ScreeningAnswerFormat.ScreeningAnswerFormatId;
import com.cobaltplatform.api.model.db.ScreeningQuestion;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class ScreeningQuestionApiResponse {
	@Nonnull
	private final UUID screeningQuestionId;
	@Nonnull
	private final UUID screeningVersionId;
	@Nonnull
	private final ScreeningAnswerFormatId screeningAnswerFormatId;
	@Nonnull
	private final ScreeningAnswerContentHintId screeningAnswerContentHintId;
	@Nullable
	private final String introText;
	@Nonnull
	private final String questionText;
	@Nonnull
	private final Integer displayOrder;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface ScreeningQuestionApiResponseFactory {
		@Nonnull
		ScreeningQuestionApiResponse create(@Nonnull ScreeningQuestion screeningQuestion);
	}

	@AssistedInject
	public ScreeningQuestionApiResponse(@Nonnull Formatter formatter,
																			@Nonnull Strings strings,
																			@Assisted @Nonnull ScreeningQuestion screeningQuestion) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(screeningQuestion);

		this.screeningQuestionId = screeningQuestion.getScreeningQuestionId();
		this.screeningVersionId = screeningQuestion.getScreeningVersionId();
		this.screeningAnswerFormatId = screeningQuestion.getScreeningAnswerFormatId();
		this.screeningAnswerContentHintId = screeningQuestion.getScreeningAnswerContentHintId();
		this.questionText = screeningQuestion.getQuestionText();
		this.introText = screeningQuestion.getIntroText();
		this.displayOrder = screeningQuestion.getDisplayOrder();
	}

	@Nonnull
	public UUID getScreeningQuestionId() {
		return this.screeningQuestionId;
	}

	@Nonnull
	public UUID getScreeningVersionId() {
		return this.screeningVersionId;
	}

	@Nonnull
	public ScreeningAnswerFormatId getScreeningAnswerFormatId() {
		return this.screeningAnswerFormatId;
	}

	@Nonnull
	public ScreeningAnswerContentHintId getScreeningAnswerContentHintId() {
		return this.screeningAnswerContentHintId;
	}

	@Nullable
	public String getIntroText() {
		return this.introText;
	}

	@Nonnull
	public String getQuestionText() {
		return this.questionText;
	}

	@Nonnull
	public Integer getDisplayOrder() {
		return this.displayOrder;
	}
}