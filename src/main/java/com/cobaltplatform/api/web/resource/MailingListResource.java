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
import com.cobaltplatform.api.model.api.request.CreateMailingListEntryRequest;
import com.cobaltplatform.api.model.api.response.MailingListEntryApiResponse.MailingListEntryApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.MailingList;
import com.cobaltplatform.api.model.db.MailingListEntry;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.service.MailingListService;
import com.cobaltplatform.api.web.request.RequestBodyParser;
import com.soklet.web.annotation.POST;
import com.soklet.web.annotation.PathParameter;
import com.soklet.web.annotation.RequestBody;
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
import java.util.Map;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Resource
@Singleton
@ThreadSafe
public class MailingListResource {
	@Nonnull
	private final MailingListService mailingListService;
	@Nonnull
	private final RequestBodyParser requestBodyParser;
	@Nonnull
	private final MailingListEntryApiResponseFactory mailingListEntryApiResponseFactory;
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final Logger logger;

	@Inject
	public MailingListResource(@Nonnull MailingListService mailingListService,
														 @Nonnull RequestBodyParser requestBodyParser,
														 @Nonnull MailingListEntryApiResponseFactory mailingListEntryApiResponseFactory,
														 @Nonnull Provider<CurrentContext> currentContextProvider) {
		requireNonNull(mailingListService);
		requireNonNull(requestBodyParser);
		requireNonNull(mailingListEntryApiResponseFactory);
		requireNonNull(currentContextProvider);

		this.mailingListService = mailingListService;
		this.requestBodyParser = requestBodyParser;
		this.mailingListEntryApiResponseFactory = mailingListEntryApiResponseFactory;
		this.currentContextProvider = currentContextProvider;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	@AuthenticationRequired
	@POST("/mailing-lists/{mailingListId}/entry")
	public ApiResponse createMailingListEntry(@Nonnull @PathParameter UUID mailingListId,
																						@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		MailingList mailingList = getMailingListService().findMailingListById(mailingListId).orElse(null);

		if (mailingList == null)
			throw new NotFoundException();

		Account account = getCurrentContext().getAccount().get();

		CreateMailingListEntryRequest request = getRequestBodyParser().parse(requestBody, CreateMailingListEntryRequest.class);
		request.setMailingListId(mailingListId);
		request.setCreatedByAccountId(account.getAccountId());

		// If request doesn't specify an account, set it explicitly
		if (request.getAccountId() == null)
			request.setAccountId(account.getAccountId());

		UUID mailingListEntryId = getMailingListService().createMailingListEntry(request);
		MailingListEntry mailingListEntry = getMailingListService().findMailingListEntryById(mailingListEntryId).get();

		return new ApiResponse(Map.of(
				"mailingListEntry", getMailingListEntryApiResponseFactory().create(mailingListEntry)
		));
	}

	@Nonnull
	protected MailingListService getMailingListService() {
		return this.mailingListService;
	}

	@Nonnull
	protected RequestBodyParser getRequestBodyParser() {
		return this.requestBodyParser;
	}

	@Nonnull
	protected MailingListEntryApiResponseFactory getMailingListEntryApiResponseFactory() {
		return this.mailingListEntryApiResponseFactory;
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
