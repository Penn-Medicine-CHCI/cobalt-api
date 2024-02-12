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

package com.cobaltplatform.api.integration.hl7.model.segment;

import ca.uhn.hl7v2.model.v251.segment.IN1;
import com.cobaltplatform.api.integration.hl7.model.Hl7Object;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7AuthorizationInformation;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7CodedElement;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7CompositePrice;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7ExtendedAddress;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7ExtendedCompositeIdNumberAndNameForPersons;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7ExtendedCompositeIdWithCheckDigit;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7ExtendedCompositeNameAndIdentificationNumberForOrganizations;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7ExtendedPersonName;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7ExtendedTelecommunicationNumber;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7TimeStamp;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * See https://hl7-definition.caristix.com/v2/hl7v2.5.1/Segments/IN1
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7InsuranceSegment extends Hl7Object {
	@Nullable
	private Integer setId; // IN1.1 - Set ID
	@Nullable
	private Hl7CodedElement insurancePlanId; // IN1.2 - Insurance Plan ID
	@Nullable
	private List<Hl7ExtendedCompositeIdWithCheckDigit> insuranceCompanyId; // IN1.3 - Insurance Company ID
	@Nullable
	private List<Hl7ExtendedCompositeNameAndIdentificationNumberForOrganizations> insuranceCompanyName; // IN1.4 - Insurance Company Name
	@Nullable
	private List<Hl7ExtendedAddress> insuranceCompanyAddress; // IN1.5 - Insurance Company Address
	@Nullable
	private List<Hl7ExtendedPersonName> insuranceCoContactPerson; // IN1.6 - Insurance Co Contact Person
	@Nullable
	private List<Hl7ExtendedTelecommunicationNumber> insuranceCoPhoneNumber; // IN1.7 - Insurance Co Phone Number
	@Nullable
	private String groupNumber; // IN1.8 - Group Number
	@Nullable
	private List<Hl7ExtendedCompositeNameAndIdentificationNumberForOrganizations> groupName; // IN1.9 - Group Name
	@Nullable
	private List<Hl7ExtendedCompositeIdWithCheckDigit> insuredsGroupEmpId; // IN1.10 - Insured's Group Emp ID
	@Nullable
	private List<Hl7ExtendedCompositeNameAndIdentificationNumberForOrganizations> insuredsGroupEmpName; // IN1.11 - Insured's Group Emp Name
	@Nullable
	private String planEffectiveDate; // IN1.12 - Plan Effective Date
	@Nullable
	private String planExpirationDate; // IN1.13 - Plan Expiration Date
	@Nullable
	private Hl7AuthorizationInformation authorizationInformation; // IN1.14 - Authorization Information
	@Nullable
	private String planType; // IN1.15- Plan Type
	@Nullable
	private List<Hl7ExtendedPersonName> nameOfInsured; // IN1.16 - Name Of Insured
	@Nullable
	private Hl7CodedElement insuredsRelationshipToPatient; // IN1.17 - Insured's Relationship To Patient
	@Nullable
	private Hl7TimeStamp insuredsDateOfBirth; // IN1.18 - Insured's Date Of Birth
	@Nullable
	private List<Hl7ExtendedAddress> insuredsAddress; // IN1.19 - Insured's Address
	@Nullable
	private String assignmentOfBenefits; // IN1.20 - Assignment Of Benefits
	@Nullable
	private String coordinationOfBenefits; // IN1.21 - Coordination Of Benefits
	@Nullable
	private String coordOfBenPriority; // IN1.22 - Coord Of Ben. Priority
	@Nullable
	private String noticeOfAdmissionFlag; // IN1.23 - Notice Of Admission Flag
	@Nullable
	private String noticeOfAdmissionDate; // IN1.24 - Notice Of Admission Date
	@Nullable
	private String reportOfEligibilityFlag; // IN1.25 - Report Of Eligibility Flag
	@Nullable
	private String reportOfEligibilityDate; // IN1.26 - Report Of Eligibility Date
	@Nullable
	private String releaseInformationCode; // IN1.27 - Release Information Code
	@Nullable
	private String preAdmitCert; // IN1.28 - Pre-Admit Cert (PAC)
	@Nullable
	private Hl7TimeStamp verificationDateTime; // IN1.29 - Verification Date/Time
	@Nullable
	private List<Hl7ExtendedCompositeIdNumberAndNameForPersons> verificationBy; // IN1.30 - Verification By
	@Nullable
	private String typeOfAgreementCode; // IN1.31 - Type Of Agreement Code
	@Nullable
	private String billingStatus; // IN1.32 - Billing Status
	@Nullable
	private Double lifetimeReserveDays; // IN1.33 - Lifetime Reserve Days
	@Nullable
	private Double delayBeforeLrDay; // IN1.34 - Delay Before L.R. Day
	@Nullable
	private String companyPlanCode; // IN1.35 - Company Plan Code
	@Nullable
	private String policyNumber; // IN1.36 - Policy Number
	@Nullable
	private Hl7CompositePrice policyDeductible; // IN1.37 - Policy Deductible
	@Nullable
	private Hl7CompositePrice policyLimitAmount; // IN1.38 - Policy Limit - Amount
	@Nullable
	private Double policyLimitDays; // IN1.39 - Policy Limit - Days
	@Nullable
	private Hl7CompositePrice roomRateSemiPrivate; // IN1.40 - Room Rate - Semi-Private
	@Nullable
	private Hl7CompositePrice roomRatePrivate; // IN1.41 - Room Rate - Private
	@Nullable
	private Hl7CodedElement insuredsEmploymentStatus; // IN1.42 - Insured's Employment Status
	@Nullable
	private String insuredsAdministrativeSex; // IN1.43 - Insured's Administrative Sex
	@Nullable
	private List<Hl7ExtendedAddress> insuredsEmployersAddress; // IN1.44 - Insured's Employer's Address
	@Nullable
	private String verificationStatus; // IN1.45 - Verification Status
	@Nullable
	private String priorInsurancePlanId; // IN1.46 - Prior Insurance Plan ID
	@Nullable
	private String coverageType; // IN1.47 - Coverage Type
	@Nullable
	private String handicap; // IN1.48 - Handicap
	@Nullable
	private List<Hl7ExtendedCompositeIdWithCheckDigit> insuredsIdNumber; // IN1.49 - Insured's ID Number
	@Nullable
	private String signatureCode; // IN1.50 - Signature Code
	@Nullable
	private String signatureCodeDate; // IN1.51 - Signature Code Date
	@Nullable
	private String insuredsBirthPlace; // IN1.52 - Insured's Birth Place
	@Nullable
	private String vipIndicator; // IN1.53 - VIP Indicator

	@Nonnull
	public static Boolean isPresent(@Nullable IN1 in1) {
		if (in1 == null)
			return false;

		return trimToNull(in1.getSetIDIN1().getValue()) != null;
	}

	public Hl7InsuranceSegment() {
		// Nothing to do
	}

	public Hl7InsuranceSegment(@Nullable IN1 in1) {
		if (in1 != null) {
			String setIdAsString = trimToNull(in1.getSetIDIN1().getValue());
			if (setIdAsString != null)
				this.setId = Integer.parseInt(setIdAsString, 10);

			if (Hl7CodedElement.isPresent(in1.getInsurancePlanID()))
				this.insurancePlanId = new Hl7CodedElement(in1.getInsurancePlanID());

			if (in1.getInsuranceCompanyID() != null && in1.getInsuranceCompanyID().length > 0)
				this.insuranceCompanyId = Arrays.stream(in1.getInsuranceCompanyID())
						.map(cx -> Hl7ExtendedCompositeIdWithCheckDigit.isPresent(cx) ? new Hl7ExtendedCompositeIdWithCheckDigit(cx) : null)
						.filter(insuranceCompanyId -> insuranceCompanyId != null)
						.collect(Collectors.toList());

			if (in1.getInsuranceCompanyName() != null && in1.getInsuranceCompanyName().length > 0)
				this.insuranceCompanyName = Arrays.stream(in1.getInsuranceCompanyName())
						.map(xon -> Hl7ExtendedCompositeNameAndIdentificationNumberForOrganizations.isPresent(xon) ? new Hl7ExtendedCompositeNameAndIdentificationNumberForOrganizations(xon) : null)
						.filter(insuranceCompanyName -> insuranceCompanyName != null)
						.collect(Collectors.toList());

			if (in1.getInsuranceCompanyAddress() != null && in1.getInsuranceCompanyAddress().length > 0)
				this.insuranceCompanyAddress = Arrays.stream(in1.getInsuranceCompanyAddress())
						.map(xad -> Hl7ExtendedAddress.isPresent(xad) ? new Hl7ExtendedAddress(xad) : null)
						.filter(insuranceCompanyAddress -> insuranceCompanyAddress != null)
						.collect(Collectors.toList());

			if (in1.getInsuranceCoContactPerson() != null && in1.getInsuranceCoContactPerson().length > 0)
				this.insuranceCoContactPerson = Arrays.stream(in1.getInsuranceCoContactPerson())
						.map(xpn -> Hl7ExtendedPersonName.isPresent(xpn) ? new Hl7ExtendedPersonName(xpn) : null)
						.filter(insuranceCoContactPerson -> insuranceCoContactPerson != null)
						.collect(Collectors.toList());

			if (in1.getInsuranceCoPhoneNumber() != null && in1.getInsuranceCoPhoneNumber().length > 0)
				this.insuranceCoPhoneNumber = Arrays.stream(in1.getInsuranceCoPhoneNumber())
						.map(xtn -> Hl7ExtendedTelecommunicationNumber.isPresent(xtn) ? new Hl7ExtendedTelecommunicationNumber(xtn) : null)
						.filter(insuranceCoPhoneNumber -> insuranceCoPhoneNumber != null)
						.collect(Collectors.toList());

			this.groupNumber = trimToNull(in1.getGroupNumber().getValueOrEmpty());

			if (in1.getGroupName() != null && in1.getGroupName().length > 0)
				this.groupName = Arrays.stream(in1.getGroupName())
						.map(xon -> Hl7ExtendedCompositeNameAndIdentificationNumberForOrganizations.isPresent(xon) ? new Hl7ExtendedCompositeNameAndIdentificationNumberForOrganizations(xon) : null)
						.filter(groupName -> groupName != null)
						.collect(Collectors.toList());

			if (in1.getInsuredSGroupEmpID() != null && in1.getInsuredSGroupEmpID().length > 0)
				this.insuredsGroupEmpId = Arrays.stream(in1.getInsuredSGroupEmpID())
						.map(cx -> Hl7ExtendedCompositeIdWithCheckDigit.isPresent(cx) ? new Hl7ExtendedCompositeIdWithCheckDigit(cx) : null)
						.filter(insuredsGroupEmpId -> insuredsGroupEmpId != null)
						.collect(Collectors.toList());

			if (in1.getInsuredSGroupEmpName() != null && in1.getInsuredSGroupEmpName().length > 0)
				this.insuredsGroupEmpName = Arrays.stream(in1.getInsuredSGroupEmpName())
						.map(xon -> Hl7ExtendedCompositeNameAndIdentificationNumberForOrganizations.isPresent(xon) ? new Hl7ExtendedCompositeNameAndIdentificationNumberForOrganizations(xon) : null)
						.filter(insuredsGroupEmpName -> insuredsGroupEmpName != null)
						.collect(Collectors.toList());

			this.planEffectiveDate = trimToNull(in1.getPlanEffectiveDate().getValue());
			this.planExpirationDate = trimToNull(in1.getPlanExpirationDate().getValue());

			if (Hl7AuthorizationInformation.isPresent(in1.getAuthorizationInformation()))
				this.authorizationInformation = new Hl7AuthorizationInformation(in1.getAuthorizationInformation());

			this.planType = trimToNull(in1.getPlanType().getValue());

			if (in1.getNameOfInsured() != null && in1.getNameOfInsured().length > 0)
				this.nameOfInsured = Arrays.stream(in1.getNameOfInsured())
						.map(xpn -> Hl7ExtendedPersonName.isPresent(xpn) ? new Hl7ExtendedPersonName(xpn) : null)
						.filter(nameOfInsured -> nameOfInsured != null)
						.collect(Collectors.toList());

			if (Hl7CodedElement.isPresent(in1.getInsuredSRelationshipToPatient()))
				this.insuredsRelationshipToPatient = new Hl7CodedElement(in1.getInsuredSRelationshipToPatient());

			if (Hl7TimeStamp.isPresent(in1.getInsuredSDateOfBirth()))
				this.insuredsDateOfBirth = new Hl7TimeStamp(in1.getInsuredSDateOfBirth());

			if (in1.getInsuredSAddress() != null && in1.getInsuredSAddress().length > 0)
				this.insuredsAddress = Arrays.stream(in1.getInsuredSAddress())
						.map(xad -> Hl7ExtendedAddress.isPresent(xad) ? new Hl7ExtendedAddress(xad) : null)
						.filter(insuredsAddress -> insuredsAddress != null)
						.collect(Collectors.toList());

			this.assignmentOfBenefits = trimToNull(in1.getAssignmentOfBenefits().getValueOrEmpty());
			this.coordinationOfBenefits = trimToNull(in1.getCoordinationOfBenefits().getValueOrEmpty());
			this.coordOfBenPriority = trimToNull(in1.getCoordOfBenPriority().getValueOrEmpty());
			this.noticeOfAdmissionFlag = trimToNull(in1.getNoticeOfAdmissionFlag().getValueOrEmpty());
			this.noticeOfAdmissionDate = trimToNull(in1.getNoticeOfAdmissionDate().getValue());
			this.reportOfEligibilityFlag = trimToNull(in1.getReportOfEligibilityFlag().getValueOrEmpty());
			this.reportOfEligibilityDate = trimToNull(in1.getReportOfEligibilityDate().getValue());
			this.releaseInformationCode = trimToNull(in1.getReleaseInformationCode().getValueOrEmpty());
			this.preAdmitCert = trimToNull(in1.getPreAdmitCert().getValueOrEmpty());

			if (Hl7TimeStamp.isPresent(in1.getVerificationDateTime()))
				this.verificationDateTime = new Hl7TimeStamp(in1.getVerificationDateTime());

			if (in1.getVerificationBy() != null && in1.getVerificationBy().length > 0)
				this.verificationBy = Arrays.stream(in1.getVerificationBy())
						.map(xcn -> Hl7ExtendedCompositeIdNumberAndNameForPersons.isPresent(xcn) ? new Hl7ExtendedCompositeIdNumberAndNameForPersons(xcn) : null)
						.filter(verificationBy -> verificationBy != null)
						.collect(Collectors.toList());

			this.typeOfAgreementCode = trimToNull(in1.getTypeOfAgreementCode().getValueOrEmpty());
			this.billingStatus = trimToNull(in1.getBillingStatus().getValueOrEmpty());

			String lifetimeReserveDaysAsString = trimToNull(in1.getLifetimeReserveDays().getValue());
			if (lifetimeReserveDaysAsString != null)
				this.lifetimeReserveDays = Double.parseDouble(lifetimeReserveDaysAsString);

			String delayBeforeLrDayAsString = trimToNull(in1.getDelayBeforeLRDay().getValue());
			if (delayBeforeLrDayAsString != null)
				this.delayBeforeLrDay = Double.parseDouble(delayBeforeLrDayAsString);

			this.companyPlanCode = trimToNull(in1.getCompanyPlanCode().getValueOrEmpty());
			this.policyNumber = trimToNull(in1.getPolicyNumber().getValueOrEmpty());

			if (Hl7CompositePrice.isPresent(in1.getPolicyDeductible()))
				this.policyDeductible = new Hl7CompositePrice(in1.getPolicyDeductible());

			if (Hl7CompositePrice.isPresent(in1.getPolicyLimitAmount()))
				this.policyLimitAmount = new Hl7CompositePrice(in1.getPolicyLimitAmount());

			String policyLimitDaysAsString = trimToNull(in1.getPolicyLimitDays().getValue());
			if (policyLimitDaysAsString != null)
				this.policyLimitDays = Double.parseDouble(policyLimitDaysAsString);

			if (Hl7CompositePrice.isPresent(in1.getRoomRateSemiPrivate()))
				this.roomRateSemiPrivate = new Hl7CompositePrice(in1.getRoomRateSemiPrivate());

			if (Hl7CompositePrice.isPresent(in1.getRoomRatePrivate()))
				this.roomRatePrivate = new Hl7CompositePrice(in1.getRoomRatePrivate());

			if (Hl7CodedElement.isPresent(in1.getInsuredSEmploymentStatus()))
				this.insuredsEmploymentStatus = new Hl7CodedElement(in1.getInsuredSEmploymentStatus());

			this.insuredsAdministrativeSex = trimToNull(in1.getInsuredSAdministrativeSex().getValueOrEmpty());

			if (in1.getInsuredSEmployerSAddress() != null && in1.getInsuredSEmployerSAddress().length > 0)
				this.insuredsEmployersAddress = Arrays.stream(in1.getInsuredSEmployerSAddress())
						.map(xad -> Hl7ExtendedAddress.isPresent(xad) ? new Hl7ExtendedAddress(xad) : null)
						.filter(insuredsEmployersAddress -> insuredsEmployersAddress != null)
						.collect(Collectors.toList());

			this.verificationStatus = trimToNull(in1.getVerificationStatus().getValueOrEmpty());
			this.priorInsurancePlanId = trimToNull(in1.getPriorInsurancePlanID().getValueOrEmpty());
			this.coverageType = trimToNull(in1.getCoverageType().getValueOrEmpty());
			this.handicap = trimToNull(in1.getHandicap().getValueOrEmpty());

			if (in1.getInsuredSIDNumber() != null && in1.getInsuredSIDNumber().length > 0)
				this.insuredsIdNumber = Arrays.stream(in1.getInsuredSIDNumber())
						.map(cx -> Hl7ExtendedCompositeIdWithCheckDigit.isPresent(cx) ? new Hl7ExtendedCompositeIdWithCheckDigit(cx) : null)
						.filter(insuredsIdNumber -> insuredsIdNumber != null)
						.collect(Collectors.toList());

			this.signatureCode = trimToNull(in1.getSignatureCode().getValueOrEmpty());
			this.signatureCodeDate = trimToNull(in1.getSignatureCodeDate().getValue());
			this.insuredsBirthPlace = trimToNull(in1.getInsuredSBirthPlace().getValueOrEmpty());
			this.vipIndicator = trimToNull(in1.getVIPIndicator().getValueOrEmpty());
		}
	}

	@Nullable
	public Integer getSetId() {
		return this.setId;
	}

	public void setSetId(@Nullable Integer setId) {
		this.setId = setId;
	}

	@Nullable
	public Hl7CodedElement getInsurancePlanId() {
		return this.insurancePlanId;
	}

	public void setInsurancePlanId(@Nullable Hl7CodedElement insurancePlanId) {
		this.insurancePlanId = insurancePlanId;
	}

	@Nullable
	public List<Hl7ExtendedCompositeIdWithCheckDigit> getInsuranceCompanyId() {
		return this.insuranceCompanyId;
	}

	public void setInsuranceCompanyId(@Nullable List<Hl7ExtendedCompositeIdWithCheckDigit> insuranceCompanyId) {
		this.insuranceCompanyId = insuranceCompanyId;
	}

	@Nullable
	public List<Hl7ExtendedCompositeNameAndIdentificationNumberForOrganizations> getInsuranceCompanyName() {
		return this.insuranceCompanyName;
	}

	public void setInsuranceCompanyName(@Nullable List<Hl7ExtendedCompositeNameAndIdentificationNumberForOrganizations> insuranceCompanyName) {
		this.insuranceCompanyName = insuranceCompanyName;
	}

	@Nullable
	public List<Hl7ExtendedAddress> getInsuranceCompanyAddress() {
		return this.insuranceCompanyAddress;
	}

	public void setInsuranceCompanyAddress(@Nullable List<Hl7ExtendedAddress> insuranceCompanyAddress) {
		this.insuranceCompanyAddress = insuranceCompanyAddress;
	}

	@Nullable
	public List<Hl7ExtendedPersonName> getInsuranceCoContactPerson() {
		return this.insuranceCoContactPerson;
	}

	public void setInsuranceCoContactPerson(@Nullable List<Hl7ExtendedPersonName> insuranceCoContactPerson) {
		this.insuranceCoContactPerson = insuranceCoContactPerson;
	}

	@Nullable
	public List<Hl7ExtendedTelecommunicationNumber> getInsuranceCoPhoneNumber() {
		return this.insuranceCoPhoneNumber;
	}

	public void setInsuranceCoPhoneNumber(@Nullable List<Hl7ExtendedTelecommunicationNumber> insuranceCoPhoneNumber) {
		this.insuranceCoPhoneNumber = insuranceCoPhoneNumber;
	}

	@Nullable
	public String getGroupNumber() {
		return this.groupNumber;
	}

	public void setGroupNumber(@Nullable String groupNumber) {
		this.groupNumber = groupNumber;
	}

	@Nullable
	public List<Hl7ExtendedCompositeNameAndIdentificationNumberForOrganizations> getGroupName() {
		return this.groupName;
	}

	public void setGroupName(@Nullable List<Hl7ExtendedCompositeNameAndIdentificationNumberForOrganizations> groupName) {
		this.groupName = groupName;
	}

	@Nullable
	public List<Hl7ExtendedCompositeIdWithCheckDigit> getInsuredsGroupEmpId() {
		return this.insuredsGroupEmpId;
	}

	public void setInsuredsGroupEmpId(@Nullable List<Hl7ExtendedCompositeIdWithCheckDigit> insuredsGroupEmpId) {
		this.insuredsGroupEmpId = insuredsGroupEmpId;
	}

	@Nullable
	public List<Hl7ExtendedCompositeNameAndIdentificationNumberForOrganizations> getInsuredsGroupEmpName() {
		return this.insuredsGroupEmpName;
	}

	public void setInsuredsGroupEmpName(@Nullable List<Hl7ExtendedCompositeNameAndIdentificationNumberForOrganizations> insuredsGroupEmpName) {
		this.insuredsGroupEmpName = insuredsGroupEmpName;
	}

	@Nullable
	public String getPlanEffectiveDate() {
		return this.planEffectiveDate;
	}

	public void setPlanEffectiveDate(@Nullable String planEffectiveDate) {
		this.planEffectiveDate = planEffectiveDate;
	}

	@Nullable
	public String getPlanExpirationDate() {
		return this.planExpirationDate;
	}

	public void setPlanExpirationDate(@Nullable String planExpirationDate) {
		this.planExpirationDate = planExpirationDate;
	}

	@Nullable
	public Hl7AuthorizationInformation getAuthorizationInformation() {
		return this.authorizationInformation;
	}

	public void setAuthorizationInformation(@Nullable Hl7AuthorizationInformation authorizationInformation) {
		this.authorizationInformation = authorizationInformation;
	}

	@Nullable
	public String getPlanType() {
		return this.planType;
	}

	public void setPlanType(@Nullable String planType) {
		this.planType = planType;
	}

	@Nullable
	public List<Hl7ExtendedPersonName> getNameOfInsured() {
		return this.nameOfInsured;
	}

	public void setNameOfInsured(@Nullable List<Hl7ExtendedPersonName> nameOfInsured) {
		this.nameOfInsured = nameOfInsured;
	}

	@Nullable
	public Hl7CodedElement getInsuredsRelationshipToPatient() {
		return this.insuredsRelationshipToPatient;
	}

	public void setInsuredsRelationshipToPatient(@Nullable Hl7CodedElement insuredsRelationshipToPatient) {
		this.insuredsRelationshipToPatient = insuredsRelationshipToPatient;
	}

	@Nullable
	public Hl7TimeStamp getInsuredsDateOfBirth() {
		return this.insuredsDateOfBirth;
	}

	public void setInsuredsDateOfBirth(@Nullable Hl7TimeStamp insuredsDateOfBirth) {
		this.insuredsDateOfBirth = insuredsDateOfBirth;
	}

	@Nullable
	public List<Hl7ExtendedAddress> getInsuredsAddress() {
		return this.insuredsAddress;
	}

	public void setInsuredsAddress(@Nullable List<Hl7ExtendedAddress> insuredsAddress) {
		this.insuredsAddress = insuredsAddress;
	}

	@Nullable
	public String getAssignmentOfBenefits() {
		return this.assignmentOfBenefits;
	}

	public void setAssignmentOfBenefits(@Nullable String assignmentOfBenefits) {
		this.assignmentOfBenefits = assignmentOfBenefits;
	}

	@Nullable
	public String getCoordinationOfBenefits() {
		return this.coordinationOfBenefits;
	}

	public void setCoordinationOfBenefits(@Nullable String coordinationOfBenefits) {
		this.coordinationOfBenefits = coordinationOfBenefits;
	}

	@Nullable
	public String getCoordOfBenPriority() {
		return this.coordOfBenPriority;
	}

	public void setCoordOfBenPriority(@Nullable String coordOfBenPriority) {
		this.coordOfBenPriority = coordOfBenPriority;
	}

	@Nullable
	public String getNoticeOfAdmissionFlag() {
		return this.noticeOfAdmissionFlag;
	}

	public void setNoticeOfAdmissionFlag(@Nullable String noticeOfAdmissionFlag) {
		this.noticeOfAdmissionFlag = noticeOfAdmissionFlag;
	}

	@Nullable
	public String getNoticeOfAdmissionDate() {
		return this.noticeOfAdmissionDate;
	}

	public void setNoticeOfAdmissionDate(@Nullable String noticeOfAdmissionDate) {
		this.noticeOfAdmissionDate = noticeOfAdmissionDate;
	}

	@Nullable
	public String getReportOfEligibilityFlag() {
		return this.reportOfEligibilityFlag;
	}

	public void setReportOfEligibilityFlag(@Nullable String reportOfEligibilityFlag) {
		this.reportOfEligibilityFlag = reportOfEligibilityFlag;
	}

	@Nullable
	public String getReportOfEligibilityDate() {
		return this.reportOfEligibilityDate;
	}

	public void setReportOfEligibilityDate(@Nullable String reportOfEligibilityDate) {
		this.reportOfEligibilityDate = reportOfEligibilityDate;
	}

	@Nullable
	public String getReleaseInformationCode() {
		return this.releaseInformationCode;
	}

	public void setReleaseInformationCode(@Nullable String releaseInformationCode) {
		this.releaseInformationCode = releaseInformationCode;
	}

	@Nullable
	public String getPreAdmitCert() {
		return this.preAdmitCert;
	}

	public void setPreAdmitCert(@Nullable String preAdmitCert) {
		this.preAdmitCert = preAdmitCert;
	}

	@Nullable
	public Hl7TimeStamp getVerificationDateTime() {
		return this.verificationDateTime;
	}

	public void setVerificationDateTime(@Nullable Hl7TimeStamp verificationDateTime) {
		this.verificationDateTime = verificationDateTime;
	}

	@Nullable
	public List<Hl7ExtendedCompositeIdNumberAndNameForPersons> getVerificationBy() {
		return this.verificationBy;
	}

	public void setVerificationBy(@Nullable List<Hl7ExtendedCompositeIdNumberAndNameForPersons> verificationBy) {
		this.verificationBy = verificationBy;
	}

	@Nullable
	public String getTypeOfAgreementCode() {
		return this.typeOfAgreementCode;
	}

	public void setTypeOfAgreementCode(@Nullable String typeOfAgreementCode) {
		this.typeOfAgreementCode = typeOfAgreementCode;
	}

	@Nullable
	public String getBillingStatus() {
		return this.billingStatus;
	}

	public void setBillingStatus(@Nullable String billingStatus) {
		this.billingStatus = billingStatus;
	}

	@Nullable
	public Double getLifetimeReserveDays() {
		return this.lifetimeReserveDays;
	}

	public void setLifetimeReserveDays(@Nullable Double lifetimeReserveDays) {
		this.lifetimeReserveDays = lifetimeReserveDays;
	}

	@Nullable
	public Double getDelayBeforeLrDay() {
		return this.delayBeforeLrDay;
	}

	public void setDelayBeforeLrDay(@Nullable Double delayBeforeLrDay) {
		this.delayBeforeLrDay = delayBeforeLrDay;
	}

	@Nullable
	public String getCompanyPlanCode() {
		return this.companyPlanCode;
	}

	public void setCompanyPlanCode(@Nullable String companyPlanCode) {
		this.companyPlanCode = companyPlanCode;
	}

	@Nullable
	public String getPolicyNumber() {
		return this.policyNumber;
	}

	public void setPolicyNumber(@Nullable String policyNumber) {
		this.policyNumber = policyNumber;
	}

	@Nullable
	public Hl7CompositePrice getPolicyDeductible() {
		return this.policyDeductible;
	}

	public void setPolicyDeductible(@Nullable Hl7CompositePrice policyDeductible) {
		this.policyDeductible = policyDeductible;
	}

	@Nullable
	public Hl7CompositePrice getPolicyLimitAmount() {
		return this.policyLimitAmount;
	}

	public void setPolicyLimitAmount(@Nullable Hl7CompositePrice policyLimitAmount) {
		this.policyLimitAmount = policyLimitAmount;
	}

	@Nullable
	public Double getPolicyLimitDays() {
		return this.policyLimitDays;
	}

	public void setPolicyLimitDays(@Nullable Double policyLimitDays) {
		this.policyLimitDays = policyLimitDays;
	}

	@Nullable
	public Hl7CompositePrice getRoomRateSemiPrivate() {
		return this.roomRateSemiPrivate;
	}

	public void setRoomRateSemiPrivate(@Nullable Hl7CompositePrice roomRateSemiPrivate) {
		this.roomRateSemiPrivate = roomRateSemiPrivate;
	}

	@Nullable
	public Hl7CompositePrice getRoomRatePrivate() {
		return this.roomRatePrivate;
	}

	public void setRoomRatePrivate(@Nullable Hl7CompositePrice roomRatePrivate) {
		this.roomRatePrivate = roomRatePrivate;
	}

	@Nullable
	public Hl7CodedElement getInsuredsEmploymentStatus() {
		return this.insuredsEmploymentStatus;
	}

	public void setInsuredsEmploymentStatus(@Nullable Hl7CodedElement insuredsEmploymentStatus) {
		this.insuredsEmploymentStatus = insuredsEmploymentStatus;
	}

	@Nullable
	public String getInsuredsAdministrativeSex() {
		return this.insuredsAdministrativeSex;
	}

	public void setInsuredsAdministrativeSex(@Nullable String insuredsAdministrativeSex) {
		this.insuredsAdministrativeSex = insuredsAdministrativeSex;
	}

	@Nullable
	public List<Hl7ExtendedAddress> getInsuredsEmployersAddress() {
		return this.insuredsEmployersAddress;
	}

	public void setInsuredsEmployersAddress(@Nullable List<Hl7ExtendedAddress> insuredsEmployersAddress) {
		this.insuredsEmployersAddress = insuredsEmployersAddress;
	}

	@Nullable
	public String getVerificationStatus() {
		return this.verificationStatus;
	}

	public void setVerificationStatus(@Nullable String verificationStatus) {
		this.verificationStatus = verificationStatus;
	}

	@Nullable
	public String getPriorInsurancePlanId() {
		return this.priorInsurancePlanId;
	}

	public void setPriorInsurancePlanId(@Nullable String priorInsurancePlanId) {
		this.priorInsurancePlanId = priorInsurancePlanId;
	}

	@Nullable
	public String getCoverageType() {
		return this.coverageType;
	}

	public void setCoverageType(@Nullable String coverageType) {
		this.coverageType = coverageType;
	}

	@Nullable
	public String getHandicap() {
		return this.handicap;
	}

	public void setHandicap(@Nullable String handicap) {
		this.handicap = handicap;
	}

	@Nullable
	public List<Hl7ExtendedCompositeIdWithCheckDigit> getInsuredsIdNumber() {
		return this.insuredsIdNumber;
	}

	public void setInsuredsIdNumber(@Nullable List<Hl7ExtendedCompositeIdWithCheckDigit> insuredsIdNumber) {
		this.insuredsIdNumber = insuredsIdNumber;
	}

	@Nullable
	public String getSignatureCode() {
		return this.signatureCode;
	}

	public void setSignatureCode(@Nullable String signatureCode) {
		this.signatureCode = signatureCode;
	}

	@Nullable
	public String getSignatureCodeDate() {
		return this.signatureCodeDate;
	}

	public void setSignatureCodeDate(@Nullable String signatureCodeDate) {
		this.signatureCodeDate = signatureCodeDate;
	}

	@Nullable
	public String getInsuredsBirthPlace() {
		return this.insuredsBirthPlace;
	}

	public void setInsuredsBirthPlace(@Nullable String insuredsBirthPlace) {
		this.insuredsBirthPlace = insuredsBirthPlace;
	}

	@Nullable
	public String getVipIndicator() {
		return this.vipIndicator;
	}

	public void setVipIndicator(@Nullable String vipIndicator) {
		this.vipIndicator = vipIndicator;
	}
}