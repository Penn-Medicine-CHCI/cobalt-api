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
import com.cobaltplatform.api.model.api.response.InstitutionResourceApiResponse.InstitutionResourceApiResponseFactory;
import com.cobaltplatform.api.model.api.response.InstitutionResourceGroupApiResponse.InstitutionResourceGroupApiResponseFactory;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.InstitutionResource;
import com.cobaltplatform.api.model.db.InstitutionResourceGroup;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.service.InstitutionResourceService;
import com.cobaltplatform.api.service.InstitutionService;
import com.soklet.web.annotation.GET;
import com.soklet.web.annotation.PathParameter;
import com.soklet.web.annotation.QueryParameter;
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
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Resource
@Singleton
@ThreadSafe
public class InstitutionResourceResource {
	@Nonnull
	private final InstitutionService institutionService;
	@Nonnull
	private final InstitutionResourceService institutionResourceService;
	@Nonnull
	private final InstitutionResourceApiResponseFactory institutionResourceApiResponseFactory;
	@Nonnull
	private final InstitutionResourceGroupApiResponseFactory institutionResourceGroupApiResponseFactory;
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final Logger logger;

	@Inject
	public InstitutionResourceResource(@Nonnull InstitutionService institutionService,
																		 @Nonnull InstitutionResourceService institutionResourceService,
																		 @Nonnull InstitutionResourceApiResponseFactory institutionResourceApiResponseFactory,
																		 @Nonnull InstitutionResourceGroupApiResponseFactory institutionResourceGroupApiResponseFactory,
																		 @Nonnull Provider<CurrentContext> currentContextProvider) {
		requireNonNull(institutionService);
		requireNonNull(institutionResourceService);
		requireNonNull(institutionResourceApiResponseFactory);
		requireNonNull(institutionResourceGroupApiResponseFactory);
		requireNonNull(currentContextProvider);

		this.institutionService = institutionService;
		this.institutionResourceService = institutionResourceService;
		this.institutionResourceApiResponseFactory = institutionResourceApiResponseFactory;
		this.institutionResourceGroupApiResponseFactory = institutionResourceGroupApiResponseFactory;
		this.currentContextProvider = currentContextProvider;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	@GET("/institution-resource-groups")
	@AuthenticationRequired
	public ApiResponse institutionResourceGroups() {
		Institution institution = getInstitutionService().findInstitutionById(getCurrentContext().getInstitutionId()).get();
		List<InstitutionResourceGroup> institutionResourceGroups = getInstitutionResourceService().findInstitutionResourceGroupsByInstitutionId(getCurrentContext().getInstitutionId());

		return new ApiResponse(new HashMap<String, Object>() {{
			put("institutionResourceGroupsTitle", institution.getResourceGroupsTitle());
			put("institutionResourceGroupsDescription", institution.getResourceGroupsDescription());
			put("institutionResourceGroups", institutionResourceGroups.stream()
					.map(institutionResourceGroup -> getInstitutionResourceGroupApiResponseFactory().create(institutionResourceGroup))
					.collect(Collectors.toList()));
		}});
	}

	@Nonnull
	@GET("/institution-resources/{institutionResourceId}")
	@AuthenticationRequired
	public ApiResponse institutionResource(@Nonnull @PathParameter("institutionResourceId") String institutionResourceIdentifier) {
		requireNonNull(institutionResourceIdentifier);

		InstitutionResource institutionResource = getInstitutionResourceService().findInstitutionResourceByIdentifier(institutionResourceIdentifier, getCurrentContext().getInstitutionId()).orElse(null);

		if (institutionResource == null)
			throw new NotFoundException();

		if (!institutionResource.getInstitutionId().equals(getCurrentContext().getInstitutionId()))
			throw new AuthorizationException();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("institutionResource", getInstitutionResourceApiResponseFactory().create(institutionResource));
		}});
	}

	@Nonnull
	@GET("/institution-resources")
	@AuthenticationRequired
	public ApiResponse institutionResources(@Nonnull @QueryParameter("institutionResourceGroupId") String institutionResourceGroupIdentifier) {
		requireNonNull(institutionResourceGroupIdentifier);

		InstitutionResourceGroup institutionResourceGroup = getInstitutionResourceService().findInstitutionResourceGroupByIdentifier(institutionResourceGroupIdentifier, getCurrentContext().getInstitutionId()).orElse(null);

		if (institutionResourceGroup == null)
			throw new NotFoundException();

		if (!institutionResourceGroup.getInstitutionId().equals(getCurrentContext().getInstitutionId()))
			throw new AuthorizationException();

		List<InstitutionResource> institutionResources = getInstitutionResourceService().findInstitutionResourcesByInstitutionResourceGroupId(institutionResourceGroup.getInstitutionResourceGroupId());

		return new ApiResponse(new HashMap<String, Object>() {{
			put("institutionResources", institutionResources.stream()
					.map(institutionResource -> getInstitutionResourceApiResponseFactory().create(institutionResource))
					.collect(Collectors.toList()));
		}});
	}

	@Nonnull
	protected InstitutionService getInstitutionService() {
		return this.institutionService;
	}

	@Nonnull
	protected InstitutionResourceService getInstitutionResourceService() {
		return this.institutionResourceService;
	}

	@Nonnull
	protected InstitutionResourceApiResponseFactory getInstitutionResourceApiResponseFactory() {
		return this.institutionResourceApiResponseFactory;
	}

	@Nonnull
	protected InstitutionResourceGroupApiResponseFactory getInstitutionResourceGroupApiResponseFactory() {
		return this.institutionResourceGroupApiResponseFactory;
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
