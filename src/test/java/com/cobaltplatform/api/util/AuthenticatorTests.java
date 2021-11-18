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
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.model.security.AccessTokenClaims;
import com.cobaltplatform.api.model.security.SigningTokenClaims;
import com.cobaltplatform.api.service.AccountService;
import com.cobaltplatform.api.util.Authenticator.SigningTokenValidationException;
import com.cobaltplatform.api.util.CryptoUtility.KeyFormat;
import io.jsonwebtoken.Jwts;
import org.junit.Test;
import org.testng.Assert;

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
		final UUID ACCOUNT_ID = UUID.fromString("6e54bdb9-b19c-4ca3-9cd0-52cae0c9d2a0");
		final RoleId ROLE_ID = RoleId.ADMINISTRATOR;

		Authenticator authenticator = new Authenticator(new Configuration(), new AccountService());
		String accessToken = authenticator.generateAccessToken(ACCOUNT_ID, ROLE_ID);
		AccessTokenClaims accessTokenClaims = authenticator.validateAccessToken(accessToken).get();

		Assert.assertEquals(ACCOUNT_ID, accessTokenClaims.getAccountId(), "Account ID was not correctly stored in access token claims");
	}

	@Test
	public void testAccessTokenVerificationWithPublicKey() {
		final UUID ACCOUNT_ID = UUID.fromString("6e54bdb9-b19c-4ca3-9cd0-52cae0c9d2a0");
		final RoleId ROLE_ID = RoleId.ADMINISTRATOR;

		Configuration configuration = new Configuration();
		Authenticator authenticator = new Authenticator(configuration ,new AccountService());
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
	}

	@Test
	public void testSigningToken() throws SigningTokenValidationException {
		Configuration configuration = new Configuration();
		Authenticator authenticator = new Authenticator(configuration, new AccountService());

		Map<String, Object> claims = new HashMap<String, Object>() {{
			put("a", 1);
			put("b", 2);
			put("c", "three");
		}};

		String signingToken = authenticator.generateSigningToken(2L, claims);

		SigningTokenClaims validatedClaims = authenticator.validateSigningToken(signingToken);

		Assert.assertEquals(claims.get("a"), validatedClaims.getClaims().get("a"), "Claims were invalid");
		Assert.assertEquals(claims.get("b"), validatedClaims.getClaims().get("b"), "Claims were invalid");
		Assert.assertEquals(claims.get("c"), validatedClaims.getClaims().get("c"), "Claims were invalid");

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
	}
}
