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

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class PatientOrder {
	@Nullable
	private UUID patientOrderId;
	@Nullable
	private UUID patientOrderImportId;
	@Nullable
	private UUID patientAccountId;
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
	private String orderingProviderName;
	@Nullable
	private String billingProviderId;
	@Nullable
	private String billingProviderIdType;
	@Nullable
	private String billingProviderName;
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
	private String patientBirthSexId;
	@Nullable
	private LocalDate patientBirthdate;
	@Nullable
	private String patientAddressLine1;
	@Nullable
	private String patientAddressLine2;
	@Nullable
	private String patientCity;
	@Nullable
	private String patientPostalCode;
	@Nullable
	private String patientRegion;
	@Nullable
	private String patientCountryCode;
	@Nullable
	private String primaryPayor;
	@Nullable
	private String primaryPlan;
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
	private String diagnosis;
	@Nullable
	private String associatedDiagnosis;
	@Nullable
	private String callbackPhoneNumber;
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
	public UUID getPatientOrderImportId() {
		return this.patientOrderImportId;
	}

	public void setPatientOrderImportId(@Nullable UUID patientOrderImportId) {
		this.patientOrderImportId = patientOrderImportId;
	}

	@Nullable
	public UUID getPatientAccountId() {
		return this.patientAccountId;
	}

	public void setPatientAccountId(@Nullable UUID patientAccountId) {
		this.patientAccountId = patientAccountId;
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
	public String getOrderingProviderName() {
		return this.orderingProviderName;
	}

	public void setOrderingProviderName(@Nullable String orderingProviderName) {
		this.orderingProviderName = orderingProviderName;
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
	public String getBillingProviderName() {
		return this.billingProviderName;
	}

	public void setBillingProviderName(@Nullable String billingProviderName) {
		this.billingProviderName = billingProviderName;
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
	public String getPatientBirthSexId() {
		return this.patientBirthSexId;
	}

	public void setPatientBirthSexId(@Nullable String patientBirthSexId) {
		this.patientBirthSexId = patientBirthSexId;
	}

	@Nullable
	public LocalDate getPatientBirthdate() {
		return this.patientBirthdate;
	}

	public void setPatientBirthdate(@Nullable LocalDate patientBirthdate) {
		this.patientBirthdate = patientBirthdate;
	}

	@Nullable
	public String getPatientAddressLine1() {
		return this.patientAddressLine1;
	}

	public void setPatientAddressLine1(@Nullable String patientAddressLine1) {
		this.patientAddressLine1 = patientAddressLine1;
	}

	@Nullable
	public String getPatientAddressLine2() {
		return this.patientAddressLine2;
	}

	public void setPatientAddressLine2(@Nullable String patientAddressLine2) {
		this.patientAddressLine2 = patientAddressLine2;
	}

	@Nullable
	public String getPatientCity() {
		return this.patientCity;
	}

	public void setPatientCity(@Nullable String patientCity) {
		this.patientCity = patientCity;
	}

	@Nullable
	public String getPatientPostalCode() {
		return this.patientPostalCode;
	}

	public void setPatientPostalCode(@Nullable String patientPostalCode) {
		this.patientPostalCode = patientPostalCode;
	}

	@Nullable
	public String getPatientRegion() {
		return this.patientRegion;
	}

	public void setPatientRegion(@Nullable String patientRegion) {
		this.patientRegion = patientRegion;
	}

	@Nullable
	public String getPatientCountryCode() {
		return this.patientCountryCode;
	}

	public void setPatientCountryCode(@Nullable String patientCountryCode) {
		this.patientCountryCode = patientCountryCode;
	}

	@Nullable
	public String getPrimaryPayor() {
		return this.primaryPayor;
	}

	public void setPrimaryPayor(@Nullable String primaryPayor) {
		this.primaryPayor = primaryPayor;
	}

	@Nullable
	public String getPrimaryPlan() {
		return this.primaryPlan;
	}

	public void setPrimaryPlan(@Nullable String primaryPlan) {
		this.primaryPlan = primaryPlan;
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
	public String getDiagnosis() {
		return this.diagnosis;
	}

	public void setDiagnosis(@Nullable String diagnosis) {
		this.diagnosis = diagnosis;
	}

	@Nullable
	public String getAssociatedDiagnosis() {
		return this.associatedDiagnosis;
	}

	public void setAssociatedDiagnosis(@Nullable String associatedDiagnosis) {
		this.associatedDiagnosis = associatedDiagnosis;
	}

	@Nullable
	public String getCallbackPhoneNumber() {
		return this.callbackPhoneNumber;
	}

	public void setCallbackPhoneNumber(@Nullable String callbackPhoneNumber) {
		this.callbackPhoneNumber = callbackPhoneNumber;
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
}
