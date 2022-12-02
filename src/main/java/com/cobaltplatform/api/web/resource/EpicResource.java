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

package com.cobaltplatform.api.web.resource;

import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.util.CryptoUtility.PublicKeyExponentModulus;
import com.soklet.web.annotation.GET;
import com.soklet.web.annotation.Resource;
import com.soklet.web.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Resource
@Singleton
@ThreadSafe
public class EpicResource {
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Logger logger;

	@Inject
	public EpicResource(@Nonnull Configuration configuration) {
		requireNonNull(configuration);

		this.configuration = configuration;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	@GET("/epic/fhir/jwks")
	public ApiResponse epicFhirJwks(@Nonnull HttpServletResponse httpServletResponse) throws Exception {
		requireNonNull(httpServletResponse);

		// Applications using JSON Web Token (JWT) authentication can provide their public keys to Epic as a JSON Web Key (JWK) Set.
		// Each key in the set should be of type RSA and have the kty, n, e, and kid fields present.

		PublicKeyExponentModulus nonProdPublicKeyExponentModulus = new PublicKeyExponentModulus(getConfiguration().getEpicNonProdKeyPair().getPublic());

		Map<String, Object> nonProdKey = new HashMap<>();
		nonProdKey.put("kty", "RSA");
		nonProdKey.put("kid", getConfiguration().getEpicNonProdKeyId());
		nonProdKey.put("e", nonProdPublicKeyExponentModulus.getExponentInBase64());
		nonProdKey.put("n", nonProdPublicKeyExponentModulus.getModulusInBase64());

		PublicKeyExponentModulus prodPublicKeyExponentModulus = new PublicKeyExponentModulus(getConfiguration().getEpicProdKeyPair().getPublic());

		Map<String, Object> prodKey = new HashMap<>();
		prodKey.put("kty", "RSA");
		prodKey.put("kid", getConfiguration().getEpicProdKeyId());
		prodKey.put("e", prodPublicKeyExponentModulus.getExponentInBase64());
		prodKey.put("n", prodPublicKeyExponentModulus.getModulusInBase64());

		httpServletResponse.setHeader("Cache-Control", "max-age=3600");

		return new ApiResponse(new HashMap<String, Object>() {{
			put("keys", List.of(nonProdKey, prodKey));
		}});
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return this.configuration;
	}

	@Nonnull
	protected Logger getLogger() {
		return this.logger;
	}
}