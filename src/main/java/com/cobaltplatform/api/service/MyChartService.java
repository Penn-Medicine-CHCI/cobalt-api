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

package com.cobaltplatform.api.service;

import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.integration.enterprise.EnterprisePlugin;
import com.cobaltplatform.api.integration.enterprise.EnterprisePluginProvider;
import com.cobaltplatform.api.integration.mychart.MyChartAuthenticator;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.util.Authenticator;
import com.cobaltplatform.api.util.ValidationException;
import com.lokalized.Strings;
import com.pyranid.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class MyChartService {
	@Nonnull
	private static final String SIGNING_TOKEN_NAME;
	@Nonnull
	private static final Long SIGNING_TOKEN_EXPIRATION_IN_SECONDS;
	@Nonnull
	private static final String ENVIRONMENT_CLAIMS_NAME;

	static {
		SIGNING_TOKEN_NAME = "mychart";
		SIGNING_TOKEN_EXPIRATION_IN_SECONDS = 30L * 60L;
		ENVIRONMENT_CLAIMS_NAME = "environment";
	}

	@Nonnull
	private final EnterprisePluginProvider enterprisePluginProvider;
	@Nonnull
	private final Authenticator authenticator;
	@Nonnull
	private final Database database;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;

	@Inject
	public MyChartService(@Nonnull EnterprisePluginProvider enterprisePluginProvider,
												@Nonnull Authenticator authenticator,
												@Nonnull Database database,
												@Nonnull Configuration configuration,
												@Nonnull Strings strings) {
		requireNonNull(enterprisePluginProvider);
		requireNonNull(authenticator);
		requireNonNull(database);
		requireNonNull(configuration);
		requireNonNull(strings);

		this.enterprisePluginProvider = enterprisePluginProvider;
		this.authenticator = authenticator;
		this.database = database;
		this.configuration = configuration;
		this.strings = strings;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	public String generateAuthenticationUrlForInstitutionId(@Nonnull InstitutionId institutionId) {
		requireNonNull(institutionId);
		return generateAuthenticationUrlForInstitutionId(institutionId, null);
	}

	@Nonnull
	public String generateAuthenticationUrlForInstitutionId(@Nonnull InstitutionId institutionId,
																													@Nullable Map<String, Object> claims) {
		requireNonNull(institutionId);

		if (claims == null)
			claims = Collections.emptyMap();

		if (claims.containsKey(getEnvironmentClaimsName()))
			throw new IllegalArgumentException(format("The claims name '%s' is reserved.", getEnvironmentClaimsName()));

		EnterprisePlugin enterprisePlugin = getEnterprisePluginProvider().enterprisePluginForInstitutionId(institutionId);
		MyChartAuthenticator myChartAuthenticator = enterprisePlugin.myChartAuthenticator().orElse(null);

		if (myChartAuthenticator == null)
			throw new ValidationException(getStrings().get("MyChart is not available for this institution."));

		// Use a signing token (basically, a JWT with short lifespan) to encode our OAuth "state".
		// This lets us verify the callback from MyChart is actually coming from a request initiated by us - not someone
		// else forging a request and pretending to be MyChart - and also permits attaching arbitrary data
		// for later use by our callback.
		//
		// By default, we add the environment to the signing token JWT's claims so we can key off of it when we get our OAuth
		// callback.  This is useful for development, e.g. the scenario where we want to use the real MyChart flow
		// but have it redirect back from a dev environment to a local environment instead
		Map<String, Object> finalClaims = new HashMap<>(claims.size() + 1);
		finalClaims.putAll(claims);
		finalClaims.put(getEnvironmentClaimsName(), getConfiguration().getEnvironment());

		String state = getAuthenticator().generateSigningToken(getSigningTokenName(), getSigningTokenExpirationInSeconds(), claims);

		return myChartAuthenticator.generateAuthenticationRedirectUrl(state);
	}

	@Nonnull
	protected String getSigningTokenName() {
		return SIGNING_TOKEN_NAME;
	}

	@Nonnull
	protected Long getSigningTokenExpirationInSeconds() {
		return SIGNING_TOKEN_EXPIRATION_IN_SECONDS;
	}

	@Nonnull
	protected String getEnvironmentClaimsName() {
		return ENVIRONMENT_CLAIMS_NAME;
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
	protected Database getDatabase() {
		return this.database;
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return this.configuration;
	}

	@Nonnull
	protected Strings getStrings() {
		return this.strings;
	}

	@Nonnull
	protected Logger getLogger() {
		return this.logger;
	}
}
