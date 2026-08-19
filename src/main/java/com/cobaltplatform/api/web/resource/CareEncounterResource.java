/*
 * Copyright 2021 The University of Pennsylvania and Penn Medicine
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package com.cobaltplatform.api.web.resource;

import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.api.request.CreateCareEncounterRequest;
import com.cobaltplatform.api.model.api.request.CancelAppointmentRequest;
import com.cobaltplatform.api.model.api.request.FindCareEncountersRequest;
import com.cobaltplatform.api.model.api.request.FindCareEncountersRequest.OrderBy;
import com.cobaltplatform.api.model.api.request.UpdateCareEncounterRequest;
import com.cobaltplatform.api.model.api.response.CareEncounterApiResponse.CareEncounterApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.CareEncounter;
import com.cobaltplatform.api.model.db.CareEncounterStatus.CareEncounterStatusId;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.model.service.FindResult;
import com.cobaltplatform.api.service.AuthorizationService;
import com.cobaltplatform.api.service.CareEncounterService;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.db.ReadReplica;
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

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Care Navigator administrative endpoints.
 *
 * @author Transmogrify, LLC.
 */
@Resource
@Singleton
@ThreadSafe
public class CareEncounterResource {
	@Nonnull
	private final CareEncounterService careEncounterService;
	@Nonnull
	private final AuthorizationService authorizationService;
	@Nonnull
	private final RequestBodyParser requestBodyParser;
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final CareEncounterApiResponseFactory careEncounterApiResponseFactory;
	@Nonnull
	private final Formatter formatter;

	@Inject
	public CareEncounterResource(@Nonnull CareEncounterService careEncounterService,
															 @Nonnull AuthorizationService authorizationService,
														 @Nonnull RequestBodyParser requestBodyParser,
														 @Nonnull Provider<CurrentContext> currentContextProvider,
														 @Nonnull CareEncounterApiResponseFactory careEncounterApiResponseFactory,
														 @Nonnull Formatter formatter) {
		this.careEncounterService = careEncounterService;
		this.authorizationService = authorizationService;
		this.requestBodyParser = requestBodyParser;
		this.currentContextProvider = currentContextProvider;
		this.careEncounterApiResponseFactory = careEncounterApiResponseFactory;
		this.formatter = formatter;
	}

	@Nonnull
	@GET("/admin/care-encounters")
	@AuthenticationRequired
	@ReadReplica
	public ApiResponse careEncounters(@Nonnull @QueryParameter Optional<Integer> pageNumber,
															 @Nonnull @QueryParameter Optional<Integer> pageSize,
															 @Nonnull @QueryParameter Optional<LocalDate> startDate,
															 @Nonnull @QueryParameter Optional<LocalDate> endDate,
															 @Nonnull @QueryParameter Optional<String> searchQuery,
															 @Nonnull @QueryParameter Optional<CareEncounterStatusId> careEncounterStatusId,
															 @Nonnull @QueryParameter Optional<OrderBy> orderBy) {
		requireNonNull(pageNumber);
		requireNonNull(pageSize);
		requireNonNull(startDate);
		requireNonNull(endDate);
		requireNonNull(searchQuery);
		requireNonNull(careEncounterStatusId);
		requireNonNull(orderBy);

		Account account = requireCareNavigatorAccount();
		FindCareEncountersRequest request = new FindCareEncountersRequest();
		request.setProviderId(account.getProviderId());
		request.setPageNumber(pageNumber.orElse(null));
		request.setPageSize(pageSize.orElse(null));
		request.setStartDate(startDate.orElse(null));
		request.setEndDate(endDate.orElse(null));
		request.setSearchQuery(searchQuery.orElse(null));
		request.setCareEncounterStatusId(careEncounterStatusId.orElse(null));
		request.setOrderBy(orderBy.orElse(null));

		FindResult<CareEncounter> result = getCareEncounterService().findCareEncounters(request);

		return new ApiResponse(new LinkedHashMap<String, Object>() {{
			put("totalCount", result.getTotalCount());
			put("totalCountDescription", getFormatter().formatNumber(result.getTotalCount()));
			put("careEncounters", result.getResults().stream()
						.map(getCareEncounterApiResponseFactory()::create)
						.collect(Collectors.toList()));
		}});
	}

	@Nonnull
	@GET("/admin/care-encounters/{careEncounterId}")
	@AuthenticationRequired
	@ReadReplica
	public ApiResponse careEncounter(@Nonnull @PathParameter UUID careEncounterId) {
		requireNonNull(careEncounterId);

		Account account = requireCareNavigatorAccount();
		CareEncounter careEncounter = getCareEncounterService()
				.findCareEncounterByIdForProviderId(careEncounterId, account.getProviderId())
				.orElseThrow(NotFoundException::new);

		return new ApiResponse(Map.of("careEncounter", getCareEncounterApiResponseFactory().create(careEncounter)));
	}

	@Nonnull
	@POST("/admin/care-encounters")
	@AuthenticationRequired
	public ApiResponse createCareEncounter(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		Account account = requireCareNavigatorAccount();
		CreateCareEncounterRequest request = getRequestBodyParser().parse(requestBody, CreateCareEncounterRequest.class);
		request.setProviderId(account.getProviderId());
		request.setAccountId(account.getAccountId());
		CareEncounter careEncounter = getCareEncounterService().createCareEncounter(request);

		return new ApiResponse(Map.of("careEncounter", getCareEncounterApiResponseFactory().create(careEncounter)));
	}

	@Nonnull
	@PUT("/admin/care-encounters/{careEncounterId}")
	@AuthenticationRequired
	public ApiResponse updateCareEncounter(@Nonnull @PathParameter UUID careEncounterId,
																						 @Nonnull @RequestBody String requestBody) {
		requireNonNull(careEncounterId);
		requireNonNull(requestBody);

		Account account = requireCareNavigatorAccount();
		UpdateCareEncounterRequest request = getRequestBodyParser().parse(requestBody, UpdateCareEncounterRequest.class);
		request.setCareEncounterId(careEncounterId);
		request.setProviderId(account.getProviderId());
		request.setAccountId(account.getAccountId());
		CareEncounter careEncounter = getCareEncounterService().updateCareEncounter(request);

		return new ApiResponse(Map.of("careEncounter", getCareEncounterApiResponseFactory().create(careEncounter)));
	}

	@Nonnull
	@PUT("/admin/care-encounters/{careEncounterId}/cancel")
	@AuthenticationRequired
	public ApiResponse cancelCareEncounter(@Nonnull @PathParameter UUID careEncounterId,
																			 @Nonnull @RequestBody String requestBody) {
		requireNonNull(careEncounterId);
		requireNonNull(requestBody);

		Account account = requireCareNavigatorAccount();
		CancelAppointmentRequest request = getRequestBodyParser().parse(requestBody, CancelAppointmentRequest.class);
		CareEncounter careEncounter = getCareEncounterService().cancelCareEncounter(
				careEncounterId,
				account.getProviderId(),
				account.getAccountId(),
				request);

		return new ApiResponse(Map.of("careEncounter", getCareEncounterApiResponseFactory().create(careEncounter)));
	}

	@Nonnull
	@DELETE("/admin/care-encounters/{careEncounterId}")
	@AuthenticationRequired
	public ApiResponse deleteCareEncounter(@Nonnull @PathParameter UUID careEncounterId) {
		requireNonNull(careEncounterId);

		Account account = requireCareNavigatorAccount();
		CareEncounter careEncounter = getCareEncounterService()
				.findCareEncounterByIdForProviderId(careEncounterId, account.getProviderId())
				.orElseThrow(NotFoundException::new);

		getCareEncounterService().deleteCareEncounter(careEncounter.getCareEncounterId(), account.getProviderId(), account.getAccountId());
		return new ApiResponse();
	}

	@Nonnull
	protected Account requireCareNavigatorAccount() {
		Account account = getCurrentContext().getAccount().get();

		if (!getAuthorizationService().canManageCareEncounters(account))
			throw new AuthorizationException();

		return account;
	}

	@Nonnull
	protected CurrentContext getCurrentContext() {
		return this.currentContextProvider.get();
	}

	@Nonnull
	protected CareEncounterService getCareEncounterService() {
		return this.careEncounterService;
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
	protected CareEncounterApiResponseFactory getCareEncounterApiResponseFactory() {
		return this.careEncounterApiResponseFactory;
	}

	@Nonnull
	protected Formatter getFormatter() {
		return this.formatter;
	}
}
