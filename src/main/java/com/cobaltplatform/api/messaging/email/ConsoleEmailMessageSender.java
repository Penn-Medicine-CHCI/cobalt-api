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

import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.messaging.MessageSender;
import com.cobaltplatform.api.model.db.MessageType.MessageTypeId;
import com.cobaltplatform.api.model.db.MessageVendor.MessageVendorId;
import com.cobaltplatform.api.util.HandlebarsTemplater;
import com.cobaltplatform.api.util.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class ConsoleEmailMessageSender implements MessageSender<EmailMessage> {
	@Nonnull
	private final HandlebarsTemplater handlebarsTemplater;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final JsonMapper jsonMapper;
	@Nonnull
	private final Logger logger;

	public ConsoleEmailMessageSender(@Nonnull HandlebarsTemplater handlebarsTemplater,
																	 @Nonnull Configuration configuration) {
		requireNonNull(handlebarsTemplater);
		requireNonNull(configuration);

		this.handlebarsTemplater = handlebarsTemplater;
		this.configuration = configuration;
		this.jsonMapper = new JsonMapper();
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Override
	public String sendMessage(@Nonnull EmailMessage emailMessage) {
		requireNonNull(emailMessage);

		Map<String, Object> messageContext = emailMessage.getMessageContext();
		String fromAddress = emailMessage.getFromAddress().isPresent() ? emailMessage.getFromAddress().get() : getConfiguration().getEmailDefaultFromAddress();
		String replyToAddress = emailMessage.getReplyToAddress().orElse(null);
		String subject = getHandlebarsTemplater().mergeTemplate(emailMessage.getMessageTemplate().name(), "subject", emailMessage.getLocale(), messageContext).get();
		String body = getHandlebarsTemplater().mergeTemplate(emailMessage.getMessageTemplate().name(), "body", emailMessage.getLocale(), messageContext).get();

		List<String> logMessages = new ArrayList<>(8);
		logMessages.add(format("Fake-sending '%s' email...", emailMessage.getMessageTemplate()));
		logMessages.add(format("From: %s", fromAddress));

		if (replyToAddress != null)
			logMessages.add(format("Reply-To: %s", replyToAddress));

		if (emailMessage.getToAddresses().size() > 0)
			logMessages.add(format("To: %s", emailMessage.getToAddresses().stream().collect(Collectors.joining(", "))));

		if (emailMessage.getCcAddresses().size() > 0)
			logMessages.add(format("CC: %s", emailMessage.getCcAddresses().stream().collect(Collectors.joining(", "))));

		if (emailMessage.getBccAddresses().size() > 0)
			logMessages.add(format("BCC: %s", emailMessage.getBccAddresses().stream().collect(Collectors.joining(", "))));

		if (emailMessage.getMessageContext() != null && emailMessage.getMessageContext().size() > 0)
			logMessages.add(format("Context: %s", getJsonMapper().toJson(emailMessage.getMessageContext())));

		logMessages.add(format("Subject: %s", subject.trim()));
		logMessages.add(format("Body:\n%s", body.trim()));

		getLogger().info(logMessages.stream().collect(Collectors.joining("\n")));

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
		return MessageTypeId.EMAIL;
	}

	@Nonnull
	protected HandlebarsTemplater getHandlebarsTemplater() {
		return this.handlebarsTemplater;
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return this.configuration;
	}

	@Nonnull
	protected JsonMapper getJsonMapper() {
		return this.jsonMapper;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}