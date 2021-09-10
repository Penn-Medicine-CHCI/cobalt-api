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
import com.cobaltplatform.api.model.api.request.UpdateContentApprovalStatusRequest;
import com.cobaltplatform.api.model.api.request.UpdateContentArchivedStatus;
import com.cobaltplatform.api.model.api.request.UpdateContentRequest;
import com.cobaltplatform.api.model.api.response.AdminAvailableContentApiResponse.AdminAvailableContentApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AdminInstitutionApiResponse.AdminInstitutionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ContentApiResponse.ContentApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AdminContentApiResponse.AdminContentApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PresignedUploadApiResponse.PresignedUploadApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.ApprovalStatus;
import com.cobaltplatform.api.model.db.AvailableStatus;
import com.cobaltplatform.api.model.db.ContentType;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Role;
import com.cobaltplatform.api.model.db.Visibility;
import com.cobaltplatform.api.model.db.assessment.Assessment;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.model.service.AdminContent;
import com.cobaltplatform.api.model.service.FindResult;
import com.cobaltplatform.api.service.AssessmentService;
import com.cobaltplatform.api.service.ContentService;
import com.cobaltplatform.api.service.ImageUploadService;
import com.cobaltplatform.api.service.InstitutionService;
import com.cobaltplatform.api.util.UploadManager.PresignedUpload;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.cobaltplatform.api.model.api.response.AdminContentApiResponse.*;
import static com.cobaltplatform.api.model.api.response.AssessmentFormApiResponse.*;
import static com.cobaltplatform.api.model.db.assessment.Assessment.AssessmentType.*;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
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
	private final RequestBodyParser requestBodyParser;
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final ContentApiResponseFactory contentApiResponseFactory;
	@Nonnull
	private final AdminContentApiResponseFactory adminContentApiResponseFactory;
	@Nonnull
	private final AdminAvailableContentApiResponseFactory adminAvailableContentApiResponseFactory;
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


	@Inject
	public AdminResource(@Nonnull ContentService contentService,
											 @Nonnull RequestBodyParser requestBodyParser,
											 @Nonnull Provider<CurrentContext> currentContextProvider,
											 @Nonnull ContentApiResponseFactory contentApiResponseFactory,
											 @Nonnull AdminContentApiResponseFactory adminContentApiResponseFactory,
											 @Nonnull AdminAvailableContentApiResponseFactory adminAvailableContentApiResponseFactory,
											 @Nonnull InstitutionService institutionService,
											 @Nonnull AdminInstitutionApiResponseFactory adminInstitutionApiResponseFactory,
											 @Nonnull Provider<ImageUploadService> imageUploadServiceProvider,
											 @Nonnull Provider<PresignedUploadApiResponseFactory> presignedUploadApiResponseFactoryProvider,
											 @Nonnull Provider<AssessmentService> assessmentServiceProvider,
											 @Nonnull Provider<AssessmentFormApiResponseFactory> assessmentFormApiResponseFactoryProvider) {
		this.contentService = contentService;
		this.requestBodyParser = requestBodyParser;
		this.currentContextProvider = currentContextProvider;
		this.contentApiResponseFactory = contentApiResponseFactory;
		this.adminContentApiResponseFactory = adminContentApiResponseFactory;
		this.institutionService = institutionService;
		this.adminInstitutionApiResponseFactory = adminInstitutionApiResponseFactory;
		this.adminAvailableContentApiResponseFactory = adminAvailableContentApiResponseFactory;
		this.imageUploadServiceProvider = imageUploadServiceProvider;
		this.presignedUploadApiResponseFactoryProvider = presignedUploadApiResponseFactoryProvider;
		this.assessmentServiceProvider = assessmentServiceProvider;
		this.assessmentFormApiResponseFactoryProvider = assessmentFormApiResponseFactoryProvider;
	}

	@GET("/admin/my-content/filter")
	@AuthenticationRequired
	public ApiResponse getMyContentFilter() {
		return new ApiResponse(new HashMap<String, Object>() {{
			put("contentTypes", getContentService().findContentTypes());
			put("institutions", getInstitutionService().findInstitutions().stream().
					map(it -> getAdminInstitutionApiResponseFactory().create(it)).collect(Collectors.toList()));
			put("myApprovalStatuses", getContentService().findApprovalStatuses());
			put("otherApprovalStatuses", getContentService().findApprovalStatuses());
		}});
	}

	@GET("/admin/my-content")
	@AuthenticationRequired
	public ApiResponse getMyContent(@QueryParameter Optional<Integer> page,
																	@QueryParameter Optional<ContentType.ContentTypeId> contentTypeId,
																	@QueryParameter Optional<Institution.InstitutionId> institutionId,
																	@QueryParameter Optional<Visibility.VisibilityId> visibilityId,
																	@QueryParameter Optional<ApprovalStatus.ApprovalStatusId> myApprovalStatusId,
																	@QueryParameter Optional<ApprovalStatus.ApprovalStatusId> otherApprovalStatusId,
																	@QueryParameter Optional<String> search) {
		Account account = getCurrentContext().getAccount().get();

		//TODO: create a filter object to pass all the query params
		FindResult<AdminContent> content = getContentService()
				.findAllContentForAccount(true, account, page, contentTypeId, institutionId, visibilityId, myApprovalStatusId, otherApprovalStatusId, Optional.empty(), search);

		return new ApiResponse(new HashMap<String, Object>() {{
			put("adminContent", content.getResults().stream().map(content -> getAdminContentApiResponseFactory().create(account, content, AdminContentDisplayType.MY_CONTENT)).collect(Collectors.toList()));
			put("totalCount", content.getTotalCount());
		}});
	}

	@GET("/admin/available-content/filter")
	@AuthenticationRequired
	public ApiResponse getAvailableContentFilter() {
		return new ApiResponse(new HashMap<String, Object>() {{
			put("contentTypes", getContentService().findContentTypes());
			put("availableStatuses", getContentService().findAvailableStatuses());
		}});
	}

	@GET("/admin/available-content")
	@AuthenticationRequired
	public ApiResponse getAvailableContent(@QueryParameter Optional<Integer> page,
																				 @QueryParameter Optional<ContentType.ContentTypeId> contentTypeId,
																				 @QueryParameter Optional<AvailableStatus.AvailableStatusId> availableStatusId,
																				 @QueryParameter Optional<String> search) {
		Account account = getCurrentContext().getAccount().get();
		if (account.getRoleId() == Role.RoleId.SUPER_ADMINISTRATOR)
			throw new AuthorizationException();

		FindResult<AdminContent> content = getContentService()
				.findAllContentForAccount(false, account, page, contentTypeId, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), availableStatusId, search);

		return new ApiResponse(new HashMap<String, Object>() {{
			put("adminContent", content.getResults().stream().map(content -> getAdminContentApiResponseFactory().create(account, content, AdminContentDisplayType.AVAILABLE_CONTENT)).collect(Collectors.toList()));
			put("totalCount", content.getTotalCount());
		}});
	}

	@GET("/admin/content-tags")
	@AuthenticationRequired
	public ApiResponse tagsForContent() {
		Account account = getCurrentContext().getAccount().get();
		if (account.getRoleId() == Role.RoleId.ADMINISTRATOR) {

			Assessment assessment = getAssessmentService().findAssessmentByTypeForUser(INTRO, account).orElseThrow();
			return new ApiResponse(Map.of(
					"contentTags", getAssessmentFormApiResponseFactory().create(assessment, Optional.empty(), AssessmentFormApiResponseType.CMS)
			));
		} else {
			return new ApiResponse(Map.of(
					"contentTags", emptyMap()
			));
		}

	}

	@GET("/admin/content-institutions")
	@AuthenticationRequired
	public ApiResponse getInNetworkInstitutions() {
		Account account = getCurrentContext().getAccount().get();
		if(account.getRoleId() == Role.RoleId.SUPER_ADMINISTRATOR || account.getRoleId() == Role.RoleId.ADMINISTRATOR) {
			return new ApiResponse(Map.of(
					"institutions", getInstitutionService().findNetworkInstitutions(account.getInstitutionId()).stream().
							map(it -> getAdminInstitutionApiResponseFactory().create(it)).collect(Collectors.toList())
			));
		} else {
			return new ApiResponse(Map.of("institutions", emptyList()));
		}
	}

	@GET("/admin/content-type-labels")
	@AuthenticationRequired
	public ApiResponse getContentTypeLabels() {
		Account account = getCurrentContext().getAccount().get();
		return new ApiResponse(Map.of(
				"contentTypeLabels", getContentService().findContentTypeLabels()
		));
	}

	@POST("/admin/content/")
	@AuthenticationRequired
	public ApiResponse createContent(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		Account account = getCurrentContext().getAccount().get();
		CreateContentRequest request = getRequestBodyParser().parse(requestBody, CreateContentRequest.class);
		AdminContent adminContent = getContentService().createContent(account, request);

		return new ApiResponse(new HashMap<String, Object>() {{
			put("adminContent", getAdminContentApiResponseFactory().create(account, adminContent, AdminContentDisplayType.DETAIL));
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
		AdminContent adminContent = getContentService().updateContent(account, request);
		AdminContentDisplayType adminContentDisplayType = (request.getRemoveFromInstitution() == null || !request.getRemoveFromInstitution() ) ?
				AdminContentDisplayType.DETAIL : AdminContentDisplayType.AVAILABLE_CONTENT;

		return new ApiResponse(new HashMap<String, Object>() {{
			put("adminContent", getAdminContentApiResponseFactory().create(account, adminContent, adminContentDisplayType));
		}});
	}


	@GET("/admin/content/{contentId}")
	@AuthenticationRequired
	public ApiResponse getContentById(@Nonnull @PathParameter UUID contentId) {
		requireNonNull(contentId);

		Account account = getCurrentContext().getAccount().get();
		Optional<AdminContent> content = getContentService().findAdminContentByIdForInstitution(account.getInstitutionId(), contentId);

		if (!content.isPresent())
			throw new NotFoundException();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("networkInstitutions", getInstitutionService().findNetworkInstitutions(account.getInstitutionId()).stream().
					map(it -> getAdminInstitutionApiResponseFactory().create(it)).collect(Collectors.toList()));
			put("content", getAdminContentApiResponseFactory().create(account, content.get(), AdminContentDisplayType.DETAIL));
		}});

	}

	@PUT("/admin/content/{contentId}/approval-status")
	@AuthenticationRequired
	public ApiResponse updateApprovalStatus(@Nonnull @RequestBody String requestBody,
																					@Nonnull @PathParameter UUID contentId) {
		requireNonNull(requestBody);
		requireNonNull(contentId);
		Account account = getCurrentContext().getAccount().get();
		UpdateContentApprovalStatusRequest request = getRequestBodyParser().parse(requestBody, UpdateContentApprovalStatusRequest.class);
		Optional<AdminContent> content = getContentService().findAdminContentByIdForInstitution(account.getInstitutionId(), contentId);

		if (!content.isPresent())
			throw new NotFoundException();
		else if (!getContentService().hasAdminAccessToContent(account, content.get()))
			throw new AuthorizationException();

		request.setContentId(contentId);
		getContentService().updateContentVisibilityApprovalStatusForAccount(account, request);
		AdminContent adminContent = contentService.findAdminContentByIdForInstitution(account.getInstitutionId(), contentId).get();

		AdminContentDisplayType displayType;
		if(adminContent.getOwnerInstitutionId() == account.getInstitutionId()){
			displayType = AdminContentDisplayType.MY_CONTENT;
		} else {
			displayType = AdminContentDisplayType.AVAILABLE_CONTENT;
		}

		return new ApiResponse(new HashMap<String, Object>() {{
			put("content", getAdminContentApiResponseFactory().create(account, adminContent, displayType));
		}});

	}

	@DELETE("/admin/content/{contentId}")
	@AuthenticationRequired
	public ApiResponse deleteContent(@Nonnull @PathParameter UUID contentId) {
		requireNonNull(contentId);
		Account account = getCurrentContext().getAccount().get();
		Optional<AdminContent> content = getContentService().findAdminContentByIdForInstitution(account.getInstitutionId(), contentId);

		if (!content.isPresent())
			throw new NotFoundException();
		else if (!getContentService().hasAdminAccessToContent(account, content.get()))
			throw new AuthorizationException();

		getContentService().deleteContentById(contentId);

		return new ApiResponse(new HashMap<String, Object>() {{
			put("contentId", contentId);
		}});

	}

	@PUT("/admin/content/{contentId}/archive")
	@AuthenticationRequired
	public ApiResponse archiveContent(@Nonnull @RequestBody String requestBody,
																		@Nonnull @PathParameter UUID contentId) {
		requireNonNull(requestBody);
		requireNonNull(contentId);
		Account account = getCurrentContext().getAccount().get();
		UpdateContentArchivedStatus request = getRequestBodyParser().parse(requestBody, UpdateContentArchivedStatus.class);
		Optional<AdminContent> content = getContentService().findAdminContentByIdForInstitution(account.getInstitutionId(), contentId);

		if (!content.isPresent())
			throw new NotFoundException();
		else if (!getContentService().hasAdminAccessToContent(account, content.get()))
			throw new AuthorizationException();

		request.setContentId(contentId);
		getContentService().updateArchiveFlagContentById(request);

		return new ApiResponse(new HashMap<String, Object>() {{
			put("content", getAdminContentApiResponseFactory().create(account, contentService.findAdminContentByIdForInstitution(account.getInstitutionId(), contentId).get(), AdminContentDisplayType.MY_CONTENT));
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
	protected ContentService getContentService() {
		return contentService;
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
	protected AdminAvailableContentApiResponseFactory getAdminAvailableContentApiResponseFactory() {
		return adminAvailableContentApiResponseFactory;
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

}

