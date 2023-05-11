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

import com.cobaltplatform.api.messaging.MessageSender;
import com.cobaltplatform.api.model.db.MessageType.MessageTypeId;
import com.cobaltplatform.api.model.db.MessageVendor.MessageVendorId;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.UUID;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author Transmogrify LLC.
 */
@ThreadSafe
public class ConsoleSmsMessageSender implements MessageSender<SmsMessage> {
	@Nonnull
	private final Logger logger;

	public ConsoleSmsMessageSender() {
		this.logger = getLogger(getClass());
	}

	@Override
	public String sendMessage(@Nonnull SmsMessage message) {
		logger.debug("Fake-sending SMS message {}", message);
		return UUID.randomUUID().toString();
	}

	@Nonnull
	@Override
	public MessageVendorId getMessageVendorId() {
		return MessageVendorId.UNSPECIFIED;
	}

	@Nonnull
	@Override
	public MessageTypeId getMessageTypeId() {
		return MessageTypeId.SMS;
	}
}
