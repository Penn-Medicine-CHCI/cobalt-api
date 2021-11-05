package com.cobaltplatform.ic.backend.model.db;

import javax.persistence.*;

import io.ebean.Model;

@Entity
@Table(name="referral_order_report", schema = "ic")
public class DReferralOrderReport extends Model {
    @ManyToOne
    private DPatientDisposition disposition;


    private String encounterDeptName;


    private String encounterDeptId;


    private String referringPractice;


    private String referringPracticeSecond;


    private String orderingProvider;


    private String billingProvider;


    private String lastName;


    private String firstName;


    private String mrn;


    private String uid;


    private String sex;


    private String dateOfBirth;


    private String primaryPayor;


    private String primaryPlan;


    private String orderDate;

    @Id
    private String orderId;

    private String ageOfOrder;


    private String ccbhOrderRouting;


    private String reasonsForReferral;

    private String dx;


    private String orderAssociatedDiagnosis;


    private String callBackNumber;


    private String preferredContactHours;


    @Lob
    private String orderComments;


    private String imgCcRecipients;


    private String patientAddressLine1;


    private String patientAddressLine2;


    private String city;


    private String patientState;


    private String patientZipCode;


    private String ccbhLastActiveMedOrderSummary;


    private String ccbhMedicationsList;


    private String psychotherapeuticMedLstTwoWeeks;

    public String getEncounterDeptName() {
        return encounterDeptName;
    }

    public DReferralOrderReport setEncounterDeptName(final String encounterDeptName) {
        this.encounterDeptName = encounterDeptName;
        return this;
    }

    public String getEncounterDeptId() {
        return encounterDeptId;
    }

    public DReferralOrderReport setEncounterDeptId(final String encounterDeptId) {
        this.encounterDeptId = encounterDeptId;
        return this;
    }

    public String getReferringPractice() {
        return referringPractice;
    }

    public DReferralOrderReport setReferringPractice(final String referringPractice) {
        this.referringPractice = referringPractice;
        return this;
    }

    public String getReferringPracticeSecond() {
        return referringPracticeSecond;
    }

    public DReferralOrderReport setReferringPracticeSecond(final String referringPracticeSecond) {
        this.referringPracticeSecond = referringPracticeSecond;
        return this;
    }

    public String getOrderingProvider() {
        return orderingProvider;
    }

    public DReferralOrderReport setOrderingProvider(final String orderingProvider) {
        this.orderingProvider = orderingProvider;
        return this;
    }

    public String getBillingProvider() {
        return billingProvider;
    }

    public DReferralOrderReport setBillingProvider(final String billingProvider) {
        this.billingProvider = billingProvider;
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public DReferralOrderReport setLastName(final String lastName) {
        this.lastName = lastName;
        return this;
    }

    public String getFirstName() {
        return firstName;
    }

    public DReferralOrderReport setFirstName(final String firstName) {
        this.firstName = firstName;
        return this;
    }

    public String getMrn() {
        return mrn;
    }

    public DReferralOrderReport setMrn(final String mrn) {
        this.mrn = mrn;
        return this;
    }

    public String getUid() {
        return uid;
    }

    public DReferralOrderReport setUid(final String uid) {
        this.uid = uid;
        return this;
    }

    public String getSex() {
        return sex;
    }

    public DReferralOrderReport setSex(final String sex) {
        this.sex = sex;
        return this;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public DReferralOrderReport setDateOfBirth(final String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
        return this;
    }

    public String getPrimaryPayor() {
        return primaryPayor;
    }

    public DReferralOrderReport setPrimaryPayor(final String primaryPayor) {
        this.primaryPayor = primaryPayor;
        return this;
    }

    public String getPrimaryPlan() {
        return primaryPlan;
    }

    public DReferralOrderReport setPrimaryPlan(final String primaryPlan) {
        this.primaryPlan = primaryPlan;
        return this;
    }

    public String getOrderDate() {
        return orderDate;
    }

    public DReferralOrderReport setOrderDate(final String orderDate) {
        this.orderDate = orderDate;
        return this;
    }

    public String getOrderId() {
        return orderId;
    }

    public DReferralOrderReport setOrderId(final String orderId) {
        this.orderId = orderId;
        return this;
    }

    public String getAgeOfOrder() {
        return ageOfOrder;
    }

    public DReferralOrderReport setAgeOfOrder(final String ageOfOrder) {
        this.ageOfOrder = ageOfOrder;
        return this;
    }

    public String getCcbhOrderRouting() {
        return ccbhOrderRouting;
    }

    public DReferralOrderReport setCcbhOrderRouting(final String ccbhOrderRouting) {
        this.ccbhOrderRouting = ccbhOrderRouting;
        return this;
    }

    public String getReasonsForReferral() {
        return reasonsForReferral;
    }

    public DReferralOrderReport setReasonsForReferral(final String reasonsForReferral) {
        this.reasonsForReferral = reasonsForReferral;
        return this;
    }

    public String getDx() {
        return dx;
    }

    public DReferralOrderReport setDx(final String dx) {
        this.dx = dx;
        return this;
    }

    public String getOrderAssociatedDiagnosis() {
        return orderAssociatedDiagnosis;
    }

    public DReferralOrderReport setOrderAssociatedDiagnosis(final String orderAssociatedDiagnosis) {
        this.orderAssociatedDiagnosis = orderAssociatedDiagnosis;
        return this;
    }

    public String getCallBackNumber() {
        return callBackNumber;
    }

    public DReferralOrderReport setCallBackNumber(final String callBackNumber) {
        this.callBackNumber = callBackNumber;
        return this;
    }

    public String getPreferredContactHours() {
        return preferredContactHours;
    }

    public DReferralOrderReport setPreferredContactHours(final String preferredContactHours) {
        this.preferredContactHours = preferredContactHours;
        return this;
    }

    public String getOrderComments() {
        return orderComments;
    }

    public DReferralOrderReport setOrderComments(final String orderComments) {
        this.orderComments = orderComments;
        return this;
    }

    public String getImgCcRecipients() {
        return imgCcRecipients;
    }

    public DReferralOrderReport setImgCcRecipients(final String imgCcRecipients) {
        this.imgCcRecipients = imgCcRecipients;
        return this;
    }

    public String getPatientAddressLine1() {
        return patientAddressLine1;
    }

    public DReferralOrderReport setPatientAddressLine1(final String patientAddressLine1) {
        this.patientAddressLine1 = patientAddressLine1;
        return this;
    }

    public String getPatientAddressLine2() {
        return patientAddressLine2;
    }

    public DReferralOrderReport setPatientAddressLine2(final String patientAddressLine2) {
        this.patientAddressLine2 = patientAddressLine2;
        return this;
    }

    public String getCity() {
        return city;
    }

    public DReferralOrderReport setCity(final String city) {
        this.city = city;
        return this;
    }

    public String getPatientState() {
        return patientState;
    }

    public DReferralOrderReport setPatientState(final String patientState) {
        this.patientState = patientState;
        return this;
    }

    public String getPatientZipCode() {
        return patientZipCode;
    }

    public DReferralOrderReport setPatientZipCode(final String patientZipCode) {
        this.patientZipCode = patientZipCode;
        return this;
    }

    public String getCcbhLastActiveMedOrderSummary() {
        return ccbhLastActiveMedOrderSummary;
    }

    public DReferralOrderReport setCcbhLastActiveMedOrderSummary(final String ccbhLastActiveMedOrderSummary) {
        this.ccbhLastActiveMedOrderSummary = ccbhLastActiveMedOrderSummary;
        return this;
    }

    public String getCcbhMedicationsList() {
        return ccbhMedicationsList;
    }

    public DReferralOrderReport setCcbhMedicationsList(final String ccbhMedicationsList) {
        this.ccbhMedicationsList = ccbhMedicationsList;
        return this;
    }

    public String getPsychotherapeuticMedLstTwoWeeks() {
        return psychotherapeuticMedLstTwoWeeks;
    }

    public DReferralOrderReport setPsychotherapeuticMedLstTwoWeeks(final String psychotherapeuticMedLstTwoWeeks) {
        this.psychotherapeuticMedLstTwoWeeks = psychotherapeuticMedLstTwoWeeks;
        return this;
    }

    public DPatientDisposition getDisposition() {
        return this.disposition;
    }

    public DReferralOrderReport setDisposition(final DPatientDisposition disposition) {
        this.disposition = disposition;
        return this;
    }
}
