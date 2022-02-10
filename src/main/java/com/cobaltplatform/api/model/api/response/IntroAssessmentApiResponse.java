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

import com.cobaltplatform.api.model.db.AccountSession;
import com.cobaltplatform.api.model.db.Answer;
import com.cobaltplatform.api.model.db.Question;
import com.cobaltplatform.api.service.AssessmentService;
import com.cobaltplatform.api.service.SessionService;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Comparator;
import java.util.List;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * @author Transmogrify LLC.
 */
@ThreadSafe
public class IntroAssessmentApiResponse {

	@Nullable
	private List<AssessmentResponse> questions = null;

	@ThreadSafe
	public interface IntroAssessmentApiResponseFactory {
		@Nonnull
		IntroAssessmentApiResponse create(@Nonnull AccountSession accountSession);
	}

	@Nullable
	public List<AssessmentResponse> getQuestions() {
		return questions;
	}

	@AssistedInject
	public IntroAssessmentApiResponse(@Nonnull AssessmentService assessmentService,
																		@Nonnull SessionService sessionService,
																		@Assisted @Nonnull AccountSession accountSession) {
		List<Question> assessmentQuestions = assessmentService.findQuestionsForAssessmentId(accountSession.getAssessmentId());
		List<Answer> answers = sessionService.findAnswersForSession(accountSession);

		questions = assessmentQuestions.stream().map(question -> {
			String questionTitle = question.getQuestionText();
			String answerText = answers.stream()
					.sorted(Comparator.comparing(Answer::getDisplayOrder))
					.filter(a -> a.getQuestionId().equals(question.getQuestionId()))
					.map(Answer::getAnswerText)
					.collect(joining(", "));
			return new AssessmentResponse(questionTitle, answerText);
		}).collect(toList());

	}

	public static class AssessmentResponse {

		private final String question;
		private final String responses;

		public AssessmentResponse(String question, String responses) {

			this.question = question;
			this.responses = responses;
		}

		public String getQuestion() {
			return question;
		}

		public String getResponses() {
			return responses;
		}
	}

}
