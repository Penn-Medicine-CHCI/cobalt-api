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

package com.cobaltplatform.api.model.db;

import com.cobaltplatform.api.model.db.ClientDeviceType.ClientDeviceTypeId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class ClientDevice {
	@Nullable
	private UUID clientDeviceId;
	@Nullable
	private ClientDeviceTypeId clientDeviceTypeId;
	@Nullable
	private String fingerprint;
	@Nullable
	private String pushToken;
	@Nullable
	private String operatingSystemName;
	@Nullable
	private String operatingSystemVersion;
	@Nullable
	private String browserName;
	@Nullable
	private String browserVersion;
	@Nullable
	private String modelName;
	@Nullable
	private String appVersion;
	@Nullable
	private Instant createdTimestamp;
	@Nullable
	private UUID createdAccountId;
	@Nullable
	private Instant lastUpdatedTimestamp;
	@Nullable
	private UUID lastUpdatedAccountId;

	@Nullable
	public UUID getClientDeviceId() {
		return clientDeviceId;
	}

	public void setClientDeviceId(@Nullable UUID clientDeviceId) {
		this.clientDeviceId = clientDeviceId;
	}

	@Nullable
	public ClientDeviceTypeId getClientDeviceTypeId() {
		return clientDeviceTypeId;
	}

	public void setClientDeviceTypeId(@Nullable ClientDeviceTypeId clientDeviceTypeId) {
		this.clientDeviceTypeId = clientDeviceTypeId;
	}

	@Nullable
	public String getFingerprint() {
		return fingerprint;
	}

	public void setFingerprint(@Nullable String fingerprint) {
		this.fingerprint = fingerprint;
	}

	@Nullable
	public String getPushToken() {
		return pushToken;
	}

	public void setPushToken(@Nullable String pushToken) {
		this.pushToken = pushToken;
	}

	@Nullable
	public String getOperatingSystemName() {
		return operatingSystemName;
	}

	public void setOperatingSystemName(@Nullable String operatingSystemName) {
		this.operatingSystemName = operatingSystemName;
	}

	@Nullable
	public String getOperatingSystemVersion() {
		return operatingSystemVersion;
	}

	public void setOperatingSystemVersion(@Nullable String operatingSystemVersion) {
		this.operatingSystemVersion = operatingSystemVersion;
	}

	@Nullable
	public String getBrowserName() {
		return browserName;
	}

	public void setBrowserName(@Nullable String browserName) {
		this.browserName = browserName;
	}

	@Nullable
	public String getBrowserVersion() {
		return browserVersion;
	}

	public void setBrowserVersion(@Nullable String browserVersion) {
		this.browserVersion = browserVersion;
	}

	@Nullable
	public String getModelName() {
		return modelName;
	}

	public void setModelName(@Nullable String modelName) {
		this.modelName = modelName;
	}

	@Nullable
	public String getAppVersion() {
		return appVersion;
	}

	public void setAppVersion(@Nullable String appVersion) {
		this.appVersion = appVersion;
	}

	@Nullable
	public Instant getCreatedTimestamp() {
		return createdTimestamp;
	}

	public void setCreatedTimestamp(@Nullable Instant createdTimestamp) {
		this.createdTimestamp = createdTimestamp;
	}

	@Nullable
	public UUID getCreatedAccountId() {
		return createdAccountId;
	}

	public void setCreatedAccountId(@Nullable UUID createdAccountId) {
		this.createdAccountId = createdAccountId;
	}

	@Nullable
	public Instant getLastUpdatedTimestamp() {
		return lastUpdatedTimestamp;
	}

	public void setLastUpdatedTimestamp(@Nullable Instant lastUpdatedTimestamp) {
		this.lastUpdatedTimestamp = lastUpdatedTimestamp;
	}

	@Nullable
	public UUID getLastUpdatedAccountId() {
		return lastUpdatedAccountId;
	}

	public void setLastUpdatedAccountId(@Nullable UUID lastUpdatedAccountId) {
		this.lastUpdatedAccountId = lastUpdatedAccountId;
	}
}
