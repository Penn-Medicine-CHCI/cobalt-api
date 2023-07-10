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

package com.cobaltplatform.api.integration.epic.response;

import com.cobaltplatform.api.integration.epic.EpicException;
import com.cobaltplatform.api.integration.epic.MyChartAccessToken;
import com.cobaltplatform.api.integration.epic.MyChartAuthenticator;
import com.cobaltplatform.api.integration.epic.MyChartConfiguration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.cobaltplatform.api.util.WebUtility.urlEncode;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class MockMyChartAuthenticator implements MyChartAuthenticator {
	@Nonnull
	private final MyChartConfiguration myChartConfiguration;

	public MockMyChartAuthenticator(@Nonnull MyChartConfiguration myChartConfiguration) {
		requireNonNull(myChartConfiguration);

		verifyMyChartConfiguration(myChartConfiguration);
		this.myChartConfiguration = myChartConfiguration;
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

	@Override
	@Nonnull
	public String generateAuthenticationRedirectUrl(@Nullable String state) {
		state = trimToNull(state);

		List<String> queryParameters = new ArrayList<>();
		queryParameters.add(format("response_type=%s", urlEncode(getMyChartConfiguration().getResponseType())));
		queryParameters.add(format("client_id=%s", urlEncode(getMyChartConfiguration().getClientId())));
		queryParameters.add(format("redirect_uri=%s", urlEncode(getMyChartConfiguration().getCallbackUrl())));
		queryParameters.add(format("scope=%s", urlEncode(getMyChartConfiguration().getScope())));

		String aud = trimToNull(getMyChartConfiguration().getAud());

		if (aud != null)
			queryParameters.add(format("aud=%s", urlEncode(aud)));

		if (state != null)
			queryParameters.add(format("state=%s", urlEncode(state)));

		return format("%s?%s", getMyChartConfiguration().getAuthorizeUrl(), queryParameters.stream()
				.collect(Collectors.joining("&")));
	}

	@Nonnull
	@Override
	public MyChartAccessToken obtainAccessTokenFromCode(@Nonnull String code,
																											@Nullable String state) throws EpicException {
		requireNonNull(code);
		state = trimToNull(state);

		String accessToken = format("fake-access-token-%s", UUID.randomUUID());
		String tokenType = "Bearer";
		String tokenState = state;
		Integer expiresIn = 3600;
		String scope = "patient/Patient.read launch/patient";

		Instant expiresAt = Instant.now().plus(expiresIn, ChronoUnit.SECONDS);

		return new MyChartAccessToken.Builder(accessToken, tokenType, expiresAt)
				.scope(scope)
				.state(tokenState)
				.metadata(Map.of("patient", format("fake-patient-%s", UUID.randomUUID())))
				.build();
	}

	@Nonnull
	protected MyChartConfiguration getMyChartConfiguration() {
		return this.myChartConfiguration;
	}
}