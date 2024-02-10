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

	@Nonnull
	public static Boolean isPresent(@Nullable XCN xcn) {
		if (xcn == null)
			return false;

		return trimToNull(xcn.getIDNumber().getValueOrEmpty()) != null
				|| Hl7FamilyName.isPresent(xcn.getFamilyName());
	}

	public Hl7ExtendedCompositeIdNumberAndNameForPersons() {
		// Nothing to do
	}

	public Hl7ExtendedCompositeIdNumberAndNameForPersons(@Nullable XCN xcn) {
		if (xcn != null) {
			this.idNumber = trimToNull(xcn.getIDNumber().getValueOrEmpty());
			this.familyName = Hl7FamilyName.isPresent(xcn.getFamilyName()) ? new Hl7FamilyName(xcn.getFamilyName()) : null;

		}
	}

	// TODO: getters and setters
}