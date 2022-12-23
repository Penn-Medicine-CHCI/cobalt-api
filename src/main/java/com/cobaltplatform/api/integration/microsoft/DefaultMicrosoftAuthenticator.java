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

package com.cobaltplatform.api.integration.microsoft;

import com.cobaltplatform.api.http.DefaultHttpClient;
import com.cobaltplatform.api.http.HttpClient;
import com.cobaltplatform.api.integration.microsoft.request.AccessTokenRequest;
import com.cobaltplatform.api.integration.microsoft.request.AuthenticationRedirectRequest;
import com.google.gson.Gson;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.cobaltplatform.api.util.WebUtility.urlEncode;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class DefaultMicrosoftAuthenticator implements MicrosoftAuthenticator {
	@Nonnull
	private final HttpClient httpClient;
	@Nonnull
	private final Gson gson;

	public DefaultMicrosoftAuthenticator() {
		this(new DefaultHttpClient("microsoft-authenticator"));
	}

	public DefaultMicrosoftAuthenticator(@Nonnull HttpClient httpClient) {
		requireNonNull(httpClient);

		this.httpClient = httpClient;
		this.gson = new Gson();
	}

	@Nonnull
	@Override
	public String generateAuthenticationRedirectUrl(@Nonnull AuthenticationRedirectRequest request) {
		requireNonNull(request);

		String tenant = trimToNull(request.getTenant());
		String clientId = trimToNull(request.getClientId());
		String responseType = trimToNull(request.getResponseType());
		String redirectUri = trimToNull(request.getRedirectUri());
		String scope = trimToNull(request.getScope());
		String responseMode = trimToNull(request.getResponseMode());
		String state = trimToNull(request.getState());
		String prompt = trimToNull(request.getPrompt());
		String loginHint = trimToNull(request.getLoginHint());
		String domainHint = trimToNull(request.getDomainHint());
		String codeChallenge = trimToNull(request.getCodeChallenge());
		String codeChallengeMethod = trimToNull(request.getCodeChallengeMethod());

		requireNonNull(tenant);
		requireNonNull(clientId);
		requireNonNull(responseType);
		requireNonNull(redirectUri);
		requireNonNull(scope);

		String baseUrl = format("https://login.microsoftonline.com/%s/oauth2/v2.0/authorize", tenant);

		List<String> queryParameters = new ArrayList<>(12);
		queryParameters.add(format("client_id=%s", urlEncode(clientId)));
		queryParameters.add(format("response_type=%s", urlEncode(responseType)));
		queryParameters.add(format("redirect_uri=%s", urlEncode(redirectUri)));
		queryParameters.add(format("scope=%s", urlEncode(scope)));

		if (responseMode != null)
			queryParameters.add(format("response_mode=%s", urlEncode(responseMode)));

		if (state != null)
			queryParameters.add(format("state=%s", urlEncode(state)));

		if (prompt != null)
			queryParameters.add(format("prompt=%s", urlEncode(prompt)));

		if (loginHint != null)
			queryParameters.add(format("login_hint=%s", urlEncode(loginHint)));

		if (domainHint != null)
			queryParameters.add(format("domain_hint=%s", urlEncode(domainHint)));

		if (codeChallenge != null)
			queryParameters.add(format("code_challenge=%s", urlEncode(codeChallenge)));

		if (codeChallengeMethod != null)
			queryParameters.add(format("code_challenge_method=%s", urlEncode(codeChallengeMethod)));

		return format("%s?%s", baseUrl, queryParameters.stream().collect(Collectors.joining("&")));
	}

	@Nonnull
	@Override
	public MicrosoftAccessToken obtainAccessTokenFromCode(@Nonnull AccessTokenRequest request) throws MicrosoftException {
		requireNonNull(request);
		throw new UnsupportedOperationException();
	}

	@Nonnull
	protected HttpClient getHttpClient() {
		return this.httpClient;
	}

	@Nonnull
	protected Gson getGson() {
		return this.gson;
	}
}
