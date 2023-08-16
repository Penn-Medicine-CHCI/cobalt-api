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
import com.cobaltplatform.api.model.api.request.CreateGroupRequestRequest;
import com.cobaltplatform.api.model.api.request.CreateGroupSessionSuggestionRequest;
import com.cobaltplatform.api.model.api.response.GroupRequestApiResponse.GroupRequestApiResponseFactory;
import com.cobaltplatform.api.model.api.response.GroupSessionSuggestionApiResponse.GroupSessionSuggestionApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.GroupRequest;
import com.cobaltplatform.api.model.db.GroupSessionSuggestion;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.service.GroupRequestService;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.web.request.RequestBodyParser;
import com.lokalized.Strings;
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
public class GroupRequestResource {
	@Nonnull
	private final GroupRequestService groupRequestService;
	@Nonnull
	private final GroupRequestApiResponseFactory groupRequestApiResponseFactory;
	@Nonnull
	private final GroupSessionSuggestionApiResponseFactory groupSessionSuggestionApiResponseFactory;
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

	@Inject
	public GroupRequestResource(@Nonnull GroupRequestService groupRequestService,
															@Nonnull GroupRequestApiResponseFactory groupRequestApiResponseFactory,
															@Nonnull GroupSessionSuggestionApiResponseFactory groupSessionSuggestionApiResponseFactory,
															@Nonnull RequestBodyParser requestBodyParser,
															@Nonnull Formatter formatter,
															@Nonnull Strings strings,
															@Nonnull Provider<CurrentContext> currentContextProvider) {
		requireNonNull(groupRequestService);
		requireNonNull(groupRequestApiResponseFactory);
		requireNonNull(requestBodyParser);
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(currentContextProvider);
		requireNonNull(groupSessionSuggestionApiResponseFactory);

		this.groupRequestService = groupRequestService;
		this.groupRequestApiResponseFactory = groupRequestApiResponseFactory;
		this.groupSessionSuggestionApiResponseFactory = groupSessionSuggestionApiResponseFactory;
		this.requestBodyParser = requestBodyParser;
		this.formatter = formatter;
		this.strings = strings;
		this.currentContextProvider = currentContextProvider;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	@POST("/group-requests")
	@AuthenticationRequired
	public ApiResponse createGroupRequest(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		Account account = getCurrentContext().getAccount().get();

		CreateGroupRequestRequest request = getRequestBodyParser().parse(requestBody, CreateGroupRequestRequest.class);
		request.setRequestorAccountId(account.getAccountId());

		UUID groupRequestId = getGroupRequestService().createGroupRequest(request);
		GroupRequest groupRequest = getGroupRequestService().findGroupRequestById(groupRequestId).get();
		
		return new ApiResponse(new HashMap<String, Object>() {{
			put("groupRequest", getGroupRequestApiResponseFactory().create(groupRequest));
		}});
	}

	@Nonnull
	@POST("/group-suggestions")
	@AuthenticationRequired
	public ApiResponse sendGroupSuggestion(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		Account account = getCurrentContext().getAccount().get();

		CreateGroupSessionSuggestionRequest request = getRequestBodyParser().parse(requestBody, CreateGroupSessionSuggestionRequest.class);
		request.setRequestorAccountId(account.getAccountId());

		UUID groupSessionSuggestionId = getGroupRequestService().createGroupSessionSuggestion(request);
		GroupSessionSuggestion groupSessionSuggestion = getGroupRequestService().findGroupSessionSuggestionById(groupSessionSuggestionId).get();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("groupSessionSuggestion", getGroupSessionSuggestionApiResponseFactory().create(groupSessionSuggestion));
		}});

	}

	@Nonnull
	protected GroupRequestService getGroupRequestService() {
		return this.groupRequestService;
	}

	@Nonnull
	protected GroupRequestApiResponseFactory getGroupRequestApiResponseFactory() {
		return this.groupRequestApiResponseFactory;
	}

	@Nonnull
	protected RequestBodyParser getRequestBodyParser() {
		return this.requestBodyParser;
	}

	@Nonnull
	protected Formatter getFormatter() {
		return this.formatter;
	}

	@Nonnull
	protected Strings getStrings() {
		return this.strings;
	}

	@Nonnull
	protected CurrentContext getCurrentContext() {
		return this.currentContextProvider.get();
	}

	@Nonnull
	protected Logger getLogger() {
		return this.logger;
	}

	@Nonnull
	protected GroupSessionSuggestionApiResponseFactory getGroupSessionSuggestionApiResponseFactory() {
		return groupSessionSuggestionApiResponseFactory;
	}
}