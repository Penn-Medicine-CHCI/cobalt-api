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

import com.cobaltplatform.api.model.db.AdministrativeGender.AdministrativeGenderId;
import com.cobaltplatform.api.model.db.BirthSex.BirthSexId;
import com.cobaltplatform.api.model.db.ClinicalSex.ClinicalSexId;
import com.cobaltplatform.api.model.db.DistanceUnit.DistanceUnitId;
import com.cobaltplatform.api.model.db.Ethnicity.EthnicityId;
import com.cobaltplatform.api.model.db.GenderIdentity.GenderIdentityId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.LegalSex.LegalSexId;
import com.cobaltplatform.api.model.db.PatientOrderCarePreference.PatientOrderCarePreferenceId;
import com.cobaltplatform.api.model.db.PatientOrderClosureReason.PatientOrderClosureReasonId;
import com.cobaltplatform.api.model.db.PatientOrderConsentStatus.PatientOrderConsentStatusId;
import com.cobaltplatform.api.model.db.PatientOrderDemographicsImportStatus.PatientOrderDemographicsImportStatusId;
import com.cobaltplatform.api.model.db.PatientOrderDisposition.PatientOrderDispositionId;
import com.cobaltplatform.api.model.db.PatientOrderIntakeInsuranceStatus.PatientOrderIntakeInsuranceStatusId;
import com.cobaltplatform.api.model.db.PatientOrderIntakeLocationStatus.PatientOrderIntakeLocationStatusId;
import com.cobaltplatform.api.model.db.PatientOrderIntakeWantsServicesStatus.PatientOrderIntakeWantsServicesStatusId;
import com.cobaltplatform.api.model.db.PatientOrderResourceCheckInResponseStatus.PatientOrderResourceCheckInResponseStatusId;
import com.cobaltplatform.api.model.db.PatientOrderResourcingStatus.PatientOrderResourcingStatusId;
import com.cobaltplatform.api.model.db.PatientOrderResourcingType.PatientOrderResourcingTypeId;
import com.cobaltplatform.api.model.db.PatientOrderSafetyPlanningStatus.PatientOrderSafetyPlanningStatusId;
import com.cobaltplatform.api.model.db.PreferredPronoun.PreferredPronounId;
import com.cobaltplatform.api.model.db.Race.RaceId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * @author Transmogrify LLC.
 */
@NotThreadSafe
public class RawPatientOrder {
	// Fields that are in patient_order, not in v_patient_order.
	// See the PatientOrder type for v_patient_order data.
	@Nullable
	private UUID patientOrderId;
	@Nullable
	private Integer referenceNumber;
	@Nullable
	private PatientOrderDispositionId patientOrderDispositionId;
	@Nullable
	private PatientOrderSafetyPlanningStatusId patientOrderSafetyPlanningStatusId;
	@Nullable
	private PatientOrderResourcingStatusId patientOrderResourcingStatusId;
	@Nullable
	private PatientOrderResourcingTypeId patientOrderResourcingTypeId;
	@Nullable
	private UUID patientOrderImportId;
	@Nullable
	private InstitutionId institutionId;
	@Nullable
	private UUID patientAccountId;
	@Nullable
	private UUID patientAddressId;
	@Nullable
	private UUID panelAccountId;
	@Nullable
	private PatientOrderClosureReasonId patientOrderClosureReasonId;
	@Nullable
	private String encounterDepartmentId;
	@Nullable
	private String encounterDepartmentIdType;
	@Nullable
	private String encounterDepartmentName;
	@Nullable
	private String referringPracticeId;
	@Nullable
	private String referringPracticeIdType;
	@Nullable
	private String referringPracticeName;
	@Nullable
	private String orderingProviderId;
	@Nullable
	private String orderingProviderIdType;
	@Nullable
	private String orderingProviderLastName;
	@Nullable
	private String orderingProviderFirstName;
	@Nullable
	private String orderingProviderMiddleName;
	@Nullable
	private String billingProviderId;
	@Nullable
	private String billingProviderIdType;
	@Nullable
	private String billingProviderLastName;
	@Nullable
	private String billingProviderFirstName;
	@Nullable
	private String billingProviderMiddleName;
	@Nullable
	private String patientLastName;
	@Nullable
	private String patientFirstName;
	@Nullable
	private String patientMrn;
	@Nullable
	private String patientUniqueId;
	@Nullable
	private String patientUniqueIdType;
	@Nullable
	private String patientLanguageCode;
	@Nullable
	private LocalDate patientBirthdate;
	@Nullable
	private String patientPhoneNumber;
	@Nullable
	private String patientEmailAddress;
	@Nullable
	private EthnicityId patientEthnicityId;
	@Nullable
	private RaceId patientRaceId;
	@Nullable
	private GenderIdentityId patientGenderIdentityId;
	@Nullable
	private BirthSexId patientBirthSexId;
	@Nullable
	private PreferredPronounId patientPreferredPronounId;
	@Nullable
	private ClinicalSexId patientClinicalSexId;
	@Nullable
	private LegalSexId patientLegalSexId;
	@Nullable
	private AdministrativeGenderId patientAdministrativeGenderId;
	@Nullable
	private PatientOrderDemographicsImportStatusId patientOrderDemographicsImportStatusId;
	@Nullable
	private Instant patientDemographicsImportedAt;
	@Nullable
	private String primaryPayorId;
	@Nullable
	private String primaryPayorName;
	@Nullable
	private String primaryPlanId;
	@Nullable
	private String primaryPlanName;
	@Nullable
	private Boolean primaryPlanAccepted;
	@Nullable
	private LocalDate orderDate;
	@Nullable
	private Integer orderAgeInMinutes;
	@Nullable
	private String orderId;
	@Nullable
	private String routing;
	@Nullable
	private String associatedDiagnosis;
	@Nullable
	private String preferredContactHours;
	@Nullable
	private String comments;
	@Nullable
	private String ccRecipients;
	@Nullable
	private String lastActiveMedicationOrderSummary;
	@Nullable
	private String medications;
	@Nullable
	private String recentPsychotherapeuticMedications;
	@Nullable
	private Instant episodeClosedAt;
	@Nullable
	private UUID episodeClosedByAccountId;
	@Nullable
	private Instant resourcesSentAt;
	@Nullable
	private String resourcesSentNote;
	@Nullable
	private Instant connectedToSafetyPlanningAt;
	@Nullable
	private PatientOrderConsentStatusId patientOrderConsentStatusId;
	@Nullable
	private UUID consentStatusUpdatedByByAccountId;
	@Nullable
	private Instant consentStatusUpdatedAt;
	@Nullable
	private UUID resourceCheckInScheduledMessageGroupId;
	@Nullable
	private PatientOrderResourceCheckInResponseStatusId patientOrderResourceCheckInResponseStatusId;
	@Nullable
	private UUID resourceCheckInResponseStatusUpdatedByByAccountId;
	@Nullable
	private Instant resourceCheckInResponseStatusUpdatedAt;
	@Nullable
	private String testPatientEmailAddress;
	@Nullable
	private String testPatientPassword;
	@Nullable
	private Instant patientDemographicsConfirmedAt;
	@Nullable
	private UUID patientDemographicsConfirmedByAccountId;
	@Nullable
	private PatientOrderCarePreferenceId patientOrderCarePreferenceId;
	@Nullable
	private Integer inPersonCareRadius;
	@Nullable
	private DistanceUnitId inPersonCareRadiusDistanceUnitId;
	@Nullable
	private UUID epicDepartmentId;
	@Nullable
	private String encounterCsn;
	@Nullable
	private Instant encounterSyncedAt;
	@Nullable
	private PatientOrderIntakeWantsServicesStatusId patientOrderIntakeWantsServicesStatusId;
	@Nullable
	private PatientOrderIntakeLocationStatusId patientOrderIntakeLocationStatusId;
	@Nullable
	private PatientOrderIntakeInsuranceStatusId patientOrderIntakeInsuranceStatusId;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nullable
	public UUID getPatientOrderId() {
		return this.patientOrderId;
	}

	public void setPatientOrderId(@Nullable UUID patientOrderId) {
		this.patientOrderId = patientOrderId;
	}

	@Nullable
	public Integer getReferenceNumber() {
		return this.referenceNumber;
	}

	public void setReferenceNumber(@Nullable Integer referenceNumber) {
		this.referenceNumber = referenceNumber;
	}

	@Nullable
	public PatientOrderDispositionId getPatientOrderDispositionId() {
		return this.patientOrderDispositionId;
	}

	public void setPatientOrderDispositionId(@Nullable PatientOrderDispositionId patientOrderDispositionId) {
		this.patientOrderDispositionId = patientOrderDispositionId;
	}

	@Nullable
	public PatientOrderSafetyPlanningStatusId getPatientOrderSafetyPlanningStatusId() {
		return this.patientOrderSafetyPlanningStatusId;
	}

	public void setPatientOrderSafetyPlanningStatusId(@Nullable PatientOrderSafetyPlanningStatusId patientOrderSafetyPlanningStatusId) {
		this.patientOrderSafetyPlanningStatusId = patientOrderSafetyPlanningStatusId;
	}

	@Nullable
	public PatientOrderResourcingStatusId getPatientOrderResourcingStatusId() {
		return this.patientOrderResourcingStatusId;
	}

	public void setPatientOrderResourcingStatusId(@Nullable PatientOrderResourcingStatusId patientOrderResourcingStatusId) {
		this.patientOrderResourcingStatusId = patientOrderResourcingStatusId;
	}

	@Nullable
	public PatientOrderResourcingTypeId getPatientOrderResourcingTypeId() {
		return this.patientOrderResourcingTypeId;
	}

	public void setPatientOrderResourcingTypeId(@Nullable PatientOrderResourcingTypeId patientOrderResourcingTypeId) {
		this.patientOrderResourcingTypeId = patientOrderResourcingTypeId;
	}

	@Nullable
	public UUID getPatientOrderImportId() {
		return this.patientOrderImportId;
	}

	public void setPatientOrderImportId(@Nullable UUID patientOrderImportId) {
		this.patientOrderImportId = patientOrderImportId;
	}

	@Nullable
	public InstitutionId getInstitutionId() {
		return this.institutionId;
	}

	public void setInstitutionId(@Nullable InstitutionId institutionId) {
		this.institutionId = institutionId;
	}

	@Nullable
	public UUID getPatientAccountId() {
		return this.patientAccountId;
	}

	public void setPatientAccountId(@Nullable UUID patientAccountId) {
		this.patientAccountId = patientAccountId;
	}

	@Nullable
	public UUID getPatientAddressId() {
		return this.patientAddressId;
	}

	public void setPatientAddressId(@Nullable UUID patientAddressId) {
		this.patientAddressId = patientAddressId;
	}

	@Nullable
	public UUID getPanelAccountId() {
		return this.panelAccountId;
	}

	public void setPanelAccountId(@Nullable UUID panelAccountId) {
		this.panelAccountId = panelAccountId;
	}

	@Nullable
	public PatientOrderClosureReasonId getPatientOrderClosureReasonId() {
		return this.patientOrderClosureReasonId;
	}

	public void setPatientOrderClosureReasonId(@Nullable PatientOrderClosureReasonId patientOrderClosureReasonId) {
		this.patientOrderClosureReasonId = patientOrderClosureReasonId;
	}

	@Nullable
	public String getEncounterDepartmentId() {
		return this.encounterDepartmentId;
	}

	public void setEncounterDepartmentId(@Nullable String encounterDepartmentId) {
		this.encounterDepartmentId = encounterDepartmentId;
	}

	@Nullable
	public String getEncounterDepartmentIdType() {
		return this.encounterDepartmentIdType;
	}

	public void setEncounterDepartmentIdType(@Nullable String encounterDepartmentIdType) {
		this.encounterDepartmentIdType = encounterDepartmentIdType;
	}

	@Nullable
	public String getEncounterDepartmentName() {
		return this.encounterDepartmentName;
	}

	public void setEncounterDepartmentName(@Nullable String encounterDepartmentName) {
		this.encounterDepartmentName = encounterDepartmentName;
	}

	@Nullable
	public String getReferringPracticeId() {
		return this.referringPracticeId;
	}

	public void setReferringPracticeId(@Nullable String referringPracticeId) {
		this.referringPracticeId = referringPracticeId;
	}

	@Nullable
	public String getReferringPracticeIdType() {
		return this.referringPracticeIdType;
	}

	public void setReferringPracticeIdType(@Nullable String referringPracticeIdType) {
		this.referringPracticeIdType = referringPracticeIdType;
	}

	@Nullable
	public String getReferringPracticeName() {
		return this.referringPracticeName;
	}

	public void setReferringPracticeName(@Nullable String referringPracticeName) {
		this.referringPracticeName = referringPracticeName;
	}

	@Nullable
	public String getOrderingProviderId() {
		return this.orderingProviderId;
	}

	public void setOrderingProviderId(@Nullable String orderingProviderId) {
		this.orderingProviderId = orderingProviderId;
	}

	@Nullable
	public String getOrderingProviderIdType() {
		return this.orderingProviderIdType;
	}

	public void setOrderingProviderIdType(@Nullable String orderingProviderIdType) {
		this.orderingProviderIdType = orderingProviderIdType;
	}

	@Nullable
	public String getOrderingProviderLastName() {
		return this.orderingProviderLastName;
	}

	public void setOrderingProviderLastName(@Nullable String orderingProviderLastName) {
		this.orderingProviderLastName = orderingProviderLastName;
	}

	@Nullable
	public String getOrderingProviderFirstName() {
		return this.orderingProviderFirstName;
	}

	public void setOrderingProviderFirstName(@Nullable String orderingProviderFirstName) {
		this.orderingProviderFirstName = orderingProviderFirstName;
	}

	@Nullable
	public String getOrderingProviderMiddleName() {
		return this.orderingProviderMiddleName;
	}

	public void setOrderingProviderMiddleName(@Nullable String orderingProviderMiddleName) {
		this.orderingProviderMiddleName = orderingProviderMiddleName;
	}

	@Nullable
	public String getBillingProviderId() {
		return this.billingProviderId;
	}

	public void setBillingProviderId(@Nullable String billingProviderId) {
		this.billingProviderId = billingProviderId;
	}

	@Nullable
	public String getBillingProviderIdType() {
		return this.billingProviderIdType;
	}

	public void setBillingProviderIdType(@Nullable String billingProviderIdType) {
		this.billingProviderIdType = billingProviderIdType;
	}

	@Nullable
	public String getBillingProviderLastName() {
		return this.billingProviderLastName;
	}

	public void setBillingProviderLastName(@Nullable String billingProviderLastName) {
		this.billingProviderLastName = billingProviderLastName;
	}

	@Nullable
	public String getBillingProviderFirstName() {
		return this.billingProviderFirstName;
	}

	public void setBillingProviderFirstName(@Nullable String billingProviderFirstName) {
		this.billingProviderFirstName = billingProviderFirstName;
	}

	@Nullable
	public String getBillingProviderMiddleName() {
		return this.billingProviderMiddleName;
	}

	public void setBillingProviderMiddleName(@Nullable String billingProviderMiddleName) {
		this.billingProviderMiddleName = billingProviderMiddleName;
	}

	@Nullable
	public String getPatientLastName() {
		return this.patientLastName;
	}

	public void setPatientLastName(@Nullable String patientLastName) {
		this.patientLastName = patientLastName;
	}

	@Nullable
	public String getPatientFirstName() {
		return this.patientFirstName;
	}

	public void setPatientFirstName(@Nullable String patientFirstName) {
		this.patientFirstName = patientFirstName;
	}

	@Nullable
	public String getPatientMrn() {
		return this.patientMrn;
	}

	public void setPatientMrn(@Nullable String patientMrn) {
		this.patientMrn = patientMrn;
	}

	@Nullable
	public String getPatientUniqueId() {
		return this.patientUniqueId;
	}

	public void setPatientUniqueId(@Nullable String patientUniqueId) {
		this.patientUniqueId = patientUniqueId;
	}

	@Nullable
	public String getPatientUniqueIdType() {
		return this.patientUniqueIdType;
	}

	public void setPatientUniqueIdType(@Nullable String patientUniqueIdType) {
		this.patientUniqueIdType = patientUniqueIdType;
	}

	@Nullable
	public String getPatientLanguageCode() {
		return this.patientLanguageCode;
	}

	public void setPatientLanguageCode(@Nullable String patientLanguageCode) {
		this.patientLanguageCode = patientLanguageCode;
	}

	@Nullable
	public LocalDate getPatientBirthdate() {
		return this.patientBirthdate;
	}

	public void setPatientBirthdate(@Nullable LocalDate patientBirthdate) {
		this.patientBirthdate = patientBirthdate;
	}

	@Nullable
	public String getPatientPhoneNumber() {
		return this.patientPhoneNumber;
	}

	public void setPatientPhoneNumber(@Nullable String patientPhoneNumber) {
		this.patientPhoneNumber = patientPhoneNumber;
	}

	@Nullable
	public String getPatientEmailAddress() {
		return this.patientEmailAddress;
	}

	public void setPatientEmailAddress(@Nullable String patientEmailAddress) {
		this.patientEmailAddress = patientEmailAddress;
	}

	@Nullable
	public EthnicityId getPatientEthnicityId() {
		return this.patientEthnicityId;
	}

	public void setPatientEthnicityId(@Nullable EthnicityId patientEthnicityId) {
		this.patientEthnicityId = patientEthnicityId;
	}

	@Nullable
	public RaceId getPatientRaceId() {
		return this.patientRaceId;
	}

	public void setPatientRaceId(@Nullable RaceId patientRaceId) {
		this.patientRaceId = patientRaceId;
	}

	@Nullable
	public GenderIdentityId getPatientGenderIdentityId() {
		return this.patientGenderIdentityId;
	}

	public void setPatientGenderIdentityId(@Nullable GenderIdentityId patientGenderIdentityId) {
		this.patientGenderIdentityId = patientGenderIdentityId;
	}

	@Nullable
	public BirthSexId getPatientBirthSexId() {
		return this.patientBirthSexId;
	}

	public void setPatientBirthSexId(@Nullable BirthSexId patientBirthSexId) {
		this.patientBirthSexId = patientBirthSexId;
	}

	@Nullable
	public PreferredPronounId getPatientPreferredPronounId() {
		return this.patientPreferredPronounId;
	}

	public void setPatientPreferredPronounId(@Nullable PreferredPronounId patientPreferredPronounId) {
		this.patientPreferredPronounId = patientPreferredPronounId;
	}

	@Nullable
	public ClinicalSexId getPatientClinicalSexId() {
		return this.patientClinicalSexId;
	}

	public void setPatientClinicalSexId(@Nullable ClinicalSexId patientClinicalSexId) {
		this.patientClinicalSexId = patientClinicalSexId;
	}

	@Nullable
	public LegalSexId getPatientLegalSexId() {
		return this.patientLegalSexId;
	}

	public void setPatientLegalSexId(@Nullable LegalSexId patientLegalSexId) {
		this.patientLegalSexId = patientLegalSexId;
	}

	@Nullable
	public AdministrativeGenderId getPatientAdministrativeGenderId() {
		return this.patientAdministrativeGenderId;
	}

	public void setPatientAdministrativeGenderId(@Nullable AdministrativeGenderId patientAdministrativeGenderId) {
		this.patientAdministrativeGenderId = patientAdministrativeGenderId;
	}

	@Nullable
	public PatientOrderDemographicsImportStatusId getPatientOrderDemographicsImportStatusId() {
		return this.patientOrderDemographicsImportStatusId;
	}

	public void setPatientOrderDemographicsImportStatusId(@Nullable PatientOrderDemographicsImportStatusId patientOrderDemographicsImportStatusId) {
		this.patientOrderDemographicsImportStatusId = patientOrderDemographicsImportStatusId;
	}

	@Nullable
	public Instant getPatientDemographicsImportedAt() {
		return this.patientDemographicsImportedAt;
	}

	public void setPatientDemographicsImportedAt(@Nullable Instant patientDemographicsImportedAt) {
		this.patientDemographicsImportedAt = patientDemographicsImportedAt;
	}

	@Nullable
	public String getPrimaryPayorId() {
		return this.primaryPayorId;
	}

	public void setPrimaryPayorId(@Nullable String primaryPayorId) {
		this.primaryPayorId = primaryPayorId;
	}

	@Nullable
	public String getPrimaryPayorName() {
		return this.primaryPayorName;
	}

	public void setPrimaryPayorName(@Nullable String primaryPayorName) {
		this.primaryPayorName = primaryPayorName;
	}

	@Nullable
	public String getPrimaryPlanId() {
		return this.primaryPlanId;
	}

	public void setPrimaryPlanId(@Nullable String primaryPlanId) {
		this.primaryPlanId = primaryPlanId;
	}

	@Nullable
	public String getPrimaryPlanName() {
		return this.primaryPlanName;
	}

	public void setPrimaryPlanName(@Nullable String primaryPlanName) {
		this.primaryPlanName = primaryPlanName;
	}

	@Nullable
	public Boolean getPrimaryPlanAccepted() {
		return this.primaryPlanAccepted;
	}

	public void setPrimaryPlanAccepted(@Nullable Boolean primaryPlanAccepted) {
		this.primaryPlanAccepted = primaryPlanAccepted;
	}

	@Nullable
	public LocalDate getOrderDate() {
		return this.orderDate;
	}

	public void setOrderDate(@Nullable LocalDate orderDate) {
		this.orderDate = orderDate;
	}

	@Nullable
	public Integer getOrderAgeInMinutes() {
		return this.orderAgeInMinutes;
	}

	public void setOrderAgeInMinutes(@Nullable Integer orderAgeInMinutes) {
		this.orderAgeInMinutes = orderAgeInMinutes;
	}

	@Nullable
	public String getOrderId() {
		return this.orderId;
	}

	public void setOrderId(@Nullable String orderId) {
		this.orderId = orderId;
	}

	@Nullable
	public String getRouting() {
		return this.routing;
	}

	public void setRouting(@Nullable String routing) {
		this.routing = routing;
	}

	@Nullable
	public String getAssociatedDiagnosis() {
		return this.associatedDiagnosis;
	}

	public void setAssociatedDiagnosis(@Nullable String associatedDiagnosis) {
		this.associatedDiagnosis = associatedDiagnosis;
	}

	@Nullable
	public String getPreferredContactHours() {
		return this.preferredContactHours;
	}

	public void setPreferredContactHours(@Nullable String preferredContactHours) {
		this.preferredContactHours = preferredContactHours;
	}

	@Nullable
	public String getComments() {
		return this.comments;
	}

	public void setComments(@Nullable String comments) {
		this.comments = comments;
	}

	@Nullable
	public String getCcRecipients() {
		return this.ccRecipients;
	}

	public void setCcRecipients(@Nullable String ccRecipients) {
		this.ccRecipients = ccRecipients;
	}

	@Nullable
	public String getLastActiveMedicationOrderSummary() {
		return this.lastActiveMedicationOrderSummary;
	}

	public void setLastActiveMedicationOrderSummary(@Nullable String lastActiveMedicationOrderSummary) {
		this.lastActiveMedicationOrderSummary = lastActiveMedicationOrderSummary;
	}

	@Nullable
	public String getMedications() {
		return this.medications;
	}

	public void setMedications(@Nullable String medications) {
		this.medications = medications;
	}

	@Nullable
	public String getRecentPsychotherapeuticMedications() {
		return this.recentPsychotherapeuticMedications;
	}

	public void setRecentPsychotherapeuticMedications(@Nullable String recentPsychotherapeuticMedications) {
		this.recentPsychotherapeuticMedications = recentPsychotherapeuticMedications;
	}

	@Nullable
	public Instant getEpisodeClosedAt() {
		return this.episodeClosedAt;
	}

	public void setEpisodeClosedAt(@Nullable Instant episodeClosedAt) {
		this.episodeClosedAt = episodeClosedAt;
	}

	@Nullable
	public UUID getEpisodeClosedByAccountId() {
		return this.episodeClosedByAccountId;
	}

	public void setEpisodeClosedByAccountId(@Nullable UUID episodeClosedByAccountId) {
		this.episodeClosedByAccountId = episodeClosedByAccountId;
	}

	@Nullable
	public Instant getResourcesSentAt() {
		return this.resourcesSentAt;
	}

	public void setResourcesSentAt(@Nullable Instant resourcesSentAt) {
		this.resourcesSentAt = resourcesSentAt;
	}

	@Nullable
	public String getResourcesSentNote() {
		return this.resourcesSentNote;
	}

	public void setResourcesSentNote(@Nullable String resourcesSentNote) {
		this.resourcesSentNote = resourcesSentNote;
	}

	@Nullable
	public Instant getConnectedToSafetyPlanningAt() {
		return this.connectedToSafetyPlanningAt;
	}

	public void setConnectedToSafetyPlanningAt(@Nullable Instant connectedToSafetyPlanningAt) {
		this.connectedToSafetyPlanningAt = connectedToSafetyPlanningAt;
	}

	@Nullable
	public PatientOrderConsentStatusId getPatientOrderConsentStatusId() {
		return this.patientOrderConsentStatusId;
	}

	public void setPatientOrderConsentStatusId(@Nullable PatientOrderConsentStatusId patientOrderConsentStatusId) {
		this.patientOrderConsentStatusId = patientOrderConsentStatusId;
	}

	@Nullable
	public UUID getConsentStatusUpdatedByByAccountId() {
		return this.consentStatusUpdatedByByAccountId;
	}

	public void setConsentStatusUpdatedByByAccountId(@Nullable UUID consentStatusUpdatedByByAccountId) {
		this.consentStatusUpdatedByByAccountId = consentStatusUpdatedByByAccountId;
	}

	@Nullable
	public Instant getConsentStatusUpdatedAt() {
		return this.consentStatusUpdatedAt;
	}

	public void setConsentStatusUpdatedAt(@Nullable Instant consentStatusUpdatedAt) {
		this.consentStatusUpdatedAt = consentStatusUpdatedAt;
	}

	@Nullable
	public UUID getResourceCheckInScheduledMessageGroupId() {
		return this.resourceCheckInScheduledMessageGroupId;
	}

	public void setResourceCheckInScheduledMessageGroupId(@Nullable UUID resourceCheckInScheduledMessageGroupId) {
		this.resourceCheckInScheduledMessageGroupId = resourceCheckInScheduledMessageGroupId;
	}

	@Nullable
	public PatientOrderResourceCheckInResponseStatusId getPatientOrderResourceCheckInResponseStatusId() {
		return this.patientOrderResourceCheckInResponseStatusId;
	}

	public void setPatientOrderResourceCheckInResponseStatusId(@Nullable PatientOrderResourceCheckInResponseStatusId patientOrderResourceCheckInResponseStatusId) {
		this.patientOrderResourceCheckInResponseStatusId = patientOrderResourceCheckInResponseStatusId;
	}

	@Nullable
	public UUID getResourceCheckInResponseStatusUpdatedByByAccountId() {
		return this.resourceCheckInResponseStatusUpdatedByByAccountId;
	}

	public void setResourceCheckInResponseStatusUpdatedByByAccountId(@Nullable UUID resourceCheckInResponseStatusUpdatedByByAccountId) {
		this.resourceCheckInResponseStatusUpdatedByByAccountId = resourceCheckInResponseStatusUpdatedByByAccountId;
	}

	@Nullable
	public Instant getResourceCheckInResponseStatusUpdatedAt() {
		return this.resourceCheckInResponseStatusUpdatedAt;
	}

	public void setResourceCheckInResponseStatusUpdatedAt(@Nullable Instant resourceCheckInResponseStatusUpdatedAt) {
		this.resourceCheckInResponseStatusUpdatedAt = resourceCheckInResponseStatusUpdatedAt;
	}

	@Nullable
	public String getTestPatientEmailAddress() {
		return this.testPatientEmailAddress;
	}

	public void setTestPatientEmailAddress(@Nullable String testPatientEmailAddress) {
		this.testPatientEmailAddress = testPatientEmailAddress;
	}

	@Nullable
	public String getTestPatientPassword() {
		return this.testPatientPassword;
	}

	public void setTestPatientPassword(@Nullable String testPatientPassword) {
		this.testPatientPassword = testPatientPassword;
	}

	@Nullable
	public Instant getPatientDemographicsConfirmedAt() {
		return this.patientDemographicsConfirmedAt;
	}

	public void setPatientDemographicsConfirmedAt(@Nullable Instant patientDemographicsConfirmedAt) {
		this.patientDemographicsConfirmedAt = patientDemographicsConfirmedAt;
	}

	@Nullable
	public UUID getPatientDemographicsConfirmedByAccountId() {
		return this.patientDemographicsConfirmedByAccountId;
	}

	public void setPatientDemographicsConfirmedByAccountId(@Nullable UUID patientDemographicsConfirmedByAccountId) {
		this.patientDemographicsConfirmedByAccountId = patientDemographicsConfirmedByAccountId;
	}

	@Nullable
	public PatientOrderCarePreferenceId getPatientOrderCarePreferenceId() {
		return this.patientOrderCarePreferenceId;
	}

	public void setPatientOrderCarePreferenceId(@Nullable PatientOrderCarePreferenceId patientOrderCarePreferenceId) {
		this.patientOrderCarePreferenceId = patientOrderCarePreferenceId;
	}

	@Nullable
	public Integer getInPersonCareRadius() {
		return this.inPersonCareRadius;
	}

	public void setInPersonCareRadius(@Nullable Integer inPersonCareRadius) {
		this.inPersonCareRadius = inPersonCareRadius;
	}

	@Nullable
	public DistanceUnitId getInPersonCareRadiusDistanceUnitId() {
		return this.inPersonCareRadiusDistanceUnitId;
	}

	public void setInPersonCareRadiusDistanceUnitId(@Nullable DistanceUnitId inPersonCareRadiusDistanceUnitId) {
		this.inPersonCareRadiusDistanceUnitId = inPersonCareRadiusDistanceUnitId;
	}

	@Nullable
	public UUID getEpicDepartmentId() {
		return this.epicDepartmentId;
	}

	public void setEpicDepartmentId(@Nullable UUID epicDepartmentId) {
		this.epicDepartmentId = epicDepartmentId;
	}

	@Nullable
	public String getEncounterCsn() {
		return this.encounterCsn;
	}

	public void setEncounterCsn(@Nullable String encounterCsn) {
		this.encounterCsn = encounterCsn;
	}

	@Nullable
	public Instant getEncounterSyncedAt() {
		return this.encounterSyncedAt;
	}

	public void setEncounterSyncedAt(@Nullable Instant encounterSyncedAt) {
		this.encounterSyncedAt = encounterSyncedAt;
	}

	@Nullable
	public Instant getCreated() {
		return this.created;
	}

	public void setCreated(@Nullable Instant created) {
		this.created = created;
	}

	@Nullable
	public Instant getLastUpdated() {
		return this.lastUpdated;
	}

	public void setLastUpdated(@Nullable Instant lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	@Nullable
	public PatientOrderIntakeWantsServicesStatusId getPatientOrderIntakeWantsServicesStatusId() {
		return this.patientOrderIntakeWantsServicesStatusId;
	}

	public void setPatientOrderIntakeWantsServicesStatusId(@Nullable PatientOrderIntakeWantsServicesStatusId patientOrderIntakeWantsServicesStatusId) {
		this.patientOrderIntakeWantsServicesStatusId = patientOrderIntakeWantsServicesStatusId;
	}

	@Nullable
	public PatientOrderIntakeLocationStatusId getPatientOrderIntakeLocationStatusId() {
		return this.patientOrderIntakeLocationStatusId;
	}

	public void setPatientOrderIntakeLocationStatusId(@Nullable PatientOrderIntakeLocationStatusId patientOrderIntakeLocationStatusId) {
		this.patientOrderIntakeLocationStatusId = patientOrderIntakeLocationStatusId;
	}

	@Nullable
	public PatientOrderIntakeInsuranceStatusId getPatientOrderIntakeInsuranceStatusId() {
		return this.patientOrderIntakeInsuranceStatusId;
	}

	public void setPatientOrderIntakeInsuranceStatusId(@Nullable PatientOrderIntakeInsuranceStatusId patientOrderIntakeInsuranceStatusId) {
		this.patientOrderIntakeInsuranceStatusId = patientOrderIntakeInsuranceStatusId;
	}
}