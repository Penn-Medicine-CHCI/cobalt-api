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
import com.cobaltplatform.api.model.api.request.UpdateMailingListEntryStatusRequest;
import com.cobaltplatform.api.model.api.response.MailingListEntryApiResponse.MailingListEntryApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PageApiResponse.PageApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.FootprintEventGroupType.FootprintEventGroupTypeId;
import com.cobaltplatform.api.model.db.MailingList;
import com.cobaltplatform.api.model.db.MailingListEntry;
import com.cobaltplatform.api.model.db.MailingListEntryStatus.MailingListEntryStatusId;
import com.cobaltplatform.api.model.db.Page;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.service.AuthorizationService;
import com.cobaltplatform.api.service.MailingListService;
import com.cobaltplatform.api.service.PageService;
import com.cobaltplatform.api.service.SystemService;
import com.cobaltplatform.api.web.request.RequestBodyParser;
import com.soklet.web.annotation.GET;
import com.soklet.web.annotation.POST;
import com.soklet.web.annotation.PUT;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
	private final PageService pageService;
	@Nonnull
	private final SystemService systemService;
	@Nonnull
	private final AuthorizationService authorizationService;
	@Nonnull
	private final RequestBodyParser requestBodyParser;
	@Nonnull
	private final MailingListEntryApiResponseFactory mailingListEntryApiResponseFactory;
	@Nonnull
	private final PageApiResponseFactory pageApiResponseFactory;
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final Logger logger;

	@Inject
	public MailingListResource(@Nonnull MailingListService mailingListService,
														 @Nonnull PageService pageService,
														 @Nonnull SystemService systemService,
														 @Nonnull AuthorizationService authorizationService,
														 @Nonnull RequestBodyParser requestBodyParser,
														 @Nonnull MailingListEntryApiResponseFactory mailingListEntryApiResponseFactory,
														 @Nonnull PageApiResponseFactory pageApiResponseFactory,
														 @Nonnull Provider<CurrentContext> currentContextProvider) {
		requireNonNull(mailingListService);
		requireNonNull(pageService);
		requireNonNull(systemService);
		requireNonNull(authorizationService);
		requireNonNull(requestBodyParser);
		requireNonNull(mailingListEntryApiResponseFactory);
		requireNonNull(pageApiResponseFactory);
		requireNonNull(currentContextProvider);

		this.mailingListService = mailingListService;
		this.pageService = pageService;
		this.systemService = systemService;
		this.authorizationService = authorizationService;
		this.requestBodyParser = requestBodyParser;
		this.mailingListEntryApiResponseFactory = mailingListEntryApiResponseFactory;
		this.pageApiResponseFactory = pageApiResponseFactory;
		this.currentContextProvider = currentContextProvider;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	@AuthenticationRequired
	@POST("/mailing-list-entries")
	public ApiResponse createMailingListEntry(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		getSystemService().applyFootprintEventGroupToCurrentTransaction(FootprintEventGroupTypeId.MAILING_LIST_ENTRY_CREATE);

		Account account = getCurrentContext().getAccount().get();

		CreateMailingListEntryRequest request = getRequestBodyParser().parse(requestBody, CreateMailingListEntryRequest.class);
		request.setCreatedByAccountId(account.getAccountId());

		// If request doesn't specify an account, set it explicitly
		if (request.getAccountId() == null)
			request.setAccountId(account.getAccountId());

		MailingList mailingList = getMailingListService().findMailingListById(request.getMailingListId()).orElse(null);

		if (mailingList == null)
			throw new NotFoundException();

		UUID mailingListEntryId = getMailingListService().createMailingListEntry(request);
		MailingListEntry mailingListEntry = getMailingListService().findMailingListEntryById(mailingListEntryId).get();

		return new ApiResponse(Map.of(
				"mailingListEntry", getMailingListEntryApiResponseFactory().create(mailingListEntry)
		));
	}

	// Note: no @AuthenticationRequired because this can be called from a signed-out experience, e.g. clicking "unsubscribe" in an email
	@Nonnull
	@GET("/mailing-list-entries/{mailingListEntryId}")
	public ApiResponse mailingListEntry(@Nonnull @PathParameter UUID mailingListEntryId) {
		requireNonNull(mailingListEntryId);

		MailingListEntry mailingListEntry = getMailingListService().findMailingListEntryById(mailingListEntryId).orElse(null);

		if (mailingListEntry == null)
			throw new NotFoundException();

		List<Page> pages = getPageService().findPagesByMailingListEntryId(mailingListEntryId);

		return new ApiResponse(Map.of(
				"mailingListEntry", getMailingListEntryApiResponseFactory().create(mailingListEntry),
				"pages", pages.stream()
						.map(page -> getPageApiResponseFactory().create(page, false))
						.collect(Collectors.toUnmodifiableList())
		));
	}

	// Note: no @AuthenticationRequired because this can be called from a signed-out experience, e.g. clicking "unsubscribe" in an email
	@Nonnull
	@PUT("/mailing-list-entries/{mailingListEntryId}/unsubscribe")
	public ApiResponse updateMailingListEntryStatus(@Nonnull @PathParameter UUID mailingListEntryId) {
		requireNonNull(mailingListEntryId);

		getSystemService().applyFootprintEventGroupToCurrentTransaction(FootprintEventGroupTypeId.MAILING_LIST_ENTRY_UPDATE);

		MailingListEntry mailingListEntry = getMailingListService().findMailingListEntryById(mailingListEntryId).orElse(null);

		if (mailingListEntry == null)
			throw new NotFoundException();

		UpdateMailingListEntryStatusRequest request = new UpdateMailingListEntryStatusRequest();
		request.setMailingListEntryId(mailingListEntryId);
		request.setMailingListEntryStatusId(MailingListEntryStatusId.UNSUBSCRIBED);

		getMailingListService().updateMailingListEntryStatus(request);

		MailingListEntry updatedMailingListEntry = getMailingListService().findMailingListEntryById(mailingListEntryId).get();

		return new ApiResponse(Map.of(
				"mailingListEntry", getMailingListEntryApiResponseFactory().create(updatedMailingListEntry)
		));
	}

	@Nonnull
	protected MailingListService getMailingListService() {
		return this.mailingListService;
	}

	@Nonnull
	protected PageService getPageService() {
		return this.pageService;
	}

	@Nonnull
	protected SystemService getSystemService() {
		return this.systemService;
	}

	@Nonnull
	protected AuthorizationService getAuthorizationService() {
		return this.authorizationService;
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
	protected PageApiResponseFactory getPageApiResponseFactory() {
		return this.pageApiResponseFactory;
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
