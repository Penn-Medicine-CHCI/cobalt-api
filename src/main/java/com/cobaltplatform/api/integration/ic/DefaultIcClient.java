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

package com.cobaltplatform.api.integration.ic;

import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.http.DefaultHttpClient;
import com.cobaltplatform.api.http.HttpClient;
import com.cobaltplatform.api.http.HttpMethod;
import com.cobaltplatform.api.http.HttpRequest;
import com.cobaltplatform.api.http.HttpResponse;
import com.cobaltplatform.api.integration.ic.model.IcAppointmentCanceledRequest;
import com.cobaltplatform.api.integration.ic.model.IcAppointmentCreatedRequest;
import com.cobaltplatform.api.integration.ic.model.IcEpicPatient;
import com.cobaltplatform.api.util.Authenticator;
import com.cobaltplatform.api.util.CryptoUtility;
import com.cobaltplatform.api.util.JsonMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.UnsupportedJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class DefaultIcClient implements IcClient {
	@Nonnull
	private static final Long SIGNING_TOKEN_EXPIRATION_IN_SECONDS;
	@Nonnull
	private static final Long IC_PUBLIC_KEY_REFRESH_INTERVAL_IN_SECONDS;
	@Nonnull
	private static final Long IC_PUBLIC_KEY_REFRESH_INITIAL_DELAY_IN_SECONDS;

	@Nonnull
	private final Authenticator authenticator;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final HttpClient httpClient;
	@Nonnull
	private final JsonMapper jsonMapper;
	@Nonnull
	private final Object icPublicKeyRefreshLock;
	@Nonnull
	private final Logger logger;

	@Nullable
	private PublicKey icPublicKey;

	static {
		SIGNING_TOKEN_EXPIRATION_IN_SECONDS = 60L * 10L;
		IC_PUBLIC_KEY_REFRESH_INTERVAL_IN_SECONDS = 60L * 5L;
		IC_PUBLIC_KEY_REFRESH_INITIAL_DELAY_IN_SECONDS = 60L;
	}

	public DefaultIcClient(@Nonnull Authenticator authenticator,
												 @Nonnull Configuration configuration) {
		requireNonNull(authenticator);
		requireNonNull(configuration);

		this.authenticator = authenticator;
		this.configuration = configuration;
		this.httpClient = new DefaultHttpClient("IC");
		this.jsonMapper = new JsonMapper();
		this.icPublicKeyRefreshLock = new Object();
		this.logger = LoggerFactory.getLogger(getClass());

		refreshIcPublicKey();

		Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
			refreshIcPublicKey();
		}, getIcPublicKeyRefreshInitialDelayInSeconds(), getIcPublicKeyRefreshIntervalInSeconds(), TimeUnit.SECONDS);
	}

	@Override
	public void notifyOfAppointmentCreation(@Nonnull IcAppointmentCreatedRequest request) throws IcException {
		requireNonNull(request);

		UUID accountId = request.getAccountId();
		UUID appointmentId = request.getAppointmentId();

		if (accountId == null)
			throw new IcException("Account ID is required");

		if (appointmentId == null)
			throw new IcException("Appointment ID is required");

		Map<String, Object> requestBody = new HashMap<>();
		requestBody.put("accountId", request.getAccountId());
		requestBody.put("appointmentId", request.getAppointmentId());

		HttpRequest httpRequest = new HttpRequest.Builder(HttpMethod.POST, format("%s/cobalt/appointment-created", normalizedIcBackendBaseUrl()))
				.body(getJsonMapper().toJson(requestBody))
				.headers(headersForSigningToken())
				.contentType("text/json")
				.build();

		callIc(httpRequest, "appointment created");
	}

	@Override
	public void notifyOfAppointmentCancelation(@Nonnull IcAppointmentCanceledRequest request) throws IcException {
		requireNonNull(request);

		UUID accountId = request.getAccountId();
		UUID appointmentId = request.getAppointmentId();

		if (accountId == null)
			throw new IcException("Account ID is required");

		if (appointmentId == null)
			throw new IcException("Appointment ID is required");

		Map<String, Object> requestBody = new HashMap<>();
		requestBody.put("accountId", request.getAccountId());
		requestBody.put("appointmentId", request.getAppointmentId());

		HttpRequest httpRequest = new HttpRequest.Builder(HttpMethod.POST, format("%s/cobalt/appointment-canceled", normalizedIcBackendBaseUrl()))
				.body(getJsonMapper().toJson(requestBody))
				.headers(headersForSigningToken())
				.contentType("text/json")
				.build();

		callIc(httpRequest, "appointment canceled");
	}

	@Nonnull
	protected Optional<PublicKey> getIcPublicKey() {
		return Optional.ofNullable(this.icPublicKey);
	}

	@Nonnull
	public Boolean refreshIcPublicKey() {
		boolean success = false;

		synchronized (getIcPublicKeyRefreshLock()) {
			try {
				this.icPublicKey = fetchIcPublicKey();
				success = true;
			} catch (Exception e) {
				getLogger().warn("Unable to refresh IC public key. Reason: {}", e.getMessage());
			}
		}

		return success;
	}

	@Nonnull
	protected PublicKey fetchIcPublicKey() {
		String icPublicKeyUrl = format("%s/system/public-key", normalizedIcBackendBaseUrl());

		HttpRequest httpRequest = new HttpRequest.Builder(HttpMethod.GET, icPublicKeyUrl).build();

		try {
			IcPublicKeyResponse icPublicKeyResponse = callIc(httpRequest, "public key", IcPublicKeyResponse.class).orElse(null);
			return CryptoUtility.publicKeyFromStringRepresentation(icPublicKeyResponse.getPublicKey());
		} catch (IcException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	@Nonnull
	protected String normalizedIcBackendBaseUrl() {
		// New IC deployment strategy no longer uses the `/api/ic` suffix
		return getConfiguration().getIcBackendBaseUrl();
//		// In nonlocal envs the "/api/ic" suffix is used
//		return getConfiguration().getEnvironment().equals("local")
//				? getConfiguration().getIcBackendBaseUrl()
//				: format("%s/api/ic", getConfiguration().getIcBackendBaseUrl());
	}

	@Nonnull
	protected Optional<Void> callIc(@Nonnull HttpRequest httpRequest,
																	@Nonnull String description) throws IcException {
		return callIc(httpRequest, description, Void.class);
	}

	@Nonnull
	protected <T> Optional<T> callIc(@Nonnull HttpRequest httpRequest,
																	 @Nonnull String description,
																	 @Nonnull Class<T> responseClass) throws IcException {
		requireNonNull(httpRequest);
		requireNonNull(description);
		requireNonNull(responseClass);

		HttpResponse httpResponse;

		try {
			httpResponse = getHttpClient().execute(httpRequest);
		} catch (IOException e) {
			throw new IcException(format("Unable to fetch %s. Reason: %s", description, e.getMessage()), e);
		}

		if (httpResponse.getStatus() == 404)
			return Optional.empty();

		if (httpResponse.getStatus() >= 400)
			throw new IcException(format("Unable to fetch %s - IC responded with status %d", description, httpResponse.getStatus()));

		if (Void.class.equals(responseClass))
			return Optional.empty();

		String responseBody = httpResponse.getBody().isPresent() ? new String(httpResponse.getBody().get(), StandardCharsets.UTF_8) : null;

		if (responseBody == null)
			throw new IcException(format("Unable to fetch %s - IC responded with an empty response body", description));

		try {
			return Optional.of(getJsonMapper().fromJson(responseBody, responseClass));
		} catch (Exception e) {
			throw new IcException(format("Unable to fetch %s - IC responded with an invalid response body:\n%s", description, responseBody));
		}
	}

	@Nonnull
	protected Map<String, Object> headersForSigningToken() {
		String signingToken = getAuthenticator().generateSigningToken(getSigningTokenExpirationInSeconds());
		return Map.of("X-Cobalt-Signing-Token", signingToken);
	}

	@Override
	@Nonnull
	public IcEpicPatient parseEpicPatientPayload(@Nonnull String rawEpicPatientPayload) throws IcException {
		requireNonNull(rawEpicPatientPayload);

		try {
			return getJsonMapper().fromJson(rawEpicPatientPayload, IcEpicPatient.class);
		} catch (Exception e) {
			throw new IcException("Unable to parse IC patient JSON", e);
		}
	}

	@Nonnull
	@Override
	public Boolean verifyIcSigningToken(@Nonnull String icSigningToken) {
		requireNonNull(icSigningToken);

		icSigningToken = trimToEmpty(icSigningToken);

		// Suppose for some reason we were never able to fetch a IC public key (IC api was down at Cobalt startup time, etc.)
		PublicKey picPublicKey = getIcPublicKey().orElse(null);

		if (picPublicKey == null) {
			getLogger().warn("Cannot verify IC signing token because no IC public key is available. Attempting to refresh IC public key and re-try...");

			boolean refreshed = refreshIcPublicKey();

			if(refreshed) {
				getLogger().warn("Successfully refreshed IC public key.");
				picPublicKey = getIcPublicKey().get();
			} else {
				getLogger().warn("Unable to refresh IC public key, so unable to verify IC signing token.");
				return false;
			}
		}

		try {
			Jws<Claims> claims = Jwts.parserBuilder().setSigningKey(picPublicKey).build().parseClaimsJws(icSigningToken);
			Date expirationAsDate = claims.getBody().getExpiration();

			if (expirationAsDate == null) {
				getLogger().warn(format("IC signing token is missing an expiry: %s", icSigningToken));
			} else {
				Instant expiration = expirationAsDate.toInstant();

				if (Instant.now().isAfter(expiration)) {
					getLogger().warn(format("IC signing token has expired: %s", icSigningToken));
				} else {
					return true;
				}
			}
		} catch (UnsupportedJwtException e) {
			getLogger().warn(format("IC signing token is unsupported: %s", icSigningToken), e);
		} catch (ExpiredJwtException e) {
			getLogger().warn(format("IC signing token has expired: %s", icSigningToken), e);
		} catch (Exception e) {
			getLogger().warn(format("IC signing token could not be processed: %s", icSigningToken), e);
		}

		return false;
	}

	@NotThreadSafe
	protected static class IcPublicKeyResponse {
		// Looks like
		// {
		//   "format":"X.509",
		//   "jcaAlgorithm":"SHA512withRSA",
		//   "publicKey":"MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAxgOvJsn9Nh/pG7bZo9bhv2dMrvXGx4sT2msKvtvW+X/P6cLcaFMi/0DcoChRrbiTWdy139zzu+NHRoUSHqgSvklU7N6uBofNcXNj+XvuQa6hupglw1qSBOIAs6qriIndZBSB2JntonzsEZp0utI1tnbGMCUGVGrLx5vGW/qcu2JJIs8CUfy0a9ORJHksXqP8aag8yLz6x7rLHcrMSONl8MWRxJSLjz7pksuxV/fIsaT7kjTAbCMkfFJsZNdht94ErdDihfpYyV3V32MCZqtRlSCxZ7PytSlc8sjz+vFQU46TBUUc8NDdLPb5EdSZRB4sYhVvWFC82QJTgsPVw+F1yC9ODFrVTLe3pArMV5z+3/qbTklPL73c4aihhWzF94sQlCczwaL6Nabwjk8mUyEBMnuKMqnIlNm11pS0vcYS6R9TLkoTZyaRgN2zVLa1jaxB5xK7sFCVrPaVSQ1I3az2UOUYKESRQwJKAFppPpkRqX3lF/394YSYEhDr1YbJz9v+rn9U7m+MpFGG+o+4vkCP9ZZOo+NGkr8pIJ85SXufIytaU04SbxmwobFPknKamsiTav1u8YUZdzLkbymIohz1z1V4WybVLq9oSpRO6uzXspU/RRAzqVL6oY5Cls+GnzwODHya9ua3gKg9hQ5ED2cVXRD2IZ87feF1cR2fFc5bMYcCAwEAAQ==",
		//   "algorithm":"RSA"
		//}

		@Nullable
		private String format;
		@Nullable
		private String jcaAlgorithm;
		@Nullable
		private String publicKey;
		@Nullable
		private String algorithm;

		@Nullable
		public String getFormat() {
			return format;
		}

		public void setFormat(@Nullable String format) {
			this.format = format;
		}

		@Nullable
		public String getJcaAlgorithm() {
			return jcaAlgorithm;
		}

		public void setJcaAlgorithm(@Nullable String jcaAlgorithm) {
			this.jcaAlgorithm = jcaAlgorithm;
		}

		@Nullable
		public String getPublicKey() {
			return publicKey;
		}

		public void setPublicKey(@Nullable String publicKey) {
			this.publicKey = publicKey;
		}

		@Nullable
		public String getAlgorithm() {
			return algorithm;
		}

		public void setAlgorithm(@Nullable String algorithm) {
			this.algorithm = algorithm;
		}
	}

	@Nonnull
	protected Long getSigningTokenExpirationInSeconds() {
		return SIGNING_TOKEN_EXPIRATION_IN_SECONDS;
	}

	@Nonnull
	protected Long getIcPublicKeyRefreshIntervalInSeconds() {
		return IC_PUBLIC_KEY_REFRESH_INTERVAL_IN_SECONDS;
	}

	@Nonnull
	protected Long getIcPublicKeyRefreshInitialDelayInSeconds() {
		return IC_PUBLIC_KEY_REFRESH_INITIAL_DELAY_IN_SECONDS;
	}

	@Nonnull
	protected Authenticator getAuthenticator() {
		return authenticator;
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return configuration;
	}

	@Nonnull
	protected HttpClient getHttpClient() {
		return httpClient;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}

	@Nonnull
	protected JsonMapper getJsonMapper() {
		return jsonMapper;
	}

	@Nonnull
	protected Object getIcPublicKeyRefreshLock() {
		return icPublicKeyRefreshLock;
	}
}
