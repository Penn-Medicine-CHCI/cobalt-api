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
import com.cobaltplatform.api.integration.epic.EpicClient;
import com.cobaltplatform.api.integration.epic.MockEpicClient;
import com.cobaltplatform.api.integration.epic.MyChartAccessToken;
import com.cobaltplatform.api.integration.epic.MyChartAuthenticator;
import com.cobaltplatform.api.integration.epic.MyChartConfiguration;
import com.cobaltplatform.api.integration.epic.response.MockMyChartAuthenticator;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.service.InstitutionService;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class CobaltFhirEnterprisePlugin implements EnterprisePlugin {
	@Nonnull
	private final InstitutionService institutionService;
	@Nonnull
	private final Configuration configuration;

	@Inject
	public CobaltFhirEnterprisePlugin(@Nonnull InstitutionService institutionService,
																		@Nonnull Configuration configuration) {
		requireNonNull(institutionService);
		requireNonNull(configuration);

		this.institutionService = institutionService;
		this.configuration = configuration;
	}

	@Nonnull
	@Override
	public InstitutionId getInstitutionId() {
		return InstitutionId.COBALT_FHIR;
	}

	@Nonnull
	@Override
	public Optional<EpicClient> epicClientForPatient(@Nonnull MyChartAccessToken myChartAccessToken) {
		requireNonNull(myChartAccessToken);
		return Optional.of(new MockEpicClient());
	}

	@Nonnull
	@Override
	public Optional<EpicClient> epicClientForBackendService() {
		return Optional.of(new MockEpicClient());
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

		return Optional.of(new MockMyChartAuthenticator(myChartConfiguration));
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
