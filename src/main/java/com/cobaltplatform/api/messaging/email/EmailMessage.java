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

import com.cobaltplatform.api.messaging.Message;
import com.cobaltplatform.api.model.db.MessageType.MessageTypeId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class EmailMessage implements Message {
	@Nonnull
	private final UUID messageId;
	@Nonnull
	private final EmailMessageTemplate messageTemplate;
	@Nonnull
	private final Map<String, Object> messageContext;
	@Nonnull
	private final Locale locale;
	@Nullable
	private final String fromAddress;
	@Nullable
	private final String replyToAddress;
	@Nonnull
	private final List<String> toAddresses;
	@Nonnull
	private final List<String> ccAddresses;
	@Nonnull
	private final List<String> bccAddresses;
	@Nonnull
	private final List<EmailAttachment> emailAttachments;

	private EmailMessage(@Nonnull Builder builder) {
		requireNonNull(builder);

		this.messageId = builder.messageId;
		this.messageTemplate = builder.messageTemplate;
		this.messageContext = builder.messageContext == null ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(builder.messageContext));
		this.locale = builder.locale;
		this.fromAddress = builder.fromAddress;
		this.replyToAddress = builder.replyToAddress;
		this.toAddresses = builder.toAddresses == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(builder.toAddresses));
		this.ccAddresses = builder.ccAddresses == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(builder.ccAddresses));
		this.bccAddresses = builder.bccAddresses == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(builder.bccAddresses));
		this.emailAttachments = builder.emailAttachments == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(builder.emailAttachments));
	}

	/**
	 * Turns this immutable email back into a mutable builder so we can effectively clone it.
	 *
	 * @return a mutable builder prefilled with this email's data.
	 */
	@Nonnull
	public Builder toBuilder() {
		return new Builder(getMessageId(), getMessageTemplate(), getLocale())
				.messageContext(getMessageContext())
				.fromAddress(getFromAddress().orElse(null))
				.replyToAddress(getReplyToAddress().orElse(null))
				.toAddresses(getToAddresses())
				.ccAddresses(getCcAddresses())
				.bccAddresses(getBccAddresses())
				.emailAttachments(getEmailAttachments());
	}

	@Override
	@Nonnull
	public String toString() {
		return format("%s{messageId=%s, messageTemplate=%s, messageContext=%s, locale=%s, fromAddress=%s, replyToAddress=%s, toAddresses=%s, ccAddresses=%s, bccAddresses=%s, emailAttachments=%s}",
				getClass().getSimpleName(), getMessageId(), getMessageTemplate(), getMessageContext(), getLocale(), getFromAddress().orElse(null),
				getReplyToAddress().orElse(null), getToAddresses(), getCcAddresses(), getBccAddresses(), getEmailAttachments());
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other)
			return true;

		if (other == null || !getClass().equals(other.getClass()))
			return false;

		EmailMessage emailMessage = (EmailMessage) other;

		return Objects.equals(getMessageId(), emailMessage.getMessageId())
				&& Objects.equals(getMessageTypeId(), emailMessage.getMessageTypeId())
				&& Objects.equals(getMessageTemplate(), emailMessage.getMessageTemplate())
				&& Objects.equals(getMessageContext(), emailMessage.getMessageContext())
				&& Objects.equals(getLocale(), emailMessage.getLocale())
				&& Objects.equals(getFromAddress(), emailMessage.getFromAddress())
				&& Objects.equals(getReplyToAddress(), emailMessage.getReplyToAddress())
				&& Objects.equals(getToAddresses(), emailMessage.getToAddresses())
				&& Objects.equals(getCcAddresses(), emailMessage.getCcAddresses())
				&& Objects.equals(getBccAddresses(), emailMessage.getBccAddresses())
				&& Objects.equals(getEmailAttachments(), emailMessage.getEmailAttachments());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getMessageId(), getMessageTypeId(), getMessageTemplate(), getMessageContext(),
				getLocale(), getFromAddress(), getReplyToAddress(), getToAddresses(), getCcAddresses(), getBccAddresses(), getEmailAttachments());
	}

	public static class Builder {
		@Nonnull
		private final UUID messageId;
		@Nonnull
		private final EmailMessageTemplate messageTemplate;
		@Nonnull
		private final Locale locale;
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
		private List<EmailAttachment> emailAttachments;

		public Builder(@Nonnull EmailMessageTemplate messageTemplate,
									 @Nonnull Locale locale) {
			this(UUID.randomUUID(), messageTemplate, locale);
		}

		public Builder(@Nonnull UUID messageId,
									 @Nonnull EmailMessageTemplate messageTemplate,
									 @Nonnull Locale locale) {
			requireNonNull(messageId);
			requireNonNull(messageTemplate);
			requireNonNull(locale);

			this.messageId = messageId;
			this.messageTemplate = messageTemplate;
			this.locale = locale;
		}

		@Nonnull
		public Builder messageContext(@Nullable Map<String, Object> messageContext) {
			this.messageContext = messageContext;
			return this;
		}

		@Nonnull
		public Builder fromAddress(@Nullable String fromAddress) {
			this.fromAddress = fromAddress;
			return this;
		}

		@Nonnull
		public Builder replyToAddress(@Nullable String replyToAddress) {
			this.replyToAddress = replyToAddress;
			return this;
		}

		@Nonnull
		public Builder toAddresses(@Nullable List<String> toAddresses) {
			this.toAddresses = toAddresses;
			return this;
		}

		@Nonnull
		public Builder ccAddresses(@Nullable List<String> ccAddresses) {
			this.ccAddresses = ccAddresses;
			return this;
		}

		@Nonnull
		public Builder bccAddresses(@Nullable List<String> bccAddresses) {
			this.bccAddresses = bccAddresses;
			return this;
		}

		@Nonnull
		public Builder emailAttachments(@Nullable List<EmailAttachment> emailAttachments) {
			this.emailAttachments = emailAttachments;
			return this;
		}

		@Nonnull
		public EmailMessage build() {
			return new EmailMessage(this);
		}
	}

	@Nonnull
	@Override
	public UUID getMessageId() {
		return this.messageId;
	}

	@Nonnull
	@Override
	public MessageTypeId getMessageTypeId() {
		return MessageTypeId.EMAIL;
	}

	@Nonnull
	public EmailMessageTemplate getMessageTemplate() {
		return this.messageTemplate;
	}

	@Nonnull
	public Map<String, Object> getMessageContext() {
		return this.messageContext;
	}

	@Nonnull
	public Locale getLocale() {
		return this.locale;
	}

	@Nonnull
	public Optional<String> getFromAddress() {
		return Optional.ofNullable(this.fromAddress);
	}

	@Nonnull
	public Optional<String> getReplyToAddress() {
		return Optional.ofNullable(this.replyToAddress);
	}

	@Nonnull
	public List<String> getToAddresses() {
		return this.toAddresses;
	}

	@Nonnull
	public List<String> getCcAddresses() {
		return this.ccAddresses;
	}

	@Nonnull
	public List<String> getBccAddresses() {
		return bccAddresses;
	}

	@Nonnull
	public List<EmailAttachment> getEmailAttachments() {
		return emailAttachments;
	}
}