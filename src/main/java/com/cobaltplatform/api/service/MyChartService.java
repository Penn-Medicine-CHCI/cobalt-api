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
import com.cobaltplatform.api.model.api.request.CreateAccountRequest;
import com.cobaltplatform.api.model.api.request.CreateMyChartAccountRequest;
import com.cobaltplatform.api.model.db.AccountSource.AccountSourceId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.security.SigningTokenClaims;
import com.cobaltplatform.api.util.Authenticator;
import com.cobaltplatform.api.util.Authenticator.SigningTokenValidationException;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.ValidationException.FieldError;
import com.lokalized.Strings;
import com.pyranid.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

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
	@Nonnull
	private static final String INSTITUTION_ID_CLAIMS_NAME;

	@Nonnull
	private final Provider<AccountService> accountServiceProvider;
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

	static {
		SIGNING_TOKEN_NAME = "mychart";
		SIGNING_TOKEN_EXPIRATION_IN_SECONDS = 30L * 60L;
		ENVIRONMENT_CLAIMS_NAME = "environment";
		INSTITUTION_ID_CLAIMS_NAME = "institutionId";
	}

	@Inject
	public MyChartService(@Nonnull Provider<AccountService> accountServiceProvider,
												@Nonnull EnterprisePluginProvider enterprisePluginProvider,
												@Nonnull Authenticator authenticator,
												@Nonnull Database database,
												@Nonnull Configuration configuration,
												@Nonnull Strings strings) {
		requireNonNull(accountServiceProvider);
		requireNonNull(enterprisePluginProvider);
		requireNonNull(authenticator);
		requireNonNull(database);
		requireNonNull(configuration);
		requireNonNull(strings);

		this.accountServiceProvider = accountServiceProvider;
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
		Map<String, Object> finalClaims = new HashMap<>(claims.size() + 2);
		finalClaims.putAll(claims);
		finalClaims.put(getEnvironmentClaimsName(), getConfiguration().getEnvironment());
		finalClaims.put(getInstitutionIdClaimsName(), institutionId.name());

		String state = getAuthenticator().generateSigningToken(getSigningTokenName(), getSigningTokenExpirationInSeconds(), claims);

		return myChartAuthenticator.generateAuthenticationRedirectUrl(state);
	}


	@Nonnull
	public UUID createAccount(@Nonnull CreateMyChartAccountRequest request) {
		requireNonNull(request);

		String code = trimToNull(request.getCode());
		String state = trimToNull(request.getState());
		SigningTokenClaims stateClaims = null;
		InstitutionId institutionId = request.getInstitutionId();
		ValidationException validationException = new ValidationException();

		if (code == null)
			validationException.add(new FieldError("code", "Code is required."));

		if (institutionId == null)
			validationException.add(new FieldError("institutionId", "Institution ID is required."));

		if (state == null) {
			validationException.add(new FieldError("state", "State is required."));
		} else {
			try {
				stateClaims = getAuthenticator().validateSigningToken(state);
			} catch (SigningTokenValidationException e) {
				getLogger().warn("Unable to validate signing token", e);
				validationException.add(new FieldError("state", "Unable to validate your signing token."));
			}

			if (stateClaims != null) {
				try {
					InstitutionId claimsInstitutionId = (InstitutionId) stateClaims.getClaims().get(getInstitutionIdClaimsName());

					if (claimsInstitutionId != institutionId)
						validationException.add(new FieldError("state", "Institution ID in claims doesn't match."));
				} catch (Exception e) {
					getLogger().warn("Unable to extract state claims from signing token", e);
					validationException.add(new FieldError("state", "Unable to extract state claims from your signing token."));
				}
			}
		}

		if (validationException.hasErrors())
			throw validationException;

		return getAccountService().createAccount(new CreateAccountRequest() {{
			setAccountSourceId(AccountSourceId.MYCHART);
			setInstitutionId(institutionId);

			// TODO: rest of the fields
		}});
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
	protected String getInstitutionIdClaimsName() {
		return INSTITUTION_ID_CLAIMS_NAME;
	}

	@Nonnull
	protected AccountService getAccountService() {
		return this.accountServiceProvider.get();
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
