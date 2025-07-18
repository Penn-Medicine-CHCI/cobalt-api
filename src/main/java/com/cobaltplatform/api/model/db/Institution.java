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
import java.time.Instant;
import java.time.LocalDate;
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
	private String epicPatientMrnTypeName;
	@Nullable
	private String epicPatientMrnTypeAlternateName;
	@Nullable
	private String epicPatientUniqueIdType;
	@Nullable
	private String epicPatientUniqueIdSystem;
	@Nullable
	private String epicPatientMrnSystem;
	@Nullable
	private String epicPatientEncounterCsnSystem;
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
	private UUID featuredSecondaryTopicCenterId;
	@Nullable
	private String techSupportPhoneNumber;
	@Nullable
	private String privacyPolicyUrl;
	@Nullable
	private String secureFilesharingPlatformName;
	@Nullable
	private String secureFilesharingPlatformUrl;
	@Nullable
	private String googleReportingServiceAccountPrivateKey;
	@Nullable
	@DatabaseColumn("google_ga4_property_id")
	private String googleGa4PropertyId;
	@Nullable
	@DatabaseColumn("google_bigquery_resource_id")
	private String googleBigQueryResourceId;
	@Nullable
	@DatabaseColumn("google_bigquery_sync_enabled")
	private Boolean googleBigQuerySyncEnabled;
	@Nullable
	@DatabaseColumn("google_bigquery_sync_starts_at")
	private LocalDate googleBigQuerySyncStartsAt;
	@Nullable
	private Long mixpanelProjectId;
	@Nullable
	private String mixpanelServiceAccountUsername;
	@Nullable
	private String mixpanelServiceAccountSecret;
	@Nullable
	private Boolean mixpanelSyncEnabled;
	@Nullable
	private LocalDate mixpanelSyncStartsAt;
	@Nullable
	private Boolean googleFcmPushNotificationsEnabled;
	@Nullable
	private String integratedCareOrderImportBucketName;

	// Microsoft Teams config
	@Nullable
	private Boolean microsoftTeamsEnabled;
	@Nullable
	private String microsoftTeamsTenantId;
	@Nullable
	private String microsoftTeamsClientId;
	@Nullable
	private String microsoftTeamsUserId;

	// Tableau config
	@Nullable
	private Boolean tableauEnabled;
	@Nullable
	private String tableauClientId;
	@Nullable
	private String tableauApiBaseUrl;
	@Nullable
	private String tableauContentUrl;
	@Nullable
	private String tableauEmailAddress;
	@Nullable
	private String tableauViewName;
	@Nullable
	private String tableauReportName;

	@Nullable
	private UUID integratedCareSafetyPlanningManagerAccountId;
	@Nullable
	private Integer integratedCareOrderImportDelayInSeconds;

	@Nullable
	private String twilioAccountSid;
	@Nullable
	@Deprecated // Prefer twilioMessagingServiceSid instead of explicit numbers
	private String twilioFromNumber;
	@Nullable
	private String twilioMessagingServiceSid;
	@Nullable
	private Boolean callMessagesEnabled;
	@Nullable
	private Boolean smsMessagesEnabled;

	@Nullable
	private LocalTime integratedCareOrderImportStartTimeWindow;
	@Nullable
	private LocalTime integratedCareOrderImportEndTimeWindow;

	@Nullable
	private Boolean epicProviderSlotBookingSyncEnabled;
	@Nullable
	private String epicProviderSlotBookingSyncContactIdType;
	@Nullable
	private String epicProviderSlotBookingSyncDepartmentIdType;
	@Nullable
	private String epicProviderSlotBookingSyncVisitTypeIdType;

	@Nullable
	private Boolean appointmentFeedbackSurveyEnabled;
	@Nullable
	private String appointmentFeedbackSurveyUrl;
	@Nullable
	private String appointmentFeedbackSurveyDurationDescription;
	@Nullable
	private Integer appointmentFeedbackSurveyDelayInMinutes;

	@Nullable
	private Boolean googleGeoEnabled;
	@Nullable
	private Boolean contentAudiencesEnabled;
	@Nullable
	private Boolean resourcePacketsEnabled;
	@Nullable
	private Boolean integratedCarePatientDemographicsRequired;
	@Nullable
	private Boolean integratedCarePatientCarePreferenceVisible;
	@Nullable
	private String integratedCareCallCenterName;
	@Nullable
	private String integratedCareMhpTriageOverviewOverride;
	@Nullable
	private String integratedCareBookingInsuranceRequirements;
	@Nullable
	private String integratedCarePatientIntroOverride;
	@Nullable
	private String landingPageTaglineOverride;

	@Nullable
	private Boolean preferLegacyTopicCenters;

	@Nullable
	private UUID onboardingScreeningFlowId;

	@Nullable
	private String anonymousImplicitUrlPathRegex;
	@Nullable
	private Instant anonymousImplicitAccessTokensValidAfter;

	// Branding configuration

	// Top-left nav header logo
	@Nullable
	private String headerLogoUrl;
	// Footer logo, right above "Powered by Cobalt Innovations, Inc."
	@Nullable
	private String footerLogoUrl;

	// Copy/image for when patients first enter the site.  Currently ignored for IC institutions.
	@Nullable
	private String heroTitle;
	@Nullable
	private String heroDescription;
	@Nullable
	private String heroImageUrl;

	// "Sign in" screen logo at top left of screen
	@Nullable
	private String signInLogoUrl;
	// Large "Sign in" screen logo on right side of screen
	@Nullable
	private String signInLargeLogoUrl;
	// Large "Sign in" screen logo background image, sits underneath the large logo
	@Nullable
	private String signInLargeLogoBackgroundUrl;
	// Additional "Sign in" screen branding logo shown over "Welcome to Cobalt" title text, e.g. if customer is whitelabeling
	@Nullable
	private String signInBrandingLogoUrl;
	// e.g. "Welcome to Cobalt"
	@Nullable
	private String signInTitle;
	// e.g. "Cobalt is a mental health and wellness platform created for [Institution name] faculty and staff"
	@Nullable
	private String signInDescription;
	// e.g. "Select your sign in method to continue." or "Click 'Sign In With MyChart' below, then enter your details to sign in."
	@Nullable
	private String signInDirection;
	// Whether the "Crisis support" button on the sign-in screen is visible
	@Nullable
	private Boolean signInCrisisButtonVisible;
	// Whether the "If you are in crisis" box on the sign-in screen is visible
	@Nullable
	private Boolean signInCrisisSectionVisible;
	// If there is a marketing video to be shown on the sign-in screen
	@Nullable
	private UUID signInVideoId;
	// Copy to show on the "play video" button, e.g. "Watch our video"
	@Nullable
	private String signInVideoCta;

	// If signInPrivacyOverview is present, then show the "About your privacy" box on the sign-in screen.
	// If signInPrivacyDetail is present, then show the "Learn more about your privacy" link in the "privacy" box.
	// Both can include HTML.
	@Nullable
	private String signInPrivacyOverview;
	@Nullable
	private String signInPrivacyDetail;

	// Should we show the "personalized quote" area of the homepage (bubble with headshot and blurb)?
	@Nullable
	private Boolean signInQuoteVisible;
	// e.g. "Welcome to Cobalt"
	@Nullable
	private String signInQuoteTitle;
	// e.g. "Hi! I'm Dr. Example Person, the Director of Cobalt. I am a Clinical Psychologist and Clinical Assistant...".  Can include HTML
	@Nullable
	private String signInQuoteBlurb;
	// e.g. the rest of the blurb above, if applicable.  Can include HTML.  If this is non-null, a "Read More" link should be shown and this copy is rendered in a modal.
	@Nullable
	private String signInQuoteDetail;

	public enum InstitutionId {
		COBALT,
		COBALT_IC,
		COBALT_IC_SELF_REFERRAL,
		COBALT_FHIR,
		COBALT_COURSES
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
	public String getEpicPatientEncounterCsnSystem() {
		return this.epicPatientEncounterCsnSystem;
	}

	public void setEpicPatientEncounterCsnSystem(@Nullable String epicPatientEncounterCsnSystem) {
		this.epicPatientEncounterCsnSystem = epicPatientEncounterCsnSystem;
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
	public String getEpicPatientMrnTypeName() {
		return this.epicPatientMrnTypeName;
	}

	public void setEpicPatientMrnTypeName(@Nullable String epicPatientMrnTypeName) {
		this.epicPatientMrnTypeName = epicPatientMrnTypeName;
	}

	@Nullable
	public String getEpicPatientMrnTypeAlternateName() {
		return this.epicPatientMrnTypeAlternateName;
	}

	public void setEpicPatientMrnTypeAlternateName(@Nullable String epicPatientMrnTypeAlternateName) {
		this.epicPatientMrnTypeAlternateName = epicPatientMrnTypeAlternateName;
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
	public UUID getFeaturedSecondaryTopicCenterId() {
		return this.featuredSecondaryTopicCenterId;
	}

	public void setFeaturedSecondaryTopicCenterId(@Nullable UUID featuredSecondaryTopicCenterId) {
		this.featuredSecondaryTopicCenterId = featuredSecondaryTopicCenterId;
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

	public String getSecureFilesharingPlatformName() {
		return this.secureFilesharingPlatformName;
	}

	public void setSecureFilesharingPlatformName(@Nullable String secureFilesharingPlatformName) {
		this.secureFilesharingPlatformName = secureFilesharingPlatformName;
	}

	@Nullable
	public String getSecureFilesharingPlatformUrl() {
		return this.secureFilesharingPlatformUrl;
	}

	public void setSecureFilesharingPlatformUrl(@Nullable String secureFilesharingPlatformUrl) {
		this.secureFilesharingPlatformUrl = secureFilesharingPlatformUrl;
	}

	@Nullable
	public String getGoogleReportingServiceAccountPrivateKey() {
		return this.googleReportingServiceAccountPrivateKey;
	}

	public void setGoogleReportingServiceAccountPrivateKey(@Nullable String googleReportingServiceAccountPrivateKey) {
		this.googleReportingServiceAccountPrivateKey = googleReportingServiceAccountPrivateKey;
	}

	@Nullable
	public String getGoogleGa4PropertyId() {
		return this.googleGa4PropertyId;
	}

	public void setGoogleGa4PropertyId(@Nullable String googleGa4PropertyId) {
		this.googleGa4PropertyId = googleGa4PropertyId;
	}

	@Nullable
	public String getGoogleBigQueryResourceId() {
		return this.googleBigQueryResourceId;
	}

	public void setGoogleBigQueryResourceId(@Nullable String googleBigQueryResourceId) {
		this.googleBigQueryResourceId = googleBigQueryResourceId;
	}

	@Nullable
	public Boolean getGoogleBigQuerySyncEnabled() {
		return this.googleBigQuerySyncEnabled;
	}

	public void setGoogleBigQuerySyncEnabled(@Nullable Boolean googleBigQuerySyncEnabled) {
		this.googleBigQuerySyncEnabled = googleBigQuerySyncEnabled;
	}

	@Nullable
	public LocalDate getGoogleBigQuerySyncStartsAt() {
		return this.googleBigQuerySyncStartsAt;
	}

	public void setGoogleBigQuerySyncStartsAt(@Nullable LocalDate googleBigQuerySyncStartsAt) {
		this.googleBigQuerySyncStartsAt = googleBigQuerySyncStartsAt;
	}

	@Nullable
	public Long getMixpanelProjectId() {
		return this.mixpanelProjectId;
	}

	public void setMixpanelProjectId(@Nullable Long mixpanelProjectId) {
		this.mixpanelProjectId = mixpanelProjectId;
	}

	@Nullable
	public String getMixpanelServiceAccountUsername() {
		return this.mixpanelServiceAccountUsername;
	}

	public void setMixpanelServiceAccountUsername(@Nullable String mixpanelServiceAccountUsername) {
		this.mixpanelServiceAccountUsername = mixpanelServiceAccountUsername;
	}

	@Nullable
	public String getMixpanelServiceAccountSecret() {
		return this.mixpanelServiceAccountSecret;
	}

	public void setMixpanelServiceAccountSecret(@Nullable String mixpanelServiceAccountSecret) {
		this.mixpanelServiceAccountSecret = mixpanelServiceAccountSecret;
	}

	@Nullable
	public Boolean getMixpanelSyncEnabled() {
		return this.mixpanelSyncEnabled;
	}

	public void setMixpanelSyncEnabled(@Nullable Boolean mixpanelSyncEnabled) {
		this.mixpanelSyncEnabled = mixpanelSyncEnabled;
	}

	@Nullable
	public LocalDate getMixpanelSyncStartsAt() {
		return this.mixpanelSyncStartsAt;
	}

	public void setMixpanelSyncStartsAt(@Nullable LocalDate mixpanelSyncStartsAt) {
		this.mixpanelSyncStartsAt = mixpanelSyncStartsAt;
	}

	@Nullable
	public Boolean getGoogleFcmPushNotificationsEnabled() {
		return this.googleFcmPushNotificationsEnabled;
	}

	public void setGoogleFcmPushNotificationsEnabled(@Nullable Boolean googleFcmPushNotificationsEnabled) {
		this.googleFcmPushNotificationsEnabled = googleFcmPushNotificationsEnabled;
	}

	@Nullable
	public String getIntegratedCareOrderImportBucketName() {
		return this.integratedCareOrderImportBucketName;
	}

	public void setIntegratedCareOrderImportBucketName(@Nullable String integratedCareOrderImportBucketName) {
		this.integratedCareOrderImportBucketName = integratedCareOrderImportBucketName;
	}

	@Nullable
	public Boolean getMicrosoftTeamsEnabled() {
		return this.microsoftTeamsEnabled;
	}

	public void setMicrosoftTeamsEnabled(@Nullable Boolean microsoftTeamsEnabled) {
		this.microsoftTeamsEnabled = microsoftTeamsEnabled;
	}

	@Nullable
	public String getMicrosoftTeamsTenantId() {
		return this.microsoftTeamsTenantId;
	}

	public void setMicrosoftTeamsTenantId(@Nullable String microsoftTeamsTenantId) {
		this.microsoftTeamsTenantId = microsoftTeamsTenantId;
	}

	@Nullable
	public String getMicrosoftTeamsClientId() {
		return this.microsoftTeamsClientId;
	}

	public void setMicrosoftTeamsClientId(@Nullable String microsoftTeamsClientId) {
		this.microsoftTeamsClientId = microsoftTeamsClientId;
	}

	@Nullable
	public String getMicrosoftTeamsUserId() {
		return this.microsoftTeamsUserId;
	}

	public void setMicrosoftTeamsUserId(@Nullable String microsoftTeamsUserId) {
		this.microsoftTeamsUserId = microsoftTeamsUserId;
	}

	@Nullable
	public Boolean getTableauEnabled() {
		return this.tableauEnabled;
	}

	public void setTableauEnabled(@Nullable Boolean tableauEnabled) {
		this.tableauEnabled = tableauEnabled;
	}

	@Nullable
	public String getTableauClientId() {
		return this.tableauClientId;
	}

	public void setTableauClientId(@Nullable String tableauClientId) {
		this.tableauClientId = tableauClientId;
	}

	@Nullable
	public String getTableauApiBaseUrl() {
		return this.tableauApiBaseUrl;
	}

	public void setTableauApiBaseUrl(@Nullable String tableauApiBaseUrl) {
		this.tableauApiBaseUrl = tableauApiBaseUrl;
	}

	@Nullable
	public String getTableauContentUrl() {
		return this.tableauContentUrl;
	}

	public void setTableauContentUrl(@Nullable String tableauContentUrl) {
		this.tableauContentUrl = tableauContentUrl;
	}

	@Nullable
	public String getTableauViewName() {
		return this.tableauViewName;
	}

	public void setTableauViewName(@Nullable String tableauViewName) {
		this.tableauViewName = tableauViewName;
	}

	@Nullable
	public String getTableauReportName() {
		return this.tableauReportName;
	}

	public void setTableauReportName(@Nullable String tableauReportName) {
		this.tableauReportName = tableauReportName;
	}

	@Nullable
	public String getTableauEmailAddress() {
		return this.tableauEmailAddress;
	}

	public void setTableauEmailAddress(@Nullable String tableauEmailAddress) {
		this.tableauEmailAddress = tableauEmailAddress;
	}

	@Nullable
	public UUID getIntegratedCareSafetyPlanningManagerAccountId() {
		return this.integratedCareSafetyPlanningManagerAccountId;
	}

	public void setIntegratedCareSafetyPlanningManagerAccountId(@Nullable UUID integratedCareSafetyPlanningManagerAccountId) {
		this.integratedCareSafetyPlanningManagerAccountId = integratedCareSafetyPlanningManagerAccountId;
	}

	@Nullable
	public Integer getIntegratedCareOrderImportDelayInSeconds() {
		return this.integratedCareOrderImportDelayInSeconds;
	}

	public void setIntegratedCareOrderImportDelayInSeconds(@Nullable Integer integratedCareOrderImportDelayInSeconds) {
		this.integratedCareOrderImportDelayInSeconds = integratedCareOrderImportDelayInSeconds;
	}

	@Nullable
	public String getTwilioAccountSid() {
		return this.twilioAccountSid;
	}

	public void setTwilioAccountSid(@Nullable String twilioAccountSid) {
		this.twilioAccountSid = twilioAccountSid;
	}

	@Nullable
	public String getTwilioFromNumber() {
		return this.twilioFromNumber;
	}

	public void setTwilioFromNumber(@Nullable String twilioFromNumber) {
		this.twilioFromNumber = twilioFromNumber;
	}

	@Nullable
	public String getTwilioMessagingServiceSid() {
		return this.twilioMessagingServiceSid;
	}

	public void setTwilioMessagingServiceSid(@Nullable String twilioMessagingServiceSid) {
		this.twilioMessagingServiceSid = twilioMessagingServiceSid;
	}

	@Nullable
	public Boolean getCallMessagesEnabled() {
		return this.callMessagesEnabled;
	}

	public void setCallMessagesEnabled(@Nullable Boolean callMessagesEnabled) {
		this.callMessagesEnabled = callMessagesEnabled;
	}

	@Nullable
	public Boolean getSmsMessagesEnabled() {
		return this.smsMessagesEnabled;
	}

	public void setSmsMessagesEnabled(@Nullable Boolean smsMessagesEnabled) {
		this.smsMessagesEnabled = smsMessagesEnabled;
	}

	@Nullable
	public LocalTime getIntegratedCareOrderImportStartTimeWindow() {
		return this.integratedCareOrderImportStartTimeWindow;
	}

	public void setIntegratedCareOrderImportStartTimeWindow(@Nullable LocalTime integratedCareOrderImportStartTimeWindow) {
		this.integratedCareOrderImportStartTimeWindow = integratedCareOrderImportStartTimeWindow;
	}

	@Nullable
	public LocalTime getIntegratedCareOrderImportEndTimeWindow() {
		return this.integratedCareOrderImportEndTimeWindow;
	}

	public void setIntegratedCareOrderImportEndTimeWindow(@Nullable LocalTime integratedCareOrderImportEndTimeWindow) {
		this.integratedCareOrderImportEndTimeWindow = integratedCareOrderImportEndTimeWindow;
	}

	@Nullable
	public Boolean getEpicProviderSlotBookingSyncEnabled() {
		return this.epicProviderSlotBookingSyncEnabled;
	}

	public void setEpicProviderSlotBookingSyncEnabled(@Nullable Boolean epicProviderSlotBookingSyncEnabled) {
		this.epicProviderSlotBookingSyncEnabled = epicProviderSlotBookingSyncEnabled;
	}

	@Nullable
	public String getEpicProviderSlotBookingSyncContactIdType() {
		return this.epicProviderSlotBookingSyncContactIdType;
	}

	public void setEpicProviderSlotBookingSyncContactIdType(@Nullable String epicProviderSlotBookingSyncContactIdType) {
		this.epicProviderSlotBookingSyncContactIdType = epicProviderSlotBookingSyncContactIdType;
	}

	@Nullable
	public String getEpicProviderSlotBookingSyncDepartmentIdType() {
		return this.epicProviderSlotBookingSyncDepartmentIdType;
	}

	public void setEpicProviderSlotBookingSyncDepartmentIdType(@Nullable String epicProviderSlotBookingSyncDepartmentIdType) {
		this.epicProviderSlotBookingSyncDepartmentIdType = epicProviderSlotBookingSyncDepartmentIdType;
	}

	@Nullable
	public String getEpicProviderSlotBookingSyncVisitTypeIdType() {
		return this.epicProviderSlotBookingSyncVisitTypeIdType;
	}

	public void setEpicProviderSlotBookingSyncVisitTypeIdType(@Nullable String epicProviderSlotBookingSyncVisitTypeIdType) {
		this.epicProviderSlotBookingSyncVisitTypeIdType = epicProviderSlotBookingSyncVisitTypeIdType;
	}

	@Nullable
	public Boolean getAppointmentFeedbackSurveyEnabled() {
		return this.appointmentFeedbackSurveyEnabled;
	}

	public void setAppointmentFeedbackSurveyEnabled(@Nullable Boolean appointmentFeedbackSurveyEnabled) {
		this.appointmentFeedbackSurveyEnabled = appointmentFeedbackSurveyEnabled;
	}

	@Nullable
	public String getAppointmentFeedbackSurveyUrl() {
		return this.appointmentFeedbackSurveyUrl;
	}

	public void setAppointmentFeedbackSurveyUrl(@Nullable String appointmentFeedbackSurveyUrl) {
		this.appointmentFeedbackSurveyUrl = appointmentFeedbackSurveyUrl;
	}

	@Nullable
	public Integer getAppointmentFeedbackSurveyDelayInMinutes() {
		return this.appointmentFeedbackSurveyDelayInMinutes;
	}

	public void setAppointmentFeedbackSurveyDelayInMinutes(@Nullable Integer appointmentFeedbackSurveyDelayInMinutes) {
		this.appointmentFeedbackSurveyDelayInMinutes = appointmentFeedbackSurveyDelayInMinutes;
	}

	@Nullable
	public String getAppointmentFeedbackSurveyDurationDescription() {
		return this.appointmentFeedbackSurveyDurationDescription;
	}

	public void setAppointmentFeedbackSurveyDurationDescription(@Nullable String appointmentFeedbackSurveyDurationDescription) {
		this.appointmentFeedbackSurveyDurationDescription = appointmentFeedbackSurveyDurationDescription;
	}

	@Nullable
	public Boolean getGoogleGeoEnabled() {
		return this.googleGeoEnabled;
	}

	public void setGoogleGeoEnabled(@Nullable Boolean googleGeoEnabled) {
		this.googleGeoEnabled = googleGeoEnabled;
	}

	@Nullable
	public Boolean getContentAudiencesEnabled() {
		return this.contentAudiencesEnabled;
	}

	public void setContentAudiencesEnabled(@Nullable Boolean contentAudiencesEnabled) {
		this.contentAudiencesEnabled = contentAudiencesEnabled;
	}

	@Nullable
	public Boolean getResourcePacketsEnabled() {
		return resourcePacketsEnabled;
	}

	public void setResourcePacketsEnabled(@Nullable Boolean resourcePacketsEnabled) {
		this.resourcePacketsEnabled = resourcePacketsEnabled;
	}


	public Boolean getIntegratedCarePatientDemographicsRequired() {
		return this.integratedCarePatientDemographicsRequired;
	}

	public void setIntegratedCarePatientDemographicsRequired(@Nullable Boolean integratedCarePatientDemographicsRequired) {
		this.integratedCarePatientDemographicsRequired = integratedCarePatientDemographicsRequired;
	}

	@Nullable
	public Boolean getIntegratedCarePatientCarePreferenceVisible() {
		return this.integratedCarePatientCarePreferenceVisible;
	}

	public void setIntegratedCarePatientCarePreferenceVisible(@Nullable Boolean integratedCarePatientCarePreferenceVisible) {
		this.integratedCarePatientCarePreferenceVisible = integratedCarePatientCarePreferenceVisible;
	}

	@Nullable
	public String getIntegratedCareCallCenterName() {
		return this.integratedCareCallCenterName;
	}

	public void setIntegratedCareCallCenterName(@Nullable String integratedCareCallCenterName) {
		this.integratedCareCallCenterName = integratedCareCallCenterName;
	}

	@Nullable
	public String getIntegratedCareMhpTriageOverviewOverride() {
		return this.integratedCareMhpTriageOverviewOverride;
	}

	public void setIntegratedCareMhpTriageOverviewOverride(@Nullable String integratedCareMhpTriageOverviewOverride) {
		this.integratedCareMhpTriageOverviewOverride = integratedCareMhpTriageOverviewOverride;
	}

	@Nullable
	public String getIntegratedCareBookingInsuranceRequirements() {
		return this.integratedCareBookingInsuranceRequirements;
	}

	public void setIntegratedCareBookingInsuranceRequirements(@Nullable String integratedCareBookingInsuranceRequirements) {
		this.integratedCareBookingInsuranceRequirements = integratedCareBookingInsuranceRequirements;
	}

	@Nullable
	public String getIntegratedCarePatientIntroOverride() {
		return this.integratedCarePatientIntroOverride;
	}

	public void setIntegratedCarePatientIntroOverride(@Nullable String integratedCarePatientIntroOverride) {
		this.integratedCarePatientIntroOverride = integratedCarePatientIntroOverride;
	}

	@Nullable
	public String getLandingPageTaglineOverride() {
		return this.landingPageTaglineOverride;
	}

	public void setLandingPageTaglineOverride(@Nullable String landingPageTaglineOverride) {
		this.landingPageTaglineOverride = landingPageTaglineOverride;
	}

	@Nonnull
	public Boolean getPreferLegacyTopicCenters() {
		return this.preferLegacyTopicCenters;
	}

	public void setPreferLegacyTopicCenters(@Nullable Boolean preferLegacyTopicCenters) {
		this.preferLegacyTopicCenters = preferLegacyTopicCenters;
	}

	@Nullable
	public UUID getOnboardingScreeningFlowId() {
		return this.onboardingScreeningFlowId;
	}

	public void setOnboardingScreeningFlowId(@Nullable UUID onboardingScreeningFlowId) {
		this.onboardingScreeningFlowId = onboardingScreeningFlowId;
	}

	@Nullable
	public String getAnonymousImplicitUrlPathRegex() {
		return this.anonymousImplicitUrlPathRegex;
	}

	public void setAnonymousImplicitUrlPathRegex(@Nullable String anonymousImplicitUrlPathRegex) {
		this.anonymousImplicitUrlPathRegex = anonymousImplicitUrlPathRegex;
	}

	@Nullable
	public Instant getAnonymousImplicitAccessTokensValidAfter() {
		return this.anonymousImplicitAccessTokensValidAfter;
	}

	public void setAnonymousImplicitAccessTokensValidAfter(@Nullable Instant anonymousImplicitAccessTokensValidAfter) {
		this.anonymousImplicitAccessTokensValidAfter = anonymousImplicitAccessTokensValidAfter;
	}

	@Nullable
	public String getHeaderLogoUrl() {
		return this.headerLogoUrl;
	}

	public void setHeaderLogoUrl(@Nullable String headerLogoUrl) {
		this.headerLogoUrl = headerLogoUrl;
	}

	@Nullable
	public String getFooterLogoUrl() {
		return this.footerLogoUrl;
	}

	public void setFooterLogoUrl(@Nullable String footerLogoUrl) {
		this.footerLogoUrl = footerLogoUrl;
	}

	@Nullable
	public String getHeroTitle() {
		return this.heroTitle;
	}

	public void setHeroTitle(@Nullable String heroTitle) {
		this.heroTitle = heroTitle;
	}

	@Nullable
	public String getHeroDescription() {
		return this.heroDescription;
	}

	public void setHeroDescription(@Nullable String heroDescription) {
		this.heroDescription = heroDescription;
	}

	@Nullable
	public String getHeroImageUrl() {
		return this.heroImageUrl;
	}

	public void setHeroImageUrl(@Nullable String heroImageUrl) {
		this.heroImageUrl = heroImageUrl;
	}

	@Nullable
	public String getSignInLogoUrl() {
		return this.signInLogoUrl;
	}

	public void setSignInLogoUrl(@Nullable String signInLogoUrl) {
		this.signInLogoUrl = signInLogoUrl;
	}

	@Nullable
	public String getSignInLargeLogoUrl() {
		return this.signInLargeLogoUrl;
	}

	public void setSignInLargeLogoUrl(@Nullable String signInLargeLogoUrl) {
		this.signInLargeLogoUrl = signInLargeLogoUrl;
	}

	@Nullable
	public String getSignInLargeLogoBackgroundUrl() {
		return this.signInLargeLogoBackgroundUrl;
	}

	public void setSignInLargeLogoBackgroundUrl(@Nullable String signInLargeLogoBackgroundUrl) {
		this.signInLargeLogoBackgroundUrl = signInLargeLogoBackgroundUrl;
	}

	@Nullable
	public String getSignInBrandingLogoUrl() {
		return this.signInBrandingLogoUrl;
	}

	public void setSignInBrandingLogoUrl(@Nullable String signInBrandingLogoUrl) {
		this.signInBrandingLogoUrl = signInBrandingLogoUrl;
	}

	@Nullable
	public String getSignInTitle() {
		return this.signInTitle;
	}

	public void setSignInTitle(@Nullable String signInTitle) {
		this.signInTitle = signInTitle;
	}

	@Nullable
	public String getSignInDescription() {
		return this.signInDescription;
	}

	public void setSignInDescription(@Nullable String signInDescription) {
		this.signInDescription = signInDescription;
	}

	@Nullable
	public String getSignInDirection() {
		return this.signInDirection;
	}

	public void setSignInDirection(@Nullable String signInDirection) {
		this.signInDirection = signInDirection;
	}

	@Nullable
	public Boolean getSignInCrisisButtonVisible() {
		return this.signInCrisisButtonVisible;
	}

	public void setSignInCrisisButtonVisible(@Nullable Boolean signInCrisisButtonVisible) {
		this.signInCrisisButtonVisible = signInCrisisButtonVisible;
	}

	@Nullable
	public Boolean getSignInCrisisSectionVisible() {
		return this.signInCrisisSectionVisible;
	}

	public void setSignInCrisisSectionVisible(@Nullable Boolean signInCrisisSectionVisible) {
		this.signInCrisisSectionVisible = signInCrisisSectionVisible;
	}

	@Nullable
	public UUID getSignInVideoId() {
		return this.signInVideoId;
	}

	public void setSignInVideoId(@Nullable UUID signInVideoId) {
		this.signInVideoId = signInVideoId;
	}

	@Nullable
	public String getSignInVideoCta() {
		return this.signInVideoCta;
	}

	public void setSignInVideoCta(@Nullable String signInVideoCta) {
		this.signInVideoCta = signInVideoCta;
	}

	@Nullable
	public String getSignInPrivacyOverview() {
		return this.signInPrivacyOverview;
	}

	public void setSignInPrivacyOverview(@Nullable String signInPrivacyOverview) {
		this.signInPrivacyOverview = signInPrivacyOverview;
	}

	@Nullable
	public String getSignInPrivacyDetail() {
		return this.signInPrivacyDetail;
	}

	public void setSignInPrivacyDetail(@Nullable String signInPrivacyDetail) {
		this.signInPrivacyDetail = signInPrivacyDetail;
	}

	@Nullable
	public Boolean getSignInQuoteVisible() {
		return this.signInQuoteVisible;
	}

	public void setSignInQuoteVisible(@Nullable Boolean signInQuoteVisible) {
		this.signInQuoteVisible = signInQuoteVisible;
	}

	@Nullable
	public String getSignInQuoteTitle() {
		return this.signInQuoteTitle;
	}

	public void setSignInQuoteTitle(@Nullable String signInQuoteTitle) {
		this.signInQuoteTitle = signInQuoteTitle;
	}

	@Nullable
	public String getSignInQuoteBlurb() {
		return this.signInQuoteBlurb;
	}

	public void setSignInQuoteBlurb(@Nullable String signInQuoteBlurb) {
		this.signInQuoteBlurb = signInQuoteBlurb;
	}

	@Nullable
	public String getSignInQuoteDetail() {
		return this.signInQuoteDetail;
	}

	public void setSignInQuoteDetail(@Nullable String signInQuoteDetail) {
		this.signInQuoteDetail = signInQuoteDetail;
	}
}