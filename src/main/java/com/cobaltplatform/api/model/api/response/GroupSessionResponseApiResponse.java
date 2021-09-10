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

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.db.GroupSessionResponse;
import com.cobaltplatform.api.util.Formatter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class GroupSessionResponseApiResponse {
	@Nonnull
	private final UUID groupSessionResponseId;
	@Nonnull
	private final UUID groupSessionRequestId;
	@Nullable
	private final UUID respondentAccountId;
	@Nullable
	private final String respondentName;
	@Nullable
	private final String respondentEmailAddress;
	@Nullable
	private final String respondentPhoneNumber;
	@Nullable
	private final String respondentPhoneNumberDescription;
	@Nullable
	private final LocalDate suggestedDate;
	@Nullable
	private final String suggestedDateDescription;
	@Nullable
	private final String suggestedTime;
	@Nullable
	private final String expectedParticipants;
	@Nullable
	private final String notes;
	@Nullable
	private final String customAnswer1;
	@Nullable
	private final String customAnswer2;
	@Nullable
	private final Instant created;
	@Nullable
	private final String createdDescription;
	@Nullable
	private final Instant lastUpdated;
	@Nullable
	private final String lastUpdatedDescription;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface GroupSessionResponseApiResponseFactory {
		@Nonnull
		GroupSessionResponseApiResponse create(@Nonnull GroupSessionResponse groupSessionResponse);
	}

	@AssistedInject
	public GroupSessionResponseApiResponse(@Nonnull Formatter formatter,
																				 @Nonnull Strings strings,
																				 @Nonnull javax.inject.Provider<CurrentContext> currentContextProvider,
																				 @Assisted @Nonnull GroupSessionResponse groupSessionResponse) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(currentContextProvider);
		requireNonNull(groupSessionResponse);

		this.groupSessionResponseId = groupSessionResponse.getGroupSessionResponseId();
		this.groupSessionRequestId = groupSessionResponse.getGroupSessionRequestId();
		this.respondentAccountId = groupSessionResponse.getRespondentAccountId();
		this.respondentName = groupSessionResponse.getRespondentName();
		this.respondentEmailAddress = groupSessionResponse.getRespondentEmailAddress();
		this.respondentPhoneNumber = groupSessionResponse.getRespondentPhoneNumber();
		this.respondentPhoneNumberDescription = respondentPhoneNumber == null ? null : formatter.formatPhoneNumber(respondentPhoneNumber);
		this.suggestedDate = groupSessionResponse.getSuggestedDate();
		this.suggestedDateDescription = groupSessionResponse.getSuggestedDate() == null ? null : formatter.formatDate(groupSessionResponse.getSuggestedDate());
		this.suggestedTime = groupSessionResponse.getSuggestedTime();
		this.expectedParticipants = groupSessionResponse.getExpectedParticipants();
		this.notes = groupSessionResponse.getNotes();
		this.customAnswer1 = groupSessionResponse.getCustomAnswer1();
		this.customAnswer2 = groupSessionResponse.getCustomAnswer2();
		this.created = groupSessionResponse.getCreated();
		this.createdDescription = formatter.formatTimestamp(groupSessionResponse.getCreated());
		this.lastUpdated = groupSessionResponse.getLastUpdated();
		this.lastUpdatedDescription = formatter.formatTimestamp(groupSessionResponse.getLastUpdated());
	}

	@Nonnull
	public UUID getGroupSessionResponseId() {
		return groupSessionResponseId;
	}

	@Nonnull
	public UUID getGroupSessionRequestId() {
		return groupSessionRequestId;
	}

	@Nullable
	public UUID getRespondentAccountId() {
		return respondentAccountId;
	}

	@Nullable
	public String getRespondentName() {
		return respondentName;
	}

	@Nullable
	public String getRespondentEmailAddress() {
		return respondentEmailAddress;
	}

	@Nullable
	public String getRespondentPhoneNumber() {
		return respondentPhoneNumber;
	}

	@Nullable
	public String getRespondentPhoneNumberDescription() {
		return respondentPhoneNumberDescription;
	}

	@Nullable
	public LocalDate getSuggestedDate() {
		return suggestedDate;
	}

	@Nullable
	public String getSuggestedDateDescription() {
		return suggestedDateDescription;
	}

	@Nullable
	public String getSuggestedTime() {
		return suggestedTime;
	}

	@Nullable
	public String getExpectedParticipants() {
		return expectedParticipants;
	}

	@Nullable
	public String getNotes() {
		return notes;
	}

	@Nullable
	public String getCustomAnswer1() {
		return customAnswer1;
	}

	@Nullable
	public String getCustomAnswer2() {
		return customAnswer2;
	}

	@Nullable
	public Instant getCreated() {
		return created;
	}

	@Nullable
	public String getCreatedDescription() {
		return createdDescription;
	}

	@Nullable
	public Instant getLastUpdated() {
		return lastUpdated;
	}

	@Nullable
	public String getLastUpdatedDescription() {
		return lastUpdatedDescription;
	}
}
