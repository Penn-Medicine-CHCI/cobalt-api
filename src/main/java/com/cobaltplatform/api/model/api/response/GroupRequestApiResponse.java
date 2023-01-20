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

package com.cobaltplatform.api.model.api.response;

import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.api.response.GroupTopicApiResponse.GroupTopicApiResponseFactory;
import com.cobaltplatform.api.model.db.GroupRequest;
import com.cobaltplatform.api.service.GroupRequestService;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class GroupRequestApiResponse {
	@Nonnull
	private final UUID groupRequestId;
	@Nonnull
	private final UUID requestorAccountId;
	@Nonnull
	private final String requestorName;
	@Nonnull
	private final String requestorEmailAddress;
	@Nullable
	private final String preferredDateDescription;
	@Nullable
	private final String preferredTimeDescription;
	@Nullable
	private final String additionalDescription;
	@Nullable
	private final String otherGroupTopicsDescription;
	@Nonnull
	private final Integer minimumAttendeeCount;
	@Nullable
	private final Integer maximumAttendeeCount;
	@Nonnull
	private final Instant created;
	@Nonnull
	private final String createdDescription;
	@Nonnull
	private List<GroupTopicApiResponse> groupTopics;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface GroupRequestApiResponseFactory {
		@Nonnull
		GroupRequestApiResponse create(@Nonnull GroupRequest groupRequest);
	}

	@AssistedInject
	public GroupRequestApiResponse(@Nonnull GroupRequestService groupRequestService,
																 @Nonnull GroupTopicApiResponseFactory groupTopicApiResponseFactory,
																 @Nonnull Provider<CurrentContext> currentContextProvider,
																 @Nonnull Formatter formatter,
																 @Assisted @Nonnull GroupRequest groupRequest) {
		requireNonNull(groupRequestService);
		requireNonNull(groupTopicApiResponseFactory);
		requireNonNull(currentContextProvider);
		requireNonNull(formatter);
		requireNonNull(groupRequest);

		this.groupRequestId = groupRequest.getGroupRequestId();
		this.requestorAccountId = groupRequest.getRequestorAccountId();
		this.requestorName = groupRequest.getRequestorName();
		this.requestorEmailAddress = groupRequest.getRequestorEmailAddress();
		this.preferredDateDescription = groupRequest.getPreferredDateDescription();
		this.preferredTimeDescription = groupRequest.getPreferredTimeDescription();
		this.additionalDescription = groupRequest.getAdditionalDescription();
		this.otherGroupTopicsDescription = groupRequest.getOtherGroupTopicsDescription();
		this.minimumAttendeeCount = groupRequest.getMinimumAttendeeCount();
		this.maximumAttendeeCount = groupRequest.getMaximumAttendeeCount();
		this.created = groupRequest.getCreated();
		this.createdDescription = formatter.formatTimestamp(groupRequest.getCreated());

		this.groupTopics = groupRequestService.findGroupTopicsByGroupRequestId(groupRequest.getGroupRequestId()).stream()
				.map(groupTopic -> groupTopicApiResponseFactory.create(groupTopic))
				.collect(Collectors.toList());
	}

	@Nonnull
	public UUID getGroupRequestId() {
		return this.groupRequestId;
	}

	@Nonnull
	public UUID getRequestorAccountId() {
		return this.requestorAccountId;
	}

	@Nonnull
	public String getRequestorName() {
		return this.requestorName;
	}

	@Nonnull
	public String getRequestorEmailAddress() {
		return this.requestorEmailAddress;
	}

	@Nullable
	public String getPreferredDateDescription() {
		return this.preferredDateDescription;
	}

	@Nullable
	public String getPreferredTimeDescription() {
		return this.preferredTimeDescription;
	}

	@Nullable
	public String getAdditionalDescription() {
		return this.additionalDescription;
	}

	@Nullable
	public String getOtherGroupTopicsDescription() {
		return this.otherGroupTopicsDescription;
	}

	@Nonnull
	public Integer getMinimumAttendeeCount() {
		return this.minimumAttendeeCount;
	}

	@Nullable
	public Integer getMaximumAttendeeCount() {
		return this.maximumAttendeeCount;
	}

	@Nonnull
	public Instant getCreated() {
		return this.created;
	}

	@Nonnull
	public String getCreatedDescription() {
		return this.createdDescription;
	}

	@Nonnull
	public List<GroupTopicApiResponse> getGroupTopics() {
		return this.groupTopics;
	}
}