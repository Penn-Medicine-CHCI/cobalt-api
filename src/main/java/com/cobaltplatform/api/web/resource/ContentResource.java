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
import com.cobaltplatform.api.model.api.response.ContentApiResponse;
import com.cobaltplatform.api.model.api.response.ContentApiResponse.ContentApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.Content;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.service.ContentService;
import com.cobaltplatform.api.web.request.RequestBodyParser;
import com.soklet.web.annotation.GET;
import com.soklet.web.annotation.PathParameter;
import com.soklet.web.annotation.QueryParameter;
import com.soklet.web.annotation.Resource;
import com.soklet.web.exception.NotFoundException;
import com.soklet.web.response.ApiResponse;

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


/**
 * @author Transmogrify LLC.
 */

@Resource
@Singleton
@ThreadSafe
public class ContentResource {

	@Nonnull
	private final ContentService contentService;
	@Nonnull
	private final RequestBodyParser requestBodyParser;
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final ContentApiResponseFactory contentApiResponseFactory;

	@Inject
	public ContentResource(@Nonnull ContentService contentService,
												 @Nonnull RequestBodyParser requestBodyParser,
												 @Nonnull Provider<CurrentContext> currentContextProvider,
												 @Nonnull ContentApiResponse.ContentApiResponseFactory contentApiResponseFactory) {
		this.contentService = contentService;
		this.requestBodyParser = requestBodyParser;
		this.currentContextProvider = currentContextProvider;
		this.contentApiResponseFactory = contentApiResponseFactory;
	}

	@GET("/content")
	@AuthenticationRequired
	public ApiResponse getContent(@Nonnull @QueryParameter Optional<String> format,
																@Nonnull @QueryParameter Optional<Integer> maxLengthMinutes,
																@Nonnull @QueryParameter Optional<String> searchQuery) {
		requireNonNull(format);
		requireNonNull(maxLengthMinutes);
		requireNonNull(searchQuery);

		Account account = getCurrentContext().getAccount().get();
		List<Content> contents = getContentService().findContentForAccount(account, format.orElse(null),
				maxLengthMinutes.orElse(null), searchQuery.orElse(null));

		List<ContentApiResponse> filteredContent = contents.stream().map(
				content -> getContentApiResponseFactory().create(content)).collect(Collectors.toList());

		List<ContentApiResponse> additionalContent =
				getContentService().findAdditionalContentForAccount(account, contents, format, maxLengthMinutes).stream().map(
						content -> getContentApiResponseFactory().create(content)).collect(Collectors.toList());

		return new ApiResponse(new HashMap<String, Object>() {{
			put("formats", getContentService().findContentTypeLabelsForAccount(account));
			put("content", filteredContent);
			put("additionalContent", additionalContent);
		}});
	}

	@GET("/content/{contentId}")
	@AuthenticationRequired
	public ApiResponse getContentById(@Nonnull @PathParameter UUID contentId) {
		requireNonNull(contentId);

		Account account = getCurrentContext().getAccount().get();
		Content content = getContentService().findContentById(account, contentId).orElse(null);

		if (content == null)
			throw new NotFoundException();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("content", getContentApiResponseFactory().create(content));
		}});
	}

	@Nonnull
	public ContentService getContentService() {
		return contentService;
	}

	@Nonnull
	public RequestBodyParser getRequestBodyParser() {
		return requestBodyParser;
	}

	@Nonnull
	public ContentApiResponseFactory getContentApiResponseFactory() {
		return contentApiResponseFactory;
	}

	@Nonnull
	public CurrentContext getCurrentContext() {
		return currentContextProvider.get();
	}
}
