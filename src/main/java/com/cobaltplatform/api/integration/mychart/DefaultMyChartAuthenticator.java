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

package com.cobaltplatform.api.integration.mychart;

import com.cobaltplatform.api.http.DefaultHttpClient;
import com.cobaltplatform.api.http.HttpClient;
import com.cobaltplatform.api.http.HttpMethod;
import com.cobaltplatform.api.http.HttpRequest;
import com.cobaltplatform.api.http.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.cobaltplatform.api.util.WebUtility.urlEncode;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class DefaultMyChartAuthenticator implements MyChartAuthenticator {
	@Nonnull
	private final MyChartConfiguration myChartConfiguration;
	@Nonnull
	private final HttpClient httpClient;
	@Nonnull
	private final Gson gson;

	public DefaultMyChartAuthenticator(@Nonnull MyChartConfiguration myChartConfiguration) {
		this(myChartConfiguration, new DefaultHttpClient("mychart-authenticator"));
	}

	public DefaultMyChartAuthenticator(@Nonnull MyChartConfiguration myChartConfiguration,
																		 @Nonnull HttpClient httpClient) {
		requireNonNull(myChartConfiguration);
		requireNonNull(httpClient);

		verifyMyChartConfiguration(myChartConfiguration);

		this.httpClient = httpClient;
		this.myChartConfiguration = myChartConfiguration;
		this.gson = new Gson();
	}

	protected void verifyMyChartConfiguration(@Nonnull MyChartConfiguration myChartConfiguration) {
		requireNonNull(myChartConfiguration);

		if (trimToNull(myChartConfiguration.getClientId()) == null)
			throw new IllegalStateException("Client ID is required.");

		if (trimToNull(myChartConfiguration.getResponseType()) == null)
			throw new IllegalStateException("Response type is required.");

		if (trimToNull(myChartConfiguration.getScope()) == null)
			throw new IllegalStateException("Scope is required.");

		if (trimToNull(myChartConfiguration.getCallbackUrl()) == null)
			throw new IllegalStateException("Callback URL is required.");

		if (trimToNull(myChartConfiguration.getTokenUrl()) == null)
			throw new IllegalStateException("Token URL is required.");

		if (trimToNull(myChartConfiguration.getAuthorizeUrl()) == null)
			throw new IllegalStateException("Authorize URL is required.");
	}

	@Override
	@Nonnull
	public String generateAuthenticationRedirectUrl(@Nullable String state) {
		state = trimToNull(state);

		List<String> queryParameters = new ArrayList<>();
		queryParameters.add(format("response_type=%s", urlEncode(getMyChartConfiguration().getResponseType())));
		queryParameters.add(format("client_id=%s", urlEncode(getMyChartConfiguration().getClientId())));
		queryParameters.add(format("redirect_uri=%s", urlEncode(getMyChartConfiguration().getCallbackUrl())));
		queryParameters.add(format("scope=%s", urlEncode(getMyChartConfiguration().getScope())));

		String aud = trimToNull(getMyChartConfiguration().getAud());

		if (aud != null)
			queryParameters.add(format("aud=%s", urlEncode(aud)));

		if (state != null)
			queryParameters.add(format("state=%s", urlEncode(state)));

		return format("%s?%s", getMyChartConfiguration().getAuthorizeUrl(), queryParameters.stream()
				.collect(Collectors.joining("&")));
	}

	@Nonnull
	@Override
	public MyChartAccessToken obtainAccessTokenFromCode(@Nonnull String code,
																											@Nullable String state) throws MyChartException {
		requireNonNull(code);

		state = trimToNull(state);

		List<String> requestBodyComponents = new ArrayList<>(5);
		requestBodyComponents.add("grant_type=authorization_code");
		requestBodyComponents.add(format("code=%s", code.trim()));
		requestBodyComponents.add(format("redirect_uri=%s", urlEncode(getMyChartConfiguration().getCallbackUrl())));
		requestBodyComponents.add(format("client_id=%s", urlEncode(getMyChartConfiguration().getClientId())));

		String aud = trimToNull(getMyChartConfiguration().getAud());

		if (aud != null)
			requestBodyComponents.add(format("aud=%s", urlEncode(aud)));

		if (state != null)
			requestBodyComponents.add(format("state=%s", state));

		String requestBody = requestBodyComponents.stream().collect(Collectors.joining("&"));

		HttpRequest httpRequest = new HttpRequest.Builder(HttpMethod.POST, getMyChartConfiguration().getTokenUrl())
				.contentType("application/x-www-form-urlencoded")
				.body(requestBody)
				.build();

		try {
			HttpResponse httpResponse = getHttpClient().execute(httpRequest);

			if (httpResponse.getStatus() >= 400)
				throw new MyChartException(format("Unable to exchange MyChart code for access token. HTTP status %d, Response Body:\n%s",
						httpResponse.getStatus(), httpResponse.getBody().isPresent() ? new String(httpResponse.getBody().get(), StandardCharsets.UTF_8) : "[no response body]"));

			String json = new String(httpResponse.getBody().get(), StandardCharsets.UTF_8);
			Map<String, Object> jsonObject = getGson().fromJson(json, new TypeToken<Map<String, Object>>() {
			}.getType());

			// {
			//  "access_token": "Fcxxx",
			//  "token_type": "Bearer",
			//  "expires_in": 3600,
			//  "scope": "patient/Patient.read launch/patient",
			//  "__epic.dstu2.patient": "TOxxxfkB",
			//  "patient": "eazxxLA3"
			// }
			String accessToken = jsonObject.get("access_token").toString();
			String tokenType = jsonObject.get("token_type").toString();
			String tokenState = jsonObject.get("state") != null ? jsonObject.get("state").toString() : null;  // In theory should be identical to "state" passed in to this method
			Integer expiresIn = ((Number) jsonObject.get("expires_in")).intValue();
			String scope = jsonObject.get("scope").toString() == null ? null : jsonObject.get("scope").toString();
			String refreshToken = jsonObject.get("refresh_token") == null ? null : jsonObject.get("refresh_token").toString();

			Instant expiresAt = Instant.now().plus(expiresIn, ChronoUnit.SECONDS);

			// Metadata is just a copy of the response body with the well-known keys removed.
			// An institution might provide fields like "__epic.dstu2.patient" or "patient", for example,
			// and those would be surfaced in the metadata.
			Map<String, Object> metadata = new HashMap<>(jsonObject);
			metadata.remove("access_token");
			metadata.remove("token_type");
			metadata.remove("state");
			metadata.remove("expires_in");
			metadata.remove("scope");
			metadata.remove("refresh_token");

			return new MyChartAccessToken.Builder(accessToken, tokenType, expiresAt)
					.refreshToken(refreshToken)
					.scope(scope)
					.state(tokenState)
					.metadata(metadata)
					.build();
		} catch (MyChartException e) {
			throw e;
		} catch (Exception e) {
			throw new MyChartException("Unable to acquire access token for code", e);
		}
	}

	@Nonnull
	protected MyChartConfiguration getMyChartConfiguration() {
		return this.myChartConfiguration;
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