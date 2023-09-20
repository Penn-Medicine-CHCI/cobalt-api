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

package com.cobaltplatform.api.integration.enterprise;

import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.integration.epic.DefaultEpicBackendServiceAuthenticator;
import com.cobaltplatform.api.integration.epic.DefaultEpicClient;
import com.cobaltplatform.api.integration.epic.DefaultMyChartAuthenticator;
import com.cobaltplatform.api.integration.epic.EpicBackendServiceAccessToken;
import com.cobaltplatform.api.integration.epic.EpicBackendServiceAuthenticator;
import com.cobaltplatform.api.integration.epic.EpicBackendServiceConfiguration;
import com.cobaltplatform.api.integration.epic.EpicClient;
import com.cobaltplatform.api.integration.epic.EpicConfiguration;
import com.cobaltplatform.api.integration.epic.EpicEmpCredentials;
import com.cobaltplatform.api.integration.epic.MyChartAccessToken;
import com.cobaltplatform.api.integration.epic.MyChartAuthenticator;
import com.cobaltplatform.api.integration.epic.MyChartConfiguration;
import com.cobaltplatform.api.integration.google.DefaultGoogleBigQueryClient;
import com.cobaltplatform.api.integration.google.GoogleBigQueryClient;
import com.cobaltplatform.api.integration.google.MockGoogleBigQueryClient;
import com.cobaltplatform.api.integration.microsoft.DefaultMicrosoftAuthenticator;
import com.cobaltplatform.api.integration.microsoft.MicrosoftAuthenticator;
import com.cobaltplatform.api.model.db.EpicBackendServiceAuthType;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.security.SigningCredentials;
import com.cobaltplatform.api.service.InstitutionService;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Duration;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public abstract class DefaultEnterprisePlugin implements EnterprisePlugin {
	@Nonnull
	private final InstitutionService institutionService;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final LoadingCache<ExpensiveClientCacheKey, Object> expensiveClientCache;

	public DefaultEnterprisePlugin(@Nonnull InstitutionService institutionService,
																 @Nonnull Configuration configuration) {
		requireNonNull(institutionService);
		requireNonNull(configuration);

		this.institutionService = institutionService;
		this.configuration = configuration;
		this.expensiveClientCache = createExpensiveClientCache();
	}

	@Nonnull
	@Override
	public GoogleBigQueryClient googleBigQueryClient() {
		return (GoogleBigQueryClient) getExpensiveClientCache().get(ExpensiveClientCacheKey.GOOGLE_BIGQUERY);
	}

	@Nonnull
	@Override
	public Optional<MicrosoftAuthenticator> microsoftAuthenticator() {
		return (Optional<MicrosoftAuthenticator>) getExpensiveClientCache().get(ExpensiveClientCacheKey.MICROSOFT_AUTHENTICATOR);
	}

	@Nonnull
	@Override
	public Optional<MyChartAuthenticator> myChartAuthenticator() {
		return (Optional<MyChartAuthenticator>) getExpensiveClientCache().get(ExpensiveClientCacheKey.MYCHART_AUTHENTICATOR);
	}

	@Nonnull
	@Override
	public Optional<EpicClient> epicClientForBackendService() {
		return (Optional<EpicClient>) getExpensiveClientCache().get(ExpensiveClientCacheKey.EPIC_CLIENT_FOR_BACKEND_SERVICE);
	}

	@Nonnull
	@Override
	public Optional<EpicClient> epicClientForPatient(@Nonnull MyChartAccessToken myChartAccessToken) {
		requireNonNull(myChartAccessToken);

		Institution institution = getInstitutionService().findInstitutionById(getInstitutionId()).get();

		String clientId = institution.getEpicClientId();
		String baseUrl = institution.getEpicBaseUrl();

		EpicConfiguration epicConfiguration = new EpicConfiguration.Builder(myChartAccessToken, clientId, baseUrl).build();

		return Optional.of(new DefaultEpicClient(epicConfiguration));
	}

	@Nonnull
	protected LoadingCache<ExpensiveClientCacheKey, Object> createExpensiveClientCache() {
		// Keep expensive clients around for a little bit so we don't recreate them constantly.
		// We keep expiration short so changes to configuration/database (for example) can be reflected
		// without requiring a redeploy of the application
		return Caffeine.newBuilder()
				.maximumSize(25)
				.expireAfterWrite(Duration.ofMinutes(5))
				.refreshAfterWrite(Duration.ofMinutes(1))
				.build(expensiveClientCacheKey -> {
					requireNonNull(expensiveClientCacheKey);

					if (expensiveClientCacheKey == ExpensiveClientCacheKey.GOOGLE_BIGQUERY)
						return uncachedGoogleBigQueryClient();
					if (expensiveClientCacheKey == ExpensiveClientCacheKey.MICROSOFT_AUTHENTICATOR)
						return uncachedMicrosoftAuthenticator();
					if (expensiveClientCacheKey == ExpensiveClientCacheKey.MYCHART_AUTHENTICATOR)
						return uncachedMyChartAuthenticator();
					if (expensiveClientCacheKey == ExpensiveClientCacheKey.EPIC_CLIENT_FOR_BACKEND_SERVICE)
						return uncachedEpicClientForBackendService();

					throw new IllegalStateException(format("Unexpected value %s was provided for %s",
							expensiveClientCacheKey.name(), ExpensiveClientCacheKey.class.getSimpleName()));
				});
	}

	@Nonnull
	protected GoogleBigQueryClient uncachedGoogleBigQueryClient() {
		Institution institution = getInstitutionService().findInstitutionById(getInstitutionId()).get();

		String googleBigQueryServiceAccountPrivateKey = institution.getGoogleBigQueryServiceAccountPrivateKey();

		if (googleBigQueryServiceAccountPrivateKey == null)
			return new MockGoogleBigQueryClient();

		return new DefaultGoogleBigQueryClient(googleBigQueryServiceAccountPrivateKey);
	}

	@Nonnull
	protected Optional<MicrosoftAuthenticator> uncachedMicrosoftAuthenticator() {
		Institution institution = getInstitutionService().findInstitutionById(getInstitutionId()).get();

		if (institution.getMicrosoftTenantId() == null || institution.getMicrosoftClientId() == null)
			return Optional.empty();

		return Optional.of(new DefaultMicrosoftAuthenticator(
				institution.getMicrosoftTenantId(),
				institution.getMicrosoftClientId(),
				getConfiguration().getMicrosoftSigningCredentials()));
	}

	@Nonnull
	protected Optional<MyChartAuthenticator> uncachedMyChartAuthenticator() {
		Institution institution = getInstitutionService().findInstitutionById(getInstitutionId()).get();

		MyChartConfiguration myChartConfiguration = new MyChartConfiguration();
		myChartConfiguration.setClientId(institution.getMyChartClientId());
		myChartConfiguration.setScope(institution.getMyChartScope());
		myChartConfiguration.setAud(institution.getMyChartAud());
		myChartConfiguration.setResponseType(institution.getMyChartResponseType());
		myChartConfiguration.setCallbackUrl(institution.getMyChartCallbackUrl());
		myChartConfiguration.setAuthorizeUrl(institution.getEpicAuthorizeUrl());
		myChartConfiguration.setTokenUrl(institution.getEpicTokenUrl());

		return Optional.of(new DefaultMyChartAuthenticator(myChartConfiguration));
	}

	@Nonnull
	protected Optional<EpicClient> uncachedEpicClientForBackendService() {
		Institution institution = getInstitutionService().findInstitutionById(getInstitutionId()).get();
		EpicClient epicClient = null;

		if (institution.getEpicBackendServiceAuthTypeId() == EpicBackendServiceAuthType.EpicBackendServiceAuthTypeId.OAUTH_20) {
			String clientId = institution.getEpicClientId();
			String jwksKeyId = getConfiguration().getEpicCurrentEnvironmentKeyId();
			SigningCredentials signingCredentials = getConfiguration().getEpicCurrentEnvironmentSigningCredentials();
			String tokenUrl = institution.getEpicTokenUrl();
			String jwksUrl = format("%s/epic/fhir/jwks", getConfiguration().getBaseUrl());

			EpicBackendServiceConfiguration epicBackendServiceConfiguration = new EpicBackendServiceConfiguration(clientId, jwksKeyId, signingCredentials, tokenUrl, jwksUrl);
			EpicBackendServiceAuthenticator epicBackendServiceAuthenticator = new DefaultEpicBackendServiceAuthenticator(epicBackendServiceConfiguration);

			EpicBackendServiceAccessToken epicBackendServiceAccessToken = epicBackendServiceAuthenticator.obtainAccessTokenFromBackendServiceJwt();
			EpicConfiguration epicConfiguration = new EpicConfiguration.Builder(epicBackendServiceAccessToken, institution.getEpicClientId(), institution.getEpicBaseUrl())
					.build();

			epicClient = new DefaultEpicClient(epicConfiguration);
		} else if (institution.getEpicBackendServiceAuthTypeId() == EpicBackendServiceAuthType.EpicBackendServiceAuthTypeId.EMP_CREDENTIALS) {
			String clientId = institution.getEpicClientId();
			String userId = institution.getEpicUserId();
			String userIdType = institution.getEpicUserIdType();
			String username = institution.getEpicUsername();
			String password = institution.getEpicPassword();

			EpicEmpCredentials epicEmpCredentials = new EpicEmpCredentials(clientId, userId, userIdType, username, password);
			EpicConfiguration epicConfiguration = new EpicConfiguration.Builder(epicEmpCredentials, institution.getEpicClientId(), institution.getEpicBaseUrl())
					.build();

			epicClient = new DefaultEpicClient(epicConfiguration);
		}

		return Optional.ofNullable(epicClient);
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
	private LoadingCache<ExpensiveClientCacheKey, Object> getExpensiveClientCache() {
		return this.expensiveClientCache;
	}

	enum ExpensiveClientCacheKey {
		GOOGLE_BIGQUERY,
		MICROSOFT_AUTHENTICATOR,
		MYCHART_AUTHENTICATOR,
		EPIC_CLIENT_FOR_BACKEND_SERVICE
	}
}
