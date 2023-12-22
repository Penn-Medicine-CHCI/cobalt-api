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

package com.cobaltplatform.api.messaging.push;

import com.cobaltplatform.api.messaging.Message;
import com.cobaltplatform.api.model.db.ClientDeviceType.ClientDeviceTypeId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.MessageType.MessageTypeId;
import com.cobaltplatform.api.model.db.PushTokenType.PushTokenTypeId;

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
public class PushMessage implements Message {
	@Nonnull
	private final UUID messageId;
	@Nonnull
	private final InstitutionId institutionId;
	@Nonnull
	private final ClientDeviceTypeId clientDeviceTypeId;
	@Nonnull
	private final PushTokenTypeId pushTokenTypeId;
	@Nonnull
	private final String pushToken;
	@Nonnull
	private final PushMessageTemplate messageTemplate;
	@Nonnull
	private final Locale locale;
	@Nonnull
	private final Map<String, Object> messageContext;
	@Nonnull
	private final Map<String, String> metadata;

	private PushMessage(@Nonnull Builder builder) {
		requireNonNull(builder);

		this.messageId = builder.messageId;
		this.institutionId = builder.institutionId;
		this.clientDeviceTypeId = builder.clientDeviceTypeId;
		this.pushTokenTypeId = builder.pushTokenTypeId;
		this.pushToken = builder.pushToken;
		this.locale = builder.locale;
		this.messageTemplate = builder.messageTemplate;
		this.messageContext = builder.messageContext == null ? emptyMap() : Map.copyOf(builder.messageContext);
		this.metadata = builder.metadata == null ? emptyMap() : Map.copyOf(builder.metadata);
	}

	@Override
	public String toString() {
		return format("%s{messageId=%s, institutionId=%s, messageTemplate=%s, messageContext=%s, metadata=%s, locale=%s, clientDeviceTypeId, pushTokenTypeId=%s, pushToken=%s}",
				getClass().getSimpleName(), getMessageId(), getInstitutionId(), getMessageTemplate(), getMessageContext(),
				getMetadata(), getLocale(), getClientDeviceTypeId(), getPushTokenTypeId(), getPushToken());
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other)
			return true;

		if (other == null || !getClass().equals(other.getClass()))
			return false;

		PushMessage that = (PushMessage) other;

		return getMessageId().equals(that.getMessageId()) &&
				getInstitutionId().equals(that.getInstitutionId()) &&
				getClientDeviceTypeId().equals(that.getClientDeviceTypeId()) &&
				getPushTokenTypeId().equals(that.getPushTokenTypeId()) &&
				getPushToken().equals(that.getPushToken()) &&
				getMessageTemplate().equals(that.getMessageTemplate()) &&
				getLocale().equals(that.getLocale()) &&
				getMessageContext().equals(that.getMessageContext()) &&
				getMetadata().equals(that.getMetadata());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getMessageId(), getInstitutionId(), getClientDeviceTypeId(), getPushTokenTypeId(), getPushToken(),
				getMessageTemplate(), getLocale(), getMessageContext(), getMetadata());
	}

	@Override
	@Nonnull
	public InstitutionId getInstitutionId() {
		return this.institutionId;
	}

	@Nonnull
	@Override
	public UUID getMessageId() {
		return messageId;
	}

	@Nonnull
	@Override
	public MessageTypeId getMessageTypeId() {
		return MessageTypeId.PUSH;
	}

	@Nonnull
	public ClientDeviceTypeId getClientDeviceTypeId() {
		return this.clientDeviceTypeId;
	}

	@Nonnull
	public PushTokenTypeId getPushTokenTypeId() {
		return this.pushTokenTypeId;
	}

	@Nonnull
	public String getPushToken() {
		return this.pushToken;
	}

	@Nonnull
	public PushMessageTemplate getMessageTemplate() {
		return this.messageTemplate;
	}

	@Nonnull
	public Locale getLocale() {
		return this.locale;
	}

	@Nonnull
	public Map<String, Object> getMessageContext() {
		return this.messageContext;
	}

	@Nonnull
	public Map<String, String> getMetadata() {
		return this.metadata;
	}

	public static class Builder {
		@Nonnull
		private final UUID messageId;
		@Nonnull
		private final InstitutionId institutionId;
		@Nonnull
		private final ClientDeviceTypeId clientDeviceTypeId;
		@Nonnull
		private final PushTokenTypeId pushTokenTypeId;
		@Nonnull
		private final String pushToken;
		@Nonnull
		private final PushMessageTemplate messageTemplate;
		@Nonnull
		private final Locale locale;
		@Nullable
		private Map<String, Object> messageContext;
		@Nullable
		private Map<String, String> metadata;

		public Builder(@Nonnull InstitutionId institutionId,
									 @Nonnull PushMessageTemplate messageTemplate,
									 @Nonnull ClientDeviceTypeId clientDeviceTypeId,
									 @Nonnull PushTokenTypeId pushTokenTypeId,
									 @Nonnull String pushToken,
									 @Nonnull Locale locale) {
			this(UUID.randomUUID(), institutionId, messageTemplate, clientDeviceTypeId, pushTokenTypeId, pushToken, locale);
		}

		public Builder(@Nonnull UUID messageId,
									 @Nonnull InstitutionId institutionId,
									 @Nonnull PushMessageTemplate messageTemplate,
									 @Nonnull ClientDeviceTypeId clientDeviceTypeId,
									 @Nonnull PushTokenTypeId pushTokenTypeId,
									 @Nonnull String pushToken,
									 @Nonnull Locale locale) {
			requireNonNull(messageId);
			requireNonNull(institutionId);
			requireNonNull(messageTemplate);
			requireNonNull(clientDeviceTypeId);
			requireNonNull(pushTokenTypeId);
			requireNonNull(pushToken);
			requireNonNull(locale);

			this.messageId = messageId;
			this.institutionId = institutionId;
			this.messageTemplate = messageTemplate;
			this.clientDeviceTypeId = clientDeviceTypeId;
			this.pushTokenTypeId = pushTokenTypeId;
			this.pushToken = pushToken;
			this.locale = locale;
		}

		@Nonnull
		public Builder messageContext(@Nullable Map<String, Object> messageContext) {
			this.messageContext = messageContext;
			return this;
		}

		@Nonnull
		public Builder metadata(@Nullable Map<String, String> metadata) {
			this.metadata = metadata;
			return this;
		}

		@Nonnull
		public PushMessage build() {
			return new PushMessage(this);
		}
	}
}
