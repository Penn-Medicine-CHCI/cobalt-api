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
import com.cobaltplatform.api.model.api.response.ExternalGroupEventTypeApiResponse;
import com.cobaltplatform.api.model.api.response.ExternalGroupEventTypeApiResponse.ExternalGroupEventTypeApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.ExternalGroupEventType;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.service.GroupEventService;
import com.soklet.web.annotation.GET;
import com.soklet.web.annotation.PathParameter;
import com.soklet.web.annotation.QueryParameter;
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
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@Resource
@Singleton
@ThreadSafe
public class ExternalGroupEventTypeResource {
	@Nonnull
	private final GroupEventService groupEventService;
	@Nonnull
	private final ExternalGroupEventTypeApiResponseFactory externalGroupEventTypeApiResponseFactory;
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final Logger logger;

	@Inject
	public ExternalGroupEventTypeResource(@Nonnull GroupEventService groupEventService,
																				@Nonnull ExternalGroupEventTypeApiResponseFactory externalGroupEventTypeApiResponseFactory,
																				@Nonnull Provider<CurrentContext> currentContextProvider) {
		requireNonNull(groupEventService);
		requireNonNull(externalGroupEventTypeApiResponseFactory);
		requireNonNull(currentContextProvider);

		this.groupEventService = groupEventService;
		this.externalGroupEventTypeApiResponseFactory = externalGroupEventTypeApiResponseFactory;
		this.currentContextProvider = currentContextProvider;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	@GET("/external-group-event-types")
	@AuthenticationRequired
	public ApiResponse externalGroupEventTypes(@Nonnull @QueryParameter("class") Optional<String> urlName) {
		requireNonNull(urlName);

		Account account = getCurrentContext().getAccount().get();

		List<ExternalGroupEventType> externalGroupEventTypes = getGroupEventService().findExternalGroupEventTypesByInstitutionId(account.getInstitutionId());

		// Filter on URL name if available
		if (urlName.isPresent()) {
			String normalizedUrlName = trimToNull(urlName.get());

			if (normalizedUrlName != null)
				externalGroupEventTypes = externalGroupEventTypes.stream()
						.filter((externalGroupEventType -> externalGroupEventType.getUrlName().equals(normalizedUrlName)))
						.collect(Collectors.toList());
		}

		List<ExternalGroupEventTypeApiResponse> finalExternalGroupEventTypes = externalGroupEventTypes.stream()
				.map((externalGroupEventType) -> getExternalGroupEventTypeApiResponseFactory().create(externalGroupEventType))
				.collect(Collectors.toList());

		return new ApiResponse(new HashMap<String, Object>() {{
			put("externalGroupEventTypes", finalExternalGroupEventTypes);
		}});
	}

	@Nonnull
	@GET("/external-group-event-types/{externalGroupEventTypeId}")
	@AuthenticationRequired
	public ApiResponse externalGroupEventType(@Nonnull @PathParameter UUID externalGroupEventTypeId) {
		requireNonNull(externalGroupEventTypeId);

		Account account = getCurrentContext().getAccount().get();

		ExternalGroupEventType externalGroupEventType = getGroupEventService().findExternalGroupEventTypeById(externalGroupEventTypeId, account.getInstitutionId()).orElse(null);

		if (externalGroupEventType == null)
			throw new NotFoundException();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("externalGroupEventType", getExternalGroupEventTypeApiResponseFactory().create(externalGroupEventType));
		}});
	}

	@Nonnull
	protected GroupEventService getGroupEventService() {
		return groupEventService;
	}

	@Nonnull
	protected ExternalGroupEventTypeApiResponseFactory getExternalGroupEventTypeApiResponseFactory() {
		return externalGroupEventTypeApiResponseFactory;
	}

	@Nonnull
	protected CurrentContext getCurrentContext() {
		return currentContextProvider.get();
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}