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

package com.cobaltplatform.api.messaging.email;

import com.cobaltplatform.api.model.db.MessageType.MessageTypeId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class SerializableEmailMessage {
	@Nonnull
	private final MessageTypeId messageTypeId;

	@Nullable
	private UUID messageId;
	@Nullable
	private Locale locale;
	@Nullable
	private EmailMessageTemplate messageTemplate;
	@Nullable
	private Map<String, Object> messageContext;
	@Nullable
	private String fromAddress;
	@Nullable
	private String replyToAddress;
	@Nullable
	private List<String> toAddresses;
	@Nullable
	private List<String> ccAddresses;
	@Nullable
	private List<String> bccAddresses;
	@Nullable
	private List<SerializableEmailAttachment> emailAttachments;

	public SerializableEmailMessage() {
		this.messageTypeId = MessageTypeId.EMAIL;
	}

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

	@Nullable
	public Locale getLocale() {
		return locale;
	}

	public void setLocale(@Nullable Locale locale) {
		this.locale = locale;
	}

	@Nullable
	public EmailMessageTemplate getMessageTemplate() {
		return messageTemplate;
	}

	public void setMessageTemplate(@Nullable EmailMessageTemplate messageTemplate) {
		this.messageTemplate = messageTemplate;
	}

	@Nullable
	public Map<String, Object> getMessageContext() {
		return messageContext;
	}

	public void setMessageContext(@Nullable Map<String, Object> messageContext) {
		this.messageContext = messageContext;
	}

	@Nullable
	public String getFromAddress() {
		return fromAddress;
	}

	public void setFromAddress(@Nullable String fromAddress) {
		this.fromAddress = fromAddress;
	}

	@Nullable
	public String getReplyToAddress() {
		return this.replyToAddress;
	}

	public void setReplyToAddress(@Nullable String replyToAddress) {
		this.replyToAddress = replyToAddress;
	}

	@Nullable
	public List<String> getToAddresses() {
		return toAddresses;
	}

	public void setToAddresses(@Nullable List<String> toAddresses) {
		this.toAddresses = toAddresses;
	}

	@Nullable
	public List<String> getCcAddresses() {
		return ccAddresses;
	}

	public void setCcAddresses(@Nullable List<String> ccAddresses) {
		this.ccAddresses = ccAddresses;
	}

	@Nullable
	public List<String> getBccAddresses() {
		return bccAddresses;
	}

	public void setBccAddresses(@Nullable List<String> bccAddresses) {
		this.bccAddresses = bccAddresses;
	}

	@Nullable
	public List<SerializableEmailAttachment> getEmailAttachments() {
		return emailAttachments;
	}

	public void setEmailAttachments(@Nullable List<SerializableEmailAttachment> emailAttachments) {
		this.emailAttachments = emailAttachments;
	}
}
