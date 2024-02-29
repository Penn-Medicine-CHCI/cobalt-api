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

import com.cobaltplatform.api.model.db.ClientDeviceActivity;
import com.cobaltplatform.api.model.db.ClientDeviceActivityType.ClientDeviceActivityTypeId;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class ClientDeviceActivityApiResponse {
	@Nonnull
	private final UUID clientDeviceActivityId;
	@Nonnull
	private final UUID clientDeviceId;
	@Nonnull
	private final ClientDeviceActivityTypeId clientDeviceActivityTypeId;
	@Nullable
	private final UUID accountId;
	@Nonnull
	private final Instant created;
	@Nonnull
	private final String createdDescription;
	@Nonnull
	private final Instant lastUpdated;
	@Nonnull
	private final String lastUpdatedDescription;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface ClientDeviceActivityApiResponseFactory {
		@Nonnull
		ClientDeviceActivityApiResponse create(@Nonnull ClientDeviceActivity clientDeviceActivity);
	}

	@AssistedInject
	public ClientDeviceActivityApiResponse(@Nonnull Formatter formatter,
																				 @Nonnull Strings strings,
																				 @Assisted @Nonnull ClientDeviceActivity clientDeviceActivity) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(clientDeviceActivity);

		this.clientDeviceActivityId = clientDeviceActivity.getClientDeviceActivityId();
		this.clientDeviceId = clientDeviceActivity.getClientDeviceId();
		this.clientDeviceActivityTypeId = clientDeviceActivity.getClientDeviceActivityTypeId();
		this.accountId = clientDeviceActivity.getAccountId();
		this.created = clientDeviceActivity.getCreated();
		this.createdDescription = formatter.formatTimestamp(clientDeviceActivity.getCreated());
		this.lastUpdated = clientDeviceActivity.getLastUpdated();
		this.lastUpdatedDescription = formatter.formatTimestamp(clientDeviceActivity.getLastUpdated());
	}

	@Nonnull
	public UUID getClientDeviceActivityId() {
		return this.clientDeviceActivityId;
	}

	@Nonnull
	public UUID getClientDeviceId() {
		return this.clientDeviceId;
	}

	@Nonnull
	public ClientDeviceActivityTypeId getClientDeviceActivityTypeId() {
		return this.clientDeviceActivityTypeId;
	}

	@Nonnull
	public Optional<UUID> getAccountId() {
		return Optional.ofNullable(this.accountId);
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
	public Instant getLastUpdated() {
		return this.lastUpdated;
	}

	@Nonnull
	public String getLastUpdatedDescription() {
		return this.lastUpdatedDescription;
	}
}