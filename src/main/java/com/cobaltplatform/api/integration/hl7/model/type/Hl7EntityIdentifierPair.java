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

import ca.uhn.hl7v2.model.v251.datatype.EIP;
import com.cobaltplatform.api.integration.hl7.model.Hl7Object;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * See https://hl7-definition.caristix.com/v2/HL7v2.5.1/DataTypes/EIP
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7EntityIdentifierPair extends Hl7Object {
	@Nullable
	private Hl7EntityIdentifier placerAssignedIdentifier; // EIP.1 - Placer Assigned Identifier
	@Nullable
	private Hl7EntityIdentifier fillerAssignedIdentifier; // EIP.2 - Filler Assigned Identifier

	@Nonnull
	public static Boolean isPresent(@Nullable EIP eip) {
		if (eip == null)
			return false;

		return Hl7EntityIdentifier.isPresent(eip.getPlacerAssignedIdentifier())
				|| Hl7EntityIdentifier.isPresent(eip.getFillerAssignedIdentifier());
	}

	public Hl7EntityIdentifierPair() {
		// Nothing to do
	}

	public Hl7EntityIdentifierPair(@Nullable EIP eip) {
		if (eip != null) {
			this.placerAssignedIdentifier = Hl7EntityIdentifier.isPresent(eip.getPlacerAssignedIdentifier())
					? new Hl7EntityIdentifier(eip.getPlacerAssignedIdentifier()) : null;
			this.fillerAssignedIdentifier = Hl7EntityIdentifier.isPresent(eip.getFillerAssignedIdentifier())
					? new Hl7EntityIdentifier(eip.getFillerAssignedIdentifier()) : null;
		}
	}

	@Nullable
	public Hl7EntityIdentifier getPlacerAssignedIdentifier() {
		return this.placerAssignedIdentifier;
	}

	public void setPlacerAssignedIdentifier(@Nullable Hl7EntityIdentifier placerAssignedIdentifier) {
		this.placerAssignedIdentifier = placerAssignedIdentifier;
	}

	@Nullable
	public Hl7EntityIdentifier getFillerAssignedIdentifier() {
		return this.fillerAssignedIdentifier;
	}

	public void setFillerAssignedIdentifier(@Nullable Hl7EntityIdentifier fillerAssignedIdentifier) {
		this.fillerAssignedIdentifier = fillerAssignedIdentifier;
	}
}