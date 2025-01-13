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
import com.cobaltplatform.api.model.api.request.CreatePageRequest;
import com.cobaltplatform.api.model.api.request.CreatePageRowContentRequest;
import com.cobaltplatform.api.model.api.request.CreatePageRowGroupSessionRequest;
import com.cobaltplatform.api.model.api.request.CreatePageRowImageRequest;
import com.cobaltplatform.api.model.api.request.CreatePageRowRequest;
import com.cobaltplatform.api.model.api.request.CreatePageSectionRequest;
import com.cobaltplatform.api.model.api.response.PageApiResponse.PageApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PageRowApiResponse.PageRowApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PageRowContentApiResponse.PageRowContentApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PageRowGroupSessionApiResponse.PageRowGroupSessionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PageRowImageApiResponse.PageRowImageApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PageSectionApiResponse.PageSectionApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.Page;
import com.cobaltplatform.api.model.db.PageRow;
import com.cobaltplatform.api.model.db.PageRowContent;
import com.cobaltplatform.api.model.db.PageRowGroupSession;
import com.cobaltplatform.api.model.db.PageRowImage;
import com.cobaltplatform.api.model.db.PageSection;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.service.PageService;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.web.request.RequestBodyParser;
import com.soklet.web.annotation.POST;
import com.soklet.web.annotation.PathParameter;
import com.soklet.web.annotation.RequestBody;
import com.soklet.web.annotation.Resource;
import com.soklet.web.exception.NotFoundException;
import com.soklet.web.response.ApiResponse;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify LLC.
 */
@Singleton
@Resource
@ThreadSafe
public class PageResource {
	@Nonnull
	private final RequestBodyParser requestBodyParser;
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final PageApiResponseFactory pageApiResponseFactory;
	@Nonnull
	private final PageSectionApiResponseFactory pageSectionApiResponseFactory;
	@Nonnull
	private final PageRowContentApiResponseFactory pageRowContentApiResponseFactory;
	@Nonnull
	private final PageRowImageApiResponseFactory pageRowImageApiResponseFactory;
	@Nonnull
	private final PageRowGroupSessionApiResponseFactory pageRowGroupSessionApiResponseFactory;

	@Nonnull
	private final PageRowApiResponseFactory pageRowApiResponseFactory;

	@Nonnull
	private final PageService pageService;
	@Nonnull
	private final Formatter formatter;

	@Inject
	public PageResource(@Nonnull RequestBodyParser requestBodyParser,
											@Nonnull Provider<CurrentContext> currentContextProvider,
											@Nonnull PageService pageService,
											@Nonnull PageApiResponseFactory pageApiResponseFactory,
											@Nonnull PageSectionApiResponseFactory pageSectionApiResponseFactory,
											@Nonnull PageRowApiResponseFactory pageRowApiResponseFactory,
											@Nonnull PageRowContentApiResponseFactory pageRowContentApiResponseFactory,
											@Nonnull PageRowImageApiResponseFactory pageRowImageApiResponseFactory,
											@Nonnull PageRowGroupSessionApiResponseFactory pageRowGroupSessionApiResponseFactory,
											@Nonnull Formatter formatter) {

		requireNonNull(requestBodyParser);
		requireNonNull(currentContextProvider);
		requireNonNull(pageService);
		requireNonNull(pageApiResponseFactory);
		requireNonNull(pageSectionApiResponseFactory);
		requireNonNull(pageRowApiResponseFactory);
		requireNonNull(pageRowContentApiResponseFactory);
		requireNonNull(pageRowImageApiResponseFactory);
		requireNonNull(pageRowGroupSessionApiResponseFactory);
		requireNonNull(formatter);

		this.requestBodyParser = requestBodyParser;
		this.currentContextProvider = currentContextProvider;
		this.pageService = pageService;
		this.pageApiResponseFactory = pageApiResponseFactory;
		this.pageSectionApiResponseFactory = pageSectionApiResponseFactory;
		this.pageRowApiResponseFactory = pageRowApiResponseFactory;
		this.pageRowContentApiResponseFactory = pageRowContentApiResponseFactory;
		this.pageRowImageApiResponseFactory = pageRowImageApiResponseFactory;
		this.pageRowGroupSessionApiResponseFactory = pageRowGroupSessionApiResponseFactory;
		this.formatter = formatter;
	}

	@POST("/page")
	@AuthenticationRequired
	public ApiResponse createPage(@Nonnull @RequestBody String body) {
		CreatePageRequest request = getRequestBodyParser().parse(body, CreatePageRequest.class);
		Account account = getCurrentContext().getAccount().get();

		request.setCreatedByAccountId(account.getAccountId());
		UUID pageId = getPageService().createPage(request);
		Optional<Page> page = getPageService().findPageById(pageId);

		if (!page.isPresent())
			throw new NotFoundException();
		return new ApiResponse(new HashMap<String, Object>() {{
			put("page", getPageApiResponseFactory().create(page.get()));
		}});
	}

	@POST("/page/{pageId}/section")
	@AuthenticationRequired
	public ApiResponse createPageSection(@Nonnull @PathParameter("pageId") UUID pageId,
																			 @Nonnull @RequestBody String body) {
		requireNonNull(pageId);

		CreatePageSectionRequest request = getRequestBodyParser().parse(body, CreatePageSectionRequest.class);
		Account account = getCurrentContext().getAccount().get();

		request.setCreatedByAccountId(account.getAccountId());
		request.setPageId(pageId);
		UUID pageSectionId = getPageService().createPageSection(request);
		Optional<PageSection> pageSection = getPageService().findPageSectionById(pageSectionId);

		if (!pageSection.isPresent())
			throw new NotFoundException();
		return new ApiResponse(new HashMap<String, Object>() {{
			put("pageSection", getPageSectionApiResponseFactory().create(pageSection.get()));
		}});
	}

	@POST("/page/section/{pageSectionId}/row")
	@AuthenticationRequired
	public ApiResponse createPageRow(@Nonnull @PathParameter("pageSectionId") UUID pageSectionId,
																	 @Nonnull @RequestBody String body) {
		CreatePageRowRequest request = getRequestBodyParser().parse(body, CreatePageRowRequest.class);
		Account account = getCurrentContext().getAccount().get();

		request.setCreatedByAccountId(account.getAccountId());
		request.setPageSectionId(pageSectionId);
		UUID pageRowId = getPageService().createPageRow(request);
		Optional<PageRow> pageRow = getPageService().findPageRowById(pageRowId);

		if (!pageRow.isPresent())
			throw new NotFoundException();
		return new ApiResponse(new HashMap<String, Object>() {{
			put("pageRow", getPageRowApiResponseFactory().create(pageRow.get()));
		}});
	}

	@POST("/page/row/{pageRowId}/content")
	@AuthenticationRequired
	public ApiResponse createPageRowContent(@Nonnull @PathParameter("pageRowId") UUID pageRowId,
																					@Nonnull @RequestBody String body) {
		CreatePageRowContentRequest request = getRequestBodyParser().parse(body, CreatePageRowContentRequest.class);
		Account account = getCurrentContext().getAccount().get();

		request.setCreatedByAccountId(account.getAccountId());
		request.setPageRowId(pageRowId);

		UUID pageRowContentId = getPageService().createPageRowContent(request);

		Optional<PageRowContent> pageRowContent = getPageService().findPageRowContentById(pageRowContentId);

		if (!pageRowContent.isPresent())
			throw new NotFoundException();
		return new ApiResponse(new HashMap<String, Object>() {{
			put("pageRowContent", getPageRowContentApiResponseFactory().create(pageRowContent.get()));
		}});
	}

	@POST("/page/row/{pageRowId}/group-session")
	@AuthenticationRequired
	public ApiResponse createPageRowGroupSession(@Nonnull @PathParameter("pageRowId") UUID pageRowId,
																							 @Nonnull @RequestBody String body) {
		CreatePageRowGroupSessionRequest request = getRequestBodyParser().parse(body, CreatePageRowGroupSessionRequest.class);
		Account account = getCurrentContext().getAccount().get();

		request.setCreatedByAccountId(account.getAccountId());
		request.setPageRowId(pageRowId);

		UUID pageRowGroupSessionId = getPageService().createPageRowGroupSession(request);

		Optional<PageRowGroupSession> pageRowGroupSession = getPageService().findPageRowGroupSessionById(pageRowGroupSessionId);

		if (!pageRowGroupSession.isPresent())
			throw new NotFoundException();
		return new ApiResponse(new HashMap<String, Object>() {{
			put("pageRowGroupSession", getPageRowGroupSessionApiResponseFactory().create(pageRowGroupSession.get()));
		}});
	}

	@POST("/page/row/{pageRowId}/image")
	@AuthenticationRequired
	public ApiResponse createPageRowImage(@Nonnull @PathParameter("pageRowId") UUID pageRowId,
																				@Nonnull @RequestBody String body) {
		CreatePageRowImageRequest request = getRequestBodyParser().parse(body, CreatePageRowImageRequest.class);
		Account account = getCurrentContext().getAccount().get();

		request.setCreatedByAccountId(account.getAccountId());
		request.setPageRowId(pageRowId);

		UUID pageRowImageId = getPageService().createPageRowImage(request);

		Optional<PageRowImage> pageRowImage = getPageService().findPageRowImageById(pageRowImageId);

		if (!pageRowImage.isPresent())
			throw new NotFoundException();
		return new ApiResponse(new HashMap<String, Object>() {{
			put("pageRowImage", getPageRowImageApiResponseFactory().create(pageRowImage.get()));
		}});
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
	protected Formatter getFormatter() {
		return this.formatter;
	}

	@Nonnull
	public PageService getPageService() {
		return pageService;
	}

	@Nonnull
	public PageApiResponseFactory getPageApiResponseFactory() {
		return pageApiResponseFactory;
	}

	@Nonnull
	public PageSectionApiResponseFactory getPageSectionApiResponseFactory() {
		return pageSectionApiResponseFactory;
	}

	@Nonnull
	public PageRowApiResponseFactory getPageRowApiResponseFactory() {
		return pageRowApiResponseFactory;
	}

	@Nonnull
	public PageRowContentApiResponseFactory getPageRowContentApiResponseFactory() {
		return pageRowContentApiResponseFactory;
	}

	@Nonnull
	public PageRowImageApiResponseFactory getPageRowImageApiResponseFactory() {
		return pageRowImageApiResponseFactory;
	}

	@Nonnull
	public PageRowGroupSessionApiResponseFactory getPageRowGroupSessionApiResponseFactory() {
		return pageRowGroupSessionApiResponseFactory;
	}
}
