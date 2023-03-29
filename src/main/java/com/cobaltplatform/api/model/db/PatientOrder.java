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

import com.cobaltplatform.api.model.db.BirthSex.BirthSexId;
import com.cobaltplatform.api.model.db.Ethnicity.EthnicityId;
import com.cobaltplatform.api.model.db.GenderIdentity.GenderIdentityId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.PatientOrderCareType.PatientOrderCareTypeId;
import com.cobaltplatform.api.model.db.PatientOrderClosureReason.PatientOrderClosureReasonId;
import com.cobaltplatform.api.model.db.PatientOrderDisposition.PatientOrderDispositionId;
import com.cobaltplatform.api.model.db.PatientOrderResourcingStatus.PatientOrderResourcingStatusId;
import com.cobaltplatform.api.model.db.PatientOrderSafetyPlanningStatus.PatientOrderSafetyPlanningStatusId;
import com.cobaltplatform.api.model.db.PatientOrderScreeningStatus.PatientOrderScreeningStatusId;
import com.cobaltplatform.api.model.db.PatientOrderStatus.PatientOrderStatusId;
import com.cobaltplatform.api.model.db.Race.RaceId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class PatientOrder {
	@Nullable
	private UUID patientOrderId;
	@Nullable
	private PatientOrderStatusId patientOrderStatusId;
	@Nullable
	private PatientOrderDispositionId patientOrderDispositionId;
	@Nullable
	private PatientOrderScreeningStatusId patientOrderScreeningStatusId;
	@Nullable
	private PatientOrderSafetyPlanningStatusId patientOrderSafetyPlanningStatusId;
	@Nullable
	private PatientOrderResourcingStatusId patientOrderResourcingStatusId;
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
	private String patientId;
	@Nullable
	private String patientIdType;
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
	private String primaryPayorId;
	@Nullable
	private String primaryPayorName;
	@Nullable
	private String primaryPlanId;
	@Nullable
	private String primaryPlanName;
	@Nullable
	private LocalDate orderDate;
	@Nullable
	private Integer orderAgeInMinutes;
	@Nullable
	private String orderId;
	@Nullable
	private String routing;
	@Nullable
	private String reasonForReferral;
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
	private Boolean outreachNeeded;
	@Nullable
	private Boolean followupNeeded;
	@Nullable
	private Instant resourcesSentAt;
	@Nullable
	private String resourcesSentNote;
	@Nullable
	private Instant connectedToSafetyPlanningAt;
	@Nullable
	private String testPatientEmailAddress;
	@Nullable
	private String testPatientPassword;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	// Included in v_patient_order

	@Nullable
	private PatientOrderCareTypeId patientOrderCareTypeId;
	@Nullable
	private String patientOrderCareTypeDescription;
	@Nullable
	private Integer outreachCount;
	@Nullable
	private LocalDateTime mostRecentOutreachDateTime;
	@Nullable
	private UUID mostRecentScreeningSessionId;
	@Nullable
	private UUID mostRecentScreeningSessionCreatedByAccountId;
	@Nullable
	private String mostRecentScreeningSessionCreatedByAccountFirstName;
	@Nullable
	private String mostRecentScreeningSessionCreatedByAccountLastName;
	@Nullable
	private Boolean mostRecentScreeningSessionCompleted;
	@Nullable
	private Instant mostRecentScreeningSessionCompletedAt;
	@Nullable
	private String panelAccountFirstName;
	@Nullable
	private String panelAccountLastName;
	@Nullable
	private String patientOrderScreeningStatusDescription;
	@Nullable
	private String patientOrderDispositionDescription;
	@Nullable
	private String patientOrderStatusDescription;
	@Nullable
	private String patientOrderClosureReasonDescription;
	@Nullable
	private Boolean patientBelowAgeThreshold;
	@Nullable
	private Instant mostRecentEpisodeClosedAt;
	@Nullable
	private Boolean mostRecentEpisodeClosedWithinDateThreshold;
	@Nullable
	private UUID patientOrderScheduledScreeningId;
	@Nullable
	private LocalDateTime patientOrderScheduledScreeningScheduledDateTime;
	@Nullable
	private String patientOrderScheduledScreeningCalendarUrl;

	@Nullable
	public UUID getPatientOrderId() {
		return this.patientOrderId;
	}

	public void setPatientOrderId(@Nullable UUID patientOrderId) {
		this.patientOrderId = patientOrderId;
	}

	@Nullable
	public PatientOrderStatusId getPatientOrderStatusId() {
		return this.patientOrderStatusId;
	}

	public void setPatientOrderStatusId(@Nullable PatientOrderStatusId patientOrderStatusId) {
		this.patientOrderStatusId = patientOrderStatusId;
	}

	@Nullable
	public PatientOrderDispositionId getPatientOrderDispositionId() {
		return this.patientOrderDispositionId;
	}

	public void setPatientOrderDispositionId(@Nullable PatientOrderDispositionId patientOrderDispositionId) {
		this.patientOrderDispositionId = patientOrderDispositionId;
	}

	@Nullable
	public PatientOrderScreeningStatusId getPatientOrderScreeningStatusId() {
		return this.patientOrderScreeningStatusId;
	}

	public void setPatientOrderScreeningStatusId(@Nullable PatientOrderScreeningStatusId patientOrderScreeningStatusId) {
		this.patientOrderScreeningStatusId = patientOrderScreeningStatusId;
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
	public String getPatientId() {
		return this.patientId;
	}

	public void setPatientId(@Nullable String patientId) {
		this.patientId = patientId;
	}

	@Nullable
	public String getPatientIdType() {
		return this.patientIdType;
	}

	public void setPatientIdType(@Nullable String patientIdType) {
		this.patientIdType = patientIdType;
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
	public String getReasonForReferral() {
		return this.reasonForReferral;
	}

	public void setReasonForReferral(@Nullable String reasonForReferral) {
		this.reasonForReferral = reasonForReferral;
	}

	@Nullable
	public String getAssociatedDiagnosis() {
		return this.associatedDiagnosis;
	}

	public void setAssociatedDiagnosis(@Nullable String associatedDiagnosis) {
		this.associatedDiagnosis = associatedDiagnosis;
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
	public Boolean getOutreachNeeded() {
		return this.outreachNeeded;
	}

	public void setOutreachNeeded(@Nullable Boolean outreachNeeded) {
		this.outreachNeeded = outreachNeeded;
	}

	@Nullable
	public Boolean getFollowupNeeded() {
		return this.followupNeeded;
	}

	public void setFollowupNeeded(@Nullable Boolean followupNeeded) {
		this.followupNeeded = followupNeeded;
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
	public Instant getConnectedToSafetyPlanningAt() {
		return this.connectedToSafetyPlanningAt;
	}

	public void setConnectedToSafetyPlanningAt(@Nullable Instant connectedToSafetyPlanningAt) {
		this.connectedToSafetyPlanningAt = connectedToSafetyPlanningAt;
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
	public PatientOrderCareTypeId getPatientOrderCareTypeId() {
		return this.patientOrderCareTypeId;
	}

	public void setPatientOrderCareTypeId(@Nullable PatientOrderCareTypeId patientOrderCareTypeId) {
		this.patientOrderCareTypeId = patientOrderCareTypeId;
	}

	@Nullable
	public String getPatientOrderCareTypeDescription() {
		return this.patientOrderCareTypeDescription;
	}

	public void setPatientOrderCareTypeDescription(@Nullable String patientOrderCareTypeDescription) {
		this.patientOrderCareTypeDescription = patientOrderCareTypeDescription;
	}

	@Nullable
	public Integer getOutreachCount() {
		return this.outreachCount;
	}

	public void setOutreachCount(@Nullable Integer outreachCount) {
		this.outreachCount = outreachCount;
	}

	@Nullable
	public LocalDateTime getMostRecentOutreachDateTime() {
		return this.mostRecentOutreachDateTime;
	}

	public void setMostRecentOutreachDateTime(@Nullable LocalDateTime mostRecentOutreachDateTime) {
		this.mostRecentOutreachDateTime = mostRecentOutreachDateTime;
	}

	@Nullable
	public UUID getMostRecentScreeningSessionId() {
		return this.mostRecentScreeningSessionId;
	}

	public void setMostRecentScreeningSessionId(@Nullable UUID mostRecentScreeningSessionId) {
		this.mostRecentScreeningSessionId = mostRecentScreeningSessionId;
	}

	@Nullable
	public UUID getMostRecentScreeningSessionCreatedByAccountId() {
		return this.mostRecentScreeningSessionCreatedByAccountId;
	}

	public void setMostRecentScreeningSessionCreatedByAccountId(@Nullable UUID mostRecentScreeningSessionCreatedByAccountId) {
		this.mostRecentScreeningSessionCreatedByAccountId = mostRecentScreeningSessionCreatedByAccountId;
	}

	@Nullable
	public String getMostRecentScreeningSessionCreatedByAccountFirstName() {
		return this.mostRecentScreeningSessionCreatedByAccountFirstName;
	}

	public void setMostRecentScreeningSessionCreatedByAccountFirstName(@Nullable String mostRecentScreeningSessionCreatedByAccountFirstName) {
		this.mostRecentScreeningSessionCreatedByAccountFirstName = mostRecentScreeningSessionCreatedByAccountFirstName;
	}

	@Nullable
	public String getMostRecentScreeningSessionCreatedByAccountLastName() {
		return this.mostRecentScreeningSessionCreatedByAccountLastName;
	}

	public void setMostRecentScreeningSessionCreatedByAccountLastName(@Nullable String mostRecentScreeningSessionCreatedByAccountLastName) {
		this.mostRecentScreeningSessionCreatedByAccountLastName = mostRecentScreeningSessionCreatedByAccountLastName;
	}

	@Nullable
	public Boolean getMostRecentScreeningSessionCompleted() {
		return this.mostRecentScreeningSessionCompleted;
	}

	public void setMostRecentScreeningSessionCompleted(@Nullable Boolean mostRecentScreeningSessionCompleted) {
		this.mostRecentScreeningSessionCompleted = mostRecentScreeningSessionCompleted;
	}

	@Nullable
	public Instant getMostRecentScreeningSessionCompletedAt() {
		return this.mostRecentScreeningSessionCompletedAt;
	}

	public void setMostRecentScreeningSessionCompletedAt(@Nullable Instant mostRecentScreeningSessionCompletedAt) {
		this.mostRecentScreeningSessionCompletedAt = mostRecentScreeningSessionCompletedAt;
	}

	@Nullable
	public String getPanelAccountFirstName() {
		return this.panelAccountFirstName;
	}

	public void setPanelAccountFirstName(@Nullable String panelAccountFirstName) {
		this.panelAccountFirstName = panelAccountFirstName;
	}

	@Nullable
	public String getPanelAccountLastName() {
		return this.panelAccountLastName;
	}

	public void setPanelAccountLastName(@Nullable String panelAccountLastName) {
		this.panelAccountLastName = panelAccountLastName;
	}

	@Nullable
	public String getPatientOrderScreeningStatusDescription() {
		return this.patientOrderScreeningStatusDescription;
	}

	public void setPatientOrderScreeningStatusDescription(@Nullable String patientOrderScreeningStatusDescription) {
		this.patientOrderScreeningStatusDescription = patientOrderScreeningStatusDescription;
	}

	@Nullable
	public String getPatientOrderDispositionDescription() {
		return this.patientOrderDispositionDescription;
	}

	public void setPatientOrderDispositionDescription(@Nullable String patientOrderDispositionDescription) {
		this.patientOrderDispositionDescription = patientOrderDispositionDescription;
	}

	@Nullable
	public String getPatientOrderStatusDescription() {
		return this.patientOrderStatusDescription;
	}

	public void setPatientOrderStatusDescription(@Nullable String patientOrderStatusDescription) {
		this.patientOrderStatusDescription = patientOrderStatusDescription;
	}

	@Nullable
	public String getPatientOrderClosureReasonDescription() {
		return this.patientOrderClosureReasonDescription;
	}

	public void setPatientOrderClosureReasonDescription(@Nullable String patientOrderClosureReasonDescription) {
		this.patientOrderClosureReasonDescription = patientOrderClosureReasonDescription;
	}

	@Nullable
	public Boolean getPatientBelowAgeThreshold() {
		return this.patientBelowAgeThreshold;
	}

	public void setPatientBelowAgeThreshold(@Nullable Boolean patientBelowAgeThreshold) {
		this.patientBelowAgeThreshold = patientBelowAgeThreshold;
	}

	@Nullable
	public Instant getMostRecentEpisodeClosedAt() {
		return this.mostRecentEpisodeClosedAt;
	}

	public void setMostRecentEpisodeClosedAt(@Nullable Instant mostRecentEpisodeClosedAt) {
		this.mostRecentEpisodeClosedAt = mostRecentEpisodeClosedAt;
	}

	@Nullable
	public Boolean getMostRecentEpisodeClosedWithinDateThreshold() {
		return this.mostRecentEpisodeClosedWithinDateThreshold;
	}

	public void setMostRecentEpisodeClosedWithinDateThreshold(@Nullable Boolean mostRecentEpisodeClosedWithinDateThreshold) {
		this.mostRecentEpisodeClosedWithinDateThreshold = mostRecentEpisodeClosedWithinDateThreshold;
	}

	@Nullable
	public UUID getPatientOrderScheduledScreeningId() {
		return this.patientOrderScheduledScreeningId;
	}

	public void setPatientOrderScheduledScreeningId(@Nullable UUID patientOrderScheduledScreeningId) {
		this.patientOrderScheduledScreeningId = patientOrderScheduledScreeningId;
	}

	@Nullable
	public LocalDateTime getPatientOrderScheduledScreeningScheduledDateTime() {
		return this.patientOrderScheduledScreeningScheduledDateTime;
	}

	public void setPatientOrderScheduledScreeningScheduledDateTime(@Nullable LocalDateTime patientOrderScheduledScreeningScheduledDateTime) {
		this.patientOrderScheduledScreeningScheduledDateTime = patientOrderScheduledScreeningScheduledDateTime;
	}

	@Nullable
	public String getPatientOrderScheduledScreeningCalendarUrl() {
		return this.patientOrderScheduledScreeningCalendarUrl;
	}

	public void setPatientOrderScheduledScreeningCalendarUrl(@Nullable String patientOrderScheduledScreeningCalendarUrl) {
		this.patientOrderScheduledScreeningCalendarUrl = patientOrderScheduledScreeningCalendarUrl;
	}
}