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

import com.cobaltplatform.api.model.db.MessageType.MessageTypeId;
import com.cobaltplatform.api.model.db.ScheduledMessageSource.ScheduledMessageSourceId;
import com.cobaltplatform.api.model.db.ScheduledMessageStatus.ScheduledMessageStatusId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class ScheduledMessage {
	@Nullable
	private UUID scheduledMessageId;
	@Nullable
	private ScheduledMessageStatusId scheduledMessageStatusId;
	@Nullable
	private ScheduledMessageSourceId scheduledMessageSourceId;
	@Nullable
	private UUID messageId;
	@Nullable
	private UUID scheduledByAccountId;
	@Nullable
	private MessageTypeId messageTypeId;
	@Nullable
	private String serializedMessage;
	@Nullable
	private String stackTrace;
	@Nullable
	private LocalDateTime scheduledAt;
	@Nullable
	private ZoneId timeZone;
	@Nullable
	private String metadata;
	@Nullable
	private Instant createdAt;
	@Nullable
	private Instant processedAt;
	@Nullable
	private Instant canceledAt;
	@Nullable
	private Instant erroredAt;

	@Nullable
	public UUID getScheduledMessageId() {
		return scheduledMessageId;
	}

	public void setScheduledMessageId(@Nullable UUID scheduledMessageId) {
		this.scheduledMessageId = scheduledMessageId;
	}

	@Nullable
	public ScheduledMessageStatusId getScheduledMessageStatusId() {
		return scheduledMessageStatusId;
	}

	public void setScheduledMessageStatusId(@Nullable ScheduledMessageStatusId scheduledMessageStatusId) {
		this.scheduledMessageStatusId = scheduledMessageStatusId;
	}

	@Nullable
	public UUID getMessageId() {
		return messageId;
	}

	public void setMessageId(@Nullable UUID messageId) {
		this.messageId = messageId;
	}

	@Nullable
	public MessageTypeId getMessageTypeId() {
		return messageTypeId;
	}

	public void setMessageTypeId(@Nullable MessageTypeId messageTypeId) {
		this.messageTypeId = messageTypeId;
	}

	@Nullable
	public ScheduledMessageSourceId getScheduledMessageSourceId() {
		return this.scheduledMessageSourceId;
	}

	public void setScheduledMessageSourceId(@Nullable ScheduledMessageSourceId scheduledMessageSourceId) {
		this.scheduledMessageSourceId = scheduledMessageSourceId;
	}

	@Nullable
	public UUID getScheduledByAccountId() {
		return this.scheduledByAccountId;
	}

	public void setScheduledByAccountId(@Nullable UUID scheduledByAccountId) {
		this.scheduledByAccountId = scheduledByAccountId;
	}

	@Nullable
	public String getSerializedMessage() {
		return serializedMessage;
	}

	public void setSerializedMessage(@Nullable String serializedMessage) {
		this.serializedMessage = serializedMessage;
	}

	@Nullable
	public String getStackTrace() {
		return stackTrace;
	}

	public void setStackTrace(@Nullable String stackTrace) {
		this.stackTrace = stackTrace;
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
	public String getMetadata() {
		return metadata;
	}

	public void setMetadata(@Nullable String metadata) {
		this.metadata = metadata;
	}

	@Nullable
	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(@Nullable Instant createdAt) {
		this.createdAt = createdAt;
	}

	@Nullable
	public Instant getProcessedAt() {
		return processedAt;
	}

	public void setProcessedAt(@Nullable Instant processedAt) {
		this.processedAt = processedAt;
	}

	@Nullable
	public Instant getCanceledAt() {
		return canceledAt;
	}

	public void setCanceledAt(@Nullable Instant canceledAt) {
		this.canceledAt = canceledAt;
	}

	@Nullable
	public Instant getErroredAt() {
		return erroredAt;
	}

	public void setErroredAt(@Nullable Instant erroredAt) {
		this.erroredAt = erroredAt;
	}
}