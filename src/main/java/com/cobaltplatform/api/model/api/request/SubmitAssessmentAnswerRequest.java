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

import static java.util.Collections.emptyList;

/**
 * @author Transmogrify LLC.
 */
public class SubmitAssessmentAnswerRequest {
	@Nonnull
	private String questionId;
	@Nullable
	private List<AnswerRequest> assessmentAnswers;
	@Nullable
	private List<String> answers;
	@Nonnull
	private String sessionId;

	public SubmitAssessmentAnswerRequest(@Nonnull String questionId,
																			 @Nullable List<AnswerRequest> assessmentAnswers,
																			 @Nullable List<String> answers,
																			 @Nonnull String sessionId) {
		this.questionId = questionId;
		this.assessmentAnswers = assessmentAnswers;
		this.answers = answers;
		this.sessionId = sessionId;
	}

	@Nonnull
	public String getQuestionId() {
		return questionId;
	}

	@Nonnull
	public List<AnswerRequest> getAssessmentAnswers() {
		if(assessmentAnswers == null) return emptyList();
		return assessmentAnswers;
	}

	@Nonnull
	public List<String> getAnswers(){
		if(answers == null) return emptyList();
		return answers;
	}

	@Nonnull
	public String getSessionId() {
		return sessionId;
	}

	public static class AnswerRequest{
		@Nonnull
		private String answerId;
		@Nullable
		private String answerText;

		public AnswerRequest(@Nonnull String answerId, @Nullable String answerText) {
			this.answerId = answerId;
			this.answerText = answerText;
		}

		@Nonnull
		public String getAnswerId() {
			return answerId;
		}

		@Nullable
		public String getAnswerText() {
			return answerText;
		}

		public void setAnswerText(@Nullable String answerText) {
			this.answerText = answerText;
		}
	}
}
