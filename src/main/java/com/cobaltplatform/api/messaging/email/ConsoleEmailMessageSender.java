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

import com.cobaltplatform.api.messaging.MessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class ConsoleEmailMessageSender implements MessageSender<EmailMessage> {
	@Nonnull
	private final Logger logger;

	public ConsoleEmailMessageSender() {
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Override
	public void sendMessage(@Nonnull EmailMessage emailMessage) {
		requireNonNull(emailMessage);
		getLogger().debug("Fake-sending email message {}", emailMessage);
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}