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
import com.cobaltplatform.api.IntegrationTestExecutor;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.model.security.AccessTokenClaims;
import com.cobaltplatform.api.model.security.SigningTokenClaims;
import com.cobaltplatform.api.service.AccountService;
import com.cobaltplatform.api.util.Authenticator.SigningTokenValidationException;
import com.cobaltplatform.api.util.CryptoUtility.KeyFormat;
import io.jsonwebtoken.Jwts;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.concurrent.ThreadSafe;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class AuthenticatorTests {
	@Test
	public void testAccessTokenClaims() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			final UUID ACCOUNT_ID = UUID.fromString("6e54bdb9-b19c-4ca3-9cd0-52cae0c9d2a0");
			final RoleId ROLE_ID = RoleId.ADMINISTRATOR;

			Configuration configuration = new Configuration();
			Authenticator authenticator = new Authenticator(configuration, app.getInjector().getProvider(AccountService.class));
			String accessToken = authenticator.generateAccessToken(ACCOUNT_ID, ROLE_ID);
			AccessTokenClaims accessTokenClaims = authenticator.validateAccessToken(accessToken).get();

			Assert.assertEquals("Account ID was not correctly stored in access token claims", accessTokenClaims.getAccountId(), ACCOUNT_ID);
		});
	}

	@Test
	public void testAccessTokenVerificationWithPublicKey() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			Authenticator authenticator = app.getInjector().getInstance(Authenticator.class);

			final UUID ACCOUNT_ID = UUID.fromString("6e54bdb9-b19c-4ca3-9cd0-52cae0c9d2a0");
			final RoleId ROLE_ID = RoleId.ADMINISTRATOR;

			Configuration configuration = new Configuration();
			String accessToken = authenticator.generateAccessToken(ACCOUNT_ID, ROLE_ID);

			// This should succeed
			Jwts.parserBuilder().setSigningKey(configuration.getKeyPair().getPublic()).build().parseClaimsJws(accessToken);

			// This should fail, since it's some random key generated at runtime and not the key we used to sign the JWT with
			try {
				Jwts.parserBuilder().setSigningKey(CryptoUtility.generateKeyPair().getPublic()).build().parseClaimsJws(accessToken);
			} catch (Exception e) {
				// Expected behavior
			}

			// Ensure we can serialize and deserialize the public key and it still works for verification
			String publicKeyAsString = CryptoUtility.stringRepresentation(configuration.getKeyPair().getPublic(), KeyFormat.BASE64);
			PublicKey publicKey = CryptoUtility.publicKeyFromStringRepresentation(publicKeyAsString);

			Jwts.parserBuilder().setSigningKey(publicKey).build().parseClaimsJws(accessToken);
		});
	}

	@Test
	public void testSigningToken() throws SigningTokenValidationException {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {

			Configuration configuration = new Configuration();
			Authenticator authenticator = new Authenticator(configuration, app.getInjector().getProvider(AccountService.class));

			Map<String, Object> claims = new HashMap<String, Object>() {{
				put("a", 1);
				put("b", 2);
				put("c", "three");
			}};

			String signingToken = authenticator.generateSigningToken(2L, claims);

			SigningTokenClaims validatedClaims = authenticator.validateSigningToken(signingToken);

			Assert.assertEquals("Claims were invalid", validatedClaims.getClaims().get("a"), claims.get("a"));
			Assert.assertEquals("Claims were invalid", validatedClaims.getClaims().get("b"), claims.get("b"));
			Assert.assertEquals("Claims were invalid", validatedClaims.getClaims().get("c"), claims.get("c"));

			// Wait long enough for signing token to expire...
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				// Ignore
			}

			// ...then attempt to validate it again.
			try {
				authenticator.validateSigningToken(signingToken);
			} catch (SigningTokenValidationException e) {
				// Validation should fail now because the expiration time has passed
			}
		});
	}
}
