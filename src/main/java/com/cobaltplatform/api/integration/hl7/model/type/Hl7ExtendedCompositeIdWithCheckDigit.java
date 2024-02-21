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

import ca.uhn.hl7v2.model.v251.datatype.CX;
import com.cobaltplatform.api.integration.hl7.model.Hl7Object;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * See https://hl7-definition.caristix.com/v2/hl7v2.5.1/DataTypes/CX
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7ExtendedCompositeIdWithCheckDigit extends Hl7Object {
	@Nullable
	private String idNumber; // CX.1 - Id Number
	@Nullable
	private String checkDigit; // CX.2 - Check Digit
	@Nullable
	private String checkDigitScheme; // CX.3 - Check Digit Scheme
	@Nullable
	private Hl7HierarchicDesignator assigningAuthority; // CX.4 - Assigning Authority
	@Nullable
	private String identifierTypeCode; // CX.5 - Identifier Type Code
	@Nullable
	private Hl7HierarchicDesignator assigningFacility; // CX.6 - Assigning Facility
	@Nullable
	private String effectiveDate; // CX.7 - Effective Date
	@Nullable
	private String expirationDate; // CX.8 - Expiration Date
	@Nullable
	private Hl7CodedWithExceptions assigningJurisdiction; // CX.9 - Assigning Jurisdiction
	@Nullable
	private Hl7CodedWithExceptions assigningAgencyOrDepartment; // CX.10 - Assigning Agency Or Department

	@Nonnull
	public static Boolean isPresent(@Nullable CX cx) {
		if (cx == null)
			return false;

		return trimToNull(cx.getIDNumber().getValueOrEmpty()) != null
				|| trimToNull(cx.getCheckDigit().getValueOrEmpty()) != null
				|| Hl7HierarchicDesignator.isPresent(cx.getAssigningAuthority())
				|| trimToNull(cx.getCheckDigitScheme().getValueOrEmpty()) != null
				|| trimToNull(cx.getIdentifierTypeCode().getValueOrEmpty()) != null
				|| Hl7HierarchicDesignator.isPresent(cx.getAssigningFacility())
				|| trimToNull(cx.getEffectiveDate().getValue()) != null
				|| trimToNull(cx.getExpirationDate().getValue()) != null
				|| Hl7CodedWithExceptions.isPresent(cx.getAssigningJurisdiction())
				|| Hl7CodedWithExceptions.isPresent(cx.getAssigningAgencyOrDepartment());
	}

	public Hl7ExtendedCompositeIdWithCheckDigit() {
		// Nothing to do
	}

	public Hl7ExtendedCompositeIdWithCheckDigit(@Nullable CX cx) {
		if (cx != null) {
			this.idNumber = trimToNull(cx.getIDNumber().getValueOrEmpty());
			this.checkDigit = trimToNull(cx.getCheckDigit().getValueOrEmpty());
			this.checkDigitScheme = trimToNull(cx.getCheckDigitScheme().getValueOrEmpty());
			this.assigningAuthority = Hl7HierarchicDesignator.isPresent(cx.getAssigningAuthority()) ? new Hl7HierarchicDesignator(cx.getAssigningAuthority()) : null;
			this.identifierTypeCode = trimToNull(cx.getIdentifierTypeCode().getValueOrEmpty());
			this.assigningFacility = Hl7HierarchicDesignator.isPresent(cx.getAssigningFacility()) ? new Hl7HierarchicDesignator(cx.getAssigningFacility()) : null;
			this.effectiveDate = trimToNull(cx.getEffectiveDate().getValue());
			this.expirationDate = trimToNull(cx.getExpirationDate().getValue());
			this.assigningJurisdiction = Hl7CodedWithExceptions.isPresent(cx.getAssigningJurisdiction()) ? new Hl7CodedWithExceptions(cx.getAssigningJurisdiction()) : null;
			this.assigningAgencyOrDepartment = Hl7CodedWithExceptions.isPresent(cx.getAssigningAgencyOrDepartment()) ? new Hl7CodedWithExceptions(cx.getAssigningAgencyOrDepartment()) : null;
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
	public String getCheckDigit() {
		return this.checkDigit;
	}

	public void setCheckDigit(@Nullable String checkDigit) {
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
	public String getEffectiveDate() {
		return this.effectiveDate;
	}

	public void setEffectiveDate(@Nullable String effectiveDate) {
		this.effectiveDate = effectiveDate;
	}

	@Nullable
	public String getExpirationDate() {
		return this.expirationDate;
	}

	public void setExpirationDate(@Nullable String expirationDate) {
		this.expirationDate = expirationDate;
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