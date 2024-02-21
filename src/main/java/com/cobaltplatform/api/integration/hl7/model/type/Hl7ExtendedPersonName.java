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

import ca.uhn.hl7v2.model.v251.datatype.XPN;
import com.cobaltplatform.api.integration.hl7.model.Hl7Object;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * See https://hl7-definition.caristix.com/v2/hl7v2.5.1/DataTypes/XPN
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7ExtendedPersonName extends Hl7Object {
	@Nullable
	private Hl7FamilyName familyName; // XPN.1 - Family Name
	@Nullable
	private String givenName; // XPN.2 - Given Name
	@Nullable
	private String secondAndFurtherGivenNamesOrInitialsThereof; // XPN.3 - Second And Further Given Names Or Initials Thereof
	@Nullable
	private String suffix; // XPN.4 - Suffix (e.g., Jr Or Iii)
	@Nullable
	private String prefix; // XPN.5 - Prefix (e.g., Dr)
	@Nullable
	private String degree; // XPN.6 - Degree (e.g., Md)
	@Nullable
	private String nameTypeCode; // XPN.7 - Name Type Code
	@Nullable
	private String nameRepresentationCode; // XPN.8 - Name Representation Code
	@Nullable
	private Hl7CodedElement nameContext; // XPN.9 - Name Context
	@Nullable
	private Hl7DateTimeRange nameValidityRange; // XPN.10 - Name Validity Range
	@Nullable
	private String nameAssemblyOrder; // XPN.11 - Name Assembly Order
	@Nullable
	private Hl7TimeStamp effectiveDate; // XPN.12 - Effective Date
	@Nullable
	private Hl7TimeStamp expirationDate; // XPN.13 - Expiration Date
	@Nullable
	private String professionalSuffix; // XPN.14 - Professional Suffix

	@Nonnull
	public static Boolean isPresent(@Nullable XPN xpn) {
		if (xpn == null)
			return false;

		return Hl7FamilyName.isPresent(xpn.getFamilyName())
				|| trimToNull(xpn.getGivenName().getValueOrEmpty()) != null
				|| trimToNull(xpn.getSecondAndFurtherGivenNamesOrInitialsThereof().getValueOrEmpty()) != null
				|| trimToNull(xpn.getSuffixEgJRorIII().getValueOrEmpty()) != null
				|| trimToNull(xpn.getPrefixEgDR().getValueOrEmpty()) != null
				|| trimToNull(xpn.getDegreeEgMD().getValueOrEmpty()) != null
				|| trimToNull(xpn.getNameTypeCode().getValueOrEmpty()) != null
				|| trimToNull(xpn.getNameRepresentationCode().getValueOrEmpty()) != null
				|| Hl7CodedElement.isPresent(xpn.getNameContext())
				|| Hl7DateTimeRange.isPresent(xpn.getNameValidityRange())
				|| trimToNull(xpn.getNameAssemblyOrder().getValueOrEmpty()) != null
				|| Hl7TimeStamp.isPresent(xpn.getEffectiveDate())
				|| Hl7TimeStamp.isPresent(xpn.getExpirationDate())
				|| trimToNull(xpn.getProfessionalSuffix().getValueOrEmpty()) != null;
	}

	public Hl7ExtendedPersonName() {
		// Nothing to do
	}

	public Hl7ExtendedPersonName(@Nullable XPN xpn) {
		if (xpn != null) {
			this.familyName = Hl7FamilyName.isPresent(xpn.getFamilyName()) ? new Hl7FamilyName(xpn.getFamilyName()) : null;
			this.givenName = trimToNull(xpn.getGivenName().getValueOrEmpty());
			this.secondAndFurtherGivenNamesOrInitialsThereof = trimToNull(xpn.getSecondAndFurtherGivenNamesOrInitialsThereof().getValueOrEmpty());
			this.suffix = trimToNull(xpn.getSuffixEgJRorIII().getValueOrEmpty());
			this.prefix = trimToNull(xpn.getPrefixEgDR().getValueOrEmpty());
			this.degree = trimToNull(xpn.getDegreeEgMD().getValueOrEmpty());
			this.nameTypeCode = trimToNull(xpn.getNameTypeCode().getValueOrEmpty());
			this.nameRepresentationCode = trimToNull(xpn.getNameRepresentationCode().getValueOrEmpty());
			this.nameContext = Hl7CodedElement.isPresent(xpn.getNameContext()) ? new Hl7CodedElement(xpn.getNameContext()) : null;
			this.nameValidityRange = Hl7DateTimeRange.isPresent(xpn.getNameValidityRange()) ? new Hl7DateTimeRange(xpn.getNameValidityRange()) : null;
			this.nameAssemblyOrder = trimToNull(xpn.getNameAssemblyOrder().getValueOrEmpty());
			this.effectiveDate = Hl7TimeStamp.isPresent(xpn.getEffectiveDate()) ? new Hl7TimeStamp(xpn.getEffectiveDate()) : null;
			this.expirationDate = Hl7TimeStamp.isPresent(xpn.getExpirationDate()) ? new Hl7TimeStamp(xpn.getExpirationDate()) : null;
			this.professionalSuffix = trimToNull(xpn.getProfessionalSuffix().getValueOrEmpty());
		}
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
	public String getNameTypeCode() {
		return this.nameTypeCode;
	}

	public void setNameTypeCode(@Nullable String nameTypeCode) {
		this.nameTypeCode = nameTypeCode;
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
}