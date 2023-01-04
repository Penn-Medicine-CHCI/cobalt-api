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

import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.integration.epic.MyChartAccessToken;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.model.security.AccessTokenClaims;
import com.cobaltplatform.api.model.security.AccessTokenStatus;
import com.cobaltplatform.api.model.security.SigningTokenClaims;
import com.cobaltplatform.api.service.AccountService;
import com.google.gson.Gson;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
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
import javax.inject.Provider;
import javax.inject.Singleton;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
	private static final String ROLE_ID_CLAIM_NAME;
	@Nonnull
	private static final String MY_CHART_ACCESS_TOKEN_CLAIM_NAME;

	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Gson gson;
	@Nonnull
	private final Long missingIssuedAtOffsetInMinutes;
	@Nonnull
	private final Logger logger;
	@Nonnull
	private final Provider<AccountService> accountServiceProvider;

	static {
		DEFAULT_SIGNING_TOKEN_SUBJECT = "COBALT_SYSTEM";
		ROLE_ID_CLAIM_NAME = "roleId";
		MY_CHART_ACCESS_TOKEN_CLAIM_NAME = "myChartAccessToken";
	}

	@Inject
	public Authenticator(@Nonnull Configuration configuration,
											 @Nonnull Provider<AccountService> accountServiceProvider) {
		requireNonNull(configuration);

		this.configuration = configuration;
		this.gson = new Gson();
		this.missingIssuedAtOffsetInMinutes = 10080L;  // Arbitrary; supports legacy access tokens
		this.logger = LoggerFactory.getLogger(getClass());
		this.accountServiceProvider = accountServiceProvider;
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
		return generateAccessToken(accountId, roleId, null);
	}

	@Nonnull
	public String generateAccessToken(@Nonnull UUID accountId,
																		@Nonnull RoleId roleId,
																		@Nullable MyChartAccessToken myChartAccessToken) {
		requireNonNull(accountId);
		requireNonNull(roleId);

		// Use a secret that is part of a keypair.
		// We can share the public key so other systems can verify our access tokens as authentic
		Instant now = Instant.now();

		// Respect expiration of MyChart access token if it exists.
		// Otherwise, use the account's configuration.
		Date expiration = myChartAccessToken == null ?
				Date.from(now.plus(getAccountService().findAccessTokenExpirationInMinutesByAccountId(accountId), MINUTES))
				: Date.from(myChartAccessToken.getExpiresAt());

		return Jwts.builder().setSubject(accountId.toString())
				.setIssuedAt(Date.from(now))
				.setExpiration(expiration)
				.addClaims(new HashMap<String, Object>() {{
					put(getRoleIdClaimName(), roleId);

					if (myChartAccessToken != null)
						put(getMyChartAccessTokenClaimName(), myChartAccessToken.serialize());
				}})
				.signWith(getConfiguration().getSigningCredentials().getPrivateKey(), getSignatureAlgorithm())
				.compact();
	}

	@Nonnull
	public AccessTokenStatus determineAccessTokenStatus(@Nonnull AccessTokenClaims accessTokenClaims) {
		requireNonNull(accessTokenClaims);
		return determineAccessTokenStatus(accessTokenClaims, null);
	}

	@Nonnull
	public AccessTokenStatus determineAccessTokenStatus(@Nonnull AccessTokenClaims accessTokenClaims,
																											@Nullable Instant now) {
		requireNonNull(accessTokenClaims);

		if (now == null)
			now = Instant.now();

		if (now.isAfter(accessTokenClaims.getExpiration()))
			return AccessTokenStatus.FULLY_EXPIRED;

		Instant shortExpirationTimestamp = accessTokenClaims.getIssuedAt().plus(getAccountService()
				.findAccessTokenShortExpirationInMinutesByAccount(accessTokenClaims.getAccountId()), MINUTES);

		if (now.isAfter(shortExpirationTimestamp))
			return AccessTokenStatus.PARTIALLY_EXPIRED;

		return AccessTokenStatus.FULLY_ACTIVE;
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
				.signWith(getConfiguration().getSigningCredentials().getPrivateKey(), getSignatureAlgorithm())
				.compact();
	}

	@Nonnull
	public SigningTokenClaims validateSigningToken(@Nonnull String signingToken) throws SigningTokenValidationException {
		requireNonNull(signingToken);

		try {
			Jws<Claims> claims = Jwts.parserBuilder().setSigningKey(getConfiguration().getSigningCredentials().getX509Certificate().getPublicKey()).build().parseClaimsJws(signingToken);
			Map<String, Object> claimsAsMap = claims.getBody();
			Instant expiration = claims.getBody().getExpiration().toInstant();

			if (expiration.isBefore(Instant.now()))
				throw new SigningTokenValidationException("Signing token has expired");

			return new SigningTokenClaims(claimsAsMap == null ? Collections.emptyMap() : claimsAsMap, expiration);
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
		public SigningTokenValidationException(@Nullable String message) {
			super(message);
		}

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

		// Use public key of keypair to validate claims
		try {
			claims = Jwts.parserBuilder().setSigningKey(getConfiguration().getSigningCredentials().getX509Certificate().getPublicKey()).build().parseClaimsJws(accessToken);
		} catch (UnsupportedJwtException e) {
			getLogger().trace("Very likely this access token is a legacy token, continuing on...", e);
		} catch (ExpiredJwtException e) {
			getLogger().debug("Access token has expired.");
		} catch (SignatureException e) {
			getLogger().debug("Access token signature does not match locally computed signature.");
		} catch (Exception e) {
			getLogger().debug("Access token claims parsing failed.", e);
		}

		if (claims == null)
			return Optional.empty();

		try {
			UUID accountId = UUID.fromString(claims.getBody().getSubject());
			Instant issuedAt = claims.getBody().getIssuedAt() == null ? null : claims.getBody().getIssuedAt().toInstant();

			// For this special case, we don't know issued time, so we fudge it
			if (issuedAt == null)
				issuedAt = Instant.now().minus(getMissingIssuedAtOffsetInMinutes(), MINUTES);

			// See if we have a serialized MyChart access token - if so, rehydrate it
			MyChartAccessToken myChartAccessToken = null;
			String serializedMyChartAccessToken = (String) claims.getBody().get(getMyChartAccessTokenClaimName());

			if (serializedMyChartAccessToken != null)
				myChartAccessToken = MyChartAccessToken.deserialize(serializedMyChartAccessToken);

			return Optional.of(new AccessTokenClaims(accountId, issuedAt, claims.getBody().getExpiration().toInstant(), myChartAccessToken));
		} catch (Exception e) {
			getLogger().debug("Access token claims extraction failed.", e);
			return Optional.empty();
		}
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
	protected String getRoleIdClaimName() {
		return ROLE_ID_CLAIM_NAME;
	}

	@Nonnull
	protected String getMyChartAccessTokenClaimName() {
		return MY_CHART_ACCESS_TOKEN_CLAIM_NAME;
	}

	@Nonnull
	public Long getMissingIssuedAtOffsetInMinutes() {
		return missingIssuedAtOffsetInMinutes;
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

	@Nonnull
	protected AccountService getAccountService() {
		return accountServiceProvider.get();
	}
}