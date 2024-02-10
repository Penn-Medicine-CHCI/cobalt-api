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

	// TODO: finish up

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
				;
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
		}
	}

	// TODO: getters and setters
}