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

import com.cobaltplatform.api.model.api.response.ClientDeviceActivityApiResponse.ClientDeviceActivityApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ClientDevicePushTokenApiResponse.ClientDevicePushTokenApiResponseFactory;
import com.cobaltplatform.api.model.db.ClientDevice;
import com.cobaltplatform.api.model.db.ClientDeviceActivity;
import com.cobaltplatform.api.model.db.ClientDevicePushToken;
import com.cobaltplatform.api.model.db.ClientDeviceType.ClientDeviceTypeId;
import com.cobaltplatform.api.service.ClientDeviceService;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
	private final UUID fingerprint;
	@Nullable
	private final String operatingSystemName;
	@Nullable
	private final String operatingSystemVersion;
	@Nullable
	private final String model;
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

	// Via supplements

	@Nullable
	private final List<ClientDevicePushTokenApiResponse> clientDevicePushTokens;
	@Nullable
	private final List<ClientDeviceActivityApiResponse> clientDeviceActivities;

	public enum ClientDeviceApiResponseSupplement {
		CLIENT_DEVICE_PUSH_TOKENS,
		CLIENT_DEVICE_ACTIVITIES
	}

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface ClientDeviceApiResponseFactory {
		@Nonnull
		ClientDeviceApiResponse create(@Nonnull ClientDevice clientDevice);

		@Nonnull
		ClientDeviceApiResponse create(@Nonnull ClientDevice clientDevice,
																	 @Nonnull Set<ClientDeviceApiResponseSupplement> supplements);
	}

	@AssistedInject
	public ClientDeviceApiResponse(@Nonnull ClientDeviceService clientDeviceService,
																 @Nonnull ClientDevicePushTokenApiResponseFactory clientDevicePushTokenApiResponseFactory,
																 @Nonnull ClientDeviceActivityApiResponseFactory clientDeviceActivityApiResponseFactory,
																 @Nonnull Formatter formatter,
																 @Nonnull Strings strings,
																 @Assisted @Nonnull ClientDevice clientDevice) {
		this(clientDeviceService,
				clientDevicePushTokenApiResponseFactory,
				clientDeviceActivityApiResponseFactory,
				formatter,
				strings,
				clientDevice,
				null);
	}

	@AssistedInject
	public ClientDeviceApiResponse(@Nonnull ClientDeviceService clientDeviceService,
																 @Nonnull ClientDevicePushTokenApiResponseFactory clientDevicePushTokenApiResponseFactory,
																 @Nonnull ClientDeviceActivityApiResponseFactory clientDeviceActivityApiResponseFactory,
																 @Nonnull Formatter formatter,
																 @Nonnull Strings strings,
																 @Assisted @Nonnull ClientDevice clientDevice,
																 @Assisted @Nullable Set<ClientDeviceApiResponseSupplement> supplements) {
		requireNonNull(clientDeviceService);
		requireNonNull(clientDevicePushTokenApiResponseFactory);
		requireNonNull(clientDeviceActivityApiResponseFactory);
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(clientDevice);

		if (supplements == null)
			supplements = Set.of();

		this.clientDeviceId = clientDevice.getClientDeviceId();
		this.clientDeviceTypeId = clientDevice.getClientDeviceTypeId();
		this.fingerprint = clientDevice.getFingerprint();
		this.operatingSystemName = clientDevice.getOperatingSystemName();
		this.operatingSystemVersion = clientDevice.getOperatingSystemVersion();
		this.model = clientDevice.getModel();
		this.brand = clientDevice.getBrand();
		this.created = clientDevice.getCreated();
		this.createdDescription = formatter.formatTimestamp(clientDevice.getCreated());
		this.lastUpdated = clientDevice.getLastUpdated();
		this.lastUpdatedDescription = formatter.formatTimestamp(clientDevice.getLastUpdated());

		if (supplements.contains(ClientDeviceApiResponseSupplement.CLIENT_DEVICE_PUSH_TOKENS)) {
			List<ClientDevicePushToken> clientDevicePushTokens = clientDeviceService.findClientDevicePushTokensByClientDeviceId(clientDevice.getClientDeviceId());
			this.clientDevicePushTokens = clientDevicePushTokens.stream()
					.map(clientDevicePushToken -> clientDevicePushTokenApiResponseFactory.create(clientDevicePushToken))
					.collect(Collectors.toList());
		} else {
			this.clientDevicePushTokens = null;
		}

		if (supplements.contains(ClientDeviceApiResponseSupplement.CLIENT_DEVICE_ACTIVITIES)) {
			List<ClientDeviceActivity> clientDeviceActivities = clientDeviceService.findClientDeviceActivitiesByClientDeviceId(clientDevice.getClientDeviceId());
			this.clientDeviceActivities = clientDeviceActivities.stream()
					.map(clientDeviceActivity -> clientDeviceActivityApiResponseFactory.create(clientDeviceActivity))
					.collect(Collectors.toList());
		} else {
			this.clientDeviceActivities = null;
		}
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
	public UUID getFingerprint() {
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
	public Optional<String> getModel() {
		return Optional.ofNullable(this.model);
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

	@Nonnull
	public Optional<List<ClientDevicePushTokenApiResponse>> getClientDevicePushTokens() {
		return Optional.ofNullable(this.clientDevicePushTokens);
	}

	@Nonnull
	public Optional<List<ClientDeviceActivityApiResponse>> getClientDeviceActivities() {
		return Optional.ofNullable(this.clientDeviceActivities);
	}
}