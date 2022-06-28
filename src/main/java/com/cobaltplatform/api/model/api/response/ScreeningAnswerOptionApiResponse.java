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

import com.cobaltplatform.api.model.db.ScreeningAnswerOption;
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
public class ScreeningAnswerOptionApiResponse {
	@Nonnull
	private final UUID screeningAnswerOptionId;
	@Nonnull
	private final UUID screeningQuestionId;
	@Nullable
	private final String answerOptionText;
	@Nonnull
	private final Integer displayOrder;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface ScreeningAnswerOptionApiResponseFactory {
		@Nonnull
		ScreeningAnswerOptionApiResponse create(@Nonnull ScreeningAnswerOption screeningAnswerOption);
	}

	@AssistedInject
	public ScreeningAnswerOptionApiResponse(@Nonnull Formatter formatter,
																					@Nonnull Strings strings,
																					@Assisted @Nonnull ScreeningAnswerOption screeningAnswerOption) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(screeningAnswerOption);

		this.screeningAnswerOptionId = screeningAnswerOption.getScreeningAnswerOptionId();
		this.screeningQuestionId = screeningAnswerOption.getScreeningQuestionId();
		this.answerOptionText = screeningAnswerOption.getAnswerOptionText();
		this.displayOrder = screeningAnswerOption.getDisplayOrder();
	}

	@Nonnull
	public UUID getScreeningAnswerOptionId() {
		return this.screeningAnswerOptionId;
	}

	@Nonnull
	public UUID getScreeningQuestionId() {
		return this.screeningQuestionId;
	}

	@Nullable
	public String getAnswerOptionText() {
		return this.answerOptionText;
	}

	@Nonnull
	public Integer getDisplayOrder() {
		return this.displayOrder;
	}
}