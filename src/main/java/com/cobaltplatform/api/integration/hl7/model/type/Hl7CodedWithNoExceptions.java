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

import ca.uhn.hl7v2.model.v251.datatype.CNE;
import com.cobaltplatform.api.integration.hl7.model.Hl7Object;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * See https://hl7-definition.caristix.com/v2/HL7v2.5.1/DataTypes/CNE
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7CodedWithNoExceptions extends Hl7Object {
	@Nullable
	private String identifier; // CNE.1 - Identifier
	@Nullable
	private String text; // CNE.2 - Text
	@Nullable
	private String nameOfCodingSystem; // CNE.3 - Name Of Coding System
	@Nullable
	private String alternateIdentifier; // CNE.4 - Alternate Identifier
	@Nullable
	private String alternateText; // CNE.5 - Alternate Text
	@Nullable
	private String nameOfAlternateCodingSystem; // CNE.6 - Name Of Alternate Coding System
	@Nullable
	private String codingSystemVersionId; // CNE.7 - Coding System Version Id
	@Nullable
	private String alternateCodingSystemVersionId; // CNE.8 - Alternate Coding System Version Id
	@Nullable
	private String originalText; // CNE.9 - Original Text

	@Nonnull
	public static Boolean isPresent(@Nullable CNE cne) {
		if (cne == null)
			return false;

		return trimToNull(cne.getIdentifier().getValueOrEmpty()) != null
				|| trimToNull(cne.getText().getValueOrEmpty()) != null
				|| trimToNull(cne.getNameOfCodingSystem().getValueOrEmpty()) != null
				|| trimToNull(cne.getAlternateIdentifier().getValueOrEmpty()) != null
				|| trimToNull(cne.getAlternateText().getValueOrEmpty()) != null
				|| trimToNull(cne.getNameOfAlternateCodingSystem().getValueOrEmpty()) != null
				|| trimToNull(cne.getCodingSystemVersionID().getValueOrEmpty()) != null
				|| trimToNull(cne.getAlternateCodingSystemVersionID().getValueOrEmpty()) != null
				|| trimToNull(cne.getOriginalText().getValueOrEmpty()) != null;
	}

	public Hl7CodedWithNoExceptions() {
		// Nothing to do
	}

	public Hl7CodedWithNoExceptions(@Nullable CNE cne) {
		if (cne == null) {
			this.identifier = trimToNull(cne.getIdentifier().getValueOrEmpty());
			this.text = trimToNull(cne.getText().getValueOrEmpty());
			this.nameOfCodingSystem = trimToNull(cne.getNameOfCodingSystem().getValueOrEmpty());
			this.alternateIdentifier = trimToNull(cne.getAlternateIdentifier().getValueOrEmpty());
			this.alternateText = trimToNull(cne.getAlternateText().getValueOrEmpty());
			this.nameOfAlternateCodingSystem = trimToNull(cne.getNameOfAlternateCodingSystem().getValueOrEmpty());
			this.codingSystemVersionId = trimToNull(cne.getCodingSystemVersionID().getValueOrEmpty());
			this.alternateCodingSystemVersionId = trimToNull(cne.getAlternateCodingSystemVersionID().getValueOrEmpty());
			this.originalText = trimToNull(cne.getOriginalText().getValueOrEmpty());
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