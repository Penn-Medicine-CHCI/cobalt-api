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
import com.cobaltplatform.api.model.db.GroupSessionReservation;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.Normalizer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class GroupSessionReservationApiResponse {
	@Nonnull
	private final UUID groupSessionReservationId;
	@Nonnull
	private final UUID groupSessionId;
	@Nonnull
	private final UUID accountId;
	@Nullable
	private final String name;
	@Nullable
	private final String emailAddress;
	@Nullable
	private final String phoneNumber;
	@Nullable
	private final Boolean canceled;
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
	public interface GroupSessionReservationApiResponseFactory {
		@Nonnull
		GroupSessionReservationApiResponse create(@Nonnull GroupSessionReservation groupSessionReservation);
	}

	@AssistedInject
	public GroupSessionReservationApiResponse(@Nonnull Formatter formatter,
																						@Nonnull Normalizer normalizer,
																						@Nonnull Strings strings,
																						@Nonnull javax.inject.Provider<CurrentContext> currentContextProvider,
																						@Assisted @Nonnull GroupSessionReservation groupSessionReservation) {
		requireNonNull(formatter);
		requireNonNull(normalizer);
		requireNonNull(strings);
		requireNonNull(currentContextProvider);
		requireNonNull(groupSessionReservation);

		this.groupSessionReservationId = groupSessionReservation.getGroupSessionReservationId();
		this.groupSessionId = groupSessionReservation.getGroupSessionId();
		this.accountId = groupSessionReservation.getAccountId();
		this.name = normalizer.normalizeName(groupSessionReservation.getFirstName(), groupSessionReservation.getLastName()).orElse(null);
		this.emailAddress = groupSessionReservation.getEmailAddress();
		this.phoneNumber = groupSessionReservation.getPhoneNumber() == null ? null : formatter.formatPhoneNumber(groupSessionReservation.getPhoneNumber());
		this.canceled = groupSessionReservation.getCanceled();
		this.created = groupSessionReservation.getCreated();
		this.createdDescription = formatter.formatTimestamp(groupSessionReservation.getCreated());
		this.lastUpdated = groupSessionReservation.getLastUpdated();
		this.lastUpdatedDescription = formatter.formatTimestamp(groupSessionReservation.getLastUpdated());
	}

	@Nonnull
	public UUID getGroupSessionId() {
		return groupSessionId;
	}

	@Nonnull
	public UUID getGroupSessionReservationId() {
		return groupSessionReservationId;
	}

	@Nonnull
	public UUID getAccountId() {
		return accountId;
	}

	@Nullable
	public String getName() {
		return name;
	}

	@Nullable
	public String getEmailAddress() {
		return emailAddress;
	}

	@Nullable
	public String getPhoneNumber() {
		return phoneNumber;
	}

	@Nullable
	public Boolean getCanceled() {
		return canceled;
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
