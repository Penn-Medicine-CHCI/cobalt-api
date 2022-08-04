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

package com.cobaltplatform.api.model.service;

import com.cobaltplatform.api.model.db.ScreeningAnswer;
import com.cobaltplatform.api.model.db.ScreeningAnswerOption;
import com.cobaltplatform.api.model.db.ScreeningQuestion;
import com.cobaltplatform.api.model.db.ScreeningSessionScreening;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class ScreeningQuestionContext {
	@Nonnull
	private final ScreeningQuestionContextId screeningQuestionContextId;
	@Nonnull
	private final ScreeningSessionScreening screeningSessionScreening;
	@Nonnull
	private final ScreeningQuestion screeningQuestion;
	@Nonnull
	private final List<ScreeningAnswerOption> screeningAnswerOptions;
	@Nonnull
	private final List<ScreeningAnswer> screeningAnswers;

	public ScreeningQuestionContext(@Nonnull ScreeningSessionScreening screeningSessionScreening,
																	@Nonnull ScreeningQuestion screeningQuestion,
																	@Nonnull List<ScreeningAnswerOption> screeningAnswerOptions) {
		this(screeningSessionScreening, screeningQuestion, screeningAnswerOptions, null);
	}

	public ScreeningQuestionContext(@Nonnull ScreeningSessionScreening screeningSessionScreening,
																	@Nonnull ScreeningQuestion screeningQuestion,
																	@Nonnull List<ScreeningAnswerOption> screeningAnswerOptions,
																	@Nullable List<ScreeningAnswer> screeningAnswers) {
		requireNonNull(screeningSessionScreening);
		requireNonNull(screeningQuestion);
		requireNonNull(screeningAnswerOptions);

		this.screeningQuestionContextId = new ScreeningQuestionContextId(screeningSessionScreening.getScreeningSessionScreeningId(), screeningQuestion.getScreeningQuestionId());
		this.screeningSessionScreening = screeningSessionScreening;
		this.screeningAnswerOptions = screeningAnswerOptions;
		this.screeningQuestion = screeningQuestion;
		this.screeningAnswers = screeningAnswers == null ? List.of() : screeningAnswers;
	}

	@Nonnull
	public ScreeningQuestionContextId getScreeningQuestionContextId() {
		return this.screeningQuestionContextId;
	}

	@Nonnull
	public ScreeningSessionScreening getScreeningSessionScreening() {
		return this.screeningSessionScreening;
	}

	@Nonnull
	public ScreeningQuestion getScreeningQuestion() {
		return this.screeningQuestion;
	}

	@Nonnull
	public List<ScreeningAnswerOption> getScreeningAnswerOptions() {
		return this.screeningAnswerOptions;
	}

	@Nonnull
	public List<ScreeningAnswer> getScreeningAnswers() {
		return this.screeningAnswers;
	}
}
