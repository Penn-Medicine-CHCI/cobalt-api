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
public class TwilioCallWebhookRequestBody {
	@Nullable
	private final TwilioCallStatus twilioCallStatus;
	@Nonnull
	private final Map<String, String> parameters;

	public TwilioCallWebhookRequestBody(@Nonnull String requestBody) {
		requireNonNull(requestBody);

		Map<String, String> parameters = new HashMap<>();
		String[] requestBodyParameters = requestBody.split("&");

		for (String requestBodyParameter : requestBodyParameters) {
			String[] values = requestBodyParameter.split("=");
			String name = values[0];
			String value = values.length > 1 ? WebUtility.urlDecode(values[1]) : "";
			parameters.put(name, value);
		}

		this.parameters = Collections.unmodifiableMap(parameters);
		this.twilioCallStatus = twilioCallStatusFromParameters(this.parameters).orElse(null);
	}

	public TwilioCallWebhookRequestBody(@Nonnull Map<String, String> parameters) {
		requireNonNull(parameters);

		this.parameters = Collections.unmodifiableMap(new HashMap<>(parameters));
		this.twilioCallStatus = twilioCallStatusFromParameters(this.parameters).orElse(null);
	}

	@Override
	public String toString() {
		return format("%s{twilioCallStatus=%s, parameters=%s}", getClass().getSimpleName(), getTwilioCallStatus(), getParameters());
	}

	@Nonnull
	public Optional<TwilioCallStatus> getTwilioCallStatus() {
		return Optional.ofNullable(this.twilioCallStatus);
	}

	@Nonnull
	public Map<String, String> getParameters() {
		return this.parameters;
	}

	@Nonnull
	protected Optional<TwilioCallStatus> twilioCallStatusFromParameters(@Nonnull Map<String, String> parameters) {
		requireNonNull(parameters);

		for (Entry<String, String> entry : parameters.entrySet())
			if (entry.getKey().equalsIgnoreCase("CallStatus"))
				return TwilioCallStatus.fromName(entry.getValue());

		return Optional.empty();
	}
}
