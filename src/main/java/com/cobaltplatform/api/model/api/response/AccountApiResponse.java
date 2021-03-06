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

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AccountSession;
import com.cobaltplatform.api.model.db.AccountSource.AccountSourceId;
import com.cobaltplatform.api.model.db.BetaStatus.BetaStatusId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.model.db.SourceSystem.SourceSystemId;
import com.cobaltplatform.api.model.security.AccountCapabilities;
import com.cobaltplatform.api.service.AccountService;
import com.cobaltplatform.api.service.AuthorizationService;
import com.cobaltplatform.api.service.SessionService;
import com.cobaltplatform.api.util.Formatter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Provider;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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
	@Nullable
	private final Boolean completedIntroAssessment;
	@Nonnull
	private final ZoneId timeZone;
	@Nonnull
	private final Locale locale;
	@Nonnull
	private final Boolean consentFormAccepted;
	@Nullable
	private final Instant consentFormAcceptedDate;
	@Nullable
	private final String consentFormAcceptedDateDescription;
	@Nullable
	private final String epicPatientId;
	@Nullable
	private final String epicPatientIdType;
	@Nonnull
	private final Boolean epicPatientCreatedByCobalt;
	@Nonnull
	private final Instant created;
	@Nonnull
	private final String createdDescription;
	@Nullable
	private final Instant lastUpdated;
	@Nullable
	private final String lastUpdatedDescription;
	@Nullable
	private final IntroAssessmentApiResponse introAssessment;
	@Nullable
	private final String lastUpdatedIntroAssessmentDescription;
	@Nullable
	private final Map<InstitutionId, AccountCapabilities> capabilities;

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
														@Nonnull SessionService sessionService,
														@Nonnull AuthorizationService authorizationService,
														@Nonnull Formatter formatter,
														@Nonnull Strings strings,
														@Nonnull Provider<CurrentContext> currentContextProvider,
														@Nonnull IntroAssessmentApiResponse.IntroAssessmentApiResponseFactory introAssessmentApiResponseFactory,
														@Assisted @Nonnull Account account) {
		this(accountService, sessionService, authorizationService, formatter, strings, currentContextProvider, introAssessmentApiResponseFactory, account, Collections.emptySet());
	}

	@AssistedInject
	public AccountApiResponse(@Nonnull AccountService accountService,
														@Nonnull SessionService sessionService,
														@Nonnull AuthorizationService authorizationService,
														@Nonnull Formatter formatter,
														@Nonnull Strings strings,
														@Nonnull Provider<CurrentContext> currentContextProvider,
														@Nonnull IntroAssessmentApiResponse.IntroAssessmentApiResponseFactory introAssessmentApiResponseFactory,
														@Assisted @Nonnull Account account,
														@Assisted @Nonnull Set<AccountApiResponseSupplement> supplements) {
		requireNonNull(accountService);
		requireNonNull(sessionService);
		requireNonNull(authorizationService);
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(currentContextProvider);
		requireNonNull(account);
		requireNonNull(supplements);

		boolean showPrivateDetails = supplements.contains(AccountApiResponseSupplement.EVERYTHING)
				|| supplements.contains(AccountApiResponseSupplement.PRIVATE_DETAILS)
				|| shouldShowPrivateDetails(account, currentContextProvider);

		this.accountId = account.getAccountId();
		this.roleId = account.getRoleId();
		this.institutionId = account.getInstitutionId();
		this.accountSourceId = account.getAccountSourceId();
		this.sourceSystemId = account.getSourceSystemId();
		this.betaStatusId = account.getBetaStatusId();
		this.firstName = account.getFirstName();
		this.lastName = account.getLastName();
		this.displayName = account.getDisplayName();
		this.timeZone = account.getTimeZone();
		this.locale = account.getLocale();
		this.created = account.getCreated();
		this.createdDescription = formatter.formatTimestamp(account.getCreated());
		this.providerId = account.getProviderId();

		if (showPrivateDetails) {
			Optional<AccountSession> introSession = sessionService.getCurrentIntroSessionForAccount(account);
			this.completedIntroAssessment = introSession.isPresent();
			this.emailAddress = account.getEmailAddress();
			this.lastUpdated = account.getLastUpdated();
			this.consentFormAccepted = account.getConsentFormAccepted();
			this.consentFormAcceptedDate = account.getConsentFormAcceptedDate();
			this.consentFormAcceptedDateDescription = account.getConsentFormAcceptedDate() == null ? null : formatter.formatTimestamp(account.getConsentFormAcceptedDate());
			this.lastUpdatedDescription = formatter.formatTimestamp(account.getLastUpdated());
			this.phoneNumber = account.getPhoneNumber();
			this.phoneNumberDescription = account.getPhoneNumber() == null ? null : formatter.formatPhoneNumber(account.getPhoneNumber());

			if (completedIntroAssessment) {
				this.lastUpdatedIntroAssessmentDescription = formatter.formatTimestamp(introSession.get().getCreated());
				this.introAssessment = introAssessmentApiResponseFactory.create(introSession.get());
			} else {
				this.introAssessment = null;
				this.lastUpdatedIntroAssessmentDescription = null;
			}

			this.epicPatientId = account.getEpicPatientId();
			this.epicPatientIdType = account.getEpicPatientIdType();
			this.epicPatientCreatedByCobalt = account.getEpicPatientCreatedByCobalt();
		} else {
			this.completedIntroAssessment = null;
			this.consentFormAccepted = null;
			this.consentFormAcceptedDate = null;
			this.consentFormAcceptedDateDescription = null;
			this.emailAddress = null;
			this.phoneNumber = null;
			this.phoneNumberDescription = null;
			this.lastUpdated = null;
			this.lastUpdatedDescription = null;
			this.introAssessment = null;
			this.lastUpdatedIntroAssessmentDescription = null;
			this.epicPatientId = null;
			this.epicPatientIdType = null;
			this.epicPatientCreatedByCobalt = null;
		}

		if(supplements.contains(AccountApiResponseSupplement.EVERYTHING)
				|| supplements.contains(AccountApiResponseSupplement.CAPABILITIES)) {
			this.capabilities = authorizationService.determineAccountCapabilitiesByInstitutionId(account);
		} else {
			this.capabilities = null;
		}
	}

	@Nonnull
	protected static Boolean shouldShowPrivateDetails(@Nonnull Account account,
																										@Nonnull Provider<CurrentContext> currentContextProvider) {
		requireNonNull(account);
		requireNonNull(currentContextProvider);

		Account currentAccount = currentContextProvider.get().getAccount().orElse(null);
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
	public IntroAssessmentApiResponse getIntroAssessment() {
		return introAssessment;
	}

	@Nullable
	public String getLastUpdatedIntroAssessmentDescription() {
		return lastUpdatedIntroAssessmentDescription;
	}

	@Nullable
	public Boolean getCompletedIntroAssessment() {
		return completedIntroAssessment;
	}

	@Nullable
	public String getEpicPatientId() {
		return epicPatientId;
	}

	@Nullable
	public String getEpicPatientIdType() {
		return epicPatientIdType;
	}

	@Nonnull
	public Boolean getEpicPatientCreatedByCobalt() {
		return epicPatientCreatedByCobalt;
	}

	@Nullable
	public Map<InstitutionId, AccountCapabilities> getCapabilities() {
		return capabilities;
	}
}