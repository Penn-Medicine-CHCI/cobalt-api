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
import com.cobaltplatform.api.integration.jwt.JwksVerifier;
import com.cobaltplatform.api.integration.jwt.JwtVerifier;
import com.cobaltplatform.api.integration.microsoft.request.AccessTokenRequest;
import com.cobaltplatform.api.integration.microsoft.request.AuthenticationRedirectRequest;
import com.cobaltplatform.api.model.security.SigningCredentials;
import com.cobaltplatform.api.util.CryptoUtility;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

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
import java.util.UUID;
import java.util.stream.Collectors;

import static com.cobaltplatform.api.util.WebUtility.urlEncode;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class DefaultMicrosoftAuthenticator implements MicrosoftAuthenticator {
	@Nonnull
	private final String tenantId;
	@Nonnull
	private final String clientId;
	@Nonnull
	private final SigningCredentials signingCredentials;
	@Nonnull
	private final JwtVerifier jwtVerifier;
	@Nonnull
	private final HttpClient httpClient;
	@Nonnull
	private final Gson gson;

	public DefaultMicrosoftAuthenticator(@Nonnull String tenantId,
																			 @Nonnull String clientId,
																			 @Nonnull SigningCredentials signingCredentials) {
		this(tenantId, clientId, signingCredentials, null);
	}

	public DefaultMicrosoftAuthenticator(@Nonnull String tenantId,
																			 @Nonnull String clientId,
																			 @Nonnull SigningCredentials signingCredentials,
																			 @Nullable HttpClient httpClient) {
		requireNonNull(tenantId);
		requireNonNull(clientId);
		requireNonNull(signingCredentials);

		this.tenantId = tenantId;
		this.clientId = clientId;
		this.signingCredentials = signingCredentials;
		this.jwtVerifier = createJwtVerifier();
		this.httpClient = httpClient == null ? new DefaultHttpClient("microsoft-authenticator") : httpClient;
		this.gson = new Gson();
	}

	@Nonnull
	@Override
	public String generateAuthenticationUrl(@Nonnull AuthenticationRedirectRequest request) {
		requireNonNull(request);

		String tenantId = getTenantId();
		String clientId = getClientId();
		String responseType = trimToNull(request.getResponseType());
		String redirectUri = trimToNull(request.getRedirectUri());
		String scope = trimToNull(request.getScope());
		String responseMode = trimToNull(request.getResponseMode());
		String state = trimToNull(request.getState());
		String prompt = trimToNull(request.getPrompt());
		String loginHint = trimToNull(request.getLoginHint());
		String domainHint = trimToNull(request.getDomainHint());
		String codeChallenge = trimToNull(request.getCodeChallenge());
		String codeChallengeMethod = trimToNull(request.getCodeChallengeMethod());

		requireNonNull(tenantId);
		requireNonNull(clientId);
		requireNonNull(responseType);
		requireNonNull(redirectUri);
		requireNonNull(scope);

		String baseUrl = format("https://login.microsoftonline.com/%s/oauth2/v2.0/authorize", tenantId);

		List<String> queryParameters = new ArrayList<>(12);
		queryParameters.add(format("client_id=%s", urlEncode(clientId)));
		queryParameters.add(format("response_type=%s", urlEncode(responseType)));
		queryParameters.add(format("redirect_uri=%s", urlEncode(redirectUri)));
		queryParameters.add(format("scope=%s", urlEncode(scope)));

		if (responseMode != null)
			queryParameters.add(format("response_mode=%s", urlEncode(responseMode)));

		if (state != null)
			queryParameters.add(format("state=%s", urlEncode(state)));

		if (prompt != null)
			queryParameters.add(format("prompt=%s", urlEncode(prompt)));

		if (loginHint != null)
			queryParameters.add(format("login_hint=%s", urlEncode(loginHint)));

		if (domainHint != null)
			queryParameters.add(format("domain_hint=%s", urlEncode(domainHint)));

		if (codeChallenge != null)
			queryParameters.add(format("code_challenge=%s", urlEncode(codeChallenge)));

		if (codeChallengeMethod != null)
			queryParameters.add(format("code_challenge_method=%s", urlEncode(codeChallengeMethod)));

		return format("%s?%s", baseUrl, queryParameters.stream().collect(Collectors.joining("&")));
	}

	@Nonnull
	@Override
	public MicrosoftAccessToken obtainAccessToken(@Nonnull AccessTokenRequest request) throws MicrosoftException {
		requireNonNull(request);

		String tenantId = getTenantId();
		String clientId = getClientId();
		String scope = trimToNull(request.getScope());
		String code = trimToNull(request.getCode());
		String redirectUri = trimToNull(request.getRedirectUri());
		String grantType = trimToNull(request.getGrantType());
		String codeVerifier = trimToNull(request.getCodeVerifier());
		String clientSecret = trimToNull(request.getClientSecret());
		String clientAssertion = trimToNull(request.getClientAssertion());
		String clientAssertionType = trimToNull(request.getClientAssertionType());

		List<String> requestBodyComponents = new ArrayList<>(12);

		if (clientId != null)
			requestBodyComponents.add(format("client_id=%s", urlEncode(clientId)));

		if (scope != null)
			requestBodyComponents.add(format("scope=%s", urlEncode(scope)));

		if (code != null)
			requestBodyComponents.add(format("code=%s", urlEncode(code)));

		if (redirectUri != null)
			requestBodyComponents.add(format("redirect_uri=%s", urlEncode(redirectUri)));

		if (grantType != null)
			requestBodyComponents.add(format("grant_type=%s", urlEncode(grantType)));

		if (codeVerifier != null)
			requestBodyComponents.add(format("code_verifier=%s", urlEncode(codeVerifier)));

		if (clientSecret != null)
			requestBodyComponents.add(format("client_secret=%s", urlEncode(clientSecret)));

		if (clientAssertion != null)
			requestBodyComponents.add(format("client_assertion=%s", urlEncode(clientAssertion)));

		if (clientAssertionType != null)
			requestBodyComponents.add(format("client_assertion_type=%s", urlEncode(clientAssertionType)));

		String requestBody = requestBodyComponents.stream().collect(Collectors.joining("&"));

		String tokenUrl = format("https://login.microsoftonline.com/%s/oauth2/v2.0/token", tenantId);

		HttpRequest httpRequest = new HttpRequest.Builder(HttpMethod.POST, tokenUrl)
				.contentType("application/x-www-form-urlencoded")
				.body(requestBody)
				.build();

		try {
			HttpResponse httpResponse = getHttpClient().execute(httpRequest);

			// TODO: pull error response body data into exception - this is the format:
			//
			// {
			//    "error": "invalid_grant",
			//    "error_description": "AADSTS70008: The provided authorization code or refresh token has expired due to inactivity. Send a new interactive authorization request for this user and resource.\r\nTrace ID: e9128505-c534-4b38-9586-389d52ae8c02\r\nCorrelation ID: 8bdc14d5-d6dc-4cd6-a807-5ab00d8fc7e2\r\nTimestamp: 2022-12-26 15:40:06Z",
			//    "error_codes": [
			//        70008
			//    ],
			//    "timestamp": "2022-12-26 15:40:06Z",
			//    "trace_id": "e9128505-c534-4b38-9586-389d52ae8c02",
			//    "correlation_id": "8bdc14d5-d6dc-4cd6-a807-5ab00d8fc7e2",
			//    "error_uri": "https://login.microsoftonline.com/error?code=70008"
			// }

			if (httpResponse.getStatus() >= 400)
				throw new MicrosoftException(format("Unable to acquire Microsoft access token. HTTP status %d, Response Body:\n%s",
						httpResponse.getStatus(), httpResponse.getBody().isPresent() ? new String(httpResponse.getBody().get(), StandardCharsets.UTF_8) : "[no response body]"));

			String json = new String(httpResponse.getBody().get(), StandardCharsets.UTF_8);
			Map<String, Object> jsonObject = getGson().fromJson(json, new TypeToken<Map<String, Object>>() {
			}.getType());

			// {
			//    "token_type": "Bearer",
			//    "scope": "profile openid email https://graph.microsoft.com/User.Read",
			//    "expires_in": 4472,
			//    "ext_expires_in": 4472,
			//    "access_token": "eyJ0...",
			//    "id_token": "eyJ0..."
			// }
			String accessToken = jsonObject.get("access_token").toString();
			String idToken = jsonObject.get("id_token") == null ? null : jsonObject.get("id_token").toString();
			String tokenType = jsonObject.get("token_type").toString();
			String tokenState = jsonObject.get("state") != null ? jsonObject.get("state").toString() : null;  // In theory should be identical to "state" passed in to this method
			Integer expiresIn = ((Number) jsonObject.get("expires_in")).intValue();
			Integer extExpiresIn = jsonObject.get("ext_expires_in") == null ? null : ((Number) jsonObject.get("ext_expires_in")).intValue();
			String returnedScope = jsonObject.get("scope") == null ? null : jsonObject.get("scope").toString();
			String refreshToken = jsonObject.get("refresh_token") == null ? null : jsonObject.get("refresh_token").toString();

			Instant expiresAt = Instant.now().plus(expiresIn, ChronoUnit.SECONDS);
			Instant extExpiresAt = extExpiresIn == null ? null : Instant.now().plus(extExpiresIn, ChronoUnit.SECONDS);

			return new MicrosoftAccessToken.Builder(accessToken, tokenType, expiresAt)
					.idToken(idToken)
					.extExpiresAt(extExpiresAt)
					.refreshToken(refreshToken)
					.scope(returnedScope)
					.state(tokenState)
					.build();
		} catch (MicrosoftException e) {
			throw e;
		} catch (Exception e) {
			throw new MicrosoftException("Unable to acquire Microsoft access token", e);
		}
	}

	@Nonnull
	@Override
	public String generateClientAssertionJwt() {
		// See https://learn.microsoft.com/en-us/azure/active-directory/develop/active-directory-certificate-credentials

		Instant now = Instant.now();

		// JWT Header

		// Should be RS256
		String alg = "RS256";
		// Should be JWT
		String typ = "JWT";
		// Base64url-encoded SHA-1 thumbprint of the X.509 certificate's DER encoding.
		String x5t = CryptoUtility.sha1ThumbprintBase64UrlRepresentation(getSigningCredentials().getX509Certificate());

		// JWT Claims

		// The "aud" (audience) claim identifies the recipients that the JWT is intended for (here Azure AD) See RFC 7519, Section 4.1.3.
		// In this case, that recipient is the login server (login.microsoftonline.com).
		String aud = format("https://login.microsoftonline.com/%s/oauth2/v2.0/token", getTenantId());
		// The "exp" (expiration time) claim identifies the expiration time on or after which the JWT MUST NOT be accepted for processing.
		// See RFC 7519, Section 4.1.4. This allows the assertion to be used until then, so keep it short - 5-10 minutes
		// after nbf at most. Azure AD does not place restrictions on the exp time currently.
		Long exp = now.plusSeconds(60L).toEpochMilli() / 1_000L;
		// The "iss" (issuer) claim identifies the principal that issued the JWT, in this case your client application. Use the GUID application ID.
		String iss = getClientId();
		// The "jti" (JWT ID) claim provides a unique identifier for the JWT.
		// The identifier value MUST be assigned in a manner that ensures that there is a negligible probability that the same value
		// will be accidentally assigned to a different data object; if the application uses multiple issuers, collisions MUST be
		// prevented among values produced by different issuers as well. The "jti" value is a case-sensitive string. RFC 7519, Section 4.1.7
		String jti = UUID.randomUUID().toString();
		// The "nbf" (not before) claim identifies the time before which the JWT MUST NOT be accepted for processing.
		// RFC 7519, Section 4.1.5. Using the current time is appropriate.
		Long nbf = now.toEpochMilli() / 1_000L;
		// The "sub" (subject) claim identifies the subject of the JWT, in this case also your application. Use the same value as iss.
		String sub = iss;
		// TThe "iat" (issued at) claim identifies the time at which the JWT was issued.
		// This claim can be used to determine the age of the JWT. RFC 7519, Section 4.1.5.
		Long iat = now.toEpochMilli() / 1_000L;

		Map<String, Object> header = new HashMap<>();
		header.put("alg", alg);
		header.put("typ", typ);
		header.put("x5t", x5t);

		Map<String, Object> claims = new HashMap<>();
		claims.put("aud", aud);
		claims.put("exp", exp);
		claims.put("iss", iss);
		claims.put("jti", jti);
		claims.put("nbf", nbf);
		claims.put("sub", sub);
		claims.put("iat", iat);

		return Jwts.builder()
				.setHeader(header)
				.setClaims(claims)
				.signWith(getSigningCredentials().getPrivateKey(), SignatureAlgorithm.RS256)
				.compact();
	}

	@Nonnull
	protected JwtVerifier createJwtVerifier() {
		// Can find this URL by using https://login.microsoftonline.com/{tenantId}/v2.0/.well-known/openid-configuration
		String jwksUrl = format("https://login.microsoftonline.com/%s/discovery/v2.0/keys", getClientId());
		return new JwksVerifier(jwksUrl);
	}

	@Nonnull
	protected String getTenantId() {
		return this.tenantId;
	}

	@Nonnull
	protected String getClientId() {
		return this.clientId;
	}

	@Nonnull
	protected SigningCredentials getSigningCredentials() {
		return this.signingCredentials;
	}

	@Nonnull
	protected HttpClient getHttpClient() {
		return this.httpClient;
	}

	@Nonnull
	protected JwtVerifier getJwtVerifier() {
		return this.jwtVerifier;
	}

	@Nonnull
	protected Gson getGson() {
		return this.gson;
	}
}
