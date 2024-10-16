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
import com.cobaltplatform.api.model.api.request.CreateCareResourceRequest;
import com.cobaltplatform.api.model.api.request.FindCareResourcesRequest;
import com.cobaltplatform.api.model.api.response.CareResourceApiResponse;
import com.cobaltplatform.api.model.api.response.CareResourceApiResponse.CareResourceApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PayorApiResponse.PayorApiResponseFactory;
import com.cobaltplatform.api.model.api.response.SupportRoleApiResponse.SupportRoleApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.CareResource;
import com.cobaltplatform.api.model.db.Payor;
import com.cobaltplatform.api.model.db.SupportRole;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.model.service.FindResult;
import com.cobaltplatform.api.service.AuthorizationService;
import com.cobaltplatform.api.service.CareResourceService;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.web.request.RequestBodyParser;
import com.soklet.web.annotation.GET;
import com.soklet.web.annotation.POST;
import com.soklet.web.annotation.QueryParameter;
import com.soklet.web.annotation.RequestBody;
import com.soklet.web.annotation.Resource;
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
import java.util.Map;
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
public class CareResourceResource {
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final PayorApiResponseFactory payorApiResponseFactory;
	@Nonnull
	private final CareResourceApiResponseFactory careResourceApiResponseFactory;
	@Nonnull
	private final SupportRoleApiResponseFactory supportRoleApiResponseFactory;
	@Nonnull
	private final Logger logger;
	@Nonnull
	private final CareResourceService careResourceService;
	@Nonnull
	private final RequestBodyParser requestBodyParser;
	@Nonnull
	private final Formatter formatter;


	@Inject
	public CareResourceResource(@Nonnull Provider<CurrentContext> currentContextProvider,
															@Nonnull CareResourceService careResourceService,
															@Nonnull PayorApiResponseFactory payorApiResponseFactory,
															@Nonnull SupportRoleApiResponseFactory supportRoleApiResponseFactory,
															@Nonnull RequestBodyParser requestBodyParser,
															@Nonnull CareResourceApiResponseFactory careResourceApiResponseFactory,
															@Nonnull AuthorizationService authorizationService,
															@Nonnull Formatter formatter) {
		requireNonNull(currentContextProvider);
		requireNonNull(careResourceService);
		requireNonNull(payorApiResponseFactory);
		requireNonNull(supportRoleApiResponseFactory);
		requireNonNull(requestBodyParser);
		requireNonNull(careResourceApiResponseFactory);
		requireNonNull(authorizationService);
		requireNonNull(formatter);

		this.currentContextProvider = currentContextProvider;
		this.careResourceService = careResourceService;
		this.payorApiResponseFactory = payorApiResponseFactory;
		this.supportRoleApiResponseFactory = supportRoleApiResponseFactory;
		this.logger = LoggerFactory.getLogger(getClass());
		this.careResourceApiResponseFactory = careResourceApiResponseFactory;
		this.requestBodyParser = requestBodyParser;
		this.formatter = formatter;
	}

	@Nonnull
	@GET("/payors")
	@AuthenticationRequired
	public ApiResponse findPayors() {
		List<Payor> payors = getCareResourceService().findPayors();
		return new ApiResponse(new HashMap<String, Object>() {{
			put("payors", payors.stream()
					.map(payor -> getPayorApiResponseFactory().create(payor))
					.collect(Collectors.toList()));
		}});
	}

	@Nonnull
	@GET("/support-roles")
	@AuthenticationRequired
	public ApiResponse findSupportRoles() {
		List<SupportRole> supportRoles = getCareResourceService().findCareResourceSupportRoles();
		return new ApiResponse(new HashMap<String, Object>() {{
			put("supportRoles", supportRoles.stream()
					.map(supportRole -> getSupportRoleApiResponseFactory().create(supportRole))
					.collect(Collectors.toList()));
		}});
	}

	@Nonnull
	@GET("/care-resources")
	@AuthenticationRequired
	public ApiResponse findAllCareResources(@Nonnull @QueryParameter Optional<Integer> pageNumber,
																					@Nonnull @QueryParameter Optional<Integer> pageSize) {
		requireNonNull(pageNumber);
		requireNonNull(pageSize);

		Account account = getCurrentContext().getAccount().get();
		FindResult<CareResource> findResult = getCareResourceService().findAllCareResourceByInstitutionId(new FindCareResourcesRequest() {
			{
				setPageNumber(pageNumber.orElse(0));
				setPageSize(pageSize.orElse(0));
				setInstitutionId(account.getInstitutionId());
			}
		});

		List<CareResourceApiResponse> careResources = findResult.getResults().stream().map(careResource -> getCareResourceApiResponseFactory()
				.create(careResource)).collect(Collectors.toList());
		Map<String, Object> findResultJson = new HashMap<>();
		findResultJson.put("careResources", careResources);
		findResultJson.put("totalCount", findResult.getTotalCount());
		findResultJson.put("totalCountDescription", getFormatter().formatNumber(findResult.getTotalCount()));

		return new ApiResponse(new HashMap<String, Object>() {{
			put("careResources", findResultJson);
		}});
	}

	@Nonnull
	@POST("/care-resources")
	@AuthenticationRequired
	public ApiResponse createCareResource(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		CreateCareResourceRequest request = getRequestBodyParser().parse(requestBody, CreateCareResourceRequest.class);
		request.setCreatedByAccountId(getCurrentContext().getAccount().get().getAccountId());
		request.setInstitutionId(getCurrentContext().getAccount().get().getInstitutionId());
		UUID careResourceId = getCareResourceService().createCareResource(request);
		CareResource careResource = getCareResourceService().findCareResourceByInstitutionId
				(careResourceId, getCurrentContext().getInstitutionId()).orElse(null);

		if (careResource == null)
			throw new NotFoundException();
		return new ApiResponse(new HashMap<String, Object>() {{
			put("careResource", getCareResourceApiResponseFactory().create(careResource));
		}});
	}

	@Nonnull
	protected CurrentContext getCurrentContext() {
		return this.currentContextProvider.get();
	}

	@Nonnull
	protected CareResourceService getCareResourceService() {
		return this.careResourceService;
	}

	@Nonnull
	protected Logger getLogger() {
		return this.logger;
	}

	@Nonnull
	public PayorApiResponseFactory getPayorApiResponseFactory() {
		return payorApiResponseFactory;
	}

	@Nonnull
	public CareResourceApiResponseFactory getCareResourceApiResponseFactory() {
		return careResourceApiResponseFactory;
	}

	@Nonnull
	public RequestBodyParser getRequestBodyParser() {
		return requestBodyParser;
	}

	@Nonnull
	public SupportRoleApiResponseFactory getSupportRoleApiResponseFactory() {
		return supportRoleApiResponseFactory;
	}

	@Nonnull
	public Provider<CurrentContext> getCurrentContextProvider() {
		return currentContextProvider;
	}

	@Nonnull
	public Formatter getFormatter() {
		return formatter;
	}
}
