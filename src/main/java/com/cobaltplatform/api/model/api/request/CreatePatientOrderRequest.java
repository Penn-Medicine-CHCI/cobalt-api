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

package com.cobaltplatform.api.model.api.request;

import com.cobaltplatform.api.model.db.Institution.InstitutionId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class CreatePatientOrderRequest {
	@Nullable
	private UUID patientOrderImportId;
	@Nullable
	private InstitutionId institutionId;
	@Nullable
	private UUID accountId;
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
	private String patientBirthSexId;
	@Nullable
	private String patientBirthdate;
	@Nullable
	private String patientAddressLine1;
	@Nullable
	private String patientAddressLine2;
	@Nullable
	private String patientLocality;
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
	private String orderDate;
	@Nullable
	private String orderAge;
	@Nullable
	private String orderId;
	@Nullable
	private String routing;
	@Nullable
	private String reasonForReferral;
	@Nullable
	private List<CreatePatientOrderDiagnosisRequest> diagnoses;
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
	public UUID getAccountId() {
		return this.accountId;
	}

	public void setAccountId(@Nullable UUID accountId) {
		this.accountId = accountId;
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
	public String getPatientBirthSexId() {
		return this.patientBirthSexId;
	}

	public void setPatientBirthSexId(@Nullable String patientBirthSexId) {
		this.patientBirthSexId = patientBirthSexId;
	}

	@Nullable
	public String getPatientBirthdate() {
		return this.patientBirthdate;
	}

	public void setPatientBirthdate(@Nullable String patientBirthdate) {
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
	public String getPatientLocality() {
		return this.patientLocality;
	}

	public void setPatientLocality(@Nullable String patientLocality) {
		this.patientLocality = patientLocality;
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
	public String getOrderDate() {
		return this.orderDate;
	}

	public void setOrderDate(@Nullable String orderDate) {
		this.orderDate = orderDate;
	}

	@Nullable
	public String getOrderAge() {
		return this.orderAge;
	}

	public void setOrderAge(@Nullable String orderAge) {
		this.orderAge = orderAge;
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
	public List<CreatePatientOrderDiagnosisRequest> getDiagnoses() {
		return this.diagnoses;
	}

	public void setDiagnoses(@Nullable List<CreatePatientOrderDiagnosisRequest> diagnoses) {
		this.diagnoses = diagnoses;
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


	@NotThreadSafe
	public static class CreatePatientOrderDiagnosisRequest {
		@Nullable
		private String diagnosisId;
		@Nullable
		private String diagnosisIdType;
		@Nullable
		private String diagnosisName;

		@Nullable
		public String getDiagnosisId() {
			return this.diagnosisId;
		}

		public void setDiagnosisId(@Nullable String diagnosisId) {
			this.diagnosisId = diagnosisId;
		}

		@Nullable
		public String getDiagnosisIdType() {
			return this.diagnosisIdType;
		}

		public void setDiagnosisIdType(@Nullable String diagnosisIdType) {
			this.diagnosisIdType = diagnosisIdType;
		}

		@Nullable
		public String getDiagnosisName() {
			return this.diagnosisName;
		}

		public void setDiagnosisName(@Nullable String diagnosisName) {
			this.diagnosisName = diagnosisName;
		}
	}
}
