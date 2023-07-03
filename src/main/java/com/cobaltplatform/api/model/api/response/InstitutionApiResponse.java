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
import com.cobaltplatform.api.model.api.response.AlertApiResponse.AlertApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AnonymousAccountExpirationStrategy.AnonymousAccountExpirationStrategyId;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.ProviderBookingFlowType;
import com.cobaltplatform.api.model.db.UserExperienceType.UserExperienceTypeId;
import com.cobaltplatform.api.model.service.FeaturesForInstitution;
import com.cobaltplatform.api.model.service.NavigationItem;
import com.cobaltplatform.api.service.AlertService;
import com.cobaltplatform.api.service.InstitutionService;
import com.cobaltplatform.api.service.ScreeningService;
import com.cobaltplatform.api.service.TopicCenterService;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class InstitutionApiResponse {
	@Nonnull
	private final InstitutionId institutionId;
	@Nullable
	private final UUID providerTriageScreeningFlowId;
	@Nullable
	private final UUID contentScreeningFlowId;
	@Nullable
	private final UUID groupSessionsScreeningFlowId;
	@Nullable
	private final UUID integratedCareScreeningFlowId;
	@Nullable
	private final UUID featureScreeningFlowId;
	@Nonnull
	private final AnonymousAccountExpirationStrategyId anonymousAccountExpirationStrategyId;
	@Nonnull
	private final String name;
	@Nullable
	@Deprecated
	private final String crisisContent;
	@Nullable
	@Deprecated
	private final String privacyContent;
	@Nullable
	@Deprecated
	private final String covidContent;
	@Nullable
	private final Boolean requireConsentForm;
	@Nullable
	@Deprecated
	private final String consentFormContent;
	@Nullable
	private final String calendarDescription;
	@Nullable
	private final Boolean supportEnabled;
	@Nullable
	@Deprecated
	private final String wellBeingContent;
	@Nullable
	@Deprecated
	private final Boolean ssoEnabled;
	@Nullable
	@Deprecated
	private final Boolean emailEnabled;
	@Nonnull
	private final Boolean emailSignupEnabled;
	@Nullable
	@Deprecated
	private final Boolean anonymousEnabled;
	@Nonnull
	private final Boolean integratedCareEnabled;
	@Nonnull
	private final String supportEmailAddress;
	@Nonnull
	private final Boolean immediateAccessEnabled;
	@Nonnull
	private final Boolean contactUsEnabled;
	@Nullable
	private final Boolean featuresEnabled;
	@Nonnull
	private final Boolean recommendedContentEnabled;
	@Nonnull
	private final Boolean groupSessionRequestsEnabled;
	@Nonnull
	private final Boolean userSubmittedContentEnabled;
	@Nonnull
	private final Boolean userSubmittedGroupSessionEnabled;
	@Nonnull
	private final Boolean userSubmittedGroupSessionRequestEnabled;
	@Nullable
	private final String ga4MeasurementId;
	@Nonnull
	private final String patientUserExperienceBaseUrl;
	@Nonnull
	private final String staffUserExperienceBaseUrl;
	@Nullable
	private final String integratedCarePhoneNumber;
	@Nullable
	private final String integratedCarePhoneNumberDescription;
	@Nullable
	private final String integratedCareAvailabilityDescription;
	@Nullable
	private final String integratedCareProgramName;
	@Nullable
	private final String integratedCarePrimaryCareName;
	@Nullable
	private final String myChartName;
	@Nullable
	private final String myChartDefaultUrl;
	@Nonnull
	private final List<NavigationItem> additionalNavigationItems;
	@Nonnull
	private final List<FeaturesForInstitution> features;
	@Nonnull
	private final Boolean takeFeatureScreening;
	@Nonnull
	private final Boolean hasTakenFeatureScreening;
	@Nonnull
	private final UserExperienceTypeId userExperienceTypeId;
	@Nullable
	private final String clinicalSupportPhoneNumber;
	@Nullable
	private final String clinicalSupportPhoneNumberDescription;
	@Nonnull
	private final List<AlertApiResponse> alerts;
	@Nonnull
	private final ProviderBookingFlowType providerBookingFlowTypeId;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface InstitutionApiResponseFactory {
		@Nonnull
		InstitutionApiResponse create(@Nonnull Institution institution,
																	@Nonnull CurrentContext currentContext);
	}

	@AssistedInject
	public InstitutionApiResponse(@Nonnull AlertApiResponseFactory alertApiResponseFactory,
																@Nonnull AlertService alertService,
																@Nonnull TopicCenterService topicCenterService,
																@Nonnull InstitutionService institutionService,
																@Nonnull ScreeningService screeningService,
																@Nonnull Formatter formatter,
																@Nonnull Strings strings,
																@Assisted @Nonnull Institution institution,
																@Assisted @Nonnull CurrentContext currentContext) {
		requireNonNull(alertApiResponseFactory);
		requireNonNull(alertService);
		requireNonNull(topicCenterService);
		requireNonNull(institutionService);
		requireNonNull(screeningService);
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(institution);
		requireNonNull(currentContext);

		Account account = currentContext.getAccount().orElse(null);

		// TODO: we are "blanking out" some fields until FE can transition away from using them.
		// This is to provide backwards compatibility for JS clients, so they don't blow up when BE is updated.
		// In the future, we will remove these entirely.

		this.institutionId = institution.getInstitutionId();
		this.providerTriageScreeningFlowId = institution.getProviderTriageScreeningFlowId();
		this.contentScreeningFlowId = institution.getContentScreeningFlowId();
		this.groupSessionsScreeningFlowId = institution.getGroupSessionsScreeningFlowId();
		this.integratedCareScreeningFlowId = institution.getIntegratedCareScreeningFlowId();
		this.featureScreeningFlowId = institution.getFeatureScreeningFlowId();
		this.anonymousAccountExpirationStrategyId = institution.getAnonymousAccountExpirationStrategyId();
		this.name = institution.getName();
		this.crisisContent = ""; // institution.getCrisisContent();
		this.privacyContent = ""; // institution.getPrivacyContent();
		this.covidContent = ""; // institution.getCovidContent();
		this.requireConsentForm = institution.getRequireConsentForm();
		this.consentFormContent = ""; // institution.getConsentFormContent();
		this.calendarDescription = institution.getCalendarDescription();
		this.supportEnabled = institution.getSupportEnabled();
		this.wellBeingContent = ""; // institution.getWellBeingContent();
		this.ssoEnabled = false; // institution.getSsoEnabled();
		this.anonymousEnabled = false; // institution.getAnonymousEnabled();
		this.emailEnabled = false; // institution.getEmailEnabled();
		this.emailSignupEnabled = institution.getEmailSignupEnabled();
		this.supportEmailAddress = institution.getSupportEmailAddress();
		this.immediateAccessEnabled = institution.getImmediateAccessEnabled();
		this.contactUsEnabled = institution.getContactUsEnabled();
		this.featuresEnabled = institution.getFeaturesEnabled();
		this.recommendedContentEnabled = institution.getRecommendedContentEnabled();
		this.userSubmittedContentEnabled = institution.getUserSubmittedContentEnabled();
		this.userSubmittedGroupSessionEnabled = institution.getUserSubmittedGroupSessionEnabled();
		this.userSubmittedGroupSessionRequestEnabled = institution.getUserSubmittedGroupSessionRequestEnabled();
		this.integratedCareEnabled = institution.getIntegratedCareEnabled();
		this.myChartName = institution.getMyChartName();
		this.myChartDefaultUrl = institution.getMyChartDefaultUrl();
		this.groupSessionRequestsEnabled = institution.getGroupSessionRequestsEnabled();
		this.additionalNavigationItems = topicCenterService.findTopicCenterNavigationItemsByInstitutionId(institutionId);
		this.features = institutionService.findFeaturesByInstitutionId(institution, account);
		this.takeFeatureScreening = screeningService.shouldAccountIdTakeScreeningFlowId(account, institution.getFeatureScreeningFlowId());
		this.hasTakenFeatureScreening = screeningService.hasAccountIdTakenScreeningFlowId(account, institution.getFeatureScreeningFlowId());

		// TODO: would be better to error out here if no value, providing a failsafe temporarily.
		// to handle cases in prod where we have currently unused domain[s] that can be used to access the FE,
		// but crawlers will attempt to crawl and BE doesn't know what kind of experience type to serve for the domain
		this.userExperienceTypeId = currentContext.getUserExperienceTypeId().orElse(UserExperienceTypeId.PATIENT);

		// Key GA4 measurement ID off of patient vs. staff user experience type
		this.ga4MeasurementId = this.userExperienceTypeId == UserExperienceTypeId.STAFF ? institution.getGa4StaffMeasurementId() : institution.getGa4PatientMeasurementId();

		// UI needs both fields available to it because one experience might link to another, e.g. IC staff sign-in screen links to patient experience
		this.patientUserExperienceBaseUrl = institutionService.findWebappBaseUrlByInstitutionIdAndUserExperienceTypeId(institutionId, UserExperienceTypeId.PATIENT).get();
		this.staffUserExperienceBaseUrl = institutionService.findWebappBaseUrlByInstitutionIdAndUserExperienceTypeId(institutionId, UserExperienceTypeId.STAFF).get();

		this.integratedCarePhoneNumber = institution.getIntegratedCarePhoneNumber();
		this.integratedCarePhoneNumberDescription = institution.getIntegratedCarePhoneNumber() == null ? null : formatter.formatPhoneNumber(institution.getIntegratedCarePhoneNumber());
		this.integratedCareAvailabilityDescription = institution.getIntegratedCareAvailabilityDescription();
		this.integratedCareProgramName = institution.getIntegratedCareProgramName();
		this.integratedCarePrimaryCareName = institution.getIntegratedCarePrimaryCareName();

		this.clinicalSupportPhoneNumber = institution.getClinicalSupportPhoneNumber();
		this.clinicalSupportPhoneNumberDescription = institution.getClinicalSupportPhoneNumber() == null ? null : formatter.formatPhoneNumber(institution.getClinicalSupportPhoneNumber());

		if (account == null) {
			this.alerts = alertService.findAlertsByInstitutionId(institution.getInstitutionId()).stream()
					.map(alert -> alertApiResponseFactory.create(alert))
					.collect(Collectors.toList());
		} else {
			this.alerts = alertService.findUndismissedInstitutionAlertsByAccountId(account.getAccountId()).stream()
					.map(alert -> alertApiResponseFactory.create(alert))
					.collect(Collectors.toList());
		}

		this.providerBookingFlowTypeId = institution.getProviderBookingFlowTypeId();
	}

	@Nonnull
	public InstitutionId getInstitutionId() {
		return this.institutionId;
	}

	@Nullable
	public UUID getProviderTriageScreeningFlowId() {
		return this.providerTriageScreeningFlowId;
	}

	@Nullable
	public UUID getContentScreeningFlowId() {
		return this.contentScreeningFlowId;
	}

	@Nullable
	public UUID getGroupSessionsScreeningFlowId() {
		return this.groupSessionsScreeningFlowId;
	}

	@Nullable
	public UUID getIntegratedCareScreeningFlowId() {
		return this.integratedCareScreeningFlowId;
	}

	@Nullable
	public UUID getFeatureScreeningFlowId() {
		return this.featureScreeningFlowId;
	}

	@Nonnull
	public AnonymousAccountExpirationStrategyId getAnonymousAccountExpirationStrategyId() {
		return this.anonymousAccountExpirationStrategyId;
	}

	@Nonnull
	public String getName() {
		return this.name;
	}

	@Nullable
	@Deprecated
	public String getCrisisContent() {
		return this.crisisContent;
	}

	@Nullable
	@Deprecated
	public String getPrivacyContent() {
		return this.privacyContent;
	}

	@Nullable
	@Deprecated
	public String getCovidContent() {
		return this.covidContent;
	}

	@Nullable
	public Boolean getRequireConsentForm() {
		return this.requireConsentForm;
	}

	@Nullable
	@Deprecated
	public String getConsentFormContent() {
		return this.consentFormContent;
	}

	@Nullable
	public String getCalendarDescription() {
		return this.calendarDescription;
	}

	@Nullable
	public Boolean getSupportEnabled() {
		return this.supportEnabled;
	}

	@Nullable
	@Deprecated
	public String getWellBeingContent() {
		return this.wellBeingContent;
	}

	@Nullable
	@Deprecated
	public Boolean getSsoEnabled() {
		return this.ssoEnabled;
	}

	@Nullable
	@Deprecated
	public Boolean getEmailEnabled() {
		return this.emailEnabled;
	}

	@Nullable
	@Deprecated
	public Boolean getAnonymousEnabled() {
		return this.anonymousEnabled;
	}

	@Nonnull
	public Boolean getEmailSignupEnabled() {
		return this.emailSignupEnabled;
	}

	@Nonnull
	public String getSupportEmailAddress() {
		return this.supportEmailAddress;
	}

	@Nonnull
	public Boolean getImmediateAccessEnabled() {
		return this.immediateAccessEnabled;
	}

	@Nonnull
	public Boolean getContactUsEnabled() {
		return this.contactUsEnabled;
	}

	@Nullable
	public Boolean getFeaturesEnabled() {
		return this.featuresEnabled;
	}

	@Nonnull
	public Boolean getRecommendedContentEnabled() {
		return this.recommendedContentEnabled;
	}

	@Nonnull
	public Boolean getUserSubmittedContentEnabled() {
		return this.userSubmittedContentEnabled;
	}

	@Nonnull
	public Boolean getUserSubmittedGroupSessionEnabled() {
		return this.userSubmittedGroupSessionEnabled;
	}

	@Nonnull
	public Boolean getUserSubmittedGroupSessionRequestEnabled() {
		return this.userSubmittedGroupSessionRequestEnabled;
	}

	@Nonnull
	public Boolean getIntegratedCareEnabled() {
		return this.integratedCareEnabled;
	}

	@Nonnull
	public Boolean getGroupSessionRequestsEnabled() {
		return this.groupSessionRequestsEnabled;
	}

	@Nonnull
	public Optional<String> getGa4MeasurementId() {
		return Optional.ofNullable(this.ga4MeasurementId);
	}

	@Nonnull
	public List<NavigationItem> getAdditionalNavigationItems() {
		return this.additionalNavigationItems;
	}

	@Nonnull
	public List<FeaturesForInstitution> getFeatures() {
		return features;
	}

	@Nonnull
	public Boolean getTakeFeatureScreening() {
		return takeFeatureScreening;
	}

	@Nonnull
	public Boolean getHasTakenFeatureScreening() {
		return hasTakenFeatureScreening;
	}

	@Nonnull
	public UserExperienceTypeId getUserExperienceTypeId() {
		return this.userExperienceTypeId;
	}

	@Nonnull
	public List<AlertApiResponse> getAlerts() {
		return this.alerts;
	}

	@Nonnull
	public String getPatientUserExperienceBaseUrl() {
		return this.patientUserExperienceBaseUrl;
	}

	@Nonnull
	public String getStaffUserExperienceBaseUrl() {
		return this.staffUserExperienceBaseUrl;
	}

	@Nullable
	public String getIntegratedCarePhoneNumber() {
		return this.integratedCarePhoneNumber;
	}

	@Nullable
	public String getIntegratedCarePhoneNumberDescription() {
		return this.integratedCarePhoneNumberDescription;
	}

	@Nullable
	public String getIntegratedCareAvailabilityDescription() {
		return this.integratedCareAvailabilityDescription;
	}

	@Nullable
	public String getIntegratedCareProgramName() {
		return this.integratedCareProgramName;
	}

	@Nullable
	public String getIntegratedCarePrimaryCareName() {
		return this.integratedCarePrimaryCareName;
	}

	@Nullable
	public String getMyChartName() {
		return this.myChartName;
	}

	@Nullable
	public String getMyChartDefaultUrl() {
		return this.myChartDefaultUrl;
	}

	@Nullable
	public String getClinicalSupportPhoneNumber() {
		return this.clinicalSupportPhoneNumber;
	}

	@Nullable
	public String getClinicalSupportPhoneNumberDescription() {
		return this.clinicalSupportPhoneNumberDescription;
	}

	@Nonnull
	public ProviderBookingFlowType getProviderBookingFlowTypeId() {
		return this.providerBookingFlowTypeId;
	}
}