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
import com.cobaltplatform.api.model.db.MessageVendor.MessageVendorId;

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
	private InstitutionId institutionId;
	@Nullable
	private String vendorAssignedId;
	@Nullable
	private MessageTypeId messageTypeId;
	@Nullable
	private MessageStatusId messageStatusId;
	@Nullable
	private MessageVendorId messageVendorId;
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
	private Instant delivered;
	@Nullable
	private Instant deliveryFailed;
	@Nullable
	private String deliveryFailedReason;
	@Nullable
	private Instant complaintRegistered;

	@Nullable
	public UUID getMessageId() {
		return this.messageId;
	}

	public void setMessageId(@Nullable UUID messageId) {
		this.messageId = messageId;
	}

	@Nullable
	public InstitutionId getInstitutionId() {
		return this.institutionId;
	}

	public void setInstitutionId(@Nullable InstitutionId institutionId) {
		this.institutionId = institutionId;
	}

	@Nullable
	public String getVendorAssignedId() {
		return this.vendorAssignedId;
	}

	public void setVendorAssignedId(@Nullable String vendorAssignedId) {
		this.vendorAssignedId = vendorAssignedId;
	}

	@Nullable
	public MessageTypeId getMessageTypeId() {
		return this.messageTypeId;
	}

	public void setMessageTypeId(@Nullable MessageTypeId messageTypeId) {
		this.messageTypeId = messageTypeId;
	}

	@Nullable
	public MessageStatusId getMessageStatusId() {
		return this.messageStatusId;
	}

	public void setMessageStatusId(@Nullable MessageStatusId messageStatusId) {
		this.messageStatusId = messageStatusId;
	}

	@Nullable
	public MessageVendorId getMessageVendorId() {
		return this.messageVendorId;
	}

	public void setMessageVendorId(@Nullable MessageVendorId messageVendorId) {
		this.messageVendorId = messageVendorId;
	}

	@Nullable
	public String getSerializedMessage() {
		return this.serializedMessage;
	}

	public void setSerializedMessage(@Nullable String serializedMessage) {
		this.serializedMessage = serializedMessage;
	}

	@Nullable
	public String getStackTrace() {
		return this.stackTrace;
	}

	public void setStackTrace(@Nullable String stackTrace) {
		this.stackTrace = stackTrace;
	}

	@Nullable
	public Instant getCreated() {
		return this.created;
	}

	public void setCreated(@Nullable Instant created) {
		this.created = created;
	}

	@Nullable
	public Instant getEnqueued() {
		return this.enqueued;
	}

	public void setEnqueued(@Nullable Instant enqueued) {
		this.enqueued = enqueued;
	}

	@Nullable
	public Instant getProcessed() {
		return this.processed;
	}

	public void setProcessed(@Nullable Instant processed) {
		this.processed = processed;
	}

	@Nullable
	public Instant getDelivered() {
		return this.delivered;
	}

	public void setDelivered(@Nullable Instant delivered) {
		this.delivered = delivered;
	}

	@Nullable
	public Instant getDeliveryFailed() {
		return this.deliveryFailed;
	}

	public void setDeliveryFailed(@Nullable Instant deliveryFailed) {
		this.deliveryFailed = deliveryFailed;
	}

	@Nullable
	public String getDeliveryFailedReason() {
		return this.deliveryFailedReason;
	}

	public void setDeliveryFailedReason(@Nullable String deliveryFailedReason) {
		this.deliveryFailedReason = deliveryFailedReason;
	}

	@Nullable
	public Instant getComplaintRegistered() {
		return this.complaintRegistered;
	}

	public void setComplaintRegistered(@Nullable Instant complaintRegistered) {
		this.complaintRegistered = complaintRegistered;
	}
}