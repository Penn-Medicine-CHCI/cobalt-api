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
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class MyChartService {
	@Nonnull
	private final EnterprisePluginProvider enterprisePluginProvider;
	@Nonnull
	private final Authenticator authenticator;
	@Nonnull
	private final MyChartAuthenticator myChartAuthenticator;
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
												@Nonnull MyChartAuthenticator myChartAuthenticator,
												@Nonnull Database database,
												@Nonnull Configuration configuration,
												@Nonnull Strings strings) {
		requireNonNull(enterprisePluginProvider);
		requireNonNull(authenticator);
		requireNonNull(myChartAuthenticator);
		requireNonNull(database);
		requireNonNull(configuration);
		requireNonNull(strings);

		this.enterprisePluginProvider = enterprisePluginProvider;
		this.authenticator = authenticator;
		this.myChartAuthenticator = myChartAuthenticator;
		this.database = database;
		this.configuration = configuration;
		this.strings = strings;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	public String generateAuthenticationUrlForInstitutionId(@Nonnull InstitutionId institutionId) {
		requireNonNull(institutionId);

		EnterprisePlugin enterprisePlugin = getEnterprisePluginProvider().enterprisePluginForInstitutionId(institutionId);
		MyChartAuthenticator myChartAuthenticator = enterprisePlugin.myChartAuthenticator().orElse(null);

		if (myChartAuthenticator == null)
			throw new ValidationException(getStrings().get("MyChart is not available for this institution."));

		// Use a signing token (basically, a JWT with very short lifespan) to encode a "state".
		// This lets us verify the callback from MyChart is actually coming from a request initiated by us and not someone
		// else forging a request and pretending to be MyChart.
		//
		// We add the environment to the signing token JWT's claims so we can key off of it when we get our MyChart
		// callback.  This is useful for development, e.g. the scenario where we want to use the real MyChart flow
		// but have it redirect back from a dev environment to a local environment instead
		Map<String, Object> stateClaims = Map.of("environment", getConfiguration().getEnvironment());
		String state = getAuthenticator().generateSigningToken("mychart", 60L * 30L, stateClaims);

		return getMyChartAuthenticator().generateAuthenticationRedirectUrl(state);
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
	protected MyChartAuthenticator getMyChartAuthenticator() {
		return this.myChartAuthenticator;
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
