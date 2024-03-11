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

package com.cobaltplatform.api.model.db;

import com.cobaltplatform.api.model.db.AccountCapabilityType.AccountCapabilityTypeId;
import com.cobaltplatform.api.model.db.AccountSource.AccountSourceId;
import com.cobaltplatform.api.model.db.BetaStatus.BetaStatusId;
import com.cobaltplatform.api.model.db.BirthSex.BirthSexId;
import com.cobaltplatform.api.model.db.Ethnicity.EthnicityId;
import com.cobaltplatform.api.model.db.GenderIdentity.GenderIdentityId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.Race.RaceId;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.model.db.SourceSystem.SourceSystemId;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.pyranid.DatabaseColumn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Account {
	@Nonnull
	private static final Gson GSON;

	static {
		GSON = new GsonBuilder()
				.setPrettyPrinting()
				.disableHtmlEscaping()
				.create();
	}

	@Nullable
	private UUID accountId;
	@Nullable
	private RoleId roleId;
	@Nullable
	private InstitutionId institutionId;
	@Nullable
	private AccountSourceId accountSourceId;
	@Nullable
	@Deprecated // We used to use this for Integrated Care, no longer do.  Legacy data will have it
	private SourceSystemId sourceSystemId;
	@Nullable
	private BetaStatusId betaStatusId;
	@Nullable
	private UUID providerId;
	@Nullable
	private GenderIdentityId genderIdentityId;
	@Nullable
	private BirthSexId birthSexId;
	@Nullable
	private RaceId raceId;
	@Nullable
	private EthnicityId ethnicityId;
	@Nullable
	private String emailAddress;
	@Nullable
	private String password;
	@Nullable
	private String firstName;
	@Nullable
	private String lastName;
	@Nullable
	private String displayName;
	@Nullable
	private String phoneNumber;
	@Nullable
	private String ssoId;
	@Nullable
	private String ssoAttributes; // JSONB
	@Nullable
	private String epicPatientMrn;
	@Nullable
	private String epicPatientFhirId;
	@Nullable
	private String epicPatientUniqueId;
	@Nullable
	private String epicPatientUniqueIdType;
	@Nullable
	private String microsoftId;
	@Nullable
	private ZoneId timeZone;
	@Nullable
	private Locale locale;
	@Nullable
	private Boolean consentFormAccepted;
	@Nullable
	private Instant consentFormAcceptedDate;
	@Nullable
	private Instant consentFormRejectedDate;
	@Nonnull
	private Long accessTokenExpirationInMinutes;
	@Nonnull
	private Long accessTokenShortExpirationInMinutes;
	@Nullable
	private String metadata; // JSONB
	@Nullable
	private Boolean schedulingTutorialViewed;
	@Nullable
	private LocalDate birthdate;
	@Nullable
	private Boolean active;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;
	@Nullable
	private UUID institutionLocationId;
	@Nullable
	private Boolean promptedForInstitutionLocation;
	@Nullable
	private Boolean testAccount;

	@Nullable
	private String username;

	@Nullable
	private Boolean passwordResetRequired;

	@Nullable
	private UUID passwordResetToken;

	// From v_account

	@Nullable
	@DatabaseColumn("account_capability_type_ids")
	private String accountCapabilityTypeIdsAsString; // JSONB

	@Nullable
	private Set<AccountCapabilityTypeId> accountCapabilityTypeIds; // Populated from above string

	@Nonnull
	public Map<String, Object> getMetadataAsMap() {
		String metadata = trimToNull(getMetadata());
		return metadata == null ? Collections.emptyMap() : getGson().fromJson(metadata, new TypeToken<Map<String, Object>>() {
		}.getType());
	}

	@Nonnull
	public StandardMetadata getStandardMetadata() {
		String metadata = trimToNull(getMetadata());
		return metadata == null ? StandardMetadata.emptyInstance() : getGson().fromJson(metadata, StandardMetadata.class);
	}

	@Nonnull
	public Set<AccountCapabilityTypeId> getAccountCapabilityTypeIds() {
		return this.accountCapabilityTypeIds == null ? Set.of() : this.accountCapabilityTypeIds;
	}

	@Nonnull
	protected Gson getGson() {
		return GSON;
	}

	@NotThreadSafe
	public static class StandardMetadata {
		@Nullable
		private Set<UUID> interactionIds;
		@Nullable
		private Set<UUID> interactionInstanceIds;

		@Nonnull
		public static StandardMetadata emptyInstance() {
			StandardMetadata standardMetadata = new StandardMetadata();
			standardMetadata.setInteractionIds(Collections.emptySet());
			return standardMetadata;
		}

		@Nullable
		public Set<UUID> getInteractionIds() {
			return interactionIds;
		}

		public void setInteractionIds(@Nullable Set<UUID> interactionIds) {
			this.interactionIds = interactionIds;
		}

		@Nullable
		public Set<UUID> getInteractionInstanceIds() {
			return interactionInstanceIds;
		}

		public void setInteractionInstanceIds(@Nullable Set<UUID> interactionInstanceIds) {
			this.interactionInstanceIds = interactionInstanceIds;
		}
	}

	@Nullable
	public UUID getAccountId() {
		return accountId;
	}

	public void setAccountId(@Nullable UUID accountId) {
		this.accountId = accountId;
	}

	@Nullable
	public RoleId getRoleId() {
		return roleId;
	}

	public void setRoleId(@Nullable RoleId roleId) {
		this.roleId = roleId;
	}

	@Nullable
	public InstitutionId getInstitutionId() {
		return institutionId;
	}

	public void setInstitutionId(@Nullable InstitutionId institutionId) {
		this.institutionId = institutionId;
	}

	@Nullable
	public AccountSourceId getAccountSourceId() {
		return accountSourceId;
	}

	public void setAccountSourceId(@Nullable AccountSourceId accountSourceId) {
		this.accountSourceId = accountSourceId;
	}

	@Nullable
	@Deprecated
	public SourceSystemId getSourceSystemId() {
		return sourceSystemId;
	}

	@Deprecated
	public void setSourceSystemId(@Nullable SourceSystemId sourceSystemId) {
		this.sourceSystemId = sourceSystemId;
	}

	@Nullable
	public BetaStatusId getBetaStatusId() {
		return betaStatusId;
	}

	public void setBetaStatusId(@Nullable BetaStatusId betaStatusId) {
		this.betaStatusId = betaStatusId;
	}

	@Nullable
	public UUID getProviderId() {
		return providerId;
	}

	public void setProviderId(@Nullable UUID providerId) {
		this.providerId = providerId;
	}

	@Nullable
	public String getEmailAddress() {
		return emailAddress;
	}

	public void setEmailAddress(@Nullable String emailAddress) {
		this.emailAddress = emailAddress;
	}

	@Nullable
	public String getPassword() {
		return password;
	}

	public void setPassword(@Nullable String password) {
		this.password = password;
	}

	@Nullable
	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(@Nullable String firstName) {
		this.firstName = firstName;
	}

	@Nullable
	public String getLastName() {
		return lastName;
	}

	public void setLastName(@Nullable String lastName) {
		this.lastName = lastName;
	}

	@Nullable
	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(@Nullable String displayName) {
		this.displayName = displayName;
	}

	@Nullable
	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(@Nullable String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	@Nullable
	public String getSsoId() {
		return this.ssoId;
	}

	public void setSsoId(@Nullable String ssoId) {
		this.ssoId = ssoId;
	}

	@Nullable
	public String getSsoAttributes() {
		return ssoAttributes;
	}

	public void setSsoAttributes(@Nullable String ssoAttributes) {
		this.ssoAttributes = ssoAttributes;
	}

	@Nullable
	public ZoneId getTimeZone() {
		return timeZone;
	}

	public void setTimeZone(@Nullable ZoneId timeZone) {
		this.timeZone = timeZone;
	}

	@Nullable
	public Locale getLocale() {
		return locale;
	}

	public void setLocale(@Nullable Locale locale) {
		this.locale = locale;
	}

	@Nullable
	public Boolean getConsentFormAccepted() {
		return consentFormAccepted;
	}

	public void setConsentFormAccepted(@Nullable Boolean consentFormAccepted) {
		this.consentFormAccepted = consentFormAccepted;
	}

	@Nullable
	public Instant getConsentFormAcceptedDate() {
		return consentFormAcceptedDate;
	}

	public void setConsentFormAcceptedDate(@Nullable Instant consentFormAcceptedDate) {
		this.consentFormAcceptedDate = consentFormAcceptedDate;
	}

	@Nullable
	public Instant getConsentFormRejectedDate() {
		return this.consentFormRejectedDate;
	}

	public void setConsentFormRejectedDate(@Nullable Instant consentFormRejectedDate) {
		this.consentFormRejectedDate = consentFormRejectedDate;
	}

	@Nullable
	public String getEpicPatientMrn() {
		return this.epicPatientMrn;
	}

	public void setEpicPatientMrn(@Nullable String epicPatientMrn) {
		this.epicPatientMrn = epicPatientMrn;
	}

	@Nullable
	public String getEpicPatientFhirId() {
		return this.epicPatientFhirId;
	}

	public void setEpicPatientFhirId(@Nullable String epicPatientFhirId) {
		this.epicPatientFhirId = epicPatientFhirId;
	}

	@Nullable
	public String getEpicPatientUniqueId() {
		return this.epicPatientUniqueId;
	}

	public void setEpicPatientUniqueId(@Nullable String epicPatientUniqueId) {
		this.epicPatientUniqueId = epicPatientUniqueId;
	}

	@Nullable
	public String getEpicPatientUniqueIdType() {
		return this.epicPatientUniqueIdType;
	}

	public void setEpicPatientUniqueIdType(@Nullable String epicPatientUniqueIdType) {
		this.epicPatientUniqueIdType = epicPatientUniqueIdType;
	}

	@Nullable
	public String getMicrosoftId() {
		return this.microsoftId;
	}

	public void setMicrosoftId(@Nullable String microsoftId) {
		this.microsoftId = microsoftId;
	}

	@Nonnull
	public Long getAccessTokenExpirationInMinutes() {
		return accessTokenExpirationInMinutes;
	}

	public void setAccessTokenExpirationInMinutes(@Nonnull Long accessTokenExpirationInMinutes) {
		this.accessTokenExpirationInMinutes = accessTokenExpirationInMinutes;
	}

	@Nonnull
	public Long getAccessTokenShortExpirationInMinutes() {
		return accessTokenShortExpirationInMinutes;
	}

	public void setAccessTokenShortExpirationInMinutes(@Nonnull Long accessTokenShortExpirationInMinutes) {
		this.accessTokenShortExpirationInMinutes = accessTokenShortExpirationInMinutes;
	}

	@Nullable
	public Boolean getSchedulingTutorialViewed() {
		return schedulingTutorialViewed;
	}

	public void setSchedulingTutorialViewed(@Nullable Boolean schedulingTutorialViewed) {
		this.schedulingTutorialViewed = schedulingTutorialViewed;
	}

	@Nullable
	public Instant getCreated() {
		return created;
	}

	public void setCreated(@Nullable Instant created) {
		this.created = created;
	}

	@Nullable
	public Instant getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(@Nullable Instant lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	@Nullable
	public String getMetadata() {
		return metadata;
	}

	public void setMetadata(@Nullable String metadata) {
		this.metadata = metadata;
	}

	@Nullable
	public GenderIdentityId getGenderIdentityId() {
		return this.genderIdentityId;
	}

	public void setGenderIdentityId(@Nullable GenderIdentityId genderIdentityId) {
		this.genderIdentityId = genderIdentityId;
	}

	@Nullable
	public BirthSexId getBirthSexId() {
		return this.birthSexId;
	}

	public void setBirthSexId(@Nullable BirthSexId birthSexId) {
		this.birthSexId = birthSexId;
	}

	@Nullable
	public RaceId getRaceId() {
		return this.raceId;
	}

	public void setRaceId(@Nullable RaceId raceId) {
		this.raceId = raceId;
	}

	@Nullable
	public EthnicityId getEthnicityId() {
		return this.ethnicityId;
	}

	public void setEthnicityId(@Nullable EthnicityId ethnicityId) {
		this.ethnicityId = ethnicityId;
	}

	@Nullable
	public LocalDate getBirthdate() {
		return this.birthdate;
	}

	public void setBirthdate(@Nullable LocalDate birthdate) {
		this.birthdate = birthdate;
	}

	@Nullable
	public Boolean getActive() {
		return this.active;
	}

	public void setActive(@Nullable Boolean active) {
		this.active = active;
	}

	@Nullable
	public UUID getInstitutionLocationId() {
		return institutionLocationId;
	}

	public void setInstitutionLocationId(@Nullable UUID institutionLocationId) {
		this.institutionLocationId = institutionLocationId;
	}

	@Nullable
	public Boolean getPromptedForInstitutionLocation() {
		return promptedForInstitutionLocation;
	}

	public void setPromptedForInstitutionLocation(@Nullable Boolean promptedForInstitutionLocation) {
		this.promptedForInstitutionLocation = promptedForInstitutionLocation;
	}

	@Nullable
	public String getAccountCapabilityTypeIdsAsString() {
		return this.accountCapabilityTypeIdsAsString;
	}

	public void setAccountCapabilityTypeIdsAsString(@Nullable String accountCapabilityTypeIdsAsString) {
		this.accountCapabilityTypeIdsAsString = accountCapabilityTypeIdsAsString;

		// As a side effect, parse JSON and set our other field
		this.accountCapabilityTypeIds = accountCapabilityTypeIdsAsString == null ? null : GSON.fromJson(accountCapabilityTypeIdsAsString, new TypeToken<Set<AccountCapabilityTypeId>>() {
		}.getType());
	}

	@Nullable
	public Boolean getTestAccount() {
		return this.testAccount;
	}

	public void setTestAccount(@Nullable Boolean testAccount) {
		this.testAccount = testAccount;
	}

	@Nullable
	public String getUsername() {
		return username;
	}

	public void setUsername(@Nullable String username) {
		this.username = username;
	}

	@Nullable
	public Boolean getPasswordResetRequired() {
		return passwordResetRequired;
	}

	public void setPasswordResetRequired(@Nullable Boolean passwordResetRequired) {
		this.passwordResetRequired = passwordResetRequired;
	}

	@Nullable
	public UUID getPasswordResetToken() {
		return passwordResetToken;
	}

	public void setPasswordResetToken(@Nullable UUID passwordResetToken) {
		this.passwordResetToken = passwordResetToken;
	}
}