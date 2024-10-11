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
import com.cobaltplatform.api.model.api.response.CareResourceApiResponse.CareResourceApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PayorApiResponse.PayorApiResponseFactory;
import com.cobaltplatform.api.model.db.CareResource;
import com.cobaltplatform.api.model.db.Payor;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.service.CareResourceService;
import com.cobaltplatform.api.web.request.RequestBodyParser;
import com.soklet.web.annotation.GET;
import com.soklet.web.annotation.POST;
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
	private final Logger logger;
	@Nonnull
	private final CareResourceService careResourceService;
	@Nonnull
	private final RequestBodyParser requestBodyParser;


	@Inject
	public CareResourceResource(@Nonnull Provider<CurrentContext> currentContextProvider,
															@Nonnull CareResourceService careResourceService,
															@Nonnull PayorApiResponseFactory payorApiResponseFactory,
															@Nonnull RequestBodyParser requestBodyParser,
															@Nonnull CareResourceApiResponseFactory careResourceApiResponseFactory) {
		requireNonNull(currentContextProvider);

		this.currentContextProvider = currentContextProvider;
		this.careResourceService = careResourceService;
		this.payorApiResponseFactory = payorApiResponseFactory;
		this.logger = LoggerFactory.getLogger(getClass());
		this.careResourceApiResponseFactory = careResourceApiResponseFactory;
		this.requestBodyParser = requestBodyParser;
	}

	@Nonnull
	@GET("/payors")
	@AuthenticationRequired
	public ApiResponse payors() {
		List<Payor> payors = getCareResourceService().findPayors();
		return new ApiResponse(new HashMap<String, Object>() {{
			put("payors", payors.stream()
					.map(payor -> getPayorApiResponseFactory().create(payor))
					.collect(Collectors.toList()));
		}});
	}

	@Nonnull
	@GET("/care-resources")
	@AuthenticationRequired
	public ApiResponse findAllCareResources() {
		List<CareResource> careResources = getCareResourceService()
				.findAllCareResourceByInstitutionId(getCurrentContext().getAccount().get().getInstitutionId());
		return new ApiResponse(new HashMap<String, Object>() {{
			put("careResources", careResources.stream()
					.map(careResource -> getCareResourceApiResponseFactory().create(careResource))
					.collect(Collectors.toList()));
		}});
	}

	@Nonnull
	@POST("/care-resource")
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
			put("content", getCareResourceApiResponseFactory().create(careResource));
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
}
