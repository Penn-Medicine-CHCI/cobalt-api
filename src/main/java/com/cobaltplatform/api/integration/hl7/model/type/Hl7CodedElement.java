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

import ca.uhn.hl7v2.model.v251.datatype.CE;
import com.cobaltplatform.api.integration.hl7.model.Hl7Object;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * See https://hl7-definition.caristix.com/v2/hl7v2.5.1/DataTypes/CE
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7CodedElement extends Hl7Object {
	@Nullable
	private String identifier;
	@Nullable
	private String text;
	@Nullable
	private String nameOfCodingSystem;
	@Nullable
	private String alternateIdentifier;
	@Nullable
	private String alternateText;
	@Nullable
	private String nameOfAlternateCodingSystem;

	@Nonnull
	public static Boolean isPresent(@Nullable CE ce) {
		if (ce == null)
			return false;

		return trimToNull(ce.getIdentifier().getValueOrEmpty()) != null
				|| trimToNull(ce.getText().getValueOrEmpty()) != null
				|| trimToNull(ce.getNameOfCodingSystem().getValueOrEmpty()) != null
				|| trimToNull(ce.getAlternateIdentifier().getValueOrEmpty()) != null
				|| trimToNull(ce.getAlternateText().getValueOrEmpty()) != null
				|| trimToNull(ce.getNameOfAlternateCodingSystem().getValueOrEmpty()) != null;
	}

	public Hl7CodedElement() {
		// Nothing to do
	}

	public Hl7CodedElement(@Nullable CE ce) {
		if (ce != null) {
			this.identifier = trimToNull(ce.getIdentifier().getValueOrEmpty());
			this.text = trimToNull(ce.getText().getValueOrEmpty());
			this.nameOfCodingSystem = trimToNull(ce.getNameOfCodingSystem().getValueOrEmpty());
			this.alternateIdentifier = trimToNull(ce.getAlternateIdentifier().getValueOrEmpty());
			this.alternateText = trimToNull(ce.getAlternateText().getValueOrEmpty());
			this.nameOfAlternateCodingSystem = trimToNull(ce.getNameOfAlternateCodingSystem().getValueOrEmpty());
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
}