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

import com.cobaltplatform.api.model.api.request.CreateScreeningAnswersRequest;
import com.cobaltplatform.api.model.api.request.CreateScreeningAnswersRequest.CreateAnswerRequest;
import com.cobaltplatform.api.model.api.request.CreateScreeningSessionRequest;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.Screening;
import com.cobaltplatform.api.model.db.ScreeningAnswer;
import com.cobaltplatform.api.model.db.ScreeningAnswerFormat.ScreeningAnswerFormatId;
import com.cobaltplatform.api.model.db.ScreeningAnswerOption;
import com.cobaltplatform.api.model.db.ScreeningFlow;
import com.cobaltplatform.api.model.db.ScreeningFlowVersion;
import com.cobaltplatform.api.model.db.ScreeningQuestion;
import com.cobaltplatform.api.model.db.ScreeningQuestionAnsweredStatus.ScreeningQuestionAnsweredStatusId;
import com.cobaltplatform.api.model.db.ScreeningSession;
import com.cobaltplatform.api.model.db.ScreeningSessionAnsweredScreeningQuestion;
import com.cobaltplatform.api.model.db.ScreeningSessionScreening;
import com.cobaltplatform.api.model.db.ScreeningVersion;
import com.cobaltplatform.api.model.service.ScreeningQuestionWithAnswerOptions;
import com.cobaltplatform.api.model.service.ScreeningSessionScreeningContext;
import com.cobaltplatform.api.util.JavascriptExecutionException;
import com.cobaltplatform.api.util.JavascriptExecutor;
import com.cobaltplatform.api.util.Normalizer;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.ValidationException.FieldError;
import com.lokalized.Strings;
import com.pyranid.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.cobaltplatform.api.util.ValidationUtility.isValidEmailAddress;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class ScreeningService {
	@Nonnull
	private final Provider<AccountService> accountServiceProvider;
	@Nonnull
	private final Provider<AuthorizationService> authorizationServiceProvider;
	@Nonnull
	private final JavascriptExecutor javascriptExecutor;
	@Nonnull
	private final Normalizer normalizer;
	@Nonnull
	private final Database database;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;

	@Inject
	public ScreeningService(@Nonnull Provider<AccountService> accountServiceProvider,
													@Nonnull Provider<AuthorizationService> authorizationServiceProvider,
													@Nonnull JavascriptExecutor javascriptExecutor,
													@Nonnull Normalizer normalizer,
													@Nonnull Database database,
													@Nonnull Strings strings) {
		requireNonNull(accountServiceProvider);
		requireNonNull(authorizationServiceProvider);
		requireNonNull(javascriptExecutor);
		requireNonNull(normalizer);
		requireNonNull(database);
		requireNonNull(strings);

		this.accountServiceProvider = accountServiceProvider;
		this.authorizationServiceProvider = authorizationServiceProvider;
		this.javascriptExecutor = javascriptExecutor;
		this.normalizer = normalizer;
		this.database = database;
		this.strings = strings;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	public Optional<Screening> findScreeningById(@Nullable UUID screeningId) {
		if (screeningId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM screening WHERE screening_id=?",
				Screening.class, screeningId);
	}

	@Nonnull
	public Optional<ScreeningVersion> findScreeningVersionById(@Nullable UUID screeningVersionId) {
		if (screeningVersionId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM screening_version WHERE screening_version_id=?",
				ScreeningVersion.class, screeningVersionId);
	}

	@Nonnull
	public Optional<ScreeningSession> findScreeningSessionById(@Nullable UUID screeningSessionId) {
		if (screeningSessionId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM screening_session WHERE screening_session_id=?",
				ScreeningSession.class, screeningSessionId);
	}

	@Nonnull
	public Optional<ScreeningSessionScreening> findScreeningSessionScreeningById(@Nullable UUID screeningSessionScreeningId) {
		if (screeningSessionScreeningId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM screening_session_screening WHERE screening_session_screening_id=?",
				ScreeningSessionScreening.class, screeningSessionScreeningId);
	}

	@Nonnull
	public Optional<ScreeningFlow> findScreeningFlowById(@Nullable UUID screeningFlowId) {
		if (screeningFlowId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM screening_flow WHERE screening_flow_id=?",
				ScreeningFlow.class, screeningFlowId);
	}

	@Nonnull
	public Optional<ScreeningFlowVersion> findScreeningFlowVersionById(@Nullable UUID screeningFlowVersionId) {
		if (screeningFlowVersionId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM screening_flow_version WHERE screening_flow_version_id=?",
				ScreeningFlowVersion.class, screeningFlowVersionId);
	}

	@Nonnull
	public List<Screening> findScreeningsByInstitutionId(@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("""
				SELECT s.* FROM screening s, screening_institution si 
				WHERE s.screening_id=si.screening_id
				AND si.institution_id=?
				ORDER BY s.name
				""", Screening.class, institutionId);
	}

	@Nonnull
	public List<ScreeningFlow> findScreeningFlowsByInstitutionId(@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("SELECT * FROM screening_flow WHERE institution_id=? ORDER BY name",
				ScreeningFlow.class, institutionId);
	}

	@Nonnull
	public Optional<ScreeningAnswerOption> findScreeningAnswerOptionById(@Nullable UUID screeningAnswerOptionId) {
		if (screeningAnswerOptionId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM screening_answer_option WHERE screening_answer_option_id=?",
				ScreeningAnswerOption.class, screeningAnswerOptionId);
	}

	@Nonnull
	public Optional<ScreeningQuestion> findScreeningQuestionById(@Nullable UUID screeningQuestionId) {
		if (screeningQuestionId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM screening_question WHERE screening_question_id=?",
				ScreeningQuestion.class, screeningQuestionId);
	}

	@Nonnull
	public Optional<ScreeningAnswer> findScreeningAnswerById(@Nullable UUID screeningAnswerId) {
		if (screeningAnswerId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM screening_answer WHERE screening_answer_id=?",
				ScreeningAnswer.class, screeningAnswerId);
	}

	@Nonnull
	public List<ScreeningSession> findScreeningSessionsByScreeningFlowId(@Nullable UUID screeningFlowId,
																																			 @Nullable UUID participantAccountId) {
		if (screeningFlowId == null || participantAccountId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("""
						SELECT ss.* FROM screening_session ss, screening_flow_version sfv 
						WHERE sfv.screening_flow_id=? AND ss.screening_flow_version_id=sfv.screening_flow_version_id 
						AND (ss.target_account_id=? OR ss.created_by_account_id=?)
						ORDER BY ss.created DESC
						""",
				ScreeningSession.class, screeningFlowId, participantAccountId, participantAccountId);
	}

	@Nonnull
	public UUID createScreeningSession(@Nonnull CreateScreeningSessionRequest request) {
		requireNonNull(request);

		UUID targetAccountId = request.getTargetAccountId();
		UUID createdByAccountId = request.getCreatedByAccountId();
		UUID screeningFlowId = request.getScreeningFlowId();
		Account targetAccount = null;
		Account createdByAccount = null;
		ScreeningFlow screeningFlow = null;
		UUID screeningSessionId = UUID.randomUUID();
		ValidationException validationException = new ValidationException();

		if (createdByAccountId == null) {
			validationException.add(new FieldError("createdByAccountId", getStrings().get("Created-by account ID is required.")));
		} else {
			createdByAccount = getAccountService().findAccountById(createdByAccountId).orElse(null);

			if (createdByAccount == null)
				validationException.add(new FieldError("createdByAccountId", getStrings().get("Created-by account ID is invalid.")));
		}

		if (targetAccountId == null) {
			validationException.add(new FieldError("targetAccountId", getStrings().get("Target account ID is required.")));
		} else {
			targetAccount = getAccountService().findAccountById(targetAccountId).orElse(null);

			if (targetAccount == null)
				validationException.add(new FieldError("targetAccountId", getStrings().get("Target account ID is invalid.")));
		}

		if (screeningFlowId == null) {
			validationException.add(new FieldError("screeningFlowId", getStrings().get("Screening flow ID is required.")));
		} else {
			screeningFlow = findScreeningFlowById(screeningFlowId).orElse(null);

			if (screeningFlow == null)
				validationException.add(new FieldError("screeningFlowId", getStrings().get("Screening flow ID is invalid.")));
		}

		if (validationException.hasErrors())
			throw validationException;

		ScreeningFlowVersion screeningFlowVersion = findScreeningFlowVersionById(screeningFlow.getActiveScreeningFlowVersionId()).get();

		getDatabase().execute("""
				INSERT INTO screening_session(screening_session_id, screening_flow_version_id, target_account_id, created_by_account_id)
				VALUES (?,?,?,?)
				""", screeningSessionId, screeningFlowVersion.getScreeningFlowVersionId(), targetAccountId, createdByAccountId);

		Screening screening = findScreeningById(screeningFlowVersion.getInitialScreeningId()).get();

		// Initial screening is the current version of the screening specified in the flow
		getDatabase().execute("""
				INSERT INTO screening_session_screening(screening_session_id, screening_version_id, screening_order)
				VALUES (?,?,?)
				""", screeningSessionId, screening.getActiveScreeningVersionId(), 1);

		return screeningSessionId;
	}

	@Nonnull
	public Optional<ScreeningSessionScreeningContext> findScreeningSessionScreeningContextByScreeningSessionScreeningIdAndQuestionId(@Nullable UUID screeningSessionScreeningId,
																																																																	 @Nullable UUID screeningQuestionId) {
		if (screeningSessionScreeningId == null || screeningQuestionId == null)
			return Optional.empty();

		ScreeningSessionScreening screeningSessionScreening = findScreeningSessionScreeningById(screeningSessionScreeningId).orElse(null);

		if (screeningSessionScreening == null)
			return Optional.empty();

		// Get all the questions + answer options
		List<ScreeningQuestionWithAnswerOptions> screeningQuestionsWithAnswerOptions = findScreeningQuestionsWithAnswerOptionsByScreeningSessionScreeningId(screeningSessionScreening.getScreeningSessionScreeningId());

		// Get all the questions that have already been answered for this session
		List<ScreeningSessionAnsweredScreeningQuestion> screeningSessionAnsweredScreeningQuestions = findCurrentScreeningSessionAnsweredScreeningQuestionsByScreeningSessionScreeningId(screeningSessionScreening.getScreeningSessionScreeningId());

		ScreeningQuestionWithAnswerOptions screeningQuestionWithAnswerOptions = null;

		for (ScreeningQuestionWithAnswerOptions potentialScreeningQuestionWithAnswerOptions : screeningQuestionsWithAnswerOptions) {
			if (potentialScreeningQuestionWithAnswerOptions.getScreeningQuestion().getScreeningQuestionId().equals(screeningQuestionId)) {
				screeningQuestionWithAnswerOptions = potentialScreeningQuestionWithAnswerOptions;
				break;
			}
		}

		if (screeningQuestionWithAnswerOptions == null)
			return Optional.empty();

		ScreeningSessionScreeningContext screeningSessionScreeningContext = new ScreeningSessionScreeningContext();
		screeningSessionScreeningContext.setScreeningQuestion(screeningQuestionWithAnswerOptions.getScreeningQuestion());
		screeningSessionScreeningContext.setScreeningAnswerOptions(screeningQuestionWithAnswerOptions.getScreeningAnswerOptions());
		screeningSessionScreeningContext.setScreeningSessionScreening(screeningSessionScreening);

		return Optional.of(screeningSessionScreeningContext);
	}

	public Optional<ScreeningSessionScreeningContext> findPreviousScreeningSessionScreeningContextByScreeningSessionScreeningIdAndQuestionId(@Nullable UUID screeningSessionScreeningId,
																																																																					 @Nullable UUID screeningQuestionId) {
		if (screeningSessionScreeningId == null || screeningQuestionId == null)
			return Optional.empty();

		// TODO: finish implementing
		return Optional.empty();
	}

	// TODO: this currently finds the next unanswered question, if any.
	// We want to also have a variant that takes as input a screening session screening ID and screening question ID,
	// so we can find the next question after the given already-answered question (for example, if a user wants to navigate forward without re-answering the question)
	@Nonnull
	public Optional<ScreeningSessionScreeningContext> findNextUnansweredScreeningSessionScreeningContextByScreeningSessionId(@Nullable UUID screeningSessionId) {
		if (screeningSessionId == null)
			return Optional.empty();

		ScreeningSession screeningSession = findScreeningSessionById(screeningSessionId).orElse(null);

		// If the session does not exist, there is no next question
		if (screeningSession == null)
			return Optional.empty();

		// If the session is already completed, there is no next question
		if (screeningSession.getCompleted())
			return Optional.empty();

		// Get the most recent screening for this session
		ScreeningSessionScreening screeningSessionScreening = findCurrentScreeningSessionScreeningByScreeningSessionId(screeningSessionId).orElse(null);

		// Indicates programmer error
		if (screeningSessionScreening == null)
			throw new IllegalStateException(format("Screening session ID %s does not have a current screening session screening.",
					screeningSessionId));

		// Get all the questions + answer options
		List<ScreeningQuestionWithAnswerOptions> screeningQuestionsWithAnswerOptions = findScreeningQuestionsWithAnswerOptionsByScreeningSessionScreeningId(screeningSessionScreening.getScreeningSessionScreeningId());

		// Get all the questions that have already been answered for this session
		List<ScreeningSessionAnsweredScreeningQuestion> screeningSessionAnsweredScreeningQuestions = findCurrentScreeningSessionAnsweredScreeningQuestionsByScreeningSessionScreeningId(screeningSessionScreening.getScreeningSessionScreeningId());

		// Holder for the next unanswered question (if there is one)
		ScreeningQuestionWithAnswerOptions nextScreeningQuestionWithAnswerOptions = null;

		// These lists are both ordered by question order, so figure out "next" by picking the last-answered index + 1
		if (screeningSessionAnsweredScreeningQuestions.size() < screeningQuestionsWithAnswerOptions.size())
			nextScreeningQuestionWithAnswerOptions = screeningQuestionsWithAnswerOptions.get(screeningSessionAnsweredScreeningQuestions.size());

		// If everything was already answered, nothing comes next for this screening session screening
		if (nextScreeningQuestionWithAnswerOptions == null)
			return Optional.empty();

		ScreeningSessionScreeningContext screeningSessionScreeningContext = new ScreeningSessionScreeningContext();
		screeningSessionScreeningContext.setScreeningQuestion(nextScreeningQuestionWithAnswerOptions.getScreeningQuestion());
		screeningSessionScreeningContext.setScreeningAnswerOptions(nextScreeningQuestionWithAnswerOptions.getScreeningAnswerOptions());
		screeningSessionScreeningContext.setScreeningSessionScreening(screeningSessionScreening);

		return Optional.of(screeningSessionScreeningContext);
	}

	@Nonnull
	protected Optional<ScreeningSessionScreening> findCurrentScreeningSessionScreeningByScreeningSessionId(@Nullable UUID screeningSessionId) {
		if (screeningSessionId == null)
			return Optional.empty();

		// TODO: need to take inactive screenings into account
		return getDatabase().queryForObject("""
				SELECT * FROM screening_session_screening
				WHERE screening_session_id=? 
				ORDER BY screening_order DESC LIMIT 1
				""", ScreeningSessionScreening.class, screeningSessionId);
	}

	@Nonnull
	protected List<ScreeningQuestionWithAnswerOptions> findScreeningQuestionsWithAnswerOptionsByScreeningSessionScreeningId(
			@Nullable UUID screeningSessionScreeningId) {
		if (screeningSessionScreeningId == null)
			return Collections.emptyList();

		List<ScreeningQuestion> screeningQuestions = getDatabase().queryForList("""
				SELECT sq.* 
				FROM screening_question sq, screening_session_screening sss 
				WHERE sss.screening_session_screening_id=?
				AND sss.screening_version_id=sq.screening_version_id
				ORDER BY sq.display_order
				""", ScreeningQuestion.class, screeningSessionScreeningId);

		List<ScreeningAnswerOption> screeningAnswerOptions = getDatabase().queryForList("""
				SELECT sao.*
				FROM screening_answer_option sao, screening_question sq, screening_session_screening sss
				WHERE sao.screening_question_id=sq.screening_question_id
				AND sss.screening_version_id=sq.screening_version_id
				AND sss.screening_session_screening_id=?
				ORDER BY sao.display_order
				""", ScreeningAnswerOption.class, screeningSessionScreeningId);

		List<ScreeningQuestionWithAnswerOptions> screeningQuestionsWithAnswerOptions = new ArrayList<>(screeningQuestions.size());

		// Group answer options by question
		for (ScreeningQuestion screeningQuestion : screeningQuestions) {
			List<ScreeningAnswerOption> screeningAnswerOptionsForQuestion = screeningAnswerOptions.stream()
					.filter(screeningAnswerOption -> screeningAnswerOption.getScreeningQuestionId().equals(screeningQuestion.getScreeningQuestionId()))
					.collect(Collectors.toList());

			screeningQuestionsWithAnswerOptions.add(new ScreeningQuestionWithAnswerOptions(screeningQuestion, screeningAnswerOptionsForQuestion));
		}

		return screeningQuestionsWithAnswerOptions;
	}

	@Nonnull
	protected List<ScreeningSessionAnsweredScreeningQuestion> findCurrentScreeningSessionAnsweredScreeningQuestionsByScreeningSessionScreeningId(@Nullable UUID screeningSessionScreeningId) {
		if (screeningSessionScreeningId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("""
				SELECT ssasq.*
				FROM screening_session_answered_screening_question ssasq, screening_question sq
				WHERE ssasq.screening_session_screening_id=?
				AND ssasq.screening_question_answered_status_id=?
				AND ssasq.screening_question_id=sq.screening_question_id
				ORDER BY sq.display_order
				""", ScreeningSessionAnsweredScreeningQuestion.class, screeningSessionScreeningId, ScreeningQuestionAnsweredStatusId.CURRENT);
	}

	@Nonnull
	protected List<ScreeningAnswer> findCurrentScreeningAnswersByScreeningSessionScreeningId(@Nullable UUID screeningSessionScreeningId) {
		if (screeningSessionScreeningId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("""
				SELECT sa.*
				FROM screening_session_answered_screening_question ssasq, screening_question sq, screening_answer_option sao, screening_answer sa
				WHERE ssasq.screening_session_screening_id=?
				AND ssasq.screening_question_answered_status_id=?
				AND ssasq.screening_question_id=sq.screening_question_id
				AND sq.screening_question_id=sao.screening_question_id
				AND sa.screening_answer_option_id=sao.screening_answer_option_id
				ORDER BY sa.created, sa.screening_answer_id
				""", ScreeningAnswer.class, screeningSessionScreeningId, ScreeningQuestionAnsweredStatusId.CURRENT);
	}

	@Nonnull
	public List<ScreeningAnswer> findCurrentScreeningAnswersByScreeningSessionScreeningIdAndQuestionId(@Nullable UUID screeningSessionScreeningId,
																																																		 @Nullable UUID screeningQuestionId) {
		if (screeningSessionScreeningId == null || screeningQuestionId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("""
				SELECT sa.*
				FROM screening_session_answered_screening_question ssasq, screening_question sq, screening_answer_option sao, screening_answer sa
				WHERE ssasq.screening_session_screening_id=?
				AND ssasq.screening_question_answered_status_id=?
				AND ssasq.screening_question_id=?
				AND ssasq.screening_question_id=sq.screening_question_id
				AND sq.screening_question_id=sao.screening_question_id
				AND sa.screening_answer_option_id=sao.screening_answer_option_id
				ORDER BY sa.created, sa.screening_answer_id
				""", ScreeningAnswer.class, screeningSessionScreeningId, ScreeningQuestionAnsweredStatusId.CURRENT, screeningQuestionId);
	}

	@Nonnull
	protected List<ScreeningSessionScreening> findScreeningSessionScreeningsByScreeningSessionId(@Nullable UUID screeningSessionId) {
		if (screeningSessionId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("""
				SELECT * FROM screening_session_screening
				WHERE screening_session_id=?
				ORDER BY screening_order
				""", ScreeningSessionScreening.class, screeningSessionId);
	}

	@Nonnull
	public List<UUID> createScreeningAnswers(@Nullable CreateScreeningAnswersRequest request) {
		requireNonNull(request);

		UUID screeningSessionScreeningId = request.getScreeningSessionScreeningId();
		UUID screeningQuestionId = request.getScreeningQuestionId();
		List<CreateAnswerRequest> answers = request.getAnswers() == null ? List.of() : request.getAnswers().stream()
				.filter(answer -> answer != null)
				.collect(Collectors.toList());
		UUID createdByAccountId = request.getCreatedByAccountId();
		ScreeningSessionScreening screeningSessionScreening = null;
		ScreeningQuestion screeningQuestion = null;
		List<ScreeningAnswerOption> screeningAnswerOptions = new ArrayList<>();
		Account createdByAccount = null;
		ValidationException validationException = new ValidationException();

		if (screeningSessionScreeningId == null) {
			validationException.add(new FieldError("screeningSessionScreeningId", getStrings().get("Screening session screening ID is required.")));
		} else {
			screeningSessionScreening = findScreeningSessionScreeningById(screeningSessionScreeningId).orElse(null);

			if (screeningSessionScreening == null)
				validationException.add(new FieldError("screeningSessionScreening", getStrings().get("Screening session screening ID is invalid.")));
		}

		if (screeningQuestionId == null) {
			validationException.add(new FieldError("screeningQuestionId", getStrings().get("Screening question ID is required.")));
		} else {
			screeningQuestion = findScreeningQuestionById(screeningQuestionId).orElse(null);

			if (screeningQuestion == null) {
				validationException.add(new FieldError("screeningQuestionId", getStrings().get("Screening question ID is invalid.")));
			} else {
				ScreeningQuestion pinnedScreeningQuestion = screeningQuestion;

				if (answers.size() < screeningQuestion.getMinimumAnswerCount() || answers.size() > screeningQuestion.getMaximumAnswerCount()) {
					// Special case, friendlier message
					if (screeningQuestion.getMinimumAnswerCount() == 1 && screeningQuestion.getMaximumAnswerCount() == 1)
						validationException.add(new FieldError("answers", getStrings().get("You must answer the question to proceed.")));
					else if (answers.size() < screeningQuestion.getMinimumAnswerCount())
						validationException.add(new FieldError("answers", getStrings().get("You must choose at least {{minimumAnswerCount}} answers to proceed.", new HashMap<>() {{
							put("minimumAnswerCount", pinnedScreeningQuestion.getMinimumAnswerCount());
						}})));
					else if (answers.size() > screeningQuestion.getMaximumAnswerCount())
						validationException.add(new FieldError("answers", getStrings().get("You must choose at most {{maximumAnswerCount}} answers to proceed.", new HashMap<>() {{
							put("maximumAnswerCount", pinnedScreeningQuestion.getMaximumAnswerCount());
						}})));
					else
						validationException.add(new FieldError("answers", getStrings().get("You must choose {{minimumAnswerCount}}-{{maximumAnswerCount}} answers to proceed.", new HashMap<>() {{
							put("minimumAnswerCount", pinnedScreeningQuestion.getMinimumAnswerCount());
							put("maximumAnswerCount", pinnedScreeningQuestion.getMaximumAnswerCount());
						}})));
				} else {
					int i = 0;
					boolean illegalScreeningQuestionId = false;

					for (CreateAnswerRequest answer : answers) {
						UUID screeningAnswerOptionId = answer.getScreeningAnswerOptionId();
						String text = trimToNull(answer.getText());

						// Use the trimmed version for insert later
						answer.setText(text);

						if (screeningAnswerOptionId == null) {
							validationException.add(new FieldError(format("answers.screeningAnswerOptionId[%d]", i), getStrings().get("Screening answer option ID is required.")));
						} else {
							ScreeningAnswerOption screeningAnswerOption = findScreeningAnswerOptionById(screeningAnswerOptionId).orElse(null);

							if (screeningAnswerOption == null) {
								validationException.add(new FieldError(format("answers.screeningAnswerOptionId[%d]", i), getStrings().get("Screening answer option ID is invalid.")));
							} else if (screeningQuestion != null) {
								if (!screeningAnswerOption.getScreeningQuestionId().equals(screeningQuestionId))
									illegalScreeningQuestionId = true;

								if (screeningQuestion.getScreeningAnswerFormatId() == ScreeningAnswerFormatId.FREEFORM_TEXT) {
									if (text == null) {
										validationException.add(new FieldError("text", getStrings().get("Your response is required.")));
									} else {
										switch (screeningQuestion.getScreeningAnswerContentHintId()) {
											case PHONE_NUMBER -> {
												text = getNormalizer().normalizePhoneNumberToE164(text).orElse(null);

												if (text == null)
													validationException.add(new FieldError("text", getStrings().get("A valid phone number is required.")));
											}
											case EMAIL_ADDRESS -> {
												text = getNormalizer().normalizeEmailAddress(text).orElse(null);

												if (!isValidEmailAddress(text))
													validationException.add(new FieldError("text", getStrings().get("A valid email address is required.")));
											}
										}
									}
								}

								screeningAnswerOptions.add(screeningAnswerOption);
							}
						}

						++i;
					}

					if (illegalScreeningQuestionId)
						validationException.add(getStrings().get("You can only supply answers for the current question."));
				}
			}
		}

		if (createdByAccountId == null) {
			validationException.add(new FieldError("createdByAccountId", getStrings().get("Created by account ID is required.")));
		} else {
			createdByAccount = getAccountService().findAccountById(createdByAccountId).orElse(null);

			if (createdByAccount == null)
				validationException.add(new FieldError("createdByAccountId", getStrings().get("Created by account ID is invalid.")));
		}

		if (validationException.hasErrors())
			throw validationException;

		// Screening Scoring Function

		// Temporary hack for testing
		String scoringFunctionJs = """
				// We are completed if the number of answers matches the number of questions
				output.completed = input.screeningAnswers.length === input.screeningQuestionsWithAnswerOptions.length;
								
				// Track running score
				output.score = 0;
				    
				// Add each answer's score to the running total
				input.screeningAnswers.forEach(function(screeningAnswer) {
				  const screeningAnswerOption = input.screeningAnswerOptionsByScreeningAnswerId[screeningAnswer.screeningAnswerId];
				  output.score += screeningAnswerOption.score;
				});
				""";
		getDatabase().execute("UPDATE screening_version SET scoring_function=? WHERE screening_version_id=?", scoringFunctionJs, screeningSessionScreening.getScreeningVersionId());
		// End temporary hack

		ScreeningSession screeningSession = findScreeningSessionById(screeningSessionScreening.getScreeningSessionId()).get();
		ScreeningVersion screeningVersion = findScreeningVersionById(screeningSessionScreening.getScreeningVersionId()).get();
		ScreeningFlowVersion screeningFlowVersion = findScreeningFlowVersionById(screeningSession.getScreeningFlowVersionId()).get();

		// TODO: if we are re-applying the same answers to an already-answered question, no-op and return immediately

		// TODO: if we are re-answering in the same screening session screening, invalidate the existing answer and and downstream answers
		// Do this by getting the created timestamp of the previous screening_session_answered_screening_question,
		// finding any screening_session_answered_screening_question records created after that timestamp, and marking them as
		// implicitly invalidated.  Mark the one we're replacing as explicitly invalidated.

		UUID screeningSessionAnsweredScreeningQuestionId = UUID.randomUUID();

		getDatabase().execute("""
				INSERT INTO screening_session_answered_screening_question (screening_session_answered_screening_question_id,
				screening_session_screening_id, screening_question_id, screening_question_answered_status_id)
				VALUES (?,?,?,?)
				""", screeningSessionAnsweredScreeningQuestionId, screeningSessionScreeningId, screeningQuestionId, ScreeningQuestionAnsweredStatusId.CURRENT);

		List<UUID> screeningAnswerIds = new ArrayList<>(answers.size());

		// TODO: we can do a batch insert for slightly better performance here
		for (CreateAnswerRequest answer : answers) {
			UUID screeningAnswerId = UUID.randomUUID();
			screeningAnswerIds.add(screeningAnswerId);

			getDatabase().execute("""
					INSERT INTO screening_answer (screening_answer_id, screening_answer_option_id, screening_session_answered_screening_question_id, created_by_account_id, text)
					VALUES (?,?,?,?,?)
					""", screeningAnswerId, answer.getScreeningAnswerOptionId(), screeningSessionAnsweredScreeningQuestionId, createdByAccountId, answer.getText());
		}

		// Score the individual screening by calling its scoring function
		List<ScreeningQuestionWithAnswerOptions> screeningQuestionsWithAnswerOptions =
				findScreeningQuestionsWithAnswerOptionsByScreeningSessionScreeningId(screeningSessionScreeningId);
		List<ScreeningAnswer> screeningAnswers = findCurrentScreeningAnswersByScreeningSessionScreeningId(screeningSessionScreeningId);

		ScreeningScoringOutput screeningScoringOutput = executeScreeningScoringFunction(
				screeningVersion.getScoringFunction(), screeningQuestionsWithAnswerOptions, screeningAnswers);

		getLogger().info("Screening session screening ID {} ({}) was scored {} with completed flag={}. {} out of {} questions have been answered.", screeningSessionScreeningId,
				screeningVersion.getScreeningTypeId().name(), screeningScoringOutput.getScore(), screeningScoringOutput.getCompleted(), screeningAnswers.size(), screeningQuestionsWithAnswerOptions.size());

		// Based on screening scoring function output, set score/completed flags
		getDatabase().execute("""
				UPDATE screening_session_screening 
				SET completed=?, score=?
				WHERE screening_session_screening_id=?
				""", screeningScoringOutput.getCompleted(), screeningScoringOutput.getScore(), screeningSessionScreening.getScreeningSessionScreeningId());

		// Screening Flow Orchestration Function

		// Temporary hack for testing
		String orchestrationFunctionJs = """
				console.log("** Starting orchestration function");

				output.crisisIndicated = false;
				output.completed = false;
				output.nextScreeningId = null;
				output.hardStop = false;

				// WHO-5 always comes first.				
				const who5 = input.screeningSessionScreenings[0];
				const phq9 = input.screeningSessionScreenings.length > 1 ? input.screeningSessionScreenings[1] : null;
				const gad7 = input.screeningSessionScreenings.length > 2 ? input.screeningSessionScreenings[2] : null;				
				const screeningsCount = input.screeningSessionScreenings.length;

				if (screeningsCount === 1) {
				  // We have not yet progressed past WHO-5
				  if (who5.completed) {
				    console.log("WHO-5 is complete.  Score is " + who5.score);
				    if (who5.score >= 13) {
				      // We're done; triage to resilience coach support role
				      // TODO: triage here, or in scoring function?
				      output.completed = true;
				    } else {
				      // Complete PHQ9 + GAD7
				      output.nextScreeningId = input.screeningsByName["PHQ-9"].screeningId;
				    }
				  } else {
				    console.log("WHO-5 not complete yet.  Score is " + who5.score);
				  }
				} else if (screeningsCount === 2) {
				  // We are on PHQ-9. Is it done yet?
				  if (phq9.completed) {
				    console.log("PHQ-9 is complete.  Score is " + phq9.score);
				    
				    const phq9Questions = input.screeningResultsByScreeningSessionScreeningId[phq9.screeningSessionScreeningId];
				    const phq9Question9 = phq9Questions[8];
				  
				    // PHQ-9 crisis is indicated if Q9 is scored >= 1
				    if(phq9Question9.screeningResponses[0].screeningAnswerOption.score >= 1) {
				      console.log("Crisis indicated, hard stop.");
				    	output.crisisIndicated = true;
				    	output.completed = true;
				    } else {
				      console.log("Crisis not indicated, starting GAD-7");
				    	output.nextScreeningId = input.screeningsByName["GAD-7"].screeningId;
				    }				    
				  } else {
				    console.log("PHQ-9 not complete yet.  Score is " + phq9.score);
				  }
				} else if (screeningsCount === 3) {
				  // We are on GAD-7. Is it done yet?
				  if (gad7.completed) {
				    // We're done!
				    // TODO: triage here, or in scoring function?
				    output.completed = true;
				  } else {
				    console.log("GAD-7 not complete yet.  Score is " + gad7.score);
				  }
				} else {
				  throw "There is an unexpected number of screening session screenings";
				}

				console.log("** Finished orchestration function");
								""";

		getDatabase().execute("UPDATE screening_flow_version SET orchestration_function=? WHERE screening_flow_version_id=?", orchestrationFunctionJs, screeningSession.getScreeningFlowVersionId());
		screeningFlowVersion = findScreeningFlowVersionById(screeningSession.getScreeningFlowVersionId()).get();
		// End temporary hack

		// Pull data we'll need to pass in to the orchestration function
		List<Screening> screenings = findScreeningsByInstitutionId(createdByAccount.getInstitutionId());
		OrchestrationFunctionOutput orchestrationFunctionOutput = executeScreeningFlowOrchestrationFunction(screeningFlowVersion.getOrchestrationFunction(), screenings, screeningSession.getScreeningSessionId());

		if (orchestrationFunctionOutput.getNextScreeningId() != null) {
			Integer nextScreeningOrder = getDatabase().queryForObject("""
					SELECT MAX(screening_order) + 1 
					FROM screening_session_screening 
					WHERE screening_session_id=?
					""", Integer.class, screeningSession.getScreeningSessionId()).get();

			Screening nextScreening = findScreeningById(orchestrationFunctionOutput.getNextScreeningId()).get();
			ScreeningVersion nextScreeningVersion = findScreeningVersionById(nextScreening.getActiveScreeningVersionId()).get();

			getLogger().info("Orchestration function for screening session screening ID {} ({}) indicates that we should transition to screening ID {} ({}).", screeningSessionScreeningId,
					screeningVersion.getScreeningTypeId().name(), nextScreening.getScreeningId(), nextScreeningVersion.getScreeningTypeId().name());

			getDatabase().execute("""
					INSERT INTO screening_session_screening(screening_session_id, screening_version_id, screening_order)
					VALUES (?,?,?)
					""", screeningSession.getScreeningSessionId(), nextScreeningVersion.getScreeningVersionId(), nextScreeningOrder);
		}

		// If orchestration logic says we are in crisis, trigger crisis flow
		if (orchestrationFunctionOutput.getCrisisIndicated()) {
			if (screeningSession.getCrisisIndicated()) {
				getLogger().info("Orchestration function for screening session screening ID {} ({}) indicated crisis. " +
						"This session was already marked as having a crisis indicated, so no action needed.", screeningSessionScreeningId, screeningVersion.getScreeningTypeId().name());
			} else {
				getLogger().info("Orchestration function for screening session screening ID {} ({}) indicated crisis.  Creating crisis interacting instance...",
						screeningSessionScreeningId, screeningVersion.getScreeningTypeId().name());

				// TODO: Create interaction instance to be sent to relevant care provider[s] that includes screening Q and A values from this flow + scores
			}
		}

		if (orchestrationFunctionOutput.getCompleted()) {
			getLogger().info("Orchestration function for screening session screening ID {} ({}) indicated that screening session ID {} is now complete.", screeningSessionScreeningId,
					screeningVersion.getScreeningTypeId().name(), screeningSession.getScreeningSessionId());
			getDatabase().execute("UPDATE screening_session SET completed=TRUE WHERE screening_session_id=?", screeningSession.getScreeningSessionId());
		}

		// TODO: execute scoring function and insert triage-related records (support role, etc.)

		return screeningAnswerIds;
	}

	@Nonnull
	protected ScreeningScoringOutput executeScreeningScoringFunction(@Nonnull String screeningScoringFunctionJavascript,
																																	 @Nonnull List<ScreeningQuestionWithAnswerOptions> screeningQuestionsWithAnswerOptions,
																																	 @Nonnull List<ScreeningAnswer> screeningAnswers) {
		requireNonNull(screeningScoringFunctionJavascript);
		requireNonNull(screeningQuestionsWithAnswerOptions);
		requireNonNull(screeningAnswers);

		ScreeningScoringOutput screeningScoringOutput;

		// Massage data a bit to make it easier for function to get a screening answer option given a screening answer ID
		Map<UUID, ScreeningAnswerOption> screeningAnswerOptionsById = new HashMap<>();
		Map<UUID, ScreeningAnswerOption> screeningAnswerOptionsByScreeningAnswerId = new HashMap<>(screeningAnswers.size());

		for (ScreeningQuestionWithAnswerOptions screeningQuestionWithAnswerOptions : screeningQuestionsWithAnswerOptions)
			for (ScreeningAnswerOption screeningAnswerOption : screeningQuestionWithAnswerOptions.getScreeningAnswerOptions())
				screeningAnswerOptionsById.put(screeningAnswerOption.getScreeningAnswerOptionId(), screeningAnswerOption);

		for (ScreeningAnswer screeningAnswer : screeningAnswers)
			screeningAnswerOptionsByScreeningAnswerId.put(screeningAnswer.getScreeningAnswerId(), screeningAnswerOptionsById.get(screeningAnswer.getScreeningAnswerOptionId()));

		// TODO: refactor this to be more like the orchestration function, specifically the "screeningResponses" construct
		Map<String, Object> context = new HashMap<>();
		context.put("screeningQuestionsWithAnswerOptions", screeningQuestionsWithAnswerOptions);
		context.put("screeningAnswers", screeningAnswers);
		context.put("screeningAnswerOptionsByScreeningAnswerId", screeningAnswerOptionsByScreeningAnswerId);

		try {
			screeningScoringOutput = getJavascriptExecutor().execute(screeningScoringFunctionJavascript, context, ScreeningScoringOutput.class);
		} catch (JavascriptExecutionException e) {
			throw new RuntimeException(e);
		}

		if (screeningScoringOutput.getCompleted() == null)
			throw new IllegalStateException("Screening scoring function must provide a 'completed' value in output");

		if (screeningScoringOutput.getScore() == null)
			throw new IllegalStateException("Screening scoring function must provide a 'score' value in output");

		return screeningScoringOutput;
	}

	@Nonnull
	protected OrchestrationFunctionOutput executeScreeningFlowOrchestrationFunction(@Nonnull String screeningFlowOrchestrationFunctionJavascript,
																																									@Nonnull List<Screening> screenings,
																																									@Nonnull UUID screeningSessionId) {
		requireNonNull(screeningFlowOrchestrationFunctionJavascript);
		requireNonNull(screenings);
		requireNonNull(screeningSessionId);

		OrchestrationFunctionOutput orchestrationFunctionOutput;

		// Massage data a bit to make it easier for function to get a handle to a screening by using its name
		Map<String, Screening> screeningsByName = new HashMap<>(screenings.size());

		for (Screening screening : screenings)
			screeningsByName.put(screening.getName(), screening);

		// Set up data so it's easy to access questions/answers to make decisions, look at scores, etc.
		//
		// Example:
		//
		// const phq9Questions = input.screeningQuestionsByScreeningSessionScreeningId[phq9.screeningSessionScreeningId];
		// const phq9Question9 = phq9Questions[8];
		//
		// // The screeningResponses array is only present if the user has answered the question in some way
		// console.log(phq9Question9.screeningResponses[0].screeningAnswerOption.score); // easy access to the score for the selected answer option
		// console.log(phq9Question9.screeningResponses[0].screeningAnswer.text); // the answers themselves, in case you need them (e.g. free-form text)
		List<ScreeningSessionScreening> screeningSessionScreenings = findScreeningSessionScreeningsByScreeningSessionId(screeningSessionId);
		Map<UUID, Object> screeningResultsByScreeningSessionScreeningId = new HashMap<>();

		// We could do this as a single query, but the dataset is small, and this is a little clearer and fast enough
		for (ScreeningSessionScreening screeningSessionScreening : screeningSessionScreenings) {
			List<ScreeningQuestionWithAnswerOptions> screeningQuestionsWithAnswerOptions = findScreeningQuestionsWithAnswerOptionsByScreeningSessionScreeningId(screeningSessionScreening.getScreeningSessionScreeningId());
			List<ScreeningAnswer> screeningAnswers = findCurrentScreeningAnswersByScreeningSessionScreeningId(screeningSessionScreening.getScreeningSessionScreeningId());
			Map<UUID, ScreeningAnswer> screeningAnswersByAnswerOptionId = new HashMap<>(screeningAnswers.size());

			for (ScreeningAnswer screeningAnswer : screeningAnswers)
				screeningAnswersByAnswerOptionId.put(screeningAnswer.getScreeningAnswerOptionId(), screeningAnswer);

			List<Map<String, Object>> screeningResults = new ArrayList<>();

			for (ScreeningQuestionWithAnswerOptions screeningQuestionsWithAnswerOption : screeningQuestionsWithAnswerOptions) {
				Map<String, Object> screeningResult = new HashMap<>();

				ScreeningQuestion screeningQuestion = screeningQuestionsWithAnswerOption.getScreeningQuestion();
				List<ScreeningAnswerOption> screeningAnswerOptions = screeningQuestionsWithAnswerOption.getScreeningAnswerOptions();
				Map<UUID, ScreeningAnswerOption> screeningAnswerOptionsById = new HashMap<>(screeningAnswerOptions.size());

				for (ScreeningAnswerOption screeningAnswerOption : screeningAnswerOptions)
					screeningAnswerOptionsById.put(screeningAnswerOption.getScreeningAnswerOptionId(), screeningAnswerOption);

				screeningResult.put("screeningQuestionId", screeningQuestion.getScreeningQuestionId());
				screeningResult.put("screeningAnswerFormatId", screeningQuestion.getScreeningAnswerFormatId());
				screeningResult.put("screeningAnswerContentHintId", screeningQuestion.getScreeningAnswerContentHintId());
				screeningResult.put("questionText", screeningQuestion.getQuestionText());
				screeningResult.put("minimumAnswerCount", screeningQuestion.getMinimumAnswerCount());
				screeningResult.put("maximumAnswerCount", screeningQuestion.getMaximumAnswerCount());

				// Each element in this list is a screeningAnswerOption and screeningAnswer pair
				List<Map<String, Object>> screeningResponses = new ArrayList<>();

				for (ScreeningAnswer screeningAnswer : screeningAnswers) {
					if (screeningAnswerOptionsById.keySet().contains(screeningAnswer.getScreeningAnswerOptionId())) {
						ScreeningAnswerOption screeningAnswerOption = screeningAnswerOptionsById.get(screeningAnswer.getScreeningAnswerOptionId());

						Map<String, Object> screeningAnswerOptionJson = new HashMap<>(4);
						screeningAnswerOptionJson.put("screeningAnswerOptionId", screeningAnswerOption.getScreeningAnswerOptionId());
						screeningAnswerOptionJson.put("answerOptionText", screeningAnswerOption.getAnswerOptionText());
						screeningAnswerOptionJson.put("indicatesCrisis", screeningAnswerOption.getIndicatesCrisis());
						screeningAnswerOptionJson.put("score", screeningAnswerOption.getScore());

						Map<String, Object> screeningAnswerJson = new HashMap<>(2);
						screeningAnswerJson.put("screeningAnswerId", screeningAnswer.getScreeningAnswerId());
						screeningAnswerJson.put("text", screeningAnswer.getText());

						Map<String, Object> screeningResponse = new HashMap<>(2);
						screeningResponse.put("screeningAnswerOption", screeningAnswerOptionJson);
						screeningResponse.put("screeningAnswer", screeningAnswerJson);

						screeningResponses.add(screeningResponse);
					}
				}

				screeningResult.put("screeningResponses", screeningResponses);
				screeningResults.add(screeningResult);
			}

			screeningResultsByScreeningSessionScreeningId.put(screeningSessionScreening.getScreeningSessionScreeningId(), screeningResults);
		}

		Map<String, Object> context = new HashMap<>();
		context.put("screenings", screenings);
		context.put("screeningsByName", screeningsByName);
		context.put("screeningSessionScreenings", screeningSessionScreenings);
		context.put("screeningResultsByScreeningSessionScreeningId", screeningResultsByScreeningSessionScreeningId);

		try {
			orchestrationFunctionOutput = getJavascriptExecutor().execute(screeningFlowOrchestrationFunctionJavascript, context, OrchestrationFunctionOutput.class);
		} catch (JavascriptExecutionException e) {
			throw new RuntimeException(e);
		}

		if (orchestrationFunctionOutput.getCompleted() == null)
			throw new IllegalStateException("Orchestration function must provide a 'completed' value in output");

		if (orchestrationFunctionOutput.getCrisisIndicated() == null)
			throw new IllegalStateException("Orchestration function must provide a 'crisisIndicated' value in output");

		if (orchestrationFunctionOutput.getCompleted() && orchestrationFunctionOutput.getNextScreeningId() != null)
			throw new IllegalStateException(format("Orchestration function output says this screening session is completed, but also provides a nonnull 'nextScreeningId' value"));

		return orchestrationFunctionOutput;
	}

	@NotThreadSafe
	protected static class ScreeningScoringOutput {
		@Nullable
		private Boolean completed;
		@Nullable
		private Integer score;

		@Nullable
		public Boolean getCompleted() {
			return this.completed;
		}

		public void setCompleted(@Nullable Boolean completed) {
			this.completed = completed;
		}

		@Nullable
		public Integer getScore() {
			return this.score;
		}

		public void setScore(@Nullable Integer score) {
			this.score = score;
		}
	}

	@NotThreadSafe
	protected static class OrchestrationFunctionOutput {
		@Nullable
		private Boolean crisisIndicated;
		@Nullable
		private Boolean completed;
		@Nullable
		private UUID nextScreeningId;

		@Nullable
		public Boolean getCrisisIndicated() {
			return this.crisisIndicated;
		}

		public void setCrisisIndicated(@Nullable Boolean crisisIndicated) {
			this.crisisIndicated = crisisIndicated;
		}

		@Nullable
		public Boolean getCompleted() {
			return this.completed;
		}

		public void setCompleted(@Nullable Boolean completed) {
			this.completed = completed;
		}

		@Nullable
		public UUID getNextScreeningId() {
			return this.nextScreeningId;
		}

		public void setNextScreeningId(@Nullable UUID nextScreeningId) {
			this.nextScreeningId = nextScreeningId;
		}
	}

	@Nonnull
	protected AccountService getAccountService() {
		return this.accountServiceProvider.get();
	}

	@Nonnull
	protected AuthorizationService getAuthorizationService() {
		return this.authorizationServiceProvider.get();
	}

	@Nonnull
	protected JavascriptExecutor getJavascriptExecutor() {
		return this.javascriptExecutor;
	}

	@Nonnull
	protected Normalizer getNormalizer() {
		return this.normalizer;
	}

	@Nonnull
	protected Database getDatabase() {
		return this.database;
	}

	@Nonnull
	protected Strings getStrings() {
		return this.strings;
	}

	@Nonnull
	protected Logger getLogger() {
		return this.logger;
	}
}
