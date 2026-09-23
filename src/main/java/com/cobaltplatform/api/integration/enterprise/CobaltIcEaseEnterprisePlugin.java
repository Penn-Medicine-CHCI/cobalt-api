/*
 * Copyright 2021 The University of Pennsylvania and Penn Medicine
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
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.service.AccountService;
import com.cobaltplatform.api.service.InstitutionService;
import com.cobaltplatform.api.service.PatientOrderService;
import com.cobaltplatform.api.service.ScreeningService;
import com.cobaltplatform.api.util.Authenticator;
import com.cobaltplatform.api.util.AwsSecretManagerClient;
import com.google.inject.Provider;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * EASE clinic implementation for the open-source mirror environment.
 */
@Singleton
@ThreadSafe
public class CobaltIcEaseEnterprisePlugin extends EaseEnterprisePlugin {
	@Inject
	public CobaltIcEaseEnterprisePlugin(@Nonnull InstitutionService institutionService,
																			 @Nonnull AwsSecretManagerClient awsSecretManagerClient,
																			 @Nonnull Configuration configuration,
																			 @Nonnull ScreeningService screeningService,
																			 @Nonnull PatientOrderService patientOrderService,
														 @Nonnull AccountService accountService,
														 @Nonnull Authenticator authenticator,
														 @Nonnull Strings strings,
														 @Nonnull Provider<CurrentContext> currentContextProvider) {
		super(institutionService, awsSecretManagerClient, configuration, screeningService, patientOrderService,
				accountService, authenticator, strings, currentContextProvider);
	}

	@Nonnull
	@Override
	public InstitutionId getInstitutionId() {
		return InstitutionId.COBALT_IC_EASE;
	}
}
