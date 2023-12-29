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

import com.cobaltplatform.api.model.db.ClientDevicePushTokenType.ClientDevicePushTokenTypeId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class ClientDevicePushToken {
	@Nullable
	private UUID clientDevicePushTokenId;
	@Nullable
	private UUID clientDeviceId;
	@Nullable
	private ClientDevicePushTokenTypeId clientDevicePushTokenTypeId;
	@Nullable
	private String pushToken;
	@Nullable
	private Boolean valid;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nullable
	public UUID getClientDevicePushTokenId() {
		return this.clientDevicePushTokenId;
	}

	public void setClientDevicePushTokenId(@Nullable UUID clientDevicePushTokenId) {
		this.clientDevicePushTokenId = clientDevicePushTokenId;
	}

	@Nullable
	public UUID getClientDeviceId() {
		return this.clientDeviceId;
	}

	public void setClientDeviceId(@Nullable UUID clientDeviceId) {
		this.clientDeviceId = clientDeviceId;
	}

	@Nullable
	public ClientDevicePushTokenTypeId getClientDevicePushTokenTypeId() {
		return this.clientDevicePushTokenTypeId;
	}

	public void setClientDevicePushTokenTypeId(@Nullable ClientDevicePushTokenTypeId clientDevicePushTokenTypeId) {
		this.clientDevicePushTokenTypeId = clientDevicePushTokenTypeId;
	}

	@Nullable
	public String getPushToken() {
		return this.pushToken;
	}

	public void setPushToken(@Nullable String pushToken) {
		this.pushToken = pushToken;
	}

	@Nullable
	public Boolean getValid() {
		return this.valid;
	}

	public void setValid(@Nullable Boolean valid) {
		this.valid = valid;
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