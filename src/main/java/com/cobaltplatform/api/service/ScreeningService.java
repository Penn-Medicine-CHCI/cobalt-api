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

import com.cobaltplatform.api.model.api.request.CreateScreeningAnswerRequest;
import com.cobaltplatform.api.model.api.request.CreateScreeningSessionRequest;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.Screening;
import com.cobaltplatform.api.model.db.ScreeningAnswer;
import com.cobaltplatform.api.model.db.ScreeningAnswerOption;
import com.cobaltplatform.api.model.db.ScreeningFlow;
import com.cobaltplatform.api.model.db.ScreeningFlowVersion;
import com.cobaltplatform.api.model.db.ScreeningQuestion;
import com.cobaltplatform.api.model.db.ScreeningSession;
import com.cobaltplatform.api.model.db.ScreeningSessionScreening;
import com.cobaltplatform.api.model.db.ScreeningVersion;
import com.cobaltplatform.api.model.service.ScreeningQuestionWithAnswerOptions;
import com.cobaltplatform.api.model.service.ScreeningSessionQuestion;
import com.cobaltplatform.api.util.JavascriptExecutor;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.ValidationException.FieldError;
import com.lokalized.Strings;
import com.pyranid.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
	private final Database database;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;

	@Inject
	public ScreeningService(@Nonnull Provider<AccountService> accountServiceProvider,
													@Nonnull Provider<AuthorizationService> authorizationServiceProvider,
													@Nonnull JavascriptExecutor javascriptExecutor,
													@Nonnull Database database,
													@Nonnull Strings strings) {
		requireNonNull(accountServiceProvider);
		requireNonNull(authorizationServiceProvider);
		requireNonNull(javascriptExecutor);
		requireNonNull(database);
		requireNonNull(strings);

		this.accountServiceProvider = accountServiceProvider;
		this.authorizationServiceProvider = authorizationServiceProvider;
		this.javascriptExecutor = javascriptExecutor;
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

		return getDatabase().queryForObject("SELECT * FROM screening_session WHERE screening_session_screening_id=?",
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
	public List<ScreeningFlow> findScreeningFlowsByInstitutionId(@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("SELECT * FROM screening_flow WHERE institution_id=? ORDER BY name",
				ScreeningFlow.class, institutionId);
	}

	@Nonnull
	public List<ScreeningSession> findScreeningSessionsByScreeningFlowId(@Nullable UUID screeningFlowId,
																																			 @Nullable UUID participantAccountId) {
		if (screeningFlowId == null || participantAccountId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("""
						SELECT ss.* FROM screening_session ss, screening_flow_version sfv 
						WHERE sfv.screening_flow_id=? AND ss.screening_flow_version_id=sfv.screening_flow_version_id 
						AND (ss.target_account_id=? OR ss.created_by_account_id=?) ORDER BY ss.created DESC
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

		if (createdByAccount != null && targetAccount != null) {
			if (!getAuthorizationService().canPerformScreening(createdByAccount, targetAccount))
				validationException.add(getStrings().get("You are not authorized to create this screening session."));
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
	public Optional<ScreeningSessionQuestion> findNextScreeningSessionQuestionByScreeningSessionId(@Nullable UUID screeningSessionId) {
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

		// Indicates programmer error
		if (screeningSessionScreening.getCompleted())
			throw new IllegalStateException(format("Screening session ID %s is completed, but screening session screening ID %s is not.",
					screeningSessionId, screeningSessionScreening.getScreeningSessionScreeningId()));

		List<ScreeningQuestionWithAnswerOptions> screeningQuestionsWithAnswerOptions = findScreeningQuestionsWithAnswerOptionsByScreeningSessionScreeningId(screeningSessionScreening.getScreeningSessionScreeningId());
		List<ScreeningAnswer> screeningAnswers = findScreeningAnswersByScreeningSessionScreeningId(screeningSessionScreening.getScreeningSessionId());
		Set<UUID> answeredScreeningAnswerOptionIds = screeningAnswers.stream()
				.map(screeningAnswer -> screeningAnswer.getScreeningAnswerOptionId())
				.collect(Collectors.toSet());

		ScreeningQuestionWithAnswerOptions nextScreeningQuestionWithAnswerOptions = null;

		for (ScreeningQuestionWithAnswerOptions screeningQuestionWithAnswerOptions : screeningQuestionsWithAnswerOptions) {
			for (ScreeningAnswerOption screeningAnswerOption : screeningQuestionWithAnswerOptions.getScreeningAnswerOptions()) {
				if (!answeredScreeningAnswerOptionIds.contains(screeningAnswerOption.getScreeningAnswerOptionId())) {
					nextScreeningQuestionWithAnswerOptions = screeningQuestionWithAnswerOptions;
					break;
				}
			}

			if (nextScreeningQuestionWithAnswerOptions != null)
				break;
		}

		// Normally won't be in this situation - could occur if there is a race with the same screening being worked on concurrently
		if (nextScreeningQuestionWithAnswerOptions == null)
			throw new IllegalStateException(format("Screening session ID %s is incomplete, but does not have any unanswered questions.",
					screeningSessionScreening.getScreeningSessionScreeningId()));

		ScreeningSessionQuestion screeningSessionQuestion = new ScreeningSessionQuestion();

		// TODO: fill in

		return Optional.of(screeningSessionQuestion);
	}

	@Nonnull
	protected Optional<ScreeningSessionScreening> findCurrentScreeningSessionScreeningByScreeningSessionId(@Nullable UUID screeningSessionId) {
		if (screeningSessionId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT * FROM screening_session_screening WHERE screening_session_id=? 
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
	protected List<ScreeningAnswer> findScreeningAnswersByScreeningSessionScreeningId(@Nullable UUID screeningSessionScreeningId) {
		if (screeningSessionScreeningId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("SELECT * FROM screening_answer WHERE screening_session_screening_id=? ORDER BY created",
				ScreeningAnswer.class, screeningSessionScreeningId);
	}

	@Nonnull
	public UUID createScreeningAnswer(@Nullable CreateScreeningAnswerRequest request) {
		requireNonNull(request);

		UUID screeningSessionScreeningId = request.getScreeningSessionScreeningId();
		UUID screeningAnswerOptionId = request.getScreeningAnswerOptionId();
		String text = trimToNull(request.getText());
		UUID screeningAnswerId = UUID.randomUUID();
		ValidationException validationException = new ValidationException();

		if (validationException.hasErrors())
			throw validationException;

		throw new UnsupportedOperationException();

//		getDatabase().execute("""
//				INSERT INTO screening_session(screening_session_id, screening_flow_version_id, target_account_id, created_by_account_id)
//				VALUES (?,?,?,?)
//				""", screeningSessionId, screeningFlowVersion.getScreeningFlowVersionId(), targetAccountId, createdByAccountId);

		// TODO: execute javascript
		// TODO: insert into next context, if applicable

		// return screeningAnswerId;
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
