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

package com.cobaltplatform.api.integration.epic;

import com.cobaltplatform.api.model.security.SigningCredentials;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class EpicBackendServiceConfiguration {
	@Nonnull
	private final String clientId;
	@Nonnull
	private final String jwksKeyId;
	@Nonnull
	private final SigningCredentials signingCredentials;
	@Nonnull
	private final String tokenUrl; // e.g. https://EPIC_BASE_URL/ENV-FHIR/oauth2/token";
	@Nonnull
	private final String jwksUrl;

	public EpicBackendServiceConfiguration(@Nonnull String clientId,
																				 @Nonnull String jwksKeyId,
																				 @Nonnull SigningCredentials signingCredentials,
																				 @Nonnull String tokenUrl,
																				 @Nonnull String jwksUrl) {
		requireNonNull(clientId);
		requireNonNull(jwksKeyId);
		requireNonNull(signingCredentials);
		requireNonNull(tokenUrl);
		requireNonNull(jwksUrl);

		this.clientId = clientId;
		this.jwksKeyId = jwksKeyId;
		this.signingCredentials = signingCredentials;
		this.tokenUrl = tokenUrl;
		this.jwksUrl = jwksUrl;
	}

	@Nonnull
	public String getClientId() {
		return this.clientId;
	}

	@Nonnull
	public String getJwksKeyId() {
		return this.jwksKeyId;
	}

	@Nonnull
	public SigningCredentials getSigningCredentials() {
		return this.signingCredentials;
	}

	@Nonnull
	public String getTokenUrl() {
		return this.tokenUrl;
	}

	@Nonnull
	public String getJwksUrl() {
		return this.jwksUrl;
	}
}