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

package com.cobaltplatform.api.messaging.sms;

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
public class SmsMessageSerializer implements MessageSerializer<SmsMessage> {
	@Nonnull
	private final JsonMapper jsonMapper;
	@Nonnull
	private final Configuration configuration;

	@Inject
	public SmsMessageSerializer(@Nonnull JsonMapper jsonMapper,
															@Nonnull Configuration configuration) {
		requireNonNull(jsonMapper);
		requireNonNull(configuration);

		this.jsonMapper = jsonMapper;
		this.configuration = configuration;
	}

	@Nonnull
	@Override
	public String serializeMessage(@Nonnull SmsMessage smsMessage) {
		requireNonNull(smsMessage);

		SerializableSmsMessage serializableSmsMessage = new SerializableSmsMessage();
		serializableSmsMessage.setMessageId(smsMessage.getMessageId());
		serializableSmsMessage.setMessageTemplate(smsMessage.getMessageTemplate());
		serializableSmsMessage.setMessageContext(smsMessage.getMessageContext());
		serializableSmsMessage.setLocale(smsMessage.getLocale());
		serializableSmsMessage.setToNumber(smsMessage.getToNumber());

		return getJsonMapper().toJson(serializableSmsMessage);
	}

	@Nonnull
	@Override
	public SmsMessage deserializeMessage(@Nonnull String serializedMessage) {
		requireNonNull(serializedMessage);

		SerializableSmsMessage serializableSmsMessage = getJsonMapper().fromJson(serializedMessage, SerializableSmsMessage.class);

		return new SmsMessage.Builder(serializableSmsMessage.getMessageId(),
				serializableSmsMessage.getMessageTemplate(),
				serializableSmsMessage.getToNumber(),
				serializableSmsMessage.getLocale())
				.messageContext(serializableSmsMessage.getMessageContext())
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
