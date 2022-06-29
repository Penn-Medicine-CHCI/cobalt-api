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

package com.cobaltplatform.api.model.api.request;

import com.cobaltplatform.api.model.service.ScreeningQuestionContextId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class CreateScreeningAnswersRequest {
	@Nullable
	private ScreeningQuestionContextId screeningQuestionContextId;
	@Nullable
	private UUID createdByAccountId;
	@Nullable
	private List<CreateAnswerRequest> answers;

	@Nullable
	public ScreeningQuestionContextId getScreeningQuestionContextId() {
		return this.screeningQuestionContextId;
	}

	public void setScreeningQuestionContextId(@Nullable ScreeningQuestionContextId screeningQuestionContextId) {
		this.screeningQuestionContextId = screeningQuestionContextId;
	}

	@Nullable
	public UUID getCreatedByAccountId() {
		return this.createdByAccountId;
	}

	public void setCreatedByAccountId(@Nullable UUID createdByAccountId) {
		this.createdByAccountId = createdByAccountId;
	}

	@Nullable
	public List<CreateAnswerRequest> getAnswers() {
		return this.answers;
	}

	public void setAnswers(@Nullable List<CreateAnswerRequest> answers) {
		this.answers = answers;
	}

	@NotThreadSafe
	public static class CreateAnswerRequest {
		@Nullable
		private UUID screeningAnswerOptionId;
		@Nullable
		private String text;

		@Nullable
		public UUID getScreeningAnswerOptionId() {
			return this.screeningAnswerOptionId;
		}

		public void setScreeningAnswerOptionId(@Nullable UUID screeningAnswerOptionId) {
			this.screeningAnswerOptionId = screeningAnswerOptionId;
		}

		@Nullable
		public String getText() {
			return this.text;
		}

		public void setText(@Nullable String text) {
			this.text = text;
		}
	}
}
