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
import com.cobaltplatform.api.model.api.response.ContentSnippetApiResponse.ContentSnippetApiResponseFactory;
import com.cobaltplatform.api.model.db.ContentSnippet;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.service.ContentSnippetService;
import com.soklet.web.annotation.GET;
import com.soklet.web.annotation.QueryParameter;
import com.soklet.web.annotation.Resource;
import com.soklet.web.response.ApiResponse;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Resource
@Singleton
@ThreadSafe
public class ContentSnippetResource {
	@Nonnull
	private final ContentSnippetService contentSnippetService;
	@Nonnull
	private final ContentSnippetApiResponseFactory contentSnippetApiResponseFactory;
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;

	@Inject
	public ContentSnippetResource(@Nonnull ContentSnippetService contentSnippetService,
																@Nonnull ContentSnippetApiResponseFactory contentSnippetApiResponseFactory,
																@Nonnull Provider<CurrentContext> currentContextProvider) {
		requireNonNull(contentSnippetService);
		requireNonNull(contentSnippetApiResponseFactory);
		requireNonNull(currentContextProvider);

		this.contentSnippetService = contentSnippetService;
		this.contentSnippetApiResponseFactory = contentSnippetApiResponseFactory;
		this.currentContextProvider = currentContextProvider;
	}

	@Nonnull
	@GET("/content-snippets")
	@AuthenticationRequired
	public ApiResponse getContentSnippets(@Nonnull @QueryParameter("contentSnippetKey") Optional<List<String>> contentSnippetKeys) {
		requireNonNull(contentSnippetKeys);

		List<ContentSnippet> contentSnippets = getContentSnippetService().findContentSnippetsByInstitutionIdAndKeys(
				getCurrentContext().getInstitutionId(), contentSnippetKeys.orElse(List.of()));

		return new ApiResponse(Map.of(
				"contentSnippets", contentSnippets.stream()
						.map(contentSnippet -> getContentSnippetApiResponseFactory().create(contentSnippet))
						.collect(Collectors.toUnmodifiableList())
		));
	}

	@Nonnull
	protected ContentSnippetService getContentSnippetService() {
		return this.contentSnippetService;
	}

	@Nonnull
	protected ContentSnippetApiResponseFactory getContentSnippetApiResponseFactory() {
		return this.contentSnippetApiResponseFactory;
	}

	@Nonnull
	protected CurrentContext getCurrentContext() {
		return this.currentContextProvider.get();
	}
}
