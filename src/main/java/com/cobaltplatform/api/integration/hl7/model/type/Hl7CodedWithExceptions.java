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

import ca.uhn.hl7v2.model.v251.datatype.CWE;
import com.cobaltplatform.api.integration.hl7.model.Hl7Object;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * See https://hl7-definition.caristix.com/v2/HL7v2.5.1/DataTypes/CWE
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7CodedWithExceptions extends Hl7Object {
	@Nullable
	private String identifier; // CWE.1 - Identifier
	@Nullable
	private String text; // CWE.2 - Text
	@Nullable
	private String nameOfCodingSystem; // CWE.3 - Name Of Coding System
	@Nullable
	private String alternateIdentifier; // CWE.4 - Alternate Identifier
	@Nullable
	private String alternateText; // CWE.5 - Alternate Text
	@Nullable
	private String nameOfAlternateCodingSystem; // CWE.6 - Name Of Alternate Coding System
	@Nullable
	private String codingSystemVersionId; // CWE.7 - Coding System Version Id
	@Nullable
	private String alternateCodingSystemVersionId; // CWE.8 - Alternate Coding System Version Id
	@Nullable
	private String originalText; // CWE.9 - Original Text

	@Nonnull
	public static Boolean isPresent(@Nullable CWE cwe) {
		if (cwe == null)
			return false;

		return trimToNull(cwe.getIdentifier().getValueOrEmpty()) != null
				|| trimToNull(cwe.getText().getValueOrEmpty()) != null
				|| trimToNull(cwe.getNameOfCodingSystem().getValueOrEmpty()) != null
				|| trimToNull(cwe.getAlternateIdentifier().getValueOrEmpty()) != null
				|| trimToNull(cwe.getAlternateText().getValueOrEmpty()) != null
				|| trimToNull(cwe.getNameOfAlternateCodingSystem().getValueOrEmpty()) != null
				|| trimToNull(cwe.getCodingSystemVersionID().getValueOrEmpty()) != null
				|| trimToNull(cwe.getAlternateCodingSystemVersionID().getValueOrEmpty()) != null
				|| trimToNull(cwe.getOriginalText().getValueOrEmpty()) != null;
	}

	public Hl7CodedWithExceptions() {
		// Nothing to do
	}

	public Hl7CodedWithExceptions(@Nullable CWE cwe) {
		if (cwe == null) {
			this.identifier = trimToNull(cwe.getIdentifier().getValueOrEmpty());
			this.text = trimToNull(cwe.getText().getValueOrEmpty());
			this.nameOfCodingSystem = trimToNull(cwe.getNameOfCodingSystem().getValueOrEmpty());
			this.alternateIdentifier = trimToNull(cwe.getAlternateIdentifier().getValueOrEmpty());
			this.alternateText = trimToNull(cwe.getAlternateText().getValueOrEmpty());
			this.nameOfAlternateCodingSystem = trimToNull(cwe.getNameOfAlternateCodingSystem().getValueOrEmpty());
			this.codingSystemVersionId = trimToNull(cwe.getCodingSystemVersionID().getValueOrEmpty());
			this.alternateCodingSystemVersionId = trimToNull(cwe.getAlternateCodingSystemVersionID().getValueOrEmpty());
			this.originalText = trimToNull(cwe.getOriginalText().getValueOrEmpty());
		}
	}

	@Nullable
	public String getIdentifier() {
		return this.identifier;
	}

	public void setIdentifier(@Nullable String identifier) {
		this.identifier = identifier;
	}

	@Nullable
	public String getText() {
		return this.text;
	}

	public void setText(@Nullable String text) {
		this.text = text;
	}

	@Nullable
	public String getNameOfCodingSystem() {
		return this.nameOfCodingSystem;
	}

	public void setNameOfCodingSystem(@Nullable String nameOfCodingSystem) {
		this.nameOfCodingSystem = nameOfCodingSystem;
	}

	@Nullable
	public String getAlternateIdentifier() {
		return this.alternateIdentifier;
	}

	public void setAlternateIdentifier(@Nullable String alternateIdentifier) {
		this.alternateIdentifier = alternateIdentifier;
	}

	@Nullable
	public String getAlternateText() {
		return this.alternateText;
	}

	public void setAlternateText(@Nullable String alternateText) {
		this.alternateText = alternateText;
	}

	@Nullable
	public String getNameOfAlternateCodingSystem() {
		return this.nameOfAlternateCodingSystem;
	}

	public void setNameOfAlternateCodingSystem(@Nullable String nameOfAlternateCodingSystem) {
		this.nameOfAlternateCodingSystem = nameOfAlternateCodingSystem;
	}

	@Nullable
	public String getCodingSystemVersionId() {
		return this.codingSystemVersionId;
	}

	public void setCodingSystemVersionId(@Nullable String codingSystemVersionId) {
		this.codingSystemVersionId = codingSystemVersionId;
	}

	@Nullable
	public String getAlternateCodingSystemVersionId() {
		return this.alternateCodingSystemVersionId;
	}

	public void setAlternateCodingSystemVersionId(@Nullable String alternateCodingSystemVersionId) {
		this.alternateCodingSystemVersionId = alternateCodingSystemVersionId;
	}

	@Nullable
	public String getOriginalText() {
		return this.originalText;
	}

	public void setOriginalText(@Nullable String originalText) {
		this.originalText = originalText;
	}
}