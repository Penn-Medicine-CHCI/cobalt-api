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

package com.cobaltplatform.api.integration.tableau;

import com.cobaltplatform.api.http.DefaultHttpClient;
import com.cobaltplatform.api.http.HttpClient;
import com.cobaltplatform.api.http.HttpMethod;
import com.cobaltplatform.api.http.HttpRequest;
import com.cobaltplatform.api.http.HttpResponse;
import com.cobaltplatform.api.integration.tableau.request.AccessTokenRequest;
import com.google.gson.Gson;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class DefaultTableauClient implements TableauClient {
	@Nonnull
	private final String apiBaseUrl;
	@Nonnull
	private final TableauDirectTrustCredential directTrustCredential;
	@Nonnull
	private final HttpClient httpClient;
	@Nonnull
	private final Gson gson;

	public DefaultTableauClient(@Nonnull String apiBaseUrl,
															@Nonnull TableauDirectTrustCredential directTrustCredential) {
		this(apiBaseUrl, directTrustCredential, null);
	}

	public DefaultTableauClient(@Nonnull String apiBaseUrl,
															@Nonnull TableauDirectTrustCredential directTrustCredential,
															@Nullable HttpClient httpClient) {
		requireNonNull(apiBaseUrl);
		requireNonNull(directTrustCredential);

		this.apiBaseUrl = apiBaseUrl;
		this.directTrustCredential = directTrustCredential;
		this.httpClient = httpClient == null ? new DefaultHttpClient("tableau-client") : httpClient;
		this.gson = new Gson();
	}

	@Nonnull
	@Override
	public String generateDirectTrustJwt(@Nonnull AccessTokenRequest accessTokenRequest) {
		requireNonNull(accessTokenRequest);

		// See https://help.tableau.com/current/online/en-us/connected_apps_direct.htm

		Instant now = Instant.now();
		String alg = "HS256";
		String iss = getDirectTrustCredential().getClientId();
		String kid = getDirectTrustCredential().getSecretId();
		String aud = "tableau";
		Long exp = now.plus(5L, ChronoUnit.MINUTES).toEpochMilli() / 1_000L;
		String jti = UUID.randomUUID().toString();
		String sub = accessTokenRequest.getEmailAddress();
		Long iat = now.toEpochMilli() / 1_000L;
		String email = accessTokenRequest.getEmailAddress();

		Map<String, Object> header = new HashMap<>();
		header.put("alg", alg);
		header.put("kid", kid);
		header.put("iss", iss);

		Map<String, Object> claims = new HashMap<>();
		claims.put("exp", exp);
		claims.put("jti", jti);
		claims.put("aud", aud);
		claims.put("sub", sub);
		claims.put("iat", iat);
		claims.put("email", email);

		if (accessTokenRequest.getScopes().size() > 0)
			claims.put("scp", accessTokenRequest.getScopes());

		for (Entry<String, String> entry : accessTokenRequest.getClaims().entrySet())
			claims.put(entry.getKey(), entry.getValue());

		return Jwts.builder()
				.setHeader(header)
				.setClaims(claims)
				.signWith(SignatureAlgorithm.HS256, getDirectTrustCredential().getSecretValue())
				.compact();
	}

	@Nonnull
	@Override
	public String authenticateUsingDirectTrustJwt(@Nonnull String jwt,
																								@Nonnull String contentUrl) throws TableauException {
		requireNonNull(jwt);
		requireNonNull(contentUrl);

		// See https://help.tableau.com/current/api/rest_api/en-us/REST/rest_api_concepts_auth.htm

		String url = format("%s/api/3.22/auth/signin", getApiBaseUrl());

		String requestBody = getGson().toJson(Map.of(
				"credentials", Map.of(
						"jwt", jwt,
						"site", Map.of(
								"contentUrl", contentUrl
						)
				)
		));

		HttpRequest httpRequest = new HttpRequest.Builder(HttpMethod.POST, url)
				.contentType("application/json")
				.headers(Map.of("Accept", "application/json"))
				.body(requestBody.getBytes(StandardCharsets.UTF_8))
				.build();

		try {
			HttpResponse httpResponse = getHttpClient().execute(httpRequest);
			String responseBodyAsString = httpResponse.getBody().isPresent() ? new String(httpResponse.getBody().get(), StandardCharsets.UTF_8).trim() : null;

			if (httpResponse.getStatus() > 299)
				throw new TableauException(format("Received HTTP %d from Tableau. Response body:\n%s", httpResponse.getStatus(),
						responseBodyAsString == null ? "[none]" : responseBodyAsString));

			try {
				AuthenticateResponse authenticateResponse = getGson().fromJson(responseBodyAsString, AuthenticateResponse.class);
				return authenticateResponse.getCredentials().getToken();
			} catch (Exception e) {
				throw new TableauException(format("Unable to parse response body from Tableau:\n%s",
						responseBodyAsString == null ? "[none]" : responseBodyAsString), e);
			}
		} catch (IOException e) {
			throw new TableauException(e);
		}
	}

	@NotThreadSafe
	protected static class AuthenticateResponse {
		// {
		//    "credentials": {
		//        "site": {
		//            "id": "9a8b7c6d5-e4f3-a2b1-c0d9-e8f7a6b5c4d",
		//            "contentUrl": ""
		//        },
		//        "user": {
		//            "id": "9f9e9d9c-8b8a-8f8e-7d7c-7b7a6f6d6e6d"
		//        },
		//        "token": "HvZMqFFfQQmOM4L-AZNIQA|5fI6T54OPK1Gn1p4w0RtHv6EkojWRTwq|a946d998-2ead-4894-bb50-1054a91dcab3"
		//    }
		// }

		@Nullable
		private Credentials credentials;

		@NotThreadSafe
		protected static class Credentials {
			@Nullable
			private Site site;
			@Nullable
			private User user;
			@Nullable
			private String token;

			@NotThreadSafe
			protected static class Site {
				@Nullable
				private String id;
				@Nullable
				private String contentUrl;

				@Nullable
				public String getId() {
					return this.id;
				}

				public void setId(@Nullable String id) {
					this.id = id;
				}

				@Nullable
				public String getContentUrl() {
					return this.contentUrl;
				}

				public void setContentUrl(@Nullable String contentUrl) {
					this.contentUrl = contentUrl;
				}
			}

			@NotThreadSafe
			protected static class User {
				@Nullable
				private String id;

				@Nullable
				public String getId() {
					return this.id;
				}

				public void setId(@Nullable String id) {
					this.id = id;
				}
			}

			@Nullable
			public Site getSite() {
				return this.site;
			}

			public void setSite(@Nullable Site site) {
				this.site = site;
			}

			@Nullable
			public User getUser() {
				return this.user;
			}

			public void setUser(@Nullable User user) {
				this.user = user;
			}

			@Nullable
			public String getToken() {
				return this.token;
			}

			public void setToken(@Nullable String token) {
				this.token = token;
			}
		}

		@Nullable
		public Credentials getCredentials() {
			return this.credentials;
		}

		public void setCredentials(@Nullable Credentials credentials) {
			this.credentials = credentials;
		}
	}

	@Nonnull
	protected String getApiBaseUrl() {
		return this.apiBaseUrl;
	}

	@Nonnull
	protected TableauDirectTrustCredential getDirectTrustCredential() {
		return this.directTrustCredential;
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