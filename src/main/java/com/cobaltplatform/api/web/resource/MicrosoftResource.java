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
import com.cobaltplatform.api.integration.enterprise.EnterprisePluginProvider;
import com.cobaltplatform.api.integration.microsoft.MicrosoftAccessToken;
import com.cobaltplatform.api.integration.microsoft.MicrosoftAuthenticator;
import com.cobaltplatform.api.integration.microsoft.request.AccessTokenRequest;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.soklet.web.annotation.GET;
import com.soklet.web.annotation.QueryParameter;
import com.soklet.web.annotation.Resource;
import com.soklet.web.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Resource
@Singleton
@ThreadSafe
public class MicrosoftResource {
	@Nonnull
	private final EnterprisePluginProvider enterprisePluginProvider;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Logger logger;

	@Inject
	public MicrosoftResource(@Nonnull EnterprisePluginProvider enterprisePluginProvider,
													 @Nonnull Configuration configuration) {
		requireNonNull(enterprisePluginProvider);
		requireNonNull(configuration);

		this.enterprisePluginProvider = enterprisePluginProvider;
		this.configuration = configuration;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	@GET("/microsoft/oauth/callback")
	public ApiResponse microsoftOAuthCallback(@Nonnull @QueryParameter String code,
																						@Nonnull @QueryParameter String state,
																						@Nonnull @QueryParameter("session_state") Optional<String> sessionState,
																						@Nonnull @QueryParameter Optional<String> error,
																						@Nonnull @QueryParameter("error_description") Optional<String> errorDescription) {
		requireNonNull(code);
		requireNonNull(state);
		requireNonNull(sessionState);
		requireNonNull(error);
		requireNonNull(errorDescription);

		// TODO: drive institution from data encoded in `state` JWT
		MicrosoftAuthenticator microsoftAuthenticator = getEnterprisePluginProvider().enterprisePluginForInstitutionId(InstitutionId.COBALT).microsoftAuthenticator().get();

		String clientAssertion = microsoftAuthenticator.generateClientAssertionJwt();

		// TODO: data-drive
		MicrosoftAccessToken microsoftAccessToken = microsoftAuthenticator.obtainAccessTokenFromCode(new AccessTokenRequest() {{
			setCode(code);
			setRedirectUri("http://localhost:8080/microsoft/oauth/callback");
			setGrantType("authorization_code");
			setClientAssertionType("urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
			setClientAssertion(clientAssertion);
		}});

		return new ApiResponse(new HashMap<String, Object>() {{
			put("microsoftAccessToken", microsoftAccessToken);
		}});
	}

	@Nonnull
	protected EnterprisePluginProvider getEnterprisePluginProvider() {
		return this.enterprisePluginProvider;
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
