/*
 * Copyright 2021 The University of Pennsylvania and Penn Medicine
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.cobaltplatform.api.model.db;

import com.cobaltplatform.api.model.db.CareEncounterScheduledMessageType.CareEncounterScheduledMessageTypeId;
import com.cobaltplatform.api.model.db.CareEncounterStatus.CareEncounterStatusId;
import com.cobaltplatform.api.model.db.MessageStatus.MessageStatusId;
import com.cobaltplatform.api.model.db.ScheduledMessageSource.ScheduledMessageSourceId;
import com.cobaltplatform.api.model.db.ScheduledMessageStatus.ScheduledMessageStatusId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@NotThreadSafe
public class CareEncounterScheduledMessage {
	@Nullable private UUID careEncounterScheduledMessageId;
	@Nullable private UUID careEncounterId;
	@Nullable private CareEncounterStatusId careEncounterStatusId;
	@Nullable private CareEncounterScheduledMessageTypeId careEncounterScheduledMessageTypeId;
	@Nullable private String careEncounterScheduledMessageTypeDescription;
	@Nullable private UUID scheduledMessageId;
	@Nullable private ScheduledMessageStatusId scheduledMessageStatusId;
	@Nullable private String scheduledMessageStatusDescription;
	@Nullable private ScheduledMessageSourceId scheduledMessageSourceId;
	@Nullable private UUID scheduledByAccountId;
	@Nullable private String scheduledByAccountDisplayName;
	@Nullable private UUID messageId;
	@Nullable private LocalDateTime scheduledAt;
	@Nullable private ZoneId timeZone;
	@Nullable private Instant processedAt;
	@Nullable private Instant canceledAt;
	@Nullable private Instant erroredAt;
	@Nullable private MessageStatusId messageStatusId;
	@Nullable private String messageStatusDescription;
	@Nullable private Instant sentAt;
	@Nullable private Instant deliveredAt;
	@Nullable private Instant deliveryFailedAt;
	@Nullable private String deliveryFailedReason;
	@Nullable private Instant complaintRegisteredAt;
	@Nullable private Long resourceLinkCount;
	@Nullable private Long resourceLinkOpenedCount;
	@Nullable private Instant resourceLinkLastClickedAt;
	@Nullable private String recipientEmailAddress;
	@Nullable private String customEmailText;
	@Nullable private String emailSubject;
	@Nullable private String emailBody;
	@Nullable private Boolean deleted;
	@Nullable private Instant deletedAt;
	@Nullable private UUID deletedByAccountId;
	@Nullable private String deletedByAccountDisplayName;
	@Nullable private UUID createdByAccountId;
	@Nullable private String createdByAccountDisplayName;
	@Nullable private UUID lastUpdatedByAccountId;
	@Nullable private String lastUpdatedByAccountDisplayName;
	@Nullable private Instant created;
	@Nullable private Instant lastUpdated;

	@Nullable public UUID getCareEncounterScheduledMessageId() { return careEncounterScheduledMessageId; }
	public void setCareEncounterScheduledMessageId(@Nullable UUID v) { careEncounterScheduledMessageId = v; }
	@Nullable public UUID getCareEncounterId() { return careEncounterId; }
	public void setCareEncounterId(@Nullable UUID v) { careEncounterId = v; }
	@Nullable public CareEncounterStatusId getCareEncounterStatusId() { return careEncounterStatusId; }
	public void setCareEncounterStatusId(@Nullable CareEncounterStatusId v) { careEncounterStatusId = v; }
	@Nullable public CareEncounterScheduledMessageTypeId getCareEncounterScheduledMessageTypeId() { return careEncounterScheduledMessageTypeId; }
	public void setCareEncounterScheduledMessageTypeId(@Nullable CareEncounterScheduledMessageTypeId v) { careEncounterScheduledMessageTypeId = v; }
	@Nullable public String getCareEncounterScheduledMessageTypeDescription() { return careEncounterScheduledMessageTypeDescription; }
	public void setCareEncounterScheduledMessageTypeDescription(@Nullable String v) { careEncounterScheduledMessageTypeDescription = v; }
	@Nullable public UUID getScheduledMessageId() { return scheduledMessageId; }
	public void setScheduledMessageId(@Nullable UUID v) { scheduledMessageId = v; }
	@Nullable public ScheduledMessageStatusId getScheduledMessageStatusId() { return scheduledMessageStatusId; }
	public void setScheduledMessageStatusId(@Nullable ScheduledMessageStatusId v) { scheduledMessageStatusId = v; }
	@Nullable public String getScheduledMessageStatusDescription() { return scheduledMessageStatusDescription; }
	public void setScheduledMessageStatusDescription(@Nullable String v) { scheduledMessageStatusDescription = v; }
	@Nullable public ScheduledMessageSourceId getScheduledMessageSourceId() { return scheduledMessageSourceId; }
	public void setScheduledMessageSourceId(@Nullable ScheduledMessageSourceId v) { scheduledMessageSourceId = v; }
	@Nullable public UUID getScheduledByAccountId() { return scheduledByAccountId; }
	public void setScheduledByAccountId(@Nullable UUID v) { scheduledByAccountId = v; }
	@Nullable public String getScheduledByAccountDisplayName() { return scheduledByAccountDisplayName; }
	public void setScheduledByAccountDisplayName(@Nullable String v) { scheduledByAccountDisplayName = v; }
	@Nullable public UUID getMessageId() { return messageId; }
	public void setMessageId(@Nullable UUID v) { messageId = v; }
	@Nullable public LocalDateTime getScheduledAt() { return scheduledAt; }
	public void setScheduledAt(@Nullable LocalDateTime v) { scheduledAt = v; }
	@Nullable public ZoneId getTimeZone() { return timeZone; }
	public void setTimeZone(@Nullable ZoneId v) { timeZone = v; }
	@Nullable public Instant getProcessedAt() { return processedAt; }
	public void setProcessedAt(@Nullable Instant v) { processedAt = v; }
	@Nullable public Instant getCanceledAt() { return canceledAt; }
	public void setCanceledAt(@Nullable Instant v) { canceledAt = v; }
	@Nullable public Instant getErroredAt() { return erroredAt; }
	public void setErroredAt(@Nullable Instant v) { erroredAt = v; }
	@Nullable public MessageStatusId getMessageStatusId() { return messageStatusId; }
	public void setMessageStatusId(@Nullable MessageStatusId v) { messageStatusId = v; }
	@Nullable public String getMessageStatusDescription() { return messageStatusDescription; }
	public void setMessageStatusDescription(@Nullable String v) { messageStatusDescription = v; }
	@Nullable public Instant getSentAt() { return sentAt; }
	public void setSentAt(@Nullable Instant v) { sentAt = v; }
	@Nullable public Instant getDeliveredAt() { return deliveredAt; }
	public void setDeliveredAt(@Nullable Instant v) { deliveredAt = v; }
	@Nullable public Instant getDeliveryFailedAt() { return deliveryFailedAt; }
	public void setDeliveryFailedAt(@Nullable Instant v) { deliveryFailedAt = v; }
	@Nullable public String getDeliveryFailedReason() { return deliveryFailedReason; }
	public void setDeliveryFailedReason(@Nullable String v) { deliveryFailedReason = v; }
	@Nullable public Instant getComplaintRegisteredAt() { return complaintRegisteredAt; }
	public void setComplaintRegisteredAt(@Nullable Instant v) { complaintRegisteredAt = v; }
	@Nullable public Long getResourceLinkCount() { return resourceLinkCount; }
	public void setResourceLinkCount(@Nullable Long v) { resourceLinkCount = v; }
	@Nullable public Long getResourceLinkOpenedCount() { return resourceLinkOpenedCount; }
	public void setResourceLinkOpenedCount(@Nullable Long v) { resourceLinkOpenedCount = v; }
	@Nullable public Instant getResourceLinkLastClickedAt() { return resourceLinkLastClickedAt; }
	public void setResourceLinkLastClickedAt(@Nullable Instant v) { resourceLinkLastClickedAt = v; }
	@Nullable public String getRecipientEmailAddress() { return recipientEmailAddress; }
	public void setRecipientEmailAddress(@Nullable String v) { recipientEmailAddress = v; }
	@Nullable public String getCustomEmailText() { return customEmailText; }
	public void setCustomEmailText(@Nullable String v) { customEmailText = v; }
	@Nullable public String getEmailSubject() { return emailSubject; }
	public void setEmailSubject(@Nullable String v) { emailSubject = v; }
	@Nullable public String getEmailBody() { return emailBody; }
	public void setEmailBody(@Nullable String v) { emailBody = v; }
	@Nullable public Boolean getDeleted() { return deleted; }
	public void setDeleted(@Nullable Boolean v) { deleted = v; }
	@Nullable public Instant getDeletedAt() { return deletedAt; }
	public void setDeletedAt(@Nullable Instant v) { deletedAt = v; }
	@Nullable public UUID getDeletedByAccountId() { return deletedByAccountId; }
	public void setDeletedByAccountId(@Nullable UUID v) { deletedByAccountId = v; }
	@Nullable public String getDeletedByAccountDisplayName() { return deletedByAccountDisplayName; }
	public void setDeletedByAccountDisplayName(@Nullable String v) { deletedByAccountDisplayName = v; }
	@Nullable public UUID getCreatedByAccountId() { return createdByAccountId; }
	public void setCreatedByAccountId(@Nullable UUID v) { createdByAccountId = v; }
	@Nullable public String getCreatedByAccountDisplayName() { return createdByAccountDisplayName; }
	public void setCreatedByAccountDisplayName(@Nullable String v) { createdByAccountDisplayName = v; }
	@Nullable public UUID getLastUpdatedByAccountId() { return lastUpdatedByAccountId; }
	public void setLastUpdatedByAccountId(@Nullable UUID v) { lastUpdatedByAccountId = v; }
	@Nullable public String getLastUpdatedByAccountDisplayName() { return lastUpdatedByAccountDisplayName; }
	public void setLastUpdatedByAccountDisplayName(@Nullable String v) { lastUpdatedByAccountDisplayName = v; }
	@Nullable public Instant getCreated() { return created; }
	public void setCreated(@Nullable Instant v) { created = v; }
	@Nullable public Instant getLastUpdated() { return lastUpdated; }
	public void setLastUpdated(@Nullable Instant v) { lastUpdated = v; }
}
