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
import com.cobaltplatform.api.service.TagService;
import com.cobaltplatform.api.util.ValidationUtility;
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
import java.net.URL;
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
	private final TagService tagService;
	@Nonnull
	private final RequestBodyParser requestBodyParser;
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final ContentApiResponseFactory contentApiResponseFactory;

	@Inject
	public ContentResource(@Nonnull ContentService contentService,
												 @Nonnull TagService tagService,
												 @Nonnull RequestBodyParser requestBodyParser,
												 @Nonnull Provider<CurrentContext> currentContextProvider,
												 @Nonnull ContentApiResponse.ContentApiResponseFactory contentApiResponseFactory) {
		requireNonNull(contentService);
		requireNonNull(tagService);
		requireNonNull(requestBodyParser);
		requireNonNull(currentContextProvider);
		requireNonNull(contentApiResponseFactory);

		this.contentService = contentService;
		this.tagService = tagService;
		this.requestBodyParser = requestBodyParser;
		this.currentContextProvider = currentContextProvider;
		this.contentApiResponseFactory = contentApiResponseFactory;
	}

	@GET("/content/{contentId}")
	@AuthenticationRequired
	public ApiResponse getContentById(@Nonnull @PathParameter UUID contentId) {
		requireNonNull(contentId);

		Account account = getCurrentContext().getAccount().get();
		Content content = getContentService().findContentById(account, contentId).orElse(null);

		if (content == null)
			throw new NotFoundException();

		// Temporary hack: if webapp is viewing the detail page when this call is made,
		// massage newlines into HTML.
		// TODO: we are moving to a rich text editor that will eliminate the need for this.
		// But for the moment, simplest to just override this one spot
		boolean webappViewingPatientDetailPage = false;
		URL webappCurrentUrl = getCurrentContext().getWebappCurrentUrl().orElse(null);

		if (webappCurrentUrl != null) {
			// e.g. /resource-library/0f33f0b4-e022-4fde-80ac-12151afa4f1c
			String[] components = webappCurrentUrl.getPath().split("/");

			if (components.length == 3
					&& components[0].length() == 0
					&& components[1].equals("resource-library")
					&& ValidationUtility.isValidUUID(components[2]))
				webappViewingPatientDetailPage = true;
		}

		if (webappViewingPatientDetailPage)
			content.setDescription(content.getDescription().replace("\n", "<br/>"));

		return new ApiResponse(new HashMap<String, Object>() {{
			put("content", getContentApiResponseFactory().create(content));
		}});
	}

	@Nonnull
	protected ContentService getContentService() {
		return this.contentService;
	}

	@Nonnull
	protected TagService getTagService() {
		return this.tagService;
	}

	@Nonnull
	protected RequestBodyParser getRequestBodyParser() {
		return this.requestBodyParser;
	}

	@Nonnull
	protected ContentApiResponseFactory getContentApiResponseFactory() {
		return this.contentApiResponseFactory;
	}

	@Nonnull
	protected CurrentContext getCurrentContext() {
		return this.currentContextProvider.get();
	}
}
