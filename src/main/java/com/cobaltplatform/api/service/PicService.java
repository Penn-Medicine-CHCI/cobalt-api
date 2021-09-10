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
import com.cobaltplatform.api.integration.pic.PicClient;
import com.cobaltplatform.api.integration.pic.model.PicEpicPatient;
import com.cobaltplatform.api.integration.pic.model.PicEpicPatientNormalized;
import com.cobaltplatform.api.model.api.request.CreateAccountRequest;
import com.cobaltplatform.api.model.api.request.CreatePicMpmAccountRequest;
import com.cobaltplatform.api.model.api.request.CreatePicOrderReportAccountRequest;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AccountSource.AccountSourceId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.model.db.SourceSystem.SourceSystemId;
import com.cobaltplatform.api.util.JsonMapper;
import com.cobaltplatform.api.util.ValidationException;
import com.lokalized.Strings;
import com.pyranid.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class PicService {
	@Nonnull
	private final javax.inject.Provider<AccountService> accountServiceProvider;
	@Nonnull
	private final javax.inject.Provider<ProviderService> providerServiceProvider;
	@Nonnull
	private final javax.inject.Provider<AppointmentService> appointmentServiceProvider;
	@Nonnull
	private final PicClient picClient;
	@Nonnull
	private final Database database;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final JsonMapper jsonMapper;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;

	@Inject
	public PicService(@Nonnull javax.inject.Provider<AccountService> accountServiceProvider,
										@Nonnull javax.inject.Provider<ProviderService> providerServiceProvider,
										@Nonnull javax.inject.Provider<AppointmentService> appointmentServiceProvider,
										@Nonnull Database database,
										@Nonnull PicClient picClient,
										@Nonnull Configuration configuration,
										@Nonnull JsonMapper jsonMapper,
										@Nonnull Strings strings) {
		requireNonNull(accountServiceProvider);
		requireNonNull(providerServiceProvider);
		requireNonNull(appointmentServiceProvider);
		requireNonNull(database);
		requireNonNull(picClient);
		requireNonNull(configuration);
		requireNonNull(jsonMapper);
		requireNonNull(strings);

		this.accountServiceProvider = accountServiceProvider;
		this.providerServiceProvider = providerServiceProvider;
		this.appointmentServiceProvider = appointmentServiceProvider;
		this.picClient = picClient;
		this.database = database;
		this.configuration = configuration;
		this.jsonMapper = jsonMapper;
		this.strings = strings;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	public UUID createOrUpdatePicPatientAccount(@Nonnull CreatePicOrderReportAccountRequest request) {
		requireNonNull(request);

		String uid = trimToNull(request.getUid());
		String firstName = trimToNull(request.getFirstname());
		String lastName = trimToNull(request.getLastName());
		ValidationException validationException = new ValidationException();

		if (uid == null)
			validationException.add(new ValidationException.FieldError("uid", "UID is required."));

		if (validationException.hasErrors())
			throw validationException;

		Account account = getAccountService().findAccountByAccountSourceIdAndSsoId(AccountSourceId.COBALT_SSO, uid).orElse(null);

		if (account != null)
			return account.getAccountId();

		// Store off the whole MPM patient payload as SSO attributes JSONB
		Map<String, Object> ssoAttributes = new HashMap<>();
		ssoAttributes.put("uid", uid);
		ssoAttributes.put("firstName", firstName);
		ssoAttributes.put("lastName", lastName);
		ssoAttributes.put("picOrderImport", true);

		account = getAccountService().findAccountById(getAccountService().createAccount(new CreateAccountRequest() {{
			setAccountSourceId(AccountSourceId.COBALT_SSO);
			setSourceSystemId(SourceSystemId.PIC);
			setInstitutionId(InstitutionId.COBALT);
			setRoleId(RoleId.PATIENT);
			setSsoId(uid);
			setEmailAddress(null);
			setFirstName(firstName);
			setLastName(lastName);
			setPhoneNumber(null);
			setSsoAttributes(ssoAttributes);
			setEpicPatientId(uid);
			setEpicPatientIdType("UID");
		}})).get();

		return account.getAccountId();
	}

	@Nonnull
	public UUID createOrUpdatePicPatientAccount(@Nonnull CreatePicMpmAccountRequest request) {
		requireNonNull(request);

		PicEpicPatient picEpicPatient = request.getPatient();
		PicEpicPatientNormalized picEpicPatientNormalized = null;
		ValidationException validationException = new ValidationException();

		if (picEpicPatient == null) {
			validationException.add(new ValidationException.FieldError("patient", "Patient is required."));
		} else {
			// Extract the relevant information we need to create a Cobalt account from the PIC payload
			picEpicPatientNormalized = getPicClient().extractPicEpicPatientNormalized(picEpicPatient);

			if (picEpicPatientNormalized.getSsoId() == null) {
				// TODO: do we need to fix up accounts when we are not provided with a UID?
				// General idea is that we pass in whatever ID we do have and EPIC gives us back the One True UID and that's what we use
				//
				// ssoId = getEpicClient().determineLatestUIDForPatientIdentifier(picEpicPatientNormalized.getEpicPatientId(), "EXTERNAL").get();
				validationException.add(new ValidationException.FieldError("patient", "Patient does not have a supported unique identifier."));
			}
		}

		if (validationException.hasErrors())
			throw validationException;

		Account account = getAccountService().findAccountByAccountSourceIdAndSsoId(AccountSourceId.COBALT_SSO, picEpicPatientNormalized.getSsoId()).orElse(null);

		if (account != null)
			return account.getAccountId();

		// Store off the whole MPM patient payload as SSO attributes JSONB
		Map<String, Object> ssoAttributes = getJsonMapper().toMap(picEpicPatient);
		PicEpicPatientNormalized pinnedPicEpicPatientNormalized = picEpicPatientNormalized;

		account = getAccountService().findAccountById(getAccountService().createAccount(new CreateAccountRequest() {{
			setAccountSourceId(AccountSourceId.COBALT_SSO);
			setSourceSystemId(SourceSystemId.PIC);
			setInstitutionId(InstitutionId.COBALT);
			setRoleId(RoleId.PATIENT);
			setSsoId(pinnedPicEpicPatientNormalized.getSsoId());
			setEmailAddress(pinnedPicEpicPatientNormalized.getEmailAddress());
			setFirstName(pinnedPicEpicPatientNormalized.getFirstName());
			setLastName(pinnedPicEpicPatientNormalized.getLastName());
			setPhoneNumber(pinnedPicEpicPatientNormalized.getPhoneNumber());
			setSsoAttributes(ssoAttributes);
			setEpicPatientId(pinnedPicEpicPatientNormalized.getEpicPatientId());
			setEpicPatientIdType("UID");
		}})).get();

		return account.getAccountId();
	}

	@Nonnull
	protected AccountService getAccountService() {
		return accountServiceProvider.get();
	}

	@Nonnull
	protected ProviderService getProviderService() {
		return providerServiceProvider.get();
	}

	@Nonnull
	protected AppointmentService getAppointmentService() {
		return appointmentServiceProvider.get();
	}

	@Nonnull
	protected PicClient getPicClient() {
		return picClient;
	}

	@Nonnull
	protected Database getDatabase() {
		return database;
	}

	@Nonnull
	protected JsonMapper getJsonMapper() {
		return jsonMapper;
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return configuration;
	}

	@Nonnull
	protected Strings getStrings() {
		return strings;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}
