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
import com.cobaltplatform.api.messaging.MessageSender;
import com.cobaltplatform.api.util.HandlebarsTemplater;
import com.cobaltplatform.api.util.Normalizer;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
@Singleton
public class TwilioSmsMessageSender implements MessageSender<SmsMessage> {
	@Nonnull
	private final HandlebarsTemplater handlebarsTemplater;
	@Nonnull
	private final Normalizer normalizer;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Logger logger;

	public TwilioSmsMessageSender(@Nonnull HandlebarsTemplater handlebarsTemplater,
																@Nonnull Configuration configuration) {
		this(handlebarsTemplater, new Normalizer(), configuration);
	}

	public TwilioSmsMessageSender(@Nonnull HandlebarsTemplater handlebarsTemplater,
																@Nonnull Normalizer normalizer,
																@Nonnull Configuration configuration) {
		requireNonNull(handlebarsTemplater);
		requireNonNull(normalizer);
		requireNonNull(configuration);

		// Wish there were a way for this to NOT be global...
		Twilio.init(configuration.getTwilioSid(), configuration.getTwilioAuthToken());

		this.handlebarsTemplater = handlebarsTemplater;
		this.normalizer = normalizer;
		this.configuration = configuration;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Override
	public void sendMessage(@Nonnull SmsMessage smsMessage) {
		requireNonNull(smsMessage);

		Map<String, Object> messageContext = new HashMap<>(smsMessage.getMessageContext());
		String body = getHandlebarsTemplater().mergeTemplate(smsMessage.getMessageTemplate().name(), "body", smsMessage.getLocale(), messageContext).get();
		String normalizedToNumber = getNormalizer().normalizePhoneNumberToE164(smsMessage.getToNumber(), Locale.US).get();

		getLogger().debug("Sending SMS from {} to {} using Twilio. Message is '{}'...", getConfiguration().getTwilioFromNumber(), normalizedToNumber, smsMessage);

		long time = System.currentTimeMillis();

		try {
			Message message = Message.creator(
					getConfiguration().getTwilioAccountSid(),
					new PhoneNumber(normalizedToNumber),
					new PhoneNumber(getConfiguration().getTwilioFromNumber()),
					body
			).create();

			getLogger().info("Successfully sent SMS (SID {}) in {} ms.", message.getSid(), System.currentTimeMillis() - time);
		} catch (RuntimeException e) {
			getLogger().error(format("Unable to send SMS to %s", normalizedToNumber), e);
			throw e;
		}
	}

	@Nonnull
	protected HandlebarsTemplater getHandlebarsTemplater() {
		return handlebarsTemplater;
	}

	@Nonnull
	protected Normalizer getNormalizer() {
		return normalizer;
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return configuration;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}