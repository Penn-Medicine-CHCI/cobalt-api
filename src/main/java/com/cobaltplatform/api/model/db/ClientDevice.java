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
	private String operatingSystemName;
	@Nullable
	private String operatingSystemVersion;
	@Nullable
	private String modelName;
	@Nullable
	private String brand;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

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
	public String getModelName() {
		return modelName;
	}

	public void setModelName(@Nullable String modelName) {
		this.modelName = modelName;
	}

	@Nullable
	public String getBrand() {
		return this.brand;
	}

	public void setBrand(@Nullable String brand) {
		this.brand = brand;
	}

	@Nullable
	public Instant getCreated() {
		return this.created;
	}

	public void setCreated(@Nullable Instant created) {
		this.created = created;
	}

	@Nullable
	public Instant getLastUpdated() {
		return this.lastUpdated;
	}

	public void setLastUpdated(@Nullable Instant lastUpdated) {
		this.lastUpdated = lastUpdated;
	}
}