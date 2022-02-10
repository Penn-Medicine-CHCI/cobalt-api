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
import com.cobaltplatform.api.model.api.request.CreateAppointmentTypeRequest;
import com.cobaltplatform.api.model.api.request.UpdateAppointmentTypeRequest;
import com.cobaltplatform.api.model.api.response.AppointmentTypeApiResponse.AppointmentTypeApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AppointmentTypeApiResponse.AppointmentTypeApiResponseSupplement;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AppointmentType;
import com.cobaltplatform.api.model.db.Provider;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.service.AccountService;
import com.cobaltplatform.api.service.AppointmentService;
import com.cobaltplatform.api.service.AuthorizationService;
import com.cobaltplatform.api.service.ProviderService;
import com.cobaltplatform.api.web.request.RequestBodyParser;
import com.soklet.web.annotation.DELETE;
import com.soklet.web.annotation.GET;
import com.soklet.web.annotation.POST;
import com.soklet.web.annotation.PUT;
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
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Resource
@Singleton
@ThreadSafe
public class AppointmentTypeResource {
	@Nonnull
	private final AccountService accountService;
	@Nonnull
	private final ProviderService providerService;
	@Nonnull
	private final AppointmentService appointmentService;
	@Nonnull
	private final AuthorizationService authorizationService;
	@Nonnull
	private final javax.inject.Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final RequestBodyParser requestBodyParser;
	@Nonnull
	private final AppointmentTypeApiResponseFactory appointmentTypeApiResponseFactory;
	@Nonnull
	private final Logger logger;

	@Inject
	public AppointmentTypeResource(@Nonnull AccountService accountService,
																 @Nonnull ProviderService providerService,
																 @Nonnull AppointmentService appointmentService,
																 @Nonnull AuthorizationService authorizationService,
																 @Nonnull javax.inject.Provider<CurrentContext> currentContextProvider,
																 @Nonnull RequestBodyParser requestBodyParser,
																 @Nonnull AppointmentTypeApiResponseFactory appointmentTypeApiResponseFactory) {
		requireNonNull(accountService);
		requireNonNull(providerService);
		requireNonNull(appointmentService);
		requireNonNull(authorizationService);
		requireNonNull(currentContextProvider);
		requireNonNull(requestBodyParser);
		requireNonNull(appointmentTypeApiResponseFactory);

		this.accountService = accountService;
		this.providerService = providerService;
		this.appointmentService = appointmentService;
		this.authorizationService = authorizationService;
		this.currentContextProvider = currentContextProvider;
		this.requestBodyParser = requestBodyParser;
		this.appointmentTypeApiResponseFactory = appointmentTypeApiResponseFactory;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	@GET("/appointment-types")
	@AuthenticationRequired
	public ApiResponse appointmentTypes(@Nonnull @QueryParameter UUID providerId) {
		requireNonNull(providerId);

		Account account = getCurrentContext().getAccount().get();
		Provider provider = getProviderService().findProviderById(providerId).orElse(null);

		if (provider == null)
			throw new NotFoundException();

		if (!getAuthorizationService().canViewAppointmentTypesForProvider(provider, account))
			throw new AuthorizationException();

		List<AppointmentType> appointmentTypes = getAppointmentService().findAppointmentTypesByProviderId(providerId);

		return new ApiResponse(new HashMap<String, Object>() {{
			put("appointmentTypes", appointmentTypes.stream()
					.map(appointmentType -> getAppointmentTypeApiResponseFactory().create(appointmentType))
					.collect(Collectors.toList()));
		}});
	}

	@Nonnull
	@POST("/appointment-types")
	@AuthenticationRequired
	public ApiResponse createAppointmentType(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		Account account = getCurrentContext().getAccount().get();

		// You can only create appointment types for yourself, if you are a provider
		if (account.getProviderId() == null)
			throw new AuthorizationException();

		CreateAppointmentTypeRequest request = getRequestBodyParser().parse(requestBody, CreateAppointmentTypeRequest.class);
		request.setProviderId(account.getProviderId());

		UUID appointmentTypeId = getAppointmentService().createAppointmentType(request);
		AppointmentType appointmentType = getAppointmentService().findAppointmentTypeById(appointmentTypeId).get();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("appointmentType", getAppointmentTypeApiResponseFactory().create(appointmentType, Set.of(AppointmentTypeApiResponseSupplement.EVERYTHING)));
		}});
	}

	@Nonnull
	@GET("/appointment-types/{appointmentTypeId}")
	@AuthenticationRequired
	public ApiResponse getAppointmentType(@Nonnull @PathParameter UUID appointmentTypeId) {
		requireNonNull(appointmentTypeId);

		Account account = getCurrentContext().getAccount().get();
		AppointmentType appointmentType = getAppointmentService().findAppointmentTypeById(appointmentTypeId).orElse(null);

		if (appointmentType == null)
			throw new NotFoundException();

		if (!getAuthorizationService().canViewAppointmentType(appointmentType, account))
			throw new AuthorizationException();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("appointmentType", getAppointmentTypeApiResponseFactory().create(appointmentType, Set.of(AppointmentTypeApiResponseSupplement.EVERYTHING)));
		}});
	}

	@Nonnull
	@PUT("/appointment-types/{appointmentTypeId}")
	@AuthenticationRequired
	public ApiResponse updateAppointmentType(@Nonnull @RequestBody String requestBody,
																					 @Nonnull @PathParameter UUID appointmentTypeId) {
		requireNonNull(requestBody);
		requireNonNull(appointmentTypeId);

		Account account = getCurrentContext().getAccount().get();
		AppointmentType appointmentType = getAppointmentService().findAppointmentTypeById(appointmentTypeId).orElse(null);

		if (appointmentType == null)
			throw new NotFoundException();

		if (!getAuthorizationService().canDeleteAppointmentType(appointmentType, account))
			throw new AuthorizationException();

		UpdateAppointmentTypeRequest request = getRequestBodyParser().parse(requestBody, UpdateAppointmentTypeRequest.class);
		request.setAppointmentTypeId(appointmentTypeId);

		getAppointmentService().updateAppointmentType(request);
		AppointmentType updatedAppointmentType = getAppointmentService().findAppointmentTypeById(appointmentTypeId).get();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("appointmentType", getAppointmentTypeApiResponseFactory().create(updatedAppointmentType, Set.of(AppointmentTypeApiResponseSupplement.EVERYTHING)));
		}});
	}

	@Nonnull
	@DELETE("/appointment-types/{appointmentTypeId}")
	@AuthenticationRequired
	public ApiResponse deleteAppointmentType(@Nonnull @PathParameter UUID appointmentTypeId) {
		requireNonNull(appointmentTypeId);

		Account account = getCurrentContext().getAccount().get();
		AppointmentType appointmentType = getAppointmentService().findAppointmentTypeById(appointmentTypeId).orElse(null);

		if (appointmentType == null)
			throw new NotFoundException();

		if (!getAuthorizationService().canDeleteAppointmentType(appointmentType, account))
			throw new AuthorizationException();

		getAppointmentService().deleteAppointmentType(appointmentTypeId);

		return new ApiResponse();
	}

	@Nonnull
	protected AccountService getAccountService() {
		return accountService;
	}

	@Nonnull
	protected AppointmentService getAppointmentService() {
		return appointmentService;
	}

	@Nonnull
	protected AuthorizationService getAuthorizationService() {
		return authorizationService;
	}

	@Nonnull
	protected ProviderService getProviderService() {
		return providerService;
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
	protected AppointmentTypeApiResponseFactory getAppointmentTypeApiResponseFactory() {
		return appointmentTypeApiResponseFactory;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}