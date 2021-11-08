package com.cobaltplatform.ic.backend.model.db.v2;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class ReferralOrderReport {
	@Nonnull
	private static final DateTimeFormatter DATE_FORMATTER;

	static {
		DATE_FORMATTER = DateTimeFormatter.ofPattern("M/d/yyyy", Locale.US);
	}

	@Nullable
	private String orderId;
	@Nullable
	private String encounterDeptName;
	@Nullable
	private String encounterDeptId;
	@Nullable
	private String referringPractice;
	@Nullable
	private String referringPracticeSecond;
	@Nullable
	private String orderingProvider;
	@Nullable
	private String billingProvider;
	@Nullable
	private String lastName;
	@Nullable
	private String firstName;
	@Nullable
	private String mrn;
	@Nullable
	private String uid;
	@Nullable
	private String sex;
	@Nullable
	private String dateOfBirth;
	@Nullable
	private String primaryPayor;
	@Nullable
	private String primaryPlan;
	@Nullable
	private String orderDate;
	@Nullable
	private String ageOfOrder;
	@Nullable
	private String ccbhOrderRouting;
	@Nullable
	private String reasonsForReferral;
	@Nullable
	private String dx;
	@Nullable
	private String orderAssociatedDiagnosis;
	@Nullable
	private String callBackNumber;
	@Nullable
	private String preferredContactHours;
	@Nullable
	private String orderComments;
	@Nullable
	private String imgCcRecipients;
	@Nullable
	private String patientAddressLine1;
	@Nullable
	private String patientAddressLine2;
	@Nullable
	private String city;
	@Nullable
	private String patientState;
	@Nullable
	private String patientZipCode;
	@Nullable
	private String ccbhLastActiveMedOrderSummary;
	@Nullable
	private String ccbhMedicationsList;
	@Nullable
	private String psychotherapeuticMedLstTwoWeeks;
	@Nullable
	private UUID dispositionId;

	// Calculated fields
	@Nullable
	private LocalDate localOrderDate;

	@Nonnull
	public Optional<LocalDate> getLocalOrderDate() {
		return Optional.ofNullable(localOrderDate);
	}

	public void setOrderDate(@Nullable String orderDate) {
		this.orderDate = orderDate;

		String normalizedOrderDate = trimToNull(orderDate);
		localOrderDate = normalizedOrderDate == null ? null : LocalDate.parse(normalizedOrderDate, DATE_FORMATTER);
	}

	@Nullable
	public String getOrderId() {
		return orderId;
	}

	public void setOrderId(@Nullable String orderId) {
		this.orderId = orderId;
	}

	@Nullable
	public String getEncounterDeptName() {
		return encounterDeptName;
	}

	public void setEncounterDeptName(@Nullable String encounterDeptName) {
		this.encounterDeptName = encounterDeptName;
	}

	@Nullable
	public String getEncounterDeptId() {
		return encounterDeptId;
	}

	public void setEncounterDeptId(@Nullable String encounterDeptId) {
		this.encounterDeptId = encounterDeptId;
	}

	@Nullable
	public String getReferringPractice() {
		return referringPractice;
	}

	public void setReferringPractice(@Nullable String referringPractice) {
		this.referringPractice = referringPractice;
	}

	@Nullable
	public String getReferringPracticeSecond() {
		return referringPracticeSecond;
	}

	public void setReferringPracticeSecond(@Nullable String referringPracticeSecond) {
		this.referringPracticeSecond = referringPracticeSecond;
	}

	@Nullable
	public String getOrderingProvider() {
		return orderingProvider;
	}

	public void setOrderingProvider(@Nullable String orderingProvider) {
		this.orderingProvider = orderingProvider;
	}

	@Nullable
	public String getBillingProvider() {
		return billingProvider;
	}

	public void setBillingProvider(@Nullable String billingProvider) {
		this.billingProvider = billingProvider;
	}

	@Nullable
	public String getLastName() {
		return lastName;
	}

	public void setLastName(@Nullable String lastName) {
		this.lastName = lastName;
	}

	@Nullable
	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(@Nullable String firstName) {
		this.firstName = firstName;
	}

	@Nullable
	public String getMrn() {
		return mrn;
	}

	public void setMrn(@Nullable String mrn) {
		this.mrn = mrn;
	}

	@Nullable
	public String getUid() {
		return uid;
	}

	public void setUid(@Nullable String uid) {
		this.uid = uid;
	}

	@Nullable
	public String getSex() {
		return sex;
	}

	public void setSex(@Nullable String sex) {
		this.sex = sex;
	}

	@Nullable
	public String getDateOfBirth() {
		return dateOfBirth;
	}

	public void setDateOfBirth(@Nullable String dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	@Nullable
	public String getPrimaryPayor() {
		return primaryPayor;
	}

	public void setPrimaryPayor(@Nullable String primaryPayor) {
		this.primaryPayor = primaryPayor;
	}

	@Nullable
	public String getPrimaryPlan() {
		return primaryPlan;
	}

	public void setPrimaryPlan(@Nullable String primaryPlan) {
		this.primaryPlan = primaryPlan;
	}

	@Nullable
	public String getOrderDate() {
		return orderDate;
	}

	@Nullable
	public String getAgeOfOrder() {
		return ageOfOrder;
	}

	public void setAgeOfOrder(@Nullable String ageOfOrder) {
		this.ageOfOrder = ageOfOrder;
	}

	@Nullable
	public String getCcbhOrderRouting() {
		return ccbhOrderRouting;
	}

	public void setCcbhOrderRouting(@Nullable String ccbhOrderRouting) {
		this.ccbhOrderRouting = ccbhOrderRouting;
	}

	@Nullable
	public String getReasonsForReferral() {
		return reasonsForReferral;
	}

	public void setReasonsForReferral(@Nullable String reasonsForReferral) {
		this.reasonsForReferral = reasonsForReferral;
	}

	@Nullable
	public String getDx() {
		return dx;
	}

	public void setDx(@Nullable String dx) {
		this.dx = dx;
	}

	@Nullable
	public String getOrderAssociatedDiagnosis() {
		return orderAssociatedDiagnosis;
	}

	public void setOrderAssociatedDiagnosis(@Nullable String orderAssociatedDiagnosis) {
		this.orderAssociatedDiagnosis = orderAssociatedDiagnosis;
	}

	@Nullable
	public String getCallBackNumber() {
		return callBackNumber;
	}

	public void setCallBackNumber(@Nullable String callBackNumber) {
		this.callBackNumber = callBackNumber;
	}

	@Nullable
	public String getPreferredContactHours() {
		return preferredContactHours;
	}

	public void setPreferredContactHours(@Nullable String preferredContactHours) {
		this.preferredContactHours = preferredContactHours;
	}

	@Nullable
	public String getOrderComments() {
		return orderComments;
	}

	public void setOrderComments(@Nullable String orderComments) {
		this.orderComments = orderComments;
	}

	@Nullable
	public String getImgCcRecipients() {
		return imgCcRecipients;
	}

	public void setImgCcRecipients(@Nullable String imgCcRecipients) {
		this.imgCcRecipients = imgCcRecipients;
	}

	@Nullable
	public String getPatientAddressLine1() {
		return patientAddressLine1;
	}

	public void setPatientAddressLine1(@Nullable String patientAddressLine1) {
		this.patientAddressLine1 = patientAddressLine1;
	}

	@Nullable
	public String getPatientAddressLine2() {
		return patientAddressLine2;
	}

	public void setPatientAddressLine2(@Nullable String patientAddressLine2) {
		this.patientAddressLine2 = patientAddressLine2;
	}

	@Nullable
	public String getCity() {
		return city;
	}

	public void setCity(@Nullable String city) {
		this.city = city;
	}

	@Nullable
	public String getPatientState() {
		return patientState;
	}

	public void setPatientState(@Nullable String patientState) {
		this.patientState = patientState;
	}

	@Nullable
	public String getPatientZipCode() {
		return patientZipCode;
	}

	public void setPatientZipCode(@Nullable String patientZipCode) {
		this.patientZipCode = patientZipCode;
	}

	@Nullable
	public String getCcbhLastActiveMedOrderSummary() {
		return ccbhLastActiveMedOrderSummary;
	}

	public void setCcbhLastActiveMedOrderSummary(@Nullable String ccbhLastActiveMedOrderSummary) {
		this.ccbhLastActiveMedOrderSummary = ccbhLastActiveMedOrderSummary;
	}

	@Nullable
	public String getCcbhMedicationsList() {
		return ccbhMedicationsList;
	}

	public void setCcbhMedicationsList(@Nullable String ccbhMedicationsList) {
		this.ccbhMedicationsList = ccbhMedicationsList;
	}

	@Nullable
	public String getPsychotherapeuticMedLstTwoWeeks() {
		return psychotherapeuticMedLstTwoWeeks;
	}

	public void setPsychotherapeuticMedLstTwoWeeks(@Nullable String psychotherapeuticMedLstTwoWeeks) {
		this.psychotherapeuticMedLstTwoWeeks = psychotherapeuticMedLstTwoWeeks;
	}

	@Nullable
	public UUID getDispositionId() {
		return dispositionId;
	}

	public void setDispositionId(@Nullable UUID dispositionId) {
		this.dispositionId = dispositionId;
	}
}
