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
public class PatientOrderScheduledMessage {
	@Nullable
	private UUID patientOrderScheduledMessageId;
	@Nullable
	private UUID patientOrderId;
	@Nullable
	private UUID scheduledMessageId;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nullable
	private String patientOrderScheduledMessageTypeDescription;
	@Nullable
	private ScheduledMessageStatusId scheduledMessageStatusId;
	@Nullable
	private UUID scheduledByAccountId;
	@Nullable
	private ScheduledMessageSourceId scheduledMessageSourceId;
	@Nullable
	private MessageTypeId messageTypeId;
	@Nullable
	private String messageTypeDescription;
	@Nullable
	private LocalDateTime scheduledAt;
	@Nullable
	private ZoneId timeZone;
	@Nullable
	private Instant processedAt;
	@Nullable
	private Instant canceledAt;
	@Nullable
	private Instant erroredAt;

	@Nullable
	public UUID getPatientOrderScheduledMessageId() {
		return this.patientOrderScheduledMessageId;
	}

	public void setPatientOrderScheduledMessageId(@Nullable UUID patientOrderScheduledMessageId) {
		this.patientOrderScheduledMessageId = patientOrderScheduledMessageId;
	}

	@Nullable
	public UUID getPatientOrderId() {
		return this.patientOrderId;
	}

	public void setPatientOrderId(@Nullable UUID patientOrderId) {
		this.patientOrderId = patientOrderId;
	}

	@Nullable
	public UUID getScheduledMessageId() {
		return this.scheduledMessageId;
	}

	public void setScheduledMessageId(@Nullable UUID scheduledMessageId) {
		this.scheduledMessageId = scheduledMessageId;
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

	@Nullable
	public String getPatientOrderScheduledMessageTypeDescription() {
		return this.patientOrderScheduledMessageTypeDescription;
	}

	public void setPatientOrderScheduledMessageTypeDescription(@Nullable String patientOrderScheduledMessageTypeDescription) {
		this.patientOrderScheduledMessageTypeDescription = patientOrderScheduledMessageTypeDescription;
	}

	@Nullable
	public ScheduledMessageStatusId getScheduledMessageStatusId() {
		return this.scheduledMessageStatusId;
	}

	public void setScheduledMessageStatusId(@Nullable ScheduledMessageStatusId scheduledMessageStatusId) {
		this.scheduledMessageStatusId = scheduledMessageStatusId;
	}

	@Nullable
	public UUID getScheduledByAccountId() {
		return this.scheduledByAccountId;
	}

	public void setScheduledByAccountId(@Nullable UUID scheduledByAccountId) {
		this.scheduledByAccountId = scheduledByAccountId;
	}

	@Nullable
	public ScheduledMessageSourceId getScheduledMessageSourceId() {
		return this.scheduledMessageSourceId;
	}

	public void setScheduledMessageSourceId(@Nullable ScheduledMessageSourceId scheduledMessageSourceId) {
		this.scheduledMessageSourceId = scheduledMessageSourceId;
	}

	@Nullable
	public MessageTypeId getMessageTypeId() {
		return this.messageTypeId;
	}

	public void setMessageTypeId(@Nullable MessageTypeId messageTypeId) {
		this.messageTypeId = messageTypeId;
	}

	@Nullable
	public String getMessageTypeDescription() {
		return this.messageTypeDescription;
	}

	public void setMessageTypeDescription(@Nullable String messageTypeDescription) {
		this.messageTypeDescription = messageTypeDescription;
	}

	@Nullable
	public LocalDateTime getScheduledAt() {
		return this.scheduledAt;
	}

	public void setScheduledAt(@Nullable LocalDateTime scheduledAt) {
		this.scheduledAt = scheduledAt;
	}

	@Nullable
	public ZoneId getTimeZone() {
		return this.timeZone;
	}

	public void setTimeZone(@Nullable ZoneId timeZone) {
		this.timeZone = timeZone;
	}

	@Nullable
	public Instant getProcessedAt() {
		return this.processedAt;
	}

	public void setProcessedAt(@Nullable Instant processedAt) {
		this.processedAt = processedAt;
	}

	@Nullable
	public Instant getCanceledAt() {
		return this.canceledAt;
	}

	public void setCanceledAt(@Nullable Instant canceledAt) {
		this.canceledAt = canceledAt;
	}

	@Nullable
	public Instant getErroredAt() {
		return this.erroredAt;
	}

	public void setErroredAt(@Nullable Instant erroredAt) {
		this.erroredAt = erroredAt;
	}
}
