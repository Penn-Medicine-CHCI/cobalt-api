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

import com.cobaltplatform.api.http.DefaultHttpClient;
import com.cobaltplatform.api.http.HttpClient;
import com.cobaltplatform.api.http.HttpMethod;
import com.cobaltplatform.api.http.HttpRequest;
import com.cobaltplatform.api.http.HttpResponse;
import com.cobaltplatform.api.messaging.MessageSender;
import com.cobaltplatform.api.model.db.MessageType.MessageTypeId;
import com.cobaltplatform.api.model.db.MessageVendor.MessageVendorId;
import com.cobaltplatform.api.util.HandlebarsTemplater;
import com.cobaltplatform.api.util.Normalizer;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
@Singleton
public class TwilioSmsMessageSender implements MessageSender<SmsMessage> {
	@Nonnull
	private final String twilioAccountSid;
	@Nonnull
	private final String twilioAuthToken;
	@Nonnull
	private final String twilioFromNumber;
	@Nullable
	private final String twilioStatusCallbackUrl;
	@Nonnull
	private final HttpClient httpClient;
	@Nonnull
	private final HandlebarsTemplater handlebarsTemplater;
	@Nonnull
	private final Normalizer normalizer;
	@Nonnull
	private final Gson gson;
	@Nonnull
	private final Logger logger;


	private TwilioSmsMessageSender(@Nonnull Builder builder) {
		requireNonNull(builder);

		this.twilioAccountSid = requireNonNull(builder.twilioAccountSid);
		this.twilioAuthToken = requireNonNull(builder.twilioAuthToken);
		this.twilioStatusCallbackUrl = builder.twilioStatusCallbackUrl;
		this.httpClient = builder.httpClient == null ? new DefaultHttpClient("twilio-sms") : builder.httpClient;
		this.handlebarsTemplater = builder.handlebarsTemplater == null ? new HandlebarsTemplater.Builder(Paths.get("messages/sms")).build() : builder.handlebarsTemplater;
		this.normalizer = builder.normalizer == null ? new Normalizer() : builder.normalizer;
		this.gson = new Gson();
		this.logger = LoggerFactory.getLogger(getClass());

		String normalizedFromNumber = getNormalizer().normalizePhoneNumberToE164(builder.twilioFromNumber, Locale.US).orElse(null);

		// In the future, we might want to support a messaging service ID instead of/in combination with "from number".
		// For now, just require the "from number" exists and is valid.
		if (normalizedFromNumber == null)
			throw new IllegalArgumentException("Valid Twilio 'from number' is required.");

		this.twilioFromNumber = normalizedFromNumber;
	}

	@Override
	public String sendMessage(@Nonnull SmsMessage smsMessage) {
		requireNonNull(smsMessage);

		Map<String, Object> messageContext = new HashMap<>(smsMessage.getMessageContext());
		String body = getHandlebarsTemplater().mergeTemplate(smsMessage.getMessageTemplate().name(), "body", smsMessage.getLocale(), messageContext).get();
		String normalizedToNumber = getNormalizer().normalizePhoneNumberToE164(smsMessage.getToNumber(), Locale.US).get();

		if ("+12155551212".equals(normalizedToNumber)) {
			getLogger().debug("Fake-sending SMS from {} to {} because the destination number is a test number. Message is '{}'...", getTwilioFromNumber(), normalizedToNumber, smsMessage);
			return format("fake-%s", UUID.randomUUID());
		}

		getLogger().debug("Sending SMS from {} to {} using Twilio. Message is '{}'...", getTwilioFromNumber(), normalizedToNumber, smsMessage);

		// See https://www.twilio.com/docs/messaging/api/message-resource#create-a-message-resource
		String requestUrl = format("https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json", getTwilioAccountSid());

		String basicAuthCredentials = format("%s:%s", getTwilioAccountSid(), getTwilioAuthToken());
		String encodedBasicAuthCredentials = Base64.getEncoder().encodeToString(basicAuthCredentials.getBytes(StandardCharsets.UTF_8));

		Map<String, Object> requestHeaders = Map.of(
				"Authorization", format("Basic %s", encodedBasicAuthCredentials)
		);

		String twilioStatusCallbackUrl = getTwilioStatusCallbackUrl().orElse(null);

		Map<String, String> requestBodyComponents = new LinkedHashMap<>();
		requestBodyComponents.put("To", normalizedToNumber);
		requestBodyComponents.put("From", getTwilioFromNumber());
		requestBodyComponents.put("Body", body);

		if (twilioStatusCallbackUrl != null)
			requestBodyComponents.put("StatusCallback", twilioStatusCallbackUrl);

		// "Old time" HTML FORM POST body
		String requestBody = requestBodyComponents.entrySet().stream()
				.map(entry -> {
					String name = entry.getKey();
					String value = entry.getValue();

					try {
						value = URLEncoder.encode(value, "UTF-8");
					} catch (UnsupportedEncodingException e) {
						throw new IllegalStateException("Platform does not support UTF-8", e);
					}

					return format("%s=%s", name, value);
				})
				.collect(Collectors.joining("&"));

		HttpRequest httpRequest = new HttpRequest.Builder(HttpMethod.POST, requestUrl)
				.contentType("application/x-www-form-urlencoded; charset=utf-8")
				.headers(requestHeaders)
				.body(requestBody)
				.build();

		try {
			long time = System.currentTimeMillis();

			HttpResponse httpResponse = getHttpClient().execute(httpRequest);

			long elapsedTime = System.currentTimeMillis() - time;

			byte[] responseBodyAsBytes = httpResponse.getBody().orElse(null);
			String responseBody = responseBodyAsBytes == null ? null : new String(responseBodyAsBytes, StandardCharsets.UTF_8).trim();

			if (httpResponse.getStatus() >= 400)
				throw new RuntimeException(format("Unable to send SMS to %s. Response body was: %s", normalizedToNumber, responseBody));

			String sid;

			try {
				TwilioSendMessageResponse twilioSendMessageResponse = getGson().fromJson(responseBody, TwilioSendMessageResponse.class);
				sid = twilioSendMessageResponse.getSid();

				if (sid == null)
					throw new Exception("Response is missing 'sid' field.");
			} catch (Exception e) {
				throw new RuntimeException(format("Unable parse Twilio response to 'Send SMS' for %s. Response body was: %s", normalizedToNumber, responseBody));
			}

			getLogger().info("Successfully sent SMS (SID {}) in {} ms.", sid, elapsedTime);

			return sid;
		} catch (IOException e) {
			throw new UncheckedIOException(format("Unable to send SMS to %s", normalizedToNumber), e);
		}
	}

	@NotThreadSafe
	protected static class TwilioSendMessageResponse {
		@Nullable
		private String sid;

		// In the future, add other fields as needed.
		//
		// Response example:
		// {
		//  "account_sid": "ACXXXXXXXX",
		//  "api_version": "2010-04-01",
		//  "body": "twilio test",
		//  "date_created": "Fri, 08 Mar 2024 21:05:11 +0000",
		//  "date_sent": null,
		//  "date_updated": "Fri, 08 Mar 2024 21:05:11 +0000",
		//  "direction": "outbound-api",
		//  "error_code": null,
		//  "error_message": null,
		//  "from": "+12155551212",
		//  "messaging_service_sid": null,
		//  "num_media": "0",
		//  "num_segments": "1",
		//  "price": null,
		//  "price_unit": "USD",
		//  "sid": "SMXXXXXXXX",
		//  "status": "queued",
		//  "subresource_uris": {
		//    "media": "/2010-04-01/Accounts/ACXXXXXXXX/Messages/SMXXXXXXXX/Media.json"
		//  },
		//  "to": "+12155551212",
		//  "uri": "/2010-04-01/Accounts/ACXXXXXXXX/Messages/SMXXXXXXXX.json"
		// }

		// Example of an error response for a 400:
		// {"code": 21604, "message": "A 'To' phone number is required.", "more_info": "https://www.twilio.com/docs/errors/21604", "status": 400}

		@Nullable
		public String getSid() {
			return this.sid;
		}

		public void setSid(@Nullable String sid) {
			this.sid = sid;
		}
	}

	@NotThreadSafe
	public static class Builder {
		@Nonnull
		private final String twilioAccountSid;
		@Nonnull
		private final String twilioAuthToken;
		@Nullable
		private String twilioFromNumber;
		@Nullable
		private String twilioStatusCallbackUrl;
		@Nullable
		private HttpClient httpClient;
		@Nonnull
		private HandlebarsTemplater handlebarsTemplater;
		@Nullable
		private Normalizer normalizer;

		public Builder(@Nonnull String twilioAccountSid,
									 @Nonnull String twilioAuthToken) {
			requireNonNull(twilioAccountSid);
			requireNonNull(twilioAuthToken);

			this.twilioAccountSid = twilioAccountSid;
			this.twilioAuthToken = twilioAuthToken;
		}

		@Nonnull
		public Builder twilioFromNumber(@Nullable String twilioFromNumber) {
			this.twilioFromNumber = twilioFromNumber;
			return this;
		}

		@Nonnull
		public Builder twilioStatusCallbackUrl(@Nullable String twilioStatusCallbackUrl) {
			this.twilioStatusCallbackUrl = twilioStatusCallbackUrl;
			return this;
		}

		@Nonnull
		public Builder httpClient(@Nullable HttpClient httpClient) {
			this.httpClient = httpClient;
			return this;
		}

		@Nonnull
		public Builder handlebarsTemplater(@Nullable HandlebarsTemplater handlebarsTemplater) {
			this.handlebarsTemplater = handlebarsTemplater;
			return this;
		}

		@Nonnull
		public Builder normalizer(@Nullable Normalizer normalizer) {
			this.normalizer = normalizer;
			return this;
		}

		@Nonnull
		public TwilioSmsMessageSender build() {
			return new TwilioSmsMessageSender(this);
		}
	}

	@Nonnull
	@Override
	public MessageVendorId getMessageVendorId() {
		return MessageVendorId.TWILIO;
	}

	@Nonnull
	@Override
	public MessageTypeId getMessageTypeId() {
		return MessageTypeId.SMS;
	}

	@Nonnull
	protected String getTwilioAccountSid() {
		return this.twilioAccountSid;
	}

	@Nonnull
	protected String getTwilioAuthToken() {
		return this.twilioAuthToken;
	}

	@Nonnull
	protected String getTwilioFromNumber() {
		return this.twilioFromNumber;
	}

	@Nonnull
	protected Optional<String> getTwilioStatusCallbackUrl() {
		return Optional.ofNullable(this.twilioStatusCallbackUrl);
	}

	@Nonnull
	protected HttpClient getHttpClient() {
		return this.httpClient;
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
	protected Gson getGson() {
		return this.gson;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}