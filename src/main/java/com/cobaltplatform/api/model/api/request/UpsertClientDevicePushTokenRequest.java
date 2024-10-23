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

import com.cobaltplatform.api.model.db.ClientDevicePushTokenType.ClientDevicePushTokenTypeId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class UpsertClientDevicePushTokenRequest {
	@Nullable
	private UUID clientDeviceId;
	@Nullable
	private UUID fingerprint;
	@Nullable
	private ClientDevicePushTokenTypeId clientDevicePushTokenTypeId;
	@Nullable
	private String pushToken;

	@Nullable
	public UUID getClientDeviceId() {
		return this.clientDeviceId;
	}

	public void setClientDeviceId(@Nullable UUID clientDeviceId) {
		this.clientDeviceId = clientDeviceId;
	}

	@Nullable
	public UUID getFingerprint() {
		return this.fingerprint;
	}

	public void setFingerprint(@Nullable UUID fingerprint) {
		this.fingerprint = fingerprint;
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
}