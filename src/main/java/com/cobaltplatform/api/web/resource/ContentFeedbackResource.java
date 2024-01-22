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
import com.cobaltplatform.api.model.api.request.CreateContentFeedbackRequest;
import com.cobaltplatform.api.model.api.request.UpdateContentFeedbackRequest;
import com.cobaltplatform.api.model.api.response.ContentFeedbackApiResponse.ContentFeedbackApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.ContentFeedback;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.service.ContentService;
import com.cobaltplatform.api.web.request.RequestBodyParser;
import com.soklet.web.annotation.POST;
import com.soklet.web.annotation.PUT;
import com.soklet.web.annotation.PathParameter;
import com.soklet.web.annotation.RequestBody;
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
import java.util.Map;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Resource
@Singleton
@ThreadSafe
public class ContentFeedbackResource {
	@Nonnull
	private final ContentService contentService;
	@Nonnull
	private final ContentFeedbackApiResponseFactory contentFeedbackApiResponseFactory;
	@Nonnull
	private final RequestBodyParser requestBodyParser;
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final Logger logger;

	@Inject
	public ContentFeedbackResource(@Nonnull ContentService contentService,
																 @Nonnull ContentFeedbackApiResponseFactory contentFeedbackApiResponseFactory,
																 @Nonnull RequestBodyParser requestBodyParser,
																 @Nonnull Provider<CurrentContext> currentContextProvider) {
		requireNonNull(contentService);
		requireNonNull(contentFeedbackApiResponseFactory);
		requireNonNull(requestBodyParser);
		requireNonNull(currentContextProvider);

		this.contentService = contentService;
		this.contentFeedbackApiResponseFactory = contentFeedbackApiResponseFactory;
		this.requestBodyParser = requestBodyParser;
		this.currentContextProvider = currentContextProvider;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	@POST("/content-feedbacks")
	@AuthenticationRequired
	public ApiResponse createContentFeedback(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		Account account = getCurrentContext().getAccount().get();

		CreateContentFeedbackRequest request = getRequestBodyParser().parse(requestBody, CreateContentFeedbackRequest.class);
		request.setAccountId(account.getAccountId());

		UUID contentFeedbackId = getContentService().createContentFeedback(request);
		ContentFeedback contentFeedback = getContentService().findContentFeedbackById(contentFeedbackId).get();

		return new ApiResponse(201, Map.of("contentFeedback", getContentFeedbackApiResponseFactory().create(contentFeedback)));
	}

	@Nonnull
	@PUT("/content-feedbacks/{contentFeedbackId}")
	@AuthenticationRequired
	public Object updateContentFeedback(@Nonnull @PathParameter UUID contentFeedbackId,
																			@Nonnull @RequestBody String requestBody) {
		requireNonNull(contentFeedbackId);
		requireNonNull(requestBody);

		ContentFeedback contentFeedback = getContentService().findContentFeedbackById(contentFeedbackId).orElse(null);

		if (contentFeedback == null)
			throw new NotFoundException();

		Account account = getCurrentContext().getAccount().get();

		// For now, you can only update feedback you have created yourself
		if (!contentFeedback.getAccountId().equals(account.getAccountId()))
			throw new AuthorizationException();

		UpdateContentFeedbackRequest request = getRequestBodyParser().parse(requestBody, UpdateContentFeedbackRequest.class);
		request.setContentFeedbackId(contentFeedbackId);
		request.setAccountId(account.getAccountId());

		getContentService().updateContentFeedback(request);
		ContentFeedback updatedContentFeedback = getContentService().findContentFeedbackById(contentFeedbackId).get();

		return new ApiResponse(Map.of("contentFeedback", getContentFeedbackApiResponseFactory().create(updatedContentFeedback)));
	}

	@Nonnull
	protected ContentService getContentService() {
		return this.contentService;
	}

	@Nonnull
	protected ContentFeedbackApiResponseFactory getContentFeedbackApiResponseFactory() {
		return this.contentFeedbackApiResponseFactory;
	}

	@Nonnull
	protected RequestBodyParser getRequestBodyParser() {
		return this.requestBodyParser;
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
