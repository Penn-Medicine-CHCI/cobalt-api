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

package com.cobaltplatform.api.integration.twilio;

import com.cobaltplatform.api.messaging.sms.SmsMessage;
import com.cobaltplatform.api.messaging.sms.SmsMessageTemplate;
import com.cobaltplatform.api.messaging.sms.TwilioSmsMessageSender;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.google.gson.Gson;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;

import static com.soklet.util.LoggingUtils.initializeLogback;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class TwilioTests {
	@BeforeClass
	public static void initialize() {
		initializeLogback(Paths.get("config/local/logback.xml"));
	}

	@Test
	public void sendSmsMessage() {
		TwilioTestConfig twilioTestConfig = loadTwilioTestConfig();

		String twilioStatusCallbackUrl = trimToNull(twilioTestConfig.getTwilioStatusCallbackBaseUrl());

		if (twilioStatusCallbackUrl != null)
			twilioStatusCallbackUrl = format("%s/twilio/%s/message-status-callback", twilioStatusCallbackUrl, InstitutionId.COBALT_IC);

		TwilioSmsMessageSender twilioSmsMessageSender = new TwilioSmsMessageSender.Builder(twilioTestConfig.getTwilioAccountSid(), twilioTestConfig.getTwilioAuthToken())
				.twilioFromNumber(twilioTestConfig.getFromPhoneNumber())
				.twilioStatusCallbackUrl(twilioStatusCallbackUrl)
				.build();

		SmsMessage smsMessage = new SmsMessage.Builder(InstitutionId.COBALT, SmsMessageTemplate.IC_WELCOME, twilioTestConfig.getToPhoneNumber(), Locale.US)
				.messageContext(Map.of(
						"integratedCarePrimaryCareName", "Cobalt Integrated Care",
						"webappBaseUrl", "https://www.cobaltinnovations.org",
						"integratedCarePhoneNumberDescription", "(215) 555-1212"
				)).build();

		String messageId = twilioSmsMessageSender.sendMessage(smsMessage);

		System.out.println("Message ID: " + messageId);
	}

	@Nonnull
	protected TwilioTestConfig loadTwilioTestConfig() {
		try {
			String twilioTestConfigFile = Files.readString(Path.of("resources/test/twilio-config.json"), StandardCharsets.UTF_8);
			return new Gson().fromJson(twilioTestConfigFile, TwilioTestConfig.class);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@NotThreadSafe
	protected static class TwilioTestConfig {
		@Nullable
		private String twilioAccountSid;
		@Nullable
		private String twilioAuthToken;
		@Nullable
		private String twilioStatusCallbackBaseUrl;
		@Nullable
		private String toPhoneNumber;
		@Nullable
		private String fromPhoneNumber;

		@Nullable
		public String getTwilioAccountSid() {
			return this.twilioAccountSid;
		}

		public void setTwilioAccountSid(@Nullable String twilioAccountSid) {
			this.twilioAccountSid = twilioAccountSid;
		}

		@Nullable
		public String getTwilioAuthToken() {
			return this.twilioAuthToken;
		}

		public void setTwilioAuthToken(@Nullable String twilioAuthToken) {
			this.twilioAuthToken = twilioAuthToken;
		}

		@Nullable
		public String getTwilioStatusCallbackBaseUrl() {
			return this.twilioStatusCallbackBaseUrl;
		}

		public void setTwilioStatusCallbackBaseUrl(@Nullable String twilioStatusCallbackBaseUrl) {
			this.twilioStatusCallbackBaseUrl = twilioStatusCallbackBaseUrl;
		}

		@Nullable
		public String getToPhoneNumber() {
			return this.toPhoneNumber;
		}

		public void setToPhoneNumber(@Nullable String toPhoneNumber) {
			this.toPhoneNumber = toPhoneNumber;
		}

		@Nullable
		public String getFromPhoneNumber() {
			return this.fromPhoneNumber;
		}

		public void setFromPhoneNumber(@Nullable String fromPhoneNumber) {
			this.fromPhoneNumber = fromPhoneNumber;
		}
	}
}
