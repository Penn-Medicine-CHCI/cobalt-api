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
import com.cobaltplatform.api.model.db.InteractionType.InteractionTypeId;
import com.cobaltplatform.api.model.db.InteractionSendMethod.InteractionSendMethodId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;


/**
 * @author Transmogrify LLC.
 */
@NotThreadSafe
public class Interaction {
	@Nullable
	private UUID interactionId;
	@Nullable
	private InteractionTypeId interactionTypeId;
	@Nullable
	private InstitutionId institutionId;
	@Nullable
	private Integer maxInteractionCount;
	@Nullable
	private String interactionCompleteMessage;
	@Nullable
	private Integer frequencyInMinutes;
	@Nullable
	private String messageTemplate;
	@Nullable
	private String messageTemplateBody;
	@Nullable
	private Integer sendOffsetInMinutes;
	@Nullable
	private Integer sendDayOffset;
	@Nullable
	private LocalTime sendTimeOfDay;
	@Nullable
	private InteractionSendMethodId interactionSendMethodId;
	@Nullable
	private String emailSubject;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nullable
	public UUID getInteractionId() {
		return interactionId;
	}

	public void setInteractionId(@Nullable UUID interactionId) {
		this.interactionId = interactionId;
	}

	@Nullable
	public InteractionTypeId getInteractionTypeId() {
		return interactionTypeId;
	}

	public void setInteractionTypeId(@Nullable InteractionTypeId interactionTypeId) {
		this.interactionTypeId = interactionTypeId;
	}

	@Nullable
	public Integer getMaxInteractionCount() {
		return maxInteractionCount;
	}

	public void setMaxInteractionCount(@Nullable Integer maxInteractionCount) {
		this.maxInteractionCount = maxInteractionCount;
	}

	@Nullable
	public String getInteractionCompleteMessage() {
		return interactionCompleteMessage;
	}

	public void setInteractionCompleteMessage(@Nullable String interactionCompleteMessage) {
		this.interactionCompleteMessage = interactionCompleteMessage;
	}

	@Nullable
	public Integer getFrequencyInMinutes() {
		return frequencyInMinutes;
	}

	public void setFrequencyInMinutes(@Nullable Integer frequencyInMinutes) {
		this.frequencyInMinutes = frequencyInMinutes;
	}

	@Nullable
	public Instant getCreated() {
		return created;
	}

	public void setCreated(@Nullable Instant created) {
		this.created = created;
	}

	@Nullable
	public Instant getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(@Nullable Instant lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	@Nullable
	public InstitutionId getInstitutionId() {
		return institutionId;
	}

	public void setInstitutionId(@Nullable InstitutionId institutionId) {
		this.institutionId = institutionId;
	}

	@Nullable
	public String getMessageTemplate() {
		return messageTemplate;
	}

	public void setMessageTemplate(@Nullable String messageTemplate) {
		this.messageTemplate = messageTemplate;
	}

	@Nullable
	public Integer getSendOffsetInMinutes() {
		return sendOffsetInMinutes;
	}

	public void setSendOffsetInMinutes(@Nullable Integer sendOffsetInMinutes) {
		this.sendOffsetInMinutes = sendOffsetInMinutes;
	}

	@Nullable
	public String getMessageTemplateBody() {
		return messageTemplateBody;
	}

	public void setMessageTemplateBody(@Nullable String messageTemplateBody) {
		this.messageTemplateBody = messageTemplateBody;
	}

	@Nullable
	public Integer getSendDayOffset() {
		return sendDayOffset;
	}

	public void setSendDayOffset(@Nullable Integer sendDayOffset) {
		this.sendDayOffset = sendDayOffset;
	}

	@Nullable
	public LocalTime getSendTimeOfDay() {
		return sendTimeOfDay;
	}

	public void setSendTimeOfDay(@Nullable LocalTime sendTimeOfDay) {
		this.sendTimeOfDay = sendTimeOfDay;
	}

	@Nullable
	public InteractionSendMethodId getInteractionSendMethodId() {
		return interactionSendMethodId;
	}

	public void setInteractionSendMethodId(@Nullable InteractionSendMethodId interactionSendMethodId) {
		this.interactionSendMethodId = interactionSendMethodId;
	}

	@Nullable
	public String getEmailSubject() {
		return emailSubject;
	}

	public void setEmailSubject(@Nullable String emailSubject) {
		this.emailSubject = emailSubject;
	}
}