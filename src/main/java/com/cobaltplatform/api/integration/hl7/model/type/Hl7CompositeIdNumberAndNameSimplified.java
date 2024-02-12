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

import ca.uhn.hl7v2.model.v251.datatype.CNN;
import com.cobaltplatform.api.integration.hl7.model.Hl7Object;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * See https://hl7-definition.caristix.com/v2/hl7v2.5.1/DataTypes/CNN
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7CompositeIdNumberAndNameSimplified extends Hl7Object {
	@Nullable
	private String idNumber; // CNN.1 - Id Number
	@Nullable
	private String familyName; // CNN.2 - Family Name
	@Nullable
	private String givenName; // CNN.3 - Given Name
	@Nullable
	private String secondAndFurtherGivenNamesOrInitialsThereof; // CNN.4 - Second And Further Given Names Or Initials Thereof
	@Nullable
	private String suffix; // CNN.5 - Suffix
	@Nullable
	private String prefix; // CNN.6 - Prefix
	@Nullable
	private String degree; // CNN.7 - Degree
	@Nullable
	private String sourceTable; // CNN.8 - Source Table
	@Nullable
	private String assigningAuthorityNamespaceId; // CNN.9 - Assigning Authority - Namespace Id
	@Nullable
	private String assigningAuthorityUniversalId; // CNN.10 - Assigning Authority - Universal Id
	@Nullable
	private String assigningAuthorityUniversalIdType; // CNN.11 - Assigning Authority - Universal Id Type

	@Nonnull
	public static Boolean isPresent(@Nullable CNN cnn) {
		if (cnn == null)
			return false;

		return trimToNull(cnn.getIDNumber().getValueOrEmpty()) != null
				|| trimToNull(cnn.getFamilyName().getValueOrEmpty()) != null
				|| trimToNull(cnn.getGivenName().getValueOrEmpty()) != null
				|| trimToNull(cnn.getSecondAndFurtherGivenNamesOrInitialsThereof().getValueOrEmpty()) != null
				|| trimToNull(cnn.getSuffixEgJRorIII().getValueOrEmpty()) != null
				|| trimToNull(cnn.getPrefixEgDR().getValueOrEmpty()) != null
				|| trimToNull(cnn.getDegreeEgMD().getValueOrEmpty()) != null
				|| trimToNull(cnn.getSourceTable().getValueOrEmpty()) != null
				|| trimToNull(cnn.getAssigningAuthorityNamespaceID().getValueOrEmpty()) != null
				|| trimToNull(cnn.getAssigningAuthorityUniversalID().getValueOrEmpty()) != null
				|| trimToNull(cnn.getAssigningAuthorityUniversalIDType().getValueOrEmpty()) != null;
	}

	public Hl7CompositeIdNumberAndNameSimplified() {
		// Nothing to do
	}

	public Hl7CompositeIdNumberAndNameSimplified(@Nullable CNN cnn) {
		if (cnn != null) {
			this.idNumber = trimToNull(cnn.getIDNumber().getValueOrEmpty());
			this.familyName = trimToNull(cnn.getFamilyName().getValueOrEmpty());
			this.givenName = trimToNull(cnn.getGivenName().getValueOrEmpty());
			this.secondAndFurtherGivenNamesOrInitialsThereof = trimToNull(cnn.getSecondAndFurtherGivenNamesOrInitialsThereof().getValueOrEmpty());
			this.suffix = trimToNull(cnn.getSuffixEgJRorIII().getValueOrEmpty());
			this.prefix = trimToNull(cnn.getPrefixEgDR().getValueOrEmpty());
			this.degree = trimToNull(cnn.getDegreeEgMD().getValueOrEmpty());
			this.sourceTable = trimToNull(cnn.getSourceTable().getValueOrEmpty());
			this.assigningAuthorityNamespaceId = trimToNull(cnn.getAssigningAuthorityNamespaceID().getValueOrEmpty());
			this.assigningAuthorityUniversalId = trimToNull(cnn.getAssigningAuthorityUniversalID().getValueOrEmpty());
			this.assigningAuthorityUniversalIdType = trimToNull(cnn.getAssigningAuthorityUniversalIDType().getValueOrEmpty());
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
	public String getFamilyName() {
		return this.familyName;
	}

	public void setFamilyName(@Nullable String familyName) {
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
	public String getAssigningAuthorityNamespaceId() {
		return this.assigningAuthorityNamespaceId;
	}

	public void setAssigningAuthorityNamespaceId(@Nullable String assigningAuthorityNamespaceId) {
		this.assigningAuthorityNamespaceId = assigningAuthorityNamespaceId;
	}

	@Nullable
	public String getAssigningAuthorityUniversalId() {
		return this.assigningAuthorityUniversalId;
	}

	public void setAssigningAuthorityUniversalId(@Nullable String assigningAuthorityUniversalId) {
		this.assigningAuthorityUniversalId = assigningAuthorityUniversalId;
	}

	@Nullable
	public String getAssigningAuthorityUniversalIdType() {
		return this.assigningAuthorityUniversalIdType;
	}

	public void setAssigningAuthorityUniversalIdType(@Nullable String assigningAuthorityUniversalIdType) {
		this.assigningAuthorityUniversalIdType = assigningAuthorityUniversalIdType;
	}
}