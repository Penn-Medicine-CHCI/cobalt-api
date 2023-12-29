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

import com.cobaltplatform.api.model.db.ClientDevice;
import com.cobaltplatform.api.model.db.ClientDeviceType.ClientDeviceTypeId;
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
public class ClientDeviceApiResponse {
	@Nonnull
	private final UUID clientDeviceId;
	@Nonnull
	private final ClientDeviceTypeId clientDeviceTypeId;
	@Nonnull
	private final String fingerprint;
	@Nullable
	private final String operatingSystemName;
	@Nullable
	private final String operatingSystemVersion;
	@Nullable
	private final String modelName;
	@Nullable
	private final String brand;
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
	public interface ClientDeviceApiResponseFactory {
		@Nonnull
		ClientDeviceApiResponse create(@Nonnull ClientDevice clientDevice);
	}

	@AssistedInject
	public ClientDeviceApiResponse(@Nonnull Formatter formatter,
																 @Nonnull Strings strings,
																 @Assisted @Nonnull ClientDevice clientDevice) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(clientDevice);

		this.clientDeviceId = clientDevice.getClientDeviceId();
		this.clientDeviceTypeId = clientDevice.getClientDeviceTypeId();
		this.fingerprint = clientDevice.getFingerprint();
		this.operatingSystemName = clientDevice.getOperatingSystemName();
		this.operatingSystemVersion = clientDevice.getOperatingSystemVersion();
		this.modelName = clientDevice.getModelName();
		this.brand = clientDevice.getBrand();
		this.created = clientDevice.getCreated();
		this.createdDescription = formatter.formatTimestamp(clientDevice.getCreated());
		this.lastUpdated = clientDevice.getLastUpdated();
		this.lastUpdatedDescription = formatter.formatTimestamp(clientDevice.getLastUpdated());
	}

	@Nonnull
	public UUID getClientDeviceId() {
		return this.clientDeviceId;
	}

	@Nonnull
	public ClientDeviceTypeId getClientDeviceTypeId() {
		return this.clientDeviceTypeId;
	}

	@Nonnull
	public String getFingerprint() {
		return this.fingerprint;
	}

	@Nonnull
	public Optional<String> getOperatingSystemName() {
		return Optional.ofNullable(this.operatingSystemName);
	}

	@Nonnull
	public Optional<String> getOperatingSystemVersion() {
		return Optional.ofNullable(this.operatingSystemVersion);
	}

	@Nonnull
	public Optional<String> getModelName() {
		return Optional.ofNullable(this.modelName);
	}

	@Nonnull
	public Optional<String> getBrand() {
		return Optional.ofNullable(this.brand);
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