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

import com.cobaltplatform.api.model.db.ClientDeviceType.ClientDeviceTypeId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.PushTokenType.PushTokenTypeId;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * @author Transmogrify LLC.
 */
@NotThreadSafe
public class SerializablePushMessage {
	@Nonnull
	private UUID messageId;
	@Nonnull
	private InstitutionId institutionId;
	@Nonnull
	private ClientDeviceTypeId clientDeviceTypeId;
	@Nonnull
	private PushTokenTypeId pushTokenTypeId;
	@Nonnull
	private String pushToken;
	@Nonnull
	private PushMessageTemplate messageTemplate;
	@Nonnull
	private Locale locale;
	@Nonnull
	private Map<String, Object> messageContext;
	@Nonnull
	private Map<String, Object> metadata;

	@Nonnull
	public UUID getMessageId() {
		return this.messageId;
	}

	public void setMessageId(@Nonnull UUID messageId) {
		this.messageId = messageId;
	}

	@Nonnull
	public InstitutionId getInstitutionId() {
		return this.institutionId;
	}

	public void setInstitutionId(@Nonnull InstitutionId institutionId) {
		this.institutionId = institutionId;
	}

	@Nonnull
	public ClientDeviceTypeId getClientDeviceTypeId() {
		return this.clientDeviceTypeId;
	}

	public void setClientDeviceTypeId(@Nonnull ClientDeviceTypeId clientDeviceTypeId) {
		this.clientDeviceTypeId = clientDeviceTypeId;
	}

	@Nonnull
	public PushTokenTypeId getPushTokenTypeId() {
		return this.pushTokenTypeId;
	}

	public void setPushTokenTypeId(@Nonnull PushTokenTypeId pushTokenTypeId) {
		this.pushTokenTypeId = pushTokenTypeId;
	}

	@Nonnull
	public String getPushToken() {
		return this.pushToken;
	}

	public void setPushToken(@Nonnull String pushToken) {
		this.pushToken = pushToken;
	}

	@Nonnull
	public PushMessageTemplate getMessageTemplate() {
		return this.messageTemplate;
	}

	public void setMessageTemplate(@Nonnull PushMessageTemplate messageTemplate) {
		this.messageTemplate = messageTemplate;
	}

	@Nonnull
	public Locale getLocale() {
		return this.locale;
	}

	public void setLocale(@Nonnull Locale locale) {
		this.locale = locale;
	}

	@Nonnull
	public Map<String, Object> getMessageContext() {
		return this.messageContext;
	}

	public void setMessageContext(@Nonnull Map<String, Object> messageContext) {
		this.messageContext = messageContext;
	}

	@Nonnull
	public Map<String, Object> getMetadata() {
		return this.metadata;
	}

	public void setMetadata(@Nonnull Map<String, Object> metadata) {
		this.metadata = metadata;
	}
}
