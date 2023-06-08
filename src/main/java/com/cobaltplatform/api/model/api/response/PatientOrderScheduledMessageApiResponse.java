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

import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.MessageStatus.MessageStatusId;
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
import java.util.List;
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
	private final InstitutionId institutionId;
	@Nonnull
	private final UUID scheduledMessageId;
	@Nonnull
	private final ScheduledMessageStatusId scheduledMessageStatusId;
	@Nonnull
	private final MessageTypeId messageTypeId;
	@Nonnull
	private final String messageTypeDescription;
	@Nonnull
	private final UUID messageId;
	@Nonnull
	private final MessageStatusId messageStatusId;
	@Nonnull
	private final String messageStatusDescription;
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
	@Nullable
	private final Instant sentAt;
	@Nullable
	private final String sentAtDescription;
	@Nullable
	private final Instant deliveredAt;
	@Nullable
	private final String deliveredAtDescription;
	@Nullable
	private final Instant deliveryFailedAt;
	@Nullable
	private final String deliveryFailedAtDescription;
	@Nullable
	private final String deliveryFailedReason;
	@Nullable
	private final Instant complaintRegisteredAt;
	@Nullable
	private final String complaintRegisteredAtDescription;
	@Nullable
	private final String smsToNumber; // Only populated for SMSes
	@Nullable
	private final String smsToNumberDescription; // Only populated for SMSes
	@Nullable
	private final List<String> emailToAddresses; // Only populated for emails

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
		this.institutionId = patientOrderScheduledMessage.getInstitutionId();
		this.scheduledMessageId = patientOrderScheduledMessage.getScheduledMessageId();
		this.scheduledMessageStatusId = patientOrderScheduledMessage.getScheduledMessageStatusId();
		this.messageTypeId = patientOrderScheduledMessage.getMessageTypeId();
		this.messageTypeDescription = patientOrderScheduledMessage.getMessageTypeDescription();
		this.messageId = patientOrderScheduledMessage.getMessageId();
		this.messageStatusId = patientOrderScheduledMessage.getMessageStatusId();
		this.messageStatusDescription = patientOrderScheduledMessage.getMessageStatusDescription();
		this.processedAt = patientOrderScheduledMessage.getProcessedAt();
		this.processedAtDescription = patientOrderScheduledMessage.getProcessedAt() == null ? null : formatter.formatTimestamp(patientOrderScheduledMessage.getProcessedAt(), FormatStyle.MEDIUM, FormatStyle.SHORT);
		this.canceledAt = patientOrderScheduledMessage.getCanceledAt();
		this.canceledAtDescription = patientOrderScheduledMessage.getCanceledAt() == null ? null : formatter.formatTimestamp(patientOrderScheduledMessage.getCanceledAt(), FormatStyle.MEDIUM, FormatStyle.SHORT);
		this.erroredAt = patientOrderScheduledMessage.getErroredAt();
		this.erroredAtDescription = patientOrderScheduledMessage.getErroredAt() == null ? null : formatter.formatTimestamp(patientOrderScheduledMessage.getErroredAt(), FormatStyle.MEDIUM, FormatStyle.SHORT);
		this.sentAt = patientOrderScheduledMessage.getSentAt();
		this.sentAtDescription = patientOrderScheduledMessage.getSentAt() == null ? null : formatter.formatTimestamp(patientOrderScheduledMessage.getSentAt(), FormatStyle.MEDIUM, FormatStyle.SHORT);
		this.deliveredAt = patientOrderScheduledMessage.getDeliveredAt();
		this.deliveredAtDescription = patientOrderScheduledMessage.getDeliveredAt() == null ? null : formatter.formatTimestamp(patientOrderScheduledMessage.getDeliveredAt(), FormatStyle.MEDIUM, FormatStyle.SHORT);
		this.deliveryFailedAt = patientOrderScheduledMessage.getDeliveryFailedAt();
		this.deliveryFailedAtDescription = patientOrderScheduledMessage.getDeliveryFailedAt() == null ? null : formatter.formatTimestamp(patientOrderScheduledMessage.getDeliveryFailedAt(), FormatStyle.MEDIUM, FormatStyle.SHORT);
		this.deliveryFailedReason = patientOrderScheduledMessage.getDeliveryFailedReason();
		this.complaintRegisteredAt = patientOrderScheduledMessage.getComplaintRegisteredAt();
		this.complaintRegisteredAtDescription = patientOrderScheduledMessage.getComplaintRegisteredAt() == null ? null : formatter.formatTimestamp(patientOrderScheduledMessage.getComplaintRegisteredAt(), FormatStyle.MEDIUM, FormatStyle.SHORT);
		this.smsToNumber = patientOrderScheduledMessage.getSmsToNumber();
		this.smsToNumberDescription = patientOrderScheduledMessage.getSmsToNumber() == null ? null : formatter.formatPhoneNumber(patientOrderScheduledMessage.getSmsToNumber());
		this.emailToAddresses = patientOrderScheduledMessage.getEmailToAddresses() == null ? List.of() : patientOrderScheduledMessage.getEmailToAddressesAsList();
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

	@Nonnull
	public InstitutionId getInstitutionId() {
		return this.institutionId;
	}

	@Nonnull
	public UUID getMessageId() {
		return this.messageId;
	}

	@Nonnull
	public MessageStatusId getMessageStatusId() {
		return this.messageStatusId;
	}

	@Nonnull
	public String getMessageStatusDescription() {
		return this.messageStatusDescription;
	}

	@Nullable
	public Instant getSentAt() {
		return this.sentAt;
	}

	@Nullable
	public String getSentAtDescription() {
		return this.sentAtDescription;
	}

	@Nullable
	public Instant getDeliveredAt() {
		return this.deliveredAt;
	}

	@Nullable
	public String getDeliveredAtDescription() {
		return this.deliveredAtDescription;
	}

	@Nullable
	public Instant getDeliveryFailedAt() {
		return this.deliveryFailedAt;
	}

	@Nullable
	public String getDeliveryFailedAtDescription() {
		return this.deliveryFailedAtDescription;
	}

	@Nullable
	public String getDeliveryFailedReason() {
		return this.deliveryFailedReason;
	}

	@Nullable
	public Instant getComplaintRegisteredAt() {
		return this.complaintRegisteredAt;
	}

	@Nullable
	public String getComplaintRegisteredAtDescription() {
		return this.complaintRegisteredAtDescription;
	}

	@Nullable
	public String getSmsToNumber() {
		return this.smsToNumber;
	}

	@Nullable
	public String getSmsToNumberDescription() {
		return this.smsToNumberDescription;
	}

	@Nullable
	public List<String> getEmailToAddresses() {
		return this.emailToAddresses;
	}
}