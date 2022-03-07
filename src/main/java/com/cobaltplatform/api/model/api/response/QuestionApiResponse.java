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

import com.cobaltplatform.api.model.db.QuestionContentHint;
import com.cobaltplatform.api.model.db.QuestionContentHint.QuestionContentHintId;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.db.FontSize.FontSizeId;
import com.cobaltplatform.api.model.db.Question;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class QuestionApiResponse {
	@Nonnull
	private final UUID questionId;
	@Nonnull
	private final FontSizeId fontSizeId;
	@Nonnull
	private final QuestionContentHintId questionContentHintId;
	@Nullable
	private final String question;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface QuestionApiResponseFactory {
		@Nonnull
		QuestionApiResponse create(@Nonnull Question question);
	}

	@AssistedInject
	public QuestionApiResponse(@Nonnull Provider<CurrentContext> currentContextProvider,
														 @Assisted @Nonnull Question question) {
		requireNonNull(currentContextProvider);
		requireNonNull(question);

		this.questionId = question.getQuestionId();
		this.fontSizeId = question.getFontSizeId();
		this.questionContentHintId = question.getQuestionContentHintId();
		this.question = question.getQuestionText();
	}

	@Nonnull
	public UUID getQuestionId() {
		return questionId;
	}

	@Nonnull
	public FontSizeId getFontSizeId() {
		return fontSizeId;
	}

	@Nonnull
	public QuestionContentHintId getQuestionContentHintId() {
		return questionContentHintId;
	}

	@Nonnull
	public Optional<String> getQuestion() {
		return Optional.ofNullable(question);
	}
}