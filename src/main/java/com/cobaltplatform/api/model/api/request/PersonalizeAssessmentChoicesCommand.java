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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyList;

/**
 * @author Transmogrify LLC.
 */
public class PersonalizeAssessmentChoicesCommand {

	@Nullable
	private List<AssessmentSubmission> choices;

	@Nullable
	public List<AssessmentSubmission> getChoices() {
		return choices;
	}

	public PersonalizeAssessmentChoicesCommand(@Nullable List<AssessmentSubmission> choices) {
		this.choices = choices;
	}

	public static class AssessmentSubmission {
		@Nullable
		private UUID questionId;
		@Nullable
		private List<SubmissionAnswer> selectedAnswers;

		public AssessmentSubmission(@Nullable UUID questionId, @Nullable List<SubmissionAnswer> answers) {
			this.questionId = questionId;
			this.selectedAnswers = answers;
		}

		@Nullable
		public UUID getQuestionId() {
			return questionId;
		}

		@Nonnull
		public List<SubmissionAnswer> getSelectedAnswers() {
			if(selectedAnswers == null) return emptyList();
			return selectedAnswers;
		}
	}

	public static class SubmissionAnswer {

		@Nullable
		private UUID answerId;
		@Nullable
		private String answerText;

		public SubmissionAnswer(@Nullable UUID answerId) {
			this.answerId = answerId;
		}

		@Nullable
		public UUID getAnswerId() {
			return answerId;
		}

		@Nullable
		public String getAnswerText() {
			return answerText;
		}
	}
}
