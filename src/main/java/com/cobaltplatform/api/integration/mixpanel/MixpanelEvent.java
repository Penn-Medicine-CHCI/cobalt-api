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

package com.cobaltplatform.api.integration.mixpanel;

import com.cobaltplatform.api.util.GsonUtility;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class MixpanelEvent {
	@Nonnull
	private static final Gson GSON;

	static {
		GsonBuilder gsonBuilder = new GsonBuilder()
				.setPrettyPrinting()
				.disableHtmlEscaping();

		GsonUtility.applyDefaultTypeAdapters(gsonBuilder);

		GSON = gsonBuilder.create();
	}

	@Nonnull
	private final String distinctId;
	@Nonnull
	private final String anonId;
	@Nullable
	private final String userId;
	@Nonnull
	private final String deviceId;
	@Nonnull
	private final Instant time;
	@Nonnull
	private final String event;
	@Nonnull
	private final Map<String, Object> properties;

	public MixpanelEvent(@Nonnull String distinctId,
											 @Nonnull String anonId,
											 @Nullable String userId,
											 @Nonnull String deviceId,
											 @Nonnull Instant time,
											 @Nonnull String event,
											 @Nonnull Map<String, Object> properties) {
		requireNonNull(distinctId);
		requireNonNull(anonId);
		requireNonNull(deviceId);
		requireNonNull(time);
		requireNonNull(event);
		requireNonNull(properties);

		this.distinctId = distinctId;
		this.anonId = anonId;
		this.userId = userId;
		this.deviceId = deviceId;
		this.time = time;
		this.event = event;
		this.properties = Collections.unmodifiableMap(properties);
	}

	@Override
	@Nonnull
	public String toString() {
		return GSON.toJson(this);
	}

	@Nonnull
	public Optional<String> getPropertiesAsJson() {
		return getProperties() == null ? Optional.empty() : Optional.of(GSON.toJson(getProperties()));
	}

	@Nonnull
	public String getDistinctId() {
		return this.distinctId;
	}

	@Nonnull
	public String getAnonId() {
		return this.anonId;
	}

	@Nonnull
	public Optional<String> getUserId() {
		return Optional.ofNullable(this.userId);
	}

	@Nonnull
	public String getDeviceId() {
		return this.deviceId;
	}

	@Nonnull
	public Instant getTime() {
		return this.time;
	}

	@Nonnull
	public String getEvent() {
		return this.event;
	}

	@Nonnull
	public Map<String, Object> getProperties() {
		return this.properties;
	}
}
