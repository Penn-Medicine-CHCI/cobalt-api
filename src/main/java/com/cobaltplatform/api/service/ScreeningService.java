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

import com.cobaltplatform.api.error.ErrorReporter;
import com.cobaltplatform.api.model.api.request.CreateScreeningAnswersRequest;
import com.cobaltplatform.api.model.api.request.CreateScreeningAnswersRequest.CreateAnswerRequest;
import com.cobaltplatform.api.model.api.request.CreateScreeningSessionRequest;
import com.cobaltplatform.api.model.api.request.SkipScreeningSessionRequest;
import com.cobaltplatform.api.model.api.response.ScreeningConfirmationPromptApiResponse.ScreeningConfirmationPromptApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.Screening;
import com.cobaltplatform.api.model.db.ScreeningAnswer;
import com.cobaltplatform.api.model.db.ScreeningAnswerFormat.ScreeningAnswerFormatId;
import com.cobaltplatform.api.model.db.ScreeningAnswerOption;
import com.cobaltplatform.api.model.db.ScreeningConfirmationPrompt;
import com.cobaltplatform.api.model.db.ScreeningFlow;
import com.cobaltplatform.api.model.db.ScreeningFlowVersion;
import com.cobaltplatform.api.model.db.ScreeningQuestion;
import com.cobaltplatform.api.model.db.ScreeningSession;
import com.cobaltplatform.api.model.db.ScreeningSessionAnsweredScreeningQuestion;
import com.cobaltplatform.api.model.db.ScreeningSessionScreening;
import com.cobaltplatform.api.model.db.ScreeningType;
import com.cobaltplatform.api.model.db.ScreeningType.ScreeningTypeId;
import com.cobaltplatform.api.model.db.ScreeningVersion;
import com.cobaltplatform.api.model.db.SupportRole;
import com.cobaltplatform.api.model.db.SupportRole.SupportRoleId;
import com.cobaltplatform.api.model.service.ScreeningQuestionContext;
import com.cobaltplatform.api.model.service.ScreeningQuestionContextId;
import com.cobaltplatform.api.model.service.ScreeningQuestionWithAnswerOptions;
import com.cobaltplatform.api.model.service.ScreeningScore;
import com.cobaltplatform.api.model.service.ScreeningSessionDestination;
import com.cobaltplatform.api.model.service.ScreeningSessionDestination.ScreeningSessionDestinationId;
import com.cobaltplatform.api.service.ScreeningService.ResultsFunctionOutput.SupportRoleRecommendation;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
	private final Provider<InstitutionService> institutionServiceProvider;
	@Nonnull
	private final Provider<InteractionService> interactionServiceProvider;
	@Nonnull
	private final Provider<AccountService> accountServiceProvider;
	@Nonnull
	private final Provider<AuthorizationService> authorizationServiceProvider;
	@Nonnull
	private final ScreeningConfirmationPromptApiResponseFactory screeningConfirmationPromptApiResponseFactory;
	@Nonnull
	private final JavascriptExecutor javascriptExecutor;
	@Nonnull
	private final ErrorReporter errorReporter;
	@Nonnull
	private final Normalizer normalizer;
	@Nonnull
	private final Database database;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;

	@Inject
	public ScreeningService(@Nonnull Provider<InstitutionService> institutionServiceProvider,
													@Nonnull Provider<InteractionService> interactionServiceProvider,
													@Nonnull Provider<AccountService> accountServiceProvider,
													@Nonnull Provider<AuthorizationService> authorizationServiceProvider,
													@Nonnull ScreeningConfirmationPromptApiResponseFactory screeningConfirmationPromptApiResponseFactory,
													@Nonnull JavascriptExecutor javascriptExecutor,
													@Nonnull ErrorReporter errorReporter,
													@Nonnull Normalizer normalizer,
													@Nonnull Database database,
													@Nonnull Strings strings) {
		requireNonNull(institutionServiceProvider);
		requireNonNull(interactionServiceProvider);
		requireNonNull(accountServiceProvider);
		requireNonNull(authorizationServiceProvider);
		requireNonNull(screeningConfirmationPromptApiResponseFactory);
		requireNonNull(javascriptExecutor);
		requireNonNull(errorReporter);
		requireNonNull(normalizer);
		requireNonNull(database);
		requireNonNull(strings);

		this.institutionServiceProvider = institutionServiceProvider;
		this.interactionServiceProvider = interactionServiceProvider;
		this.accountServiceProvider = accountServiceProvider;
		this.authorizationServiceProvider = authorizationServiceProvider;
		this.screeningConfirmationPromptApiResponseFactory = screeningConfirmationPromptApiResponseFactory;
		this.javascriptExecutor = javascriptExecutor;
		this.errorReporter = errorReporter;
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
	public Optional<ScreeningType> findScreeningTypeById(@Nullable ScreeningTypeId screeningTypeId) {
		if (screeningTypeId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM screening_type WHERE screening_type_id=?",
				ScreeningType.class, screeningTypeId);
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
	public Optional<ScreeningConfirmationPrompt> findScreeningConfirmationPromptById(@Nullable UUID screeningConfirmationPromptId) {
		if (screeningConfirmationPromptId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM screening_confirmation_prompt WHERE screening_confirmation_prompt_id=?",
				ScreeningConfirmationPrompt.class, screeningConfirmationPromptId);
	}

	@Nonnull
	public List<SupportRole> findRecommendedSupportRolesByAccountId(@Nullable UUID accountId,
																																	@Nullable UUID triageScreeningFlowId) {
		if (accountId == null || triageScreeningFlowId == null)
			return Collections.emptyList();

		Account account = getAccountService().findAccountById(accountId).orElse(null);

		if (account == null)
			return Collections.emptyList();

		ScreeningSession mostRecentCompletedTriageScreeningSession = getDatabase().queryForObject("""
				SELECT ss.*
				FROM screening_session ss, screening_flow_version sfv
				WHERE sfv.screening_flow_version_id=ss.screening_flow_version_id
				AND sfv.screening_flow_id=? 
				AND ss.completed = TRUE
				AND ss.target_account_id=?
				ORDER BY ss.last_updated DESC
				LIMIT 1
				""", ScreeningSession.class, triageScreeningFlowId, accountId).orElse(null);

		if (mostRecentCompletedTriageScreeningSession == null)
			return Collections.emptyList();

		List<SupportRole> recommendedSupportRoles = getDatabase().queryForList("""
				SELECT sr.*
				FROM support_role sr, screening_session_support_role_recommendation sssrr
				WHERE sr.support_role_id=sssrr.support_role_id
				AND sssrr.screening_session_id=?
				ORDER BY sssrr.weight DESC
				""", SupportRole.class, mostRecentCompletedTriageScreeningSession.getScreeningSessionId());

		return recommendedSupportRoles;
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
	public List<ScreeningFlowVersion> findScreeningFlowVersionsByScreeningFlowId(@Nullable UUID screeningFlowId) {
		if (screeningFlowId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("""
				SELECT *
				FROM screening_flow_version
				WHERE screening_flow_id=?
				ORDER BY version_number DESC
				""", ScreeningFlowVersion.class, screeningFlowId);
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
	public List<ScreeningSession> findScreeningSessionsByScreeningFlowIdAndTargetAccountId(@Nullable UUID screeningFlowId,
																																												 @Nullable UUID targetAccountId) {
		if (screeningFlowId == null || targetAccountId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("""
						SELECT ss.* FROM screening_session ss, screening_flow_version sfv 
						WHERE sfv.screening_flow_id=? AND ss.screening_flow_version_id=sfv.screening_flow_version_id 
						AND ss.target_account_id=?
						ORDER BY ss.created DESC
						""",
				ScreeningSession.class, screeningFlowId, targetAccountId);
	}

	@Nonnull
	public Optional<ScreeningSession> findMostRecentlyCompletedScreeningSessionByScreeningFlowAndTargetAccountId(@Nullable UUID screeningFlowId,
																																																							 @Nullable UUID targetAccountId) {
		if (screeningFlowId == null || targetAccountId == null)
			return Optional.empty();

		List<ScreeningSession> completedScreeningSessions = findScreeningSessionsByScreeningFlowIdAndTargetAccountId(screeningFlowId, targetAccountId).stream()
				.filter(screeningSession -> screeningSession.getCompleted() && !screeningSession.getSkipped())
				.collect(Collectors.toList());

		// Most recently completed first
		Collections.sort(completedScreeningSessions, (ss1, ss2) -> ss2.getCompletedAt().compareTo(ss1.getCompletedAt()));

		return Optional.ofNullable(completedScreeningSessions.size() > 0 ? completedScreeningSessions.get(0) : null);
	}

	@Nonnull
	public List<ScreeningSessionScreening> findScreeningSessionScreeningsByScreeningSessionId(@Nullable UUID screeningSessionId) {
		if (screeningSessionId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("""
				SELECT * 
				FROM v_screening_session_screening 
				WHERE screening_session_id=? 
				ORDER BY screening_order
				""", ScreeningSessionScreening.class, screeningSessionId);
	}

	@Nonnull
	public UUID createScreeningSession(@Nonnull CreateScreeningSessionRequest request) {
		requireNonNull(request);

		UUID targetAccountId = request.getTargetAccountId();
		UUID createdByAccountId = request.getCreatedByAccountId();
		UUID screeningFlowId = request.getScreeningFlowId();
		UUID screeningFlowVersionId = request.getScreeningFlowVersionId();
		ScreeningFlowVersion screeningFlowVersion = null;
		Account targetAccount = null;
		Account createdByAccount = null;
		boolean immediatelySkip = request.getImmediatelySkip() == null ? false : request.getImmediatelySkip();
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

		if (screeningFlowId == null && screeningFlowVersionId == null) {
			validationException.add(getStrings().get("Either a screening flow ID or screening flow version ID is required."));
		} else if (screeningFlowId != null && screeningFlowVersionId != null) {
			validationException.add(getStrings().get("You cannot provide both a screening flow ID and a screening flow version ID."));
		} else if (screeningFlowVersionId != null) {
			screeningFlowVersion = findScreeningFlowVersionById(screeningFlowVersionId).orElse(null);

			if (screeningFlowVersion == null)
				validationException.add(new FieldError("screeningFlowVersionId", getStrings().get("Screening flow ID is invalid.")));
		} else {
			ScreeningFlow screeningFlow = findScreeningFlowById(screeningFlowId).orElse(null);

			if (screeningFlow == null)
				validationException.add(new FieldError("screeningFlowId", getStrings().get("Screening flow ID is invalid.")));
			else
				screeningFlowVersion = findScreeningFlowVersionById(screeningFlow.getActiveScreeningFlowVersionId()).get();
		}

		if (validationException.hasErrors())
			throw validationException;

		getDatabase().execute("""
				INSERT INTO screening_session(screening_session_id, screening_flow_version_id, target_account_id, created_by_account_id)
				VALUES (?,?,?,?)
				""", screeningSessionId, screeningFlowVersion.getScreeningFlowVersionId(), targetAccountId, createdByAccountId);

		// If we're immediately skipping, mark this session as completed/skipped and do nothing else.
		// If we're not immediately skipping, create an initial screening session screening
		if (immediatelySkip) {
			skipScreeningSession(new SkipScreeningSessionRequest() {{
				setScreeningSessionId(screeningSessionId);
			}});
		} else {
			Screening screening = findScreeningById(screeningFlowVersion.getInitialScreeningId()).get();

			// Initial screening is the current version of the screening specified in the flow
			getDatabase().execute("""
					INSERT INTO screening_session_screening(screening_session_id, screening_version_id, screening_order)
					VALUES (?,?,?)
					""", screeningSessionId, screening.getActiveScreeningVersionId(), 1);
		}

		return screeningSessionId;
	}

	@Nonnull
	public void skipScreeningSession(@Nonnull SkipScreeningSessionRequest request) {
		requireNonNull(request);

		UUID screeningSessionId = request.getScreeningSessionId();
		ValidationException validationException = new ValidationException();

		if (screeningSessionId == null) {
			validationException.add(new FieldError("screeningSessionId", getStrings().get("Screening Session ID is required.")));
		} else {
			ScreeningSession screeningSession = findScreeningSessionById(screeningSessionId).orElse(null);

			if (screeningSession == null) {
				validationException.add(new FieldError("screeningSessionId", getStrings().get("Screening Session ID is invalid.")));
			} else {
				ScreeningFlowVersion screeningFlowVersion = findScreeningFlowVersionById(screeningSession.getScreeningFlowVersionId()).get();

				if (!screeningFlowVersion.getSkippable())
					validationException.add(getStrings().get("Sorry, you are not permitted to skip this screening."));
			}
		}

		if (validationException.hasErrors())
			throw validationException;

		getDatabase().execute("""
				UPDATE screening_session
				SET completed=TRUE, completed_at=NOW(), skipped=TRUE, skipped_at=NOW()
				WHERE screening_session_id=?
				""", screeningSessionId);
	}

	@Nonnull
	public Optional<ScreeningQuestionContext> findScreeningQuestionContextById(@Nullable ScreeningQuestionContextId screeningQuestionContextId) {
		if (screeningQuestionContextId == null)
			return Optional.empty();

		ScreeningSessionScreening screeningSessionScreening = findScreeningSessionScreeningById(screeningQuestionContextId.getScreeningSessionScreeningId()).orElse(null);

		if (screeningSessionScreening == null)
			return Optional.empty();

		// Get all the questions + answer options
		List<ScreeningQuestionWithAnswerOptions> screeningQuestionsWithAnswerOptions = findScreeningQuestionsWithAnswerOptionsByScreeningSessionScreeningId(screeningSessionScreening.getScreeningSessionScreeningId());

		ScreeningQuestionWithAnswerOptions screeningQuestionWithAnswerOptions = null;

		for (ScreeningQuestionWithAnswerOptions potentialScreeningQuestionWithAnswerOptions : screeningQuestionsWithAnswerOptions) {
			if (potentialScreeningQuestionWithAnswerOptions.getScreeningQuestion().getScreeningQuestionId().equals(screeningQuestionContextId.getScreeningQuestionId())) {
				screeningQuestionWithAnswerOptions = potentialScreeningQuestionWithAnswerOptions;
				break;
			}
		}

		if (screeningQuestionWithAnswerOptions == null)
			return Optional.empty();

		List<ScreeningAnswer> screeningAnswers = findScreeningAnswersByScreeningQuestionContextId(screeningQuestionContextId);

		return Optional.of(new ScreeningQuestionContext(screeningSessionScreening,
				screeningQuestionWithAnswerOptions.getScreeningQuestion(),
				screeningQuestionWithAnswerOptions.getScreeningAnswerOptions(),
				screeningAnswers));
	}

	@Nonnull
	public Optional<ScreeningQuestionContext> findPreviousScreeningQuestionContextByScreeningQuestionContextId(@Nullable ScreeningQuestionContextId screeningQuestionContextId) {
		if (screeningQuestionContextId == null)
			return Optional.empty();

		ScreeningSessionScreening screeningSessionScreening = findScreeningSessionScreeningById(screeningQuestionContextId.getScreeningSessionScreeningId()).orElse(null);

		if (screeningSessionScreening == null)
			return Optional.empty();

		// First, see if the previously-answered question is within the same screening session screening...
		List<ScreeningSessionAnsweredScreeningQuestion> screeningSessionAnsweredScreeningQuestions =
				findScreeningSessionAnsweredScreeningQuestionsByScreeningSessionScreeningId(screeningSessionScreening.getScreeningSessionScreeningId());

		UUID sameScreeningSessionScreeningPreviousAnsweredQuestionId = null;

		for (ScreeningSessionAnsweredScreeningQuestion screeningSessionAnsweredScreeningQuestion : screeningSessionAnsweredScreeningQuestions) {
			if (screeningSessionAnsweredScreeningQuestion.getScreeningQuestionId().equals(screeningQuestionContextId.getScreeningQuestionId()))
				break;

			sameScreeningSessionScreeningPreviousAnsweredQuestionId = screeningSessionAnsweredScreeningQuestion.getScreeningQuestionId();
		}

		// Great, we found a previously-answered question in the same screening.  Let's return its context
		if (sameScreeningSessionScreeningPreviousAnsweredQuestionId != null)
			return findScreeningQuestionContextById(new ScreeningQuestionContextId(
					screeningSessionScreening.getScreeningSessionScreeningId(), sameScreeningSessionScreeningPreviousAnsweredQuestionId));

		// The previously-answered question was not in the same screening session screening.
		// Let's check the previous screening session screening and find the last-answered question there.
		//
		// Note: we are assuming the previous screening has at least 1 answered question.  It's technically possible but not practically likely that we
		// will have "empty" screenings w/ no questions
		List<ScreeningSessionScreening> screeningSessionScreenings = findCurrentScreeningSessionScreeningsByScreeningSessionId(screeningSessionScreening.getScreeningSessionId());

		ScreeningSessionScreening previousScreeningSessionScreening = null;

		for (ScreeningSessionScreening potentialPreviousScreeningSessionScreening : screeningSessionScreenings) {
			if (potentialPreviousScreeningSessionScreening.getScreeningSessionScreeningId().equals(screeningSessionScreening.getScreeningSessionScreeningId()))
				break;

			previousScreeningSessionScreening = potentialPreviousScreeningSessionScreening;
		}

		// No previous screening in the session?  That means the request was for the first question of the first screening, so there is no previous context
		if (previousScreeningSessionScreening == null)
			return Optional.empty();

		// Grab the previous screening's last answered question
		List<ScreeningSessionAnsweredScreeningQuestion> previousScreeningSessionAnsweredScreeningQuestions =
				findScreeningSessionAnsweredScreeningQuestionsByScreeningSessionScreeningId(previousScreeningSessionScreening.getScreeningSessionScreeningId());

		// We should not be in this state
		if (previousScreeningSessionAnsweredScreeningQuestions.size() == 0)
			return Optional.empty();

		ScreeningSessionAnsweredScreeningQuestion previousScreeningSessionLastAnsweredScreeningQuestion =
				previousScreeningSessionAnsweredScreeningQuestions.get(previousScreeningSessionAnsweredScreeningQuestions.size() - 1);

		return findScreeningQuestionContextById(new ScreeningQuestionContextId(
				previousScreeningSessionScreening.getScreeningSessionScreeningId(), previousScreeningSessionLastAnsweredScreeningQuestion.getScreeningQuestionId()));
	}

	@Nonnull
	public Optional<ScreeningQuestionContext> findNextUnansweredScreeningQuestionContextByScreeningSessionId(@Nullable UUID screeningSessionId) {
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
		List<ScreeningSessionAnsweredScreeningQuestion> screeningSessionAnsweredScreeningQuestions = findScreeningSessionAnsweredScreeningQuestionsByScreeningSessionScreeningId(screeningSessionScreening.getScreeningSessionScreeningId());

		// Holder for the next unanswered question (if there is one)
		ScreeningQuestionWithAnswerOptions nextScreeningQuestionWithAnswerOptions = null;

		// These lists are both ordered by question order, so figure out "next" by picking the last-answered index + 1
		if (screeningSessionAnsweredScreeningQuestions.size() < screeningQuestionsWithAnswerOptions.size())
			nextScreeningQuestionWithAnswerOptions = screeningQuestionsWithAnswerOptions.get(screeningSessionAnsweredScreeningQuestions.size());

		// If everything was already answered, nothing comes next for this screening session screening
		if (nextScreeningQuestionWithAnswerOptions == null)
			return Optional.empty();

		return Optional.of(new ScreeningQuestionContext(screeningSessionScreening, nextScreeningQuestionWithAnswerOptions.getScreeningQuestion(),
				nextScreeningQuestionWithAnswerOptions.getScreeningAnswerOptions()));
	}

	@Nonnull
	protected Optional<ScreeningSessionScreening> findCurrentScreeningSessionScreeningByScreeningSessionId(@Nullable UUID screeningSessionId) {
		if (screeningSessionId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM v_screening_session_screening
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
				FROM screening_question sq, v_screening_session_screening sss 
				WHERE sss.screening_session_screening_id=?
				AND sss.screening_version_id=sq.screening_version_id
				ORDER BY sq.display_order
				""", ScreeningQuestion.class, screeningSessionScreeningId);

		List<ScreeningAnswerOption> screeningAnswerOptions = getDatabase().queryForList("""
				SELECT sao.*
				FROM screening_answer_option sao, screening_question sq, v_screening_session_screening sss
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
	protected List<ScreeningSessionAnsweredScreeningQuestion> findScreeningSessionAnsweredScreeningQuestionsByScreeningSessionScreeningId(@Nullable UUID screeningSessionScreeningId) {
		if (screeningSessionScreeningId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("""
				SELECT ssasq.*
				FROM v_screening_session_answered_screening_question ssasq, screening_question sq
				WHERE ssasq.screening_session_screening_id=?
				AND ssasq.screening_question_id=sq.screening_question_id
				ORDER BY sq.display_order
				""", ScreeningSessionAnsweredScreeningQuestion.class, screeningSessionScreeningId);
	}

	@Nonnull
	protected List<ScreeningAnswer> findScreeningAnswersAcrossAllQuestionsByScreeningSessionScreeningId(@Nullable UUID screeningSessionScreeningId) {
		if (screeningSessionScreeningId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("""
				SELECT sa.*
				FROM v_screening_session_answered_screening_question ssasq, screening_question sq, screening_answer_option sao, v_screening_answer sa
				WHERE ssasq.screening_session_screening_id=?
				AND ssasq.screening_question_id=sq.screening_question_id
				AND sq.screening_question_id=sao.screening_question_id
				AND sa.screening_answer_option_id=sao.screening_answer_option_id
				AND sa.screening_session_answered_screening_question_id=ssasq.screening_session_answered_screening_question_id
				ORDER BY sa.created, sa.screening_answer_id
				""", ScreeningAnswer.class, screeningSessionScreeningId);
	}

	@Nonnull
	public List<ScreeningAnswer> findScreeningAnswersByScreeningQuestionContextId(@Nullable ScreeningQuestionContextId screeningQuestionContextId) {
		if (screeningQuestionContextId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("""
				SELECT sa.*
				FROM v_screening_session_answered_screening_question ssasq, screening_question sq, screening_answer_option sao, v_screening_answer sa
				WHERE ssasq.screening_session_screening_id=?
				AND ssasq.screening_question_id=?
				AND ssasq.screening_question_id=sq.screening_question_id
				AND sq.screening_question_id=sao.screening_question_id
				AND sa.screening_answer_option_id=sao.screening_answer_option_id
				AND sa.screening_session_answered_screening_question_id=ssasq.screening_session_answered_screening_question_id
				ORDER BY sa.created, sa.screening_answer_id
				""", ScreeningAnswer.class, screeningQuestionContextId.getScreeningSessionScreeningId(), screeningQuestionContextId.getScreeningQuestionId());
	}

	@Nonnull
	protected List<ScreeningSessionScreening> findCurrentScreeningSessionScreeningsByScreeningSessionId(@Nullable UUID screeningSessionId) {
		if (screeningSessionId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("""
				SELECT * 
				FROM v_screening_session_screening
				WHERE screening_session_id=?
				ORDER BY screening_order
				""", ScreeningSessionScreening.class, screeningSessionId);
	}

	@Nonnull
	public List<UUID> createScreeningAnswers(@Nullable CreateScreeningAnswersRequest request) {
		requireNonNull(request);

		ScreeningQuestionContextId screeningQuestionContextId = request.getScreeningQuestionContextId();
		List<CreateAnswerRequest> answers = request.getAnswers() == null ? List.of() : request.getAnswers().stream()
				.filter(answer -> answer != null)
				.collect(Collectors.toList());
		UUID createdByAccountId = request.getCreatedByAccountId();
		UUID screeningSessionScreeningId = null;
		UUID screeningQuestionId = null;
		ScreeningSessionScreening screeningSessionScreening = null;
		ScreeningQuestion screeningQuestion;
		List<ScreeningAnswerOption> screeningAnswerOptions = new ArrayList<>();
		Account createdByAccount = null;
		boolean force = request.getForce() == null ? false : request.getForce();
		ValidationException validationException = new ValidationException();

		if (screeningQuestionContextId == null) {
			validationException.add(new FieldError("screeningQuestionContextId", getStrings().get("Screening question context ID is required.")));
		} else {
			screeningSessionScreeningId = screeningQuestionContextId.getScreeningSessionScreeningId();
			screeningSessionScreening = findScreeningSessionScreeningById(screeningSessionScreeningId).orElse(null);

			if (screeningSessionScreening == null)
				validationException.add(new FieldError("screeningQuestionContextId", getStrings().get("Screening question context ID specifies an invalid screening session screening ID.")));

			screeningQuestionId = screeningQuestionContextId.getScreeningQuestionId();

			if (screeningQuestionId == null)
				validationException.add(new FieldError("screeningQuestionContextId", getStrings().get("Screening question context ID specifies an invalid screening question ID.")));
		}

		if (screeningQuestionId != null && screeningSessionScreening != null) {
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
								} else if (screeningQuestion.getScreeningAnswerFormatId() != ScreeningAnswerFormatId.FREEFORM_TEXT) {
									// Handle "supplement" (like a radio button with an "Other" option where people can type whatever they like)
									if (screeningAnswerOption.getFreeformSupplement()) {
										if (text == null)
											validationException.add(new FieldError("text", getStrings().get("Please specify a value for '{{screeningAnswerOption}}'.", new HashMap<String, Object>() {{
												put("screeningAnswerOption", screeningAnswerOption.getAnswerOptionText());
											}})));
									} else {
										// If we're not freeform text and this is NOT a supplement, clear out the text so we don't get odd
										// data in the DB in the event of a FE bug
										answer.setText(null);
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

		ScreeningSession screeningSession = findScreeningSessionById(screeningSessionScreening.getScreeningSessionId()).get();
		ScreeningVersion screeningVersion = findScreeningVersionById(screeningSessionScreening.getScreeningVersionId()).get();
		ScreeningFlowVersion screeningFlowVersion = findScreeningFlowVersionById(screeningSession.getScreeningFlowVersionId()).get();

		// See if this question was already answered for this session...
		ScreeningSessionAnsweredScreeningQuestion screeningSessionAnsweredScreeningQuestion = getDatabase().queryForObject("""
				SELECT *
				FROM v_screening_session_answered_screening_question
				WHERE screening_session_screening_id=?
				AND screening_question_id=?
				""", ScreeningSessionAnsweredScreeningQuestion.class, screeningSessionScreeningId, screeningQuestionId).orElse(null);

		if (screeningSessionAnsweredScreeningQuestion != null) {
			// The question was already answered.
			// Mark all downstream answered questions, answers, and screenings in this session as invalid.
			getLogger().info("Screening session screening ID {} ({}) screening question ID {} was previously answered and is being answered again.",
					screeningSessionScreeningId, screeningVersion.getScreeningTypeId().name(), screeningQuestionId);

			// Mark downstream answered questions as invalid
			long downstreamQuestionsInvalidatedCount = getDatabase().execute("""
					UPDATE screening_session_answered_screening_question AS ssasq 
					SET valid=FALSE
					FROM screening_session_screening sss
					WHERE sss.screening_session_id=?
					AND ssasq.valid=TRUE
					AND sss.screening_session_screening_id=ssasq.screening_session_screening_id
					AND ssasq.created >= ?""", screeningSession.getScreeningSessionId(), screeningSessionAnsweredScreeningQuestion.getCreated());

			// Mark downstream answers as invalid
			getDatabase().execute("""
					UPDATE screening_answer AS sa
					SET valid=FALSE
					FROM screening_session_answered_screening_question ssasq, screening_session_screening sss
					WHERE sa.screening_session_answered_screening_question_id=ssasq.screening_session_answered_screening_question_id
					AND sa.valid=TRUE
					AND ssasq.screening_session_screening_id=sss.screening_session_screening_id
					AND sss.screening_session_id=?
					AND sa.created >= ?""", screeningSession.getScreeningSessionId(), screeningSessionAnsweredScreeningQuestion.getCreated());

			// Mark downstream screenings as invalid
			long downstreamScreeningSessionScreeningsInvalidatedCount = getDatabase().execute("""
					UPDATE screening_session_screening 
					SET valid=FALSE
					WHERE screening_session_id=?
					AND valid=TRUE
					AND screening_order > ?""", screeningSession.getScreeningSessionId(), screeningSessionScreening.getScreeningOrder());

			getLogger().info("Marked {} downstream answered question[s] as invalid and {} downstream screening session screening[s] as invalid.",
					downstreamQuestionsInvalidatedCount, downstreamScreeningSessionScreeningsInvalidatedCount);
		}

		UUID screeningSessionAnsweredScreeningQuestionId = UUID.randomUUID();

		getDatabase().execute("""
						INSERT INTO screening_session_answered_screening_question(screening_session_answered_screening_question_id, 
						screening_session_screening_id, screening_question_id, created)
						VALUES(?,?,?,?)
				""", screeningSessionAnsweredScreeningQuestionId, screeningSessionScreeningId, screeningQuestionId, Instant.now());

		List<UUID> screeningAnswerIds = new ArrayList<>(answers.size());

		// TODO: we can do a batch insert for slightly better performance here
		for (CreateAnswerRequest answer : answers) {
			UUID screeningAnswerId = UUID.randomUUID();
			screeningAnswerIds.add(screeningAnswerId);

			getDatabase().execute("""
					INSERT INTO
					screening_answer(screening_answer_id, screening_answer_option_id, screening_session_answered_screening_question_id, created_by_account_id, text, created)
					VALUES(?,?,?,?,?,?)
					""", screeningAnswerId, answer.getScreeningAnswerOptionId(), screeningSessionAnsweredScreeningQuestionId, createdByAccountId, answer.getText(), Instant.now());
		}

		// Score the individual screening by calling its scoring function
		List<ScreeningQuestionWithAnswerOptions> screeningQuestionsWithAnswerOptions =
				findScreeningQuestionsWithAnswerOptionsByScreeningSessionScreeningId(screeningSessionScreeningId);
		List<ScreeningAnswer> screeningAnswers = findScreeningAnswersAcrossAllQuestionsByScreeningSessionScreeningId(screeningSessionScreeningId);

		ScreeningScoringFunctionOutput screeningScoringFunctionOutput = executeScreeningScoringFunction(
				screeningVersion.getScoringFunction(), screeningQuestionsWithAnswerOptions, screeningAnswers);

		getLogger().info("Screening session screening ID {} ({}) was scored {} with completed flag={}.", screeningSessionScreeningId,
				screeningVersion.getScreeningTypeId().name(), screeningScoringFunctionOutput.getScore(), screeningScoringFunctionOutput.getCompleted());

		// Based on screening scoring function output, set score/completed flags
		getDatabase().execute("""
						UPDATE screening_session_screening
						SET completed=?, score=CAST(? AS JSONB)
						WHERE screening_session_screening_id=?
						""", screeningScoringFunctionOutput.getCompleted(),
				screeningScoringFunctionOutput.getScore().toJsonRepresentation(),
				screeningSessionScreening.getScreeningSessionScreeningId());

		OrchestrationFunctionOutput orchestrationFunctionOutput = executeScreeningFlowOrchestrationFunction(screeningFlowVersion.getOrchestrationFunction(), screeningSession.getScreeningSessionId(), createdByAccount.getInstitutionId()).get();

		if (orchestrationFunctionOutput.getNextScreeningId() != null) {
			Integer nextScreeningOrder = getDatabase().queryForObject("""
					SELECT MAX (screening_order) + 1
					FROM screening_session_screening
					WHERE screening_session_id =?
					""", Integer.class, screeningSession.getScreeningSessionId()).get();

			Screening nextScreening = findScreeningById(orchestrationFunctionOutput.getNextScreeningId()).get();
			ScreeningVersion nextScreeningVersion = findScreeningVersionById(nextScreening.getActiveScreeningVersionId()).get();

			getLogger().info("Orchestration function for screening session screening ID {} ({}) indicates that we should transition to screening ID {} ({}).", screeningSessionScreeningId,
					screeningVersion.getScreeningTypeId().name(), nextScreening.getScreeningId(), nextScreeningVersion.getScreeningTypeId().name());

			getDatabase().execute("""
					INSERT INTO screening_session_screening(screening_session_id, screening_version_id, screening_order)
					VALUES( ?,?,?)
					""", screeningSession.getScreeningSessionId(), nextScreeningVersion.getScreeningVersionId(), nextScreeningOrder);
		}

		// If orchestration logic says we are in crisis, trigger crisis flow
		if (orchestrationFunctionOutput.getCrisisIndicated()) {
			if (screeningSession.getCrisisIndicated()) {
				getLogger().info("Orchestration function for screening session screening ID {} ({}) indicated crisis. " +
						"This session was already marked as having a crisis indicated, so no action needed.", screeningSessionScreeningId, screeningVersion.getScreeningTypeId().name());
			} else {
				getLogger().info("Orchestration function for screening session screening ID {} ({}) indicated crisis.  Creating crisis interaction instance...",
						screeningSessionScreeningId, screeningVersion.getScreeningTypeId().name());

				getDatabase().execute("UPDATE screening_session SET crisis_indicated=TRUE, crisis_indicated_at=NOW() WHERE screening_session_id=?", screeningSession.getScreeningSessionId());

				getInteractionService().createCrisisInteraction(screeningSession.getScreeningSessionId());
			}
		}

		// TODO: should we handle un-marking as crisis (? - need futher clinical input)

		if (orchestrationFunctionOutput.getCompleted()) {
			getLogger().info("Orchestration function for screening session screening ID {} ({}) indicated that screening session ID {} is now complete.", screeningSessionScreeningId,
					screeningVersion.getScreeningTypeId().name(), screeningSession.getScreeningSessionId());

			// Special case: if we have a pre-completion screening confirmation prompt for this flow AND request did not indicate that we
			// should "force" the answer, throw a special exception that provides the confirmation prompt to display to the user
			// (if user accepts, request should specify to "force" the answer to indicate confirmation and skip this check).
			if (screeningFlowVersion.getPreCompletionScreeningConfirmationPromptId() != null && !force) {
				ValidationException screeningConfirmationPromptValidationException = new ValidationException(getStrings().get("Please confirm your answers before submitting."));

				// Frontend should look for this special metadata field "screeningConfirmationPrompt" and present the user with it
				ScreeningConfirmationPrompt screeningConfirmationPrompt = findScreeningConfirmationPromptById(screeningFlowVersion.getPreCompletionScreeningConfirmationPromptId()).get();
				screeningConfirmationPromptValidationException.setMetadata(Map.of("screeningConfirmationPrompt", getScreeningConfirmationPromptApiResponseFactory().create(screeningConfirmationPrompt)));

				throw screeningConfirmationPromptValidationException;
			}

			getDatabase().execute("UPDATE screening_session SET completed=TRUE, completed_at=NOW() WHERE screening_session_id=?", screeningSession.getScreeningSessionId());

			// Execute results function and store off results
			ResultsFunctionOutput resultsFunctionOutput = executeScreeningFlowResultsFunction(screeningFlowVersion.getResultsFunction(),
					screeningSession.getScreeningSessionId(), createdByAccount.getInstitutionId()).get();

			getDatabase().execute("DELETE FROM screening_session_support_role_recommendation WHERE screening_session_id=?",
					screeningSession.getScreeningSessionId());

			for (SupportRoleRecommendation supportRoleRecommendation : resultsFunctionOutput.getSupportRoleRecommendations())
				getDatabase().execute("INSERT INTO screening_session_support_role_recommendation(screening_session_id, support_role_id, weight) " +
						"VALUES (?,?,?)", screeningSession.getScreeningSessionId(), supportRoleRecommendation.getSupportRoleId(), supportRoleRecommendation.getWeight());
		}

		return screeningAnswerIds;
	}

	@Nonnull
	public Optional<ScreeningSessionDestination> determineDestinationForScreeningSessionId(@Nullable UUID screeningSessionId) {
		if (screeningSessionId == null)
			return Optional.empty();

		ScreeningSession screeningSession = findScreeningSessionById(screeningSessionId).orElse(null);

		if (screeningSession == null || !screeningSession.getCompleted())
			return Optional.empty();

		ScreeningFlowVersion screeningFlowVersion = findScreeningFlowVersionById(screeningSession.getScreeningFlowVersionId()).get();
		Account targetAccount = getAccountService().findAccountById(screeningSession.getTargetAccountId()).get();

		DestinationFunctionOutput destinationFunctionOutput = executeScreeningFlowDestinationFunction(screeningFlowVersion.getDestinationFunction(),
				screeningSessionId, targetAccount.getInstitutionId()).get();

		if (destinationFunctionOutput.getScreeningSessionDestinationId() == null)
			return Optional.empty();

		return Optional.of(new ScreeningSessionDestination(
				destinationFunctionOutput.getScreeningSessionDestinationId(), destinationFunctionOutput.getContext()));
	}

	@Nonnull
	protected ScreeningScoringFunctionOutput executeScreeningScoringFunction(@Nonnull String screeningScoringFunctionJavascript,
																																					 @Nonnull List<ScreeningQuestionWithAnswerOptions> screeningQuestionsWithAnswerOptions,
																																					 @Nonnull List<ScreeningAnswer> screeningAnswers) {
		requireNonNull(screeningScoringFunctionJavascript);
		requireNonNull(screeningQuestionsWithAnswerOptions);
		requireNonNull(screeningAnswers);

		ScreeningScoringFunctionOutput screeningScoringFunctionOutput;

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
			screeningScoringFunctionOutput = getJavascriptExecutor().execute(screeningScoringFunctionJavascript, context, ScreeningScoringFunctionOutput.class);
		} catch (JavascriptExecutionException e) {
			throw new RuntimeException(e);
		}

		if (screeningScoringFunctionOutput.getCompleted() == null)
			throw new IllegalStateException("Screening scoring function must provide a 'completed' value in output");

		if (screeningScoringFunctionOutput.getScore() == null)
			throw new IllegalStateException("Screening scoring function must provide a 'score' value in output");

		return screeningScoringFunctionOutput;
	}


	@Nonnull
	protected Optional<OrchestrationFunctionOutput> executeScreeningFlowOrchestrationFunction(@Nonnull String screeningFlowOrchestrationFunctionJavascript,
																																														@Nullable UUID screeningSessionId,
																																														@Nullable InstitutionId institutionId) {
		requireNonNull(screeningFlowOrchestrationFunctionJavascript);

		if (screeningSessionId == null || institutionId == null)
			return Optional.empty();

		OrchestrationFunctionOutput orchestrationFunctionOutput = executeScreeningFlowFunction(screeningFlowOrchestrationFunctionJavascript,
				OrchestrationFunctionOutput.class, screeningSessionId, institutionId).orElse(null);

		if (orchestrationFunctionOutput == null)
			return Optional.empty();

		if (orchestrationFunctionOutput.getCompleted() == null)
			throw new IllegalStateException("Screening flow orchestration function must provide a 'completed' value in output");

		if (orchestrationFunctionOutput.getCrisisIndicated() == null)
			throw new IllegalStateException("Screening flow orchestration function must provide a 'crisisIndicated' value in output");

		if (orchestrationFunctionOutput.getCompleted() && orchestrationFunctionOutput.getNextScreeningId() != null)
			throw new IllegalStateException(format("Screening flow orchestration function output says this screening session is completed, but also provides a nonnull 'nextScreeningId' value"));

		return Optional.of(orchestrationFunctionOutput);
	}

	@Nonnull
	protected Optional<ResultsFunctionOutput> executeScreeningFlowResultsFunction(@Nonnull String screeningFlowResultsFunctionJavascript,
																																								@Nullable UUID screeningSessionId,
																																								@Nullable InstitutionId institutionId) {
		requireNonNull(screeningFlowResultsFunctionJavascript);

		if (screeningSessionId == null || institutionId == null)
			return Optional.empty();

		ResultsFunctionOutput resultsFunctionOutput = executeScreeningFlowFunction(screeningFlowResultsFunctionJavascript,
				ResultsFunctionOutput.class, screeningSessionId, institutionId).orElse(null);

		if (resultsFunctionOutput == null)
			return Optional.empty();

		if (resultsFunctionOutput.getSupportRoleRecommendations() == null)
			throw new IllegalStateException("Screening flow results function must provide a 'supportRoleRecommendations' value in output");

		return Optional.of(resultsFunctionOutput);
	}

	@Nonnull
	protected Optional<DestinationFunctionOutput> executeScreeningFlowDestinationFunction(@Nonnull String screeningFlowDestinationFunctionJavascript,
																																												@Nullable UUID screeningSessionId,
																																												@Nullable InstitutionId institutionId) {
		requireNonNull(screeningFlowDestinationFunctionJavascript);

		if (screeningSessionId == null || institutionId == null)
			return Optional.empty();

		DestinationFunctionOutput destinationFunctionOutput = executeScreeningFlowFunction(screeningFlowDestinationFunctionJavascript,
				DestinationFunctionOutput.class, screeningSessionId, institutionId).orElse(null);

		if (destinationFunctionOutput == null)
			return Optional.empty();

		return Optional.of(destinationFunctionOutput);
	}

	@Nonnull
	protected <T> Optional<T> executeScreeningFlowFunction(@Nonnull String screeningFlowFunctionJavascript,
																												 @Nonnull Class<T> screeningFlowFunctionResultType,
																												 @Nullable UUID screeningSessionId,
																												 @Nullable InstitutionId institutionId) {
		requireNonNull(screeningFlowFunctionJavascript);

		if (screeningSessionId == null || institutionId == null)
			return Optional.empty();

		T screeningFlowFunctionResult;

		ScreeningSession screeningSession = findScreeningSessionById(screeningSessionId).orElse(null);

		if (screeningSession == null)
			return Optional.empty();

		List<Screening> screenings = findScreeningsByInstitutionId(institutionId);

		if (screenings.size() == 0)
			return Optional.empty();

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
		List<ScreeningSessionScreening> screeningSessionScreenings = findCurrentScreeningSessionScreeningsByScreeningSessionId(screeningSessionId);
		Map<UUID, Object> screeningResultsByScreeningSessionScreeningId = new HashMap<>();

		// We could do this as a single query, but the dataset is small, and this is a little clearer and fast enough
		for (ScreeningSessionScreening screeningSessionScreening : screeningSessionScreenings) {
			List<ScreeningQuestionWithAnswerOptions> screeningQuestionsWithAnswerOptions = findScreeningQuestionsWithAnswerOptionsByScreeningSessionScreeningId(screeningSessionScreening.getScreeningSessionScreeningId());
			List<ScreeningAnswer> screeningAnswers = findScreeningAnswersAcrossAllQuestionsByScreeningSessionScreeningId(screeningSessionScreening.getScreeningSessionScreeningId());
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
		context.put("screeningSession", screeningSession);
		context.put("screeningSessionScreenings", screeningSessionScreenings);
		context.put("screeningResultsByScreeningSessionScreeningId", screeningResultsByScreeningSessionScreeningId);

		try {
			screeningFlowFunctionResult = getJavascriptExecutor().execute(screeningFlowFunctionJavascript, context, screeningFlowFunctionResultType);
		} catch (JavascriptExecutionException e) {
			throw new RuntimeException(e);
		}

		return Optional.ofNullable(screeningFlowFunctionResult);
	}

	protected void debugScreeningSession(@Nonnull UUID screeningSessionId) {
		requireNonNull(screeningSessionId);

		if (!getLogger().isDebugEnabled())
			return;

		ScreeningSession screeningSession = findScreeningSessionById(screeningSessionId).get();
		ScreeningFlowVersion screeningFlowVersion = findScreeningFlowVersionById(screeningSession.getScreeningFlowVersionId()).get();
		ScreeningFlow screeningFlow = findScreeningFlowById(screeningFlowVersion.getScreeningFlowId()).get();
		List<ScreeningSessionScreening> screeningSessionScreenings = findCurrentScreeningSessionScreeningsByScreeningSessionId(screeningSessionId);

		List<String> logLines = new ArrayList<>();

		logLines.add("*** SCREENING SESSION DEBUG ***");
		logLines.add(format("Screening Flow '%s', version %d", screeningFlow.getName(), screeningFlowVersion.getVersionNumber()));

		for (ScreeningSessionScreening screeningSessionScreening : screeningSessionScreenings) {
			ScreeningVersion screeningVersion = findScreeningVersionById(screeningSessionScreening.getScreeningVersionId()).get();
			Screening screening = findScreeningById(screeningVersion.getScreeningId()).get();

			logLines.add(format("\tScreening '%s', version %d", screening.getName(), screeningVersion.getVersionNumber()));

			List<ScreeningQuestionWithAnswerOptions> screeningQuestionsWithAnswerOptions = findScreeningQuestionsWithAnswerOptionsByScreeningSessionScreeningId(screeningSessionScreening.getScreeningSessionScreeningId());
			List<ScreeningSessionAnsweredScreeningQuestion> screeningSessionAnsweredScreeningQuestions = findScreeningSessionAnsweredScreeningQuestionsByScreeningSessionScreeningId(screeningSessionScreening.getScreeningSessionScreeningId());

			for (ScreeningSessionAnsweredScreeningQuestion screeningSessionAnsweredScreeningQuestion : screeningSessionAnsweredScreeningQuestions) {
				for (ScreeningQuestionWithAnswerOptions screeningQuestionWithAnswerOptions : screeningQuestionsWithAnswerOptions) {
					if (screeningQuestionWithAnswerOptions.getScreeningQuestion().getScreeningQuestionId().equals(screeningSessionAnsweredScreeningQuestion.getScreeningQuestionId())) {
						logLines.add(format("\t\tQuestion: %s", screeningQuestionWithAnswerOptions.getScreeningQuestion().getQuestionText()));

						List<ScreeningAnswer> screeningAnswers = findScreeningAnswersByScreeningQuestionContextId(
								new ScreeningQuestionContextId(screeningSessionScreening.getScreeningSessionScreeningId(), screeningQuestionWithAnswerOptions.getScreeningQuestion().getScreeningQuestionId()));

						logLines.add(format("\t\tAnswer[s]: %s", screeningAnswers.stream().map(screeningAnswer -> {
							ScreeningAnswerOption screeningAnswerOption = null;

							for (ScreeningAnswerOption potentialScreeningAnswerOption : screeningQuestionWithAnswerOptions.getScreeningAnswerOptions()) {
								if (screeningAnswer.getScreeningAnswerOptionId().equals(potentialScreeningAnswerOption.getScreeningAnswerOptionId())) {
									return potentialScreeningAnswerOption.getAnswerOptionText();
								}
							}

							throw new IllegalStateException("Answer w/o matching option, should never happen");
						}).collect(Collectors.joining(", "))));
					}
				}
			}
		}

		getLogger().debug(logLines.stream().collect(Collectors.joining("\n")));
	}

	@NotThreadSafe
	protected static class ScreeningScoringFunctionOutput {
		@Nullable
		private Boolean completed;
		@Nullable
		private ScreeningScore score;

		@Nullable
		public Boolean getCompleted() {
			return this.completed;
		}

		public void setCompleted(@Nullable Boolean completed) {
			this.completed = completed;
		}

		@Nullable
		public ScreeningScore getScore() {
			return this.score;
		}

		public void setScore(@Nullable ScreeningScore score) {
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

	@NotThreadSafe
	protected static class ResultsFunctionOutput {
		@Nullable
		private Set<SupportRoleRecommendation> supportRoleRecommendations;

		// TODO: recommended Content tags


		@Nullable
		public Set<SupportRoleRecommendation> getSupportRoleRecommendations() {
			return this.supportRoleRecommendations;
		}

		public void setSupportRoleRecommendations(@Nullable Set<SupportRoleRecommendation> supportRoleRecommendations) {
			this.supportRoleRecommendations = supportRoleRecommendations;
		}

		@NotThreadSafe
		public static class SupportRoleRecommendation {
			@Nullable
			private SupportRoleId supportRoleId;
			@Nullable
			private Double weight;

			@Nullable
			public SupportRoleId getSupportRoleId() {
				return this.supportRoleId;
			}

			public void setSupportRoleId(@Nullable SupportRoleId supportRoleId) {
				this.supportRoleId = supportRoleId;
			}

			@Nullable
			public Double getWeight() {
				return this.weight;
			}

			public void setWeight(@Nullable Double weight) {
				this.weight = weight;
			}
		}
	}

	@NotThreadSafe
	protected static class DestinationFunctionOutput {
		@Nullable
		private ScreeningSessionDestinationId screeningSessionDestinationId;
		@Nullable
		private Map<String, Object> context;

		@Nullable
		public ScreeningSessionDestinationId getScreeningSessionDestinationId() {
			return this.screeningSessionDestinationId;
		}

		public void setScreeningSessionDestinationId(@Nullable ScreeningSessionDestinationId screeningSessionDestinationId) {
			this.screeningSessionDestinationId = screeningSessionDestinationId;
		}

		@Nullable
		public Map<String, Object> getContext() {
			return this.context;
		}

		public void setContext(@Nullable Map<String, Object> context) {
			this.context = context;
		}
	}

	@Nonnull
	protected InstitutionService getInstitutionService() {
		return this.institutionServiceProvider.get();
	}

	@Nonnull
	protected InteractionService getInteractionService() {
		return this.interactionServiceProvider.get();
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
	protected ScreeningConfirmationPromptApiResponseFactory getScreeningConfirmationPromptApiResponseFactory() {
		return this.screeningConfirmationPromptApiResponseFactory;
	}

	@Nonnull
	protected JavascriptExecutor getJavascriptExecutor() {
		return this.javascriptExecutor;
	}

	@Nonnull
	protected ErrorReporter getErrorReporter() {
		return this.errorReporter;
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
