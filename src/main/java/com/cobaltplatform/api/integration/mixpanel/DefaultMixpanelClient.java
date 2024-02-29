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

package com.cobaltplatform.api.integration.mixpanel;

import com.cobaltplatform.api.http.DefaultHttpClient;
import com.cobaltplatform.api.http.HttpClient;
import com.cobaltplatform.api.http.HttpMethod;
import com.cobaltplatform.api.http.HttpRequest;
import com.cobaltplatform.api.http.HttpRequestOption;
import com.cobaltplatform.api.http.HttpResponse;
import com.cobaltplatform.api.util.WebUtility;
import com.google.gson.Gson;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
public class DefaultMixpanelClient implements MixpanelClient {
	@Nonnull
	private final Long projectId;
	@Nonnull
	private final String serviceAccountUsername;
	@Nonnull
	private final String serviceAccountSecret;
	@Nonnull
	private final HttpClient httpClient;
	@Nonnull
	private final Gson gson;

	public DefaultMixpanelClient(@Nonnull Long projectId,
															 @Nonnull String serviceAccountUsername,
															 @Nonnull String serviceAccountSecret) {
		requireNonNull(projectId);
		requireNonNull(serviceAccountUsername);
		requireNonNull(serviceAccountSecret);

		this.projectId = projectId;
		this.serviceAccountUsername = serviceAccountUsername;
		this.serviceAccountSecret = serviceAccountSecret;
		this.gson = new Gson();
		this.httpClient = new DefaultHttpClient("mixpanel-client", (okHttpClientBuilder) -> {
			// Nice long timeouts because Mixpanel can take a long time...
			okHttpClientBuilder.connectTimeout(60, TimeUnit.SECONDS)
					.writeTimeout(5, TimeUnit.MINUTES)
					.readTimeout(5, TimeUnit.MINUTES);
		});
	}

	@Nonnull
	@Override
	public List<MixpanelEvent> findEventsForDateRange(@Nonnull LocalDate fromDate,
																										@Nonnull LocalDate toDate) {
		requireNonNull(fromDate);
		requireNonNull(toDate);

		String authorization = Base64.getEncoder().encodeToString(format("%s:%s", getServiceAccountUsername(), getServiceAccountSecret()).getBytes(StandardCharsets.UTF_8));

		HttpRequest httpRequest = new HttpRequest.Builder(HttpMethod.GET, "https://data.mixpanel.com/api/2.0/export")
				.headers(Map.of(
						"Accept", "text/plain",
						"Authorization", format("Basic %s", authorization)
				))
				.queryParameters(Map.of(
						"project_id", getProjectId(),
						"from_date", DateTimeFormatter.ISO_LOCAL_DATE.format(fromDate),
						"to_date", DateTimeFormatter.ISO_LOCAL_DATE.format(toDate)
				))
				.build();

		try {
			HttpResponse httpResponse = getHttpClient().execute(httpRequest, HttpRequestOption.SUPPRESS_RESPONSE_BODY_LOGGING);
			String responseBody = httpResponse.getBody().isPresent() ? new String(httpResponse.getBody().get()) : null;

			if (httpResponse.getStatus() >= 400)
				throw new IOException(format("Bad mixpanel status: %d.  Response body was\n%s", httpResponse.getStatus(), responseBody));

			// Response format is a textfile where every line is a JSON event object
			List<MixpanelEvent> mixpanelEvents = responseBody.lines()
					.filter(line -> line != null && line.trim().length() > 0)
					.map(line -> {
						Map<String, Object> eventJson = getGson().fromJson(line, Map.class);
						Map<String, Object> properties = (Map<String, Object>) eventJson.get("properties");

						elideSensitiveDataInUrlProperty("$current_url", properties);
						elideSensitiveDataInUrlProperty("$initial_referrer", properties);
						elideSensitiveDataInUrlProperty("$referrer", properties);

						String event = (String) eventJson.get("event");
						String distinctId = (String) properties.get("distinct_id");
						String anonId = (String) properties.get("$anon_id");
						String userId = (String) properties.get("$user_id");
						String deviceId = (String) properties.get("$device_id");
						Instant time = Instant.ofEpochSecond(((Double) properties.get("time")).longValue());

						try {
							return new MixpanelEvent(distinctId, anonId, userId, deviceId, time, event, properties);
						} catch (Exception e) {
							throw new IllegalArgumentException(format("Unable to create Mixpanel event from data: %s", eventJson), e);
						}
					})
					.collect(Collectors.toList());

			return mixpanelEvents;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	protected void elideSensitiveDataInUrlProperty(@Nonnull String propertyName,
																								 @Nonnull Map<String, Object> properties) {
		requireNonNull(propertyName);
		requireNonNull(properties);

		String url = (String) properties.get(propertyName);

		if (url != null) {
			url = WebUtility.elideSensitiveDataInUrl(url);
			properties.put(propertyName, url);
		}
	}

	@Nonnull
	protected Long getProjectId() {
		return this.projectId;
	}

	@Nonnull
	protected String getServiceAccountUsername() {
		return this.serviceAccountUsername;
	}

	@Nonnull
	protected String getServiceAccountSecret() {
		return this.serviceAccountSecret;
	}

	@Nonnull
	protected HttpClient getHttpClient() {
		return this.httpClient;
	}

	@Nonnull
	protected Gson getGson() {
		return this.gson;
	}
}