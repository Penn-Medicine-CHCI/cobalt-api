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
import com.cobaltplatform.api.util.WebUtility;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

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
	public String generateAuthorizationRedirectUrl(@Nullable String state,
																								 @Nullable Map<String, String> additionalParameters) {
		List<String> queryParameters = new ArrayList<>();
		queryParameters.add(format("response_type=%s", getMyChartConfiguration().getResponseType()));
		queryParameters.add(format("client_id=%s", getMyChartConfiguration().getClientId()));
		queryParameters.add(format("redirect_uri=%s", WebUtility.urlEncode(getMyChartConfiguration().getCallbackUrl())));
		queryParameters.add(format("scope=%s", WebUtility.urlEncode(getMyChartConfiguration().getScope())));

		if (state != null)
			queryParameters.add(format("state=%s", WebUtility.urlEncode(state)));

		if (additionalParameters != null) {
			for (Entry<String, String> entry : additionalParameters.entrySet()) {
				queryParameters.add(format("%s=%s", entry.getKey(), WebUtility.urlEncode(entry.getValue())));
			}
		}

		return format("%s?%s", getMyChartConfiguration().getAuthorizeUrl(), queryParameters.stream().collect(Collectors.joining("&")));
	}

	@Nonnull
	@Override
	public MyChartAccessToken obtainAccessTokenFromCode(@Nonnull String code) throws MyChartException {
		requireNonNull(code);

		String requestBody = List.of(
				"grant_type=authorization_code",
				format("code=%s", code.trim()),
				format("redirect_uri=%s", getMyChartConfiguration().getCallbackUrl()),
				format("client_id=%s", getMyChartConfiguration().getClientId())
		).stream().collect(Collectors.joining("&"));

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
			Integer expiresIn = ((Number) jsonObject.get("expires_in")).intValue();
			String scope = jsonObject.get("scope").toString() == null ? null : jsonObject.get("scope").toString();
			String refreshToken = jsonObject.get("refresh_token") == null ? null : jsonObject.get("refresh_token").toString();

			return new MyChartAccessToken(accessToken, tokenType, Instant.now().plus(expiresIn, ChronoUnit.SECONDS),
					jsonObject, scope, refreshToken);
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
