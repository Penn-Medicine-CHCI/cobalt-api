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
import com.cobaltplatform.api.model.api.request.CreateLogicalAvailabilityRequest;
import com.cobaltplatform.api.model.api.response.LogicalAvailabilityApiResponse.LogicalAvailabilityApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.LogicalAvailability;
import com.cobaltplatform.api.model.db.Provider;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.service.AccountService;
import com.cobaltplatform.api.service.AuthorizationService;
import com.cobaltplatform.api.service.AvailabilityService;
import com.cobaltplatform.api.service.ProviderService;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.web.request.RequestBodyParser;
import com.lokalized.Strings;
import com.soklet.web.annotation.DELETE;
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
import javax.inject.Singleton;
import java.time.LocalDate;
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
public class AvailabilityResource {
	@Nonnull
	private final AccountService accountService;
	@Nonnull
	private final ProviderService providerService;
	@Nonnull
	private final AvailabilityService availabilityService;
	@Nonnull
	private final AuthorizationService authorizationService;
	@Nonnull
	private final LogicalAvailabilityApiResponseFactory logicalAvailabilityApiResponseFactory;
	@Nonnull
	private final javax.inject.Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final RequestBodyParser requestBodyParser;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;

	@Inject
	public AvailabilityResource(@Nonnull AccountService accountService,
															@Nonnull ProviderService providerService,
															@Nonnull AvailabilityService availabilityService,
															@Nonnull AuthorizationService authorizationService,
															@Nonnull LogicalAvailabilityApiResponseFactory logicalAvailabilityApiResponseFactory,
															@Nonnull javax.inject.Provider<CurrentContext> currentContextProvider,
															@Nonnull RequestBodyParser requestBodyParser,
															@Nonnull Strings strings) {
		requireNonNull(accountService);
		requireNonNull(providerService);
		requireNonNull(availabilityService);
		requireNonNull(authorizationService);
		requireNonNull(logicalAvailabilityApiResponseFactory);
		requireNonNull(currentContextProvider);
		requireNonNull(requestBodyParser);
		requireNonNull(strings);

		this.accountService = accountService;
		this.providerService = providerService;
		this.availabilityService = availabilityService;
		this.authorizationService = authorizationService;
		this.logicalAvailabilityApiResponseFactory = logicalAvailabilityApiResponseFactory;
		this.currentContextProvider = currentContextProvider;
		this.requestBodyParser = requestBodyParser;
		this.strings = strings;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	@POST("/logical-availabilities")
	@AuthenticationRequired
	public ApiResponse createLogicalAvailability(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		Account account = getCurrentContext().getAccount().get();

		CreateLogicalAvailabilityRequest request = getRequestBodyParser().parse(requestBody, CreateLogicalAvailabilityRequest.class);
		request.setAccountId(account.getAccountId());

		Provider provider = getProviderService().findProviderById(request.getProviderId()).orElse(null);

		if (provider == null)
			throw new ValidationException(new ValidationException.FieldError("providerId", getStrings().get("Provider is invalid.")));

		if (!getAuthorizationService().canEditProviderCalendar(provider, account))
			throw new AuthorizationException();

		UUID logicalAvailabilityId = getAvailabilityService().createLogicalAvailability(request);
		LogicalAvailability logicalAvailability = getAvailabilityService().findLogicalAvailabilityById(logicalAvailabilityId).get();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("logicalAvailability", getLogicalAvailabilityApiResponseFactory().create(logicalAvailability));
		}});
	}

	@Nonnull
	@GET("/logical-availabilities")
	@AuthenticationRequired
	public ApiResponse logicalAvailabilities(@Nonnull @QueryParameter UUID providerId,
																					 @Nonnull @QueryParameter Optional<LocalDate> startDate,
																					 @Nonnull @QueryParameter Optional<LocalDate> endDate) {
		requireNonNull(providerId);
		requireNonNull(startDate);
		requireNonNull(endDate);

		Account account = getCurrentContext().getAccount().get();
		Provider provider = getProviderService().findProviderById(providerId).orElse(null);

		if (provider == null)
			throw new ValidationException(new ValidationException.FieldError("providerId", getStrings().get("Provider is invalid.")));

		if (!getAuthorizationService().canViewProviderCalendar(provider, account))
			throw new AuthorizationException();

		List<LogicalAvailability> logicalAvailabilities = getAvailabilityService().findLogicalAvailabilities(providerId, startDate.orElse(null), endDate.orElse(null));

		return new ApiResponse(new HashMap<String, Object>() {{
			put("logicalAvailabilities", logicalAvailabilities.stream()
					.map(logicalAvailability -> getLogicalAvailabilityApiResponseFactory().create(logicalAvailability))
					.collect(Collectors.toList()));
		}});
	}

	@Nonnull
	@DELETE("/logical-availabilities/{logicalAvailabilityId}")
	@AuthenticationRequired
	public void deleteLogicalAvailability(@Nonnull @PathParameter UUID logicalAvailabilityId) {
		requireNonNull(logicalAvailabilityId);

		LogicalAvailability logicalAvailability = getAvailabilityService().findLogicalAvailabilityById(logicalAvailabilityId).orElse(null);

		if (logicalAvailability == null)
			throw new NotFoundException();

		Account account = getCurrentContext().getAccount().get();
		Provider provider = getProviderService().findProviderById(logicalAvailability.getProviderId()).orElse(null);

		if (!getAuthorizationService().canEditProviderCalendar(provider, account))
			throw new AuthorizationException();

		getAvailabilityService().deleteLogicalAvailability(logicalAvailabilityId);
	}

	@Nonnull
	protected AccountService getAccountService() {
		return accountService;
	}

	@Nonnull
	protected ProviderService getProviderService() {
		return providerService;
	}

	@Nonnull
	protected AvailabilityService getAvailabilityService() {
		return availabilityService;
	}

	@Nonnull
	protected AuthorizationService getAuthorizationService() {
		return authorizationService;
	}

	@Nonnull
	protected LogicalAvailabilityApiResponseFactory getLogicalAvailabilityApiResponseFactory() {
		return logicalAvailabilityApiResponseFactory;
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
	protected Strings getStrings() {
		return strings;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}