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

package com.cobaltplatform.api.model.api.request;

import com.cobaltplatform.api.model.db.ClientDeviceType.ClientDeviceTypeId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class UpsertClientDeviceRequest {
	@Nullable
	private UUID accountId;
	@Nullable
	private ClientDeviceTypeId clientDeviceTypeId;
	@Nullable
	private String fingerprint;
	@Nullable
	private String modelName;
	@Nullable
	private String operatingSystemName;
	@Nullable
	private String operatingSystemVersion;

	@Nullable
	public UUID getAccountId() {
		return this.accountId;
	}

	public void setAccountId(@Nullable UUID accountId) {
		this.accountId = accountId;
	}

	@Nullable
	public ClientDeviceTypeId getClientDeviceTypeId() {
		return this.clientDeviceTypeId;
	}

	public void setClientDeviceTypeId(@Nullable ClientDeviceTypeId clientDeviceTypeId) {
		this.clientDeviceTypeId = clientDeviceTypeId;
	}

	@Nullable
	public String getFingerprint() {
		return this.fingerprint;
	}

	public void setFingerprint(@Nullable String fingerprint) {
		this.fingerprint = fingerprint;
	}

	@Nullable
	public String getModelName() {
		return this.modelName;
	}

	public void setModelName(@Nullable String modelName) {
		this.modelName = modelName;
	}

	@Nullable
	public String getOperatingSystemName() {
		return this.operatingSystemName;
	}

	public void setOperatingSystemName(@Nullable String operatingSystemName) {
		this.operatingSystemName = operatingSystemName;
	}

	@Nullable
	public String getOperatingSystemVersion() {
		return this.operatingSystemVersion;
	}

	public void setOperatingSystemVersion(@Nullable String operatingSystemVersion) {
		this.operatingSystemVersion = operatingSystemVersion;
	}
}