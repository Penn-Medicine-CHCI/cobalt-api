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

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify LLC.
 */
public class QuestionType {
	public enum QuestionTypeId {
		HORIZONTAL_CHECKBOX,
		CHECKBOX,
		QUAD,
		RADIO,
		DROPDOWN,
		TEXT,
		DATE,
		STUDENT_ID,  // TODO: replace with QuestionContentHintId value in the future
		PHONE_NUMBER; // TODO: replace with QuestionContentHintId value in the future

		@Nonnull
		public static Boolean isFreeform(@Nonnull QuestionTypeId questionTypeId) {
			requireNonNull(questionTypeId);
			return questionTypeId == TEXT || questionTypeId == DATE || questionTypeId == STUDENT_ID || questionTypeId == PHONE_NUMBER;
		}

		@Nonnull
		public Boolean isFreeform() {
			return QuestionTypeId.isFreeform(this);
		}
	}

	@Nonnull
	private QuestionTypeId questionTypeId;
	@Nonnull
	private String description;
	@Nonnull
	private Boolean allowMultipleAnswers;
	@Nonnull
	private Boolean requiresTextResponse;

	@Nonnull
	public QuestionTypeId getQuestionTypeId() {
		return questionTypeId;
	}

	public void setQuestionTypeId(@Nonnull QuestionTypeId questionTypeId) {
		this.questionTypeId = questionTypeId;
	}

	@Nonnull
	public String getDescription() {
		return description;
	}

	public void setDescription(@Nonnull String description) {
		this.description = description;
	}

	@Nonnull
	public Boolean getAllowMultipleAnswers() {
		return allowMultipleAnswers;
	}

	public void setAllowMultipleAnswers(@Nonnull Boolean allowMultipleAnswers) {
		this.allowMultipleAnswers = allowMultipleAnswers;
	}

	@Nonnull
	public Boolean getRequiresTextResponse() {
		return requiresTextResponse;
	}

	public void setRequiresTextResponse(@Nonnull Boolean requiresTextResponse) {
		this.requiresTextResponse = requiresTextResponse;
	}
}
