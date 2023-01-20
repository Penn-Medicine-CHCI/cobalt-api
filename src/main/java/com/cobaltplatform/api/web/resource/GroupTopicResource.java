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
import com.cobaltplatform.api.model.api.response.GroupTopicApiResponse.GroupTopicApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.GroupTopic;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.service.GroupRequestService;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.web.request.RequestBodyParser;
import com.lokalized.Strings;
import com.soklet.web.annotation.GET;
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
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Resource
@Singleton
@ThreadSafe
public class GroupTopicResource {
	@Nonnull
	private final GroupRequestService groupRequestService;
	@Nonnull
	private final GroupTopicApiResponseFactory groupTopicApiResponseFactory;
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
	public GroupTopicResource(@Nonnull GroupRequestService groupRequestService,
														@Nonnull GroupTopicApiResponseFactory groupTopicApiResponseFactory,
														@Nonnull RequestBodyParser requestBodyParser,
														@Nonnull Formatter formatter,
														@Nonnull Strings strings,
														@Nonnull Provider<CurrentContext> currentContextProvider) {
		requireNonNull(groupRequestService);
		requireNonNull(groupTopicApiResponseFactory);
		requireNonNull(requestBodyParser);
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(currentContextProvider);

		this.groupRequestService = groupRequestService;
		this.groupTopicApiResponseFactory = groupTopicApiResponseFactory;
		this.requestBodyParser = requestBodyParser;
		this.formatter = formatter;
		this.strings = strings;
		this.currentContextProvider = currentContextProvider;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	@GET("/group-topics")
	@AuthenticationRequired
	public ApiResponse groupTopics() {
		Account account = getCurrentContext().getAccount().get();

		List<GroupTopic> groupTopics = getGroupRequestService().findGroupTopicsByInstitutionId(account.getInstitutionId());

		return new ApiResponse(new HashMap<String, Object>() {{
			put("groupTopics", groupTopics.stream()
					.map(groupTopic -> getGroupTopicApiResponseFactory().create(groupTopic))
					.collect(Collectors.toList()));
		}});
	}

	@Nonnull
	protected GroupRequestService getGroupRequestService() {
		return this.groupRequestService;
	}

	@Nonnull
	protected GroupTopicApiResponseFactory getGroupTopicApiResponseFactory() {
		return this.groupTopicApiResponseFactory;
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
}