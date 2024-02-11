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

import ca.uhn.hl7v2.model.v251.datatype.DLN;
import com.cobaltplatform.api.integration.hl7.model.Hl7Object;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * See https://hl7-definition.caristix.com/v2/hl7v2.5.1/DataTypes/DLN
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7DriverLicenseNumber extends Hl7Object {
	@Nullable
	private String licenseNumber; // DLN.1 - License Number
	@Nullable
	private String issuingStateProvinceCountry; // DLN.2 - Issuing State, Province, Country
	@Nullable
	private String expirationDate; // DLN.3 - Expiration Date

	@Nonnull
	public static Boolean isPresent(@Nullable DLN dln) {
		if (dln == null)
			return false;

		return trimToNull(dln.getLicenseNumber().getValueOrEmpty()) != null
				|| trimToNull(dln.getIssuingStateProvinceCountry().getValueOrEmpty()) != null
				|| trimToNull(dln.getExpirationDate().getValue()) != null;
	}

	public Hl7DriverLicenseNumber() {
		// Nothing to do
	}

	public Hl7DriverLicenseNumber(@Nullable DLN dln) {
		if (dln != null) {
			this.licenseNumber = trimToNull(dln.getLicenseNumber().getValueOrEmpty());
			this.issuingStateProvinceCountry = trimToNull(dln.getIssuingStateProvinceCountry().getValueOrEmpty());
			this.expirationDate = trimToNull(dln.getExpirationDate().getValue());
		}
	}

	@Nullable
	public String getLicenseNumber() {
		return this.licenseNumber;
	}

	public void setLicenseNumber(@Nullable String licenseNumber) {
		this.licenseNumber = licenseNumber;
	}

	@Nullable
	public String getIssuingStateProvinceCountry() {
		return this.issuingStateProvinceCountry;
	}

	public void setIssuingStateProvinceCountry(@Nullable String issuingStateProvinceCountry) {
		this.issuingStateProvinceCountry = issuingStateProvinceCountry;
	}

	@Nullable
	public String getExpirationDate() {
		return this.expirationDate;
	}

	public void setExpirationDate(@Nullable String expirationDate) {
		this.expirationDate = expirationDate;
	}
}