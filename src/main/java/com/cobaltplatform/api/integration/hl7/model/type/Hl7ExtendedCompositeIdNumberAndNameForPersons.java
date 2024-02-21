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

package com.cobaltplatform.api.integration.hl7.model.type;

import ca.uhn.hl7v2.model.v251.datatype.XCN;
import com.cobaltplatform.api.integration.hl7.model.Hl7Object;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * See https://hl7-definition.caristix.com/v2/HL7v2.5.1/DataTypes/XCN
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7ExtendedCompositeIdNumberAndNameForPersons extends Hl7Object {
	@Nullable
	private String idNumber; // XCN.1 - Id Number
	@Nullable
	private Hl7FamilyName familyName; // XCN.2 - Family Name
	@Nullable
	private String givenName; // XCN.3 - Given Name
	@Nullable
	private String secondAndFurtherGivenNamesOrInitialsThereof; // XCN.4 - Second And Further Given Names Or Initials Thereof
	@Nullable
	private String suffix; // XCN.5 - Suffix (e.g., Jr Or Iii)
	@Nullable
	private String prefix; // XCN.6 - Prefix (e.g., Dr)
	@Nullable
	private String degree; // XCN.7 - Degree (e.g., Md)
	@Nullable
	private String sourceTable; // XCN.8 - Source Table
	@Nullable
	private Hl7HierarchicDesignator assigningAuthority; // XCN.9 - Assigning Authority
	@Nullable
	private String nameTypeCode; // XCN.10 - Name Type Code
	@Nullable
	private String identifierCheckDigit; // XCN.11 - Identifier Check Digit
	@Nullable
	private String checkDigitScheme; // XCN.12 - Check Digit Scheme
	@Nullable
	private String identifierTypeCode; // XCN.13 - Identifier Type Code
	@Nullable
	private Hl7HierarchicDesignator assigningFacility; // XCN.14 - Assigning Facility
	@Nullable
	private String nameRepresentationCode; // XCN.15 - Name Representation Code
	@Nullable
	private Hl7CodedElement nameContext; // XCN.16 - Name Context
	@Nullable
	private Hl7DateTimeRange nameValidityRange; // XCN.17 - Name Validity Range
	@Nullable
	private String nameAssemblyOrder; // XCN.18 - Name Assembly Order
	@Nullable
	private Hl7TimeStamp effectiveDate; // XCN.19 - Effective Date
	@Nullable
	private Hl7TimeStamp expirationDate; // XCN.20 - Expiration Date
	@Nullable
	private String professionalSuffix; // XCN.21 - Professional Suffix
	@Nullable
	private Hl7CodedWithExceptions assigningJurisdiction; // XCN.22 - Assigning Jurisdiction
	@Nullable
	private Hl7CodedWithExceptions assigningAgencyOrDepartment; // XCN.23 - Assigning Agency Or Department

	@Nonnull
	public static Boolean isPresent(@Nullable XCN xcn) {
		if (xcn == null)
			return false;

		return trimToNull(xcn.getIDNumber().getValueOrEmpty()) != null
				|| Hl7FamilyName.isPresent(xcn.getFamilyName())
				|| trimToNull(xcn.getGivenName().getValueOrEmpty()) != null
				|| trimToNull(xcn.getSecondAndFurtherGivenNamesOrInitialsThereof().getValueOrEmpty()) != null
				|| trimToNull(xcn.getSuffixEgJRorIII().getValueOrEmpty()) != null
				|| trimToNull(xcn.getPrefixEgDR().getValueOrEmpty()) != null
				|| trimToNull(xcn.getDegreeEgMD().getValueOrEmpty()) != null
				|| trimToNull(xcn.getSourceTable().getValueOrEmpty()) != null
				|| Hl7HierarchicDesignator.isPresent(xcn.getAssigningAuthority()) != null
				|| trimToNull(xcn.getNameTypeCode().getValueOrEmpty()) != null
				|| trimToNull(xcn.getIdentifierCheckDigit().getValueOrEmpty()) != null
				|| trimToNull(xcn.getCheckDigitScheme().getValueOrEmpty()) != null
				|| trimToNull(xcn.getIdentifierTypeCode().getValueOrEmpty()) != null
				|| Hl7HierarchicDesignator.isPresent(xcn.getAssigningFacility())
				|| trimToNull(xcn.getNameRepresentationCode().getValueOrEmpty()) != null
				|| Hl7CodedElement.isPresent(xcn.getNameContext())
				|| Hl7DateTimeRange.isPresent(xcn.getNameValidityRange())
				|| trimToNull(xcn.getNameAssemblyOrder().getValueOrEmpty()) != null
				|| Hl7TimeStamp.isPresent(xcn.getEffectiveDate())
				|| Hl7TimeStamp.isPresent(xcn.getExpirationDate())
				|| trimToNull(xcn.getProfessionalSuffix().getValueOrEmpty()) != null
				|| Hl7CodedWithExceptions.isPresent(xcn.getAssigningJurisdiction())
				|| Hl7CodedWithExceptions.isPresent(xcn.getAssigningAgencyOrDepartment());
	}

	public Hl7ExtendedCompositeIdNumberAndNameForPersons() {
		// Nothing to do
	}

	public Hl7ExtendedCompositeIdNumberAndNameForPersons(@Nullable XCN xcn) {
		if (xcn != null) {
			this.idNumber = trimToNull(xcn.getIDNumber().getValueOrEmpty());
			this.familyName = Hl7FamilyName.isPresent(xcn.getFamilyName()) ? new Hl7FamilyName(xcn.getFamilyName()) : null;
			this.givenName = trimToNull(xcn.getGivenName().getValueOrEmpty());
			this.secondAndFurtherGivenNamesOrInitialsThereof = trimToNull(xcn.getSecondAndFurtherGivenNamesOrInitialsThereof().getValueOrEmpty());
			this.suffix = trimToNull(xcn.getSuffixEgJRorIII().getValueOrEmpty());
			this.prefix = trimToNull(xcn.getPrefixEgDR().getValueOrEmpty());
			this.degree = trimToNull(xcn.getDegreeEgMD().getValueOrEmpty());
			this.sourceTable = trimToNull(xcn.getSourceTable().getValueOrEmpty());
			this.assigningAuthority = Hl7HierarchicDesignator.isPresent(xcn.getAssigningAuthority()) ? new Hl7HierarchicDesignator(xcn.getAssigningAuthority()) : null;
			this.nameTypeCode = trimToNull(xcn.getNameTypeCode().getValueOrEmpty());
			this.identifierCheckDigit = trimToNull(xcn.getIdentifierCheckDigit().getValueOrEmpty());
			this.checkDigitScheme = trimToNull(xcn.getCheckDigitScheme().getValueOrEmpty());
			this.identifierTypeCode = trimToNull(xcn.getIdentifierTypeCode().getValueOrEmpty());
			this.assigningFacility = Hl7HierarchicDesignator.isPresent(xcn.getAssigningFacility()) ? new Hl7HierarchicDesignator(xcn.getAssigningFacility()) : null;
			this.nameRepresentationCode = trimToNull(xcn.getNameRepresentationCode().getValueOrEmpty());
			this.nameContext = Hl7CodedElement.isPresent(xcn.getNameContext()) ? new Hl7CodedElement(xcn.getNameContext()) : null;
			this.nameValidityRange = Hl7DateTimeRange.isPresent(xcn.getNameValidityRange()) ? new Hl7DateTimeRange(xcn.getNameValidityRange()) : null;
			this.nameAssemblyOrder = trimToNull(xcn.getNameAssemblyOrder().getValueOrEmpty());
			this.effectiveDate = Hl7TimeStamp.isPresent(xcn.getEffectiveDate()) ? new Hl7TimeStamp(xcn.getEffectiveDate()) : null;
			this.expirationDate = Hl7TimeStamp.isPresent(xcn.getExpirationDate()) ? new Hl7TimeStamp(xcn.getExpirationDate()) : null;
			this.professionalSuffix = trimToNull(xcn.getProfessionalSuffix().getValueOrEmpty());
			this.assigningJurisdiction = Hl7CodedWithExceptions.isPresent(xcn.getAssigningJurisdiction()) ? new Hl7CodedWithExceptions(xcn.getAssigningJurisdiction()) : null;
			this.assigningAgencyOrDepartment = Hl7CodedWithExceptions.isPresent(xcn.getAssigningAgencyOrDepartment()) ? new Hl7CodedWithExceptions(xcn.getAssigningAgencyOrDepartment()) : null;
		}
	}

	@Nullable
	public String getIdNumber() {
		return this.idNumber;
	}

	public void setIdNumber(@Nullable String idNumber) {
		this.idNumber = idNumber;
	}

	@Nullable
	public Hl7FamilyName getFamilyName() {
		return this.familyName;
	}

	public void setFamilyName(@Nullable Hl7FamilyName familyName) {
		this.familyName = familyName;
	}

	@Nullable
	public String getGivenName() {
		return this.givenName;
	}

	public void setGivenName(@Nullable String givenName) {
		this.givenName = givenName;
	}

	@Nullable
	public String getSecondAndFurtherGivenNamesOrInitialsThereof() {
		return this.secondAndFurtherGivenNamesOrInitialsThereof;
	}

	public void setSecondAndFurtherGivenNamesOrInitialsThereof(@Nullable String secondAndFurtherGivenNamesOrInitialsThereof) {
		this.secondAndFurtherGivenNamesOrInitialsThereof = secondAndFurtherGivenNamesOrInitialsThereof;
	}

	@Nullable
	public String getSuffix() {
		return this.suffix;
	}

	public void setSuffix(@Nullable String suffix) {
		this.suffix = suffix;
	}

	@Nullable
	public String getPrefix() {
		return this.prefix;
	}

	public void setPrefix(@Nullable String prefix) {
		this.prefix = prefix;
	}

	@Nullable
	public String getDegree() {
		return this.degree;
	}

	public void setDegree(@Nullable String degree) {
		this.degree = degree;
	}

	@Nullable
	public String getSourceTable() {
		return this.sourceTable;
	}

	public void setSourceTable(@Nullable String sourceTable) {
		this.sourceTable = sourceTable;
	}

	@Nullable
	public Hl7HierarchicDesignator getAssigningAuthority() {
		return this.assigningAuthority;
	}

	public void setAssigningAuthority(@Nullable Hl7HierarchicDesignator assigningAuthority) {
		this.assigningAuthority = assigningAuthority;
	}

	@Nullable
	public String getNameTypeCode() {
		return this.nameTypeCode;
	}

	public void setNameTypeCode(@Nullable String nameTypeCode) {
		this.nameTypeCode = nameTypeCode;
	}

	@Nullable
	public String getIdentifierCheckDigit() {
		return this.identifierCheckDigit;
	}

	public void setIdentifierCheckDigit(@Nullable String identifierCheckDigit) {
		this.identifierCheckDigit = identifierCheckDigit;
	}

	@Nullable
	public String getCheckDigitScheme() {
		return this.checkDigitScheme;
	}

	public void setCheckDigitScheme(@Nullable String checkDigitScheme) {
		this.checkDigitScheme = checkDigitScheme;
	}

	@Nullable
	public String getIdentifierTypeCode() {
		return this.identifierTypeCode;
	}

	public void setIdentifierTypeCode(@Nullable String identifierTypeCode) {
		this.identifierTypeCode = identifierTypeCode;
	}

	@Nullable
	public Hl7HierarchicDesignator getAssigningFacility() {
		return this.assigningFacility;
	}

	public void setAssigningFacility(@Nullable Hl7HierarchicDesignator assigningFacility) {
		this.assigningFacility = assigningFacility;
	}

	@Nullable
	public String getNameRepresentationCode() {
		return this.nameRepresentationCode;
	}

	public void setNameRepresentationCode(@Nullable String nameRepresentationCode) {
		this.nameRepresentationCode = nameRepresentationCode;
	}

	@Nullable
	public Hl7CodedElement getNameContext() {
		return this.nameContext;
	}

	public void setNameContext(@Nullable Hl7CodedElement nameContext) {
		this.nameContext = nameContext;
	}

	@Nullable
	public Hl7DateTimeRange getNameValidityRange() {
		return this.nameValidityRange;
	}

	public void setNameValidityRange(@Nullable Hl7DateTimeRange nameValidityRange) {
		this.nameValidityRange = nameValidityRange;
	}

	@Nullable
	public String getNameAssemblyOrder() {
		return this.nameAssemblyOrder;
	}

	public void setNameAssemblyOrder(@Nullable String nameAssemblyOrder) {
		this.nameAssemblyOrder = nameAssemblyOrder;
	}

	@Nullable
	public Hl7TimeStamp getEffectiveDate() {
		return this.effectiveDate;
	}

	public void setEffectiveDate(@Nullable Hl7TimeStamp effectiveDate) {
		this.effectiveDate = effectiveDate;
	}

	@Nullable
	public Hl7TimeStamp getExpirationDate() {
		return this.expirationDate;
	}

	public void setExpirationDate(@Nullable Hl7TimeStamp expirationDate) {
		this.expirationDate = expirationDate;
	}

	@Nullable
	public String getProfessionalSuffix() {
		return this.professionalSuffix;
	}

	public void setProfessionalSuffix(@Nullable String professionalSuffix) {
		this.professionalSuffix = professionalSuffix;
	}

	@Nullable
	public Hl7CodedWithExceptions getAssigningJurisdiction() {
		return this.assigningJurisdiction;
	}

	public void setAssigningJurisdiction(@Nullable Hl7CodedWithExceptions assigningJurisdiction) {
		this.assigningJurisdiction = assigningJurisdiction;
	}

	@Nullable
	public Hl7CodedWithExceptions getAssigningAgencyOrDepartment() {
		return this.assigningAgencyOrDepartment;
	}

	public void setAssigningAgencyOrDepartment(@Nullable Hl7CodedWithExceptions assigningAgencyOrDepartment) {
		this.assigningAgencyOrDepartment = assigningAgencyOrDepartment;
	}
}