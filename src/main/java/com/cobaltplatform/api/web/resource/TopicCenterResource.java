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
import com.cobaltplatform.api.model.api.response.TopicCenterApiResponse.TopicCenterApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.TopicCenter;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.service.AuthorizationService;
import com.cobaltplatform.api.service.TopicCenterService;
import com.cobaltplatform.api.util.ValidationUtility;
import com.soklet.web.annotation.GET;
import com.soklet.web.annotation.PathParameter;
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
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Resource
@Singleton
@ThreadSafe
public class TopicCenterResource {
	@Nonnull
	private final TopicCenterService topicCenterService;
	@Nonnull
	private final AuthorizationService authorizationService;
	@Nonnull
	private final TopicCenterApiResponseFactory topicCenterApiResponseFactory;
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final Logger logger;

	@Inject
	public TopicCenterResource(@Nonnull TopicCenterService topicCenterService,
														 @Nonnull AuthorizationService authorizationService,
														 @Nonnull TopicCenterApiResponseFactory topicCenterApiResponseFactory,
														 @Nonnull Provider<CurrentContext> currentContextProvider) {
		requireNonNull(topicCenterService);
		requireNonNull(authorizationService);
		requireNonNull(topicCenterApiResponseFactory);
		requireNonNull(currentContextProvider);

		this.topicCenterService = topicCenterService;
		this.authorizationService = authorizationService;
		this.topicCenterApiResponseFactory = topicCenterApiResponseFactory;
		this.currentContextProvider = currentContextProvider;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	@GET("/topic-centers/{topicCenterIdentifier}")
	@AuthenticationRequired
	public ApiResponse topicCenter(@Nonnull @PathParameter String topicCenterIdentifier) {
		requireNonNull(topicCenterIdentifier);

		Account account = getCurrentContext().getAccount().get();
		// If the identifier looks like a UUID, fetch by UUID.
		// If the identifier is not a UUID, fetch by combination of URL name and current account institution
		TopicCenter topicCenter = ValidationUtility.isValidUUID(topicCenterIdentifier)
				? getTopicCenterService().findTopicCenterById(UUID.fromString(topicCenterIdentifier)).orElse(null)
				: getTopicCenterService().findTopicCenterByInstitutionIdAndUrlName(account.getInstitutionId(), topicCenterIdentifier).orElse(null);

		if (topicCenter == null)
			throw new NotFoundException();

		if (!getAuthorizationService().canViewTopicCenter(topicCenter, account))
			throw new AuthorizationException();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("topicCenter", getTopicCenterApiResponseFactory().create(topicCenter));
		}});
	}

	@Nonnull
	protected TopicCenterService getTopicCenterService() {
		return this.topicCenterService;
	}

	@Nonnull
	protected AuthorizationService getAuthorizationService() {
		return this.authorizationService;
	}

	@Nonnull
	protected TopicCenterApiResponseFactory getTopicCenterApiResponseFactory() {
		return this.topicCenterApiResponseFactory;
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
