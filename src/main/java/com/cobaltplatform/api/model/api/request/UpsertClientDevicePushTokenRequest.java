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

import com.cobaltplatform.api.model.db.PushTokenType.PushTokenTypeId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class UpsertClientDevicePushTokenRequest {
	@Nullable
	private UUID accountId;
	@Nullable
	private String fingerprint;
	@Nullable
	private PushTokenTypeId pushTokenTypeId;
	@Nullable
	private String pushToken;

	@Nullable
	public UUID getAccountId() {
		return this.accountId;
	}

	public void setAccountId(@Nullable UUID accountId) {
		this.accountId = accountId;
	}

	@Nullable
	public String getFingerprint() {
		return this.fingerprint;
	}

	public void setFingerprint(@Nullable String fingerprint) {
		this.fingerprint = fingerprint;
	}

	@Nullable
	public PushTokenTypeId getPushTokenTypeId() {
		return this.pushTokenTypeId;
	}

	public void setPushTokenTypeId(@Nullable PushTokenTypeId pushTokenTypeId) {
		this.pushTokenTypeId = pushTokenTypeId;
	}

	@Nullable
	public String getPushToken() {
		return this.pushToken;
	}

	public void setPushToken(@Nullable String pushToken) {
		this.pushToken = pushToken;
	}
}