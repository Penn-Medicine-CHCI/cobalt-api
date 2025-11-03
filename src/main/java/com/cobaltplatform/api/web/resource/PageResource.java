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
import com.cobaltplatform.api.model.api.request.CreatePageRowMailingListRequest;
import com.cobaltplatform.api.model.api.request.CreatePageRowTagGroupRequest;
import com.cobaltplatform.api.model.api.request.CreatePageRowTagRequest;
import com.cobaltplatform.api.model.api.request.CreatePageSectionRequest;
import com.cobaltplatform.api.model.api.request.DuplicatePageRequest;
import com.cobaltplatform.api.model.api.request.FindPagesRequest;
import com.cobaltplatform.api.model.api.request.UpdatePageHeroRequest;
import com.cobaltplatform.api.model.api.request.UpdatePageRowContentRequest;
import com.cobaltplatform.api.model.api.request.UpdatePageRowCustomOneColumnRequest;
import com.cobaltplatform.api.model.api.request.UpdatePageRowCustomThreeColumnRequest;
import com.cobaltplatform.api.model.api.request.UpdatePageRowCustomTwoColumnRequest;
import com.cobaltplatform.api.model.api.request.UpdatePageRowDisplayOrderRequest;
import com.cobaltplatform.api.model.api.request.UpdatePageRowGroupSessionRequest;
import com.cobaltplatform.api.model.api.request.UpdatePageRowMailingListRequest;
import com.cobaltplatform.api.model.api.request.UpdatePageRowTagGroupRequest;
import com.cobaltplatform.api.model.api.request.UpdatePageRowTagRequest;
import com.cobaltplatform.api.model.api.request.UpdatePageSectionDisplayOrderRequest;
import com.cobaltplatform.api.model.api.request.UpdatePageSectionRequest;
import com.cobaltplatform.api.model.api.request.UpdatePageSettingsRequest;
import com.cobaltplatform.api.model.api.response.FileUploadResultApiResponse.FileUploadResultApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PageApiResponse.PageApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PageRowApiResponse.PageRowApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PageRowColumnApiResponse.PageRowImageApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PageRowContentApiResponse.PageRowContentApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PageRowCustomOneColumnApiResponse.PageCustomOneColumnApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PageRowCustomThreeColumnApiResponse.PageCustomThreeColumnApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PageRowCustomTwoColumnApiResponse.PageCustomTwoColumnApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PageRowGroupSessionApiResponse.PageRowGroupSessionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PageRowMailingListApiResponse.PageRowMailingListApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PageRowTagApiResponse.PageRowTagApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PageRowTagGroupApiResponse.PageRowTagGroupApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PageSectionApiResponse.PageSectionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PageSiteLocationApiResponse.PageSiteLocationApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PageUrlValidationResultApiResponse.PageAutocompleteResultApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.FileUploadType;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.Page;
import com.cobaltplatform.api.model.db.PageRow;
import com.cobaltplatform.api.model.db.PageRowMailingList;
import com.cobaltplatform.api.model.db.PageRowTag;
import com.cobaltplatform.api.model.db.PageSection;
import com.cobaltplatform.api.model.db.PageStatus;
import com.cobaltplatform.api.model.db.SiteLocation.SiteLocationId;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.model.service.FileUploadResult;
import com.cobaltplatform.api.model.service.FindResult;
import com.cobaltplatform.api.model.service.PageSiteLocation;
import com.cobaltplatform.api.model.service.PageUrlValidationResult;
import com.cobaltplatform.api.service.AuthorizationService;
import com.cobaltplatform.api.service.PageService;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.db.ReadReplica;
import com.cobaltplatform.api.web.request.RequestBodyParser;
import com.soklet.web.annotation.DELETE;
import com.soklet.web.annotation.GET;
import com.soklet.web.annotation.POST;
import com.soklet.web.annotation.PUT;
import com.soklet.web.annotation.PathParameter;
import com.soklet.web.annotation.QueryParameter;
import com.soklet.web.annotation.RequestBody;
import com.soklet.web.annotation.Resource;
import com.soklet.web.exception.AuthorizationException;
import com.soklet.web.exception.NotFoundException;
import com.soklet.web.response.ApiResponse;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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
	private final PageAutocompleteResultApiResponseFactory pageAutocompleteResultApiResponseFactory;
	@Nonnull
	private final PageSiteLocationApiResponseFactory pageSiteLocationApiResponseFactory;
	@Nonnull
	private final PageRowTagApiResponseFactory pageRowTagApiResponseFactory;
	@Nonnull
	private final PageRowMailingListApiResponseFactory pageRowMailingListApiResponseFactory;
	@Nonnull
	private final PageService pageService;
	@Nonnull
	private final Formatter formatter;
	@Nonnull
	private final AuthorizationService authorizationService;

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
											@Nonnull AuthorizationService authorizationService,
											@Nonnull PageAutocompleteResultApiResponseFactory pageAutocompleteResultApiResponseFactory,
											@Nonnull PageSiteLocationApiResponseFactory pageSiteLocationApiResponseFactory,
											@Nonnull PageRowTagApiResponseFactory pageRowTagApiResponseFactory,
											@Nonnull PageRowMailingListApiResponseFactory pageRowMailingListApiResponseFactory,
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
		requireNonNull(authorizationService);
		requireNonNull(pageAutocompleteResultApiResponseFactory);
		requireNonNull(pageSiteLocationApiResponseFactory);
		requireNonNull(pageRowTagApiResponseFactory);
		requireNonNull(pageRowMailingListApiResponseFactory);
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
		this.authorizationService = authorizationService;
		this.pageAutocompleteResultApiResponseFactory = pageAutocompleteResultApiResponseFactory;
		this.pageSiteLocationApiResponseFactory = pageSiteLocationApiResponseFactory;
		this.pageRowTagApiResponseFactory = pageRowTagApiResponseFactory;
		this.pageRowMailingListApiResponseFactory = pageRowMailingListApiResponseFactory;
		this.formatter = formatter;
	}

	@POST("/pages")
	@AuthenticationRequired
	public ApiResponse createPage(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		CreatePageRequest request = getRequestBodyParser().parse(requestBody, CreatePageRequest.class);
		InstitutionId institutionId = getCurrentContext().getInstitutionId();
		Account account = getCurrentContext().getAccount().get();

		if (!getAuthorizationService().canManagePages(institutionId, account))
			throw new AuthorizationException();

		request.setCreatedByAccountId(account.getAccountId());
		request.setInstitutionId(institutionId);
		UUID pageId = getPageService().createPage(request);
		Optional<Page> page = getPageService().findPageById(pageId, account.getInstitutionId(), true);

		if (!page.isPresent())
			throw new NotFoundException();
		return new ApiResponse(new HashMap<String, Object>() {{
			put("page", getPageApiResponseFactory().create(page.get(), false));
		}});
	}

	@Nonnull
	@GET("/pages/validate-url-name")
	@AuthenticationRequired
	@ReadReplica
	public ApiResponse pageUrlValidation(@Nonnull @QueryParameter String searchQuery,
																			 @Nonnull @QueryParameter Optional<UUID> pageId) {
		requireNonNull(searchQuery);

		InstitutionId institutionId = getCurrentContext().getInstitutionId();
		Account account = getCurrentContext().getAccount().get();

		if (!getAuthorizationService().canManagePages(institutionId, account))
			throw new AuthorizationException();
		PageUrlValidationResult result = getPageService().findPageUrlValidationResults(searchQuery, institutionId, pageId.orElse(null));

		return new ApiResponse(new HashMap<>() {{
			put("pageUrlNameValidationResult", getPageAutocompleteResultApiResponseFactory().create(result));
		}});
	}

	@DELETE("/pages/row/{pageRowId}")
	@AuthenticationRequired
	public ApiResponse deletePageRow(@Nonnull @PathParameter("pageRowId") UUID pageRowId) {
		requireNonNull(pageRowId);

		InstitutionId institutionId = getCurrentContext().getInstitutionId();
		Account account = getCurrentContext().getAccount().get();

		if (!getAuthorizationService().canManagePages(institutionId, account))
			throw new AuthorizationException();

		getPageService().deletePageRow(pageRowId, institutionId);

		return new ApiResponse();
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

	@GET("/page-site-locations")
	@AuthenticationRequired
	@ReadReplica
	public ApiResponse allPageSiteLocations() {
		Account account = getCurrentContext().getAccount().get();

		List<PageSiteLocation> pages = getPageService().findAllPageSiteLocations(account.getInstitutionId());

		return new ApiResponse(new LinkedHashMap<String, Object>() {{
			put("pageSiteLocations", pages.stream()
					.map(page -> getPageSiteLocationApiResponseFactory().create(page))
					.collect(Collectors.toList()));
		}});
	}

	@GET("/page-site-locations/{siteLocationId}")
	@AuthenticationRequired
	@ReadReplica
	public ApiResponse specificPageSiteLocations(@Nonnull @PathParameter("siteLocationId") SiteLocationId siteLocationId) {
		requireNonNull(siteLocationId);

		Account account = getCurrentContext().getAccount().get();

		List<PageSiteLocation> pages = getPageService().findAllPagesBySiteLocation(siteLocationId, account.getInstitutionId());
		return new ApiResponse(new LinkedHashMap<String, Object>() {{
			put("pageSiteLocations", pages.stream()
					.map(page -> getPageSiteLocationApiResponseFactory().create(page))
					.collect(Collectors.toList()));
		}});
	}

	@GET("/pages/published/{pageIdentifier}")
	@AuthenticationRequired
	@ReadReplica
	public ApiResponse publishedPage(@Nonnull @PathParameter("pageIdentifier") String pageIdentifier) {
		requireNonNull(pageIdentifier);

		Account account = getCurrentContext().getAccount().get();
		Optional<Page> page = getPageService().findPageById(pageIdentifier, account.getInstitutionId(), false);

		if (!page.isPresent())
			throw new NotFoundException();
		return new ApiResponse(new HashMap<String, Object>() {{
			put("page", getPageApiResponseFactory().create(page.get(), true));
		}});
	}

	@POST("/pages/{pageIdentifier}/edit")
	@AuthenticationRequired
	public ApiResponse editPage(@Nonnull @PathParameter("pageIdentifier") String pageIdentifier) {
		requireNonNull(pageIdentifier);

		Account account = getCurrentContext().getAccount().get();
		InstitutionId institutionId = getCurrentContext().getInstitutionId();

		if (!getAuthorizationService().canManagePages(institutionId, account))
			throw new AuthorizationException();

		Optional<Page> page = getPageService().findPageById(pageIdentifier, account.getInstitutionId(), true);

		if (!page.isPresent())
			throw new NotFoundException();

		if (page.get().getPageStatusId().equals(PageStatus.PageStatusId.LIVE)) {
			DuplicatePageRequest request = new DuplicatePageRequest();
			request.setCreatedByAccountId(account.getAccountId());
			request.setPageId(page.get().getPageId());
			request.setName(page.get().getName());
			request.setUrlName(page.get().getUrlName());
			request.setCopyForEditing(true);
			request.setPageStatusId(PageStatus.PageStatusId.COPY_FOR_EDITING);

			UUID pageId = getPageService().duplicatePage(request, institutionId);
			page = getPageService().findPageById(pageId, institutionId, true);
		}

		final Page finalPage = page.get();
		return new ApiResponse(new HashMap<String, Object>() {{
			put("pageId", finalPage.getPageId());
		}});

	}

	@GET("/pages/{pageIdentifier}")
	@AuthenticationRequired
	public ApiResponse page(@Nonnull @PathParameter("pageIdentifier") String pageIdentifier) {
		requireNonNull(pageIdentifier);

		Account account = getCurrentContext().getAccount().get();
		InstitutionId institutionId = getCurrentContext().getInstitutionId();

		if (!getAuthorizationService().canManagePages(institutionId, account))
			throw new AuthorizationException();

		Optional<Page> page = getPageService().findPageById(pageIdentifier, account.getInstitutionId(), true);

		if (!page.isPresent())
			throw new NotFoundException();

		if (page.get().getPageStatusId().equals(PageStatus.PageStatusId.LIVE)) {
			throw new IllegalStateException();
		}

		return new ApiResponse(new HashMap<String, Object>() {{
			put("page", getPageApiResponseFactory().create(page.get(), true));
		}});
	}

	@PUT("/pages/{pageId}/settings")
	@AuthenticationRequired
	public ApiResponse updatePageSettings(@Nonnull @RequestBody String requestBody,
																				@Nonnull @PathParameter("pageId") UUID pageId) {
		requireNonNull(requestBody);
		requireNonNull(pageId);

		UpdatePageSettingsRequest request = getRequestBodyParser().parse(requestBody, UpdatePageSettingsRequest.class);
		Account account = getCurrentContext().getAccount().get();
		InstitutionId institutionId = getCurrentContext().getInstitutionId();

		if (!getAuthorizationService().canManagePages(institutionId, account))
			throw new AuthorizationException();

		request.setPageId(pageId);
		request.setInstitutionId(institutionId);
		getPageService().updatePageSettings(request);

		Optional<Page> page = getPageService().findPageById(pageId, account.getInstitutionId(), true);

		if (!page.isPresent())
			throw new NotFoundException();
		return new ApiResponse(new HashMap<String, Object>() {{
			put("page", getPageApiResponseFactory().create(page.get(), true));
		}});
	}

	@PUT("/pages/{pageId}/publish")
	@AuthenticationRequired
	public ApiResponse publishPage(@Nonnull @PathParameter("pageId") UUID pageId) {
		requireNonNull(pageId);

		Account account = getCurrentContext().getAccount().get();
		InstitutionId institutionId = getCurrentContext().getInstitutionId();

		if (!getAuthorizationService().canManagePages(institutionId, account))
			throw new AuthorizationException();

		getPageService().publishPage(pageId, account.getInstitutionId());

		Optional<Page> page = getPageService().findPageById(pageId, account.getInstitutionId(), true);

		if (!page.isPresent())
			throw new NotFoundException();
		return new ApiResponse(new HashMap<String, Object>() {{
			put("page", getPageApiResponseFactory().create(page.get(), true));
		}});
	}

	@PUT("/pages/{pageId}/duplicate")
	@AuthenticationRequired
	public ApiResponse duplicatePage(@Nonnull @RequestBody String requestBody,
																	 @Nonnull @PathParameter("pageId") UUID pageId) {
		requireNonNull(pageId);

		Account account = getCurrentContext().getAccount().get();
		InstitutionId institutionId = getCurrentContext().getInstitutionId();

		if (!getAuthorizationService().canManagePages(institutionId, account))
			throw new AuthorizationException();

		DuplicatePageRequest request = getRequestBodyParser().parse(requestBody, DuplicatePageRequest.class);
		request.setPageId(pageId);
		request.setCreatedByAccountId(account.getAccountId());
		request.setPageStatusId(PageStatus.PageStatusId.DRAFT);

		UUID newPageId = getPageService().duplicatePage(request, institutionId);
		Optional<Page> page = getPageService().findPageById(newPageId, account.getInstitutionId(), true);

		if (!page.isPresent())
			throw new NotFoundException();
		return new ApiResponse(new HashMap<String, Object>() {{
			put("page", getPageApiResponseFactory().create(page.get(), true));
		}});
	}

	@PUT("/pages/{pageId}/unpublish")
	@AuthenticationRequired
	public ApiResponse unpublishPage(@Nonnull @PathParameter("pageId") UUID pageId) {
		requireNonNull(pageId);

		Account account = getCurrentContext().getAccount().get();
		InstitutionId institutionId = getCurrentContext().getInstitutionId();

		if (!getAuthorizationService().canManagePages(institutionId, account))
			throw new AuthorizationException();

		getPageService().unpublishPage(pageId, account.getInstitutionId());

		Optional<Page> page = getPageService().findPageById(pageId, account.getInstitutionId(), true);

		if (!page.isPresent())
			throw new NotFoundException();
		return new ApiResponse(new HashMap<String, Object>() {{
			put("page", getPageApiResponseFactory().create(page.get(), true));
		}});
	}

	@PUT("/pages/{pageId}/hero")
	@AuthenticationRequired
	public ApiResponse updatePageHero(@Nonnull @RequestBody String requestBody,
																		@Nonnull @PathParameter("pageId") UUID pageId) {
		requireNonNull(requestBody);
		requireNonNull(pageId);

		UpdatePageHeroRequest request = getRequestBodyParser().parse(requestBody, UpdatePageHeroRequest.class);
		Account account = getCurrentContext().getAccount().get();
		InstitutionId institutionId = getCurrentContext().getInstitutionId();

		if (!getAuthorizationService().canManagePages(institutionId, account))
			throw new AuthorizationException();

		request.setPageId(pageId);
		request.setInstitutionId(institutionId);
		getPageService().updatePageHero(request);

		Optional<Page> page = getPageService().findPageById(pageId, account.getInstitutionId(), true);

		if (!page.isPresent())
			throw new NotFoundException();
		return new ApiResponse(new HashMap<String, Object>() {{
			put("page", getPageApiResponseFactory().create(page.get(), true));
		}});
	}

	@DELETE("/pages/{pageId}")
	@AuthenticationRequired
	public ApiResponse deletePage(@Nonnull @PathParameter("pageId") UUID pageId) {
		requireNonNull(pageId);

		Account account = getCurrentContext().getAccount().get();
		InstitutionId institutionId = getCurrentContext().getInstitutionId();

		if (!getAuthorizationService().canManagePages(institutionId, account))
			throw new AuthorizationException();

		getPageService().deletePage(pageId, account.getInstitutionId());

		return new ApiResponse();
	}

	@POST("/pages/{pageId}/section")
	@AuthenticationRequired
	public ApiResponse createPageSection(@Nonnull @PathParameter("pageId") UUID pageId,
																			 @Nonnull @RequestBody String requestBody) {
		requireNonNull(pageId);
		requireNonNull(requestBody);

		CreatePageSectionRequest request = getRequestBodyParser().parse(requestBody, CreatePageSectionRequest.class);
		Account account = getCurrentContext().getAccount().get();
		InstitutionId institutionId = getCurrentContext().getInstitutionId();

		if (!getAuthorizationService().canManagePages(institutionId, account))
			throw new AuthorizationException();

		request.setCreatedByAccountId(account.getAccountId());
		request.setPageId(pageId);
		request.setInstitutionId(account.getInstitutionId());
		UUID pageSectionId = getPageService().createPageSection(request);
		Optional<PageSection> pageSection = getPageService().findPageSectionById(pageSectionId, institutionId);

		if (!pageSection.isPresent())
			throw new NotFoundException();
		return new ApiResponse(new HashMap<String, Object>() {{
			put("pageSection", getPageSectionApiResponseFactory().create(pageSection.get()));
		}});
	}

	@DELETE("/pages/section/{pageSectionId}")
	@AuthenticationRequired
	public ApiResponse deletePageSection(@Nonnull @PathParameter("pageSectionId") UUID pageSectionId) {
		requireNonNull(pageSectionId);

		Account account = getCurrentContext().getAccount().get();
		InstitutionId institutionId = getCurrentContext().getInstitutionId();

		if (!getAuthorizationService().canManagePages(institutionId, account))
			throw new AuthorizationException();

		getPageService().deletePageSection(pageSectionId, institutionId);

		return new ApiResponse();
	}

	@PUT("/pages/section/{pageSectionId}")
	@AuthenticationRequired
	public ApiResponse updatePageSection(@Nonnull @PathParameter("pageSectionId") UUID pageSectionId,
																			 @Nonnull @RequestBody String requestBody) {
		requireNonNull(pageSectionId);
		requireNonNull(requestBody);

		UpdatePageSectionRequest request = getRequestBodyParser().parse(requestBody, UpdatePageSectionRequest.class);
		Account account = getCurrentContext().getAccount().get();
		InstitutionId institutionId = getCurrentContext().getInstitutionId();

		if (!getAuthorizationService().canManagePages(institutionId, account))
			throw new AuthorizationException();

		request.setPageSectionId(pageSectionId);
		request.setInstitutionId(institutionId);

		getPageService().updatePageSection(request);
		Optional<PageSection> pageSection = getPageService().findPageSectionById(pageSectionId, institutionId);

		if (!pageSection.isPresent())
			throw new NotFoundException();
		return new ApiResponse(new HashMap<String, Object>() {{
			put("pageSection", getPageSectionApiResponseFactory().create(pageSection.get()));
		}});
	}

	@PUT("/pages/{pageId}/section")
	@AuthenticationRequired
	public ApiResponse updatePageSectionDisplayOrder(@Nonnull @PathParameter("pageId") UUID pageId,
																									 @Nonnull @RequestBody String requestBody) {
		requireNonNull(pageId);
		requireNonNull(requestBody);

		Account account = getCurrentContext().getAccount().get();
		InstitutionId institutionId = getCurrentContext().getInstitutionId();

		if (!getAuthorizationService().canManagePages(institutionId, account))
			throw new AuthorizationException();

		UpdatePageSectionDisplayOrderRequest request = getRequestBodyParser().parse(requestBody, UpdatePageSectionDisplayOrderRequest.class);
		request.setInstitutionId(institutionId);

		getPageService().updatePageSectionDisplayOrder(request);

		List<PageSection> pageSections = getPageService().findPageSectionsByPageId(pageId, institutionId);

		return new ApiResponse(new HashMap<String, Object>() {{
			put("pageSections", pageSections.stream().map(pageSection -> getPageSectionApiResponseFactory().create(pageSection))
					.collect(Collectors.toList()));
		}});
	}

	@PUT("/pages/row/{pageSectionId}")
	@AuthenticationRequired
	public ApiResponse updatePageRowDisplayOrder(@Nonnull @PathParameter("pageSectionId") UUID pageId,
																							 @Nonnull @RequestBody String requestBody) {
		requireNonNull(pageId);
		requireNonNull(requestBody);

		Account account = getCurrentContext().getAccount().get();
		InstitutionId institutionId = getCurrentContext().getInstitutionId();

		if (!getAuthorizationService().canManagePages(institutionId, account))
			throw new AuthorizationException();

		UpdatePageRowDisplayOrderRequest request = getRequestBodyParser().parse(requestBody, UpdatePageRowDisplayOrderRequest.class);

		getPageService().updatePageRowDisplayOrder(request);

		List<PageRow> pageRows = getPageService().findPageRowsBySectionId(pageId, institutionId);

		return new ApiResponse(new HashMap<String, Object>() {{
			put("pageRows", pageRows.stream().map(pageRow -> getPageRowApiResponseFactory().create(pageRow))
					.collect(Collectors.toList()));
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
		InstitutionId institutionId = getCurrentContext().getInstitutionId();

		if (!getAuthorizationService().canManagePages(institutionId, account))
			throw new AuthorizationException();

		request.setCreatedByAccountId(account.getAccountId());
		request.setPageSectionId(pageSectionId);

		UUID pageRowId = getPageService().createPageRowContent(request, institutionId);

		Optional<PageRow> pageRow = getPageService().findPageRowById(pageRowId, institutionId);

		if (!pageRow.isPresent())
			throw new NotFoundException();
		return new ApiResponse(new HashMap<String, Object>() {{
			put("pageRow", getPageRowContentApiResponseFactory().create(pageRow.get()));
		}});
	}

	@PUT("/pages/row/{pageRowId}/content")
	@AuthenticationRequired
	public ApiResponse updatePageRowContent(@Nonnull @PathParameter("pageRowId") UUID pageRowId,
																					@Nonnull @RequestBody String requestBody) {
		requireNonNull(pageRowId);
		requireNonNull(requestBody);

		UpdatePageRowContentRequest request = getRequestBodyParser().parse(requestBody, UpdatePageRowContentRequest.class);
		Account account = getCurrentContext().getAccount().get();
		InstitutionId institutionId = getCurrentContext().getInstitutionId();

		if (!getAuthorizationService().canManagePages(institutionId, account))
			throw new AuthorizationException();

		request.setPageRowId(pageRowId);

		getPageService().updatePageRowContent(request, institutionId);

		Optional<PageRow> pageRow = getPageService().findPageRowById(pageRowId, institutionId);

		if (!pageRow.isPresent())
			throw new NotFoundException();
		return new ApiResponse(new HashMap<String, Object>() {{
			put("pageRow", getPageRowContentApiResponseFactory().create(pageRow.get()));
		}});
	}

	@DELETE("/pages/row/{pageRowId}/content/{contentId}")
	@AuthenticationRequired
	public ApiResponse deletePageRowContent(@Nonnull @PathParameter("pageRowId") UUID pageRowId,
																					@Nonnull @PathParameter("contentId") UUID contentId) {
		requireNonNull(pageRowId);
		requireNonNull(contentId);

		Account account = getCurrentContext().getAccount().get();
		InstitutionId institutionId = getCurrentContext().getInstitutionId();

		if (!getAuthorizationService().canManagePages(institutionId, account))
			throw new AuthorizationException();

		getPageService().deletePageRowContent(pageRowId, contentId);

		Optional<PageRow> pageRow = getPageService().findPageRowById(pageRowId, institutionId);

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
		InstitutionId institutionId = getCurrentContext().getInstitutionId();

		if (!getAuthorizationService().canManagePages(institutionId, account))
			throw new AuthorizationException();

		request.setCreatedByAccountId(account.getAccountId());
		request.setPageSectionId(pageSectionId);

		UUID pageRowId = getPageService().createPageTagGroup(request, institutionId);

		Optional<PageRow> pageRow = getPageService().findPageRowById(pageRowId, institutionId);

		if (!pageRow.isPresent())
			throw new NotFoundException();
		return new ApiResponse(new HashMap<String, Object>() {{
			put("pageRow", getPageRowTagGroupApiResponseFactory().create(pageRow.get()));
		}});
	}

	@PUT("/pages/row/{pageRowId}/tag-group")
	@AuthenticationRequired
	public ApiResponse updatePageRowTagGroup(@Nonnull @PathParameter("pageRowId") UUID pageRowId,
																					 @Nonnull @RequestBody String requestBody) {
		requireNonNull(pageRowId);
		requireNonNull(requestBody);

		UpdatePageRowTagGroupRequest request = getRequestBodyParser().parse(requestBody, UpdatePageRowTagGroupRequest.class);
		Account account = getCurrentContext().getAccount().get();
		InstitutionId institutionId = getCurrentContext().getInstitutionId();

		if (!getAuthorizationService().canManagePages(institutionId, account))
			throw new AuthorizationException();

		request.setPageRowId(pageRowId);

		getPageService().updatePageTagGroup(request, institutionId);

		Optional<PageRow> pageRow = getPageService().findPageRowById(pageRowId, institutionId);

		if (!pageRow.isPresent())
			throw new NotFoundException();
		return new ApiResponse(new HashMap<String, Object>() {{
			put("pageRow", getPageRowTagGroupApiResponseFactory().create(pageRow.get()));
		}});
	}

	@POST("/pages/row/{pageSectionId}/tag")
	@AuthenticationRequired
	public ApiResponse createPageRowTag(@Nonnull @PathParameter("pageSectionId") UUID pageSectionId,
																			@Nonnull @RequestBody String requestBody) {
		requireNonNull(pageSectionId);
		requireNonNull(requestBody);

		CreatePageRowTagRequest request = getRequestBodyParser().parse(requestBody, CreatePageRowTagRequest.class);
		Account account = getCurrentContext().getAccount().get();
		InstitutionId institutionId = getCurrentContext().getInstitutionId();

		if (!getAuthorizationService().canManagePages(institutionId, account))
			throw new AuthorizationException();

		request.setCreatedByAccountId(account.getAccountId());
		request.setPageSectionId(pageSectionId);

		UUID pageRowId = getPageService().createPageTag(request, institutionId);

		Optional<PageRow> pageRow = getPageService().findPageRowById(pageRowId, institutionId);
		Optional<PageRowTag> pageRowTag = getPageService().findPageRowTagByRowId(pageRowId);

		if (!pageRow.isPresent() || !pageRowTag.isPresent())
			throw new NotFoundException();
		return new ApiResponse(new HashMap<String, Object>() {{
			put("pageRow", getPageRowTagApiResponseFactory().create(pageRow.get(), pageRowTag.get()));
		}});
	}

	@PUT("/pages/row/{pageRowId}/tag")
	@AuthenticationRequired
	public ApiResponse updatePageRowTag(@Nonnull @PathParameter("pageRowId") UUID pageRowId,
																			@Nonnull @RequestBody String requestBody) {
		requireNonNull(pageRowId);
		requireNonNull(requestBody);

		UpdatePageRowTagRequest request = getRequestBodyParser().parse(requestBody, UpdatePageRowTagRequest.class);
		Account account = getCurrentContext().getAccount().get();
		InstitutionId institutionId = getCurrentContext().getInstitutionId();

		if (!getAuthorizationService().canManagePages(institutionId, account))
			throw new AuthorizationException();

		request.setPageRowId(pageRowId);

		getPageService().updatePageTag(request, institutionId);

		Optional<PageRow> pageRow = getPageService().findPageRowById(pageRowId, institutionId);
		Optional<PageRowTag> pageRowTag = getPageService().findPageRowTagByRowId(pageRowId);

		if (!pageRow.isPresent() || !pageRowTag.isPresent())
			throw new NotFoundException();
		return new ApiResponse(new HashMap<String, Object>() {{
			put("pageRow", getPageRowTagApiResponseFactory().create(pageRow.get(), pageRowTag.get()));
		}});
	}

	@POST("/pages/row/{pageSectionId}/mailing-list")
	@AuthenticationRequired
	public ApiResponse createPageRowMailingList(@Nonnull @PathParameter("pageSectionId") UUID pageSectionId,
																							@Nonnull @RequestBody String requestBody) {
		requireNonNull(pageSectionId);
		requireNonNull(requestBody);

		CreatePageRowMailingListRequest request = getRequestBodyParser().parse(requestBody, CreatePageRowMailingListRequest.class);
		Account account = getCurrentContext().getAccount().get();
		InstitutionId institutionId = getCurrentContext().getInstitutionId();

		if (!getAuthorizationService().canManagePages(institutionId, account))
			throw new AuthorizationException();

		request.setCreatedByAccountId(account.getAccountId());
		request.setPageSectionId(pageSectionId);

		UUID pageRowId = getPageService().createPageRowMailingList(request, institutionId);

		Optional<PageRow> pageRow = getPageService().findPageRowById(pageRowId, institutionId);
		Optional<PageRowMailingList> pageRowMailingList = getPageService().findPageRowMailingListByRowId(pageRowId);

		if (!pageRow.isPresent() || !pageRowMailingList.isPresent())
			throw new NotFoundException();
		return new ApiResponse(new HashMap<String, Object>() {{
			put("pageRow", getPageRowMailingListApiResponseFactory().create(pageRow.get(), pageRowMailingList.get()));
		}});
	}

	@PUT("/pages/row/{pageRowId}/mailing-list")
	@AuthenticationRequired
	public ApiResponse updatePageRowMailingList(@Nonnull @PathParameter("pageRowId") UUID pageRowId,
																							@Nonnull @RequestBody String requestBody) {
		requireNonNull(pageRowId);
		requireNonNull(requestBody);

		UpdatePageRowMailingListRequest request = getRequestBodyParser().parse(requestBody, UpdatePageRowMailingListRequest.class);
		Account account = getCurrentContext().getAccount().get();
		InstitutionId institutionId = getCurrentContext().getInstitutionId();

		if (!getAuthorizationService().canManagePages(institutionId, account))
			throw new AuthorizationException();

		request.setPageRowId(pageRowId);

		getPageService().updatePageRowMailingList(request, institutionId);

		Optional<PageRow> pageRow = getPageService().findPageRowById(pageRowId, institutionId);
		Optional<PageRowMailingList> pageRowMailingList = getPageService().findPageRowMailingListByRowId(pageRowId);

		if (!pageRow.isPresent() || !pageRowMailingList.isPresent())
			throw new NotFoundException();
		return new ApiResponse(new HashMap<String, Object>() {{
			put("pageRow", getPageRowMailingListApiResponseFactory().create(pageRow.get(), pageRowMailingList.get()));
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
		InstitutionId institutionId = getCurrentContext().getInstitutionId();

		if (!getAuthorizationService().canManagePages(institutionId, account))
			throw new AuthorizationException();

		request.setCreatedByAccountId(account.getAccountId());
		request.setPageSectionId(pageSectionId);

		UUID pageId = getPageService().createPageRowGroupSession(request, institutionId);

		Optional<PageRow> pageRow = getPageService().findPageRowById(pageId, institutionId);

		if (!pageRow.isPresent())
			throw new NotFoundException();
		return new ApiResponse(new HashMap<String, Object>() {{
			put("pageRow", getPageRowGroupSessionApiResponseFactory().create(pageRow.get()));
		}});
	}

	@DELETE("/pages/row/{pageRowId}/group-session/{groupSessionId}")
	@AuthenticationRequired
	public ApiResponse deletePageRowGroupSession(@Nonnull @PathParameter("pageRowId") UUID pageRowId,
																							 @Nonnull @PathParameter("groupSessionId") UUID groupSessionId) {
		requireNonNull(pageRowId);
		requireNonNull(groupSessionId);

		Account account = getCurrentContext().getAccount().get();
		InstitutionId institutionId = getCurrentContext().getInstitutionId();

		if (!getAuthorizationService().canManagePages(institutionId, account))
			throw new AuthorizationException();

		getPageService().deletePageRowGroupSession(pageRowId, groupSessionId);
		Optional<PageRow> pageRow = getPageService().findPageRowById(pageRowId, institutionId);

		if (!pageRow.isPresent())
			throw new NotFoundException();
		return new ApiResponse(new HashMap<String, Object>() {{
			put("pageRow", getPageRowGroupSessionApiResponseFactory().create(pageRow.get()));
		}});
	}

	@PUT("/pages/row/{pageRowId}/group-session")
	@AuthenticationRequired
	public ApiResponse updatePageRowGroupSession(@Nonnull @PathParameter("pageRowId") UUID pageRowId,
																							 @Nonnull @RequestBody String requestBody) {
		requireNonNull(pageRowId);
		requireNonNull(requestBody);

		UpdatePageRowGroupSessionRequest request = getRequestBodyParser().parse(requestBody, UpdatePageRowGroupSessionRequest.class);
		Account account = getCurrentContext().getAccount().get();
		InstitutionId institutionId = getCurrentContext().getInstitutionId();

		if (!getAuthorizationService().canManagePages(institutionId, account))
			throw new AuthorizationException();

		request.setPageRowId(pageRowId);

		getPageService().updatePageRowGroupSession(request, institutionId);

		Optional<PageRow> pageRow = getPageService().findPageRowById(pageRowId, institutionId);

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
		InstitutionId institutionId = getCurrentContext().getInstitutionId();

		if (!getAuthorizationService().canManagePages(institutionId, account))
			throw new AuthorizationException();

		request.setCreatedByAccountId(account.getAccountId());
		request.setPageSectionId(pageSectionId);

		UUID pageId = getPageService().createPageRowOneColumn(request, institutionId);

		Optional<PageRow> pageRow = getPageService().findPageRowById(pageId, institutionId);

		if (!pageRow.isPresent())
			throw new NotFoundException();
		return new ApiResponse(new HashMap<String, Object>() {{
			put("pageRow", getPageCustomOneColumnApiResponseFactory().create(pageRow.get()));
		}});
	}

	@PUT("/pages/row/{pageRowId}/custom-one-column")
	@AuthenticationRequired
	public ApiResponse updatePageRowCustomOneColumn(@Nonnull @PathParameter("pageRowId") UUID pageRowId,
																									@Nonnull @RequestBody String requestBody) {
		requireNonNull(pageRowId);
		requireNonNull(requestBody);

		UpdatePageRowCustomOneColumnRequest request = getRequestBodyParser().parse(requestBody, UpdatePageRowCustomOneColumnRequest.class);
		Account account = getCurrentContext().getAccount().get();
		InstitutionId institutionId = getCurrentContext().getInstitutionId();

		if (!getAuthorizationService().canManagePages(institutionId, account))
			throw new AuthorizationException();

		request.setPageRowId(pageRowId);

		getPageService().updatePageRowOneColumn(request, institutionId);

		Optional<PageRow> pageRow = getPageService().findPageRowById(pageRowId, institutionId);

		if (!pageRow.isPresent())
			throw new NotFoundException();
		return new ApiResponse(new HashMap<String, Object>() {{
			put("pageRow", getPageCustomOneColumnApiResponseFactory().create(pageRow.get()));
		}});
	}

	@PUT("/pages/row/{pageRowId}/custom-two-column")
	@AuthenticationRequired
	public ApiResponse updatePageRowCustomTwoColumn(@Nonnull @PathParameter("pageRowId") UUID pageRowId,
																									@Nonnull @RequestBody String requestBody) {
		requireNonNull(pageRowId);
		requireNonNull(requestBody);

		UpdatePageRowCustomTwoColumnRequest request = getRequestBodyParser().parse(requestBody, UpdatePageRowCustomTwoColumnRequest.class);
		Account account = getCurrentContext().getAccount().get();
		InstitutionId institutionId = getCurrentContext().getInstitutionId();

		if (!getAuthorizationService().canManagePages(institutionId, account))
			throw new AuthorizationException();

		request.setPageRowId(pageRowId);

		getPageService().updatePageRowTwoColumn(request, institutionId);

		Optional<PageRow> pageRow = getPageService().findPageRowById(pageRowId, institutionId);

		if (!pageRow.isPresent())
			throw new NotFoundException();
		return new ApiResponse(new HashMap<String, Object>() {{
			put("pageRow", getPageCustomTwoColumnApiResponseFactory().create(pageRow.get()));
		}});
	}

	@PUT("/pages/row/{pageRowId}/custom-three-column")
	@AuthenticationRequired
	public ApiResponse updatePageRowCustomThreeColumn(@Nonnull @PathParameter("pageRowId") UUID pageRowId,
																										@Nonnull @RequestBody String requestBody) {
		requireNonNull(pageRowId);
		requireNonNull(requestBody);

		UpdatePageRowCustomThreeColumnRequest request = getRequestBodyParser().parse(requestBody, UpdatePageRowCustomThreeColumnRequest.class);
		Account account = getCurrentContext().getAccount().get();
		InstitutionId institutionId = getCurrentContext().getInstitutionId();

		if (!getAuthorizationService().canManagePages(institutionId, account))
			throw new AuthorizationException();

		request.setPageRowId(pageRowId);

		getPageService().updatePageRowThreeColumn(request, institutionId);

		Optional<PageRow> pageRow = getPageService().findPageRowById(pageRowId, institutionId);

		if (!pageRow.isPresent())
			throw new NotFoundException();
		return new ApiResponse(new HashMap<String, Object>() {{
			put("pageRow", getPageRowCustomThreeColumnApiResponseFactory().create(pageRow.get()));
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
		InstitutionId institutionId = getCurrentContext().getInstitutionId();

		if (!getAuthorizationService().canManagePages(institutionId, account))
			throw new AuthorizationException();

		request.setCreatedByAccountId(account.getAccountId());
		request.setPageSectionId(pageSectionId);

		UUID pageId = getPageService().createPageRowTwoColumn(request, institutionId);

		Optional<PageRow> pageRow = getPageService().findPageRowById(pageId, institutionId);

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
		InstitutionId institutionId = getCurrentContext().getInstitutionId();

		if (!getAuthorizationService().canManagePages(institutionId, account))
			throw new AuthorizationException();

		request.setCreatedByAccountId(account.getAccountId());
		request.setPageSectionId(pageSectionId);

		UUID pageId = getPageService().createPageRowThreeColumn(request, institutionId);

		Optional<PageRow> pageRow = getPageService().findPageRowById(pageId, institutionId);

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
		InstitutionId institutionId = getCurrentContext().getInstitutionId();

		if (!getAuthorizationService().canManagePages(institutionId, account))
			throw new AuthorizationException();

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

	@Nonnull
	public AuthorizationService getAuthorizationService() {
		return authorizationService;
	}

	@Nonnull
	public PageAutocompleteResultApiResponseFactory getPageAutocompleteResultApiResponseFactory() {
		return pageAutocompleteResultApiResponseFactory;
	}

	@Nonnull
	public PageSiteLocationApiResponseFactory getPageSiteLocationApiResponseFactory() {
		return pageSiteLocationApiResponseFactory;
	}

	@Nonnull
	public PageRowTagApiResponseFactory getPageRowTagApiResponseFactory() {
		return pageRowTagApiResponseFactory;
	}

	@Nonnull
	protected PageRowMailingListApiResponseFactory getPageRowMailingListApiResponseFactory() {
		return this.pageRowMailingListApiResponseFactory;
	}
}
