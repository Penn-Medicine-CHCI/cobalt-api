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
import com.cobaltplatform.api.model.api.response.TagGroupApiResponse;
import com.cobaltplatform.api.model.api.response.TagGroupApiResponse.TagGroupApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.service.TagService;
import com.soklet.web.annotation.GET;
import com.soklet.web.annotation.Resource;
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
public class TagResource {
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final Logger logger;
	@Nonnull
	private final TagGroupApiResponseFactory tagGroupApiResponseFactory;
	@Nonnull
	private final TagService tagService;

	@Inject
	public TagResource(@Nonnull Provider<CurrentContext> currentContextProvider,
										 @Nonnull TagGroupApiResponseFactory tagGroupApiResponseFactory,
										 @Nonnull TagService tagService) {
		requireNonNull(currentContextProvider);
		requireNonNull(tagService);
		requireNonNull(tagGroupApiResponseFactory);

		this.tagGroupApiResponseFactory = tagGroupApiResponseFactory;
		this.currentContextProvider = currentContextProvider;
		this.tagService = tagService;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	@GET("/tags/tag-groups")
	@AuthenticationRequired
	public ApiResponse tagGroups() {
		Account account = getCurrentContext().getAccount().get();

		List<TagGroupApiResponse> tagGroups = getTagService().findTagGroupsByInstitutionId(account.getInstitutionId()).stream()
				.map(tagGroup -> getTagGroupApiResponseFactory().create(tagGroup))
				.collect(Collectors.toList());

		return new ApiResponse(new HashMap<String, Object>() {{
			put("tagGroups", tagGroups);
		}});
	}

	@Nonnull
	protected CurrentContext getCurrentContext() {
		return this.currentContextProvider.get();
	}

	@Nonnull
	protected Logger getLogger() {
		return this.logger;
	}

	@Nonnull
	protected TagService getTagService() { return this.tagService; }

	@Nonnull
	protected TagGroupApiResponseFactory getTagGroupApiResponseFactory() {
		return this.tagGroupApiResponseFactory;
	}

}
