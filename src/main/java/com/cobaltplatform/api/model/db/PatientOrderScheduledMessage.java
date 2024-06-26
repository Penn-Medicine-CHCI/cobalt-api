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

import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.MessageStatus.MessageStatusId;
import com.cobaltplatform.api.model.db.MessageType.MessageTypeId;
import com.cobaltplatform.api.model.db.PatientOrderScheduledMessageType.PatientOrderScheduledMessageTypeId;
import com.cobaltplatform.api.model.db.ScheduledMessageSource.ScheduledMessageSourceId;
import com.cobaltplatform.api.model.db.ScheduledMessageStatus.ScheduledMessageStatusId;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.pyranid.DatabaseColumn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class PatientOrderScheduledMessage {
	@Nonnull
	private static final Gson GSON;

	static {
		GSON = new GsonBuilder()
				.setPrettyPrinting()
				.disableHtmlEscaping()
				.create();
	}

	@Nullable
	private UUID patientOrderScheduledMessageId;
	@Nullable
	private UUID patientOrderScheduledMessageGroupId;
	@Nullable
	private UUID scheduledMessageId;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	// From v_patient_order_scheduled_message

	@Nullable
	private PatientOrderScheduledMessageTypeId patientOrderScheduledMessageTypeId;
	@Nullable
	private UUID patientOrderId;
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
	private InstitutionId institutionId;
	@Nullable
	private UUID messageId;
	@Nullable
	private MessageStatusId messageStatusId;
	@Nullable
	private String messageStatusDescription;
	@Nullable
	private Instant sentAt;
	@Nullable
	private Instant deliveredAt;
	@Nullable
	private Instant deliveryFailedAt;
	@Nullable
	private String deliveryFailedReason;
	@Nullable
	private Instant complaintRegisteredAt;
	@Nullable
	private String smsToNumber;
	@Nullable
	@DatabaseColumn("email_to_addresses")
	private String emailToAddresses;
	@Nullable
	private List<String> emailToAddressesAsList;

	@Nonnull
	protected Gson getGson() {
		return GSON;
	}

	public void setEmailToAddresses(@Nullable String emailToAddresses) {
		this.emailToAddresses = emailToAddresses;

		emailToAddresses = trimToNull(emailToAddresses);
		this.emailToAddressesAsList = emailToAddresses == null ? List.of() : getGson().fromJson(emailToAddresses, new TypeToken<List<String>>() {
		}.getType());
	}

	@Nullable
	public String getEmailToAddresses() {
		return this.emailToAddresses;
	}

	@Nullable
	public List<String> getEmailToAddressesAsList() {
		return this.emailToAddressesAsList;
	}

	@Nullable
	public UUID getPatientOrderScheduledMessageId() {
		return this.patientOrderScheduledMessageId;
	}

	public void setPatientOrderScheduledMessageId(@Nullable UUID patientOrderScheduledMessageId) {
		this.patientOrderScheduledMessageId = patientOrderScheduledMessageId;
	}

	@Nullable
	public UUID getScheduledMessageId() {
		return this.scheduledMessageId;
	}

	public void setScheduledMessageId(@Nullable UUID scheduledMessageId) {
		this.scheduledMessageId = scheduledMessageId;
	}

	@Nullable
	public UUID getPatientOrderScheduledMessageGroupId() {
		return this.patientOrderScheduledMessageGroupId;
	}

	public void setPatientOrderScheduledMessageGroupId(@Nullable UUID patientOrderScheduledMessageGroupId) {
		this.patientOrderScheduledMessageGroupId = patientOrderScheduledMessageGroupId;
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
	public PatientOrderScheduledMessageTypeId getPatientOrderScheduledMessageTypeId() {
		return this.patientOrderScheduledMessageTypeId;
	}

	public void setPatientOrderScheduledMessageTypeId(@Nullable PatientOrderScheduledMessageTypeId patientOrderScheduledMessageTypeId) {
		this.patientOrderScheduledMessageTypeId = patientOrderScheduledMessageTypeId;
	}

	@Nullable
	public UUID getPatientOrderId() {
		return this.patientOrderId;
	}

	public void setPatientOrderId(@Nullable UUID patientOrderId) {
		this.patientOrderId = patientOrderId;
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

	@Nullable
	public InstitutionId getInstitutionId() {
		return this.institutionId;
	}

	public void setInstitutionId(@Nullable InstitutionId institutionId) {
		this.institutionId = institutionId;
	}

	@Nullable
	public UUID getMessageId() {
		return this.messageId;
	}

	public void setMessageId(@Nullable UUID messageId) {
		this.messageId = messageId;
	}

	@Nullable
	public MessageStatusId getMessageStatusId() {
		return this.messageStatusId;
	}

	public void setMessageStatusId(@Nullable MessageStatusId messageStatusId) {
		this.messageStatusId = messageStatusId;
	}

	@Nullable
	public String getMessageStatusDescription() {
		return this.messageStatusDescription;
	}

	public void setMessageStatusDescription(@Nullable String messageStatusDescription) {
		this.messageStatusDescription = messageStatusDescription;
	}

	@Nullable
	public Instant getSentAt() {
		return this.sentAt;
	}

	public void setSentAt(@Nullable Instant sentAt) {
		this.sentAt = sentAt;
	}

	@Nullable
	public Instant getDeliveredAt() {
		return this.deliveredAt;
	}

	public void setDeliveredAt(@Nullable Instant deliveredAt) {
		this.deliveredAt = deliveredAt;
	}

	@Nullable
	public Instant getDeliveryFailedAt() {
		return this.deliveryFailedAt;
	}

	public void setDeliveryFailedAt(@Nullable Instant deliveryFailedAt) {
		this.deliveryFailedAt = deliveryFailedAt;
	}

	@Nullable
	public String getDeliveryFailedReason() {
		return this.deliveryFailedReason;
	}

	public void setDeliveryFailedReason(@Nullable String deliveryFailedReason) {
		this.deliveryFailedReason = deliveryFailedReason;
	}

	@Nullable
	public Instant getComplaintRegisteredAt() {
		return this.complaintRegisteredAt;
	}

	public void setComplaintRegisteredAt(@Nullable Instant complaintRegisteredAt) {
		this.complaintRegisteredAt = complaintRegisteredAt;
	}

	@Nullable
	public String getSmsToNumber() {
		return this.smsToNumber;
	}

	public void setSmsToNumber(@Nullable String smsToNumber) {
		this.smsToNumber = smsToNumber;
	}
}