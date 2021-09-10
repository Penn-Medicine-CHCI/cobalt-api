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
import com.cobaltplatform.api.model.api.request.CreateGroupSessionResponseRequest;
import com.cobaltplatform.api.model.api.response.GroupSessionResponseApiResponse.GroupSessionResponseApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.GroupSessionResponse;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.service.AuditLogService;
import com.cobaltplatform.api.service.GroupSessionService;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.JsonMapper;
import com.cobaltplatform.api.web.request.RequestBodyParser;
import com.soklet.web.annotation.POST;
import com.soklet.web.annotation.RequestBody;
import com.soklet.web.annotation.Resource;
import com.soklet.web.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Resource
@Singleton
@ThreadSafe
public class GroupSessionResponseResource {
	@Nonnull
	private final GroupSessionService groupSessionService;
	@Nonnull
	private final GroupSessionResponseApiResponseFactory groupSessionResponseApiResponseFactory;
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
	public GroupSessionResponseResource(@Nonnull GroupSessionService groupSessionService,
																			@Nonnull GroupSessionResponseApiResponseFactory groupSessionResponseApiResponseFactory,
																			@Nonnull RequestBodyParser requestBodyParser,
																			@Nonnull Formatter formatter,
																			@Nonnull Strings strings,
																			@Nonnull Provider<CurrentContext> currentContextProvider,
																			@Nonnull AuditLogService auditLogService,
																			@Nonnull JsonMapper jsonMapper) {
		requireNonNull(groupSessionService);
		requireNonNull(groupSessionResponseApiResponseFactory);
		requireNonNull(requestBodyParser);
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(currentContextProvider);
		requireNonNull(auditLogService);
		requireNonNull(jsonMapper);

		this.groupSessionService = groupSessionService;
		this.groupSessionResponseApiResponseFactory = groupSessionResponseApiResponseFactory;
		this.requestBodyParser = requestBodyParser;
		this.formatter = formatter;
		this.strings = strings;
		this.currentContextProvider = currentContextProvider;
		this.logger = LoggerFactory.getLogger(getClass());
		this.auditLogService = auditLogService;
		this.jsonMapper = jsonMapper;
	}

	@Nonnull
	@POST("/group-session-responses")
	@AuthenticationRequired
	public ApiResponse createGroupSessionResponse(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		Account account = getCurrentContext().getAccount().get();

		CreateGroupSessionResponseRequest request = getRequestBodyParser().parse(requestBody, CreateGroupSessionResponseRequest.class);
		request.setRespondentAccountId(account.getAccountId());

		UUID groupSessionResponseId = getGroupSessionService().createGroupSessionResponse(request);
		GroupSessionResponse groupSessionResponse = getGroupSessionService().findGroupSessionResponseById(groupSessionResponseId).get();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("groupSessionResponse", getGroupSessionResponseApiResponseFactory().create(groupSessionResponse));
		}});
	}

	@Nonnull
	protected GroupSessionService getGroupSessionService() {
		return groupSessionService;
	}

	@Nonnull
	protected GroupSessionResponseApiResponseFactory getGroupSessionResponseApiResponseFactory() {
		return groupSessionResponseApiResponseFactory;
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