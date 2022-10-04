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
import com.cobaltplatform.api.model.db.GroupSessionRequest;
import com.cobaltplatform.api.model.db.GroupSessionRequestStatus.GroupSessionRequestStatusId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.service.GroupSessionService;
import com.cobaltplatform.api.service.InstitutionService;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.FormatStyle;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class GroupSessionRequestApiResponse {
	@Nonnull
	private final UUID groupSessionRequestId;
	@Nullable
	private final InstitutionId institutionId;
	@Nullable
	private final String institutionDescription;
	@Nullable
	private final GroupSessionRequestStatusId groupSessionRequestStatusId;
	@Nullable
	private final String groupSessionRequestStatusIdDescription;
	@Nonnull
	private final UUID submitterAccountId;
	@Nullable
	private final String title;
	@Nullable
	private final String description;
	@Nullable
	private final String urlName;
	@Nullable
	private final UUID facilitatorAccountId;
	@Nullable
	private final String facilitatorName;
	@Nullable
	private final String facilitatorEmailAddress;
	@Nullable
	private final String imageUrl;
	@Nullable
	private final String customQuestion1;
	@Nullable
	private final String customQuestion2;
	@Nonnull
	private final Boolean dataCollectionEnabled;
	@Nonnull
	private final Instant created;
	@Nonnull
	private final String createdDescription;
	@Nonnull
	private final String createdDateDescription;
	@Nullable
	private final Instant lastUpdated;
	@Nullable
	private final String lastUpdatedDescription;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface GroupSessionRequestApiResponseFactory {
		@Nonnull
		GroupSessionRequestApiResponse create(@Nonnull GroupSessionRequest groupSessionRequest);
	}

	@AssistedInject
	public GroupSessionRequestApiResponse(@Nonnull Formatter formatter,
																				@Nonnull Strings strings,
																				@Nonnull InstitutionService institutionService,
																				@Nonnull GroupSessionService groupSessionService,
																				@Nonnull javax.inject.Provider<CurrentContext> currentContextProvider,
																				@Assisted @Nonnull GroupSessionRequest groupSessionRequest) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(institutionService);
		requireNonNull(groupSessionService);
		requireNonNull(currentContextProvider);
		requireNonNull(groupSessionRequest);

		this.groupSessionRequestId = groupSessionRequest.getGroupSessionRequestId();
		this.institutionId = groupSessionRequest.getInstitutionId();
		this.institutionDescription = institutionService.findInstitutionById(institutionId).get().getName();
		this.groupSessionRequestStatusId = groupSessionRequest.getGroupSessionRequestStatusId();
		this.groupSessionRequestStatusIdDescription = groupSessionService.findGroupSessionRequestStatusById(groupSessionRequest.getGroupSessionRequestStatusId()).get().getDescription();
		this.submitterAccountId = groupSessionRequest.getSubmitterAccountId();
		this.title = groupSessionRequest.getTitle();
		this.description = groupSessionRequest.getDescription();
		this.urlName = groupSessionRequest.getUrlName();
		this.facilitatorAccountId = groupSessionRequest.getFacilitatorAccountId();
		this.facilitatorName = groupSessionRequest.getFacilitatorName();
		this.facilitatorEmailAddress = groupSessionRequest.getFacilitatorEmailAddress();
		this.imageUrl = groupSessionRequest.getImageUrl();
		this.customQuestion1 = groupSessionRequest.getCustomQuestion1();
		this.customQuestion2 = groupSessionRequest.getCustomQuestion2();
		this.dataCollectionEnabled = groupSessionRequest.getDataCollectionEnabled();
		this.created = groupSessionRequest.getCreated();
		this.createdDescription = formatter.formatTimestamp(groupSessionRequest.getCreated());

		LocalDate createdDate = LocalDate.ofInstant(groupSessionRequest.getCreated(), currentContextProvider.get().getTimeZone());
		this.createdDateDescription = formatter.formatDate(createdDate, FormatStyle.SHORT);

		this.lastUpdated = groupSessionRequest.getLastUpdated();
		this.lastUpdatedDescription = formatter.formatTimestamp(groupSessionRequest.getLastUpdated());
	}

	@Nonnull
	public UUID getGroupSessionRequestId() {
		return this.groupSessionRequestId;
	}

	@Nullable
	public InstitutionId getInstitutionId() {
		return this.institutionId;
	}

	@Nullable
	public String getInstitutionDescription() {
		return this.institutionDescription;
	}

	@Nullable
	public GroupSessionRequestStatusId getGroupSessionRequestStatusId() {
		return this.groupSessionRequestStatusId;
	}

	@Nullable
	public String getGroupSessionRequestStatusIdDescription() {
		return this.groupSessionRequestStatusIdDescription;
	}

	@Nonnull
	public UUID getSubmitterAccountId() {
		return this.submitterAccountId;
	}

	@Nullable
	public String getTitle() {
		return this.title;
	}

	@Nullable
	public String getDescription() {
		return this.description;
	}

	@Nullable
	public String getUrlName() {
		return this.urlName;
	}

	@Nullable
	public UUID getFacilitatorAccountId() {
		return this.facilitatorAccountId;
	}

	@Nullable
	public String getFacilitatorName() {
		return this.facilitatorName;
	}

	@Nullable
	public String getFacilitatorEmailAddress() {
		return this.facilitatorEmailAddress;
	}

	@Nullable
	public String getImageUrl() {
		return this.imageUrl;
	}

	@Nullable
	public String getCustomQuestion1() {
		return this.customQuestion1;
	}

	@Nullable
	public String getCustomQuestion2() {
		return this.customQuestion2;
	}

	@Nonnull
	public Boolean getDataCollectionEnabled() {
		return this.dataCollectionEnabled;
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
	public String getCreatedDateDescription() {
		return this.createdDateDescription;
	}

	@Nullable
	public Instant getLastUpdated() {
		return this.lastUpdated;
	}

	@Nullable
	public String getLastUpdatedDescription() {
		return this.lastUpdatedDescription;
	}
}
