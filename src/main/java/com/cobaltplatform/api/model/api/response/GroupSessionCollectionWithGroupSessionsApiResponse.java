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
import com.cobaltplatform.api.model.api.request.FindGroupSessionsRequest;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.GroupSession;
import com.cobaltplatform.api.model.db.GroupSessionCollection;
import com.cobaltplatform.api.model.db.GroupSessionStatus;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.service.FindResult;
import com.cobaltplatform.api.service.GroupSessionService;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class GroupSessionCollectionWithGroupSessionsApiResponse {
	@Nonnull
	private final UUID groupSessionCollectionId;
	@Nonnull
	private final InstitutionId institutionId;
	@Nonnull
	private final String description;
	@Nonnull
	private final Integer displayOrder;
	@Nonnull
	private final List<GroupSessionApiResponse> groupSessions;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface GroupSessionCollectionWithGroupSessionsResponseFactory {
		@Nonnull
		GroupSessionCollectionWithGroupSessionsApiResponse create(@Nonnull GroupSessionCollection groupSessionCollection,
																															@Nonnull Account account);
	}

	@AssistedInject
	public GroupSessionCollectionWithGroupSessionsApiResponse(@Nonnull Provider<CurrentContext> currentContextProvider,
																														@Assisted @Nonnull GroupSessionCollection groupSessionCollection,
																														@Nonnull GroupSessionService groupSessionService,
																														@Assisted @Nonnull Account account,
																														@Nonnull GroupSessionApiResponse.GroupSessionApiResponseFactory groupSessionApiResponseFactory) {
		requireNonNull(currentContextProvider);
		requireNonNull(groupSessionCollection);
		requireNonNull(groupSessionService);
		requireNonNull(account);

		FindGroupSessionsRequest request = new FindGroupSessionsRequest();
		request.setInstitutionId(account.getInstitutionId());
		request.setFilterBehavior(FindGroupSessionsRequest.FilterBehavior.DEFAULT);
		request.setGroupSessionStatusId(GroupSessionStatus.GroupSessionStatusId.ADDED);
		request.setFilterBehavior(FindGroupSessionsRequest.FilterBehavior.VISIBLE);
		request.setGroupSessionCollectionId(groupSessionCollection.getGroupSessionCollectionId());
		FindResult<GroupSession> findResult = groupSessionService.findGroupSessions(request);

		this.groupSessionCollectionId = groupSessionCollection.getGroupSessionCollectionId();
		this.institutionId = groupSessionCollection.getInstitutionId();
		this.description = groupSessionCollection.getDescription();
		this.displayOrder = groupSessionCollection.getDisplayOrder();
		this.groupSessions = findResult.getResults().stream()
				.map(groupSession -> groupSessionApiResponseFactory.create(groupSession))
				.collect(Collectors.toList());

	}

	@Nonnull
	public UUID getGroupSessionCollectionId() {
		return groupSessionCollectionId;
	}

	@Nonnull
	public InstitutionId getInstitutionId() {
		return institutionId;
	}

	@Nonnull
	public String getDescription() {
		return description;
	}

	@Nonnull
	public Integer getDisplayOrder() {
		return displayOrder;
	}

	@Nonnull
	public List<GroupSessionApiResponse> getGroupSessions() {
		return groupSessions;
	}
}