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

package com.cobaltplatform.api.model.service;

import com.cobaltplatform.api.model.db.ClientDeviceType.ClientDeviceTypeId;
import com.cobaltplatform.api.util.UserAgent;
import com.cobaltplatform.api.util.UserAgentParser;
import com.cobaltplatform.api.util.ValidationUtility;
import com.cobaltplatform.api.util.WebUtility;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify LLC.
 */
@Immutable
public class RemoteClient {
	@Nonnull
	private static final UserAgentParser USER_AGENT_PARSER;

	@Nullable
	private final UUID sessionId;
	@Nullable
	private final UUID fingerprint;
	@Nullable
	private final String appName;
	@Nullable
	private final String appVersion;
	@Nullable
	private final String appBuildNumber;
	@Nullable
	private final ClientDeviceTypeId typeId;
	@Nullable
	private final String operatingSystemName;
	@Nullable
	private final String operatingSystemVersion;
	@Nullable
	private final String model;
	@Nullable
	private final String brand;
	@Nullable
	private final String manufacturer;
	@Nullable
	private final UserAgent userAgent;
	@Nullable
	private final String ipAddress;

	static {
		USER_AGENT_PARSER = new UserAgentParser();
	}

	private RemoteClient(@Nullable UUID sessionId,
											 @Nullable UUID fingerprint,
											 @Nullable String appName,
											 @Nullable String appVersion,
											 @Nullable String appBuildNumber,
											 @Nullable ClientDeviceTypeId typeId,
											 @Nullable String operatingSystemName,
											 @Nullable String operatingSystemVersion,
											 @Nullable String model,
											 @Nullable String brand,
											 @Nullable String manufacturer,
											 @Nullable UserAgent userAgent,
											 @Nullable String ipAddress) {
		this.sessionId = sessionId;
		this.fingerprint = fingerprint;
		this.appName = appName;
		this.appVersion = appVersion;
		this.appBuildNumber = appBuildNumber;
		this.typeId = typeId;
		this.operatingSystemName = operatingSystemName;
		this.operatingSystemVersion = operatingSystemVersion;
		this.model = model;
		this.brand = brand;
		this.manufacturer = manufacturer;
		this.userAgent = userAgent;
		this.ipAddress = ipAddress;
	}

	@Nonnull
	public static RemoteClient fromHttpServletRequest(@Nonnull HttpServletRequest httpServletRequest) {
		requireNonNull(httpServletRequest);

		UserAgent userAgent = USER_AGENT_PARSER.parse(httpServletRequest);
		String ipAddressFromHeader = trimToNull(httpServletRequest.getHeader("X-Real-IP"));
		String ipAddress = ipAddressFromHeader == null ? httpServletRequest.getRemoteAddr() : ipAddressFromHeader;

		String fingerprintAsString = WebUtility.extractValueFromRequest(httpServletRequest, "X-Client-Device-Fingerprint").orElse(null);
		UUID fingerprint = null;

		if (fingerprintAsString != null && ValidationUtility.isValidUUID(fingerprintAsString))
			fingerprint = UUID.fromString(fingerprintAsString);

		String appName = WebUtility.extractValueFromRequest(httpServletRequest, "X-Client-Device-App-Name").orElse(null);
		String appVersion = WebUtility.extractValueFromRequest(httpServletRequest, "X-Client-Device-App-Version").orElse(null);
		String appBuildNumber = WebUtility.extractValueFromRequest(httpServletRequest, "X-Client-Device-App-Build-Number").orElse(null);
		String typeIdAsString = WebUtility.extractValueFromRequest(httpServletRequest, "X-Client-Device-Type-Id").orElse(null);
		ClientDeviceTypeId typeId = typeIdAsString == null ? null : ClientDeviceTypeId.valueOf(typeIdAsString);
		String model = WebUtility.extractValueFromRequest(httpServletRequest, "X-Client-Device-Model").orElse(null);
		String brand = WebUtility.extractValueFromRequest(httpServletRequest, "X-Client-Device-Brand").orElse(null);
		String manufacturer = WebUtility.extractValueFromRequest(httpServletRequest, "X-Client-Device-Manufacturer").orElse(null);
		String operatingSystemName = WebUtility.extractValueFromRequest(httpServletRequest, "X-Client-Device-Operating-System-Name").orElse(null);
		String operatingSystemVersion = WebUtility.extractValueFromRequest(httpServletRequest, "X-Client-Device-Operating-System-Version").orElse(null);

		String sessionIdAsString = WebUtility.extractValueFromRequest(httpServletRequest, "X-Client-Device-Session-Id").orElse(null);
		UUID sessionId = null;

		if (sessionIdAsString != null && ValidationUtility.isValidUUID(sessionIdAsString))
			sessionId = UUID.fromString(sessionIdAsString);

		return new RemoteClient(
				sessionId,
				fingerprint,
				appName,
				appVersion,
				appBuildNumber,
				typeId,
				operatingSystemName,
				operatingSystemVersion,
				model,
				brand,
				manufacturer,
				userAgent,
				ipAddress
		);
	}

	@Override
	public String toString() {
		return getDescription();
	}

	@Nonnull
	public String getDescription() {
		String appVersion = getAppVersion().orElse(null);
		ClientDeviceTypeId clientDeviceTypeId = getTypeId().orElse(null);
		UserAgent userAgent = getUserAgent().orElse(null);

		if ((appVersion == null || clientDeviceTypeId == null) && userAgent != null)
			return userAgent.getDescription();

		String operatingSystemName = getOperatingSystemName().orElse(null);

		if (operatingSystemName == null && userAgent != null)
			operatingSystemName = userAgent.getOperatingSystemName().orElse("Unknown OS");

		String operatingSystemVersion = getOperatingSystemVersion().orElse(null);

		if (operatingSystemVersion == null && userAgent != null)
			operatingSystemVersion = userAgent.getOperatingSystemVersion().orElse("Unknown OS Version");

		String model = getModel().orElse("Unknown Model");

		String clientDeviceTypeIdDescription = "Unknown App";

		if (clientDeviceTypeId == ClientDeviceTypeId.ANDROID_APP)
			clientDeviceTypeIdDescription = "Android App";
		else if (clientDeviceTypeId == ClientDeviceTypeId.IOS_APP)
			clientDeviceTypeIdDescription = "iOS App";

		return format("%s %s %s (App %s on %s)", clientDeviceTypeIdDescription, operatingSystemName, operatingSystemVersion, appVersion, model);
	}

	@Nonnull
	public Optional<UUID> getSessionId() {
		return Optional.ofNullable(this.sessionId);
	}

	@Nonnull
	public Optional<UUID> getFingerprint() {
		return Optional.ofNullable(this.fingerprint);
	}

	@Nonnull
	public Optional<String> getAppName() {
		return Optional.ofNullable(this.appName);
	}

	@Nonnull
	public Optional<String> getAppVersion() {
		return Optional.ofNullable(this.appVersion);
	}

	@Nonnull
	public Optional<String> getAppBuildNumber() {
		return Optional.ofNullable(this.appBuildNumber);
	}

	@Nonnull
	public Optional<ClientDeviceTypeId> getTypeId() {
		return Optional.ofNullable(this.typeId);
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
	public Optional<String> getManufacturer() {
		return Optional.ofNullable(this.manufacturer);
	}

	@Nonnull
	public Optional<UserAgent> getUserAgent() {
		return Optional.ofNullable(this.userAgent);
	}

	@Nonnull
	public Optional<String> getIpAddress() {
		return Optional.ofNullable(this.ipAddress);
	}
}