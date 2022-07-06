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

import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.api.request.CreateScreeningAnswersRequest;
import com.cobaltplatform.api.model.api.request.CreateScreeningSessionRequest;
import com.cobaltplatform.api.model.api.response.ScreeningAnswerApiResponse.ScreeningAnswerApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ScreeningAnswerOptionApiResponse.ScreeningAnswerOptionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ScreeningQuestionApiResponse.ScreeningQuestionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ScreeningSessionApiResponse.ScreeningSessionApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.ScreeningAnswer;
import com.cobaltplatform.api.model.db.ScreeningAnswerOption;
import com.cobaltplatform.api.model.db.ScreeningQuestion;
import com.cobaltplatform.api.model.db.ScreeningSession;
import com.cobaltplatform.api.model.db.ScreeningSessionScreening;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.model.service.ScreeningQuestionContext;
import com.cobaltplatform.api.model.service.ScreeningQuestionContextId;
import com.cobaltplatform.api.model.service.ScreeningSessionDestination;
import com.cobaltplatform.api.service.AccountService;
import com.cobaltplatform.api.service.AuthorizationService;
import com.cobaltplatform.api.service.ScreeningService;
import com.cobaltplatform.api.web.request.RequestBodyParser;
import com.soklet.web.annotation.GET;
import com.soklet.web.annotation.POST;
import com.soklet.web.annotation.PathParameter;
import com.soklet.web.annotation.QueryParameter;
import com.soklet.web.annotation.RequestBody;
import com.soklet.web.annotation.Resource;
import com.soklet.web.exception.AuthorizationException;
import com.soklet.web.exception.NotFoundException;
import com.soklet.web.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Resource
@Singleton
@ThreadSafe
public class ScreeningResource {
	@Nonnull
	private final ScreeningService screeningService;
	@Nonnull
	private final AccountService accountService;
	@Nonnull
	private final AuthorizationService authorizationService;
	@Nonnull
	private final RequestBodyParser requestBodyParser;
	@Nonnull
	private final ScreeningSessionApiResponseFactory screeningSessionApiResponseFactory;
	@Nonnull
	private final ScreeningQuestionApiResponseFactory screeningQuestionApiResponseFactory;
	@Nonnull
	private final ScreeningAnswerOptionApiResponseFactory screeningAnswerOptionApiResponseFactory;
	@Nonnull
	private final ScreeningAnswerApiResponseFactory screeningAnswerApiResponseFactory;
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final Logger logger;

	@Inject
	public ScreeningResource(@Nonnull ScreeningService screeningService,
													 @Nonnull AccountService accountService,
													 @Nonnull AuthorizationService authorizationService,
													 @Nonnull RequestBodyParser requestBodyParser,
													 @Nonnull ScreeningSessionApiResponseFactory screeningSessionApiResponseFactory,
													 @Nonnull ScreeningQuestionApiResponseFactory screeningQuestionApiResponseFactory,
													 @Nonnull ScreeningAnswerOptionApiResponseFactory screeningAnswerOptionApiResponseFactory,
													 @Nonnull ScreeningAnswerApiResponseFactory screeningAnswerApiResponseFactory,
													 @Nonnull Provider<CurrentContext> currentContextProvider) {
		requireNonNull(screeningService);
		requireNonNull(accountService);
		requireNonNull(authorizationService);
		requireNonNull(requestBodyParser);
		requireNonNull(screeningSessionApiResponseFactory);
		requireNonNull(screeningQuestionApiResponseFactory);
		requireNonNull(screeningAnswerOptionApiResponseFactory);
		requireNonNull(screeningAnswerApiResponseFactory);
		requireNonNull(currentContextProvider);

		this.screeningService = screeningService;
		this.accountService = accountService;
		this.authorizationService = authorizationService;
		this.requestBodyParser = requestBodyParser;
		this.screeningSessionApiResponseFactory = screeningSessionApiResponseFactory;
		this.screeningQuestionApiResponseFactory = screeningQuestionApiResponseFactory;
		this.screeningAnswerOptionApiResponseFactory = screeningAnswerOptionApiResponseFactory;
		this.screeningAnswerApiResponseFactory = screeningAnswerApiResponseFactory;
		this.currentContextProvider = currentContextProvider;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	@POST("/screening-sessions")
	@AuthenticationRequired
	public ApiResponse createScreeningSession(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		Account account = getCurrentContext().getAccount().get();

		CreateScreeningSessionRequest request = getRequestBodyParser().parse(requestBody, CreateScreeningSessionRequest.class);
		request.setCreatedByAccountId(account.getAccountId());

		// If you don't supply a target account, we assume you are starting a session for yourself
		if (request.getTargetAccountId() == null)
			request.setTargetAccountId(account.getAccountId());

		// Ensure you are permitted to start a screening session for the specified account
		if (request.getTargetAccountId() != null) {
			Account targetAccount = getAccountService().findAccountById(request.getTargetAccountId()).orElse(null);

			if (!getAuthorizationService().canPerformScreening(account, targetAccount))
				throw new AuthorizationException();
		}

		UUID screeningSessionId = getScreeningService().createScreeningSession(request);
		ScreeningSession screeningSession = getScreeningService().findScreeningSessionById(screeningSessionId).get();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("screeningSession", getScreeningSessionApiResponseFactory().create(screeningSession));
		}});
	}

	@Nonnull
	@GET("/screening-sessions")
	@AuthenticationRequired
	public ApiResponse screeningSessions(@Nonnull @QueryParameter UUID screeningFlowId,
																			 @Nonnull @QueryParameter Optional<UUID> targetAccountId) {
		requireNonNull(screeningFlowId);
		requireNonNull(targetAccountId);

		Account account = getCurrentContext().getAccount().get();

		List<ScreeningSession> screeningSessions = getScreeningService().findScreeningSessionsByScreeningFlowId(screeningFlowId, account.getAccountId()).stream()
				.filter(screeningSession -> targetAccountId.isEmpty() ? true : screeningSession.getTargetAccountId().equals(targetAccountId.get()))
				.filter(screeningSession -> getAuthorizationService().canViewScreeningSession(screeningSession, account, getAccountService().findAccountById(screeningSession.getTargetAccountId()).get()))
				.collect(Collectors.toList());

		return new ApiResponse(new HashMap<String, Object>() {{
			put("screeningSessions", screeningSessions.stream()
					.map(screeningSession -> getScreeningSessionApiResponseFactory().create(screeningSession))
					.collect(Collectors.toList()));
		}});
	}

	@Nonnull
	@GET("/screening-question-contexts/{screeningQuestionContextId}")
	@AuthenticationRequired
	public ApiResponse screeningQuestionContext(@Nonnull @PathParameter ScreeningQuestionContextId screeningQuestionContextId) {
		requireNonNull(screeningQuestionContextId);

		ScreeningQuestionContext screeningQuestionContext = getScreeningService().findScreeningQuestionContextById(
				screeningQuestionContextId).orElse(null);

		if (screeningQuestionContext == null)
			throw new NotFoundException();

		// Questions, answer options, and any answers already given for this screening session screening
		ScreeningQuestion screeningQuestion = screeningQuestionContext.getScreeningQuestion();
		List<ScreeningAnswerOption> screeningAnswerOptions = screeningQuestionContext.getScreeningAnswerOptions();
		List<ScreeningAnswer> screeningAnswers = getScreeningService().findScreeningAnswersByScreeningQuestionContextId(screeningQuestionContextId);

		// Generate a link back to the previously-answered question in the same screening session.
		// This might be a question in a previous screening session screening.
		// This will not exist at all if we have not answered any questions yet.
		ScreeningQuestionContext previousScreeningQuestionContext =
				getScreeningService().findPreviousScreeningQuestionContextByScreeningQuestionContextId(screeningQuestionContextId).orElse(null);

		ScreeningSessionDestination screeningSessionDestination = getScreeningService().determineDestinationForScreeningSessionId(screeningQuestionContext.getScreeningSessionScreening().getScreeningSessionId()).orElse(null);

		return new ApiResponse(new HashMap<String, Object>() {{
			put("previousScreeningQuestionContextId", previousScreeningQuestionContext == null ? null
					: previousScreeningQuestionContext.getScreeningQuestionContextId());
			put("screeningQuestion", getScreeningQuestionApiResponseFactory().create(screeningQuestion));
			put("screeningAnswerOptions", screeningAnswerOptions.stream()
					.map(screeningAnswerOption -> getScreeningAnswerOptionApiResponseFactory().create(screeningAnswerOption))
					.collect(Collectors.toList()));
			put("screeningAnswers", screeningAnswers.stream()
					.map(screeningAnswer -> getScreeningAnswerApiResponseFactory().create(screeningAnswer))
					.collect(Collectors.toList()));
			put("screeningSessionDestination", screeningSessionDestination);
		}});
	}

	@Nonnull
	@POST("/screening-answers")
	@AuthenticationRequired
	public ApiResponse createScreeningAnswers(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		Account account = getCurrentContext().getAccount().get();

		CreateScreeningAnswersRequest request = getRequestBodyParser().parse(requestBody, CreateScreeningAnswersRequest.class);
		request.setCreatedByAccountId(account.getAccountId());

		ScreeningSessionScreening screeningSessionScreening = request.getScreeningQuestionContextId() == null ? null :
				getScreeningService().findScreeningSessionScreeningById(request.getScreeningQuestionContextId().getScreeningSessionScreeningId()).orElse(null);

		if (screeningSessionScreening == null)
			throw new NotFoundException();

		ScreeningSession screeningSession = getScreeningService().findScreeningSessionById(screeningSessionScreening.getScreeningSessionId()).get();
		Account targetAccount = getAccountService().findAccountById(screeningSession.getTargetAccountId()).orElse(null);

		// Ensure you have permission to answer for this screening session
		if (!getAuthorizationService().canPerformScreening(account, targetAccount))
			throw new AuthorizationException();

		List<UUID> screeningAnswerIds = getScreeningService().createScreeningAnswers(request);
		List<ScreeningAnswer> screeningAnswers = screeningAnswerIds.stream()
				.map(screeningAnswerId -> getScreeningService().findScreeningAnswerById(screeningAnswerId).get())
				.collect(Collectors.toList());

		ScreeningQuestionContext nextScreeningQuestionContext =
				getScreeningService().findNextUnansweredScreeningQuestionContextByScreeningSessionId(screeningSession.getScreeningSessionId()).orElse(null);

		ScreeningSessionDestination screeningSessionDestination = getScreeningService().determineDestinationForScreeningSessionId(screeningSession.getScreeningSessionId()).orElse(null);

		return new ApiResponse(new HashMap<String, Object>() {{
			put("screeningAnswers", screeningAnswers.stream()
					.map(screeningAnswer -> getScreeningAnswerApiResponseFactory().create(screeningAnswer))
					.collect(Collectors.toList()));
			put("nextScreeningQuestionContextId", nextScreeningQuestionContext == null ? null : nextScreeningQuestionContext.getScreeningQuestionContextId());
			put("screeningSessionDestination", screeningSessionDestination);
		}});
	}

	@Nonnull
	protected ScreeningService getScreeningService() {
		return this.screeningService;
	}

	@Nonnull
	protected AccountService getAccountService() {
		return this.accountService;
	}

	@Nonnull
	protected AuthorizationService getAuthorizationService() {
		return this.authorizationService;
	}

	@Nonnull
	protected RequestBodyParser getRequestBodyParser() {
		return this.requestBodyParser;
	}

	@Nonnull
	protected ScreeningSessionApiResponseFactory getScreeningSessionApiResponseFactory() {
		return this.screeningSessionApiResponseFactory;
	}

	@Nonnull
	protected ScreeningQuestionApiResponseFactory getScreeningQuestionApiResponseFactory() {
		return this.screeningQuestionApiResponseFactory;
	}

	@Nonnull
	protected ScreeningAnswerOptionApiResponseFactory getScreeningAnswerOptionApiResponseFactory() {
		return this.screeningAnswerOptionApiResponseFactory;
	}

	@Nonnull
	protected ScreeningAnswerApiResponseFactory getScreeningAnswerApiResponseFactory() {
		return this.screeningAnswerApiResponseFactory;
	}

	@Nonnull
	protected CurrentContext getCurrentContext() {
		return this.currentContextProvider.get();
	}

	@Nonnull
	protected Logger getLogger() {
		return this.logger;
	}
}
