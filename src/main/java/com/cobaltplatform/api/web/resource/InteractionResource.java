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
import com.cobaltplatform.api.model.api.request.CreateInteractionInstance;
import com.cobaltplatform.api.model.api.response.InteractionInstanceApiResponse.InteractionInstanceApiResponseFactory;
import com.cobaltplatform.api.model.api.response.InteractionOptionApiResponse;
import com.cobaltplatform.api.model.api.response.InteractionOptionApiResponse.InteractionOptionApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.InteractionInstance;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.service.InteractionService;
import com.cobaltplatform.api.web.request.RequestBodyParser;
import com.soklet.web.annotation.POST;
import com.soklet.web.annotation.PathParameter;
import com.soklet.web.annotation.RequestBody;
import com.soklet.web.annotation.Resource;
import com.soklet.web.exception.AuthorizationException;
import com.soklet.web.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.UUID;

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
	private final InteractionInstanceApiResponseFactory interactionInstanceApiResponseFactory;
	@Nonnull
	private final InteractionOptionApiResponseFactory interactionOptionApiResponseFactory;

	@Inject
	public InteractionResource(@Nonnull javax.inject.Provider<CurrentContext> currentContextProvider,
														 @Nonnull RequestBodyParser requestBodyParser,
														 @Nonnull InteractionService interactionService,
														 @Nonnull InteractionInstanceApiResponseFactory interactionInstanceApiResponseFactory,
														 @Nonnull InteractionOptionApiResponseFactory interactionOptionApiResponseFactory) {
		requireNonNull(currentContextProvider);
		requireNonNull(requestBodyParser);
		requireNonNull(interactionService);
		requireNonNull(interactionInstanceApiResponseFactory);
		requireNonNull(interactionOptionApiResponseFactory);

		this.currentContextProvider = currentContextProvider;
		this.requestBodyParser = requestBodyParser;
		this.logger = LoggerFactory.getLogger(getClass());
		this.interactionService = interactionService;
		this.interactionInstanceApiResponseFactory = interactionInstanceApiResponseFactory;
		this.interactionOptionApiResponseFactory = interactionOptionApiResponseFactory;
	}

	@Nonnull
	@POST("/interaction/instance")
	@AuthenticationRequired
	public ApiResponse createInteractionInstance(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		Account account = getCurrentContext().getAccount().get();

		if (account.getRoleId() != RoleId.ADMINISTRATOR)
			throw new AuthorizationException();

		CreateInteractionInstance request = getRequestBodyParser().parse(requestBody, CreateInteractionInstance.class);
		request.setAccountId(account.getAccountId());

		UUID interactionInstanceId = getInteractionService().createInteractionInstance(request);
		InteractionInstance interactionInstance = getInteractionService().findRequiredInteractionInstanceById(interactionInstanceId);

		return new ApiResponse(new HashMap<String, Object>() {{
			put("interactionInstance", getInteractionInstanceApiResponseFactory().create(interactionInstance));
		}});
	}

	@Nonnull
	@POST("/interaction/{interactionInstanceId}/option/{interactionOptionId}")
	@AuthenticationRequired
	public ApiResponse createInteractionOptionAction(@Nonnull @PathParameter UUID interactionInstanceId,
																									 @Nonnull @PathParameter UUID interactionOptionId) {
		requireNonNull(interactionInstanceId);
		requireNonNull(interactionOptionId);

		Account account = getCurrentContext().getAccount().get();

		getInteractionService().createInteractionOptionAction(account.getAccountId(), interactionInstanceId, interactionOptionId);

		return new ApiResponse(new HashMap<String, Object>() {{
			put("interactionOption", getInteractionOptionApiResponse().create(getInteractionService().findRequiredInteractionOptionsById(interactionOptionId),
					getInteractionService().findRequiredInteractionInstanceById(interactionInstanceId)));
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
	protected InteractionInstanceApiResponseFactory getInteractionInstanceApiResponseFactory() {
		return interactionInstanceApiResponseFactory;
	}

	@Nonnull
	protected InteractionOptionApiResponseFactory getInteractionOptionApiResponse() {
		return interactionOptionApiResponseFactory;
	}

}