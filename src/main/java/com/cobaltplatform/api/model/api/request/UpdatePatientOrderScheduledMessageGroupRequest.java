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

import com.cobaltplatform.api.model.db.MessageType.MessageTypeId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class UpdatePatientOrderScheduledMessageGroupRequest {
	@Nullable
	private UUID patientOrderScheduledMessageGroupRequestId;
	@Nullable
	private UUID accountId;
	@Nullable
	private Set<MessageTypeId> messageTypeIds;
	@Nullable
	private LocalDate scheduledAtDate;
	@Nullable
	private String scheduledAtTime; // Manually parse string from the UI

	@Nullable
	public UUID getPatientOrderScheduledMessageGroupRequestId() {
		return this.patientOrderScheduledMessageGroupRequestId;
	}

	public void setPatientOrderScheduledMessageGroupRequestId(@Nullable UUID patientOrderScheduledMessageGroupRequestId) {
		this.patientOrderScheduledMessageGroupRequestId = patientOrderScheduledMessageGroupRequestId;
	}

	@Nullable
	public UUID getAccountId() {
		return this.accountId;
	}

	public void setAccountId(@Nullable UUID accountId) {
		this.accountId = accountId;
	}

	@Nullable
	public Set<MessageTypeId> getMessageTypeIds() {
		return this.messageTypeIds;
	}

	public void setMessageTypeIds(@Nullable Set<MessageTypeId> messageTypeIds) {
		this.messageTypeIds = messageTypeIds;
	}

	@Nullable
	public LocalDate getScheduledAtDate() {
		return this.scheduledAtDate;
	}

	public void setScheduledAtDate(@Nullable LocalDate scheduledAtDate) {
		this.scheduledAtDate = scheduledAtDate;
	}

	@Nullable
	public String getScheduledAtTime() {
		return this.scheduledAtTime;
	}

	public void setScheduledAtTime(@Nullable String scheduledAtTime) {
		this.scheduledAtTime = scheduledAtTime;
	}
}
