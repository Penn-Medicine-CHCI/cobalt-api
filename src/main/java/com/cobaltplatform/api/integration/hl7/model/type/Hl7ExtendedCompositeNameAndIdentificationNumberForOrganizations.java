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

import ca.uhn.hl7v2.model.v251.datatype.XON;
import com.cobaltplatform.api.integration.hl7.model.Hl7Object;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * See https://hl7-definition.caristix.com/v2/HL7v2.5.1/DataTypes/XON
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7ExtendedCompositeNameAndIdentificationNumberForOrganizations extends Hl7Object {
	@Nullable
	private String organizationName; // XON.1 - Organization Name
	@Nullable
	private String organizationNameTypeCode; // XON.2 - Organization Name Type Code
	@Nullable
	private Integer idNumber; // XON.3 - Id Number
	@Nullable
	private Integer checkDigit; // XON.4 - Check Digit
	@Nullable
	private String checkDigitScheme; // XON.5 - Check Digit Scheme
	@Nullable
	private Hl7HierarchicDesignator assigningAuthority; // XON.6 - Assigning Authority
	@Nullable
	private String identifierTypeCode; // XON.7 - Identifier Type Code
	@Nullable
	private Hl7HierarchicDesignator assigningFacility; // XON.8 - Assigning Facility
	@Nullable
	private String nameRepresentationCode; // XON.9 - Name Representation Code
	@Nullable
	private String organizationIdentifier; // XON.10 - Organization Identifier

	@Nonnull
	public static Boolean isPresent(@Nullable XON xon) {
		if (xon == null)
			return false;

		return trimToNull(xon.getOrganizationName().getValueOrEmpty()) != null
				|| trimToNull(xon.getOrganizationNameTypeCode().getValueOrEmpty()) != null
				|| trimToNull(xon.getIDNumber().getValue()) != null
				|| trimToNull(xon.getCheckDigit().getValue()) != null
				|| trimToNull(xon.getCheckDigitScheme().getValueOrEmpty()) != null
				|| Hl7HierarchicDesignator.isPresent(xon.getAssigningAuthority())
				|| trimToNull(xon.getIdentifierTypeCode().getValueOrEmpty()) != null
				|| Hl7HierarchicDesignator.isPresent(xon.getAssigningFacility())
				|| trimToNull(xon.getNameRepresentationCode().getValueOrEmpty()) != null
				|| trimToNull(xon.getOrganizationIdentifier().getValueOrEmpty()) != null;
	}

	public Hl7ExtendedCompositeNameAndIdentificationNumberForOrganizations() {
		// Nothing to do
	}

	public Hl7ExtendedCompositeNameAndIdentificationNumberForOrganizations(@Nullable XON xon) {
		if (xon != null) {
			this.organizationName = trimToNull(xon.getOrganizationName().getValueOrEmpty());
			this.organizationNameTypeCode = trimToNull(xon.getOrganizationNameTypeCode().getValueOrEmpty());

			String idNumberAsString = trimToNull(xon.getIDNumber().getValue());
			if (idNumberAsString != null)
				this.idNumber = Integer.parseInt(idNumberAsString, 10);

			String checkDigitAsString = trimToNull(xon.getCheckDigit().getValue());
			if (checkDigitAsString != null)
				this.checkDigit = Integer.parseInt(checkDigitAsString, 10);

			this.checkDigitScheme = trimToNull(xon.getCheckDigitScheme().getValueOrEmpty());

			if (Hl7HierarchicDesignator.isPresent(xon.getAssigningAuthority()))
				this.assigningAuthority = new Hl7HierarchicDesignator(xon.getAssigningAuthority());

			this.identifierTypeCode = trimToNull(xon.getIdentifierTypeCode().getValueOrEmpty());

			if (Hl7HierarchicDesignator.isPresent(xon.getAssigningFacility()))
				this.assigningFacility = new Hl7HierarchicDesignator(xon.getAssigningFacility());

			this.nameRepresentationCode = trimToNull(xon.getNameRepresentationCode().getValueOrEmpty());
			this.organizationIdentifier = trimToNull(xon.getOrganizationIdentifier().getValueOrEmpty());
		}
	}

	@Nullable
	public String getOrganizationName() {
		return this.organizationName;
	}

	public void setOrganizationName(@Nullable String organizationName) {
		this.organizationName = organizationName;
	}

	@Nullable
	public String getOrganizationNameTypeCode() {
		return this.organizationNameTypeCode;
	}

	public void setOrganizationNameTypeCode(@Nullable String organizationNameTypeCode) {
		this.organizationNameTypeCode = organizationNameTypeCode;
	}

	@Nullable
	public Integer getIdNumber() {
		return this.idNumber;
	}

	public void setIdNumber(@Nullable Integer idNumber) {
		this.idNumber = idNumber;
	}

	@Nullable
	public Integer getCheckDigit() {
		return this.checkDigit;
	}

	public void setCheckDigit(@Nullable Integer checkDigit) {
		this.checkDigit = checkDigit;
	}

	@Nullable
	public String getCheckDigitScheme() {
		return this.checkDigitScheme;
	}

	public void setCheckDigitScheme(@Nullable String checkDigitScheme) {
		this.checkDigitScheme = checkDigitScheme;
	}

	@Nullable
	public Hl7HierarchicDesignator getAssigningAuthority() {
		return this.assigningAuthority;
	}

	public void setAssigningAuthority(@Nullable Hl7HierarchicDesignator assigningAuthority) {
		this.assigningAuthority = assigningAuthority;
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
	public String getOrganizationIdentifier() {
		return this.organizationIdentifier;
	}

	public void setOrganizationIdentifier(@Nullable String organizationIdentifier) {
		this.organizationIdentifier = organizationIdentifier;
	}
}