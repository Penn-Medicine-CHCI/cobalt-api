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

import com.cobaltplatform.api.model.db.ClientDevicePushToken;
import com.cobaltplatform.api.model.db.ClientDevicePushTokenType.ClientDevicePushTokenTypeId;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class ClientDevicePushTokenApiResponse {
	@Nonnull
	private final UUID clientDevicePushTokenId;
	@Nonnull
	private final UUID clientDeviceId;
	@Nonnull
	private final ClientDevicePushTokenTypeId clientDevicePushTokenTypeId;
	@Nonnull
	private final String pushToken;
	@Nonnull
	private final Boolean valid;
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
	public interface ClientDevicePushTokenApiResponseFactory {
		@Nonnull
		ClientDevicePushTokenApiResponse create(@Nonnull ClientDevicePushToken clientDevicePushToken);
	}

	@AssistedInject
	public ClientDevicePushTokenApiResponse(@Nonnull Formatter formatter,
																					@Nonnull Strings strings,
																					@Assisted @Nonnull ClientDevicePushToken clientDevicePushToken) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(clientDevicePushToken);

		this.clientDevicePushTokenId = clientDevicePushToken.getClientDevicePushTokenId();
		this.clientDevicePushTokenTypeId = clientDevicePushToken.getClientDevicePushTokenTypeId();
		this.clientDeviceId = clientDevicePushToken.getClientDeviceId();
		this.pushToken = clientDevicePushToken.getPushToken();
		this.valid = clientDevicePushToken.getValid();
		this.created = clientDevicePushToken.getCreated();
		this.createdDescription = formatter.formatTimestamp(clientDevicePushToken.getCreated());
		this.lastUpdated = clientDevicePushToken.getLastUpdated();
		this.lastUpdatedDescription = formatter.formatTimestamp(clientDevicePushToken.getLastUpdated());
	}

	@Nonnull
	public UUID getClientDevicePushTokenId() {
		return this.clientDevicePushTokenId;
	}

	@Nonnull
	public UUID getClientDeviceId() {
		return this.clientDeviceId;
	}

	@Nonnull
	public ClientDevicePushTokenTypeId getClientDevicePushTokenTypeId() {
		return this.clientDevicePushTokenTypeId;
	}

	@Nonnull
	public String getPushToken() {
		return this.pushToken;
	}

	@Nonnull
	public Boolean getValid() {
		return this.valid;
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