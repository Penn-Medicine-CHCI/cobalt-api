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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.UUID;

import static java.lang.String.format;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class ClientDeviceType {
	@Nullable
	private ClientDeviceTypeId clientDeviceTypeId;
	@Nullable
	private String description;
	@Nullable
	private Instant createdTimestamp;
	@Nullable
	private UUID createdAccountId;
	@Nullable
	private Instant lastUpdatedTimestamp;
	@Nullable
	private UUID lastUpdatedAccountId;

	public enum ClientDeviceTypeId {
		IOS_APP,
		ANDROID_APP,
		WEB_BROWSER;

		@Nonnull
		public static Boolean isNativeApp(@Nullable ClientDeviceTypeId clientDeviceTypeId) {
			return clientDeviceTypeId == IOS_APP || clientDeviceTypeId == ANDROID_APP;
		}
	}

	@Override
	public String toString() {
		return format("%s{clientDeviceTypeId=%s, description=%s}", getClass().getSimpleName(), getClientDeviceTypeId(), getDescription());
	}

	@Nullable
	public ClientDeviceTypeId getClientDeviceTypeId() {
		return clientDeviceTypeId;
	}

	public void setClientDeviceTypeId(@Nullable ClientDeviceTypeId clientDeviceTypeId) {
		this.clientDeviceTypeId = clientDeviceTypeId;
	}

	@Nullable
	public String getDescription() {
		return description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
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
