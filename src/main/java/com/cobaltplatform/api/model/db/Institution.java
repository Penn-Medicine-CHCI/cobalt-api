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

import com.cobaltplatform.api.model.db.AnonymousAccountExpirationStrategy.AnonymousAccountExpirationStrategyId;
import com.cobaltplatform.api.model.db.EpicBackendServiceAuthType.EpicBackendServiceAuthTypeId;
import com.cobaltplatform.api.model.db.GroupSessionSystem.GroupSessionSystemId;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.pyranid.DatabaseColumn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.LocalTime;
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
	private UUID groupSessionDefaultIntakeScreeningFlowId;
	@Nullable
	private UUID integratedCareScreeningFlowId;
	@Nullable
	private UUID integratedCareIntakeScreeningFlowId;
	@Nullable
	private UUID featureScreeningFlowId;
	@Nullable
	private AnonymousAccountExpirationStrategyId anonymousAccountExpirationStrategyId;
	@Nullable
	private ZoneId timeZone;
	@Nullable
	private Locale locale;
	@Nullable
	private Boolean requireConsentForm;
	@Nullable
	private String calendarDescription;
	@Nullable
	private Boolean supportEnabled;
	@Nullable
	private String name;
	@Nullable
	private Boolean emailSignupEnabled;
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
	private Boolean groupSessionRequestsEnabled;
	@Nullable
	private Boolean immediateAccessEnabled;
	@Nullable
	private Boolean contactUsEnabled;
	@Nullable
	private Boolean featuresEnabled;
	@Nullable
	private Boolean recommendedContentEnabled;
	@Nullable
	private Boolean userSubmittedContentEnabled;
	@Nullable
	private Boolean userSubmittedGroupSessionEnabled;
	@Nullable
	private Boolean userSubmittedGroupSessionRequestEnabled;
	@Deprecated // TODO: remove this in favor of ga4PatientMeasurementId and ga4StaffMeasurementId
	@Nullable
	@DatabaseColumn("ga4_measurement_id")
	private String ga4MeasurementId;
	@Nullable
	@DatabaseColumn("ga4_patient_measurement_id")
	private String ga4PatientMeasurementId;
	@Nullable
	@DatabaseColumn("ga4_staff_measurement_id")
	private String ga4StaffMeasurementId;

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
	private String epicMrnTypeName;
	@Nullable
	private String epicPatientUniqueIdType;
	@Nullable
	private String epicPatientUniqueIdSystem;
	@Nullable
	private String epicPatientMrnSystem;
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
	@DatabaseColumn("mychart_name")
	private String myChartName;
	@Nullable
	@DatabaseColumn("mychart_default_url")
	private String myChartDefaultUrl;
	@Nullable
	private EpicBackendServiceAuthTypeId epicBackendServiceAuthTypeId;
	@Nullable
	private String microsoftTenantId;
	@Nullable
	private String microsoftClientId;
	@Nullable
	private Integer groupSessionReservationDefaultReminderMinutesOffset;
	@Nullable
	private LocalTime groupSessionReservationDefaultFollowupTimeOfDay;
	@Nullable
	private Integer groupSessionReservationDefaultFollowupDayOffset;
	@Nullable
	private LocalTime appointmentReservationDefaultReminderTimeOfDay;
	@Nullable
	private Integer appointmentReservationDefaultReminderDayOffset;
	@Nullable
	private Integer integratedCareSentResourcesFollowupWeekOffset;
	@Nullable
	private Integer integratedCareSentResourcesFollowupDayOffset;
	@Nullable
	private Integer integratedCareOutreachFollowupDayOffset;
	@Nullable
	private String defaultFromEmailAddress;
	@Nullable
	private String integratedCarePhoneNumber;
	@Nullable
	private String integratedCareAvailabilityDescription;
	@Nullable
	private String integratedCareProgramName;
	@Nullable
	private String integratedCarePrimaryCareName;
	@Nullable
	private String clinicalSupportPhoneNumber;
	@Nullable
	private String integratedCareClinicalReportDisclaimer;
	@Nullable
	private Integer epicFhirAppointmentFindCacheExpirationInSeconds;
	@Nullable
	private String resourceGroupsTitle;
	@Nullable
	private String resourceGroupsDescription;
	@Nullable
	private Boolean epicFhirEnabled;
	@Nullable
	private Boolean faqEnabled;
	@Nullable
	private String externalContactUsUrl;
	@Nullable
	@DatabaseColumn("mychart_instructions_url")
	private String myChartInstructionsUrl;
	@Nullable
	private UUID featuredTopicCenterId;
	@Nullable
	private String techSupportPhoneNumber;
	@Nullable
	private String privacyPolicyUrl;
	@Nullable
	private String googleReportingServiceAccountPrivateKey;
	@Nullable
	private String ga4PropertyId;

	public enum InstitutionId {
		COBALT,
		COBALT_IC,
		COBALT_FHIR,
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
	public UUID getGroupSessionDefaultIntakeScreeningFlowId() {
		return this.groupSessionDefaultIntakeScreeningFlowId;
	}

	public void setGroupSessionDefaultIntakeScreeningFlowId(@Nullable UUID groupSessionDefaultIntakeScreeningFlowId) {
		this.groupSessionDefaultIntakeScreeningFlowId = groupSessionDefaultIntakeScreeningFlowId;
	}

	@Nullable
	public UUID getIntegratedCareScreeningFlowId() {
		return this.integratedCareScreeningFlowId;
	}

	public void setIntegratedCareScreeningFlowId(@Nullable UUID integratedCareScreeningFlowId) {
		this.integratedCareScreeningFlowId = integratedCareScreeningFlowId;
	}

	@Nullable
	public UUID getIntegratedCareIntakeScreeningFlowId() {
		return this.integratedCareIntakeScreeningFlowId;
	}

	public void setIntegratedCareIntakeScreeningFlowId(@Nullable UUID integratedCareIntakeScreeningFlowId) {
		this.integratedCareIntakeScreeningFlowId = integratedCareIntakeScreeningFlowId;
	}

	@Nullable
	public UUID getFeatureScreeningFlowId() {
		return this.featureScreeningFlowId;
	}

	public void setFeatureScreeningFlowId(@Nullable UUID featureScreeningFlowId) {
		this.featureScreeningFlowId = featureScreeningFlowId;
	}

	@Nullable
	public AnonymousAccountExpirationStrategyId getAnonymousAccountExpirationStrategyId() {
		return this.anonymousAccountExpirationStrategyId;
	}

	public void setAnonymousAccountExpirationStrategyId(@Nullable AnonymousAccountExpirationStrategyId anonymousAccountExpirationStrategyId) {
		this.anonymousAccountExpirationStrategyId = anonymousAccountExpirationStrategyId;
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
	public Boolean getRequireConsentForm() {
		return requireConsentForm;
	}

	public void setRequireConsentForm(@Nullable Boolean requireConsentForm) {
		this.requireConsentForm = requireConsentForm;
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
	public Boolean getEmailSignupEnabled() {
		return this.emailSignupEnabled;
	}

	public void setEmailSignupEnabled(@Nullable Boolean emailSignupEnabled) {
		this.emailSignupEnabled = emailSignupEnabled;
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
	public Boolean getGroupSessionRequestsEnabled() {
		return this.groupSessionRequestsEnabled;
	}

	public void setGroupSessionRequestsEnabled(@Nullable Boolean groupSessionRequestsEnabled) {
		this.groupSessionRequestsEnabled = groupSessionRequestsEnabled;
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
	public Boolean getFeaturesEnabled() {
		return this.featuresEnabled;
	}

	public void setFeaturesEnabled(@Nullable Boolean featuresEnabled) {
		this.featuresEnabled = featuresEnabled;
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
	@Deprecated
	public String getGa4MeasurementId() {
		return this.ga4MeasurementId;
	}

	@Deprecated
	public void setGa4MeasurementId(@Nullable String ga4MeasurementId) {
		this.ga4MeasurementId = ga4MeasurementId;
	}

	@Nullable
	public String getGa4PatientMeasurementId() {
		return this.ga4PatientMeasurementId;
	}

	public void setGa4PatientMeasurementId(@Nullable String ga4PatientMeasurementId) {
		this.ga4PatientMeasurementId = ga4PatientMeasurementId;
	}

	@Nullable
	public String getGa4StaffMeasurementId() {
		return this.ga4StaffMeasurementId;
	}

	public void setGa4StaffMeasurementId(@Nullable String ga4StaffMeasurementId) {
		this.ga4StaffMeasurementId = ga4StaffMeasurementId;
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
	public String getEpicPatientUniqueIdType() {
		return this.epicPatientUniqueIdType;
	}

	public void setEpicPatientUniqueIdType(@Nullable String epicPatientUniqueIdType) {
		this.epicPatientUniqueIdType = epicPatientUniqueIdType;
	}

	@Nullable
	public String getEpicPatientUniqueIdSystem() {
		return this.epicPatientUniqueIdSystem;
	}

	public void setEpicPatientUniqueIdSystem(@Nullable String epicPatientUniqueIdSystem) {
		this.epicPatientUniqueIdSystem = epicPatientUniqueIdSystem;
	}

	@Nullable
	public String getEpicPatientMrnSystem() {
		return this.epicPatientMrnSystem;
	}

	public void setEpicPatientMrnSystem(@Nullable String epicPatientMrnSystem) {
		this.epicPatientMrnSystem = epicPatientMrnSystem;
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
	public String getMyChartDefaultUrl() {
		return this.myChartDefaultUrl;
	}

	public void setMyChartDefaultUrl(@Nullable String myChartDefaultUrl) {
		this.myChartDefaultUrl = myChartDefaultUrl;
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
	public String getEpicMrnTypeName() {
		return this.epicMrnTypeName;
	}

	public void setEpicMrnTypeName(@Nullable String epicMrnTypeName) {
		this.epicMrnTypeName = epicMrnTypeName;
	}

	@Nullable
	public String getMyChartCallbackUrl() {
		return this.myChartCallbackUrl;
	}

	public void setMyChartCallbackUrl(@Nullable String myChartCallbackUrl) {
		this.myChartCallbackUrl = myChartCallbackUrl;
	}

	@Nullable
	public String getMyChartName() {
		return this.myChartName;
	}

	public void setMyChartName(@Nullable String myChartName) {
		this.myChartName = myChartName;
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

	@Nullable
	public Integer getGroupSessionReservationDefaultReminderMinutesOffset() {
		return this.groupSessionReservationDefaultReminderMinutesOffset;
	}

	public void setGroupSessionReservationDefaultReminderMinutesOffset(@Nullable Integer groupSessionReservationDefaultReminderMinutesOffset) {
		this.groupSessionReservationDefaultReminderMinutesOffset = groupSessionReservationDefaultReminderMinutesOffset;
	}

	@Nullable
	public LocalTime getGroupSessionReservationDefaultFollowupTimeOfDay() {
		return this.groupSessionReservationDefaultFollowupTimeOfDay;
	}

	public void setGroupSessionReservationDefaultFollowupTimeOfDay(@Nullable LocalTime groupSessionReservationDefaultFollowupTimeOfDay) {
		this.groupSessionReservationDefaultFollowupTimeOfDay = groupSessionReservationDefaultFollowupTimeOfDay;
	}

	@Nullable
	public Integer getGroupSessionReservationDefaultFollowupDayOffset() {
		return this.groupSessionReservationDefaultFollowupDayOffset;
	}

	public void setGroupSessionReservationDefaultFollowupDayOffset(@Nullable Integer groupSessionReservationDefaultFollowupDayOffset) {
		this.groupSessionReservationDefaultFollowupDayOffset = groupSessionReservationDefaultFollowupDayOffset;
	}

	@Nullable
	public LocalTime getAppointmentReservationDefaultReminderTimeOfDay() {
		return this.appointmentReservationDefaultReminderTimeOfDay;
	}

	public void setAppointmentReservationDefaultReminderTimeOfDay(@Nullable LocalTime appointmentReservationDefaultReminderTimeOfDay) {
		this.appointmentReservationDefaultReminderTimeOfDay = appointmentReservationDefaultReminderTimeOfDay;
	}

	@Nullable
	public Integer getAppointmentReservationDefaultReminderDayOffset() {
		return this.appointmentReservationDefaultReminderDayOffset;
	}

	public void setAppointmentReservationDefaultReminderDayOffset(@Nullable Integer appointmentReservationDefaultReminderDayOffset) {
		this.appointmentReservationDefaultReminderDayOffset = appointmentReservationDefaultReminderDayOffset;
	}

	@Nullable
	public Integer getIntegratedCareSentResourcesFollowupDayOffset() {
		return this.integratedCareSentResourcesFollowupDayOffset;
	}

	public void setIntegratedCareSentResourcesFollowupDayOffset(@Nullable Integer integratedCareSentResourcesFollowupDayOffset) {
		this.integratedCareSentResourcesFollowupDayOffset = integratedCareSentResourcesFollowupDayOffset;
	}

	@Nullable
	public Integer getIntegratedCareSentResourcesFollowupWeekOffset() {
		return this.integratedCareSentResourcesFollowupWeekOffset;
	}

	public void setIntegratedCareSentResourcesFollowupWeekOffset(@Nullable Integer integratedCareSentResourcesFollowupWeekOffset) {
		this.integratedCareSentResourcesFollowupWeekOffset = integratedCareSentResourcesFollowupWeekOffset;
	}

	@Nullable
	public Integer getIntegratedCareOutreachFollowupDayOffset() {
		return this.integratedCareOutreachFollowupDayOffset;
	}

	public void setIntegratedCareOutreachFollowupDayOffset(@Nullable Integer integratedCareOutreachFollowupDayOffset) {
		this.integratedCareOutreachFollowupDayOffset = integratedCareOutreachFollowupDayOffset;
	}

	@Nullable
	public String getDefaultFromEmailAddress() {
		return this.defaultFromEmailAddress;
	}

	public void setDefaultFromEmailAddress(@Nullable String defaultFromEmailAddress) {
		this.defaultFromEmailAddress = defaultFromEmailAddress;
	}

	@Nullable
	public String getIntegratedCarePhoneNumber() {
		return this.integratedCarePhoneNumber;
	}

	public void setIntegratedCarePhoneNumber(@Nullable String integratedCarePhoneNumber) {
		this.integratedCarePhoneNumber = integratedCarePhoneNumber;
	}

	@Nullable
	public String getIntegratedCareAvailabilityDescription() {
		return this.integratedCareAvailabilityDescription;
	}

	public void setIntegratedCareAvailabilityDescription(@Nullable String integratedCareAvailabilityDescription) {
		this.integratedCareAvailabilityDescription = integratedCareAvailabilityDescription;
	}

	@Nullable
	public String getIntegratedCareProgramName() {
		return this.integratedCareProgramName;
	}

	public void setIntegratedCareProgramName(@Nullable String integratedCareProgramName) {
		this.integratedCareProgramName = integratedCareProgramName;
	}

	@Nullable
	public String getIntegratedCarePrimaryCareName() {
		return this.integratedCarePrimaryCareName;
	}

	public void setIntegratedCarePrimaryCareName(@Nullable String integratedCarePrimaryCareName) {
		this.integratedCarePrimaryCareName = integratedCarePrimaryCareName;
	}

	@Nullable
	public String getClinicalSupportPhoneNumber() {
		return this.clinicalSupportPhoneNumber;
	}

	public void setClinicalSupportPhoneNumber(@Nullable String clinicalSupportPhoneNumber) {
		this.clinicalSupportPhoneNumber = clinicalSupportPhoneNumber;
	}

	@Nullable
	public String getIntegratedCareClinicalReportDisclaimer() {
		return this.integratedCareClinicalReportDisclaimer;
	}

	public void setIntegratedCareClinicalReportDisclaimer(@Nullable String integratedCareClinicalReportDisclaimer) {
		this.integratedCareClinicalReportDisclaimer = integratedCareClinicalReportDisclaimer;
	}

	@Nullable
	public Integer getEpicFhirAppointmentFindCacheExpirationInSeconds() {
		return this.epicFhirAppointmentFindCacheExpirationInSeconds;
	}

	public void setEpicFhirAppointmentFindCacheExpirationInSeconds(@Nullable Integer epicFhirAppointmentFindCacheExpirationInSeconds) {
		this.epicFhirAppointmentFindCacheExpirationInSeconds = epicFhirAppointmentFindCacheExpirationInSeconds;
	}

	@Nullable
	public String getResourceGroupsTitle() {
		return this.resourceGroupsTitle;
	}

	public void setResourceGroupsTitle(@Nullable String resourceGroupsTitle) {
		this.resourceGroupsTitle = resourceGroupsTitle;
	}

	@Nullable
	public String getResourceGroupsDescription() {
		return this.resourceGroupsDescription;
	}

	public void setResourceGroupsDescription(@Nullable String resourceGroupsDescription) {
		this.resourceGroupsDescription = resourceGroupsDescription;
	}

	@Nullable
	public Boolean getEpicFhirEnabled() {
		return this.epicFhirEnabled;
	}

	public void setEpicFhirEnabled(@Nullable Boolean epicFhirEnabled) {
		this.epicFhirEnabled = epicFhirEnabled;
	}

	@Nullable
	public Boolean getFaqEnabled() {
		return this.faqEnabled;
	}

	public void setFaqEnabled(@Nullable Boolean faqEnabled) {
		this.faqEnabled = faqEnabled;
	}

	@Nullable
	public String getExternalContactUsUrl() {
		return this.externalContactUsUrl;
	}

	public void setExternalContactUsUrl(@Nullable String externalContactUsUrl) {
		this.externalContactUsUrl = externalContactUsUrl;
	}

	@Nullable
	public String getMyChartInstructionsUrl() {
		return this.myChartInstructionsUrl;
	}

	public void setMyChartInstructionsUrl(@Nullable String myChartInstructionsUrl) {
		this.myChartInstructionsUrl = myChartInstructionsUrl;
	}

	@Nullable
	public UUID getFeaturedTopicCenterId() {
		return this.featuredTopicCenterId;
	}

	public void setFeaturedTopicCenterId(@Nullable UUID featuredTopicCenterId) {
		this.featuredTopicCenterId = featuredTopicCenterId;
	}

	@Nullable
	public String getTechSupportPhoneNumber() {
		return this.techSupportPhoneNumber;
	}

	public void setTechSupportPhoneNumber(@Nullable String techSupportPhoneNumber) {
		this.techSupportPhoneNumber = techSupportPhoneNumber;
	}

	@Nullable
	public String getPrivacyPolicyUrl() {
		return this.privacyPolicyUrl;
	}

	public void setPrivacyPolicyUrl(@Nullable String privacyPolicyUrl) {
		this.privacyPolicyUrl = privacyPolicyUrl;
	}

	@Nullable
	public String getGoogleReportingServiceAccountPrivateKey() {
		return this.googleReportingServiceAccountPrivateKey;
	}

	public void setGoogleReportingServiceAccountPrivateKey(@Nullable String googleReportingServiceAccountPrivateKey) {
		this.googleReportingServiceAccountPrivateKey = googleReportingServiceAccountPrivateKey;
	}

	@Nullable
	public String getGa4PropertyId() {
		return this.ga4PropertyId;
	}

	public void setGa4PropertyId(@Nullable String ga4PropertyId) {
		this.ga4PropertyId = ga4PropertyId;
	}
}