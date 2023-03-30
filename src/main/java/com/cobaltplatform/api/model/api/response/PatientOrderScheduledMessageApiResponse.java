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

package com.cobaltplatform.api.model.api.response;

import com.cobaltplatform.api.model.db.MessageType.MessageTypeId;
import com.cobaltplatform.api.model.db.PatientOrderScheduledMessage;
import com.cobaltplatform.api.model.db.ScheduledMessageStatus.ScheduledMessageStatusId;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.time.format.FormatStyle;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class PatientOrderScheduledMessageApiResponse {
	@Nonnull
	private final UUID patientOrderScheduledMessageId;
	@Nonnull
	private final UUID scheduledMessageId;
	@Nonnull
	private final ScheduledMessageStatusId scheduledMessageStatusId;
	@Nonnull
	private final MessageTypeId messageTypeId;
	@Nonnull
	private final String messageTypeDescription;
	@Nullable
	private final Instant processedAt;
	@Nullable
	private final String processedAtDescription;
	@Nullable
	private final Instant canceledAt;
	@Nullable
	private final String canceledAtDescription;
	@Nullable
	private final Instant erroredAt;
	@Nullable
	private final String erroredAtDescription;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface PatientOrderScheduledMessageApiResponseFactory {
		@Nonnull
		PatientOrderScheduledMessageApiResponse create(@Nonnull PatientOrderScheduledMessage patientOrderScheduledMessage);
	}

	@AssistedInject
	public PatientOrderScheduledMessageApiResponse(@Nonnull Formatter formatter,
																								 @Nonnull Strings strings,
																								 @Assisted @Nonnull PatientOrderScheduledMessage patientOrderScheduledMessage) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(patientOrderScheduledMessage);

		this.patientOrderScheduledMessageId = patientOrderScheduledMessage.getPatientOrderScheduledMessageId();
		this.scheduledMessageId = patientOrderScheduledMessage.getScheduledMessageId();
		this.scheduledMessageStatusId = patientOrderScheduledMessage.getScheduledMessageStatusId();
		this.messageTypeId = patientOrderScheduledMessage.getMessageTypeId();
		this.messageTypeDescription = patientOrderScheduledMessage.getMessageTypeDescription();
		this.processedAt = patientOrderScheduledMessage.getProcessedAt();
		this.processedAtDescription = patientOrderScheduledMessage.getProcessedAt() == null ? null : formatter.formatTimestamp(patientOrderScheduledMessage.getProcessedAt(), FormatStyle.MEDIUM, FormatStyle.SHORT);
		this.canceledAt = patientOrderScheduledMessage.getCanceledAt();
		this.canceledAtDescription = patientOrderScheduledMessage.getCanceledAt() == null ? null : formatter.formatTimestamp(patientOrderScheduledMessage.getCanceledAt(), FormatStyle.MEDIUM, FormatStyle.SHORT);
		this.erroredAt = patientOrderScheduledMessage.getErroredAt();
		this.erroredAtDescription = patientOrderScheduledMessage.getErroredAt() == null ? null : formatter.formatTimestamp(patientOrderScheduledMessage.getErroredAt(), FormatStyle.MEDIUM, FormatStyle.SHORT);
	}

	@Nonnull
	public UUID getPatientOrderScheduledMessageId() {
		return this.patientOrderScheduledMessageId;
	}

	@Nonnull
	public UUID getScheduledMessageId() {
		return this.scheduledMessageId;
	}

	@Nonnull
	public ScheduledMessageStatusId getScheduledMessageStatusId() {
		return this.scheduledMessageStatusId;
	}

	@Nonnull
	public MessageTypeId getMessageTypeId() {
		return this.messageTypeId;
	}

	@Nonnull
	public String getMessageTypeDescription() {
		return this.messageTypeDescription;
	}

	@Nullable
	public Instant getProcessedAt() {
		return this.processedAt;
	}

	@Nullable
	public String getProcessedAtDescription() {
		return this.processedAtDescription;
	}

	@Nullable
	public Instant getCanceledAt() {
		return this.canceledAt;
	}

	@Nullable
	public String getCanceledAtDescription() {
		return this.canceledAtDescription;
	}

	@Nullable
	public Instant getErroredAt() {
		return this.erroredAt;
	}

	@Nullable
	public String getErroredAtDescription() {
		return this.erroredAtDescription;
	}
}