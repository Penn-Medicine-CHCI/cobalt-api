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

import com.cobaltplatform.api.Configuration;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
public class DefaultTwilioRequestValidator implements TwilioRequestValidator {
	@Nonnull
	private final String twilioAuthToken;
	@Nonnull
	private final Logger logger;

	public DefaultTwilioRequestValidator(@Nonnull String twilioAuthToken) {
		requireNonNull(twilioAuthToken);

		this.twilioAuthToken = twilioAuthToken;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	public DefaultTwilioRequestValidator(@Nonnull Configuration configuration) {
		requireNonNull(configuration);

		this.twilioAuthToken = configuration.getTwilioAuthToken();
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	@Override
	public Boolean validateRequest(@Nonnull String requestUrl,
																 @Nonnull String twilioSignature,
																 @Nonnull String requestBody) {
		requireNonNull(requestUrl);
		requireNonNull(twilioSignature);
		requireNonNull(requestBody);

		// See https://www.twilio.com/docs/usage/security#validating-requests
		//
		// 1. Take the full URL of the request URL you specify for your phone number or app, from the protocol (https...) through the end of the query string (everything after the ?).
		// 2. If the request is a POST, sort all of the POST parameters alphabetically (using Unix-style case-sensitive sorting order).
		// 3. Iterate through the sorted list of POST parameters, and append the variable name and value (with no delimiters) to the end of the URL string.
		// 4. Sign the resulting string with HMAC-SHA1 using your AuthToken as the key (remember, your AuthToken's case matters!).
		// 5. Base64 encode the resulting hash value.
		// 6. Compare your hash to ours, submitted in the X-Twilio-Signature header. If they match, then you're good to go.
		TwilioRequestBody twilioRequestBody = new TwilioRequestBody(requestBody);
		SortedMap<String, String> sortedRequestBodyParameters = new TreeMap<>(twilioRequestBody.getParameters());

		String hashableString = requestUrl + sortedRequestBodyParameters.entrySet().stream()
				.map(requestBodyParameter -> requestBodyParameter.getKey() + requestBodyParameter.getValue())
				.collect(Collectors.joining());

		HashCode hashCode = Hashing.hmacSha1(getTwilioAuthToken().getBytes(StandardCharsets.UTF_8)).hashString(hashableString, StandardCharsets.UTF_8);
		String calculatedSignature = BaseEncoding.base64().encode(hashCode.asBytes());

		// Note: not obvious from the documentation, but it does not appear this can succeed for subaccount AuthTokens -
		// Twilio appears to sign using primary account AuthToken.
		return twilioSignature.equals(calculatedSignature);
	}

	@Nonnull
	protected String getTwilioAuthToken() {
		return this.twilioAuthToken;
	}

	@Nonnull
	protected Logger getLogger() {
		return this.logger;
	}
}
