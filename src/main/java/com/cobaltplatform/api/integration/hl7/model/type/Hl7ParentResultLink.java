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

import ca.uhn.hl7v2.model.v251.datatype.PRL;
import com.cobaltplatform.api.integration.hl7.model.Hl7Object;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * See https://hl7-definition.caristix.com/v2/hl7v2.5.1/DataTypes/PRL
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7ParentResultLink extends Hl7Object {
	@Nullable
	private Hl7CodedElement parentObservationIdentifier; // PRL.1 - Parent Observation Identifier
	@Nullable
	private String parentObservationSubIdentifier; // PRL.2 - Parent Observation Sub-identifier
	@Nullable
	private String parentObservationValueDescriptor; // PRL.3 - Parent Observation Value Descriptor

	@Nonnull
	public static Boolean isPresent(@Nullable PRL prl) {
		if (prl == null)
			return false;

		return Hl7CodedElement.isPresent(prl.getParentObservationIdentifier());
	}

	public Hl7ParentResultLink() {
		// Nothing to do
	}

	public Hl7ParentResultLink(@Nullable PRL prl) {
		if (prl != null) {
			if (Hl7CodedElement.isPresent(prl.getParentObservationIdentifier()))
				this.parentObservationIdentifier = new Hl7CodedElement(prl.getParentObservationIdentifier());

			this.parentObservationSubIdentifier = trimToNull(prl.getParentObservationSubIdentifier().getValueOrEmpty());
			this.parentObservationValueDescriptor = trimToNull(prl.getParentObservationValueDescriptor().getValueOrEmpty());
		}
	}

	@Nullable
	public Hl7CodedElement getParentObservationIdentifier() {
		return this.parentObservationIdentifier;
	}

	public void setParentObservationIdentifier(@Nullable Hl7CodedElement parentObservationIdentifier) {
		this.parentObservationIdentifier = parentObservationIdentifier;
	}

	@Nullable
	public String getParentObservationSubIdentifier() {
		return this.parentObservationSubIdentifier;
	}

	public void setParentObservationSubIdentifier(@Nullable String parentObservationSubIdentifier) {
		this.parentObservationSubIdentifier = parentObservationSubIdentifier;
	}

	@Nullable
	public String getParentObservationValueDescriptor() {
		return this.parentObservationValueDescriptor;
	}

	public void setParentObservationValueDescriptor(@Nullable String parentObservationValueDescriptor) {
		this.parentObservationValueDescriptor = parentObservationValueDescriptor;
	}
}