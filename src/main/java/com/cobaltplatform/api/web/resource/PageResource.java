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
import com.cobaltplatform.api.model.api.request.CreateFileUploadRequest;
import com.cobaltplatform.api.model.api.request.CreatePageRequest;
import com.cobaltplatform.api.model.api.request.CreatePageRowContentRequest;
import com.cobaltplatform.api.model.api.request.CreatePageRowCustomOneColumnRequest;
import com.cobaltplatform.api.model.api.request.CreatePageRowCustomThreeColumnRequest;
import com.cobaltplatform.api.model.api.request.CreatePageRowCustomTwoColumnRequest;
import com.cobaltplatform.api.model.api.request.CreatePageRowGroupSessionRequest;
import com.cobaltplatform.api.model.api.request.CreatePageRowTagGroupRequest;
import com.cobaltplatform.api.model.api.request.CreatePageSectionRequest;
import com.cobaltplatform.api.model.api.request.FindPagesRequest;
import com.cobaltplatform.api.model.api.response.FileUploadResultApiResponse.FileUploadResultApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PageApiResponse.PageApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PageRowApiResponse.PageRowApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PageRowContentApiResponse.PageRowContentApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PageRowCustomOneColumnApiResponse.PageCustomOneColumnApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PageRowCustomThreeColumnApiResponse.PageCustomThreeColumnApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PageRowCustomTwoColumnApiResponse.PageCustomTwoColumnApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PageRowGroupSessionApiResponse.PageRowGroupSessionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PageRowColumnApiResponse.PageRowImageApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PageRowTagGroupApiResponse.PageRowTagGroupApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PageSectionApiResponse.PageSectionApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.FileUploadType;
import com.cobaltplatform.api.model.db.Page;
import com.cobaltplatform.api.model.db.PageRow;
import com.cobaltplatform.api.model.db.PageSection;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.model.service.FileUploadResult;
import com.cobaltplatform.api.model.service.FindResult;
import com.cobaltplatform.api.service.PageService;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.db.ReadReplica;
import com.cobaltplatform.api.web.request.RequestBodyParser;
import com.soklet.web.annotation.GET;
import com.soklet.web.annotation.POST;
import com.soklet.web.annotation.PathParameter;
import com.soklet.web.annotation.QueryParameter;
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
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
	private final PageRowTagGroupApiResponseFactory pageRowTagGroupApiResponseFactory;
	@Nonnull
	private final FileUploadResultApiResponseFactory fileUploadResultApiResponseFactory;
	@Nonnull
	private final PageRowApiResponseFactory pageRowApiResponseFactory;
	@Nonnull
	private final PageCustomOneColumnApiResponseFactory pageCustomOneColumnApiResponseFactory;
	@Nonnull
	private final PageCustomTwoColumnApiResponseFactory pageCustomTwoColumnApiResponseFactory;
	@Nonnull
	private final PageCustomThreeColumnApiResponseFactory pageRowCustomThreeColumnApiResponseFactory;
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
											@Nonnull FileUploadResultApiResponseFactory fileUploadResultApiResponseFactory,
											@Nonnull PageRowTagGroupApiResponseFactory pageRowTagGroupApiResponseFactory,
											@Nonnull PageCustomOneColumnApiResponseFactory pageCustomOneColumnApiResponseFactory,
											@Nonnull PageCustomTwoColumnApiResponseFactory pageCustomTwoColumnApiResponseFactory,
											@Nonnull PageCustomThreeColumnApiResponseFactory pageCustomThreeColumnApiResponseFactory,
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
		requireNonNull(fileUploadResultApiResponseFactory);
		requireNonNull(pageRowTagGroupApiResponseFactory);
		requireNonNull(pageCustomOneColumnApiResponseFactory);
		requireNonNull(pageCustomTwoColumnApiResponseFactory);
		requireNonNull(pageCustomThreeColumnApiResponseFactory);
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
		this.fileUploadResultApiResponseFactory = fileUploadResultApiResponseFactory;
		this.pageRowTagGroupApiResponseFactory = pageRowTagGroupApiResponseFactory;
		this.pageCustomOneColumnApiResponseFactory = pageCustomOneColumnApiResponseFactory;
		this.pageCustomTwoColumnApiResponseFactory = pageCustomTwoColumnApiResponseFactory;
		this.pageRowCustomThreeColumnApiResponseFactory = pageCustomThreeColumnApiResponseFactory;
		this.formatter = formatter;
	}

	@POST("/pages")
	@AuthenticationRequired
	public ApiResponse createPage(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		CreatePageRequest request = getRequestBodyParser().parse(requestBody, CreatePageRequest.class);
		Account account = getCurrentContext().getAccount().get();

		request.setCreatedByAccountId(account.getAccountId());
		request.setInstitutionId(account.getInstitutionId());
		UUID pageId = getPageService().createPage(request);
		Optional<Page> page = getPageService().findPageById(pageId);

		if (!page.isPresent())
			throw new NotFoundException();
		return new ApiResponse(new HashMap<String, Object>() {{
			put("page", getPageApiResponseFactory().create(page.get(), false));
		}});
	}

	@GET("/pages")
	@AuthenticationRequired
	@ReadReplica
	public ApiResponse pages(@Nonnull @QueryParameter Optional<Integer> pageNumber,
													 @Nonnull @QueryParameter Optional<Integer> pageSize,
													 @Nonnull @QueryParameter Optional<FindPagesRequest.OrderBy> orderBy) {
		requireNonNull(pageNumber);
		requireNonNull(pageSize);
		requireNonNull(orderBy);

		Account account = getCurrentContext().getAccount().get();

		FindResult<Page> findResult = getPageService().findAllPagesByInstitutionId(new FindPagesRequest() {
			{
				setPageNumber(pageNumber.orElse(0));
				setPageSize(pageSize.orElse(0));
				setOrderBy(orderBy.orElse(null));
				setInstitutionId(account.getInstitutionId());
			}
		});

		return new ApiResponse(new LinkedHashMap<String, Object>() {{
			put("totalCount", findResult.getTotalCount());
			put("totalCountDescription", getFormatter().formatNumber(findResult.getTotalCount()));
			put("pages", findResult.getResults().stream()
					.map(page -> getPageApiResponseFactory().create(page, false))
					.collect(Collectors.toList()));
		}});
	}

	@GET("/pages/{pageId}")
	@AuthenticationRequired
	@ReadReplica
	public ApiResponse page(@Nonnull @PathParameter("pageId") UUID pageId) {
		requireNonNull(pageId);

		Account account = getCurrentContext().getAccount().get();

		Optional<Page> page = getPageService().findPageById(pageId);

		if (!page.isPresent())
			throw new NotFoundException();
		return new ApiResponse(new HashMap<String, Object>() {{
			put("page", getPageApiResponseFactory().create(page.get(), true));
		}});
	}

	@POST("/pages/{pageId}/section")
	@AuthenticationRequired
	public ApiResponse createPageSection(@Nonnull @PathParameter("pageId") UUID pageId,
																			 @Nonnull @RequestBody String requestBody) {
		requireNonNull(pageId);
		requireNonNull(requestBody);

		CreatePageSectionRequest request = getRequestBodyParser().parse(requestBody, CreatePageSectionRequest.class);
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


	@POST("/pages/row/{pageSectionId}/content")
	@AuthenticationRequired
	public ApiResponse createPageRowContent(@Nonnull @PathParameter("pageSectionId") UUID pageSectionId,
																					@Nonnull @RequestBody String requestBody) {
		requireNonNull(pageSectionId);
		requireNonNull(requestBody);

		CreatePageRowContentRequest request = getRequestBodyParser().parse(requestBody, CreatePageRowContentRequest.class);
		Account account = getCurrentContext().getAccount().get();

		request.setCreatedByAccountId(account.getAccountId());
		request.setPageSectionId(pageSectionId);

		UUID pageRowId = getPageService().createPageRowContent(request);

		Optional<PageRow> pageRow = getPageService().findPageRowById(pageRowId);

		if (!pageRow.isPresent())
			throw new NotFoundException();
		return new ApiResponse(new HashMap<String, Object>() {{
			put("pageRow", getPageRowContentApiResponseFactory().create(pageRow.get()));
		}});
	}

	@POST("/pages/row/{pageSectionId}/tag-group")
	@AuthenticationRequired
	public ApiResponse createPageRowTagGroup(@Nonnull @PathParameter("pageSectionId") UUID pageSectionId,
																					 @Nonnull @RequestBody String requestBody) {
		requireNonNull(pageSectionId);
		requireNonNull(requestBody);

		CreatePageRowTagGroupRequest request = getRequestBodyParser().parse(requestBody, CreatePageRowTagGroupRequest.class);
		Account account = getCurrentContext().getAccount().get();

		request.setCreatedByAccountId(account.getAccountId());
		request.setPageSectionId(pageSectionId);

		UUID pageRowId = getPageService().createPageTagGroup(request);

		Optional<PageRow> pageRow = getPageService().findPageRowById(pageRowId);

		if (!pageRow.isPresent())
			throw new NotFoundException();
		return new ApiResponse(new HashMap<String, Object>() {{
			put("pageRow", getPageRowTagGroupApiResponseFactory().create(pageRow.get()));
		}});
	}

	@POST("/pages/row/{pageSectionId}/group-session")
	@AuthenticationRequired
	public ApiResponse createPageRowGroupSession(@Nonnull @PathParameter("pageSectionId") UUID pageSectionId,
																							 @Nonnull @RequestBody String requestBody) {
		requireNonNull(pageSectionId);
		requireNonNull(requestBody);

		CreatePageRowGroupSessionRequest request = getRequestBodyParser().parse(requestBody, CreatePageRowGroupSessionRequest.class);
		Account account = getCurrentContext().getAccount().get();

		request.setCreatedByAccountId(account.getAccountId());
		request.setPageSectionId(pageSectionId);

		UUID pageId = getPageService().createPageRowGroupSession(request);

		Optional<PageRow> pageRow = getPageService().findPageRowById(pageId);

		if (!pageRow.isPresent())
			throw new NotFoundException();
		return new ApiResponse(new HashMap<String, Object>() {{
			put("pageRow", getPageRowGroupSessionApiResponseFactory().create(pageRow.get()));
		}});
	}

	@POST("/pages/row/{pageSectionId}/custom-one-column")
	@AuthenticationRequired
	public ApiResponse createPageRowCustomOneColumn(@Nonnull @PathParameter("pageSectionId") UUID pageSectionId,
																									@Nonnull @RequestBody String requestBody) {
		requireNonNull(pageSectionId);
		requireNonNull(requestBody);

		CreatePageRowCustomOneColumnRequest request = getRequestBodyParser().parse(requestBody, CreatePageRowCustomOneColumnRequest.class);
		Account account = getCurrentContext().getAccount().get();

		request.setCreatedByAccountId(account.getAccountId());
		request.setPageSectionId(pageSectionId);

		UUID pageId = getPageService().createPageRowOneColumn(request);

		Optional<PageRow> pageRow = getPageService().findPageRowById(pageId);

		if (!pageRow.isPresent())
			throw new NotFoundException();
		return new ApiResponse(new HashMap<String, Object>() {{
			put("pageRow", getPageCustomOneColumnApiResponseFactory().create(pageRow.get()));
		}});
	}

	@POST("/pages/row/{pageSectionId}/custom-two-column")
	@AuthenticationRequired
	public ApiResponse createPageRowCustomTwoColumn(@Nonnull @PathParameter("pageSectionId") UUID pageSectionId,
																									@Nonnull @RequestBody String requestBody) {
		requireNonNull(pageSectionId);
		requireNonNull(requestBody);

		CreatePageRowCustomTwoColumnRequest request = getRequestBodyParser().parse(requestBody, CreatePageRowCustomTwoColumnRequest.class);
		Account account = getCurrentContext().getAccount().get();

		request.setCreatedByAccountId(account.getAccountId());
		request.setPageSectionId(pageSectionId);

		UUID pageId = getPageService().createPageRowTwoColumn(request);

		Optional<PageRow> pageRow = getPageService().findPageRowById(pageId);

		if (!pageRow.isPresent())
			throw new NotFoundException();
		return new ApiResponse(new HashMap<String, Object>() {{
			put("pageRow", getPageCustomTwoColumnApiResponseFactory().create(pageRow.get()));
		}});
	}


	@POST("/pages/row/{pageSectionId}/custom-three-column")
	@AuthenticationRequired
	public ApiResponse createPageRowCustomThreeColumn(@Nonnull @PathParameter("pageSectionId") UUID pageSectionId,
																										@Nonnull @RequestBody String requestBody) {
		requireNonNull(pageSectionId);
		requireNonNull(requestBody);

		CreatePageRowCustomThreeColumnRequest request = getRequestBodyParser().parse(requestBody, CreatePageRowCustomThreeColumnRequest.class);
		Account account = getCurrentContext().getAccount().get();

		request.setCreatedByAccountId(account.getAccountId());
		request.setPageSectionId(pageSectionId);

		UUID pageId = getPageService().createPageRowThreeColumn(request);

		Optional<PageRow> pageRow = getPageService().findPageRowById(pageId);

		if (!pageRow.isPresent())
			throw new NotFoundException();
		return new ApiResponse(new HashMap<String, Object>() {{
			put("pageRow", getPageRowCustomThreeColumnApiResponseFactory().create(pageRow.get()));
		}});
	}

	@Nonnull
	@POST("/pages/file-presigned-upload")
	@AuthenticationRequired
	public ApiResponse createFileImagePresignedUpload(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		Account account = getCurrentContext().getAccount().get();

		CreateFileUploadRequest request = getRequestBodyParser().parse(requestBody, CreateFileUploadRequest.class);
		request.setAccountId(account.getAccountId());
		request.setFileUploadTypeId(FileUploadType.FileUploadTypeId.PAGE_IMAGE);

		FileUploadResult fileUploadResult = getPageService().createPageFileUpload(request, "pages/files");
		return new ApiResponse(new HashMap<String, Object>() {{
			put("fileUploadResult", getFileUploadResultApiResponseFactory().create(fileUploadResult));
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

	@Nonnull
	public FileUploadResultApiResponseFactory getFileUploadResultApiResponseFactory() {
		return fileUploadResultApiResponseFactory;
	}

	@Nonnull
	public PageRowTagGroupApiResponseFactory getPageRowTagGroupApiResponseFactory() {
		return pageRowTagGroupApiResponseFactory;
	}

	@Nonnull
	public PageCustomOneColumnApiResponseFactory getPageCustomOneColumnApiResponseFactory() {
		return pageCustomOneColumnApiResponseFactory;
	}

	@Nonnull
	public PageCustomTwoColumnApiResponseFactory getPageCustomTwoColumnApiResponseFactory() {
		return pageCustomTwoColumnApiResponseFactory;
	}

	@Nonnull
	public PageCustomThreeColumnApiResponseFactory getPageRowCustomThreeColumnApiResponseFactory() {
		return pageRowCustomThreeColumnApiResponseFactory;
	}
}
