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
import com.cobaltplatform.api.integration.hl7.model.type.Hl7ExtendedAddress;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7ExtendedCompositeIdWithCheckDigit;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7ExtendedCompositeNameAndIdentificationNumberForOrganizations;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7ExtendedPersonName;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7ExtendedTelecommunicationNumber;

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
			
		}
	}
}