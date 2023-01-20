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

import com.cobaltplatform.api.model.db.EpicBackendServiceAuthType.EpicBackendServiceAuthTypeId;
import com.cobaltplatform.api.model.db.GroupSessionSystem.GroupSessionSystemId;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.pyranid.DatabaseColumn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify LLC.
 */
@NotThreadSafe
public class Institution {
	@Nonnull
	private static final Gson GSON;

	static {
		GSON = new GsonBuilder()
				.setPrettyPrinting()
				.disableHtmlEscaping()
				.create();
	}

	@Nullable
	private InstitutionId institutionId;
	@Nullable
	private GroupSessionSystemId groupSessionSystemId;
	@Nullable
	private UUID providerTriageScreeningFlowId;
	@Nullable
	private UUID contentScreeningFlowId;
	@Nullable
	private UUID groupSessionsScreeningFlowId;
	@Nullable
	private UUID integratedCareScreeningFlowId;
	@Nullable
	private ZoneId timeZone;
	@Nullable
	private Locale locale;
	@Nullable
	@Deprecated
	private String crisisContent;
	@Nullable
	@Deprecated
	private String privacyContent;
	@Nullable
	@Deprecated
	private String covidContent;
	@Nullable
	private Boolean requireConsentForm;
	@Nullable
	@Deprecated
	private String consentFormContent;
	@Nullable
	private String calendarDescription;
	@Nullable
	private Boolean supportEnabled;
	@Nullable
	@Deprecated
	private String wellBeingContent;
	@Nullable
	private String name;
	@Nullable
	@Deprecated
	private Boolean ssoEnabled;
	@Nullable
	@Deprecated
	private Boolean emailEnabled;
	@Nullable
	private Boolean emailSignupEnabled;
	@Nullable
	@Deprecated
	private Boolean anonymousEnabled;
	@Nullable
	private Boolean integratedCareEnabled;
	@Nullable
	private String metadata;
	@Nonnull
	private Long accessTokenExpirationInMinutes;
	@Nonnull
	private Long accessTokenShortExpirationInMinutes;
	@Nonnull
	private Long anonAccessTokenExpirationInMinutes;
	@Nonnull
	private Long anonAccessTokenShortExpirationInMinutes;
	@Nullable
	private String supportEmailAddress;
	@Nullable
	private Boolean recommendGroupSessionRequests;
	@Nullable
	private Boolean immediateAccessEnabled;
	@Nullable
	private Boolean contactUsEnabled;
	@Nullable
	private Boolean recommendedContentEnabled;
	@Nullable
	private Boolean userSubmittedContentEnabled;
	@Nullable
	private Boolean userSubmittedGroupSessionEnabled;
	@Nullable
	private Boolean userSubmittedGroupSessionRequestEnabled;
	@Nullable
	@DatabaseColumn("ga4_measurement_id")
	private String ga4MeasurementId;

	// EPIC / MyChart configuration

	@Nullable
	private String epicClientId;
	@Nullable
	private String epicUserId;
	@Nullable
	private String epicUserIdType;
	@Nullable
	private String epicUsername;
	@Nullable
	private String epicPassword;
	@Nullable
	private String epicBaseUrl;
	@Nullable
	private String epicTokenUrl;
	@Nullable
	private String epicAuthorizeUrl;
	@Nullable
	@DatabaseColumn("mychart_client_id") // In DB, it's "mychart", in code, it's "MyChart"
	private String myChartClientId;
	@Nullable
	@DatabaseColumn("mychart_scope")
	private String myChartScope;
	@Nullable
	@DatabaseColumn("mychart_aud")
	private String myChartAud;
	@Nullable
	@DatabaseColumn("mychart_response_type")
	private String myChartResponseType;
	@Nullable
	@DatabaseColumn("mychart_callback_url")
	private String myChartCallbackUrl;
	@Nullable
	private EpicBackendServiceAuthTypeId epicBackendServiceAuthTypeId;
	@Nullable
	private String microsoftTenantId;
	@Nullable
	private String microsoftClientId;

	public enum InstitutionId {
		COBALT,
		COBALT_IC,
	}

	@Override
	public String toString() {
		return format("%s{institutionId=%s, name=%s}", getClass().getSimpleName(), getInstitutionId(), getName());
	}

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
		private UUID defaultCrisisInteractionId;
		@Nullable
		private UUID defaultRoleRequestInteractionId;
		@Nullable
		private List<Way2HealthIncidentTrackingConfig> way2HealthIncidentTrackingConfigs;

		@Nonnull
		public static StandardMetadata emptyInstance() {
			StandardMetadata standardMetadata = new StandardMetadata();
			standardMetadata.setWay2HealthIncidentTrackingConfigs(Collections.emptyList());
			return standardMetadata;
		}

		@NotThreadSafe
		public static class Way2HealthIncidentTrackingConfig {
			@Nullable
			private Long studyId;
			@Nullable
			private String type;
			@Nullable
			private UUID interactionId;
			@Nullable
			private Boolean enabled;

			@Nullable
			public Long getStudyId() {
				return studyId;
			}

			public void setStudyId(@Nullable Long studyId) {
				this.studyId = studyId;
			}

			@Nullable
			public String getType() {
				return type;
			}

			public void setType(@Nullable String type) {
				this.type = type;
			}

			@Nullable
			public UUID getInteractionId() {
				return interactionId;
			}

			public void setInteractionId(@Nullable UUID interactionId) {
				this.interactionId = interactionId;
			}

			@Nullable
			public Boolean getEnabled() {
				return enabled;
			}

			public void setEnabled(@Nullable Boolean enabled) {
				this.enabled = enabled;
			}
		}

		@Nullable
		public UUID getDefaultCrisisInteractionId() {
			return defaultCrisisInteractionId;
		}

		public void setDefaultCrisisInteractionId(@Nullable UUID defaultCrisisInteractionId) {
			this.defaultCrisisInteractionId = defaultCrisisInteractionId;
		}

		@Nullable
		public UUID getDefaultRoleRequestInteractionId() {
			return defaultRoleRequestInteractionId;
		}

		public void setDefaultRoleRequestInteractionId(@Nullable UUID defaultRoleRequestInteractionId) {
			this.defaultRoleRequestInteractionId = defaultRoleRequestInteractionId;
		}

		@Nullable
		public List<Way2HealthIncidentTrackingConfig> getWay2HealthIncidentTrackingConfigs() {
			return way2HealthIncidentTrackingConfigs;
		}

		public void setWay2HealthIncidentTrackingConfigs(@Nullable List<Way2HealthIncidentTrackingConfig> way2HealthIncidentTrackingConfigs) {
			this.way2HealthIncidentTrackingConfigs = way2HealthIncidentTrackingConfigs;
		}
	}

	@Nullable
	public InstitutionId getInstitutionId() {
		return institutionId;
	}

	public void setInstitutionId(@Nullable InstitutionId institutionId) {
		this.institutionId = institutionId;
	}

	@Nullable
	public GroupSessionSystemId getGroupSessionSystemId() {
		return groupSessionSystemId;
	}

	public void setGroupSessionSystemId(@Nullable GroupSessionSystemId groupSessionSystemId) {
		this.groupSessionSystemId = groupSessionSystemId;
	}

	@Nullable
	public UUID getProviderTriageScreeningFlowId() {
		return this.providerTriageScreeningFlowId;
	}

	public void setProviderTriageScreeningFlowId(@Nullable UUID providerTriageScreeningFlowId) {
		this.providerTriageScreeningFlowId = providerTriageScreeningFlowId;
	}

	@Nullable
	public UUID getContentScreeningFlowId() {
		return this.contentScreeningFlowId;
	}

	public void setContentScreeningFlowId(@Nullable UUID contentScreeningFlowId) {
		this.contentScreeningFlowId = contentScreeningFlowId;
	}

	@Nullable
	public UUID getGroupSessionsScreeningFlowId() {
		return this.groupSessionsScreeningFlowId;
	}

	public void setGroupSessionsScreeningFlowId(@Nullable UUID groupSessionsScreeningFlowId) {
		this.groupSessionsScreeningFlowId = groupSessionsScreeningFlowId;
	}

	@Nullable
	public UUID getIntegratedCareScreeningFlowId() {
		return this.integratedCareScreeningFlowId;
	}

	public void setIntegratedCareScreeningFlowId(@Nullable UUID integratedCareScreeningFlowId) {
		this.integratedCareScreeningFlowId = integratedCareScreeningFlowId;
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
	@Deprecated
	public String getCrisisContent() {
		return crisisContent;
	}

	@Deprecated
	public void setCrisisContent(@Nullable String crisisContent) {
		this.crisisContent = crisisContent;
	}

	@Nullable
	@Deprecated
	public String getPrivacyContent() {
		return privacyContent;
	}

	@Deprecated
	public void setPrivacyContent(@Nullable String privacyContent) {
		this.privacyContent = privacyContent;
	}

	@Nullable
	@Deprecated
	public String getCovidContent() {
		return covidContent;
	}

	@Deprecated
	public void setCovidContent(@Nullable String covidContent) {
		this.covidContent = covidContent;
	}

	@Nullable
	public Boolean getRequireConsentForm() {
		return requireConsentForm;
	}

	public void setRequireConsentForm(@Nullable Boolean requireConsentForm) {
		this.requireConsentForm = requireConsentForm;
	}

	@Nullable
	@Deprecated
	public String getConsentFormContent() {
		return consentFormContent;
	}

	@Deprecated
	public void setConsentFormContent(@Nullable String consentFormContent) {
		this.consentFormContent = consentFormContent;
	}

	@Nullable
	public String getCalendarDescription() {
		return calendarDescription;
	}

	public void setCalendarDescription(@Nullable String calendarDescription) {
		this.calendarDescription = calendarDescription;
	}

	@Nullable
	public Boolean getSupportEnabled() {
		return supportEnabled;
	}

	public void setSupportEnabled(@Nullable Boolean supportEnabled) {
		this.supportEnabled = supportEnabled;
	}

	@Nullable
	@Deprecated
	public String getWellBeingContent() {
		return wellBeingContent;
	}

	@Deprecated
	public void setWellBeingContent(@Nullable String wellBeingContent) {
		this.wellBeingContent = wellBeingContent;
	}

	@Nullable
	public String getName() {
		return name;
	}

	public void setName(@Nullable String name) {
		this.name = name;
	}

	@Nullable
	public String getMetadata() {
		return metadata;
	}

	public void setMetadata(@Nullable String metadata) {
		this.metadata = metadata;
	}

	@Nullable
	@Deprecated
	public Boolean getSsoEnabled() {
		return ssoEnabled;
	}

	@Deprecated
	public void setSsoEnabled(@Nullable Boolean ssoEnabled) {
		this.ssoEnabled = ssoEnabled;
	}

	@Nullable
	@Deprecated
	public Boolean getEmailEnabled() {
		return emailEnabled;
	}

	@Deprecated
	public void setEmailEnabled(@Nullable Boolean emailEnabled) {
		this.emailEnabled = emailEnabled;
	}

	@Nullable
	public Boolean getEmailSignupEnabled() {
		return this.emailSignupEnabled;
	}

	public void setEmailSignupEnabled(@Nullable Boolean emailSignupEnabled) {
		this.emailSignupEnabled = emailSignupEnabled;
	}

	@Nullable
	@Deprecated
	public Boolean getAnonymousEnabled() {
		return anonymousEnabled;
	}

	@Deprecated
	public void setAnonymousEnabled(@Nullable Boolean anonymousEnabled) {
		this.anonymousEnabled = anonymousEnabled;
	}

	@Nullable
	public Boolean getIntegratedCareEnabled() {
		return this.integratedCareEnabled;
	}

	public void setIntegratedCareEnabled(@Nullable Boolean integratedCareEnabled) {
		this.integratedCareEnabled = integratedCareEnabled;
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

	@Nonnull
	public Long getAnonAccessTokenExpirationInMinutes() {
		return anonAccessTokenExpirationInMinutes;
	}

	public void setAnonAccessTokenExpirationInMinutes(@Nonnull Long anonAccessTokenExpirationInMinutes) {
		this.anonAccessTokenExpirationInMinutes = anonAccessTokenExpirationInMinutes;
	}

	@Nonnull
	public Long getAnonAccessTokenShortExpirationInMinutes() {
		return anonAccessTokenShortExpirationInMinutes;
	}

	public void setAnonAccessTokenShortExpirationInMinutes(@Nonnull Long anonAccessTokenShortExpirationInMinutes) {
		this.anonAccessTokenShortExpirationInMinutes = anonAccessTokenShortExpirationInMinutes;
	}

	@Nullable
	public String getSupportEmailAddress() {
		return this.supportEmailAddress;
	}

	public void setSupportEmailAddress(@Nullable String supportEmailAddress) {
		this.supportEmailAddress = supportEmailAddress;
	}

	@Nullable
	public Boolean getRecommendGroupSessionRequests() {
		return this.recommendGroupSessionRequests;
	}

	public void setRecommendGroupSessionRequests(@Nullable Boolean recommendGroupSessionRequests) {
		this.recommendGroupSessionRequests = recommendGroupSessionRequests;
	}

	@Nullable
	public Boolean getImmediateAccessEnabled() {
		return this.immediateAccessEnabled;
	}

	public void setImmediateAccessEnabled(@Nullable Boolean immediateAccessEnabled) {
		this.immediateAccessEnabled = immediateAccessEnabled;
	}

	@Nullable
	public Boolean getContactUsEnabled() {
		return this.contactUsEnabled;
	}

	public void setContactUsEnabled(@Nullable Boolean contactUsEnabled) {
		this.contactUsEnabled = contactUsEnabled;
	}

	@Nullable
	public Boolean getRecommendedContentEnabled() {
		return this.recommendedContentEnabled;
	}

	public void setRecommendedContentEnabled(@Nullable Boolean recommendedContentEnabled) {
		this.recommendedContentEnabled = recommendedContentEnabled;
	}

	@Nullable
	public Boolean getUserSubmittedContentEnabled() {
		return this.userSubmittedContentEnabled;
	}

	public void setUserSubmittedContentEnabled(@Nullable Boolean userSubmittedContentEnabled) {
		this.userSubmittedContentEnabled = userSubmittedContentEnabled;
	}

	@Nullable
	public Boolean getUserSubmittedGroupSessionEnabled() {
		return this.userSubmittedGroupSessionEnabled;
	}

	public void setUserSubmittedGroupSessionEnabled(@Nullable Boolean userSubmittedGroupSessionEnabled) {
		this.userSubmittedGroupSessionEnabled = userSubmittedGroupSessionEnabled;
	}

	@Nullable
	public Boolean getUserSubmittedGroupSessionRequestEnabled() {
		return this.userSubmittedGroupSessionRequestEnabled;
	}

	public void setUserSubmittedGroupSessionRequestEnabled(@Nullable Boolean userSubmittedGroupSessionRequestEnabled) {
		this.userSubmittedGroupSessionRequestEnabled = userSubmittedGroupSessionRequestEnabled;
	}

	@Nullable
	public String getGa4MeasurementId() {
		return this.ga4MeasurementId;
	}

	public void setGa4MeasurementId(@Nullable String ga4MeasurementId) {
		this.ga4MeasurementId = ga4MeasurementId;
	}

	@Nullable
	public String getEpicClientId() {
		return this.epicClientId;
	}

	public void setEpicClientId(@Nullable String epicClientId) {
		this.epicClientId = epicClientId;
	}

	@Nullable
	public String getEpicUserId() {
		return this.epicUserId;
	}

	public void setEpicUserId(@Nullable String epicUserId) {
		this.epicUserId = epicUserId;
	}

	@Nullable
	public String getEpicUserIdType() {
		return this.epicUserIdType;
	}

	public void setEpicUserIdType(@Nullable String epicUserIdType) {
		this.epicUserIdType = epicUserIdType;
	}

	@Nullable
	public String getEpicUsername() {
		return this.epicUsername;
	}

	public void setEpicUsername(@Nullable String epicUsername) {
		this.epicUsername = epicUsername;
	}

	@Nullable
	public String getEpicPassword() {
		return this.epicPassword;
	}

	public void setEpicPassword(@Nullable String epicPassword) {
		this.epicPassword = epicPassword;
	}

	@Nullable
	public String getEpicBaseUrl() {
		return this.epicBaseUrl;
	}

	public void setEpicBaseUrl(@Nullable String epicBaseUrl) {
		this.epicBaseUrl = epicBaseUrl;
	}

	@Nullable
	public String getMyChartClientId() {
		return this.myChartClientId;
	}

	public void setMyChartClientId(@Nullable String myChartClientId) {
		this.myChartClientId = myChartClientId;
	}

	@Nullable
	public String getMyChartScope() {
		return this.myChartScope;
	}

	public void setMyChartScope(@Nullable String myChartScope) {
		this.myChartScope = myChartScope;
	}

	@Nullable
	public String getMyChartAud() {
		return this.myChartAud;
	}

	public void setMyChartAud(@Nullable String myChartAud) {
		this.myChartAud = myChartAud;
	}

	@Nullable
	public String getMyChartResponseType() {
		return this.myChartResponseType;
	}

	public void setMyChartResponseType(@Nullable String myChartResponseType) {
		this.myChartResponseType = myChartResponseType;
	}

	@Nullable
	public String getEpicTokenUrl() {
		return this.epicTokenUrl;
	}

	public void setEpicTokenUrl(@Nullable String epicTokenUrl) {
		this.epicTokenUrl = epicTokenUrl;
	}

	@Nullable
	public String getEpicAuthorizeUrl() {
		return this.epicAuthorizeUrl;
	}

	public void setEpicAuthorizeUrl(@Nullable String epicAuthorizeUrl) {
		this.epicAuthorizeUrl = epicAuthorizeUrl;
	}

	@Nullable
	public String getMyChartCallbackUrl() {
		return this.myChartCallbackUrl;
	}

	public void setMyChartCallbackUrl(@Nullable String myChartCallbackUrl) {
		this.myChartCallbackUrl = myChartCallbackUrl;
	}

	@Nullable
	public EpicBackendServiceAuthTypeId getEpicBackendServiceAuthTypeId() {
		return this.epicBackendServiceAuthTypeId;
	}

	public void setEpicBackendServiceAuthTypeId(@Nullable EpicBackendServiceAuthTypeId epicBackendServiceAuthTypeId) {
		this.epicBackendServiceAuthTypeId = epicBackendServiceAuthTypeId;
	}

	@Nullable
	public String getMicrosoftTenantId() {
		return this.microsoftTenantId;
	}

	public void setMicrosoftTenantId(@Nullable String microsoftTenantId) {
		this.microsoftTenantId = microsoftTenantId;
	}

	@Nullable
	public String getMicrosoftClientId() {
		return this.microsoftClientId;
	}

	public void setMicrosoftClientId(@Nullable String microsoftClientId) {
		this.microsoftClientId = microsoftClientId;
	}
}