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
import com.cobaltplatform.api.model.api.request.CreateInteractionInstanceRequest;
import com.cobaltplatform.api.model.api.request.CreateInteractionOptionActionRequest;
import com.cobaltplatform.api.model.api.response.InteractionInstanceApiResponse.InteractionInstanceApiResponseFactory;
import com.cobaltplatform.api.model.api.response.InteractionOptionActionApiResponse.InteractionOptionActionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.InteractionOptionApiResponse.InteractionOptionApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.Interaction;
import com.cobaltplatform.api.model.db.InteractionInstance;
import com.cobaltplatform.api.model.db.InteractionOption;
import com.cobaltplatform.api.model.db.InteractionOptionAction;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.service.AuthorizationService;
import com.cobaltplatform.api.service.InteractionService;
import com.cobaltplatform.api.web.request.RequestBodyParser;
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
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Resource
@Singleton
@ThreadSafe
public class InteractionResource {
	@Nonnull
	private final javax.inject.Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final RequestBodyParser requestBodyParser;
	@Nonnull
	private final Logger logger;
	@Nonnull
	private final InteractionService interactionService;
	@Nonnull
	private final AuthorizationService authorizationService;
	@Nonnull
	private final InteractionInstanceApiResponseFactory interactionInstanceApiResponseFactory;
	@Nonnull
	private final InteractionOptionApiResponseFactory interactionOptionApiResponseFactory;
	@Nonnull
	private final InteractionOptionActionApiResponseFactory interactionOptionActionApiResponseFactory;

	@Inject
	public InteractionResource(@Nonnull javax.inject.Provider<CurrentContext> currentContextProvider,
														 @Nonnull RequestBodyParser requestBodyParser,
														 @Nonnull InteractionService interactionService,
														 @Nonnull AuthorizationService authorizationService,
														 @Nonnull InteractionInstanceApiResponseFactory interactionInstanceApiResponseFactory,
														 @Nonnull InteractionOptionApiResponseFactory interactionOptionApiResponseFactory,
														 @Nonnull InteractionOptionActionApiResponseFactory interactionOptionActionApiResponseFactory) {
		requireNonNull(currentContextProvider);
		requireNonNull(requestBodyParser);
		requireNonNull(interactionService);
		requireNonNull(authorizationService);
		requireNonNull(interactionInstanceApiResponseFactory);
		requireNonNull(interactionOptionApiResponseFactory);
		requireNonNull(interactionOptionActionApiResponseFactory);

		this.currentContextProvider = currentContextProvider;
		this.requestBodyParser = requestBodyParser;
		this.logger = LoggerFactory.getLogger(getClass());
		this.interactionService = interactionService;
		this.authorizationService = authorizationService;
		this.interactionInstanceApiResponseFactory = interactionInstanceApiResponseFactory;
		this.interactionOptionApiResponseFactory = interactionOptionApiResponseFactory;
		this.interactionOptionActionApiResponseFactory = interactionOptionActionApiResponseFactory;
	}

	@Nonnull
	@POST("/interaction-instances")
	@AuthenticationRequired
	public ApiResponse createInteractionInstance(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		Account account = getCurrentContext().getAccount().get();

		CreateInteractionInstanceRequest request = getRequestBodyParser().parse(requestBody, CreateInteractionInstanceRequest.class);
		request.setAccountId(account.getAccountId());

		Interaction interaction = getInteractionService().findInteractionById(request.getInteractionId()).orElse(null);

		if (interaction == null)
			throw new NotFoundException();

		if (!getAuthorizationService().canCreateInteractionInstance(interaction, account))
			throw new AuthorizationException();

		if (request.getTimeZone() == null)
			request.setTimeZone(getCurrentContext().getTimeZone());

		UUID interactionInstanceId = getInteractionService().createInteractionInstance(request);

		return createInteractionInstanceApiResponse(interactionInstanceId);
	}

	@Nonnull
	@GET("/interaction-instances/{interactionInstanceId}")
	@AuthenticationRequired
	public ApiResponse interactionInstance(@Nonnull @PathParameter UUID interactionInstanceId) {
		requireNonNull(interactionInstanceId);

		InteractionInstance interactionInstance = getInteractionService().findInteractionInstanceById(interactionInstanceId).orElse(null);

		if (interactionInstance == null)
			throw new NotFoundException();

		if (!getAuthorizationService().canViewInteractionInstance(interactionInstance, getCurrentContext().getAccount().get()))
			throw new AuthorizationException();

		return createInteractionInstanceApiResponse(interactionInstanceId);
	}

	@Nonnull
	@POST("/interaction-option-actions")
	@AuthenticationRequired
	public ApiResponse createInteractionOptionAction(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		Account account = getCurrentContext().getAccount().get();

		CreateInteractionOptionActionRequest request = getRequestBodyParser().parse(requestBody, CreateInteractionOptionActionRequest.class);
		request.setAccountId(account.getAccountId());

		InteractionInstance interactionInstance = getInteractionService().findInteractionInstanceById(request.getInteractionInstanceId()).orElse(null);

		if (interactionInstance == null)
			throw new NotFoundException();

		InteractionOption interactionOption = getInteractionService().findInteractionOptionById(request.getInteractionOptionId()).orElse(null);

		if (interactionOption == null)
			throw new NotFoundException();

		if (!getAuthorizationService().canTakeActionOnInteractionInstance(interactionInstance, account))
			throw new AuthorizationException();

		getInteractionService().createInteractionOptionAction(request);

		return createInteractionInstanceApiResponse(request.getInteractionInstanceId());
	}

	@Nonnull
	protected ApiResponse createInteractionInstanceApiResponse(@Nonnull UUID interactionInstanceId) {
		requireNonNull(interactionInstanceId);

		InteractionInstance interactionInstance = getInteractionService().findInteractionInstanceById(interactionInstanceId).orElse(null);
		List<InteractionOption> interactionOptions = getInteractionService().findInteractionOptionsByInteractionId(interactionInstance.getInteractionId());
		List<InteractionOptionAction> interactionOptionActions = getInteractionService().findInteractionOptionActionsByInteractionInstanceId(interactionInstanceId);

		// If this interaction is complete, hide all the options.
		// Otherwise, if there was a nonfinal interaction action taken under an hour ago,
		// don't show the option to re-take it until the remainder of the hour passes
		// (this helps avoid -- but does not strictly prevent -- people smashing the "Contacted, no response" button, for example)
		if (interactionInstance.getCompletedFlag()) {
			interactionOptions.clear();
		} else if (interactionOptionActions.size() > 0) {
			Instant mostRecentActionTimestamp = interactionOptionActions.stream()
					.map(interactionOptionAction -> interactionOptionAction.getCreated())
					.max(Instant::compareTo)
					.get();

			Instant now = Instant.now();
			Instant threshold = now.minus(1, ChronoUnit.HOURS);

			if (mostRecentActionTimestamp.isAfter(threshold)) {
				List<InteractionOption> nonfinalInteractionOptions = interactionOptions.stream()
						.filter(interactionOption -> !interactionOption.getFinalFlag())
						.collect(Collectors.toList());

				interactionOptions.removeAll(nonfinalInteractionOptions);
			}
		}

		return new ApiResponse(new HashMap<String, Object>() {{
			put("interactionInstance", getInteractionInstanceApiResponseFactory().create(interactionInstance));
			put("interactionOptions", interactionOptions.stream()
					.map(interactionOption -> getInteractionOptionApiResponseFactory().create(interactionOption, interactionInstance))
					.collect(Collectors.toList()));
			put("interactionOptionActions", interactionOptionActions.stream()
					.map(interactionOptionAction -> getInteractionOptionActionApiResponseFactory().create(interactionOptionAction))
					.collect(Collectors.toList()));
		}});
	}

	@Nonnull
	protected CurrentContext getCurrentContext() {
		return currentContextProvider.get();
	}

	@Nonnull
	protected RequestBodyParser getRequestBodyParser() {
		return requestBodyParser;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}

	@Nonnull
	protected InteractionService getInteractionService() {
		return interactionService;
	}

	@Nonnull
	protected AuthorizationService getAuthorizationService() {
		return authorizationService;
	}

	@Nonnull
	protected InteractionInstanceApiResponseFactory getInteractionInstanceApiResponseFactory() {
		return interactionInstanceApiResponseFactory;
	}

	@Nonnull
	protected InteractionOptionApiResponseFactory getInteractionOptionApiResponseFactory() {
		return interactionOptionApiResponseFactory;
	}

	@Nonnull
	protected InteractionOptionActionApiResponseFactory getInteractionOptionActionApiResponseFactory() {
		return interactionOptionActionApiResponseFactory;
	}
}