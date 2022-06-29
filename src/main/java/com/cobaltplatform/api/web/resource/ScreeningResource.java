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
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.model.service.ScreeningSessionScreeningContext;
import com.cobaltplatform.api.service.AccountService;
import com.cobaltplatform.api.service.AuthorizationService;
import com.cobaltplatform.api.service.ScreeningService;
import com.cobaltplatform.api.web.request.RequestBodyParser;
import com.devskiller.friendly_id.FriendlyId;
import com.soklet.web.annotation.GET;
import com.soklet.web.annotation.POST;
import com.soklet.web.annotation.PathParameter;
import com.soklet.web.annotation.RequestBody;
import com.soklet.web.annotation.Resource;
import com.soklet.web.exception.AuthorizationException;
import com.soklet.web.exception.NotFoundException;
import com.soklet.web.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.String.format;
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

		ScreeningSessionScreeningContext nextScreeningSessionScreeningContext = getScreeningService().findNextScreeningSessionScreeningContextByScreeningSessionId(screeningSessionId).orElse(null);
		ScreeningQuestionContextId nextScreeningQuestionContextId = new ScreeningQuestionContextId(
				nextScreeningSessionScreeningContext.getScreeningSessionScreening().getScreeningSessionScreeningId(),
				nextScreeningSessionScreeningContext.getScreeningQuestion().getScreeningQuestionId());

		return new ApiResponse(new HashMap<String, Object>() {{
			put("screeningSession", getScreeningSessionApiResponseFactory().create(screeningSession));
			put("nextScreeningQuestionContextId", nextScreeningQuestionContextId);
		}});
	}

	@Nonnull
	@GET("/screening-question-contexts/{screeningQuestionContextIdAsString}")
	@AuthenticationRequired
	public ApiResponse screeningQuestionContext(@Nonnull @PathParameter("screeningQuestionContextIdAsString") String screeningQuestionContextIdAsString) {
		requireNonNull(screeningQuestionContextIdAsString);

		ScreeningQuestionContextId screeningQuestionContextId = new ScreeningQuestionContextId(screeningQuestionContextIdAsString);
		ScreeningSessionScreeningContext screeningSessionScreeningContext = getScreeningService().findScreeningSessionScreeningContextByScreeningSessionScreeningIdAndQuestionId(
				screeningQuestionContextId.getScreeningSessionScreeningId(), screeningQuestionContextId.getScreeningQuestionId()).orElse(null);

		if (screeningSessionScreeningContext == null)
			throw new NotFoundException();

		// Questions, answer options, and any answers already given for this screening session screening
		ScreeningQuestion screeningQuestion = screeningSessionScreeningContext.getScreeningQuestion();
		List<ScreeningAnswerOption> screeningAnswerOptions = screeningSessionScreeningContext.getScreeningAnswerOptions();
		List<ScreeningAnswer> screeningAnswers = getScreeningService().findCurrentScreeningAnswersByScreeningSessionScreeningIdAndQuestionId(screeningQuestionContextId.getScreeningSessionScreeningId(), screeningQuestionContextId.getScreeningQuestionId());

		// Generate a link back to the previously-answered question in the same screening session.
		// This might be a question in a previous screening session screening.
		// This will not exist at all if we have not answered any questions yet.
		ScreeningSessionScreeningContext previousScreeningSessionScreeningContext = getScreeningService().findPreviousScreeningSessionScreeningContextByScreeningSessionScreeningIdAndQuestionId(
				screeningSessionScreeningContext.getScreeningSessionScreening().getScreeningSessionScreeningId(), screeningQuestion.getScreeningQuestionId()).orElse(null);

		String previousScreeningQuestionContextId = previousScreeningSessionScreeningContext == null ? null :
				new ScreeningQuestionContextId(previousScreeningSessionScreeningContext.getScreeningSessionScreening().getScreeningSessionScreeningId(),
						previousScreeningSessionScreeningContext.getScreeningQuestion().getScreeningQuestionId()).getIdentifier();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("previousScreeningQuestionContextId", previousScreeningQuestionContextId);
			put("screeningQuestion", getScreeningQuestionApiResponseFactory().create(screeningQuestion));
			put("screeningAnswerOptions", screeningAnswerOptions.stream()
					.map(screeningAnswerOption -> getScreeningAnswerOptionApiResponseFactory().create(screeningAnswerOption))
					.collect(Collectors.toList()));
			put("screeningAnswers", screeningAnswers.stream()
					.map(screeningAnswer -> getScreeningAnswerApiResponseFactory().create(screeningAnswer))
					.collect(Collectors.toList()));
		}});
	}

	/**
	 * Combines screeningSessionScreeningId and screeningQuestionId into a single identifier string for API ease-of-use.
	 */
	@Immutable
	public static class ScreeningQuestionContextId {
		@Nonnull
		private final String identifier;
		@Nonnull
		private final UUID screeningSessionScreeningId;
		@Nonnull
		private final UUID screeningQuestionId;

		public ScreeningQuestionContextId(@Nonnull UUID screeningSessionScreeningId,
																			@Nonnull UUID screeningQuestionId) {
			requireNonNull(screeningSessionScreeningId);
			requireNonNull(screeningQuestionId);

			this.screeningSessionScreeningId = screeningSessionScreeningId;
			this.screeningQuestionId = screeningQuestionId;
			this.identifier = format("%s-%s", FriendlyId.toFriendlyId(screeningSessionScreeningId), FriendlyId.toFriendlyId(screeningQuestionId));
		}

		public ScreeningQuestionContextId(@Nonnull String screeningQuestionContextId) {
			requireNonNull(screeningQuestionContextId);

			screeningQuestionContextId = screeningQuestionContextId.trim();

			try {
				String[] components = screeningQuestionContextId.split("-");

				this.screeningSessionScreeningId = FriendlyId.toUuid(components[0]);
				this.screeningQuestionId = FriendlyId.toUuid(components[1]);
				this.identifier = screeningQuestionContextId;
			} catch (Exception e) {
				throw new IllegalArgumentException(format("Illegal ScreeningQuestionContextId was specified: '%s'", screeningQuestionContextId));
			}
		}

		@Nonnull
		public String getIdentifier() {
			return this.identifier;
		}

		@Nonnull
		public UUID getScreeningSessionScreeningId() {
			return this.screeningSessionScreeningId;
		}

		@Nonnull
		public UUID getScreeningQuestionId() {
			return this.screeningQuestionId;
		}
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
