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

import com.cobaltplatform.api.messaging.Message;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class CreateScheduledMessageRequest<T extends Message> {
	@Nullable
	private T message;
	@Nullable
	private LocalDateTime scheduledAt;
	@Nullable
	private ZoneId timeZone;
	@Nullable
	private Map<String, Object> metadata;

	@Nullable
	public T getMessage() {
		return message;
	}

	public void setMessage(@Nullable T message) {
		this.message = message;
	}

	@Nullable
	public LocalDateTime getScheduledAt() {
		return scheduledAt;
	}

	public void setScheduledAt(@Nullable LocalDateTime scheduledAt) {
		this.scheduledAt = scheduledAt;
	}

	@Nullable
	public ZoneId getTimeZone() {
		return timeZone;
	}

	public void setTimeZone(@Nullable ZoneId timeZone) {
		this.timeZone = timeZone;
	}

	@Nullable
	public Map<String, Object> getMetadata() {
		return metadata;
	}

	public void setMetadata(@Nullable Map<String, Object> metadata) {
		this.metadata = metadata;
	}
}
