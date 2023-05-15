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

package com.cobaltplatform.api.messaging.call;

import com.cobaltplatform.api.messaging.Message;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.MessageType.MessageTypeId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify LLC.
 */
@Immutable
public class CallMessage implements Message {
	@Nonnull
	private final UUID messageId;
	@Nonnull
	private final InstitutionId institutionId;
	@Nonnull
	private final String toNumber;
	@Nonnull
	private final CallMessageTemplate messageTemplate;
	@Nonnull
	private final Locale locale;
	@Nonnull
	private final Map<String, Object> messageContext;

	private CallMessage(@Nonnull Builder builder) {
		requireNonNull(builder);

		this.messageId = builder.messageId;
		this.institutionId = builder.institutionId;
		this.toNumber = builder.toNumber;
		this.locale = builder.locale;
		this.messageTemplate = builder.messageTemplate;
		this.messageContext = builder.messageContext == null ? emptyMap() : Map.copyOf(builder.messageContext);
	}

	@Override
	public String toString() {
		return format("%s{messageId=%s, messageTemplate=%s, messageContext=%s, locale=%s, toNumber=%s}",
				getClass().getSimpleName(), getMessageId(), getMessageTemplate(), getMessageContext(), getLocale(), getToNumber());
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other)
			return true;

		if (other == null || !getClass().equals(other.getClass()))
			return false;

		CallMessage that = (CallMessage) other;

		return getMessageId().equals(that.getMessageId()) &&
				getInstitutionId().equals(that.getInstitutionId()) &&
				getToNumber().equals(that.getToNumber()) &&
				getMessageTemplate().equals(that.getMessageTemplate()) &&
				getLocale().equals(that.getLocale()) &&
				getMessageContext().equals(that.getMessageContext());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getMessageId(), getInstitutionId(), getToNumber(), getMessageTemplate(), getLocale(), getMessageContext());
	}

	@Nonnull
	public String getToNumber() {
		return toNumber;
	}

	@Nonnull
	public CallMessageTemplate getMessageTemplate() {
		return messageTemplate;
	}

	@Nonnull
	public Map<String, Object> getMessageContext() {
		return messageContext;
	}

	@Nonnull
	public Locale getLocale() {
		return locale;
	}

	@Nonnull
	@Override
	public UUID getMessageId() {
		return messageId;
	}

	@Override
	@Nonnull
	public InstitutionId getInstitutionId() {
		return this.institutionId;
	}

	@Nonnull
	@Override
	public MessageTypeId getMessageTypeId() {
		return MessageTypeId.CALL;
	}

	public static class Builder {
		@Nonnull
		private final UUID messageId;
		@Nonnull
		private final InstitutionId institutionId;
		@Nonnull
		private final String toNumber;
		@Nonnull
		private final CallMessageTemplate messageTemplate;
		@Nonnull
		private final Locale locale;
		@Nullable
		private Map<String, Object> messageContext;

		public Builder(@Nonnull InstitutionId institutionId,
									 @Nonnull CallMessageTemplate messageTemplate,
									 @Nonnull String toNumber,
									 @Nonnull Locale locale) {
			this(UUID.randomUUID(), institutionId, messageTemplate, toNumber, locale);
		}

		public Builder(@Nonnull UUID messageId,
									 @Nonnull InstitutionId institutionId,
									 @Nonnull CallMessageTemplate messageTemplate,
									 @Nonnull String toNumber,
									 @Nonnull Locale locale) {
			requireNonNull(messageId);
			requireNonNull(institutionId);
			requireNonNull(messageTemplate);
			requireNonNull(toNumber);
			requireNonNull(locale);

			this.messageId = messageId;
			this.institutionId = institutionId;
			this.messageTemplate = messageTemplate;
			this.toNumber = toNumber;
			this.locale = locale;
		}

		@Nonnull
		public Builder messageContext(@Nullable Map<String, Object> messageContext) {
			this.messageContext = messageContext;
			return this;
		}

		@Nonnull
		public CallMessage build() {
			return new CallMessage(this);
		}
	}
}
