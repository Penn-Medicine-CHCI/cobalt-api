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

package com.cobaltplatform.api.web.resource;

import com.lokalized.Strings;
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AccountSession;
import com.cobaltplatform.api.model.db.AccountSessionAnswer;
import com.cobaltplatform.api.model.db.Answer;
import com.cobaltplatform.api.model.db.Assessment;
import com.cobaltplatform.api.model.db.Question;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.service.AccountService;
import com.cobaltplatform.api.service.AssessmentService;
import com.cobaltplatform.api.service.SessionService;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.web.response.ResponseGenerator;
import com.soklet.web.annotation.GET;
import com.soklet.web.annotation.PathParameter;
import com.soklet.web.annotation.Resource;
import com.soklet.web.exception.AuthorizationException;
import com.soklet.web.exception.NotFoundException;
import com.soklet.web.response.BinaryResponse;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify LLC.
 */
@Singleton
@Resource
@ThreadSafe
public class AccountSessionResource {
	@Nonnull
	private final AccountService accountService;
	@Nonnull
	private final SessionService sessionService;
	@Nonnull
	private final AssessmentService assessmentService;
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Formatter formatter;

	@Inject
	public AccountSessionResource(@Nonnull AccountService accountService,
																@Nonnull SessionService sessionService,
																@Nonnull AssessmentService assessmentService,
																@Nonnull Provider<CurrentContext> currentContextProvider,
																@Nonnull Strings strings,
																@Nonnull Formatter formatter) {
		requireNonNull(accountService);
		requireNonNull(sessionService);
		requireNonNull(assessmentService);
		requireNonNull(currentContextProvider);
		requireNonNull(strings);
		requireNonNull(formatter);

		this.accountService = accountService;
		this.sessionService = sessionService;
		this.assessmentService = assessmentService;
		this.currentContextProvider = currentContextProvider;
		this.strings = strings;
		this.formatter = formatter;
	}

	@GET("/account-sessions/{accountSessionId}/text")
	@AuthenticationRequired
	public BinaryResponse accountSessionCsv(@Nonnull @PathParameter UUID accountSessionId) {
		Account account = getCurrentContext().getAccount().get();

		AccountSession accountSession = getSessionService().findAccountSessionById(accountSessionId).orElse(null);

		if (accountSession == null)
			throw new NotFoundException();

		if (!getSessionService().canAccountIdViewAccountSessionId(account.getAccountId(), accountSessionId))
			throw new AuthorizationException();

		List<String> responseLines = new ArrayList<>();
		Assessment assessment = getAssessmentService().findAssessmentById(accountSession.getAssessmentId()).get();
		List<Question> questions = getAssessmentService().findQuestionsForAssessmentId(assessment.getAssessmentId());
		List<Answer> answers = getSessionService().findAnswersForSession(accountSession);
		List<AccountSessionAnswer> accountSessionAnswers = getSessionService().findAccountSessionAnswersForAccountSessionId(accountSessionId);
		Map<UUID, Question> questionsByQuestionId = new HashMap<>(questions.size());

		for (Question question : questions)
			questionsByQuestionId.put(question.getQuestionId(), question);

		for (int i = 0; i < answers.size(); ++i) {
			Answer answer = answers.get(i);
			AccountSessionAnswer accountSessionAnswer = accountSessionAnswers.get(i);
			Question question = questionsByQuestionId.get(answer.getQuestionId());
			String finalAnswer = question.getQuestionTypeId().isFreeform() ? accountSessionAnswer.getAnswerText() : answer.getAnswerText();

			responseLines.add(format("%s %s", question.getQuestionText(), finalAnswer));
		}

		Account answeringAccount = getAccountService().findAccountById(accountSession.getAccountId()).orElse(null);

		if (answeringAccount != null && answeringAccount.getEmailAddress() != null)
			responseLines.add(format("Email address: %s", answeringAccount.getEmailAddress()));

		return ResponseGenerator.utf8Response(responseLines.stream().collect(Collectors.joining("\n")), "text/plain");
	}

	@Nonnull
	protected AccountService getAccountService() {
		return accountService;
	}

	@Nonnull
	protected SessionService getSessionService() {
		return sessionService;
	}

	@Nonnull
	protected AssessmentService getAssessmentService() {
		return assessmentService;
	}

	@Nonnull
	protected CurrentContext getCurrentContext() {
		return currentContextProvider.get();
	}

	@Nonnull
	protected Strings getStrings() {
		return strings;
	}

	@Nonnull
	protected Formatter getFormatter() {
		return formatter;
	}
}
