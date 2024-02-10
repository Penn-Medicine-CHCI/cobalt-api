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

import ca.uhn.hl7v2.model.v251.datatype.SAD;
import com.cobaltplatform.api.integration.hl7.model.Hl7Object;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * See https://hl7-definition.caristix.com/v2/HL7v2.5.1/DataTypes/SAD
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7StreetAddress extends Hl7Object {
	@Nullable
	private String streetOrMailingAddress; // SAD.1 - Street Or Mailing Address
	@Nullable
	private String streetName; // SAD.2 - Street Name
	@Nullable
	private String dwellingNumber; // SAD.3 - Dwelling Number

	@Nonnull
	public static Boolean isPresent(@Nullable SAD sad) {
		if (sad == null)
			return false;

		return trimToNull(sad.getStreetOrMailingAddress().getValueOrEmpty()) != null
				|| trimToNull(sad.getStreetName().getValueOrEmpty()) != null
				|| trimToNull(sad.getDwellingNumber().getValueOrEmpty()) != null;
	}

	public Hl7StreetAddress() {
		// Nothing to do
	}

	public Hl7StreetAddress(@Nullable SAD sad) {
		if (sad != null) {
			this.streetOrMailingAddress = trimToNull(sad.getStreetOrMailingAddress().getValueOrEmpty());
			this.streetName = trimToNull(sad.getStreetName().getValueOrEmpty());
			this.dwellingNumber = trimToNull(sad.getDwellingNumber().getValueOrEmpty());
		}
	}

	@Nullable
	public String getStreetOrMailingAddress() {
		return this.streetOrMailingAddress;
	}

	public void setStreetOrMailingAddress(@Nullable String streetOrMailingAddress) {
		this.streetOrMailingAddress = streetOrMailingAddress;
	}

	@Nullable
	public String getStreetName() {
		return this.streetName;
	}

	public void setStreetName(@Nullable String streetName) {
		this.streetName = streetName;
	}

	@Nullable
	public String getDwellingNumber() {
		return this.dwellingNumber;
	}

	public void setDwellingNumber(@Nullable String dwellingNumber) {
		this.dwellingNumber = dwellingNumber;
	}
}