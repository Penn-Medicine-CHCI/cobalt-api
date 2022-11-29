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
import com.cobaltplatform.api.model.db.EpicBackendServiceAuthType.EpicBackendServiceAuthTypeId;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.service.InstitutionService;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.security.KeyPair;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class CobaltIcEnterprisePlugin implements EnterprisePlugin {
	@Nonnull
	private final InstitutionService institutionService;
	@Nonnull
	private final Configuration configuration;

	@Inject
	public CobaltIcEnterprisePlugin(@Nonnull InstitutionService institutionService,
																	@Nonnull Configuration configuration) {
		requireNonNull(institutionService);
		requireNonNull(configuration);

		this.institutionService = institutionService;
		this.configuration = configuration;
	}

	@Nonnull
	@Override
	public InstitutionId getInstitutionId() {
		return InstitutionId.COBALT_IC;
	}

	@NotNull
	@Override
	public Optional<EpicClient> epicClientForPatient(@Nonnull MyChartAccessToken myChartAccessToken) {
		requireNonNull(myChartAccessToken);

		Institution institution = getInstitutionService().findInstitutionById(getInstitutionId()).get();

		String clientId = institution.getEpicClientId();
		String baseUrl = institution.getEpicBaseUrl();

		EpicConfiguration epicConfiguration = new EpicConfiguration.Builder(myChartAccessToken, clientId, baseUrl).build();

		return Optional.of(new DefaultEpicClient(epicConfiguration));
	}

	@NotNull
	@Override
	public Optional<EpicClient> epicClientForBackendService() {
		Institution institution = getInstitutionService().findInstitutionById(getInstitutionId()).get();
		EpicClient epicClient = null;

		if (institution.getEpicBackendServiceAuthTypeId() == EpicBackendServiceAuthTypeId.OAUTH_20) {
			String clientId = institution.getEpicClientId();
			String jwksKeyId = getConfiguration().getEpicCurrentEnvironmentKeyId();
			KeyPair keyPair = getConfiguration().getEpicCurrentEnvironmentKeyPair();
			String tokenUrl = institution.getEpicTokenUrl();
			String jwksUrl = format("%s/epic/fhir/jwks", getConfiguration().getBaseUrl());

			EpicBackendServiceConfiguration epicBackendServiceConfiguration = new EpicBackendServiceConfiguration(clientId, jwksKeyId, keyPair, tokenUrl, jwksUrl);
			EpicBackendServiceAuthenticator epicBackendServiceAuthenticator = new DefaultEpicBackendServiceAuthenticator(epicBackendServiceConfiguration);

			// Real implementations would cache this off, this is just an example for our fake institution
			EpicBackendServiceAccessToken epicBackendServiceAccessToken = epicBackendServiceAuthenticator.obtainAccessTokenFromBackendServiceJwt();
			EpicConfiguration epicConfiguration = new EpicConfiguration.Builder(epicBackendServiceAccessToken, institution.getEpicClientId(), institution.getEpicBaseUrl())
					.build();

			epicClient = new DefaultEpicClient(epicConfiguration);
		} else if (institution.getEpicBackendServiceAuthTypeId() == EpicBackendServiceAuthTypeId.EMP_CREDENTIALS) {
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
	@Override
	public Optional<MyChartAuthenticator> myChartAuthenticator() {
		Institution institution = getInstitutionService().findInstitutionById(getInstitutionId()).get();

		MyChartConfiguration myChartConfiguration = new MyChartConfiguration();
		myChartConfiguration.setClientId(institution.getMyChartClientId());
		myChartConfiguration.setScope(institution.getMyChartScope());
		myChartConfiguration.setResponseType(institution.getMyChartResponseType());
		myChartConfiguration.setCallbackUrl(institution.getMyChartCallbackUrl());
		myChartConfiguration.setAuthorizeUrl(institution.getEpicAuthorizeUrl());
		myChartConfiguration.setTokenUrl(institution.getEpicTokenUrl());

		return Optional.of(new DefaultMyChartAuthenticator(myChartConfiguration));
	}

	@Nonnull
	protected InstitutionService getInstitutionService() {
		return this.institutionService;
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return this.configuration;
	}
}
