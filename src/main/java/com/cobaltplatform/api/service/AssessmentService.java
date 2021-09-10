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

package com.cobaltplatform.api.service;

import com.cobaltplatform.api.cache.Cache;
import com.cobaltplatform.api.cache.DistributedCache;
import com.cobaltplatform.api.model.api.request.PersonalizeAssessmentChoicesCommand;
import com.cobaltplatform.api.model.api.request.PersonalizeAssessmentChoicesCommand.AssessmentSubmission;
import com.cobaltplatform.api.model.api.request.PersonalizeAssessmentChoicesCommand.SubmissionAnswer;
import com.cobaltplatform.api.model.api.request.SubmitAssessmentAnswerRequest;
import com.cobaltplatform.api.model.api.request.SubmitAssessmentAnswerRequest.AnswerRequest;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AccountSession;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.assessment.AccountSessionAnswer;
import com.cobaltplatform.api.model.db.assessment.Answer;
import com.cobaltplatform.api.model.db.assessment.Assessment;
import com.cobaltplatform.api.model.db.assessment.Question;
import com.cobaltplatform.api.model.db.assessment.QuestionType;
import com.cobaltplatform.api.model.db.assessment.QuestionType.QuestionTypeId;
import com.cobaltplatform.api.model.service.AssessmentQuestionAnswers;
import com.cobaltplatform.api.model.service.QuestionAnswers;
import com.cobaltplatform.api.util.Normalizer;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.ValidationException.FieldError;
import com.pyranid.Database;
import com.soklet.web.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.cobaltplatform.api.model.db.assessment.Assessment.AssessmentType;
import static com.cobaltplatform.api.util.ValidationUtility.isValidLocalDate;
import static com.cobaltplatform.api.util.ValidationUtility.isValidStudentId;
import static com.cobaltplatform.api.util.ValidationUtility.isValidUUID;
import static com.soklet.util.StringUtils.trimToNull;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

/**
 * @author Transmogrify LLC.
 */
@Singleton
public class AssessmentService {

	@Nonnull
	private final Database database;
	@Nonnull
	private final SessionService sessionService;
	@Nonnull
	private final AssessmentScoringService assessmentScoringService;
	@Nonnull
	private final Cache distributedCache;
	@Nonnull
	private final Normalizer normalizer;
	@Nonnull
	private final Logger logger;

	@Inject
	public AssessmentService(@Nonnull Database database,
													 @Nonnull SessionService sessionService,
													 @Nonnull AssessmentScoringService assessmentScoringService,
													 @Nonnull @DistributedCache Cache distributedCache,
													 @Nonnull Normalizer normalizer) {
		this.database = database;
		this.sessionService = sessionService;
		this.assessmentScoringService = assessmentScoringService;
		this.distributedCache = distributedCache;
		this.normalizer = normalizer;
		this.logger = LoggerFactory.getLogger(getClass());

	}

	@Nonnull
	private SessionService getSessionService() {
		return sessionService;
	}

	@Nonnull
	public Optional<Assessment> findAssessmentByTypeForUser(@Nonnull AssessmentType assessmentType,
																													@Nonnull Account account) {
		return findAssessmentByTypeForInstitution(assessmentType, account.getInstitutionId());
	}

	@Nonnull
	public Optional<Assessment> findAssessmentByTypeForInstitution(@Nonnull AssessmentType assessmentType,
																																 @Nonnull InstitutionId institutionId) {
		return database.queryForObject(
				"SELECT a.* FROM assessment as a, institution_assessment as ia WHERE " +
						"ia.institution_id = ? AND " +
						"a.assessment_type_id = ? AND " +
						"ia.assessment_id = a.assessment_id",
				Assessment.class,
				institutionId,
				assessmentType
		);
	}


	@Nonnull
	public Optional<Assessment> findIntakeAssessmentByProviderId(@Nonnull UUID providerId) {
		return database.queryForObject(
				"SELECT a.* FROM assessment as a, provider p, provider_clinic pc , clinic c WHERE " +
						"p.provider_id = pc.provider_id AND pc.clinic_id = c.clinic_id " +
						"AND c.intake_assessment_id = a.assessment_id " +
						"AND pc.primary_clinic = true AND p.provider_id = ? ",
				Assessment.class,
				providerId
		);
	}

	@Nonnull
	public Optional<Assessment> findIntakeAssessmentByGroupSessionId(@Nullable UUID groupSessionId) {
		if (groupSessionId == null)
			return Optional.empty();

		return database.queryForObject(
				"SELECT a.* FROM assessment as a, v_group_session gs WHERE gs.assessment_id=a.assessment_id AND gs.group_session_id=?",
				Assessment.class, groupSessionId
		);
	}


	@Nonnull
	public Optional<Assessment> findAssessmentById(@Nonnull UUID assessmentId) {
		return database.queryForObject("SELECT * from assessment where assessment_id = ?", Assessment.class, assessmentId);
	}

	@Nonnull
	public Optional<Question> findQuestionById(@Nonnull UUID questionId) {
		return database.queryForObject("SELECT * FROM question WHERE question_id = ?", Question.class, questionId);
	}

	@Nonnull
	private Optional<Answer> findAnswerByIdAndQuestion(@Nonnull UUID answerId, @Nonnull UUID questionId) {
		return database.queryForObject("SELECT * FROM answer WHERE answer_id = ? AND question_id = ?", Answer.class, answerId, questionId);
	}

	@Nonnull
	private Optional<Question> findFirstQuestionForAssessment(@Nullable UUID assessmentId) {
		if (assessmentId == null) return Optional.empty();
		return database.queryForObject("SELECT * FROM question WHERE assessment_id = ? and display_order = 1", Question.class, assessmentId);
	}

	@Nonnull
	private Optional<Assessment> findPreviousAssessment(@Nonnull UUID currentAssessmentId) {
		return database.queryForObject("SELECT * from assessment WHERE next_assessment_id = ?", Assessment.class, currentAssessmentId);
	}

	@Nonnull
	public List<Question> findQuestionsForAssessmentId(@Nonnull UUID assessmentId) {
		return database.queryForList("SELECT * FROM question WHERE assessment_id = ? ORDER BY display_order",
				Question.class, assessmentId);
	}

	@Nonnull
	public List<Question> findRootQuestionsForFormAssessmentId(@Nonnull UUID assessmentId) {
		return database.queryForList("SELECT * FROM question WHERE assessment_id = ? AND is_root_question = ? ORDER BY display_order",
				Question.class, assessmentId, true);
	}

	@Nonnull
	public List<Answer> findAnswersForQuestion(@Nonnull UUID questionId) {
		return database.queryForList("SELECT * FROM answer WHERE question_id = ? ORDER BY display_order", Answer.class, questionId);
	}


	@Nonnull
	@SuppressWarnings("unchecked")
	public Optional<QuestionType> findQuestionTypeById(@Nullable QuestionTypeId quesstionTypeId) {
		if (quesstionTypeId == null)
			return Optional.empty();

		return Optional.ofNullable(getDistributedCache().get(format("questionTypeByQuestionTypeId-%s", quesstionTypeId.name()), () -> {
			return database.queryForObject("SELECT * FROM question_type WHERE question_type_id=?", QuestionType.class, quesstionTypeId).orElse(null);
		}, QuestionType.class));
	}


	@Nonnull
	private void insertAnswers(@Nonnull AccountSession accountSession,
														 @Nonnull Question question,
														 @Nonnull List<AccountSessionAnswer> answers) {
		String answerSql = answers.stream().map(a -> "(?, ?, ?, ?)").collect(joining(", "));
		List<Object> sqlParams = new ArrayList<>();
		for (AccountSessionAnswer answer : answers) {
			sqlParams.add(answer.getAccountSessionAnswerId());
			sqlParams.add(answer.getAccountSessionId());
			sqlParams.add(answer.getAnswerId());
			sqlParams.add(answer.getAnswerText());
		}

		database.execute("DELETE FROM account_session_answer WHERE " +
				"account_session_id = ? AND " +
				"answer_id IN (SELECT answer_id FROM answer WHERE question_id = ?)", accountSession.getAccountSessionId(), question.getQuestionId());
		database.execute("INSERT INTO account_session_answer (account_session_answer_id, account_session_id, answer_id, answer_text) VALUES " + answerSql, sqlParams.toArray());
	}

	@Nonnull
	private Optional<Question> findNextQuestionInAssessment(@Nonnull AccountSession accountSession,
																													@Nonnull Assessment assessment,
																													@Nonnull Question currentQuestion,
																													@Nullable Answer answer) {
		int next = currentQuestion.getDisplayOrder() + 1;
		Optional<Question> nextQuestion = Optional.empty();

		if (answer == null)
			nextQuestion = database.queryForObject(
					"SELECT * from question WHERE assessment_id = ? AND display_order = ?",
					Question.class, assessment.getAssessmentId(), next);
		else {
			nextQuestion = database.queryForObject(
					"SELECT * from question WHERE question_id = ? ",
					Question.class, answer.getNextQuestionId());
			logger.debug(String.format("got answer %s", answer.getAnswerText()));
			logger.debug("next question id = " + answer.getNextQuestionId());
			logger.debug("has next question = " + nextQuestion.isPresent());
		}

		return nextQuestion;
	}

	@Nonnull
	private Optional<Question> findPreviousQuestion(@Nonnull Assessment assessment,
																									@Nonnull Question currentQuestion) {
		if (currentQuestion.getDisplayOrder() > 1) {
			return database.queryForObject("SELECT * from question WHERE assessment_id = ? AND display_order = ?", Question.class, assessment.getAssessmentId(), currentQuestion.getDisplayOrder() - 1);
		} else {
			Optional<Assessment> previousAssessment = findPreviousAssessment(assessment.getAssessmentId());
			if (previousAssessment.isPresent()) {
				return database.queryForObject("SELECT * from question WHERE assessment_id = ? ORDER by display_order DESC LIMIT 1", Question.class, previousAssessment.get().getAssessmentId());
			}
			return Optional.empty();
		}
	}

	@Nonnull
	private QuestionAnswers getFullQuestion(@Nonnull AccountSession accountSession,
																					@Nonnull Assessment assessment,
																					@Nonnull Question currentQuestion,
																					Boolean includeNext) {
		Optional<Question> nextQuestion;
		if (includeNext)
			nextQuestion = findNextQuestionInAssessment(accountSession, assessment, currentQuestion, null);
		else
			nextQuestion = Optional.empty();

		Optional<Question> previousQuestion = findPreviousQuestion(assessment, currentQuestion);
		logger.info(format("Found question: session - %s, question - %s, next - %s, previous - %s", accountSession.getAccountSessionId(), currentQuestion.getQuestionId(), nextQuestion.isPresent() ? nextQuestion.get().getQuestionId() : "none", previousQuestion.isPresent() ? previousQuestion.get().getQuestionId() : "none"));

		List<Answer> answers = findAnswersForQuestion(currentQuestion.getQuestionId());
		return new QuestionAnswers(currentQuestion, answers, nextQuestion, previousQuestion);
	}

	@Nullable
	public AssessmentQuestionAnswers submitAssessmentAnswer(@Nonnull Account account,
																													@Nonnull SubmitAssessmentAnswerRequest request) {
		String questionIdCommand = request.getQuestionId();
		String sessionIdCommand = request.getSessionId();
		List<String> answersCommand = request.getAnswers();
		List<AnswerRequest> assessmentAnswersCommand = request.getAssessmentAnswers();

		ValidationException validationException = new ValidationException();

		UUID questionId = null;
		UUID sessionId = null;
		Question question = null;
		AccountSession accountSession = null;
		Assessment assessment = null;
		if (!isValidUUID(questionIdCommand)) {
			validationException.add(new FieldError("questionId", "Invalid question id"));
		} else {
			questionId = UUID.fromString(questionIdCommand);
			Optional<Question> questionOptional = findQuestionById(questionId);
			if (questionOptional.isEmpty()) {
				validationException.add(new FieldError("questionId", "Invalid question id for assessment"));
			} else {
				question = questionOptional.get();

				Optional<Assessment> assessmentOptional = findAssessmentById(question.getAssessmentId());
				if (assessmentOptional.isEmpty()) {
					validationException.add(new FieldError("questionId", "Invalid question "));
				}
				assessment = assessmentOptional.get();
			}
		}

		if (!isValidUUID(sessionIdCommand)) {
			validationException.add(new FieldError("sessionId", "Session id must be UUID"));
		} else {
			sessionId = UUID.fromString(sessionIdCommand);
			Optional<AccountSession> accountSessionOptional = sessionService.findAccountSessionByIdAndAccount(account, sessionId);
			if (accountSessionOptional.isEmpty()) {
				validationException.add(new FieldError("sessionId", "Couldn't find session"));
			} else {
				accountSession = accountSessionOptional.get();
				if (!accountSession.getAssessmentId().equals(assessment.getAssessmentId())) {
					validationException.add(new FieldError("sessionId", "Invalid session id for assessment"));
				} else {
					if (!accountSession.getCurrentFlag()) {
						throw new NotFoundException("Sorry, this session has been ended, please start over");
					}
				}
			}
		}

		Optional<Answer> firstAnswer = Optional.empty(); // needed for next question lookup
		List<AccountSessionAnswer> answers = new ArrayList<>();
		if (answersCommand.isEmpty() && assessmentAnswersCommand.isEmpty()) {
			validationException.add(new FieldError("answers", "answers or assessment answers are required"));
		} else if (!answersCommand.isEmpty() && !assessmentAnswersCommand.isEmpty()) {
			validationException.add(new FieldError("answers", "answers or assessment answers are required"));
		} else {

			QuestionType questionType = findQuestionTypeById(question.getQuestionTypeId()).orElseThrow();

			// Map list of string of answerIds to answer requests for backwards compatibility
			if (!answersCommand.isEmpty()) {
				assessmentAnswersCommand = answersCommand.stream().map(answerId -> new AnswerRequest(answerId, null)).collect(toList());
			}

			if (!questionType.getAllowMultipleAnswers() &&
					assessmentAnswersCommand.size() > 1) {
				validationException.add(new FieldError("answerId", "Only 1 answer is allowed for this question"));
			}

			for (AnswerRequest answerCommand : assessmentAnswersCommand) {

				if (!isValidUUID(answerCommand.getAnswerId())) {
					validationException.add(new FieldError("answerId", "Invalid answer id"));
				} else {
					UUID answerId = UUID.fromString(answerCommand.getAnswerId());
					Optional<Answer> answerOptional = findAnswerByIdAndQuestion(answerId, questionId);
					if (answerOptional.isEmpty()) {
						validationException.add(new FieldError("answerId", "Invalid answer id for question"));
					} else {
						if (questionType.getRequiresTextResponse()) {
							if (trimToNull(answerCommand.getAnswerText()) == null) {
								validationException.add(new FieldError("answerText", "Answer text is required for question"));
							} else {
								switch (questionType.getQuestionTypeId()) {

									case PHONE_NUMBER:
										Optional<String> normalizedPhoneNumber = getNormalizer().normalizePhoneNumberToE164(answerCommand.getAnswerText());
										if (normalizedPhoneNumber.isEmpty()) {
											validationException.add(new FieldError("answerText", "Phone number is not a valid format"));
										} else {
											answerCommand.setAnswerText(normalizedPhoneNumber.get());
										}

										break;
									case STUDENT_ID:
										if (!isValidStudentId(answerCommand.getAnswerText())) {
											validationException.add(new FieldError("answerText", "Student Id is not a valid format"));
										}
										break;
									case DATE:
										if (!isValidLocalDate(answerCommand.getAnswerText())) {
											validationException.add(new FieldError("answerText", "Date must be YYYY-MM-DD format"));
										}
										break;
									case TEXT:
									default:
										break;
								}
							}
						}

						if (firstAnswer.isEmpty()) firstAnswer = answerOptional;

						AccountSessionAnswer asa = new AccountSessionAnswer();
						asa.setAccountSessionAnswerId(UUID.randomUUID());
						asa.setAccountSessionId(sessionId);
						asa.setQuestionId(question.getQuestionId());
						asa.setAnswerId(answerId);
						asa.setAnswerText(answerCommand.getAnswerText());
						answers.add(asa);

					}
				}
			}
		}

		if (validationException.hasErrors()) throw validationException;

		logger.info(format("Submit session answer: session - %s, assessment - %s, question - %s, answer - %s",
				accountSession.getAccountSessionId(), assessment.getAssessmentId(), question.getQuestionId(),
				answers.stream().map(a -> a.getAnswerId().toString()).collect(joining(",", "[", "]"))));

		insertAnswers(accountSession, question, answers);

		//TODO: For now we're just taking the first answer and passing that to get the
		Optional<Question> nextQuestion = findNextQuestionInAssessment(accountSession, assessment, question, firstAnswer.orElseThrow());

		if (nextQuestion.isEmpty()) {
			boolean endedSession = false;
			if (assessment.getAssessmentTypeId() == AssessmentType.INTRO ||
					assessment.getAssessmentTypeId() == AssessmentType.INTAKE) {
				sessionService.markSessionAsComplete(accountSession);
				endedSession = true;
			}

			if (assessment.getAssessmentTypeId() == AssessmentType.PHQ4) {
				List<Answer> previousAnswers = getSessionService().findAnswersForSession(accountSession);
				int score = previousAnswers.stream().mapToInt(Answer::getAnswerValue).sum();
				if (score <= 2) {
					sessionService.markSessionAsComplete(accountSession);
					endedSession = true;
				}
			}

			if (!endedSession) {
				if (assessment.getNextAssessmentId() != null) {
					Optional<Assessment> nextAssessment = findAssessmentById(assessment.getNextAssessmentId());
					if (nextAssessment.isEmpty()) throw new NotFoundException("Next assessment does not exist");
					assessment = nextAssessment.get();
					accountSession = sessionService.createSessionForAssessment(accountSession.getAccountId(), nextAssessment.get());
					nextQuestion = findFirstQuestionForAssessment(nextAssessment.get().getAssessmentId());
				} else {
					if (assessment.getAssessmentTypeId().equals(AssessmentType.PCPTSD)) {
						assessmentScoringService.finishEvidenceAssessment(account);
					}
				}
			}
		}


		if (nextQuestion.isEmpty()) {
			return null;
		}

		QuestionAnswers questionAnswers = getFullQuestion(accountSession, assessment, nextQuestion.get(), false);
		return new AssessmentQuestionAnswers(assessment, questionAnswers, accountSession);

	}

	@Nonnull
	public AssessmentQuestionAnswers getNextAssessmentQuestion(@Nonnull Account account,
																														 @Nonnull AssessmentType assessmentType,
																														 @Nullable String questionIdCommand,
																														 @Nullable String sessionIdCommand,
																														 @Nullable UUID providerId,
																														 @Nullable UUID groupSessionId) {
		boolean providerIntake = false;
		boolean groupSessionIntake = false;
		Assessment initialAssessment;

		if (assessmentType == AssessmentType.INTAKE) {
			if (providerId != null && groupSessionId != null)
				throw new IllegalArgumentException();

			if (providerId != null) {
				providerIntake = true;
				initialAssessment = findIntakeAssessmentByProviderId(providerId).orElse(null);
			} else if (groupSessionId != null) {
				groupSessionIntake = true;
				initialAssessment = findIntakeAssessmentByGroupSessionId(groupSessionId).orElse(null);
			} else {
				throw new IllegalArgumentException();
			}
		} else {
			initialAssessment = findAssessmentByTypeForUser(assessmentType, account).orElse(null);
		}

		if (initialAssessment == null)
			throw new NotFoundException("No assessment found");

		AccountSession accountSession;

		if (sessionIdCommand != null) {
			accountSession = getSessionService().findAccountSessionByIdAndAccount(account, UUID.fromString(sessionIdCommand))
					.orElseThrow(() -> new NotFoundException("Couldn't find session"));
		} else {
			if (initialAssessment.getAssessmentTypeId().equals(AssessmentType.PHQ4)) {
				accountSession = getSessionService().findCurrentIncompleteEvidenceAssessmentForAccount(account)
						.orElseGet(() -> sessionService.createSessionForAssessment(account.getAccountId(), initialAssessment));
			} else if (initialAssessment.getAssessmentTypeId().equals(AssessmentType.INTRO)) {
				accountSession = getSessionService().findCurrentIncompleteIntroAssessmentForAccount(account)
						.orElseGet(() -> sessionService.createSessionForAssessment(account.getAccountId(), initialAssessment));
			} else if (providerIntake) {
				accountSession = getSessionService().findCurrentIntakeAssessmentForAccountAndProvider(account,
						providerId, false).orElseGet(() -> sessionService.createSessionForAssessment(account.getAccountId(),
						initialAssessment));
			} else if (groupSessionIntake) {
				accountSession = getSessionService().findCurrentIntakeAssessmentForAccountAndGroupSessionId(account,
						groupSessionId, false).orElseGet(() -> sessionService.createSessionForAssessment(account.getAccountId(),
						initialAssessment));
			} else {
				throw new IllegalStateException(format("Not sure how to handle assessment type %s.%s", AssessmentType.class.getSimpleName(),
						initialAssessment.getAssessmentTypeId().name()));
			}
		}

		// Refetch the assessment, it could have changed from the session
		Assessment assessment = findAssessmentById(accountSession.getAssessmentId()).get();
		Optional<Question> currentQuestion;
		if (questionIdCommand == null) {
			currentQuestion = findFirstQuestionForAssessment(assessment.getAssessmentId());
		} else {
			if (!isValidUUID(questionIdCommand)) {
				throw new NotFoundException("No question for that id");
			}
			UUID questionId = UUID.fromString(questionIdCommand);
			currentQuestion = database.queryForObject("SELECT * FROM question WHERE question_id = ?", Question.class, questionId);
			if (currentQuestion.isEmpty()) throw new NotFoundException("Invalid submission question id");
		}

		if (!currentQuestion.get().getAssessmentId().equals(assessment.getAssessmentId())) {
			throw new NotFoundException("Invalid question for current assessment");
		}

//		if (sessionIdCommand != null && !isValidUUID(sessionIdCommand)) {
//			throw new NotFoundException("Session ID must be UUID");
//		}

		logger.info(format("Get session question: session - %s, assessmentOptional - %s, question - %s", accountSession.getAccountSessionId(), assessment.getAssessmentId(), currentQuestion.get().getQuestionId()));

		QuestionAnswers questionAnswers = getFullQuestion(accountSession, assessment, currentQuestion.get(), true);
		return new AssessmentQuestionAnswers(assessment, questionAnswers, accountSession);
	}

	//TODO: cleanup
	@Nonnull
	public Optional<com.cobaltplatform.api.model.db.RecommendationLevel> findRecommendationLevelById(String
																																																			 recommendationLevelId) {
		return database.queryForObject("SELECT * from recommendation_level WHERE recommendation_level_id = ?",
				com.cobaltplatform.api.model.db.RecommendationLevel.class, recommendationLevelId);
	}

	public Map<UUID, List<SubmissionAnswer>> validateIntroAssessmentSubmissionCommand(@Nonnull PersonalizeAssessmentChoicesCommand command,
																																										@Nonnull Assessment assessment,
																																										@Nonnull ValidationException validationException) {
		if (command.getChoices() == null || command.getChoices().isEmpty()) {
			validationException.add(new FieldError("choices", "Choices are required"));
		}

		List<UUID> questionIds = command.getChoices().stream().map(AssessmentSubmission::getQuestionId).collect(toList());

		List<Question> existingQuestions = findQuestionsForAssessmentId(assessment.getAssessmentId());

		if (!existingQuestions.stream().map(Question::getQuestionId).collect(toSet()).containsAll(questionIds)) {
			validationException.add(new FieldError("questionIds", "Invalid questionId"));
		}

		Map<UUID, List<SubmissionAnswer>> choicesAsMap = command.getChoices().stream().collect(toMap(AssessmentSubmission::getQuestionId, AssessmentSubmission::getSelectedAnswers));

		for (Question question : existingQuestions) {
			List<UUID> allowedAnswers = findAnswersForQuestion(question.getQuestionId()).stream().map(Answer::getAnswerId).collect(toList());
			List<SubmissionAnswer> selectedChoices = choicesAsMap.getOrDefault(question.getQuestionId(), emptyList());
			QuestionType questionType = findQuestionTypeById(question.getQuestionTypeId()).orElseThrow();

			if (selectedChoices.isEmpty() && question.getIsRootQuestion() && question.getAnswerRequired()) {
				validationException.add(new FieldError("answers", "Options are required"));
			} else if (!selectedChoices.isEmpty() && selectedChoices.stream().map(SubmissionAnswer::getAnswerId).noneMatch(allowedAnswers::contains)) {
				validationException.add(new FieldError("answers", format("Invalid answer for question %s", question.getQuestionId())));
			} else if (questionType.getRequiresTextResponse()) {
				if (selectedChoices.stream().anyMatch(sc -> trimToNull(sc.getAnswerText()) == null)) {
					validationException.add(new FieldError("answers", format("Text is required for question %s", question.getQuestionId())));
				}
			} else if (!questionType.getRequiresTextResponse()) {
				if (selectedChoices.stream().anyMatch(sc -> trimToNull(sc.getAnswerText()) != null)) {
					validationException.add(new FieldError("answers", format("Text is not allowed for question %s", question.getQuestionId())));
				}
			} else if (!questionType.getAllowMultipleAnswers() && selectedChoices.size() > 1) {
				validationException.add(new FieldError("answers", format("Only one selection allowed for %s", question.getQuestionId())));
			}
			// TODO validate only one nesting level
		}
		return choicesAsMap;
	}

	@Nonnull
	public UUID submitPersonalizeAssessmentAnswers(@Nonnull Account account, @Nonnull PersonalizeAssessmentChoicesCommand command) {
		ValidationException validationException = new ValidationException();

		Assessment introAssessment = findAssessmentByTypeForUser(AssessmentType.INTRO, account).orElseThrow();
		AccountSession accountSession = getSessionService().createSessionForAssessment(account.getAccountId(), introAssessment);

		Map<UUID, List<SubmissionAnswer>> choicesAsMap = validateIntroAssessmentSubmissionCommand(command, introAssessment, validationException);

		if (validationException.hasErrors()) throw validationException;

		choicesAsMap.forEach((questionId, submissionAnswers) ->
				submissionAnswers.forEach(submissionAnswer ->
						database.execute(
								"INSERT INTO account_session_answer (account_session_answer_id, account_session_id, answer_id) " +
										"VALUES (?, ?, ?)",
								UUID.randomUUID(), accountSession.getAccountSessionId(), submissionAnswer.getAnswerId())
				)
		);


		return accountSession.getAccountSessionId();
	}

	@Nonnull
	public Cache getDistributedCache() {
		return distributedCache;
	}

	@Nonnull
	public Normalizer getNormalizer() {
		return normalizer;
	}
}
