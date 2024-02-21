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

import ca.uhn.hl7v2.model.v251.datatype.XTN;
import com.cobaltplatform.api.integration.hl7.model.Hl7Object;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * See https://hl7-definition.caristix.com/v2/hl7v2.5.1/DataTypes/XTN
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7ExtendedTelecommunicationNumber extends Hl7Object {
	@Nullable
	private String telephoneNumber; // XTN.1 - Telephone Number
	@Nullable
	private String telecommunicationUseCode; // XTN.2 - Telecommunication Use Code
	@Nullable
	private String telecommunicationEquipmentType; // XTN.3 - Telecommunication Equipment Type
	@Nullable
	private String emailAddress; // XTN.4 - Email Address
	@Nullable
	private Integer countryCode; // XTN.5 - Country Code
	@Nullable
	private Integer areaCityCode; // XTN.6 - Area/City Code
	@Nullable
	private Integer localNumber; // XTN.7 - Local Number
	@Nullable
	private Integer extension; // XTN.8 - Extension
	@Nullable
	private String anyText; // XTN.9 - Any Text
	@Nullable
	private String extensionPrevix; // XTN.10 - Extension Prefix
	@Nullable
	private String speedDialCode; // XTN.11 - Speed Dial Code
	@Nullable
	private String unformattedTelephoneNumber; // XTN.12 - Unformatted Telephone Number

	@Nonnull
	public static Boolean isPresent(@Nullable XTN xtn) {
		if (xtn == null)
			return false;

		return trimToNull(xtn.getTelephoneNumber().getValueOrEmpty()) != null
				|| trimToNull(xtn.getTelecommunicationUseCode().getValueOrEmpty()) != null
				|| trimToNull(xtn.getTelecommunicationEquipmentType().getValueOrEmpty()) != null
				|| trimToNull(xtn.getEmailAddress().getValueOrEmpty()) != null
				|| trimToNull(xtn.getCountryCode().getValue()) != null
				|| trimToNull(xtn.getAreaCityCode().getValue()) != null
				|| trimToNull(xtn.getLocalNumber().getValue()) != null
				|| trimToNull(xtn.getExtension().getValue()) != null
				|| trimToNull(xtn.getAnyText().getValueOrEmpty()) != null
				|| trimToNull(xtn.getExtensionPrefix().getValueOrEmpty()) != null
				|| trimToNull(xtn.getSpeedDialCode().getValueOrEmpty()) != null
				|| trimToNull(xtn.getUnformattedTelephoneNumber().getValueOrEmpty()) != null;
	}

	public Hl7ExtendedTelecommunicationNumber() {
		// Nothing to do
	}

	public Hl7ExtendedTelecommunicationNumber(@Nullable XTN xtn) {
		if (xtn != null) {
			this.telephoneNumber = trimToNull(xtn.getTelephoneNumber().getValueOrEmpty());
			this.telecommunicationUseCode = trimToNull(xtn.getTelecommunicationUseCode().getValueOrEmpty());
			this.telecommunicationEquipmentType = trimToNull(xtn.getTelecommunicationEquipmentType().getValueOrEmpty());
			this.emailAddress = trimToNull(xtn.getEmailAddress().getValueOrEmpty());

			String countryCodeAsString = trimToNull(xtn.getCountryCode().getValue());
			if (countryCodeAsString != null)
				this.countryCode = Integer.parseInt(countryCodeAsString, 10);

			String areaCityCodeAsString = trimToNull(xtn.getAreaCityCode().getValue());
			if (areaCityCodeAsString != null)
				this.areaCityCode = Integer.parseInt(areaCityCodeAsString, 10);

			String localNumberAsString = trimToNull(xtn.getLocalNumber().getValue());
			if (localNumberAsString != null)
				this.localNumber = Integer.parseInt(localNumberAsString, 10);

			String extensionAsString = trimToNull(xtn.getExtension().getValue());
			if (extensionAsString != null)
				this.extension = Integer.parseInt(extensionAsString, 10);

			this.anyText = trimToNull(xtn.getAnyText().getValueOrEmpty());
			this.extensionPrevix = trimToNull(xtn.getExtensionPrefix().getValueOrEmpty());
			this.speedDialCode = trimToNull(xtn.getSpeedDialCode().getValueOrEmpty());
			this.unformattedTelephoneNumber = trimToNull(xtn.getUnformattedTelephoneNumber().getValueOrEmpty());
		}
	}

	@Nullable
	public String getTelephoneNumber() {
		return this.telephoneNumber;
	}

	public void setTelephoneNumber(@Nullable String telephoneNumber) {
		this.telephoneNumber = telephoneNumber;
	}

	@Nullable
	public String getTelecommunicationUseCode() {
		return this.telecommunicationUseCode;
	}

	public void setTelecommunicationUseCode(@Nullable String telecommunicationUseCode) {
		this.telecommunicationUseCode = telecommunicationUseCode;
	}

	@Nullable
	public String getTelecommunicationEquipmentType() {
		return this.telecommunicationEquipmentType;
	}

	public void setTelecommunicationEquipmentType(@Nullable String telecommunicationEquipmentType) {
		this.telecommunicationEquipmentType = telecommunicationEquipmentType;
	}

	@Nullable
	public String getEmailAddress() {
		return this.emailAddress;
	}

	public void setEmailAddress(@Nullable String emailAddress) {
		this.emailAddress = emailAddress;
	}

	@Nullable
	public Integer getCountryCode() {
		return this.countryCode;
	}

	public void setCountryCode(@Nullable Integer countryCode) {
		this.countryCode = countryCode;
	}

	@Nullable
	public Integer getAreaCityCode() {
		return this.areaCityCode;
	}

	public void setAreaCityCode(@Nullable Integer areaCityCode) {
		this.areaCityCode = areaCityCode;
	}

	@Nullable
	public Integer getLocalNumber() {
		return this.localNumber;
	}

	public void setLocalNumber(@Nullable Integer localNumber) {
		this.localNumber = localNumber;
	}

	@Nullable
	public Integer getExtension() {
		return this.extension;
	}

	public void setExtension(@Nullable Integer extension) {
		this.extension = extension;
	}

	@Nullable
	public String getAnyText() {
		return this.anyText;
	}

	public void setAnyText(@Nullable String anyText) {
		this.anyText = anyText;
	}

	@Nullable
	public String getExtensionPrevix() {
		return this.extensionPrevix;
	}

	public void setExtensionPrevix(@Nullable String extensionPrevix) {
		this.extensionPrevix = extensionPrevix;
	}

	@Nullable
	public String getSpeedDialCode() {
		return this.speedDialCode;
	}

	public void setSpeedDialCode(@Nullable String speedDialCode) {
		this.speedDialCode = speedDialCode;
	}

	@Nullable
	public String getUnformattedTelephoneNumber() {
		return this.unformattedTelephoneNumber;
	}

	public void setUnformattedTelephoneNumber(@Nullable String unformattedTelephoneNumber) {
		this.unformattedTelephoneNumber = unformattedTelephoneNumber;
	}
}