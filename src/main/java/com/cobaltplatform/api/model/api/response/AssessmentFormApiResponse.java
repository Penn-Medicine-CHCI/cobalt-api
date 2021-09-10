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

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.cobaltplatform.api.model.api.request.PersonalizeAssessmentChoicesCommand.SubmissionAnswer;
import com.cobaltplatform.api.model.db.AccountSession;
import com.cobaltplatform.api.model.db.assessment.Answer;
import com.cobaltplatform.api.model.db.assessment.Assessment;
import com.cobaltplatform.api.model.db.assessment.Question;
import com.cobaltplatform.api.service.AssessmentService;
import com.cobaltplatform.api.service.SessionService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.cobaltplatform.api.model.api.response.AssessmentFormApiResponse.AssessmentFormApiResponseType.*;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

/**
 * @author Transmogrify LLC.
 */
@ThreadSafe
public class AssessmentFormApiResponse {

	public enum AssessmentFormApiResponseType {
		CMS,
		ASSESSMENT
	}

	@Nonnull
	private List<AssessmentQuestion> assessmentQuestions;

	@ThreadSafe
	public interface AssessmentFormApiResponseFactory {
		@Nonnull
		public AssessmentFormApiResponse create(@Assisted Assessment assessment,
																						@Assisted Optional<AccountSession> accountSession);

		@Nonnull
		public AssessmentFormApiResponse create(@Assisted Assessment assessment,
																						@Assisted Optional<AccountSession> accountSession,
																						@Assisted AssessmentFormApiResponseType assessmentFormApiResponseType);
	}

	@AssistedInject
	public AssessmentFormApiResponse(@Nonnull AssessmentService assessmentService,
																	 @Nonnull SessionService sessionService,
																	 @Assisted @Nonnull Assessment assessment,
																	 @Assisted @Nonnull Optional<AccountSession> accountSession) {
		this(assessmentService, sessionService, assessment, accountSession, ASSESSMENT);
	}

	@AssistedInject
	public AssessmentFormApiResponse(@Nonnull AssessmentService assessmentService,
																	 @Nonnull SessionService sessionService,
																	 @Assisted @Nonnull Assessment assessment,
																	 @Assisted @Nonnull Optional<AccountSession> accountSession,
																	 @Assisted @Nonnull AssessmentFormApiResponseType assessmentFormApiResponseType) {
		final Map<UUID, List<Answer>> previousAnswersByQuestionId;
		if (accountSession.isPresent()) {
			previousAnswersByQuestionId = sessionService.findAnswersForSession(accountSession.get())
					.stream().collect(groupingBy(Answer::getQuestionId));
		} else {
			previousAnswersByQuestionId = Collections.emptyMap();
		}

		assessmentQuestions = assessmentService.findRootQuestionsForFormAssessmentId(assessment.getAssessmentId()).stream()
				.map(question -> generateQuestionResponse(assessmentService, previousAnswersByQuestionId, question, assessmentFormApiResponseType)
				).collect(toList());

	}

	private AssessmentQuestion generateQuestionResponse(@Nonnull AssessmentService assessmentService,
																											@Nonnull Map<UUID, List<Answer>> previousAnswersByQuestionId,
																											@Nonnull Question question,
																											@Nonnull AssessmentFormApiResponseType assessmentFormApiResponseType) {


		List<AssessmentAnswer> answers = assessmentService.findAnswersForQuestion(question.getQuestionId()).stream()
				.map(answer -> {
					AssessmentQuestion nextQuestion = null;
					if (answer.getNextQuestionId() != null) {
						nextQuestion = generateQuestionResponse(assessmentService, previousAnswersByQuestionId, assessmentService.findQuestionById(answer.getNextQuestionId()).orElseThrow(), assessmentFormApiResponseType);
					}
					return new AssessmentAnswer(
							answer.getAnswerId().toString(),
							answer.getAnswerText(),
							nextQuestion
					);
				})
				.collect(toList());

		List<SubmissionAnswer> selectedAnswers = previousAnswersByQuestionId.getOrDefault(question.getQuestionId(), Collections.emptyList()).stream()
				.map(it -> new SubmissionAnswer(it.getAnswerId())).collect(toList());

		return new AssessmentQuestion(
				question.getQuestionId().toString(),
				question.getQuestionTypeId().toString(),
				assessmentFormApiResponseType == ASSESSMENT ? question.getQuestionText() : question.getCmsQuestionText(),
				answers,
				selectedAnswers
		);

	}

	public static class AssessmentQuestion {

		@Nonnull
		private final String questionId;
		@Nonnull
		private final String questionType;
		@Nonnull
		private final String label;

		@Nullable
		private final List<AssessmentAnswer> answers;

		@Nullable
		private final List<SubmissionAnswer> selectedAnswers;

		public AssessmentQuestion(@Nonnull String questionId,
															@Nonnull String questionType,
															@Nonnull String label,
															@Nullable List<AssessmentAnswer> answers,
															@Nonnull List<SubmissionAnswer> selectedAnswers) {
			this.questionId = questionId;
			this.questionType = questionType;
			this.label = label;
			this.answers = answers;
			this.selectedAnswers = selectedAnswers;
		}

		@Nonnull
		public String getQuestionId() {
			return questionId;
		}

		@Nonnull
		public String getQuestionType() {
			return questionType;
		}

		@Nullable
		public List<AssessmentAnswer> getAnswers() {
			return answers;
		}

		@Nullable
		public List<SubmissionAnswer> getSelectedAnswers() {
			return selectedAnswers;
		}
	}

	public static class AssessmentAnswer {
		@Nonnull
		private final String answerId;
		@Nonnull
		private final String label;
		@Nonnull
		private final AssessmentQuestion question;

		public AssessmentAnswer(@Nonnull String answerId,
														@Nonnull String label,
														@Nonnull AssessmentQuestion question) {
			this.answerId = answerId;
			this.label = label;
			this.question = question;
		}

		@Nonnull
		public String getAnswerId() {
			return answerId;
		}

		@Nonnull
		public String getLabel() {
			return label;
		}

		@Nonnull
		public AssessmentQuestion getQuestion() {
			return question;
		}
	}

	@Nonnull
	public List<AssessmentQuestion> getAssessmentQuestions() {
		return assessmentQuestions;
	}
}
