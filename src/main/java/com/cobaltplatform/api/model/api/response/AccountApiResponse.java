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

package com.cobaltplatform.api.model.api.response;

import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.api.response.AddressApiResponse.AddressApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AccountSource.AccountSourceId;
import com.cobaltplatform.api.model.db.Address;
import com.cobaltplatform.api.model.db.BetaStatus.BetaStatusId;
import com.cobaltplatform.api.model.db.BirthSex.BirthSexId;
import com.cobaltplatform.api.model.db.Ethnicity.EthnicityId;
import com.cobaltplatform.api.model.db.GenderIdentity.GenderIdentityId;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.LoginDestination.LoginDestinationId;
import com.cobaltplatform.api.model.db.Race.RaceId;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.model.db.SourceSystem.SourceSystemId;
import com.cobaltplatform.api.model.security.AccountCapabilities;
import com.cobaltplatform.api.model.service.AccountCapabilityFlags;
import com.cobaltplatform.api.service.AccountService;
import com.cobaltplatform.api.service.AddressService;
import com.cobaltplatform.api.service.AuthorizationService;
import com.cobaltplatform.api.service.InstitutionService;
import com.cobaltplatform.api.service.SessionService;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Provider;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.FormatStyle;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class AccountApiResponse {
	@Nonnull
	private final UUID accountId;
	@Nonnull
	private final RoleId roleId;
	@Nonnull
	private final InstitutionId institutionId;
	@Nonnull
	private final AccountSourceId accountSourceId;
	@Nonnull
	private final SourceSystemId sourceSystemId;
	@Nonnull
	private final BetaStatusId betaStatusId;
	@Nullable
	private final UUID providerId;
	@Nullable
	private final String username; // Generally only for study-specific accounts
	@Nullable
	private final String firstName;
	@Nullable
	private final String lastName;
	@Nullable
	private final String displayName;
	@Nullable
	private final String emailAddress;
	@Nullable
	private final String phoneNumber;
	@Nullable
	private final String phoneNumberDescription;
	@Nonnull
	private final ZoneId timeZone;
	@Nonnull
	private final Locale locale;
	@Nonnull
	private final String languageCode;
	@Nonnull
	private final String countryCode;
	@Nonnull
	private final Boolean consentFormAccepted;
	@Nullable
	private final Instant consentFormAcceptedDate;
	@Nullable
	private final String consentFormAcceptedDateDescription;
	@Nullable
	private final GenderIdentityId genderIdentityId;
	@Nullable
	private final EthnicityId ethnicityId;
	@Nullable
	private final BirthSexId birthSexId;
	@Nullable
	private final RaceId raceId;
	@Nullable
	private final LocalDate birthdate;
	@Nullable
	private final String birthdateDescription;
	@Nonnull
	private final Instant created;
	@Nonnull
	private final String createdDescription;
	@Nonnull
	private final LocalDate createdDate;
	@Nonnull
	private final String createdDateDescription;
	@Nullable
	private final Instant lastUpdated;
	@Nullable
	private final String lastUpdatedDescription;
	@Nullable
	private final LoginDestinationId loginDestinationId;
	@Nullable
	private final AddressApiResponse address;
	@Nullable
	@Deprecated // in favor of accountCapabilityFlags
	private final Map<InstitutionId, AccountCapabilities> capabilities;
	@Nullable
	private final UUID institutionLocationId;
	@Nullable
	private final Boolean promptedForInstitutionLocation;
	@Nullable
	private final AccountCapabilityFlags accountCapabilityFlags;
	@Nullable
	private final String epicPatientMrn;
	@Nullable
	private final String epicPatientFhirId;
	@Nullable
	private final Boolean testAccount;
	@Nullable
	private final Boolean passwordResetRequired;
	@Nullable
	private final UUID passwordResetToken;

	public enum AccountApiResponseSupplement {
		EVERYTHING,
		PRIVATE_DETAILS,
		CAPABILITIES
	}

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface AccountApiResponseFactory {
		@Nonnull
		AccountApiResponse create(@Nonnull Account account);

		@Nonnull
		AccountApiResponse create(@Nonnull Account account,
															@Nonnull Set<AccountApiResponseSupplement> supplements);
	}

	@AssistedInject
	public AccountApiResponse(@Nonnull AccountService accountService,
														@Nonnull AddressService addressService,
														@Nonnull SessionService sessionService,
														@Nonnull InstitutionService institutionService,
														@Nonnull AuthorizationService authorizationService,
														@Nonnull Formatter formatter,
														@Nonnull Strings strings,
														@Nonnull Provider<CurrentContext> currentContextProvider,
														@Nonnull AddressApiResponseFactory addressApiResponseFactory,
														@Assisted @Nonnull Account account) {
		this(accountService, addressService, sessionService, institutionService, authorizationService, formatter, strings, currentContextProvider, addressApiResponseFactory, account, Collections.emptySet());
	}

	@AssistedInject
	public AccountApiResponse(@Nonnull AccountService accountService,
														@Nonnull AddressService addressService,
														@Nonnull SessionService sessionService,
														@Nonnull InstitutionService institutionService,
														@Nonnull AuthorizationService authorizationService,
														@Nonnull Formatter formatter,
														@Nonnull Strings strings,
														@Nonnull Provider<CurrentContext> currentContextProvider,
														@Nonnull AddressApiResponseFactory addressApiResponseFactory,
														@Assisted @Nonnull Account account,
														@Assisted @Nonnull Set<AccountApiResponseSupplement> supplements) {
		requireNonNull(accountService);
		requireNonNull(addressService);
		requireNonNull(sessionService);
		requireNonNull(institutionService);
		requireNonNull(authorizationService);
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(currentContextProvider);
		requireNonNull(addressApiResponseFactory);
		requireNonNull(account);
		requireNonNull(supplements);

		CurrentContext currentContext = currentContextProvider.get();

		boolean showPrivateDetails = supplements.contains(AccountApiResponseSupplement.EVERYTHING)
				|| supplements.contains(AccountApiResponseSupplement.PRIVATE_DETAILS)
				|| shouldShowPrivateDetails(account, currentContext);

		this.accountId = account.getAccountId();
		this.roleId = account.getRoleId();
		this.institutionId = account.getInstitutionId();
		this.accountSourceId = account.getAccountSourceId();
		this.sourceSystemId = account.getSourceSystemId();
		this.betaStatusId = account.getBetaStatusId();
		this.username = account.getUsername();
		this.firstName = account.getFirstName();
		this.lastName = account.getLastName();
		this.displayName = account.getDisplayName();
		this.timeZone = account.getTimeZone();
		this.locale = account.getLocale();
		this.languageCode = account.getLocale().getLanguage();
		this.countryCode = account.getLocale().getCountry();
		this.created = account.getCreated();
		this.createdDescription = formatter.formatTimestamp(account.getCreated());
		this.providerId = account.getProviderId();
		this.institutionLocationId = account.getInstitutionLocationId();
		this.promptedForInstitutionLocation = account.getPromptedForInstitutionLocation();
		this.testAccount = account.getTestAccount();
		this.passwordResetRequired = account.getPasswordResetRequired();
		this.passwordResetToken = account.getPasswordResetToken();
		this.createdDate = LocalDate.ofInstant(account.getCreated(), currentContext.getTimeZone());
		this.createdDateDescription = formatter.formatDate(this.createdDate, FormatStyle.MEDIUM);

		if (showPrivateDetails) {
			this.emailAddress = account.getEmailAddress();
			this.lastUpdated = account.getLastUpdated();
			this.consentFormAccepted = account.getConsentFormAccepted();
			this.consentFormAcceptedDate = account.getConsentFormAcceptedDate();
			this.consentFormAcceptedDateDescription = account.getConsentFormAcceptedDate() == null ? null : formatter.formatTimestamp(account.getConsentFormAcceptedDate());
			this.lastUpdatedDescription = formatter.formatTimestamp(account.getLastUpdated());
			this.phoneNumber = account.getPhoneNumber();
			this.phoneNumberDescription = account.getPhoneNumber() == null ? null : formatter.formatPhoneNumber(account.getPhoneNumber());
			this.epicPatientMrn = account.getEpicPatientMrn();
			this.epicPatientFhirId = account.getEpicPatientFhirId();
			this.genderIdentityId = account.getGenderIdentityId();
			this.ethnicityId = account.getEthnicityId();
			this.birthSexId = account.getBirthSexId();
			this.raceId = account.getRaceId();
			this.birthdate = account.getBirthdate();
			this.birthdateDescription = account.getBirthdate() == null ? null : formatter.formatDate(account.getBirthdate(), FormatStyle.MEDIUM);

			Address address = addressService.findActiveAddressByAccountId(accountId).orElse(null);
			this.address = address == null ? null : addressApiResponseFactory.create(address);

			Institution institution = institutionService.findInstitutionById(account.getInstitutionId()).get();
			LoginDestinationId loginDestinationId;

			if (institution.getIntegratedCareEnabled()) {
				if (account.getRoleId() == RoleId.MHIC
						|| account.getRoleId() == RoleId.ADMINISTRATOR
						|| account.getRoleId() == RoleId.PROVIDER)
					loginDestinationId = LoginDestinationId.IC_PANEL;
				else
					loginDestinationId = LoginDestinationId.IC_PATIENT;
			} else {
				loginDestinationId = LoginDestinationId.COBALT_PATIENT;
			}

			this.loginDestinationId = loginDestinationId;
			this.accountCapabilityFlags = authorizationService.determineAccountCapabilityFlagsForAccount(account);
		} else {
			this.consentFormAccepted = null;
			this.consentFormAcceptedDate = null;
			this.consentFormAcceptedDateDescription = null;
			this.emailAddress = null;
			this.phoneNumber = null;
			this.phoneNumberDescription = null;
			this.lastUpdated = null;
			this.lastUpdatedDescription = null;
			this.epicPatientMrn = null;
			this.epicPatientFhirId = null;
			this.genderIdentityId = null;
			this.ethnicityId = null;
			this.birthSexId = null;
			this.raceId = null;
			this.birthdate = null;
			this.birthdateDescription = null;
			this.address = null;
			this.loginDestinationId = null;
			this.accountCapabilityFlags = null;
		}

		// TODO: remove this legacy "capabilities" type in favor of accountCapabilityFlags
		if (supplements.contains(AccountApiResponseSupplement.EVERYTHING)
				|| supplements.contains(AccountApiResponseSupplement.CAPABILITIES)) {
			this.capabilities = authorizationService.determineAccountCapabilitiesByInstitutionId(account);
		} else {
			this.capabilities = null;
		}
	}

	@Nonnull
	protected static Boolean shouldShowPrivateDetails(@Nonnull Account account,
																										@Nonnull CurrentContext currentContext) {
		requireNonNull(account);
		requireNonNull(currentContext);

		Account currentAccount = currentContext.getAccount().orElse(null);
		return currentAccount != null && currentAccount.getAccountId().equals(account.getAccountId());
	}

	@Nonnull
	public UUID getAccountId() {
		return accountId;
	}

	@Nonnull
	public RoleId getRoleId() {
		return roleId;
	}

	@Nonnull
	public InstitutionId getInstitutionId() {
		return institutionId;
	}

	@Nonnull
	public AccountSourceId getAccountSourceId() {
		return accountSourceId;
	}

	@Nonnull
	public SourceSystemId getSourceSystemId() {
		return sourceSystemId;
	}

	@Nonnull
	public BetaStatusId getBetaStatusId() {
		return betaStatusId;
	}

	@Nullable
	public String getUsername() {
		return this.username;
	}

	@Nullable
	public UUID getProviderId() {
		return providerId;
	}

	@Nullable
	public String getFirstName() {
		return firstName;
	}

	@Nullable
	public String getLastName() {
		return lastName;
	}

	@Nullable
	public String getDisplayName() {
		return displayName;
	}

	@Nullable
	public String getEmailAddress() {
		return emailAddress;
	}

	@Nullable
	public String getPhoneNumber() {
		return phoneNumber;
	}

	@Nullable
	public String getPhoneNumberDescription() {
		return phoneNumberDescription;
	}

	@Nonnull
	public ZoneId getTimeZone() {
		return timeZone;
	}

	@Nonnull
	public Locale getLocale() {
		return locale;
	}

	@Nonnull
	public String getLanguageCode() {
		return this.languageCode;
	}

	@Nonnull
	public String getCountryCode() {
		return this.countryCode;
	}

	@Nonnull
	public Boolean getConsentFormAccepted() {
		return consentFormAccepted;
	}

	@Nullable
	public Instant getConsentFormAcceptedDate() {
		return consentFormAcceptedDate;
	}

	@Nullable
	public String getConsentFormAcceptedDateDescription() {
		return consentFormAcceptedDateDescription;
	}

	@Nonnull
	public Instant getCreated() {
		return created;
	}

	@Nonnull
	public String getCreatedDescription() {
		return createdDescription;
	}

	@Nullable
	public Instant getLastUpdated() {
		return lastUpdated;
	}

	@Nullable
	public String getLastUpdatedDescription() {
		return lastUpdatedDescription;
	}

	@Nullable
	public Map<InstitutionId, AccountCapabilities> getCapabilities() {
		return capabilities;
	}

	@Nullable
	public GenderIdentityId getGenderIdentityId() {
		return this.genderIdentityId;
	}

	@Nullable
	public EthnicityId getEthnicityId() {
		return this.ethnicityId;
	}

	@Nullable
	public BirthSexId getBirthSexId() {
		return this.birthSexId;
	}

	@Nullable
	public RaceId getRaceId() {
		return this.raceId;
	}

	@Nullable
	public LocalDate getBirthdate() {
		return this.birthdate;
	}

	@Nullable
	public String getBirthdateDescription() {
		return this.birthdateDescription;
	}

	@Nullable
	public AddressApiResponse getAddress() {
		return this.address;
	}

	@Nullable
	public LoginDestinationId getLoginDestinationId() {
		return this.loginDestinationId;
	}

	@Nullable
	public UUID getInstitutionLocationId() {
		return institutionLocationId;
	}

	@Nullable
	public Boolean getPromptedForInstitutionLocation() {
		return promptedForInstitutionLocation;
	}

	@Nullable
	public AccountCapabilityFlags getAccountCapabilityFlags() {
		return this.accountCapabilityFlags;
	}

	@Nullable
	public String getEpicPatientMrn() {
		return this.epicPatientMrn;
	}

	@Nullable
	public String getEpicPatientFhirId() {
		return this.epicPatientFhirId;
	}

	@Nullable
	public Boolean getTestAccount() {
		return this.testAccount;
	}

	@Nullable
	public Boolean getPasswordResetRequired() {
		return passwordResetRequired;
	}

	@Nullable
	public UUID getPasswordResetToken() {
		return passwordResetToken;
	}
}