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
import com.cobaltplatform.api.integration.epic.EpicClient;
import com.cobaltplatform.api.model.api.request.CreateAccountRequest;
import com.cobaltplatform.api.model.api.request.EmailPasswordAccessTokenRequest;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AccountSource.AccountSourceId;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.RawPatientOrder;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.model.db.ScreeningFlowType.ScreeningFlowTypeId;
import com.cobaltplatform.api.model.db.ScreeningQuestion;
import com.cobaltplatform.api.model.db.ScreeningSession;
import com.cobaltplatform.api.model.db.UserExperienceType.UserExperienceTypeId;
import com.cobaltplatform.api.model.service.ScreeningSessionResult;
import com.cobaltplatform.api.service.AccountService;
import com.cobaltplatform.api.service.InstitutionService;
import com.cobaltplatform.api.service.PatientOrderService;
import com.cobaltplatform.api.service.PatientOrderService.CreatePatientOrderSelfReferralMode;
import com.cobaltplatform.api.service.ScreeningService;
import com.cobaltplatform.api.util.Authenticator;
import com.cobaltplatform.api.util.AwsSecretManagerClient;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.ValidationUtility;
import com.google.inject.Provider;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * Shared EASE patient-order, assessment, and local test-account behavior.
 */
@ThreadSafe
public abstract class EaseEnterprisePlugin extends DefaultEnterprisePlugin {
	@Nonnull
	private final ScreeningService screeningService;
	@Nonnull
	private final PatientOrderService patientOrderService;
	@Nonnull
	private final AccountService accountService;
	@Nonnull
	private final Authenticator authenticator;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;

	protected EaseEnterprisePlugin(@Nonnull InstitutionService institutionService,
														 @Nonnull AwsSecretManagerClient awsSecretManagerClient,
														 @Nonnull Configuration configuration,
														 @Nonnull ScreeningService screeningService,
														 @Nonnull PatientOrderService patientOrderService,
														 @Nonnull AccountService accountService,
														 @Nonnull Authenticator authenticator,
														 @Nonnull Strings strings,
														 @Nonnull Provider<CurrentContext> currentContextProvider) {
		super(institutionService, awsSecretManagerClient, configuration);

		requireNonNull(screeningService);
		requireNonNull(patientOrderService);
		requireNonNull(accountService);
		requireNonNull(authenticator);
		requireNonNull(strings);
		requireNonNull(currentContextProvider);

		this.screeningService = screeningService;
		this.patientOrderService = patientOrderService;
		this.accountService = accountService;
		this.authenticator = authenticator;
		this.strings = strings;
		this.currentContextProvider = currentContextProvider;
	}

	@Nonnull
	@Override
	public final PatientOrderCrisisMode patientOrderCrisisMode() {
		return PatientOrderCrisisMode.EMAIL_ONLY;
	}

	@Override
	public void applyCustomProcessingForEmailPasswordAccessTokenRequest(@Nonnull EmailPasswordAccessTokenRequest request) {
		requireNonNull(request);

		Institution institution = getInstitutionService().findInstitutionById(getInstitutionId()).get();
		String emailAddress = request.getEmailAddress() == null ? null : request.getEmailAddress().trim().toLowerCase(Locale.US);
		String password = trimToNull(request.getPassword());

		// Mirror TEAM's local testing convenience. Production patients authenticate through MyChart.
		if (shouldAutoProvisionTestPatient(getConfiguration().isProduction(),
				getCurrentContext().getUserExperienceTypeId().orElse(null), institution.getIntegratedCareEnabled(),
				emailAddress, password)) {
			Account existingAccount = getAccountService().findAccountByEmailAddressAndAccountSourceId(
					emailAddress, AccountSourceId.EMAIL_PASSWORD, getInstitutionId()).orElse(null);

			if (existingAccount == null) {
				getLogger().info("An account with email address '{}' does not exist, creating and self-referring an order...",
						emailAddress);

				CreateAccountRequest createAccountRequest = createTestPatientAccountRequest(
						institution.getInstitutionId(), emailAddress, getAuthenticator().hashPassword(password));
				UUID accountId = getAccountService().createAccount(createAccountRequest);

				getPatientOrderService().createPatientOrderForSelfReferral(accountId, CreatePatientOrderSelfReferralMode.TEST_ORDER);
			}
		}
	}

	static boolean shouldAutoProvisionTestPatient(boolean production,
																					 @Nullable UserExperienceTypeId userExperienceTypeId,
																					 @Nullable Boolean integratedCareEnabled,
																					 @Nullable String emailAddress,
																					 @Nullable String password) {
		return !production
				&& userExperienceTypeId == UserExperienceTypeId.PATIENT
				&& Boolean.TRUE.equals(integratedCareEnabled)
				&& ValidationUtility.isValidEmailAddress(emailAddress)
				&& trimToNull(password) != null;
	}

	@Nonnull
	static CreateAccountRequest createTestPatientAccountRequest(@Nonnull InstitutionId institutionId,
																										 @Nonnull String emailAddress,
																										 @Nonnull String passwordHash) {
		requireNonNull(institutionId);
		requireNonNull(emailAddress);
		requireNonNull(passwordHash);

		CreateAccountRequest request = new CreateAccountRequest();
		request.setRoleId(RoleId.PATIENT);
		request.setInstitutionId(institutionId);
		request.setAccountSourceId(AccountSourceId.EMAIL_PASSWORD);
		request.setEmailAddress(emailAddress);
		request.setPassword(passwordHash);
		request.setTestAccount(true);
		return request;
	}

	@Override
	public void performPatientOrderEncounterWriteback(@Nullable UUID patientOrderId,
																						 @Nullable String encounterCsn) {
		RawPatientOrder patientOrder = getPatientOrderService().findRawPatientOrderById(patientOrderId).orElse(null);

		if (patientOrder == null)
			throw new ValidationException(getStrings().get("Cannot perform encounter writeback; could not locate patient order."));

		if (trimToNull(encounterCsn) == null)
			throw new ValidationException(getStrings().get("Cannot perform encounter writeback; encounter CSN is required."));

		List<ScreeningSession> screeningSessions = getScreeningService()
				.findScreeningSessionsByPatientOrderIdAndScreeningFlowTypeId(patientOrderId, ScreeningFlowTypeId.INTEGRATED_CARE);
		ScreeningSession completedScreeningSession = EaseFlowsheetWriteback
				.findNewestCompletedScreeningSession(screeningSessions);

		if (completedScreeningSession == null)
			throw new ValidationException(getStrings().get("Cannot perform encounter writeback; there is no completed assessment for this order."));

		if (completedScreeningSession.getCompletedAt() == null)
			throw new ValidationException(getStrings().get("Cannot perform encounter writeback; the completed assessment has no completion time."));

		Account patientAccount = patientOrder.getPatientAccountId() == null ? null
				: getAccountService().findAccountById(patientOrder.getPatientAccountId()).orElse(null);
		customizePatientOrderForEncounterWriteback(patientOrder, patientAccount);

		ScreeningSessionResult screeningSessionResult = getScreeningService()
				.findScreeningSessionResult(completedScreeningSession)
				.orElseThrow(() -> new ValidationException(getStrings().get("Cannot perform encounter writeback; assessment results are unavailable.")));
		Institution institution = getInstitutionService().findInstitutionById(getInstitutionId()).get();
		EpicClient epicClient = epicClientForBackendService()
				.orElseThrow(() -> new ValidationException(getStrings().get("Cannot perform encounter writeback; Epic integration is unavailable.")));

		EaseFlowsheetWriteback.writeFlowsheetValues(
				screeningSessionResult,
				screeningQuestionId -> getScreeningService().findScreeningQuestionById(screeningQuestionId)
						.map(ScreeningQuestion::getDisplayOrder)
						.orElse(null),
				getPatientOrderService().findFlowsheetsByInstitutionId(getInstitutionId()),
				patientOrder,
				institution,
				encounterCsn.trim(),
				completedScreeningSession.getCompletedAt(),
				epicClient
		);
	}

	protected void customizePatientOrderForEncounterWriteback(@Nonnull RawPatientOrder patientOrder,
																			 @Nullable Account patientAccount) {
		requireNonNull(patientOrder);
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
	protected Strings getStrings() {
		return this.strings;
	}

	@Nonnull
	protected CurrentContext getCurrentContext() {
		return this.currentContextProvider.get();
	}
}
