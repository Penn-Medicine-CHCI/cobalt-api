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
	private UUID insuranceId;
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
	private String epicPatientId;
	@Nullable
	private String epicPatientIdType;
	@Nullable
	private String epicPatientMrn;
	@Deprecated
	@Nullable
	private Boolean epicPatientCreatedByCobalt;
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
	private Instant created;
	@Nullable
	private Instant lastUpdated;

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
	protected Gson getGson() {
		return GSON;
	}

	@NotThreadSafe
	public static class StandardMetadata {
		@Nullable
		private Set<UUID> interactionIds;

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
	public SourceSystemId getSourceSystemId() {
		return sourceSystemId;
	}

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
	public UUID getInsuranceId() {
		return this.insuranceId;
	}

	public void setInsuranceId(@Nullable UUID insuranceId) {
		this.insuranceId = insuranceId;
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
	public String getEpicPatientId() {
		return epicPatientId;
	}

	public void setEpicPatientId(@Nullable String epicPatientId) {
		this.epicPatientId = epicPatientId;
	}

	@Nullable
	public String getEpicPatientIdType() {
		return epicPatientIdType;
	}

	public void setEpicPatientIdType(@Nullable String epicPatientIdType) {
		this.epicPatientIdType = epicPatientIdType;
	}

	@Nullable
	public String getEpicPatientMrn() {
		return this.epicPatientMrn;
	}

	public void setEpicPatientMrn(@Nullable String epicPatientMrn) {
		this.epicPatientMrn = epicPatientMrn;
	}

	@Nullable
	@Deprecated
	public Boolean getEpicPatientCreatedByCobalt() {
		return epicPatientCreatedByCobalt;
	}

	@Deprecated
	public void setEpicPatientCreatedByCobalt(@Nullable Boolean epicPatientCreatedByCobalt) {
		this.epicPatientCreatedByCobalt = epicPatientCreatedByCobalt;
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
}