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

import com.lokalized.Strings;
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.api.request.CreateGroupSessionRequest;
import com.cobaltplatform.api.model.api.request.CreatePresignedUploadRequest;
import com.cobaltplatform.api.model.api.request.FindGroupSessionsRequest;
import com.cobaltplatform.api.model.api.request.FindGroupSessionsRequest.FilterBehavior;
import com.cobaltplatform.api.model.api.request.UpdateGroupSessionRequest;
import com.cobaltplatform.api.model.api.request.UpdateGroupSessionStatusRequest;
import com.cobaltplatform.api.model.api.response.GroupSessionApiResponse.GroupSessionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.GroupSessionReservationApiResponse.GroupSessionReservationApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PresignedUploadApiResponse.PresignedUploadApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.GroupSession;
import com.cobaltplatform.api.model.db.GroupSessionReservation;
import com.cobaltplatform.api.model.db.GroupSessionStatus.GroupSessionStatusId;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.model.service.FindResult;
import com.cobaltplatform.api.model.service.GroupSessionStatusWithCount;
import com.cobaltplatform.api.service.AuditLogService;
import com.cobaltplatform.api.service.AuthorizationService;
import com.cobaltplatform.api.service.GroupSessionService;
import com.cobaltplatform.api.service.ImageUploadService;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.JsonMapper;
import com.cobaltplatform.api.util.UploadManager.PresignedUpload;
import com.cobaltplatform.api.web.request.RequestBodyParser;
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
import java.util.List;
import java.util.Map;
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
public class GroupSessionResource {
	@Nonnull
	private final GroupSessionService groupSessionService;
	@Nonnull
	private final GroupSessionApiResponseFactory groupSessionApiResponseFactory;
	@Nonnull
	private final GroupSessionReservationApiResponseFactory groupSessionReservationApiResponseFactory;
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
	private final Provider<ImageUploadService> imageUploadServiceProvider;
	@Nonnull
	private final Provider<AuthorizationService> authorizationServiceProvider;

	@Inject
	public GroupSessionResource(@Nonnull GroupSessionService groupSessionService,
															@Nonnull GroupSessionApiResponseFactory groupSessionApiResponseFactory,
															@Nonnull GroupSessionReservationApiResponseFactory groupSessionReservationApiResponseFactory,
															@Nonnull PresignedUploadApiResponseFactory presignedUploadApiResponseFactory,
															@Nonnull RequestBodyParser requestBodyParser,
															@Nonnull Formatter formatter,
															@Nonnull Strings strings,
															@Nonnull Provider<CurrentContext> currentContextProvider,
															@Nonnull AuditLogService auditLogService,
															@Nonnull JsonMapper jsonMapper,
															@Nonnull Provider<ImageUploadService> imageUploadServiceProvider,
															@Nonnull Provider<AuthorizationService> authorizationServiceProvider) {
		requireNonNull(groupSessionService);
		requireNonNull(groupSessionApiResponseFactory);
		requireNonNull(groupSessionReservationApiResponseFactory);
		requireNonNull(presignedUploadApiResponseFactory);
		requireNonNull(requestBodyParser);
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(currentContextProvider);
		requireNonNull(auditLogService);
		requireNonNull(jsonMapper);
		requireNonNull(authorizationServiceProvider);

		this.groupSessionService = groupSessionService;
		this.groupSessionApiResponseFactory = groupSessionApiResponseFactory;
		this.groupSessionReservationApiResponseFactory = groupSessionReservationApiResponseFactory;
		this.presignedUploadApiResponseFactory = presignedUploadApiResponseFactory;
		this.requestBodyParser = requestBodyParser;
		this.formatter = formatter;
		this.strings = strings;
		this.currentContextProvider = currentContextProvider;
		this.logger = LoggerFactory.getLogger(getClass());
		this.auditLogService = auditLogService;
		this.jsonMapper = jsonMapper;
		this.imageUploadServiceProvider = imageUploadServiceProvider;
		this.authorizationServiceProvider = authorizationServiceProvider;
	}

	public enum GroupSessionViewType {
		ADMINISTRATOR,
		PATIENT
	}

	@Nonnull
	@GET("/group-sessions")
	@AuthenticationRequired
	public ApiResponse groupSessions(@Nonnull @QueryParameter Optional<Integer> pageNumber,
																	 @Nonnull @QueryParameter Optional<Integer> pageSize,
																	 @Nonnull @QueryParameter Optional<GroupSessionViewType> viewType,
																	 @Nonnull @QueryParameter Optional<String> urlName,
																	 @Nonnull @QueryParameter Optional<FindGroupSessionsRequest.OrderBy> orderBy) {
		requireNonNull(pageNumber);
		requireNonNull(pageSize);
		requireNonNull(viewType);
		requireNonNull(urlName);
		requireNonNull(orderBy);

		Account account = getCurrentContext().getAccount().get();

		FindGroupSessionsRequest request = new FindGroupSessionsRequest();
		request.setInstitutionId(account.getInstitutionId());  // TODO: for superadmins, don't include institution ID
		request.setPageNumber(pageNumber.orElse(null));
		request.setPageSize(pageSize.orElse(null));
		request.setUrlName(urlName.orElse(null));
		request.setOrderBy(orderBy.orElse(null));
		request.setFilterBehavior(FilterBehavior.DEFAULT);

		GroupSessionViewType finalViewType = viewType.orElse(GroupSessionViewType.PATIENT);

		// For admin views, real admins can see everything.  But patients can only see their own
		if(finalViewType == GroupSessionViewType.ADMINISTRATOR) {
			if (account.getRoleId() != RoleId.ADMINISTRATOR
					&& account.getRoleId() != RoleId.SUPER_ADMINISTRATOR) {
				request.setFilterBehavior(FilterBehavior.ONLY_MY_SESSIONS);
				request.setAccount(account);
			}
		} else {
			// Only show 'added' sessions for patient views no matter what your role is
			request.setGroupSessionStatusId(GroupSessionStatusId.ADDED);
		}

		FindResult<GroupSession> findResult = getGroupSessionService().findGroupSessions(request);

		return new ApiResponse(new LinkedHashMap<String, Object>() {{
			put("totalCount", findResult.getTotalCount());
			put("totalCountDescription", getFormatter().formatNumber(findResult.getTotalCount()));
			put("groupSessions", findResult.getResults().stream()
					.map(groupSession -> getGroupSessionApiResponseFactory().create(groupSession))
					.collect(Collectors.toList()));
		}});
	}

	@Nonnull
	@GET("/group-sessions/counts")
	@AuthenticationRequired
	public ApiResponse groupSessionCounts() {
		Account account = getCurrentContext().getAccount().get();
		// TODO: for superadmins, don't include institution ID
		List<GroupSessionStatusWithCount> groupSessionStatusesWithCounts = getGroupSessionService().findGroupSessionStatusesWithCounts(account.getInstitutionId());

		return new ApiResponse(new LinkedHashMap<String, Object>() {{
			put("groupSessionCounts", groupSessionStatusesWithCounts.stream()
					.map(groupSessionStatus -> {
						Map<String, Object> json = new LinkedHashMap<>();
						json.put("groupSessionStatusId", groupSessionStatus.getGroupSessionStatusId());
						json.put("groupSessionStatusIdDescription", groupSessionStatus.getGroupSessionStatusIdDescription());
						json.put("totalCount", groupSessionStatus.getTotalCount());
						json.put("totalCountDescription", getFormatter().formatNumber(groupSessionStatus.getTotalCount()));
						return json;
					}).collect(Collectors.toList()));
		}});
	}

	@Nonnull
	@GET("/group-sessions/{groupSessionId}")
	@AuthenticationRequired
	public ApiResponse groupSession(@Nonnull @PathParameter UUID groupSessionId) {
		requireNonNull(groupSessionId);

		Account account = getCurrentContext().getAccount().get();
		GroupSession groupSession = getGroupSessionService().findGroupSessionById(groupSessionId).orElse(null);

		if (groupSession == null)
			throw new NotFoundException();

		List<GroupSessionReservation> groupSessionReservations = getGroupSessionService().findGroupSessionReservationsByGroupSessionId(groupSessionId);
		GroupSessionReservation groupSessionReservation = null;

		for (GroupSessionReservation potentialGroupSessionReservation : groupSessionReservations) {
			if (potentialGroupSessionReservation.getAccountId().equals(account.getAccountId())) {
				groupSessionReservation = potentialGroupSessionReservation;
				break;
			}
		}

		GroupSessionReservation pinnedGroupSessionReservation = groupSessionReservation;

		return new ApiResponse(new HashMap<String, Object>() {{
			put("groupSession", getGroupSessionApiResponseFactory().create(groupSession));

			if (pinnedGroupSessionReservation != null)
				put("groupSessionReservation", getGroupSessionReservationApiResponseFactory().create(pinnedGroupSessionReservation));
		}});
	}

	@Nonnull
	@POST("/group-sessions")
	@AuthenticationRequired
	public ApiResponse createGroupSession(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		Account account = getCurrentContext().getAccount().get();

		CreateGroupSessionRequest request = getRequestBodyParser().parse(requestBody, CreateGroupSessionRequest.class);
		request.setInstitutionId(account.getInstitutionId());
		request.setSubmitterAccountId(account.getAccountId());

		UUID groupSessionId = getGroupSessionService().createGroupSession(request);
		GroupSession groupSession = getGroupSessionService().findGroupSessionById(groupSessionId).get();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("groupSession", getGroupSessionApiResponseFactory().create(groupSession));
		}});
	}

	@Nonnull
	@POST("/group-sessions/image-presigned-upload")
	@AuthenticationRequired
	public ApiResponse createGroupSessionImagePresignedUpload(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		Account account = getCurrentContext().getAccount().get();

		CreatePresignedUploadRequest request = getRequestBodyParser().parse(requestBody, CreatePresignedUploadRequest.class);
		request.setAccountId(account.getAccountId());

		PresignedUpload presignedUpload = getImageUploadService().generatePresignedUploadForGroupSession(request);

		return new ApiResponse(new HashMap<String, Object>() {{
			put("presignedUpload", getPresignedUploadApiResponseFactory().create(presignedUpload));
		}});
	}

	@Nonnull
	@PUT("/group-sessions/{groupSessionId}/status")
	@AuthenticationRequired
	public ApiResponse updateGroupSessionStatus(@Nonnull @PathParameter UUID groupSessionId,
																							@Nonnull @RequestBody String requestBody) {
		requireNonNull(groupSessionId);
		requireNonNull(requestBody);

		Account account = getCurrentContext().getAccount().get();
		GroupSession groupSession = getGroupSessionService().findGroupSessionById(groupSessionId).orElse(null);

		if (groupSession == null)
			throw new NotFoundException();

		if(!getAuthorizationService().canEditGroupSession(groupSession, account))
			throw new AuthorizationException();

		UpdateGroupSessionStatusRequest request = getRequestBodyParser().parse(requestBody, UpdateGroupSessionStatusRequest.class);
		request.setAccountId(account.getAccountId());
		request.setGroupSessionId(groupSessionId);

		getGroupSessionService().updateGroupSessionStatus(request);

		GroupSession updatedGroupSession = getGroupSessionService().findGroupSessionById(groupSessionId).orElse(null);

		// "Deleted" case
		if (updatedGroupSession == null)
			return null;

		return new ApiResponse(new HashMap<String, Object>() {{
			put("groupSession", getGroupSessionApiResponseFactory().create(updatedGroupSession));
		}});
	}

	@Nonnull
	@PUT("/group-sessions/{groupSessionId}")
	@AuthenticationRequired
	public ApiResponse updateGroupSession(@Nonnull @PathParameter UUID groupSessionId,
																				@Nonnull @RequestBody String requestBody) {
		requireNonNull(groupSessionId);
		requireNonNull(requestBody);

		Account account = getCurrentContext().getAccount().get();
		GroupSession groupSession = getGroupSessionService().findGroupSessionById(groupSessionId).orElse(null);

		if (groupSession == null)
			throw new NotFoundException();

		if(!getAuthorizationService().canEditGroupSession(groupSession, account))
			throw new AuthorizationException();

		UpdateGroupSessionRequest request = getRequestBodyParser().parse(requestBody, UpdateGroupSessionRequest.class);
		request.setGroupSessionId(groupSessionId);

		getGroupSessionService().updateGroupSession(request);

		GroupSession updatedGroupSession = getGroupSessionService().findGroupSessionById(groupSessionId).orElse(null);

		return new ApiResponse(new HashMap<String, Object>() {{
			put("groupSession", getGroupSessionApiResponseFactory().create(updatedGroupSession));
		}});
	}

	@Nonnull
	protected GroupSessionService getGroupSessionService() {
		return groupSessionService;
	}

	@Nonnull
	protected GroupSessionApiResponseFactory getGroupSessionApiResponseFactory() {
		return groupSessionApiResponseFactory;
	}

	@Nonnull
	protected GroupSessionReservationApiResponseFactory getGroupSessionReservationApiResponseFactory() {
		return groupSessionReservationApiResponseFactory;
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
	protected ImageUploadService getImageUploadService() {
		return imageUploadServiceProvider.get();
	}

	@Nonnull
	protected AuthorizationService getAuthorizationService() {
		return authorizationServiceProvider.get();
	}
}