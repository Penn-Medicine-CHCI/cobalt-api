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
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AccountSession;
import com.cobaltplatform.api.model.db.FontSize.FontSizeId;
import com.cobaltplatform.api.model.db.Answer;
import com.cobaltplatform.api.model.db.Question;
import com.cobaltplatform.api.model.db.QuestionType.QuestionTypeId;
import com.cobaltplatform.api.model.service.AssessmentQuestionAnswers;
import com.cobaltplatform.api.service.SessionService;
import com.cobaltplatform.api.util.Formatter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.cobaltplatform.api.model.db.Assessment.AssessmentTypeId.INTRO;
import static java.util.stream.Collectors.toList;

/**
 * @author Transmogrify LLC.
 */
@ThreadSafe
public class AssessmentApiResponse {


	@Nonnull
	private Integer assessmentProgress;

	@Nonnull
	private Integer assessmentProgressTotal;

	@Nonnull
	private String assessmentPrompt;

	@Nonnull
	private String assessmentType;

	@Nonnull
	private String assessmentId;

	@Nullable
	private String previousQuestionId;

	@Nullable
	private String nextQuestionId;

	@Nonnull
	private String sessionId;

	@Nullable
	private String previousSessionId;

	@Nonnull
	private QuestionApiResponse question;

	@ThreadSafe
	public interface AssessmentQuestionAnswerApiResponseFactory {
		@Nonnull
		AssessmentApiResponse create(@Nonnull AssessmentQuestionAnswers assessment,
																 @Nonnull Account account);
	}

	@AssistedInject
	public AssessmentApiResponse(@Nonnull Formatter formatter,
															 @Nonnull SessionService sessionService,
															 @Assisted @Nonnull Account account,
															 @Assisted @Nonnull AssessmentQuestionAnswers assessment) {

		assessmentPrompt = assessment.getAssessment().getBaseQuestion();
		assessmentType = assessment.getAssessment().getAssessmentTypeId().toString();
		assessmentId = assessment.getAssessment().getAssessmentId().toString();

		sessionId = assessment.getAccountSession().getAccountSessionId().toString();

		SessionService.SessionProgress sessionProgress = sessionService.getProgressForSession(assessment.getAssessment(), assessment.getQuestionAnswers().getQuestion());
		assessmentProgressTotal = sessionProgress.getTotal();
		assessmentProgress = sessionProgress.getProgress();


		if (assessment.getQuestionAnswers().getNextQuestion().isPresent()) {
			nextQuestionId = assessment.getQuestionAnswers().getNextQuestion().get().getQuestionId().toString();
		}

		Optional<Question> prevQuestion = assessment.getQuestionAnswers().getPreviousQuestion();
		if (prevQuestion.isPresent()) {
			if (!prevQuestion.get().getAssessmentId().equals(assessment.getAssessment().getAssessmentId())) {
				Optional<AccountSession> previousSession = sessionService.findCurrentAccountSessionForAssessmentId(account, prevQuestion.get().getAssessmentId());
				previousSession.ifPresent(accountSession -> previousSessionId = accountSession.getAccountSessionId().toString());
			} else {
				previousSessionId = assessment.getAccountSession().getAccountSessionId().toString();
			}

			previousQuestionId = prevQuestion.get().getQuestionId().toString();
		}

		List<Answer> selectedAnswersForSession = sessionService.findAnswersForSessionAndQuestion(assessment.getAccountSession(), assessment.getQuestionAnswers().getQuestion());

		if (selectedAnswersForSession.isEmpty()) {

			if (assessment.getAssessment().getAssessmentTypeId().equals(INTRO)) {
				selectedAnswersForSession = sessionService.findAnswersForLastCompleteIntroSession(assessment.getAccountSession(), assessment.getQuestionAnswers().getQuestion());
			}

			if (selectedAnswersForSession.isEmpty() &&
					assessment.getQuestionAnswers().getQuestion().getQuestionTypeId().equals(QuestionTypeId.DROPDOWN)) {
				selectedAnswersForSession = new ArrayList<>() {{
					add(assessment.getQuestionAnswers().getAnswers().get(0));
				}};
			}
		}
		List<String> selectedAnswerIds = selectedAnswersForSession.stream().map(a -> a.getAnswerId().toString()).collect(toList());
		List<Map<String, Object>> selectedAssessmentAnswers = new ArrayList<>();

		for(Answer answer : selectedAnswersForSession) {
			Map<String, Object> answerObject = new HashMap<>();
			answerObject.put("answerId", answer.getAnswerId());
			answerObject.put("answerText", answer.getAnswerText());
			selectedAssessmentAnswers.add(answerObject);
		}

		Question questionModel = assessment.getQuestionAnswers().getQuestion();
		question = new QuestionApiResponse(
				questionModel.getQuestionId().toString(),
				questionModel.getFontSizeId(),
				questionModel.getQuestionText(),
				questionModel.getQuestionTypeId().toString(),
				questionModel.getAnswerColumnCount(),
				assessment.getQuestionAnswers().getAnswers().stream().map(a ->
						new AnswerApiResponse(
								a.getAnswerId().toString(),
								a.getAnswerText(),
								a.getCrisis(),
								a.getCall()
						)
				).collect(toList()),
				selectedAnswerIds,
				selectedAssessmentAnswers
		);

	}

	@Nonnull
	public Integer getAsssessmentProgress() {
		return assessmentProgress;
	}

	@Nonnull
	public Integer getAssessmentProgressTotal() {
		return assessmentProgressTotal;
	}

	@Nonnull
	public String getAssessmentPrompt() {
		return assessmentPrompt;
	}

	@Nonnull
	public QuestionApiResponse getQuestion() {
		return question;
	}

	@Nonnull
	public String getAssessmentType() {
		return assessmentType;
	}

	@Nonnull
	public String getAssessmentId() {
		return assessmentId;
	}

	@Nullable
	public String getPreviousQuestionId() {
		return previousQuestionId;
	}

	@Nullable
	public String getNextQuestionId() {
		return nextQuestionId;
	}

	@Nonnull
	public String getSessionId() {
		return sessionId;
	}

	@Nullable
	public String getPreviousSessionId() {
		return previousSessionId;
	}


	public static class QuestionApiResponse {
		@Nonnull
		private final String questionId;
		@Nonnull
		private final FontSizeId fontSizeId;
		@Nonnull
		private final String questionTitle;
		@Nonnull
		private final String questionType;
		@Nonnull
		private final Integer columnCount;
		@Nonnull
		private final List<String> selectedAnswerIds;
		@Nonnull
		private final List<Map<String, Object>> selectedAssessmentAnswers;
		@Nonnull
		private final List<AnswerApiResponse> answers;

		public QuestionApiResponse(@Nonnull String questionId,
															 @Nonnull FontSizeId fontSizeId,
															 @Nonnull String questionTitle,
															 @Nonnull String questionType,
															 @Nonnull Integer columnCount,
															 @Nonnull List<AnswerApiResponse> answers,
															 @Nonnull List<String> selectedAnswerIds,
															 @Nonnull List<Map<String, Object>> selectedAssessmentAnswers) {
			this.questionId = questionId;
			this.fontSizeId = fontSizeId;
			this.questionTitle = questionTitle;
			this.questionType = questionType;
			this.columnCount = columnCount;
			this.selectedAnswerIds = selectedAnswerIds;
			this.answers = answers;
			this.selectedAssessmentAnswers = selectedAssessmentAnswers;
		}

		@Nonnull
		public String getQuestionId() {
			return questionId;
		}

		@Nonnull
		public FontSizeId getFontSizeId() {
			return fontSizeId;
		}

		@Nonnull
		public String getQuestionTitle() {
			return questionTitle;
		}

		@Nonnull
		public String getQuestionType() {
			return questionType;
		}

		@Nonnull
		public Integer getColumnCount() {
			return columnCount;
		}

		@Nonnull
		public List<AnswerApiResponse> getAnswers() {
			return answers;
		}

		@Nonnull
		public List<String> getSelectedAnswerIds() {
			return selectedAnswerIds;
		}

		@Nonnull
		public List<Map<String, Object>> getSelectedAssessmentAnswers() {
			return selectedAssessmentAnswers;
		}
	}

	public static class AnswerApiResponse {
		@Nonnull
		private final String answerId;
		@Nonnull
		private final String answerDescription;
		@Nonnull
		private final Boolean isCall;
		@Nonnull
		private final Boolean isCrisis;

		public AnswerApiResponse(@Nonnull String answerId,
														 @Nonnull String answerDescription,
														 @Nonnull Boolean isCall,
														 @Nonnull Boolean isCrisis) {
			this.answerId = answerId;
			this.answerDescription = answerDescription;
			this.isCall = isCall;
			this.isCrisis = isCrisis;
		}

		@Nonnull
		public String getAnswerId() {
			return answerId;
		}

		@Nonnull
		public String getAnswerDescription() {
			return answerDescription;
		}
	}
}
