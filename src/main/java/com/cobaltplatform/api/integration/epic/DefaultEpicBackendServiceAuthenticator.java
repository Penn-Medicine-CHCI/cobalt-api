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

package com.cobaltplatform.api.integration.epic;

import com.cobaltplatform.api.http.DefaultHttpClient;
import com.cobaltplatform.api.http.HttpClient;
import com.cobaltplatform.api.http.HttpMethod;
import com.cobaltplatform.api.http.HttpRequest;
import com.cobaltplatform.api.http.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.cobaltplatform.api.util.WebUtility.urlEncode;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class DefaultEpicBackendServiceAuthenticator implements EpicBackendServiceAuthenticator {
	@Nonnull
	private final EpicBackendServiceConfiguration epicBackendServiceConfiguration;
	@Nonnull
	private final HttpClient httpClient;
	@Nonnull
	private final Gson gson;

	public DefaultEpicBackendServiceAuthenticator(@Nonnull EpicBackendServiceConfiguration epicBackendServiceConfiguration) {
		requireNonNull(epicBackendServiceConfiguration);

		this.epicBackendServiceConfiguration = epicBackendServiceConfiguration;
		this.httpClient = new DefaultHttpClient("epic-backend-service-authenticator");
		this.gson = new Gson();
	}

	@Nonnull
	@Override
	public String generateBackendServiceJwt() {
		// See https://fhir.epic.com/Documentation?docId=oauth2&section=Creating-Key-Pair
		// See https://fhir.epic.com/Documentation?docId=oauth2&section=Creating-JWTs

		Instant now = Instant.now();

		// JWT Header

		// This should always be set to JWT.
		String typ = "JWT";
		// For apps using JSON Web Key Sets (including dynamically registed clients), set this value to the kid of the target public key from your key set
		String kid = getEpicBackendServiceConfiguration().getJwksKeyId();
		// For apps using JSON Web Key Set URLs, optionally set this value to the URL you registered on your application
		String jku = getEpicBackendServiceConfiguration().getJwksUrl();

		// JWT Claims

		// Issuer of the JWT. This is the app's client_id, as determined during registration on the Epic on FHIR website,
		// or the client_id returned during a dynamic registration.
		// This is the same as the value for the sub claim.
		String iss = getEpicBackendServiceConfiguration().getClientId();
		// Issuer of the JWT. This is the app's client_id, as determined during registration on the Epic on FHIR website,
		// or the client_id returned during a dynamic registration.
		// This is the same as the value for the iss claim.
		String sub = getEpicBackendServiceConfiguration().getClientId();
		// The FHIR authorization server's token endpoint URL.
		// This is the same URL to which this authentication JWT will be posted.
		// It's possible that Epic community member systems will route web service traffic through a proxy server,
		// in which case the URL the JWT is posted to is not known to the authorization server, and the JWT will be rejected.
		// For such cases, Epic community member administrators can add additional audience URLs to the allowlist,
		// in addition to the FHIR server token URL if needed.
		String aud = getEpicBackendServiceConfiguration().getTokenUrl();
		// A unique identifier for the JWT.
		// The jti must be no longer than 151 characters and cannot be reused during the JWT's validity period,
		// i.e. before the exp time is reached.
		String jti = UUID.randomUUID().toString();
		// Expiration time integer for this authentication JWT, expressed in seconds since the "Epoch" (1970-01-01T00:00:00Z UTC).
		// The exp value must be in the future, and can be no more than 5 minutes in the future at the time the access token request is received.
		Long exp = now.plusSeconds(60L).toEpochMilli() / 1_000L;
		// Time integer before which the JWT must not be accepted for processing, expressed in seconds since the "Epoch" (1970-01-01T00:00:00Z UTC).
		// The nbf value cannot be in the future, cannot be more recent than the exp value, and the exp - nbf difference cannot be greater than 5 minutes.
		Long nbf = now.toEpochMilli() / 1_000L;
		// Time integer for when the JWT was created, expressed in seconds since the "Epoch" (1970-01-01T00:00:00Z UTC).
		// The iat value cannot be in the future, and the exp - iat difference cannot be greater than 5 minutes.
		Long iat = now.toEpochMilli() / 1_000L;

		Map<String, Object> header = new HashMap<>();
		header.put("typ", typ);
		header.put("kid", kid);
		header.put("jku", jku);

		Map<String, Object> claims = new HashMap<>();
		claims.put("iss", iss);
		claims.put("sub", sub);
		claims.put("aud", aud);
		claims.put("jti", jti);
		claims.put("exp", exp);
		claims.put("nbf", nbf);
		claims.put("iat", iat);

		return Jwts.builder()
				.setHeader(header)
				.setClaims(claims)
				// Currently only RSA signing algorithms are supported so RSA 384 should be used and this should be set to RS384
				.signWith(getEpicBackendServiceConfiguration().getKeyPair().getPrivate(), SignatureAlgorithm.RS384)
				.compact();
	}

	@Nonnull
	@Override
	public EpicBackendServiceAccessToken obtainAccessTokenFromBackendServiceJwt(@Nonnull String backendServiceJwt) {
		requireNonNull(backendServiceJwt);

		// Your application makes a HTTP POST request to the authorization server's OAuth 2.0 token endpoint to obtain access token.
		// The following form-urlencoded parameters are required in the POST body:
		List<String> requestBodyComponents = new ArrayList<>(3);

		// grant_type: This should be set to client_credentials.
		requestBodyComponents.add("grant_type=client_credentials");
		// client_assertion_type: This should be set to urn:ietf:params:oauth:client-assertion-type:jwt-bearer.
		requestBodyComponents.add(format("client_assertion_type=%s", urlEncode("urn:ietf:params:oauth:client-assertion-type:jwt-bearer")));
		// client_assertion: This should be set to the JWT
		requestBodyComponents.add(format("client_assertion=%s", urlEncode(backendServiceJwt)));

		String requestBody = requestBodyComponents.stream().collect(Collectors.joining("&"));

		HttpRequest httpRequest = new HttpRequest.Builder(HttpMethod.POST, getEpicBackendServiceConfiguration().getTokenUrl())
				.contentType("application/x-www-form-urlencoded")
				.body(requestBody)
				.build();

		try {
			HttpResponse httpResponse = getHttpClient().execute(httpRequest);
			byte[] rawResponseBody = httpResponse.getBody().orElse(null);
			String responseBody = rawResponseBody == null ? null : new String(rawResponseBody, StandardCharsets.UTF_8);

			if (httpResponse.getStatus() >= 400)
				throw new EpicException(format("Unable to exchange EPIC backend system JWT for access token. HTTP status %d, Response Body:\n%s",
						httpResponse.getStatus(), responseBody == null ? "[no response body]" : responseBody));

			String json = new String(httpResponse.getBody().get(), StandardCharsets.UTF_8);
			Map<String, Object> jsonObject = getGson().fromJson(json, new TypeToken<Map<String, Object>>() {
			}.getType());

			// {
			//  "access_token": "i82fGhXNxmidCt0OdjYttm2x0cOKU1ZbN6Y...",
			//  "token_type": "bearer",
			//  "expires_in": 3600,
			//  "scope": "Patient.read Patient.search"
			// }
			String accessToken = jsonObject.get("access_token").toString();
			String tokenType = jsonObject.get("token_type").toString();
			Integer expiresIn = ((Number) jsonObject.get("expires_in")).intValue();
			String scope = jsonObject.get("scope").toString() == null ? null : jsonObject.get("scope").toString();

			Instant expiresAt = Instant.now().plus(expiresIn, ChronoUnit.SECONDS);

			return new EpicBackendServiceAccessToken.Builder(accessToken, tokenType, expiresAt)
					.scope(scope)
					.build();
		} catch (EpicException e) {
			throw e;
		} catch (Exception e) {
			throw new EpicException(e);
		}
	}

	@Nonnull
	protected EpicBackendServiceConfiguration getEpicBackendServiceConfiguration() {
		return this.epicBackendServiceConfiguration;
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