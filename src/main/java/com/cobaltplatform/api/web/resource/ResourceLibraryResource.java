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
import com.cobaltplatform.api.integration.enterprise.EnterprisePlugin;
import com.cobaltplatform.api.integration.enterprise.EnterprisePluginProvider;
import com.cobaltplatform.api.model.api.response.ContentApiResponse;
import com.cobaltplatform.api.model.api.response.ContentApiResponse.ContentApiResponseFactory;
import com.cobaltplatform.api.model.api.response.TagApiResponse;
import com.cobaltplatform.api.model.api.response.TagApiResponse.TagApiResponseFactory;
import com.cobaltplatform.api.model.api.response.TagGroupApiResponse;
import com.cobaltplatform.api.model.api.response.TagGroupApiResponse.TagGroupApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.Content;
import com.cobaltplatform.api.model.db.Tag;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.service.AuthorizationService;
import com.cobaltplatform.api.service.ContentService;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
public class ResourceLibraryResource {
	@Nonnull
	private final ContentService contentService;
	@Nonnull
	private final TagService tagService;
	@Nonnull
	private final AuthorizationService authorizationService;
	@Nonnull
	private final EnterprisePluginProvider enterprisePluginProvider;
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final ContentApiResponseFactory contentApiResponseFactory;
	@Nonnull
	private final TagApiResponseFactory tagApiResponseFactory;
	@Nonnull
	private final TagGroupApiResponseFactory tagGroupApiResponseFactory;
	@Nonnull
	private final Logger logger;

	@Inject
	public ResourceLibraryResource(@Nonnull ContentService contentService,
																 @Nonnull TagService tagService,
																 @Nonnull AuthorizationService authorizationService,
																 @Nonnull EnterprisePluginProvider enterprisePluginProvider,
																 @Nonnull Provider<CurrentContext> currentContextProvider,
																 @Nonnull ContentApiResponseFactory contentApiResponseFactory,
																 @Nonnull TagApiResponseFactory tagApiResponseFactory,
																 @Nonnull TagGroupApiResponseFactory tagGroupApiResponseFactory) {
		requireNonNull(contentService);
		requireNonNull(tagService);
		requireNonNull(authorizationService);
		requireNonNull(enterprisePluginProvider);
		requireNonNull(currentContextProvider);
		requireNonNull(contentApiResponseFactory);
		requireNonNull(tagApiResponseFactory);
		requireNonNull(tagGroupApiResponseFactory);

		this.contentService = contentService;
		this.tagService = tagService;
		this.authorizationService = authorizationService;
		this.enterprisePluginProvider = enterprisePluginProvider;
		this.currentContextProvider = currentContextProvider;
		this.contentApiResponseFactory = contentApiResponseFactory;
		this.tagApiResponseFactory = tagApiResponseFactory;
		this.tagGroupApiResponseFactory = tagGroupApiResponseFactory;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	@GET("/resource-library")
	@AuthenticationRequired
	public ApiResponse resourceLibrary() {
		CurrentContext currentContext = getCurrentContext();
		Account account = currentContext.getAccount().get();
		EnterprisePlugin enterprisePlugin = getEnterprisePluginProvider().enterprisePluginForInstitutionId(account.getInstitutionId());

		// Delegate content recommendations to enterprise plugin
		List<Content> contents = enterprisePlugin.recommendedContentForAccountId(account.getAccountId());

		// Pick out tags in the content
		Set<String> tagIds = new HashSet<>();
		List<TagApiResponse> tags = new ArrayList<>();

		for (Content content : contents) {
			for (Tag tag : content.getTags()) {
				if (tagIds.contains(tag.getTagId()))
					continue;

				tagIds.add(tag.getTagId());
				tags.add(getTagApiResponseFactory().create(tag));
			}
		}

		List<TagGroupApiResponse> tagGroups = getTagService().findTagGroupsByInstitutionId(currentContext.getInstitutionId()).stream()
				.map(tagGroup -> getTagGroupApiResponseFactory().create(tagGroup))
				.collect(Collectors.toList());

		// Keep track of content IDs so we don't show the same content more than once
		Set<UUID> contentIds = new HashSet<>(contents.size());

		// Group content by tag group
		Map<String, List<ContentApiResponse>> contentsByTagGroupId = new HashMap<>(tagGroups.size());

		for (Content content : contents) {
			if (contentIds.contains(content.getContentId()))
				continue;

			contentIds.add(content.getContentId());

			String tagGroupId = content.getTags().stream()
					.map(tag -> tag.getTagGroupId())
					.findFirst()
					.orElse(null);

			if (tagGroupId != null) {
				List<ContentApiResponse> tagGroupContents = contentsByTagGroupId.get(tagGroupId);

				if (tagGroupContents == null) {
					tagGroupContents = new ArrayList<>();
					contentsByTagGroupId.put(tagGroupId, tagGroupContents);
				}

				tagGroupContents.add(getContentApiResponseFactory().create(content));
			}
		}

		return new ApiResponse(new HashMap<String, Object>() {{
			put("contentsByTagGroupId", contentsByTagGroupId);
			put("tagGroups", tagGroups);
			put("tags", tags);
		}});
	}

//	@Nonnull
//	@GET("/resource-library/search")
//	@AuthenticationRequired
//	public ApiResponse searchResourceLibrary() {
//		// TODO
//		return new ApiResponse();
//	}

//	@Nonnull
//	@GET("/resource-library/search")
//	@AuthenticationRequired
//	public ApiResponse searchResourceLibrary() {
//		// TODO
//		return new ApiResponse();
//	}

	@Nonnull
	protected ContentService getContentService() {
		return this.contentService;
	}

	@Nonnull
	protected TagService getTagService() {
		return this.tagService;
	}

	@Nonnull
	protected AuthorizationService getAuthorizationService() {
		return this.authorizationService;
	}

	@Nonnull
	protected EnterprisePluginProvider getEnterprisePluginProvider() {
		return this.enterprisePluginProvider;
	}

	@Nonnull
	protected ContentApiResponseFactory getContentApiResponseFactory() {
		return this.contentApiResponseFactory;
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
