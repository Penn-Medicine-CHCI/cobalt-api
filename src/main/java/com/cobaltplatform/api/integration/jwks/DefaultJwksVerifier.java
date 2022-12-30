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

package com.cobaltplatform.api.integration.jwks;

import com.cobaltplatform.api.http.DefaultHttpClient;
import com.cobaltplatform.api.http.HttpClient;
import com.cobaltplatform.api.http.HttpMethod;
import com.cobaltplatform.api.http.HttpRequest;
import com.cobaltplatform.api.http.HttpResponse;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.gson.Gson;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class DefaultJwksVerifier implements JwksVerifier {
	@Nonnull
	private final String jwksUrl;
	@Nonnull
	private final HttpClient httpClient;
	@Nonnull
	private final Gson gson;
	@Nonnull
	private final LoadingCache<String, Map<String, PublicKey>> publicKeysByIdForJwksUrlCache;

	public DefaultJwksVerifier(@Nonnull String jwksUrl) {
		this(jwksUrl, null, null, null);
	}

	public DefaultJwksVerifier(@Nonnull String jwksUrl,
														 @Nullable Duration refreshDuration,
														 @Nullable Duration evictionDuration) {
		this(jwksUrl, refreshDuration, evictionDuration, null);
	}

	/**
	 * Creates a JWKS-URL-backed JWT verifier with the given caching strategy.
	 * <p>
	 * For further discussion on refresh vs. eviction durations, see https://github.com/ben-manes/caffeine/wiki/Refresh.
	 *
	 * @param jwksUrl          URL where the JWKS data lives, e.g. https://login.microsoftonline.com/common/discovery/v2.0/keys
	 * @param refreshDuration  (optional) how long to wait before asynchronously refreshing the JWKS data when performing
	 *                         a verification operation. The old JWKS data (if any) is still returned while the new data
	 *                         is being refreshed
	 * @param evictionDuration (optional) enforces a hard upper bound on how long we can go without forcing a re-query of
	 *                         the JWKS data. Should be longer than the refresh duration.
	 * @param httpClient       (optional) an HTTP client to use to fetch the JWKS data
	 */
	public DefaultJwksVerifier(@Nonnull String jwksUrl,
														 @Nullable Duration refreshDuration,
														 @Nullable Duration evictionDuration,
														 @Nullable HttpClient httpClient) {
		requireNonNull(jwksUrl);

		this.jwksUrl = jwksUrl;
		this.gson = new Gson();
		this.httpClient = httpClient == null ? new DefaultHttpClient("jwks-verifier") : httpClient;
		this.publicKeysByIdForJwksUrlCache = Caffeine.newBuilder()
				.maximumSize(100)
				.refreshAfterWrite(refreshDuration == null ? Duration.ofMinutes(60 * 12) : refreshDuration)
				.expireAfterWrite(evictionDuration == null ? Duration.ofMinutes(60 * 24) : evictionDuration)
				.build(key -> loadPublicKeysByIdForJwksUrl(key));
	}

	@Override
	public void verifyJwt(@Nullable String jwt) throws JwksVerificationException {
		jwt = trimToNull(jwt);

		if (jwt == null)
			throw new JwksVerificationException("No JWT provided to verify");

		String[] components = jwt.split("\\.");

		if (components.length != 3)
			throw new JwksVerificationException(format("Provided JWT is invalid: %s", jwt));

		String decodedHeader = new String(Base64.getDecoder().decode(components[0]), StandardCharsets.UTF_8);
		String decodedPayload = new String(Base64.getDecoder().decode(components[1]), StandardCharsets.UTF_8);

		Map<String, Object> headerJson = getGson().fromJson(decodedHeader, Map.class);
		String kid = (String) headerJson.get("kid");

		if (kid == null)
			throw new JwksVerificationException(format("No kid property found in header for JWT: %s", jwt));

		Map<String, PublicKey> publicKeysById = getPublicKeysByIdForJwksUrlCache().get(getJwksUrl());
		PublicKey publicKey = publicKeysById.get(kid);

		if (publicKey == null)
			throw new JwksVerificationException(format("Unable to find public key in JWKS URL %s matching kid '%s' for JWT: %s", getJwksUrl(), kid, jwt));

		boolean verified = verifyJwtSignatureWithPublicKey(jwt, publicKey);

		if (!verified)
			throw new JwksVerificationException(format("Unable to verify signature using kid '%s' for JWT: %s", kid, jwt));
	}

	@Nonnull
	protected Boolean verifyJwtSignatureWithPublicKey(@Nonnull String jwt,
																										@Nonnull PublicKey publicKey) throws JwksVerificationException {
		requireNonNull(jwt);
		requireNonNull(publicKey);

		String[] components = jwt.split("\\.");
		String encodedHeader = components[0];
		String encodedPayload = components[1];
		String encodedSignature = components[2];
		byte decodedSignature[] = Base64.getUrlDecoder().decode(encodedSignature);

		// Figure out which Java signature algorithm to use based on value of 'alg'
		String decodedHeader = new String(Base64.getDecoder().decode(components[0]), StandardCharsets.UTF_8);
		Map<String, Object> headerJson = getGson().fromJson(decodedHeader, Map.class);
		String alg = (String) headerJson.get("alg");

		JwtSignatureAlgorithm jwtSignatureAlgorithm = JwtSignatureAlgorithm.valueOf(alg);

		if (jwtSignatureAlgorithm == null)
			throw new JwksVerificationException(format("Unsupported signature algorithm %s for JWT %s", alg, jwt));

		// Don't allow unsafe JWTs
		if (jwtSignatureAlgorithm == JwtSignatureAlgorithm.NONE)
			throw new JwksVerificationException(format("Cannot use unsafe algorithm %s for JWT %s", alg, jwt));

		try {
			Signature signature = Signature.getInstance(jwtSignatureAlgorithm.getJavaAlgorithmName().get());
			signature.initVerify(publicKey);
			signature.update(format("%s.%s", encodedHeader, encodedPayload).getBytes(StandardCharsets.UTF_8));
			return signature.verify(decodedSignature);
		} catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
			throw new JwksVerificationException(format("Unable to perform signature verification for JWT %s", jwt), e);
		}
	}

	@Nonnull
	protected Map<String, PublicKey> loadPublicKeysByIdForJwksUrl(@Nonnull String jwksUrl) {
		requireNonNull(jwksUrl);

		HttpRequest httpRequest = new HttpRequest.Builder(HttpMethod.GET, jwksUrl).build();

		try {
			HttpResponse httpResponse = getHttpClient().execute(httpRequest);

			String responseBody = httpResponse.getBody().isPresent() ? new String(httpResponse.getBody().get(), StandardCharsets.UTF_8) : null;

			if (httpResponse.getStatus() >= 400)
				throw new IOException(format("Received HTTP %d for %s. Response body: %s", httpResponse.getStatus(), jwksUrl, responseBody == null ? "(null)" : responseBody));

			JwksResponse jwksResponse = getGson().fromJson(responseBody, JwksResponse.class);

			if (jwksResponse.getKeys() == null)
				return Collections.emptyMap();

			Map<String, PublicKey> publicKeysById = new HashMap<>();

			for (JwksResponse.Key key : jwksResponse.getKeys())
				publicKeysById.put(key.getKid(), convertX5cToPublicKey(key.getX5c().get(0)));

			return Collections.unmodifiableMap(publicKeysById);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Nonnull
	protected PublicKey convertX5cToPublicKey(@Nonnull String x5c) {
		requireNonNull(x5c);

		x5c = x5c.trim();

		X509Certificate certificate;

		try (ByteArrayInputStream is = new ByteArrayInputStream(Base64.getDecoder().decode(x5c))) {
			CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
			certificate = (X509Certificate) certificateFactory.generateCertificate(is);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (CertificateException e) {
			throw new RuntimeException(e);
		}

		return certificate.getPublicKey();
	}

	@NotThreadSafe
	private static class JwksResponse {
		@Nullable
		private List<Key> keys;

		@Nullable
		public List<Key> getKeys() {
			return this.keys;
		}

		public void setKeys(@Nullable List<Key> keys) {
			this.keys = keys;
		}

		@NotThreadSafe
		private static class Key {
			@Nullable
			private String kty;
			@Nullable
			private String use;
			@Nullable
			private String kid;
			@Nullable
			private String x5t;
			@Nullable
			private String n;
			@Nullable
			private String e;
			@Nullable
			private String issuer;
			@Nullable
			private List<String> x5c;

			@Nullable
			public String getKty() {
				return this.kty;
			}

			public void setKty(@Nullable String kty) {
				this.kty = kty;
			}

			@Nullable
			public String getUse() {
				return this.use;
			}

			public void setUse(@Nullable String use) {
				this.use = use;
			}

			@Nullable
			public String getKid() {
				return this.kid;
			}

			public void setKid(@Nullable String kid) {
				this.kid = kid;
			}

			@Nullable
			public String getX5t() {
				return this.x5t;
			}

			public void setX5t(@Nullable String x5t) {
				this.x5t = x5t;
			}

			@Nullable
			public String getN() {
				return this.n;
			}

			public void setN(@Nullable String n) {
				this.n = n;
			}

			@Nullable
			public String getE() {
				return this.e;
			}

			public void setE(@Nullable String e) {
				this.e = e;
			}

			@Nullable
			public String getIssuer() {
				return this.issuer;
			}

			public void setIssuer(@Nullable String issuer) {
				this.issuer = issuer;
			}

			@Nullable
			public List<String> getX5c() {
				return this.x5c;
			}

			public void setX5c(@Nullable List<String> x5c) {
				this.x5c = x5c;
			}
		}
	}

	/**
	 * See https://github.com/jwtk/jjwt/blob/master/api/src/main/java/io/jsonwebtoken/SignatureAlgorithm.java
	 */
	public enum JwtSignatureAlgorithm {
		NONE(null),
		HS256("HmacSHA256"),
		HS384("HmacSHA384"),
		HS512("HmacSHA512"),
		RS256("SHA256withRSA"),
		RS384("SHA384withRSA"),
		RS512("SHA512withRSA"),
		ES256("SHA256withECDSA"),
		ES384("SHA384withECDSA"),
		ES512("SHA512withECDSA"),
		PS256("RSASSA-PSS"),
		PS384("RSASSA-PSS"),
		PS512("RSASSA-PSS");

		@Nullable
		private final String javaAlgorithmName;

		JwtSignatureAlgorithm(@Nullable String javaAlgorithmName) {
			this.javaAlgorithmName = javaAlgorithmName;
		}

		@Nonnull
		public Optional<String> getJavaAlgorithmName() {
			return Optional.ofNullable(this.javaAlgorithmName);
		}
	}

	@Nonnull
	protected String getJwksUrl() {
		return this.jwksUrl;
	}

	@Nonnull
	protected HttpClient getHttpClient() {
		return this.httpClient;
	}

	@Nonnull
	protected Gson getGson() {
		return this.gson;
	}

	@Nonnull
	protected LoadingCache<String, Map<String, PublicKey>> getPublicKeysByIdForJwksUrlCache() {
		return this.publicKeysByIdForJwksUrlCache;
	}
}
