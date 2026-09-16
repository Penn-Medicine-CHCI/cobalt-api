/*
 * Copyright 2021 The University of Pennsylvania and Penn Medicine
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.cobaltplatform.api.model.api.response;

import com.cobaltplatform.api.model.db.CareEncounterScheduledMessage;
import com.cobaltplatform.api.model.db.CareEncounterScheduledMessageType.CareEncounterScheduledMessageTypeId;
import com.cobaltplatform.api.model.db.CareEncounterStatus.CareEncounterStatusId;
import com.cobaltplatform.api.model.db.MessageStatus.MessageStatusId;
import com.cobaltplatform.api.model.db.ScheduledMessageSource.ScheduledMessageSourceId;
import com.cobaltplatform.api.model.db.ScheduledMessageStatus.ScheduledMessageStatusId;
import com.cobaltplatform.api.util.Formatter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

@Immutable
public class CareEncounterScheduledMessageApiResponse {
	@Nonnull private final UUID careEncounterScheduledMessageId;
	@Nonnull private final UUID careEncounterId;
	@Nonnull private final CareEncounterScheduledMessageTypeId careEncounterScheduledMessageTypeId;
	@Nonnull private final String careEncounterScheduledMessageTypeDescription;
	@Nonnull private final UUID scheduledMessageId;
	@Nonnull private final ScheduledMessageStatusId scheduledMessageStatusId;
	@Nonnull private final String scheduledMessageStatusDescription;
	@Nonnull private final ScheduledMessageSourceId scheduledMessageSourceId;
	@Nullable private final UUID scheduledByAccountId;
	@Nullable private final String scheduledByAccountDisplayName;
	@Nonnull private final UUID messageId;
	@Nonnull private final LocalDate scheduledAtDate;
	@Nonnull private final LocalTime scheduledAtTime;
	@Nonnull private final ZoneId timeZone;
	@Nonnull private final Instant scheduledAt;
	@Nonnull private final String scheduledAtDescription;
	@Nullable private final Instant processedAt;
	@Nullable private final String processedAtDescription;
	@Nullable private final Instant canceledAt;
	@Nullable private final String canceledAtDescription;
	@Nullable private final Instant erroredAt;
	@Nullable private final String erroredAtDescription;
	@Nullable private final MessageStatusId messageStatusId;
	@Nullable private final String messageStatusDescription;
	@Nullable private final Instant sentAt;
	@Nullable private final String sentAtDescription;
	@Nullable private final Instant deliveredAt;
	@Nullable private final String deliveredAtDescription;
	@Nullable private final Instant deliveryFailedAt;
	@Nullable private final String deliveryFailedAtDescription;
	@Nullable private final String deliveryFailedReason;
	@Nullable private final Instant complaintRegisteredAt;
	@Nullable private final String complaintRegisteredAtDescription;
	@Nonnull private final String recipientEmailAddress;
	@Nonnull private final String customEmailText;
	@Nonnull private final String emailSubject;
	@Nonnull private final String emailBody;
	@Nonnull private final Boolean editable;
	@Nonnull private final Boolean cancelable;
	@Nonnull private final Boolean deleted;
	@Nullable private final Instant deletedAt;
	@Nullable private final String deletedAtDescription;
	@Nullable private final UUID deletedByAccountId;
	@Nullable private final String deletedByAccountDisplayName;
	@Nonnull private final UUID createdByAccountId;
	@Nullable private final String createdByAccountDisplayName;
	@Nonnull private final UUID lastUpdatedByAccountId;
	@Nullable private final String lastUpdatedByAccountDisplayName;
	@Nonnull private final Instant created;
	@Nonnull private final String createdDescription;
	@Nonnull private final Instant lastUpdated;
	@Nonnull private final String lastUpdatedDescription;

	public CareEncounterScheduledMessageApiResponse(@Nonnull Formatter formatter,
																		 @Nonnull CareEncounterScheduledMessage model) {
		requireNonNull(formatter);
		requireNonNull(model);
		this.careEncounterScheduledMessageId = model.getCareEncounterScheduledMessageId();
		this.careEncounterId = model.getCareEncounterId();
		this.careEncounterScheduledMessageTypeId = model.getCareEncounterScheduledMessageTypeId();
		this.careEncounterScheduledMessageTypeDescription = model.getCareEncounterScheduledMessageTypeDescription();
		this.scheduledMessageId = model.getScheduledMessageId();
		this.scheduledMessageStatusId = model.getScheduledMessageStatusId();
		this.scheduledMessageStatusDescription = model.getScheduledMessageStatusDescription();
		this.scheduledMessageSourceId = model.getScheduledMessageSourceId();
		this.scheduledByAccountId = model.getScheduledByAccountId();
		this.scheduledByAccountDisplayName = model.getScheduledByAccountDisplayName();
		this.messageId = model.getMessageId();
		this.scheduledAtDate = model.getScheduledAt().toLocalDate();
		this.scheduledAtTime = model.getScheduledAt().toLocalTime();
		this.timeZone = model.getTimeZone();
		this.scheduledAt = model.getScheduledAt().atZone(model.getTimeZone()).toInstant();
		this.scheduledAtDescription = formatter.formatTimestampDescription(this.scheduledAt);
		this.processedAt = model.getProcessedAt();
		this.processedAtDescription = describe(formatter, this.processedAt);
		this.canceledAt = model.getCanceledAt();
		this.canceledAtDescription = describe(formatter, this.canceledAt);
		this.erroredAt = model.getErroredAt();
		this.erroredAtDescription = describe(formatter, this.erroredAt);
		this.messageStatusId = model.getMessageStatusId();
		this.messageStatusDescription = model.getMessageStatusDescription();
		this.sentAt = model.getSentAt();
		this.sentAtDescription = describe(formatter, this.sentAt);
		this.deliveredAt = model.getDeliveredAt();
		this.deliveredAtDescription = describe(formatter, this.deliveredAt);
		this.deliveryFailedAt = model.getDeliveryFailedAt();
		this.deliveryFailedAtDescription = describe(formatter, this.deliveryFailedAt);
		this.deliveryFailedReason = model.getDeliveryFailedReason();
		this.complaintRegisteredAt = model.getComplaintRegisteredAt();
		this.complaintRegisteredAtDescription = describe(formatter, this.complaintRegisteredAt);
		this.recipientEmailAddress = model.getRecipientEmailAddress();
		this.customEmailText = model.getCustomEmailText();
		this.emailSubject = model.getEmailSubject();
		this.emailBody = model.getEmailBody();
		this.deleted = Boolean.TRUE.equals(model.getDeleted());
		this.editable = model.getCareEncounterStatusId() == CareEncounterStatusId.OPEN
				&& model.getScheduledMessageStatusId() == ScheduledMessageStatusId.PENDING && !this.deleted;
		this.cancelable = this.editable;
		this.deletedAt = model.getDeletedAt();
		this.deletedAtDescription = describe(formatter, this.deletedAt);
		this.deletedByAccountId = model.getDeletedByAccountId();
		this.deletedByAccountDisplayName = model.getDeletedByAccountDisplayName();
		this.createdByAccountId = model.getCreatedByAccountId();
		this.createdByAccountDisplayName = model.getCreatedByAccountDisplayName();
		this.lastUpdatedByAccountId = model.getLastUpdatedByAccountId();
		this.lastUpdatedByAccountDisplayName = model.getLastUpdatedByAccountDisplayName();
		this.created = model.getCreated();
		this.createdDescription = formatter.formatTimestampDescription(this.created);
		this.lastUpdated = model.getLastUpdated();
		this.lastUpdatedDescription = formatter.formatTimestampDescription(this.lastUpdated);
	}

	@Nullable private static String describe(@Nonnull Formatter formatter, @Nullable Instant instant) {
		return instant == null ? null : formatter.formatTimestampDescription(instant);
	}

	@Nonnull public UUID getCareEncounterScheduledMessageId() { return careEncounterScheduledMessageId; }
	@Nonnull public UUID getCareEncounterId() { return careEncounterId; }
	@Nonnull public CareEncounterScheduledMessageTypeId getCareEncounterScheduledMessageTypeId() { return careEncounterScheduledMessageTypeId; }
	@Nonnull public String getCareEncounterScheduledMessageTypeDescription() { return careEncounterScheduledMessageTypeDescription; }
	@Nonnull public UUID getScheduledMessageId() { return scheduledMessageId; }
	@Nonnull public ScheduledMessageStatusId getScheduledMessageStatusId() { return scheduledMessageStatusId; }
	@Nonnull public String getScheduledMessageStatusDescription() { return scheduledMessageStatusDescription; }
	@Nonnull public ScheduledMessageSourceId getScheduledMessageSourceId() { return scheduledMessageSourceId; }
	@Nullable public UUID getScheduledByAccountId() { return scheduledByAccountId; }
	@Nullable public String getScheduledByAccountDisplayName() { return scheduledByAccountDisplayName; }
	@Nonnull public UUID getMessageId() { return messageId; }
	@Nonnull public LocalDate getScheduledAtDate() { return scheduledAtDate; }
	@Nonnull public LocalTime getScheduledAtTime() { return scheduledAtTime; }
	@Nonnull public ZoneId getTimeZone() { return timeZone; }
	@Nonnull public Instant getScheduledAt() { return scheduledAt; }
	@Nonnull public String getScheduledAtDescription() { return scheduledAtDescription; }
	@Nullable public Instant getProcessedAt() { return processedAt; }
	@Nullable public String getProcessedAtDescription() { return processedAtDescription; }
	@Nullable public Instant getCanceledAt() { return canceledAt; }
	@Nullable public String getCanceledAtDescription() { return canceledAtDescription; }
	@Nullable public Instant getErroredAt() { return erroredAt; }
	@Nullable public String getErroredAtDescription() { return erroredAtDescription; }
	@Nullable public MessageStatusId getMessageStatusId() { return messageStatusId; }
	@Nullable public String getMessageStatusDescription() { return messageStatusDescription; }
	@Nullable public Instant getSentAt() { return sentAt; }
	@Nullable public String getSentAtDescription() { return sentAtDescription; }
	@Nullable public Instant getDeliveredAt() { return deliveredAt; }
	@Nullable public String getDeliveredAtDescription() { return deliveredAtDescription; }
	@Nullable public Instant getDeliveryFailedAt() { return deliveryFailedAt; }
	@Nullable public String getDeliveryFailedAtDescription() { return deliveryFailedAtDescription; }
	@Nullable public String getDeliveryFailedReason() { return deliveryFailedReason; }
	@Nullable public Instant getComplaintRegisteredAt() { return complaintRegisteredAt; }
	@Nullable public String getComplaintRegisteredAtDescription() { return complaintRegisteredAtDescription; }
	@Nonnull public String getRecipientEmailAddress() { return recipientEmailAddress; }
	@Nonnull public String getCustomEmailText() { return customEmailText; }
	@Nonnull public String getEmailSubject() { return emailSubject; }
	@Nonnull public String getEmailBody() { return emailBody; }
	@Nonnull public Boolean getEditable() { return editable; }
	@Nonnull public Boolean getCancelable() { return cancelable; }
	@Nonnull public Boolean getDeleted() { return deleted; }
	@Nullable public Instant getDeletedAt() { return deletedAt; }
	@Nullable public String getDeletedAtDescription() { return deletedAtDescription; }
	@Nullable public UUID getDeletedByAccountId() { return deletedByAccountId; }
	@Nullable public String getDeletedByAccountDisplayName() { return deletedByAccountDisplayName; }
	@Nonnull public UUID getCreatedByAccountId() { return createdByAccountId; }
	@Nullable public String getCreatedByAccountDisplayName() { return createdByAccountDisplayName; }
	@Nonnull public UUID getLastUpdatedByAccountId() { return lastUpdatedByAccountId; }
	@Nullable public String getLastUpdatedByAccountDisplayName() { return lastUpdatedByAccountDisplayName; }
	@Nonnull public Instant getCreated() { return created; }
	@Nonnull public String getCreatedDescription() { return createdDescription; }
	@Nonnull public Instant getLastUpdated() { return lastUpdated; }
	@Nonnull public String getLastUpdatedDescription() { return lastUpdatedDescription; }
}
