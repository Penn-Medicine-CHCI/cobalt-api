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

import com.cobaltplatform.api.util.WebUtility;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class TwilioRequestBody {
	@Nullable
	private final TwilioMessageStatus twilioMessageStatus;
	@Nonnull
	private final Map<String, String> parameters;

	public TwilioRequestBody(@Nonnull String requestBody) {
		requireNonNull(requestBody);

		Map<String, String> parameters = new HashMap<>();
		String[] requestBodyParameters = requestBody.split("&");

		for (String requestBodyParameter : requestBodyParameters) {
			String[] values = requestBodyParameter.split("=");
			parameters.put(values[0], WebUtility.urlDecode(values[1]));
		}

		this.parameters = Collections.unmodifiableMap(parameters);
		this.twilioMessageStatus = twilioMessageStatusFromParameters(this.parameters).orElse(null);
	}

	public TwilioRequestBody(@Nonnull Map<String, String> parameters) {
		requireNonNull(parameters);

		this.parameters = Collections.unmodifiableMap(new HashMap<>(parameters));
		this.twilioMessageStatus = twilioMessageStatusFromParameters(this.parameters).orElse(null);
	}

	@Override
	public String toString() {
		return format("%s{twilioMessageStatus=%s, parameters=%s}", getClass().getSimpleName(), getTwilioMessageStatus(), getParameters());
	}

	@Nonnull
	public Optional<TwilioMessageStatus> getTwilioMessageStatus() {
		return Optional.ofNullable(this.twilioMessageStatus);
	}

	@Nonnull
	public Map<String, String> getParameters() {
		return this.parameters;
	}

	@Nonnull
	protected Optional<TwilioMessageStatus> twilioMessageStatusFromParameters(@Nonnull Map<String, String> parameters) {
		requireNonNull(parameters);

		for (Entry<String, String> entry : parameters.entrySet())
			if (entry.getKey().equalsIgnoreCase("MessageStatus"))
				return TwilioMessageStatus.fromName(entry.getValue());

		return Optional.empty();
	}
}
