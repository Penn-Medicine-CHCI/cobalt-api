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

import com.cobaltplatform.api.model.db.MessageStatus.MessageStatusId;
import com.cobaltplatform.api.model.db.MessageType.MessageTypeId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class MessageLog {
	@Nullable
	private UUID messageId;
	@Nullable
	private MessageTypeId messageTypeId;
	@Nullable
	private MessageStatusId messageStatusId;
	@Nullable
	private String serializedMessage;
	@Nullable
	private String stackTrace;
	@Nullable
	private Instant created;
	@Nullable
	private Instant enqueued;
	@Nullable
	private Instant processed;

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
	public MessageStatusId getMessageStatusId() {
		return messageStatusId;
	}

	public void setMessageStatusId(@Nullable MessageStatusId messageStatusId) {
		this.messageStatusId = messageStatusId;
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
	public Instant getCreated() {
		return created;
	}

	public void setCreated(@Nullable Instant created) {
		this.created = created;
	}

	@Nullable
	public Instant getEnqueued() {
		return enqueued;
	}

	public void setEnqueued(@Nullable Instant enqueued) {
		this.enqueued = enqueued;
	}

	@Nullable
	public Instant getProcessed() {
		return processed;
	}

	public void setProcessed(@Nullable Instant processed) {
		this.processed = processed;
	}
}