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
import com.cobaltplatform.api.model.api.request.CreateGroupSessionRequestRequest;
import com.cobaltplatform.api.model.api.request.CreatePresignedUploadRequest;
import com.cobaltplatform.api.model.api.request.FindGroupSessionRequestsRequest;
import com.cobaltplatform.api.model.api.request.FindGroupSessionRequestsRequest.FilterBehavior;
import com.cobaltplatform.api.model.api.request.UpdateGroupSessionRequestRequest;
import com.cobaltplatform.api.model.api.request.UpdateGroupSessionRequestStatusRequest;
import com.cobaltplatform.api.model.api.response.GroupSessionRequestApiResponse.GroupSessionRequestApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PresignedUploadApiResponse.PresignedUploadApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.GroupSessionRequest;
import com.cobaltplatform.api.model.db.GroupSessionRequestStatus.GroupSessionRequestStatusId;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.model.service.FindResult;
import com.cobaltplatform.api.model.service.PresignedUpload;
import com.cobaltplatform.api.service.AuditLogService;
import com.cobaltplatform.api.service.AuthorizationService;
import com.cobaltplatform.api.service.GroupSessionService;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.JsonMapper;
import com.cobaltplatform.api.web.request.RequestBodyParser;
import com.lokalized.Strings;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * @author Transmogrify, LLC.
 */
@Resource
@Singleton
@ThreadSafe
public class GroupSessionRequestResource {
	@Nonnull
	private final GroupSessionService groupSessionService;
	@Nonnull
	private final GroupSessionRequestApiResponseFactory groupSessionRequestApiResponseFactory;
	@Nonnull
	private final PresignedUploadApiResponseFactory presignedUploadApiResponseFactory;
	@Nonnull
	private final RequestBodyParser requestBodyParser;
	@Nonnull
	private final Formatter formatter;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final Logger logger;
	@Nonnull
	private final AuditLogService auditLogService;
	@Nonnull
	private final JsonMapper jsonMapper;
	@Nonnull
	private final Provider<AuthorizationService> authorizationServiceProvider;

	@Inject
	public GroupSessionRequestResource(@Nonnull GroupSessionService groupSessionService,
																		 @Nonnull GroupSessionRequestApiResponseFactory groupSessionRequestApiResponseFactory,
																		 @Nonnull PresignedUploadApiResponseFactory presignedUploadApiResponseFactory,
																		 @Nonnull RequestBodyParser requestBodyParser,
																		 @Nonnull Formatter formatter,
																		 @Nonnull Strings strings,
																		 @Nonnull Provider<CurrentContext> currentContextProvider,
																		 @Nonnull AuditLogService auditLogService,
																		 @Nonnull JsonMapper jsonMapper,
																		 @Nonnull Provider<AuthorizationService> authorizationServiceProvider) {
		requireNonNull(groupSessionService);
		requireNonNull(groupSessionRequestApiResponseFactory);
		requireNonNull(presignedUploadApiResponseFactory);
		requireNonNull(requestBodyParser);
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(currentContextProvider);
		requireNonNull(auditLogService);
		requireNonNull(jsonMapper);
		requireNonNull(authorizationServiceProvider);

		this.groupSessionService = groupSessionService;
		this.groupSessionRequestApiResponseFactory = groupSessionRequestApiResponseFactory;
		this.presignedUploadApiResponseFactory = presignedUploadApiResponseFactory;
		this.requestBodyParser = requestBodyParser;
		this.formatter = formatter;
		this.strings = strings;
		this.currentContextProvider = currentContextProvider;
		this.logger = LoggerFactory.getLogger(getClass());
		this.auditLogService = auditLogService;
		this.jsonMapper = jsonMapper;
		this.authorizationServiceProvider = authorizationServiceProvider;
	}

	public enum GroupSessionRequestViewType {
		ADMINISTRATOR,
		PATIENT
	}

	@Nonnull
	@GET("/group-session-requests")
	@AuthenticationRequired
	public ApiResponse groupSessionRequests(@Nonnull @QueryParameter Optional<Integer> pageNumber,
																					@Nonnull @QueryParameter Optional<Integer> pageSize,
																					@Nonnull @QueryParameter Optional<String> urlName,
																					@Nonnull @QueryParameter Optional<String> searchQuery,
																					@Nonnull @QueryParameter Optional<GroupSessionRequestViewType> viewType) {
		requireNonNull(pageNumber);
		requireNonNull(pageSize);
		requireNonNull(urlName);
		requireNonNull(searchQuery);
		requireNonNull(viewType);

		Account account = getCurrentContext().getAccount().get();

		FindGroupSessionRequestsRequest request = new FindGroupSessionRequestsRequest();
		request.setInstitutionId(account.getInstitutionId());  // TODO: for superadmins, don't include this
		request.setPageNumber(pageNumber.orElse(null));
		request.setPageSize(pageSize.orElse(null));
		request.setUrlName(urlName.orElse(null));
		request.setSearchQuery(searchQuery.orElse(null));
		request.setFilterBehavior(FilterBehavior.DEFAULT);

		GroupSessionRequestViewType finalViewType = viewType.orElse(GroupSessionRequestViewType.PATIENT);

		// For admin views, real admins can see everything.  But patients can only see their own
		if (finalViewType == GroupSessionRequestViewType.ADMINISTRATOR) {
			if (account.getRoleId() != RoleId.ADMINISTRATOR) {
				request.setFilterBehavior(FilterBehavior.ONLY_MY_SESSIONS);
				request.setAccount(account);
			}
		} else {
			// Only show 'added' sessions for patient views no matter what your role is
			request.setGroupSessionRequestStatusId(GroupSessionRequestStatusId.ADDED);
		}

		FindResult<GroupSessionRequest> findResult = getGroupSessionService().findGroupSessionRequests(request);

		return new ApiResponse(new LinkedHashMap<String, Object>() {{
			put("totalCount", findResult.getTotalCount());
			put("totalCountDescription", getFormatter().formatNumber(findResult.getTotalCount()));
			put("groupSessionRequests", findResult.getResults().stream()
					.map(groupSessionRequest -> getGroupSessionRequestApiResponseFactory().create(groupSessionRequest))
					.collect(Collectors.toList()));
		}});
	}

	@Nonnull
	@GET("/group-session-requests/{groupSessionRequestId}")
	@AuthenticationRequired
	public ApiResponse groupSessionRequest(@Nonnull @PathParameter UUID groupSessionRequestId) {
		requireNonNull(groupSessionRequestId);

		GroupSessionRequest groupSessionRequest = getGroupSessionService().findGroupSessionRequestById(groupSessionRequestId).orElse(null);

		if (groupSessionRequest == null)
			throw new NotFoundException();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("groupSessionRequest", getGroupSessionRequestApiResponseFactory().create(groupSessionRequest));
		}});
	}

	@Nonnull
	@POST("/group-session-requests")
	@AuthenticationRequired
	public ApiResponse createGroupSessionRequest(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		Account account = getCurrentContext().getAccount().get();

		CreateGroupSessionRequestRequest request = getRequestBodyParser().parse(requestBody, CreateGroupSessionRequestRequest.class);
		request.setInstitutionId(account.getInstitutionId());
		request.setSubmitterAccountId(account.getAccountId());

		UUID groupSessionRequestId = getGroupSessionService().createGroupSessionRequest(request);
		GroupSessionRequest groupSessionRequest = getGroupSessionService().findGroupSessionRequestById(groupSessionRequestId).get();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("groupSessionRequest", getGroupSessionRequestApiResponseFactory().create(groupSessionRequest));
		}});
	}

	@Nonnull
	@POST("/group-session-requests/image-presigned-upload")
	@AuthenticationRequired
	public ApiResponse createGroupSessionRequestImagePresignedUpload(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		Account account = getCurrentContext().getAccount().get();

		CreatePresignedUploadRequest request = getRequestBodyParser().parse(requestBody, CreatePresignedUploadRequest.class);
		request.setAccountId(account.getAccountId());

		PresignedUpload presignedUpload = getGroupSessionService().generatePresignedUploadForGroupSessionRequest(request);

		return new ApiResponse(new HashMap<String, Object>() {{
			put("presignedUpload", getPresignedUploadApiResponseFactory().create(presignedUpload));
		}});
	}

	@Nonnull
	@PUT("/group-session-requests/{groupSessionRequestId}")
	@AuthenticationRequired
	public ApiResponse updateGroupSessionRequest(@Nonnull @PathParameter UUID groupSessionRequestId,
																							 @Nonnull @RequestBody String requestBody) {
		requireNonNull(groupSessionRequestId);
		requireNonNull(requestBody);

		Account account = getCurrentContext().getAccount().get();
		GroupSessionRequest groupSessionRequest = getGroupSessionService().findGroupSessionRequestById(groupSessionRequestId).orElse(null);

		if (groupSessionRequest == null)
			throw new NotFoundException();

		if (!getAuthorizationService().canEditGroupSessionRequest(groupSessionRequest, account))
			throw new AuthorizationException();

		UpdateGroupSessionRequestRequest request = getRequestBodyParser().parse(requestBody, UpdateGroupSessionRequestRequest.class);
		request.setGroupSessionRequestId(groupSessionRequestId);

		getGroupSessionService().updateGroupSessionRequest(request);
		GroupSessionRequest updatedGroupSessionRequest = getGroupSessionService().findGroupSessionRequestById(groupSessionRequestId).get();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("groupSessionRequest", getGroupSessionRequestApiResponseFactory().create(updatedGroupSessionRequest));
		}});
	}

	@Nonnull
	@PUT("/group-session-requests/{groupSessionRequestId}/status")
	@AuthenticationRequired
	public ApiResponse updateGroupSessionRequestStatus(@Nonnull @PathParameter UUID groupSessionRequestId,
																										 @Nonnull @RequestBody String requestBody) {
		requireNonNull(groupSessionRequestId);
		requireNonNull(requestBody);

		Account account = getCurrentContext().getAccount().get();
		GroupSessionRequest groupSessionRequest = getGroupSessionService().findGroupSessionRequestById(groupSessionRequestId).orElse(null);

		if (groupSessionRequest == null)
			throw new NotFoundException();

		if (!getAuthorizationService().canEditGroupSessionRequestStatus(groupSessionRequest, account))
			throw new AuthorizationException();

		UpdateGroupSessionRequestStatusRequest request = getRequestBodyParser().parse(requestBody, UpdateGroupSessionRequestStatusRequest.class);
		request.setAccountId(account.getAccountId());
		request.setGroupSessionRequestId(groupSessionRequestId);

		getGroupSessionService().updateGroupSessionRequestStatus(request);

		GroupSessionRequest updatedGroupSessionRequest = getGroupSessionService().findGroupSessionRequestById(groupSessionRequestId).orElse(null);

		// "Deleted" case
		if (updatedGroupSessionRequest == null)
			return null;

		return new ApiResponse(new HashMap<String, Object>() {{
			put("groupSessionRequest", getGroupSessionRequestApiResponseFactory().create(updatedGroupSessionRequest));
		}});
	}

	@Nonnull
	protected GroupSessionService getGroupSessionService() {
		return groupSessionService;
	}

	@Nonnull
	protected GroupSessionRequestApiResponseFactory getGroupSessionRequestApiResponseFactory() {
		return groupSessionRequestApiResponseFactory;
	}

	@Nonnull
	protected PresignedUploadApiResponseFactory getPresignedUploadApiResponseFactory() {
		return presignedUploadApiResponseFactory;
	}

	@Nonnull
	protected RequestBodyParser getRequestBodyParser() {
		return requestBodyParser;
	}

	@Nonnull
	protected Formatter getFormatter() {
		return formatter;
	}

	@Nonnull
	protected Strings getStrings() {
		return strings;
	}

	@Nonnull
	protected CurrentContext getCurrentContext() {
		return currentContextProvider.get();
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}

	@Nonnull
	protected AuditLogService getAuditLogService() {
		return auditLogService;
	}

	@Nonnull
	protected JsonMapper getJsonMapper() {
		return jsonMapper;
	}

	@Nonnull
	protected AuthorizationService getAuthorizationService() {
		return authorizationServiceProvider.get();
	}
}