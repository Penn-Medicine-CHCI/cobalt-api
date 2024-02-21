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

import ca.uhn.hl7v2.model.v251.datatype.FN;
import com.cobaltplatform.api.integration.hl7.model.Hl7Object;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * See https://hl7-definition.caristix.com/v2/HL7v2.5.1/DataTypes/FN
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7FamilyName extends Hl7Object {
	@Nullable
	private String surname; // FN.1 - Surname
	@Nullable
	private String ownSurnamePrefix; // FN.2 - Own Surname Prefix
	@Nullable
	private String ownSurname; // FN.3 - Own Surname
	@Nullable
	private String surnamePrefixFromPartnerSpouse; // FN.4 - Surname Prefix From Partner/Spouse
	@Nullable
	private String surnameFromPartnerSpouse; // FN.5 - Surname From Partner/Spouse

	@Nonnull
	public static Boolean isPresent(@Nullable FN fn) {
		if (fn == null)
			return false;

		return trimToNull(fn.getSurname().getValueOrEmpty()) != null
				|| trimToNull(fn.getOwnSurnamePrefix().getValueOrEmpty()) != null
				|| trimToNull(fn.getOwnSurname().getValueOrEmpty()) != null
				|| trimToNull(fn.getSurnamePrefixFromPartnerSpouse().getValueOrEmpty()) != null
				|| trimToNull(fn.getSurnameFromPartnerSpouse().getValueOrEmpty()) != null;
	}

	public Hl7FamilyName() {
		// Nothing to do
	}

	public Hl7FamilyName(@Nullable FN fn) {
		if (fn != null) {
			this.surname = trimToNull(fn.getSurname().getValueOrEmpty());
			this.ownSurnamePrefix = trimToNull(fn.getOwnSurnamePrefix().getValueOrEmpty());
			this.ownSurname = trimToNull(fn.getOwnSurname().getValueOrEmpty());
			this.surnamePrefixFromPartnerSpouse = trimToNull(fn.getSurnamePrefixFromPartnerSpouse().getValueOrEmpty());
			this.surnameFromPartnerSpouse = trimToNull(fn.getSurnameFromPartnerSpouse().getValueOrEmpty());
		}
	}

	@Nullable
	public String getSurname() {
		return this.surname;
	}

	public void setSurname(@Nullable String surname) {
		this.surname = surname;
	}

	@Nullable
	public String getOwnSurnamePrefix() {
		return this.ownSurnamePrefix;
	}

	public void setOwnSurnamePrefix(@Nullable String ownSurnamePrefix) {
		this.ownSurnamePrefix = ownSurnamePrefix;
	}

	@Nullable
	public String getOwnSurname() {
		return this.ownSurname;
	}

	public void setOwnSurname(@Nullable String ownSurname) {
		this.ownSurname = ownSurname;
	}

	@Nullable
	public String getSurnamePrefixFromPartnerSpouse() {
		return this.surnamePrefixFromPartnerSpouse;
	}

	public void setSurnamePrefixFromPartnerSpouse(@Nullable String surnamePrefixFromPartnerSpouse) {
		this.surnamePrefixFromPartnerSpouse = surnamePrefixFromPartnerSpouse;
	}

	@Nullable
	public String getSurnameFromPartnerSpouse() {
		return this.surnameFromPartnerSpouse;
	}

	public void setSurnameFromPartnerSpouse(@Nullable String surnameFromPartnerSpouse) {
		this.surnameFromPartnerSpouse = surnameFromPartnerSpouse;
	}
}