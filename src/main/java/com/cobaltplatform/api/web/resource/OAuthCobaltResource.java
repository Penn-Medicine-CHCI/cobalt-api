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
import com.cobaltplatform.api.integration.enterprise.EnterprisePlugin;
import com.cobaltplatform.api.integration.enterprise.EnterprisePluginProvider;
import com.cobaltplatform.api.integration.mychart.MyChartAccessToken;
import com.cobaltplatform.api.integration.mychart.MyChartAuthenticator;
import com.cobaltplatform.api.integration.mychart.MyChartException;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.security.SigningTokenClaims;
import com.cobaltplatform.api.util.Authenticator;
import com.cobaltplatform.api.util.Authenticator.SigningTokenValidationException;
import com.cobaltplatform.api.util.ValidationException;
import com.lokalized.Strings;
import com.soklet.web.annotation.GET;
import com.soklet.web.annotation.QueryParameter;
import com.soklet.web.annotation.Resource;
import com.soklet.web.exception.AuthorizationException;
import com.soklet.web.response.ApiResponse;
import com.soklet.web.response.RedirectResponse;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Objects;

import static com.cobaltplatform.api.util.WebUtility.urlEncode;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify LLC.
 */
@Resource
@Singleton
@ThreadSafe
public class OAuthCobaltResource {
	@Nonnull
	private final EnterprisePluginProvider enterprisePluginProvider;
	@Nonnull
	private final Authenticator authenticator;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Strings strings;

	@Inject
	public OAuthCobaltResource(@Nonnull EnterprisePluginProvider enterprisePluginProvider,
														 @Nonnull Authenticator authenticator,
														 @Nonnull Configuration configuration,
														 @Nonnull Strings strings) {
		requireNonNull(enterprisePluginProvider);
		requireNonNull(authenticator);
		requireNonNull(configuration);
		requireNonNull(strings);

		this.enterprisePluginProvider = enterprisePluginProvider;
		this.authenticator = authenticator;
		this.configuration = configuration;
		this.strings = strings;
	}

	// TODO: this should move to Enterprise
	@GET("/oauth/pic")
	public Object oauthAssertion(@Nonnull @QueryParameter String code,
															 @Nonnull @QueryParameter String state) throws MyChartException {
		requireNonNull(code);
		requireNonNull(state);

		EnterprisePlugin enterprisePlugin = getEnterprisePluginProvider().enterprisePluginForInstitutionId(InstitutionId.COBALT_IC);
		MyChartAuthenticator myChartAuthenticator = enterprisePlugin.myChartAuthenticator().orElse(null);

		if (myChartAuthenticator == null)
			throw new ValidationException(getStrings().get("MyChart is not available for this institution."));

		SigningTokenClaims signingTokenClaims;

		try {
			signingTokenClaims = getAuthenticator().validateSigningToken(state);
		} catch (SigningTokenValidationException e) {
			throw new AuthorizationException(e);
		}

		MyChartAccessToken accessToken = myChartAuthenticator.obtainAccessTokenFromCode(code, state);
		String environment = (String) signingTokenClaims.getClaims().get("environment");

		// Special handling to send callbacks down to local env if we're deployed nonlocally.
		// Some MyChart setups don't support localhost/127.0.0.1 callbacks...
		if (Objects.equals("local", environment)
				&& !Objects.equals("local", getConfiguration().getEnvironment()))
			return new RedirectResponse(format("http://localhost:8080/oauth/pic?code=%s&state=%s", urlEncode(code), urlEncode(state)), RedirectResponse.Type.TEMPORARY);
		
		return new ApiResponse(new HashMap<String, Object>() {{
			put("accessToken", accessToken);
		}});
	}

	@Nonnull
	protected EnterprisePluginProvider getEnterprisePluginProvider() {
		return this.enterprisePluginProvider;
	}

	@Nonnull
	protected Authenticator getAuthenticator() {
		return this.authenticator;
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return this.configuration;
	}

	@Nonnull
	protected Strings getStrings() {
		return this.strings;
	}
}
