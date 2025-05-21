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
import com.cobaltplatform.api.error.ErrorReporter;
import com.cobaltplatform.api.model.api.request.CreateAccountRequest;
import com.cobaltplatform.api.model.api.request.EmailPasswordAccessTokenRequest;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AccountSource.AccountSourceId;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.service.AccountService;
import com.cobaltplatform.api.service.InstitutionService;
import com.cobaltplatform.api.service.PatientOrderService;
import com.cobaltplatform.api.service.ScreeningService;
import com.cobaltplatform.api.util.Authenticator;
import com.cobaltplatform.api.util.AwsSecretManagerClient;
import com.cobaltplatform.api.util.ValidationUtility;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.UUID;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class CobaltIcSelfReferralEnterprisePlugin extends DefaultEnterprisePlugin {
	@Nonnull
	private final ScreeningService screeningService;
	@Nonnull
	private final PatientOrderService patientOrderService;
	@Nonnull
	private final AccountService accountService;
	@Nonnull
	private final Authenticator authenticator;
	@Nonnull
	private final ErrorReporter errorReporter;
	@Nonnull
	private final Strings strings;

	@Inject
	public CobaltIcSelfReferralEnterprisePlugin(@Nonnull InstitutionService institutionService,
																							@Nonnull AwsSecretManagerClient awsSecretManagerClient,
																							@Nonnull Configuration configuration,
																							@Nonnull ScreeningService screeningService,
																							@Nonnull PatientOrderService patientOrderService,
																							@Nonnull AccountService accountService,
																							@Nonnull Authenticator authenticator,
																							@Nonnull ErrorReporter errorReporter,
																							@Nonnull Strings strings) {
		super(institutionService, awsSecretManagerClient, configuration);

		requireNonNull(screeningService);
		requireNonNull(patientOrderService);
		requireNonNull(accountService);
		requireNonNull(authenticator);
		requireNonNull(strings);

		this.screeningService = screeningService;
		this.patientOrderService = patientOrderService;
		this.accountService = accountService;
		this.authenticator = authenticator;
		this.errorReporter = errorReporter;
		this.strings = strings;
	}

	@Nonnull
	@Override
	public InstitutionId getInstitutionId() {
		return InstitutionId.COBALT_IC_SELF_REFERRAL;
	}

	@Override
	public void applyCustomProcessingForEmailPasswordAccessTokenRequest(@Nonnull EmailPasswordAccessTokenRequest request) {
		requireNonNull(request);

		Institution institution = getInstitutionService().findInstitutionById(getInstitutionId()).get();

		// Special handling for the IC self-referring institution in non-production environments:
		// If we authenticate with email/password, actually just create an account if one doesn't exist and create an order for it.
		// This makes testing simple - you can spin up any account you like.
		if (!getConfiguration().isProduction()
				&& ValidationUtility.isValidEmailAddress(request.getEmailAddress())
				&& trimToNull(request.getPassword()) != null
				&& institution.getIntegratedCareEnabled()) {

			// See if an account already exists for this email.  If not, we create one and self-refer an order for it.
			Account existingAccount = getAccountService().findAccountByEmailAddressAndAccountSourceId(request.getEmailAddress(), AccountSourceId.EMAIL_PASSWORD, getInstitutionId()).orElse(null);

			if (existingAccount == null) {
				getLogger().info("An account with email address '{}' does not exist, creating and self-referring an order...", request.getEmailAddress());

				UUID accountId = getAccountService().createAccount(new CreateAccountRequest() {{
					setRoleId(RoleId.PATIENT);
					setInstitutionId(institution.getInstitutionId());
					setAccountSourceId(AccountSourceId.EMAIL_PASSWORD);
					setEmailAddress(request.getEmailAddress());
					setPassword(getAuthenticator().hashPassword(request.getPassword()));
				}});

				getPatientOrderService().createPatientOrderForSelfReferral(accountId);
			}
		}
	}

	@Nonnull
	protected ScreeningService getScreeningService() {
		return this.screeningService;
	}

	@Nonnull
	protected PatientOrderService getPatientOrderService() {
		return this.patientOrderService;
	}

	@Nonnull
	protected AccountService getAccountService() {
		return this.accountService;
	}

	@Nonnull
	protected Authenticator getAuthenticator() {
		return this.authenticator;
	}

	@Nonnull
	protected ErrorReporter getErrorReporter() {
		return this.errorReporter;
	}

	@Nonnull
	protected Strings getStrings() {
		return this.strings;
	}
}
