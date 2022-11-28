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
import com.cobaltplatform.api.integration.ical.ICalInviteGenerator.InviteMethod;
import com.cobaltplatform.api.model.api.request.CancelGroupSessionReservationRequest;
import com.cobaltplatform.api.model.api.request.CreateGroupSessionReservationRequest;
import com.cobaltplatform.api.model.api.response.AccountApiResponse.AccountApiResponseFactory;
import com.cobaltplatform.api.model.api.response.GroupSessionApiResponse.GroupSessionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.GroupSessionReservationApiResponse.GroupSessionReservationApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.GroupSession;
import com.cobaltplatform.api.model.db.GroupSessionReservation;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.service.AccountService;
import com.cobaltplatform.api.service.AuditLogService;
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
import com.soklet.web.exception.NotFoundException;
import com.soklet.web.response.ApiResponse;
import com.soklet.web.response.CustomResponse;
import com.soklet.web.response.RedirectResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Resource
@Singleton
@ThreadSafe
public class GroupSessionReservationResource {
	@Nonnull
	private final AccountService accountService;
	@Nonnull
	private final GroupSessionService groupSessionService;
	@Nonnull
	private final GroupSessionApiResponseFactory groupSessionApiResponseFactory;
	@Nonnull
	private final GroupSessionReservationApiResponseFactory groupSessionReservationApiResponseFactory;
	@Nonnull
	private final AccountApiResponseFactory accountApiResponseFactory;
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

	@Inject
	public GroupSessionReservationResource(@Nonnull AccountService accountService,
																				 @Nonnull GroupSessionService groupSessionService,
																				 @Nonnull GroupSessionApiResponseFactory groupSessionApiResponseFactory,
																				 @Nonnull GroupSessionReservationApiResponseFactory groupSessionReservationApiResponseFactory,
																				 @Nonnull AccountApiResponseFactory accountApiResponseFactory,
																				 @Nonnull RequestBodyParser requestBodyParser,
																				 @Nonnull Formatter formatter,
																				 @Nonnull Strings strings,
																				 @Nonnull Provider<CurrentContext> currentContextProvider,
																				 @Nonnull AuditLogService auditLogService,
																				 @Nonnull JsonMapper jsonMapper) {
		requireNonNull(accountService);
		requireNonNull(groupSessionService);
		requireNonNull(groupSessionApiResponseFactory);
		requireNonNull(groupSessionReservationApiResponseFactory);
		requireNonNull(accountApiResponseFactory);
		requireNonNull(requestBodyParser);
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(currentContextProvider);
		requireNonNull(auditLogService);
		requireNonNull(jsonMapper);

		this.accountService = accountService;
		this.groupSessionService = groupSessionService;
		this.groupSessionApiResponseFactory = groupSessionApiResponseFactory;
		this.groupSessionReservationApiResponseFactory = groupSessionReservationApiResponseFactory;
		this.accountApiResponseFactory = accountApiResponseFactory;
		this.requestBodyParser = requestBodyParser;
		this.formatter = formatter;
		this.strings = strings;
		this.currentContextProvider = currentContextProvider;
		this.logger = LoggerFactory.getLogger(getClass());
		this.auditLogService = auditLogService;
		this.jsonMapper = jsonMapper;
	}

	@Nonnull
	@GET("/group-session-reservations")
	@AuthenticationRequired
	public ApiResponse groupSessionReservations(@Nonnull @QueryParameter UUID groupSessionId) {
		requireNonNull(groupSessionId);

		Account account = getCurrentContext().getAccount().get();

		// TODO: security check, this is only for superusers

		List<GroupSessionReservation> groupSessionReservations = getGroupSessionService().findGroupSessionReservationsByGroupSessionId(groupSessionId);

		return new ApiResponse(new HashMap<String, Object>() {{
			put("groupSessionReservations", groupSessionReservations.stream()
					.map(groupSessionReservation -> getGroupSessionReservationApiResponseFactory().create(groupSessionReservation))
					.collect(Collectors.toList()));
		}});
	}

	@Nonnull
	@POST("/group-session-reservations")
	@AuthenticationRequired
	public ApiResponse createGroupSessionReservation(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		Account account = getCurrentContext().getAccount().get();

		CreateGroupSessionReservationRequest request = getRequestBodyParser().parse(requestBody, CreateGroupSessionReservationRequest.class);
		request.setAccountId(account.getAccountId());

		UUID groupSessionReservationId = getGroupSessionService().createGroupSessionReservation(request);
		Pair<GroupSession, GroupSessionReservation> groupSessionReservationPair = getGroupSessionService().findGroupSessionReservationPairById(groupSessionReservationId).get();

		// It's possible creating the reservation has updated the account's email address.
		// Vend the account so client has the latest and greatest
		Account updatedAccount = getAccountService().findAccountById(account.getAccountId()).get();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("groupSessionReservation", getGroupSessionReservationApiResponseFactory().create(groupSessionReservationPair.getRight()));
			put("account", getAccountApiResponseFactory().create(updatedAccount));
		}});
	}

	@Nonnull
	@GET("/group-session-reservations/{groupSessionReservationId}")
	@AuthenticationRequired
	public ApiResponse groupSessionReservation(@Nonnull @PathParameter UUID groupSessionReservationId) {
		requireNonNull(groupSessionReservationId);

		Account account = getCurrentContext().getAccount().get();
		Pair<GroupSession, GroupSessionReservation> groupSessionReservationPair = getGroupSessionService().findGroupSessionReservationPairById(groupSessionReservationId).orElse(null);

		if (groupSessionReservationPair == null)
			throw new NotFoundException();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("groupSession", getGroupSessionApiResponseFactory().create(groupSessionReservationPair.getLeft()));
			put("groupSessionReservation", getGroupSessionReservationApiResponseFactory().create(groupSessionReservationPair.getRight()));
		}});
	}

	@PUT("/group-session-reservations/{groupSessionReservationId}/cancel")
	@AuthenticationRequired
	public void cancelGroupSessionReservation(@Nonnull @PathParameter UUID groupSessionReservationId) {
		requireNonNull(groupSessionReservationId);

		Account account = getCurrentContext().getAccount().get();

		CancelGroupSessionReservationRequest request = new CancelGroupSessionReservationRequest();
		request.setAccountId(account.getAccountId());
		request.setGroupSessionReservationId(groupSessionReservationId);

		getGroupSessionService().cancelGroupSessionReservation(request);
	}

	@GET("/group-session-reservations/{groupSessionReservationId}/google-calendar")
	@AuthenticationRequired
	@Nonnull
	public RedirectResponse googleCalendarGroupSessionReservation(@Nonnull @PathParameter UUID groupSessionReservationId) {
		requireNonNull(groupSessionReservationId);

		Pair<GroupSession, GroupSessionReservation> groupSessionReservationPair = getGroupSessionService().findGroupSessionReservationPairById(groupSessionReservationId).orElse(null);

		if (groupSessionReservationPair == null)
			throw new NotFoundException();

		return new RedirectResponse(getGroupSessionService().generateGoogleCalendarTemplateUrl(groupSessionReservationPair.getLeft()), RedirectResponse.Type.TEMPORARY);
	}

	@GET("/group-session-reservations/{groupSessionReservationId}/ical")
	@AuthenticationRequired
	@Nonnull
	public CustomResponse icalGroupSessionReservation(@Nonnull @PathParameter UUID groupSessionReservationId,
																										@Nonnull HttpServletResponse httpServletResponse) throws IOException {
		requireNonNull(groupSessionReservationId);
		requireNonNull(httpServletResponse);

		Pair<GroupSession, GroupSessionReservation> groupSessionReservationPair = getGroupSessionService().findGroupSessionReservationPairById(groupSessionReservationId).orElse(null);

		if (groupSessionReservationPair == null)
			throw new NotFoundException();

		String icalInvite = getGroupSessionService().generateICalInvite(groupSessionReservationPair.getLeft(), groupSessionReservationPair.getRight(), InviteMethod.REQUEST);

		httpServletResponse.setContentType("text/calendar; charset=UTF-8");
		httpServletResponse.setHeader("Content-Disposition", "attachment; filename=\"invite.ics\"");

		IOUtils.copy(new StringReader(icalInvite), httpServletResponse.getOutputStream(), StandardCharsets.UTF_8);

		return CustomResponse.instance();
	}

	@Nonnull
	protected AccountService getAccountService() {
		return accountService;
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
	protected AccountApiResponseFactory getAccountApiResponseFactory() {
		return accountApiResponseFactory;
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
}