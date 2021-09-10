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

package com.cobaltplatform.api.util;

import com.google.gson.Gson;
import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.model.security.AccessTokenClaims;
import com.cobaltplatform.api.model.security.SigningTokenClaims;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.WeakKeyException;
import org.mindrot.jbcrypt.BCrypt;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.LengthRule;
import org.passay.PasswordData;
import org.passay.PasswordValidator;
import org.passay.Rule;
import org.passay.RuleResult;
import org.passay.WhitespaceRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static java.lang.String.format;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class Authenticator {
	@Nonnull
	private static final String DEFAULT_SIGNING_TOKEN_SUBJECT;

	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Gson gson;
	@Nonnull
	private final Logger logger;

	static {
		DEFAULT_SIGNING_TOKEN_SUBJECT = "COBALT_SYSTEM";
	}

	@Inject
	public Authenticator(@Nonnull Configuration configuration) {
		requireNonNull(configuration);

		this.configuration = configuration;
		this.gson = new Gson();
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	public String hashPassword(@Nonnull String password) {
		requireNonNull(password);
		return BCrypt.hashpw(password, BCrypt.gensalt());
	}

	@Nonnull
	public Boolean verifyPassword(@Nonnull String unencryptedCandidate, @Nonnull String hashedActual) {
		requireNonNull(unencryptedCandidate);
		requireNonNull(hashedActual);

		return BCrypt.checkpw(unencryptedCandidate, hashedActual);
	}

	@Nonnull
	public String generateAccessToken(@Nonnull UUID accountId,
																		@Nonnull RoleId roleId) {
		requireNonNull(accountId);
		requireNonNull(roleId);

		boolean useLegacySigning = false;

		// Leaving here for reference - this was our legacy method to signing JWTs (a secret that was not part of a keypair)
		if (useLegacySigning) {
			return Jwts.builder().setSubject(accountId.toString())
					.setExpiration(
							Date.from(Instant.now().plus(getConfiguration().getAccessTokenExpirationInMinutes(), MINUTES)))
					.signWith(getConfiguration().getSecretKey(), jwtsSignatureAlgorithmForJcaSecretKeyAlgorithm(getConfiguration().getSecretKeyAlgorithm()))
					.compact();
		}

		// Current mechanism is to use a secret that is part of a keypair so we can share the public key so other systems can verify
		// our access tokens as authentic
		return Jwts.builder().setSubject(accountId.toString())
				.setExpiration(
						Date.from(Instant.now().plus(getConfiguration().getAccessTokenExpirationInMinutes(), MINUTES)))
				.addClaims(new HashMap<String, Object>() {{
					put("roleId", roleId);
				}})
				.signWith(getConfiguration().getKeyPair().getPrivate(), getSignatureAlgorithm())
				.compact();
	}

	@Nonnull
	public String generateSigningToken(@Nonnull Long expirationInSeconds) {
		requireNonNull(expirationInSeconds);

		return generateSigningToken(getDefaultSigningTokenSubject(), expirationInSeconds, Collections.emptyMap());
	}

	@Nonnull
	public String generateSigningToken(@Nonnull Long expirationInSeconds,
																		 @Nonnull Map<String, Object> claims) {
		requireNonNull(expirationInSeconds);
		requireNonNull(claims);

		return generateSigningToken(getDefaultSigningTokenSubject(), expirationInSeconds, claims);
	}

	@Nonnull
	public String generateSigningToken(@Nullable String subject,
																		 @Nonnull Long expirationInSeconds,
																		 @Nonnull Map<String, Object> claims) {
		requireNonNull(expirationInSeconds);
		requireNonNull(claims);

		if (subject == null)
			subject = getDefaultSigningTokenSubject();

		Instant now = Instant.now();

		return Jwts.builder()
				.setClaims(claims)
				.claim("nonce", CryptoUtility.generateNonce()) // Add nonce in case clients want to keep track to ensure it is impossible to perform replay attacks, even within expiration window
				.setSubject(subject)
				.setExpiration(Date.from(now.plus(expirationInSeconds, SECONDS)))
				.setIssuedAt(Date.from(now))
				.signWith(getConfiguration().getKeyPair().getPrivate(), getSignatureAlgorithm())
				.compact();
	}

	@Nonnull
	public SigningTokenClaims validateSigningToken(@Nonnull String signingToken) throws SigningTokenValidationException {
		requireNonNull(signingToken);

		try {
			Jws<Claims> claims = Jwts.parserBuilder().setSigningKey(getConfiguration().getKeyPair().getPublic()).build().parseClaimsJws(signingToken);
			Map<String, Object> claimsAsMap = claims.getBody();
			return new SigningTokenClaims(claimsAsMap == null ? Collections.emptyMap() : claimsAsMap, claims.getBody().getExpiration().toInstant());
		} catch (UnsupportedJwtException e) {
			throw new SigningTokenValidationException(e);
		} catch (ExpiredJwtException e) {
			throw new SigningTokenValidationException("Signing token has expired", e);
		} catch (Exception e) {
			throw new SigningTokenValidationException("Unable to process signing token", e);
		}
	}

	@NotThreadSafe
	public static class SigningTokenValidationException extends Exception {
		public SigningTokenValidationException(@Nullable Exception cause) {
			super(cause);
		}

		public SigningTokenValidationException(@Nullable String message,
																					 @Nullable Exception cause) {
			super(message, cause);
		}
	}

	@Nonnull
	protected SignatureAlgorithm getSignatureAlgorithm() {
		return SignatureAlgorithm.RS512;
	}

	@Nonnull
	public String getJcaSignatureAlgorithm() {
		return getSignatureAlgorithm().getJcaName();
	}

	@Nonnull
	public Optional<AccessTokenClaims> validateAccessToken(@Nullable String accessToken) {
		accessToken = trimToNull(accessToken);

		if (accessToken == null)
			return Optional.empty();

		Jws<Claims> claims = null;

		// For current access tokens - use public key of keypair to validate claims
		try {
			claims = Jwts.parserBuilder().setSigningKey(getConfiguration().getKeyPair().getPublic()).build().parseClaimsJws(accessToken);
		} catch (UnsupportedJwtException e) {
			getLogger().trace("Very likely this access token is a legacy token, continuing on...", e);
		} catch (ExpiredJwtException e) {
			getLogger().debug("Access token has expired.");
		} catch (SignatureException e) {
			getLogger().debug("Access token signature does not match locally computed signature.");
		} catch (Exception e) {
			getLogger().debug("Access token claims parsing failed.", e);
		}

		// If current access token logic failed, it might be a legacy access token - use standalone secret key to validate claims
		if (claims == null) {
			try {
				claims = Jwts.parserBuilder().setSigningKey(getConfiguration().getSecretKey()).build().parseClaimsJws(accessToken);
			} catch (ExpiredJwtException e) {
				getLogger().debug("Access token has expired.");
				return Optional.empty();
			} catch (SignatureException e) {
				getLogger().debug("Access token signature does not match locally computed signature.");
				return Optional.empty();
			} catch (WeakKeyException e) {
				// Special case for legacy production keys: new JWT library version we use will report WeakKeyException now (384 bits vs 512)
				// so we treat this as "acceptable" since that's how it has been since the system started.
				// Parse out the JWT components and validate them manually.
				// We can assume JWT library has already performed the overall signature check, and only reason for failing is key strength.
				try {
					String[] accessTokenComponents = accessToken.split("\\.");
					// [0]: {"alg":"HS512"}
					// [1]: {"sub":"eceb624a-baec-436e-a122-a7c8c2894d0e","exp":1649356571}
					JwtComponentOne jwtComponentOne = getGson().fromJson(new String(Base64.getDecoder().decode(accessTokenComponents[0]), StandardCharsets.UTF_8), JwtComponentOne.class);
					JwtComponentTwo jwtComponentTwo = getGson().fromJson(new String(Base64.getDecoder().decode(accessTokenComponents[1]), StandardCharsets.UTF_8), JwtComponentTwo.class);

					if (!Objects.equals(jwtComponentOne.getAlg(), "HS512"))
						throw new Exception(format("JWT component one algorithm '%s' doesn't match expected value '%s'", jwtComponentOne.getAlg(), "HS512"));

					UUID accountId = UUID.fromString(jwtComponentTwo.getSub());
					Instant expiration = Instant.ofEpochSecond(jwtComponentTwo.getExp());

					// Should already be handled above by ExpiredJwtException, but just in case...
					if (expiration.isBefore(Instant.now())) {
						getLogger().debug("Legacy access token has expired.");
						return Optional.empty();
					}

					return Optional.of(new AccessTokenClaims(accountId, expiration));
				} catch (Exception secondaryException) {
					getLogger().debug("Unable to handle JWT weak key workaround.", secondaryException);
					return Optional.empty();
				}
			} catch (Exception e) {
				getLogger().debug("Access token claims parsing failed.", e);
				return Optional.empty();
			}
		}

		try {
			UUID accountId = UUID.fromString(claims.getBody().getSubject());

			return Optional.of(new AccessTokenClaims(accountId, claims.getBody().getExpiration().toInstant()));
		} catch (Exception e) {
			getLogger().debug("Access token claims extraction failed.", e);
			return Optional.empty();
		}
	}

	@NotThreadSafe
	private static class JwtComponentOne {
		@Nullable
		private String alg;

		@Nullable
		public String getAlg() {
			return alg;
		}

		public void setAlg(@Nullable String alg) {
			this.alg = alg;
		}
	}

	@NotThreadSafe
	private static class JwtComponentTwo {
		@Nullable
		private String sub;
		@Nullable
		private Long exp;

		@Nullable
		public String getSub() {
			return sub;
		}

		public void setSub(@Nullable String sub) {
			this.sub = sub;
		}

		@Nullable
		public Long getExp() {
			return exp;
		}

		public void setExp(@Nullable Long exp) {
			this.exp = exp;
		}
	}

	/**
	 * JWTS uses its own signature names, so we need to be able to convert JCA
	 * names to JWTS values.
	 *
	 * @param secretKeyAlgorithm For example, {@code HmacSHA512}
	 */
	@Nonnull
	protected SignatureAlgorithm jwtsSignatureAlgorithmForJcaSecretKeyAlgorithm(@Nonnull String secretKeyAlgorithm) {
		requireNonNull(secretKeyAlgorithm);

		for (SignatureAlgorithm signatureAlgorithm : SignatureAlgorithm.values())
			if (secretKeyAlgorithm.equals(signatureAlgorithm.getJcaName()))
				return signatureAlgorithm;

		throw new IllegalArgumentException(format("No representation of JWTS %s exists for JCA algorithm named '%s'",
				SignatureAlgorithm.class.getSimpleName(), secretKeyAlgorithm));
	}

	@Nonnull
	public Boolean validatePasswordRules(@Nullable String password) {
		List<Rule> rules = List.of(
				new LengthRule(8, 128),
				new WhitespaceRule(),
				new CharacterRule(EnglishCharacterData.Alphabetical, 1),
				new CharacterRule(EnglishCharacterData.Digit, 1)
		);
		PasswordValidator validator = new PasswordValidator(rules);
		RuleResult ruleResult = validator.validate(new PasswordData(password));
		return ruleResult.isValid();
	}

	@Nonnull
	public static String getDefaultSigningTokenSubject() {
		return DEFAULT_SIGNING_TOKEN_SUBJECT;
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return configuration;
	}

	@Nonnull
	protected Gson getGson() {
		return gson;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}