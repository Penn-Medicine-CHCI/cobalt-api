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

import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.messaging.MessageSerializer;
import com.cobaltplatform.api.util.JsonMapper;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify LLC.
 */
@ThreadSafe
public class PushMessageSerializer implements MessageSerializer<PushMessage> {
	@Nonnull
	private final JsonMapper jsonMapper;
	@Nonnull
	private final Configuration configuration;

	@Inject
	public PushMessageSerializer(@Nonnull JsonMapper jsonMapper,
															 @Nonnull Configuration configuration) {
		requireNonNull(jsonMapper);
		requireNonNull(configuration);

		this.jsonMapper = jsonMapper;
		this.configuration = configuration;
	}

	@Nonnull
	@Override
	public String serializeMessage(@Nonnull PushMessage pushMessage) {
		requireNonNull(pushMessage);

		SerializablePushMessage serializablePushMessage = new SerializablePushMessage();
		serializablePushMessage.setMessageId(pushMessage.getMessageId());
		serializablePushMessage.setInstitutionId(pushMessage.getInstitutionId());
		serializablePushMessage.setClientDevicePushTokenTypeId(pushMessage.getClientDevicePushTokenTypeId());
		serializablePushMessage.setPushToken(pushMessage.getPushToken());
		serializablePushMessage.setMessageTemplate(pushMessage.getMessageTemplate());
		serializablePushMessage.setMessageContext(pushMessage.getMessageContext());
		serializablePushMessage.setLocale(pushMessage.getLocale());
		serializablePushMessage.setMetadata(pushMessage.getMetadata());

		return getJsonMapper().toJson(serializablePushMessage);
	}

	@Nonnull
	@Override
	public PushMessage deserializeMessage(@Nonnull String serializedMessage) {
		requireNonNull(serializedMessage);

		SerializablePushMessage serializablePushMessage = getJsonMapper().fromJson(serializedMessage, SerializablePushMessage.class);

		return new PushMessage.Builder(serializablePushMessage.getMessageId(),
				serializablePushMessage.getInstitutionId(),
				serializablePushMessage.getMessageTemplate(),
				serializablePushMessage.getClientDevicePushTokenTypeId(),
				serializablePushMessage.getPushToken(),
				serializablePushMessage.getLocale())
				.messageContext(serializablePushMessage.getMessageContext())
				.metadata(serializablePushMessage.getMetadata())
				.build();
	}

	@Nonnull
	protected JsonMapper getJsonMapper() {
		return jsonMapper;
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return configuration;
	}
}
