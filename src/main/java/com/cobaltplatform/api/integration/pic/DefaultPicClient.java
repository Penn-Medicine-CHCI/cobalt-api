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

package com.cobaltplatform.api.integration.pic;

import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.http.DefaultHttpClient;
import com.cobaltplatform.api.http.HttpClient;
import com.cobaltplatform.api.http.HttpMethod;
import com.cobaltplatform.api.http.HttpRequest;
import com.cobaltplatform.api.http.HttpResponse;
import com.cobaltplatform.api.integration.pic.model.PicAppointmentCanceledRequest;
import com.cobaltplatform.api.integration.pic.model.PicAppointmentCreatedRequest;
import com.cobaltplatform.api.integration.pic.model.PicEpicPatient;
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
public class DefaultPicClient implements PicClient {
	@Nonnull
	private static final Long SIGNING_TOKEN_EXPIRATION_IN_SECONDS;
	@Nonnull
	private static final Long PIC_PUBLIC_KEY_REFRESH_INTERVAL_IN_SECONDS;
	@Nonnull
	private static final Long PIC_PUBLIC_KEY_REFRESH_INITIAL_DELAY_IN_SECONDS;

	@Nonnull
	private final Authenticator authenticator;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final HttpClient httpClient;
	@Nonnull
	private final JsonMapper jsonMapper;
	@Nonnull
	private final Object picPublicKeyRefreshLock;
	@Nonnull
	private final Logger logger;

	@Nullable
	private PublicKey picPublicKey;

	static {
		SIGNING_TOKEN_EXPIRATION_IN_SECONDS = 60L * 10L;
		PIC_PUBLIC_KEY_REFRESH_INTERVAL_IN_SECONDS = 60L * 5L;
		PIC_PUBLIC_KEY_REFRESH_INITIAL_DELAY_IN_SECONDS = 60L;
	}

	public DefaultPicClient(@Nonnull Authenticator authenticator,
													@Nonnull Configuration configuration) {
		requireNonNull(authenticator);
		requireNonNull(configuration);

		this.authenticator = authenticator;
		this.configuration = configuration;
		this.httpClient = new DefaultHttpClient("PIC");
		this.jsonMapper = new JsonMapper();
		this.picPublicKeyRefreshLock = new Object();
		this.logger = LoggerFactory.getLogger(getClass());

		refreshPicPublicKey();

		Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
			refreshPicPublicKey();
		}, getPicPublicKeyRefreshInitialDelayInSeconds(), getPicPublicKeyRefreshIntervalInSeconds(), TimeUnit.SECONDS);
	}

	@Override
	public void notifyOfAppointmentCreation(@Nonnull PicAppointmentCreatedRequest request) throws PicException {
		requireNonNull(request);

		UUID accountId = request.getAccountId();
		UUID appointmentId = request.getAppointmentId();

		if (accountId == null)
			throw new PicException("Account ID is required");

		if (appointmentId == null)
			throw new PicException("Appointment ID is required");

		Map<String, Object> requestBody = new HashMap<>();
		requestBody.put("accountId", request.getAccountId());
		requestBody.put("appointmentId", request.getAppointmentId());

		HttpRequest httpRequest = new HttpRequest.Builder(HttpMethod.POST, format("%s/cobalt/appointment-created", normalizedPicBackendBaseUrl()))
				.body(getJsonMapper().toJson(requestBody))
				.headers(headersForSigningToken())
				.contentType("text/json")
				.build();

		callPic(httpRequest, "appointment created");
	}

	@Override
	public void notifyOfAppointmentCancelation(@Nonnull PicAppointmentCanceledRequest request) throws PicException {
		requireNonNull(request);

		UUID accountId = request.getAccountId();
		UUID appointmentId = request.getAppointmentId();

		if (accountId == null)
			throw new PicException("Account ID is required");

		if (appointmentId == null)
			throw new PicException("Appointment ID is required");

		Map<String, Object> requestBody = new HashMap<>();
		requestBody.put("accountId", request.getAccountId());
		requestBody.put("appointmentId", request.getAppointmentId());

		HttpRequest httpRequest = new HttpRequest.Builder(HttpMethod.POST, format("%s/cobalt/appointment-canceled", normalizedPicBackendBaseUrl()))
				.body(getJsonMapper().toJson(requestBody))
				.headers(headersForSigningToken())
				.contentType("text/json")
				.build();

		callPic(httpRequest, "appointment canceled");
	}

	@Nonnull
	protected Optional<PublicKey> getPicPublicKey() {
		return Optional.ofNullable(this.picPublicKey);
	}

	@Nonnull
	public Boolean refreshPicPublicKey() {
		boolean success = false;

		synchronized (getPicPublicKeyRefreshLock()) {
			try {
				this.picPublicKey = fetchPicPublicKey();
				success = true;
			} catch (Exception e) {
				getLogger().warn("Unable to refresh PIC public key. Reason: {}", e.getMessage());
			}
		}

		return success;
	}

	@Nonnull
	protected PublicKey fetchPicPublicKey() {
		String picPublicKeyUrl = format("%s/system/public-key", normalizedPicBackendBaseUrl());

		HttpRequest httpRequest = new HttpRequest.Builder(HttpMethod.GET, picPublicKeyUrl).build();

		try {
			PicPublicKeyResponse picPublicKeyResponse = callPic(httpRequest, "public key", PicPublicKeyResponse.class).orElse(null);
			return CryptoUtility.publicKeyFromStringRepresentation(picPublicKeyResponse.getPublicKey());
		} catch (PicException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	@Nonnull
	protected String normalizedPicBackendBaseUrl() {
		// New PIC deployment strategy no longer uses the `/api/pic` suffix
		return getConfiguration().getPicBackendBaseUrl();
//		// In nonlocal envs the "/api/pic" suffix is used
//		return getConfiguration().getEnvironment().equals("local")
//				? getConfiguration().getPicBackendBaseUrl()
//				: format("%s/api/pic", getConfiguration().getPicBackendBaseUrl());
	}

	@Nonnull
	protected Optional<Void> callPic(@Nonnull HttpRequest httpRequest,
																	 @Nonnull String description) throws PicException {
		return callPic(httpRequest, description, Void.class);
	}

	@Nonnull
	protected <T> Optional<T> callPic(@Nonnull HttpRequest httpRequest,
																		@Nonnull String description,
																		@Nonnull Class<T> responseClass) throws PicException {
		requireNonNull(httpRequest);
		requireNonNull(description);
		requireNonNull(responseClass);

		HttpResponse httpResponse;

		try {
			httpResponse = getHttpClient().execute(httpRequest);
		} catch (IOException e) {
			throw new PicException(format("Unable to fetch %s. Reason: %s", description, e.getMessage()), e);
		}

		if (httpResponse.getStatus() == 404)
			return Optional.empty();

		if (httpResponse.getStatus() >= 400)
			throw new PicException(format("Unable to fetch %s - PIC responded with status %d", description, httpResponse.getStatus()));

		if (Void.class.equals(responseClass))
			return Optional.empty();

		String responseBody = httpResponse.getBody().isPresent() ? new String(httpResponse.getBody().get(), StandardCharsets.UTF_8) : null;

		if (responseBody == null)
			throw new PicException(format("Unable to fetch %s - PIC responded with an empty response body", description));

		try {
			return Optional.of(getJsonMapper().fromJson(responseBody, responseClass));
		} catch (Exception e) {
			throw new PicException(format("Unable to fetch %s - PIC responded with an invalid response body:\n%s", description, responseBody));
		}
	}

	@Nonnull
	protected Map<String, Object> headersForSigningToken() {
		String signingToken = getAuthenticator().generateSigningToken(getSigningTokenExpirationInSeconds());
		return Map.of("X-Cobalt-Signing-Token", signingToken);
	}

	@Override
	@Nonnull
	public PicEpicPatient parseEpicPatientPayload(@Nonnull String rawEpicPatientPayload) throws PicException {
		requireNonNull(rawEpicPatientPayload);

		try {
			return getJsonMapper().fromJson(rawEpicPatientPayload, PicEpicPatient.class);
		} catch (Exception e) {
			throw new PicException("Unable to parse PIC patient JSON", e);
		}
	}

	@Nonnull
	@Override
	public Boolean verifyPicSigningToken(@Nonnull String picSigningToken) {
		requireNonNull(picSigningToken);

		picSigningToken = trimToEmpty(picSigningToken);

		// Suppose for some reason we were never able to fetch a PIC public key (PIC api was down at Cobalt startup time, etc.)
		PublicKey picPublicKey = getPicPublicKey().orElse(null);

		if (picPublicKey == null) {
			getLogger().warn("Cannot verify PIC signing token because no PIC public key is available. Attempting to refresh PIC public key and re-try...");

			boolean refreshed = refreshPicPublicKey();

			if(refreshed) {
				getLogger().warn("Successfully refreshed PIC public key.");
				picPublicKey = getPicPublicKey().get();
			} else {
				getLogger().warn("Unable to refresh PIC public key, so unable to verify PIC signing token.");
				return false;
			}
		}

		try {
			Jws<Claims> claims = Jwts.parserBuilder().setSigningKey(picPublicKey).build().parseClaimsJws(picSigningToken);
			Date expirationAsDate = claims.getBody().getExpiration();

			if (expirationAsDate == null) {
				getLogger().warn(format("PIC signing token is missing an expiry: %s", picSigningToken));
			} else {
				Instant expiration = expirationAsDate.toInstant();

				if (Instant.now().isAfter(expiration)) {
					getLogger().warn(format("PIC signing token has expired: %s", picSigningToken));
				} else {
					return true;
				}
			}
		} catch (UnsupportedJwtException e) {
			getLogger().warn(format("PIC signing token is unsupported: %s", picSigningToken), e);
		} catch (ExpiredJwtException e) {
			getLogger().warn(format("PIC signing token has expired: %s", picSigningToken), e);
		} catch (Exception e) {
			getLogger().warn(format("PIC signing token could not be processed: %s", picSigningToken), e);
		}

		return false;
	}

	@NotThreadSafe
	protected static class PicPublicKeyResponse {
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
	protected Long getPicPublicKeyRefreshIntervalInSeconds() {
		return PIC_PUBLIC_KEY_REFRESH_INTERVAL_IN_SECONDS;
	}

	@Nonnull
	protected Long getPicPublicKeyRefreshInitialDelayInSeconds() {
		return PIC_PUBLIC_KEY_REFRESH_INITIAL_DELAY_IN_SECONDS;
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
	protected Object getPicPublicKeyRefreshLock() {
		return picPublicKeyRefreshLock;
	}
}
