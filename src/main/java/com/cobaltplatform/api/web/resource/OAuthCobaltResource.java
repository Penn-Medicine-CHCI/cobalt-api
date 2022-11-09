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
import com.cobaltplatform.api.integration.mychart.MyChartException;
import com.cobaltplatform.api.model.api.request.ObtainMyChartAccessTokenRequest;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.service.MyChartAccessTokenWithClaims;
import com.cobaltplatform.api.service.InstitutionService;
import com.cobaltplatform.api.service.MyChartService;
import com.lokalized.Strings;
import com.soklet.web.annotation.GET;
import com.soklet.web.annotation.QueryParameter;
import com.soklet.web.annotation.Resource;
import com.soklet.web.response.RedirectResponse;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
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
	private final MyChartService myChartService;
	@Nonnull
	private final InstitutionService institutionService;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Strings strings;

	@Inject
	public OAuthCobaltResource(@Nonnull MyChartService myChartService,
														 @Nonnull InstitutionService institutionService,
														 @Nonnull Configuration configuration,
														 @Nonnull Strings strings) {
		requireNonNull(myChartService);
		requireNonNull(institutionService);
		requireNonNull(configuration);
		requireNonNull(strings);

		this.myChartService = myChartService;
		this.institutionService = institutionService;
		this.configuration = configuration;
		this.strings = strings;
	}

	// TODO: this should move to Enterprise
	@GET("/oauth/pic")
	public RedirectResponse oauthAssertion(@Nonnull @QueryParameter String code,
																				 @Nonnull @QueryParameter String state) throws MyChartException {
		requireNonNull(code);
		requireNonNull(state);

		Map<String, Object> claims = getMyChartService().extractAndValidateClaimsFromMyChartState(InstitutionId.COBALT_IC, state);
		String claimsEnvironment = (String) claims.get("environment");

		if (claimsEnvironment == null)
			throw new IllegalStateException("MyChart token Claims are missing 'environment' value");

		// Special handling to send callbacks down to local env if we're deployed nonlocally.
		// Some MyChart setups don't support localhost/127.0.0.1 callbacks...
		if (Objects.equals("local", claimsEnvironment)
				&& !Objects.equals("local", getConfiguration().getEnvironment()))
			return new RedirectResponse(format("http://localhost:8080/oauth/pic?code=%s&state=%s", urlEncode(code), urlEncode(state)), RedirectResponse.Type.TEMPORARY);

		MyChartAccessTokenWithClaims myChartAccessTokenWithClaims = getMyChartService().obtainMyChartAccessToken(new ObtainMyChartAccessTokenRequest() {{
			setCode(code);
			setState(state);
			setInstitutionId(InstitutionId.COBALT_IC);
		}});

		String webappBaseUrl = getInstitutionService().findWebappBaseUrlByInstitutionId(InstitutionId.COBALT_IC).get();

		return new RedirectResponse(format("%s/TBD?code=%s&state=%s", webappBaseUrl, code, state));
	}

	@Nonnull
	protected MyChartService getMyChartService() {
		return this.myChartService;
	}

	@Nonnull
	protected InstitutionService getInstitutionService() {
		return this.institutionService;
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
