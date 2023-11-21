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
import com.cobaltplatform.api.model.api.request.CreateContentRequest;
import com.cobaltplatform.api.model.api.request.CreatePresignedUploadRequest;
import com.cobaltplatform.api.model.api.request.UpdateContentRequest;
import com.cobaltplatform.api.model.api.response.AdminContentApiResponse.AdminContentApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AdminInstitutionApiResponse.AdminInstitutionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ContentApiResponse.ContentApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ContentStatusApiResponse.ContentStatusApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PresignedUploadApiResponse.PresignedUploadApiResponseFactory;
import com.cobaltplatform.api.model.api.response.TagApiResponse;
import com.cobaltplatform.api.model.api.response.TagApiResponse.TagApiResponseFactory;
import com.cobaltplatform.api.model.api.response.TagGroupApiResponse;
import com.cobaltplatform.api.model.api.response.TagGroupApiResponse.TagGroupApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.ContentStatus;
import com.cobaltplatform.api.model.db.ContentType;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.model.service.AdminContent;
import com.cobaltplatform.api.model.service.FindResult;
import com.cobaltplatform.api.service.AdminContentService;
import com.cobaltplatform.api.model.service.PresignedUpload;
import com.cobaltplatform.api.service.AssessmentService;
import com.cobaltplatform.api.service.ContentService;
import com.cobaltplatform.api.service.ImageUploadService;
import com.cobaltplatform.api.service.InstitutionService;
import com.cobaltplatform.api.service.TagService;
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
import org.checkerframework.checker.units.qual.A;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.cobaltplatform.api.model.api.response.AdminContentApiResponse.AdminContentDisplayType;
import static com.cobaltplatform.api.model.api.response.AssessmentFormApiResponse.AssessmentFormApiResponseFactory;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;


/**
 * @author Transmogrify LLC.
 */

@Resource
@Singleton
@ThreadSafe
public class AdminResource {
	@Nonnull
	private final ContentService contentService;
	@Nonnull
	private final AdminContentService adminContentService;
	@Nonnull
	private final TagService tagService;
	@Nonnull
	private final RequestBodyParser requestBodyParser;
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final ContentApiResponseFactory contentApiResponseFactory;
	@Nonnull
	private final AdminContentApiResponseFactory adminContentApiResponseFactory;
	@Nonnull
	private final Provider<ImageUploadService> imageUploadServiceProvider;
	@Nonnull
	private final Provider<PresignedUploadApiResponseFactory> presignedUploadApiResponseFactoryProvider;
	@Nonnull
	private final Provider<AssessmentService> assessmentServiceProvider;
	@Nonnull
	private final Provider<AssessmentFormApiResponseFactory> assessmentFormApiResponseFactoryProvider;
	@Nonnull
	private final InstitutionService institutionService;
	@Nonnull
	private final AdminInstitutionApiResponseFactory adminInstitutionApiResponseFactory;
	@Nonnull
	private final TagApiResponseFactory tagApiResponseFactory;
	@Nonnull
	private final TagGroupApiResponseFactory tagGroupApiResponseFactory;
	@Nonnull
	private final ContentStatusApiResponseFactory contentStatusApiResponseFactory;


	@Inject
	public AdminResource(@Nonnull ContentService contentService,
											 @Nonnull AdminContentService adminContentService,
											 @Nonnull TagService tagService,
											 @Nonnull RequestBodyParser requestBodyParser,
											 @Nonnull Provider<CurrentContext> currentContextProvider,
											 @Nonnull ContentApiResponseFactory contentApiResponseFactory,
											 @Nonnull AdminContentApiResponseFactory adminContentApiResponseFactory,
											 @Nonnull InstitutionService institutionService,
											 @Nonnull AdminInstitutionApiResponseFactory adminInstitutionApiResponseFactory,
											 @Nonnull Provider<ImageUploadService> imageUploadServiceProvider,
											 @Nonnull Provider<PresignedUploadApiResponseFactory> presignedUploadApiResponseFactoryProvider,
											 @Nonnull Provider<AssessmentService> assessmentServiceProvider,
											 @Nonnull Provider<AssessmentFormApiResponseFactory> assessmentFormApiResponseFactoryProvider,
											 @Nonnull TagApiResponseFactory tagApiResponseFactory,
											 @Nonnull TagGroupApiResponseFactory tagGroupApiResponseFactory,
											 @Nonnull ContentStatusApiResponseFactory contentStatusApiResponseFactory) {
		this.contentService = contentService;
		this.adminContentService = adminContentService;
		this.tagService = tagService;
		this.requestBodyParser = requestBodyParser;
		this.currentContextProvider = currentContextProvider;
		this.contentApiResponseFactory = contentApiResponseFactory;
		this.adminContentApiResponseFactory = adminContentApiResponseFactory;
		this.institutionService = institutionService;
		this.adminInstitutionApiResponseFactory = adminInstitutionApiResponseFactory;
		this.imageUploadServiceProvider = imageUploadServiceProvider;
		this.presignedUploadApiResponseFactoryProvider = presignedUploadApiResponseFactoryProvider;
		this.assessmentServiceProvider = assessmentServiceProvider;
		this.assessmentFormApiResponseFactoryProvider = assessmentFormApiResponseFactoryProvider;
		this.tagApiResponseFactory = tagApiResponseFactory;
		this.tagGroupApiResponseFactory = tagGroupApiResponseFactory;
		this.contentStatusApiResponseFactory = contentStatusApiResponseFactory;
	}

	@GET("/admin/my-content/filter")
	@AuthenticationRequired
	public ApiResponse getMyContentFilter() {
		return new ApiResponse(new HashMap<String, Object>() {{
			put("contentTypes", getContentService().findContentTypes());
			put("institutions", getInstitutionService().findNonCobaltInstitutions().stream().
					map(it -> getAdminInstitutionApiResponseFactory().create(it)).collect(Collectors.toList()));
		}});
	}

	@GET("/admin/content")
	@AuthenticationRequired
	public ApiResponse getContent(@QueryParameter Optional<Integer> page,
																@QueryParameter Optional<ContentType.ContentTypeId> contentTypeId,
																@QueryParameter Optional<Institution.InstitutionId> institutionId,
																@QueryParameter Optional<String> search,
																@QueryParameter Optional<ContentStatus.ContentStatusId> contentStatusId) {
		Account account = getCurrentContext().getAccount().get();

		//TODO: create a filter object to pass all the query params
		FindResult<AdminContent> content = getAdminContentService()
				.findAllContentForAdmin(account, page, contentTypeId, institutionId, search, contentStatusId);
		List<UUID> institutionContentIds = getAdminContentService().findContentIdsForInstitution(account.getInstitutionId());
		return new ApiResponse(new HashMap<String, Object>() {{
			put("adminContent", content.getResults().stream().map(content -> getAdminContentApiResponseFactory().create(account, content, AdminContentDisplayType.LIST, institutionContentIds)).collect(Collectors.toList()));
			put("totalCount", content.getTotalCount());
		}});
	}

	@Nonnull
	@GET("/admin/content-tags")
	@AuthenticationRequired
	public ApiResponse tagsForContent() {
		Account account = getCurrentContext().getAccount().get();

		List<TagGroupApiResponse> tagGroups = getTagService().findTagGroupsByInstitutionId(account.getInstitutionId()).stream()
				.map(tagGroup -> getTagGroupApiResponseFactory().create(tagGroup))
				.collect(Collectors.toList());

		List<TagApiResponse> tags = getTagService().findTagsByInstitutionId(account.getInstitutionId()).stream()
				.map(tag -> getTagApiResponseFactory().create(tag))
				.collect(Collectors.toList());

		return new ApiResponse(new HashMap<String, Object>() {{
			put("tagGroups", tagGroups);
			put("tags", tags);
		}});
	}
	@GET("/admin/content-statuses")
	@AuthenticationRequired
	public ApiResponse getContentStatuses() {
		Account account = getCurrentContext().getAccount().get();
		if (account.getRoleId() == RoleId.ADMINISTRATOR) {
			return new ApiResponse(Map.of(
					"contentStatuses", getAdminContentService().findContentStatuses().stream().
							map(it -> getContentStatusApiResponseFactory().create(it)).collect(Collectors.toList())
			));
		} else {
			return new ApiResponse(Map.of("contentStatuses", emptyList()));
		}
	}

	@POST("/admin/content/")
	@AuthenticationRequired
	public ApiResponse createContent(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		Account account = getCurrentContext().getAccount().get();
		CreateContentRequest request = getRequestBodyParser().parse(requestBody, CreateContentRequest.class);
		AdminContent adminContent = getAdminContentService().createContent(account, request);
		List<UUID> institutionContentIds = getAdminContentService().findContentIdsForInstitution(account.getInstitutionId());
		return new ApiResponse(new HashMap<String, Object>() {{
			put("adminContent", getAdminContentApiResponseFactory().create(account, adminContent, AdminContentDisplayType.DETAIL, institutionContentIds));
		}});
	}

	@PUT("/admin/content/{contentId}")
	@AuthenticationRequired
	public ApiResponse updateContent(@Nonnull @PathParameter UUID contentId,
																	 @Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);
		requireNonNull(contentId);

		Account account = getCurrentContext().getAccount().get();
		UpdateContentRequest request = getRequestBodyParser().parse(requestBody, UpdateContentRequest.class);
		request.setContentId(contentId);
		AdminContent adminContent = getAdminContentService().updateContent(account, request);

		AdminContentDisplayType adminContentDisplayType = AdminContentDisplayType.DETAIL;
		List<UUID> institutionContentIds = getAdminContentService().findContentIdsForInstitution(account.getInstitutionId());
		return new ApiResponse(new HashMap<String, Object>() {{
			put("adminContent", getAdminContentApiResponseFactory().create(account, adminContent, adminContentDisplayType, institutionContentIds));
		}});
	}

	@GET("/admin/content/{contentId}")
	@AuthenticationRequired
	public ApiResponse getContentById(@Nonnull @PathParameter UUID contentId) {
		requireNonNull(contentId);

		Account account = getCurrentContext().getAccount().get();
		Optional<AdminContent> content = getAdminContentService().findAdminContentByIdForInstitution(account.getInstitutionId(), contentId);

		if (!content.isPresent())
			throw new NotFoundException();

		List<UUID> institutionContentIds = getAdminContentService().findContentIdsForInstitution(account.getInstitutionId());
		return new ApiResponse(new HashMap<String, Object>() {{
			put("content", getAdminContentApiResponseFactory().create(account, content.get(), AdminContentDisplayType.DETAIL, institutionContentIds));
		}});

	}

	@DELETE("/admin/content/{contentId}")
	@AuthenticationRequired
	public ApiResponse deleteContent(@Nonnull @PathParameter UUID contentId) {
		requireNonNull(contentId);
		Account account = getCurrentContext().getAccount().get();
		Optional<AdminContent> content = getAdminContentService().findAdminContentByIdForInstitution(account.getInstitutionId(), contentId);

		if (!content.isPresent())
			throw new NotFoundException();
		else if (!getAdminContentService().hasAdminAccessToContent(account, content.get()))
			throw new AuthorizationException();

		getAdminContentService().deleteContentById(contentId);

		return new ApiResponse(new HashMap<String, Object>() {{
			put("contentId", contentId);
		}});

	}

	@Nonnull
	@POST("/admin/content/image-presigned-upload")
	@AuthenticationRequired
	public ApiResponse createContentImagePresignedUpload(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		Account account = getCurrentContext().getAccount().get();

		CreatePresignedUploadRequest request = getRequestBodyParser().parse(requestBody, CreatePresignedUploadRequest.class);
		request.setAccountId(account.getAccountId());

		PresignedUpload presignedUpload = getImageUploadService().generatePresignedUploadForContent(request);

		return new ApiResponse(new HashMap<String, Object>() {{
			put("presignedUpload", getPresignedUploadApiResponseFactory().create(presignedUpload));
		}});
	}

	@Nonnull
	@POST("/admin/content/file-presigned-upload")
	@AuthenticationRequired
	public ApiResponse createFileImagePresignedUpload(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		Account account = getCurrentContext().getAccount().get();

		CreatePresignedUploadRequest request = getRequestBodyParser().parse(requestBody, CreatePresignedUploadRequest.class);
		request.setAccountId(account.getAccountId());

		PresignedUpload presignedUpload = getImageUploadService().generatePresignedUploadForContent(request);

		return new ApiResponse(new HashMap<String, Object>() {{
			put("presignedUpload", getPresignedUploadApiResponseFactory().create(presignedUpload));
		}});
	}
	@Nonnull
	@POST("/admin/content/{contentId}/add")
	@AuthenticationRequired
	public ApiResponse addContent(@Nonnull @PathParameter UUID contentId){
		Account account = getCurrentContext().getAccount().get();

		getAdminContentService().addContentToInstitution(contentId, account);
		Optional<AdminContent> content = getAdminContentService()
				.findAdminContentByIdForInstitution(account.getInstitutionId(), contentId);
		List<UUID> institutionContentIds = getAdminContentService().findContentIdsForInstitution(account.getInstitutionId());
		return new ApiResponse(new HashMap<String, Object>() {{
			put("content", getAdminContentApiResponseFactory().create(account, content.get(), AdminContentDisplayType.DETAIL,institutionContentIds));
		}});
	}

	@Nonnull
	@DELETE("/admin/content/{contentId}/remove")
	@AuthenticationRequired
	public ApiResponse removeContent(@Nonnull @PathParameter UUID contentId){
		Account account = getCurrentContext().getAccount().get();

		getAdminContentService().removeContentFromInstitution(contentId, account);
		return new ApiResponse(new HashMap<String, Object>() {{
			put("contentId", contentId);
		}});
	}

	@Nonnull
	@PUT("/admin/content/{contentId}/force-expire")
	@AuthenticationRequired
	public ApiResponse forceExpireContent(@Nonnull @PathParameter UUID contentId){
		Account account = getCurrentContext().getAccount().get();
		Optional<AdminContent> content = getAdminContentService()
				.findAdminContentByIdForInstitution(account.getInstitutionId(), contentId);
		List<UUID> institutionContentIds = getAdminContentService().findContentIdsForInstitution(account.getInstitutionId());

		getAdminContentService().forceExpireContent(contentId, account);
		return new ApiResponse(new HashMap<String, Object>() {{
			put("content", getAdminContentApiResponseFactory().create(account, content.get(), AdminContentDisplayType.DETAIL,institutionContentIds));
		}});
	}

	@Nonnull
	@PUT("/admin/content/{contentId}/publish")
	@AuthenticationRequired
	public ApiResponse publishContent(@Nonnull @PathParameter UUID contentId){
		Account account = getCurrentContext().getAccount().get();

		getAdminContentService().publishContent(contentId, account);
		Optional<AdminContent> content = getAdminContentService()
				.findAdminContentByIdForInstitution(account.getInstitutionId(), contentId);
		List<UUID> institutionContentIds = getAdminContentService().findContentIdsForInstitution(account.getInstitutionId());
		return new ApiResponse(new HashMap<String, Object>() {{
			put("content", getAdminContentApiResponseFactory().create(account, content.get(), AdminContentDisplayType.DETAIL,institutionContentIds));
		}});
	}

	@Nonnull
	protected ContentService getContentService() {
		return contentService;
	}

	@Nonnull
	protected AdminContentService getAdminContentService() {
		return adminContentService;
	}

	@Nonnull
	protected RequestBodyParser getRequestBodyParser() {
		return requestBodyParser;
	}

	@Nonnull
	protected ContentApiResponseFactory getContentApiResponseFactory() {
		return contentApiResponseFactory;
	}

	@Nonnull
	protected CurrentContext getCurrentContext() {
		return currentContextProvider.get();
	}

	@Nonnull
	protected AdminContentApiResponseFactory getAdminContentApiResponseFactory() {
		return adminContentApiResponseFactory;
	}

	@Nonnull
	protected InstitutionService getInstitutionService() {
		return institutionService;
	}

	@Nonnull
	protected AdminInstitutionApiResponseFactory getAdminInstitutionApiResponseFactory() {
		return adminInstitutionApiResponseFactory;
	}

	@Nonnull
	protected ImageUploadService getImageUploadService() {
		return imageUploadServiceProvider.get();
	}

	@Nonnull
	protected PresignedUploadApiResponseFactory getPresignedUploadApiResponseFactory() {
		return presignedUploadApiResponseFactoryProvider.get();
	}

	@Nonnull
	protected AssessmentService getAssessmentService() {
		return assessmentServiceProvider.get();
	}

	@Nonnull
	protected AssessmentFormApiResponseFactory getAssessmentFormApiResponseFactory() {
		return assessmentFormApiResponseFactoryProvider.get();
	}

	@Nonnull
	protected TagService getTagService() {
		return this.tagService;
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
	protected ContentStatusApiResponseFactory getContentStatusApiResponseFactory() {
		return contentStatusApiResponseFactory;
	}
}