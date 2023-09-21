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

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class MixpanelEvent {
	@Nonnull
	private final String distinctId;
	@Nonnull
	private final Instant time;
	@Nonnull
	private final String event;
	@Nonnull
	private final Map<String, Object> properties;

	public MixpanelEvent(@Nonnull String distinctId,
											 @Nonnull Instant time,
											 @Nonnull String event,
											 @Nonnull Map<String, Object> properties) {
		requireNonNull(distinctId);
		requireNonNull(time);
		requireNonNull(event);
		requireNonNull(properties);

		this.distinctId = distinctId;
		this.time = time;
		this.event = event;
		this.properties = Collections.unmodifiableMap(properties);
	}

	@Override
	@Nonnull
	public String toString() {
		return format("%s{event=%s, distinctId=%s, time=%s, properties=%s}",
				getClass().getSimpleName(), getEvent(), getDistinctId(), getTime(), getProperties());
	}

	@Nonnull
	public String getDistinctId() {
		return this.distinctId;
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
