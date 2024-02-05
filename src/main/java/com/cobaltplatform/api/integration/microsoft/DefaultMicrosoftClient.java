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

package com.cobaltplatform.api.integration.microsoft;

import com.cobaltplatform.api.http.DefaultHttpClient;
import com.cobaltplatform.api.http.HttpClient;
import com.cobaltplatform.api.http.HttpMethod;
import com.cobaltplatform.api.http.HttpRequest;
import com.cobaltplatform.api.http.HttpResponse;
import com.cobaltplatform.api.integration.microsoft.model.OnlineMeeting;
import com.cobaltplatform.api.integration.microsoft.model.Subscription;
import com.cobaltplatform.api.integration.microsoft.model.User;
import com.cobaltplatform.api.integration.microsoft.request.OnlineMeetingCreateRequest;
import com.cobaltplatform.api.integration.microsoft.request.SubscriptionCreateRequest;
import com.google.gson.Gson;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class DefaultMicrosoftClient implements MicrosoftClient {
	@Nonnull
	private Supplier<MicrosoftAccessToken> accessTokenSupplier;
	@Nonnull
	private final HttpClient httpClient;
	@Nonnull
	private final Gson gson;

	public DefaultMicrosoftClient(@Nonnull Supplier<MicrosoftAccessToken> accessTokenSupplier) {
		this(requireNonNull(accessTokenSupplier), null);
	}

	public DefaultMicrosoftClient(@Nonnull Supplier<MicrosoftAccessToken> accessTokenSupplier,
																@Nullable HttpClient httpClient) {
		requireNonNull(accessTokenSupplier);

		this.accessTokenSupplier = accessTokenSupplier;
		this.httpClient = httpClient == null ? new DefaultHttpClient("microsoft-client") : httpClient;
		this.gson = new Gson();
	}

	@Nonnull
	@Override
	public Optional<User> getUser(@Nullable String id) throws MicrosoftException {
		id = trimToNull(id);

		if (id == null)
			return Optional.empty();

		HttpMethod httpMethod = HttpMethod.GET;
		String url = format("users/%s", id);

		Function<String, Optional<User>> responseBodyMapper = (responseBody) -> {
			User response = getGson().fromJson(responseBody, User.class);
			return Optional.of(response);
		};

		ApiCall<Optional<User>> apiCall = new ApiCall.Builder<>(httpMethod, url, responseBodyMapper)
				.build();

		return makeApiCall(apiCall);
	}

	@Nonnull
	@Override
	public Subscription createSubscription(@Nonnull SubscriptionCreateRequest request) throws MicrosoftException {
		requireNonNull(request);

		HttpMethod httpMethod = HttpMethod.POST;
		String url = "subscriptions";

		Map<String, Object> requestBodyJson = new HashMap<>();
		requestBodyJson.put("changeType", request.getChangeType());
		requestBodyJson.put("notificationUrl", request.getNotificationUrl());
		requestBodyJson.put("resource", request.getResource());
		requestBodyJson.put("expirationDateTime", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(request.getExpirationDateTime()));
		requestBodyJson.put("clientState", request.getClientState());

		String requestBody = getGson().toJson(requestBodyJson);

		Function<String, Subscription> responseBodyMapper = (responseBody) -> {
			return getGson().fromJson(responseBody, Subscription.class);
		};

		ApiCall<Subscription> apiCall = new ApiCall.Builder<>(httpMethod, url, responseBodyMapper)
				.requestBody(requestBody)
				.build();

		return makeApiCall(apiCall);
	}

	@Override
	public void deleteSubscription(@Nullable String id) throws MicrosoftException {
		id = trimToNull(id);

		if (id == null)
			return;

		HttpMethod httpMethod = HttpMethod.DELETE;
		String url = format("subscriptions/%s", id);

		Function<String, Optional<String>> responseBodyMapper = (responseBody) -> {
			return Optional.empty();
		};

		ApiCall<Optional<String>> apiCall = new ApiCall.Builder<>(httpMethod, url, responseBodyMapper)
				.build();

		makeApiCall(apiCall);
	}

	@Nonnull
	@Override
	public OnlineMeeting createOnlineMeeting(@Nonnull OnlineMeetingCreateRequest request) throws MicrosoftException {
		requireNonNull(request);

		String userId = trimToNull(request.getUserId());

		HttpMethod httpMethod = HttpMethod.POST;
		String url = userId == null ? "me/onlineMeetings" : format("users/%s/onlineMeetings", userId);

		Map<String, Object> requestBodyJson = new HashMap<>();
		requestBodyJson.put("subject", request.getSubject());
		requestBodyJson.put("startDateTime", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(request.getStartDateTime()));
		requestBodyJson.put("endDateTime", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(request.getEndDateTime()));

		Map<String, Object> participants = new HashMap<>();
		participants.put("attendees", List.of());
		participants.put("organizer", null);

		requestBodyJson.put("participants", participants);

		String requestBody = getGson().toJson(requestBodyJson);

		Function<String, OnlineMeeting> responseBodyMapper = (responseBody) -> {
			OnlineMeeting onlineMeeting = getGson().fromJson(responseBody, OnlineMeeting.class);
			onlineMeeting.setRawJson(responseBody);

			return onlineMeeting;
		};

		ApiCall<OnlineMeeting> apiCall = new ApiCall.Builder<>(httpMethod, url, responseBodyMapper)
				.requestBody(requestBody)
				.build();

		return makeApiCall(apiCall);
	}

	@Nonnull
	protected <T> T makeApiCall(@Nonnull ApiCall<T> apiCall) {
		requireNonNull(apiCall);

		String finalUrl = format("https://graph.microsoft.com/v1.0/%s", apiCall.getUrl());
		String accessToken = getAccessToken().getAccessToken();

		Map<String, Object> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");
		headers.put("Accept", "application/json");
		headers.put("Authorization", format("Bearer %s", accessToken));

		HttpRequest.Builder httpRequestBuilder = new HttpRequest.Builder(apiCall.getHttpMethod(), finalUrl)
				.headers(headers);

		if (apiCall.getQueryParameters().size() > 0)
			httpRequestBuilder.queryParameters(apiCall.getQueryParameters());

		String requestBody = apiCall.getRequestBody().orElse(null);

		if (requestBody != null)
			httpRequestBuilder.body(requestBody);

		if (apiCall.getHttpMethod() == HttpMethod.POST)
			httpRequestBuilder.contentType("application/json; charset=utf-8");

		HttpRequest httpRequest = httpRequestBuilder.build();

		String queryParametersDescription = apiCall.getQueryParameters().size() == 0 ? "[none]" : apiCall.getQueryParameters().toString();
		String requestBodyDescription = requestBody == null ? "[none]" : requestBody;

		try {
			HttpResponse httpResponse = getHttpClient().execute(httpRequest);
			byte[] rawResponseBody = httpResponse.getBody().orElse(null);
			String responseBody = rawResponseBody == null ? null : new String(rawResponseBody, StandardCharsets.UTF_8);

			// TODO: parse messaging out into fields on MicrosoftException for better error experience

			// {
			//    "error": {
			//        "code": "Authorization_RequestDenied",
			//        "message": "Insufficient privileges to complete the operation.",
			//        "innerError": {
			//            "date": "2023-01-01T20:35:34",
			//            "request-id": "fb0722f0-0d10-4b6b-ae02-cd5d79a422a7",
			//            "client-request-id": "fb0722f0-0d10-4b6b-ae02-cd5d79a422a7"
			//        }
			//    }
			// }

			if (httpResponse.getStatus() > 299)
				throw new MicrosoftException(format("Bad HTTP response %d for Microsoft endpoint %s %s with query params %s and request body %s. Response body was\n%s", httpResponse.getStatus(), httpRequest.getHttpMethod().name(), httpRequest.getUrl(), queryParametersDescription, requestBodyDescription, responseBody));

			try {
				return apiCall.getResponseBodyMapper().apply(responseBody);
			} catch (Exception e) {
				throw new MicrosoftException(format("Unable to parse JSON for Microsoft endpoint %s %s with query params %s and request body %s. Response body was\n%s", httpRequest.getHttpMethod().name(), httpRequest.getUrl(), queryParametersDescription, requestBodyDescription, responseBody), e);
			}
		} catch (IOException e) {
			throw new MicrosoftException(format("Unable to call Microsoft endpoint %s %s with query params %s and request body %s", httpRequest.getHttpMethod().name(), httpRequest.getUrl(), queryParametersDescription, requestBodyDescription), e);
		}
	}

	@Nonnull
	protected MicrosoftAccessToken getAccessToken() {
		return this.accessTokenSupplier.get();
	}

	@Nonnull
	protected HttpClient getHttpClient() {
		return this.httpClient;
	}

	@Nonnull
	protected Gson getGson() {
		return this.gson;
	}

	@ThreadSafe
	protected static class ApiCall<T> {
		@Nonnull
		private final HttpMethod httpMethod;
		@Nonnull
		private final String url;
		@Nonnull
		private final Function<String, T> responseBodyMapper;
		@Nonnull
		private final Map<String, Object> queryParameters;
		@Nullable
		private final String requestBody;

		protected ApiCall(@Nonnull ApiCall.Builder<T> builder) {
			requireNonNull(builder);

			this.httpMethod = builder.httpMethod;
			this.url = builder.url;
			this.responseBodyMapper = builder.responseBodyMapper == null ? (responseBody) -> (T) responseBody : builder.responseBodyMapper;
			this.queryParameters = builder.queryParameters == null ? Collections.emptyMap() : builder.queryParameters;
			this.requestBody = builder.requestBody;
		}

		@Nonnull
		public HttpMethod getHttpMethod() {
			return this.httpMethod;
		}

		@Nonnull
		public String getUrl() {
			return this.url;
		}

		@Nonnull
		public Function<String, T> getResponseBodyMapper() {
			return this.responseBodyMapper;
		}

		@Nonnull
		public Map<String, Object> getQueryParameters() {
			return this.queryParameters;
		}

		@Nonnull
		public Optional<String> getRequestBody() {
			return Optional.ofNullable(this.requestBody);
		}

		@NotThreadSafe
		protected static class Builder<T> {
			@Nonnull
			private final HttpMethod httpMethod;
			@Nonnull
			private final String url;
			@Nonnull
			private final Function<String, T> responseBodyMapper;
			@Nullable
			private Map<String, Object> queryParameters;
			@Nullable
			private String requestBody;

			public Builder(@Nonnull HttpMethod httpMethod,
										 @Nonnull String url,
										 @Nonnull Function<String, T> responseBodyMapper) {
				requireNonNull(httpMethod);
				requireNonNull(url);
				requireNonNull(responseBodyMapper);

				this.httpMethod = httpMethod;
				this.url = url;
				this.responseBodyMapper = responseBodyMapper;
			}

			@Nonnull
			public ApiCall.Builder queryParameters(@Nullable Map<String, Object> queryParameters) {
				this.queryParameters = queryParameters;
				return this;
			}

			@Nonnull
			public ApiCall.Builder requestBody(@Nullable String requestBody) {
				this.requestBody = requestBody;
				return this;
			}

			@Nonnull
			public ApiCall<T> build() {
				return new ApiCall<>(this);
			}
		}
	}
}
