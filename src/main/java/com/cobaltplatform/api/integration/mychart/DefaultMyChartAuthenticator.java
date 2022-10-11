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

package com.cobaltplatform.api.integration.mychart;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.oauth.OAuth20Service;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
public class DefaultMyChartAuthenticator implements MyChartAuthenticator {
	@Nonnull
	private final MyChartConfiguration myChartConfiguration;
	@Nonnull
	private final OAuth20Service oAuth20Service;

	public DefaultMyChartAuthenticator(@Nonnull MyChartConfiguration myChartConfiguration) {
		requireNonNull(myChartConfiguration);

		verifyMyChartConfiguration(myChartConfiguration);

		this.myChartConfiguration = myChartConfiguration;
		this.oAuth20Service = createOAuth20Service(myChartConfiguration);
	}

	protected void verifyMyChartConfiguration(@Nonnull MyChartConfiguration myChartConfiguration) {
		requireNonNull(myChartConfiguration);

		if (trimToNull(myChartConfiguration.getClientId()) == null)
			throw new IllegalStateException("Client ID is required.");

		if (trimToNull(myChartConfiguration.getResponseType()) == null)
			throw new IllegalStateException("Response type is required.");

		if (trimToNull(myChartConfiguration.getScope()) == null)
			throw new IllegalStateException("Scope is required.");

		if (trimToNull(myChartConfiguration.getCallbackUrl()) == null)
			throw new IllegalStateException("Callback URL is required.");

		if (trimToNull(myChartConfiguration.getTokenUrl()) == null)
			throw new IllegalStateException("Token URL is required.");

		if (trimToNull(myChartConfiguration.getAuthorizeUrl()) == null)
			throw new IllegalStateException("Authorize URL is required.");
	}

	@Nonnull
	protected OAuth20Service createOAuth20Service(@Nonnull MyChartConfiguration myChartConfiguration) {
		requireNonNull(myChartConfiguration);

		return new ServiceBuilder(myChartConfiguration.getClientId())
				.debug()
				// .apiSecret(myChartConfiguration.getClientSecret())
				.responseType(myChartConfiguration.getResponseType())
				.defaultScope(myChartConfiguration.getScope())
				.callback(myChartConfiguration.getCallbackUrl())
				.build(new DefaultApi20() {
					@Override
					public String getAccessTokenEndpoint() {
						return myChartConfiguration.getTokenUrl();
					}

					@Override
					protected String getAuthorizationBaseUrl() {
						return myChartConfiguration.getAuthorizeUrl();
					}
				});
	}

	@Override
	@Nonnull
	public String generateAuthorizationRedirectUrl(@Nullable String state,
																								 @Nullable Map<String, String> additionalParameters) {
		return getOAuth20Service().createAuthorizationUrlBuilder()
				.state(state)
				.additionalParams(additionalParameters == null ? Collections.emptyMap() : additionalParameters)
				.build();
	}

	@NotNull
	@Override
	public MyChartAccessToken obtainAccessTokenFromCode(@Nonnull String code) throws MyChartException {
		requireNonNull(code);

		code = code.trim();

		try {
			OAuth2AccessToken oauth2AccessToken = getOAuth20Service().getAccessToken(code);

			return new MyChartAccessToken(oauth2AccessToken.getAccessToken(), oauth2AccessToken.getTokenType(),
					Instant.now().plus(oauth2AccessToken.getExpiresIn(), ChronoUnit.SECONDS),
					oauth2AccessToken.getRefreshToken(), oauth2AccessToken.getScope());
		} catch (Exception e) {
			throw new MyChartException("Unable to acquire access token for code", e);
		}
	}

	@Nonnull
	protected MyChartConfiguration getMyChartConfiguration() {
		return this.myChartConfiguration;
	}

	@Nonnull
	protected OAuth20Service getOAuth20Service() {
		return this.oAuth20Service;
	}
}
