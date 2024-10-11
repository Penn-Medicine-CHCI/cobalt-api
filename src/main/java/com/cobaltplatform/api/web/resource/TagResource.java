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
import com.cobaltplatform.api.model.api.response.TagApiResponse;
import com.cobaltplatform.api.model.api.response.TagApiResponse.TagApiResponseFactory;
import com.cobaltplatform.api.model.api.response.TagGroupApiResponse;
import com.cobaltplatform.api.model.api.response.TagGroupApiResponse.TagGroupApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.service.TagService;
import com.soklet.web.annotation.GET;
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
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Resource
@Singleton
@ThreadSafe
public class TagResource {
	@Nonnull
	private final TagService tagService;
	@Nonnull
	private final TagApiResponseFactory tagApiResponseFactory;
	@Nonnull
	private final TagGroupApiResponseFactory tagGroupApiResponseFactory;
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final Logger logger;

	@Inject
	public TagResource(@Nonnull TagService tagService,
										 @Nonnull TagApiResponseFactory tagApiResponseFactory,
										 @Nonnull TagGroupApiResponseFactory tagGroupApiResponseFactory,
										 @Nonnull Provider<CurrentContext> currentContextProvider) {
		requireNonNull(tagService);
		requireNonNull(tagApiResponseFactory);
		requireNonNull(tagGroupApiResponseFactory);
		requireNonNull(currentContextProvider);

		this.tagService = tagService;
		this.tagApiResponseFactory = tagApiResponseFactory;
		this.tagGroupApiResponseFactory = tagGroupApiResponseFactory;
		this.currentContextProvider = currentContextProvider;
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
	@GET("/tags")
	@AuthenticationRequired
	public ApiResponse tags() {
		Account account = getCurrentContext().getAccount().get();

		List<TagApiResponse> tags = getTagService().findTagsByInstitutionId(account.getInstitutionId()).stream()
				.map(tag -> getTagApiResponseFactory().create(tag))
				.collect(Collectors.toList());

		return new ApiResponse(new HashMap<String, Object>() {{
			put("tags", tags);
		}});
	}

	@Nonnull
	@GET("/tags/{tagIdentifier}")
	@AuthenticationRequired
	public ApiResponse tagByIdentifier(@Nonnull String tagIdentifier /* ID or URL name */) {
		requireNonNull(tagIdentifier);

		Account account = getCurrentContext().getAccount().get();
		String normalizedTagIdentifier = tagIdentifier.trim();

		TagApiResponse tag = getTagService().findTagsByInstitutionId(account.getInstitutionId()).stream()
				.filter(potentialTag -> potentialTag.getTagId().equals(normalizedTagIdentifier) || potentialTag.getUrlName().equals(normalizedTagIdentifier))
				.map(matchedTag -> getTagApiResponseFactory().create(matchedTag))
				.findAny().orElse(null);

		if (tag == null)
			throw new NotFoundException();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("tag", tag);
		}});
	}

	@Nonnull
	protected TagService getTagService() {
		return this.tagService;
	}

	@Nonnull
	protected TagApiResponseFactory getTagApiResponseFactory() {
		return this.tagApiResponseFactory;
	}

	@Nonnull
	protected TagGroupApiResponseFactory getTagGroupApiResponseFactory() {
		return this.tagGroupApiResponseFactory;
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
