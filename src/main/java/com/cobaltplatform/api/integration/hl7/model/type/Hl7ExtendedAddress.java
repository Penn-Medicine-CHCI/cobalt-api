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

import ca.uhn.hl7v2.model.v251.datatype.XAD;
import com.cobaltplatform.api.integration.hl7.model.Hl7Object;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * See https://hl7-definition.caristix.com/v2/HL7v2.5.1/DataTypes/XAD
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7ExtendedAddress extends Hl7Object {
	@Nullable
	private Hl7StreetAddress streetAddress; // XAD.1 - Street Address
	@Nullable
	private String otherDesignation; // XAD.2 - Other Designation
	@Nullable
	private String city; // XAD.3 - City
	@Nullable
	private String stateOrProvince; // XAD.4 - State Or Province
	@Nullable
	private String zipOrPostalCode; // XAD.5 - Zip Or Postal Code
	@Nullable
	private String country; // XAD.6 - Country
	@Nullable
	private String addressType; // XAD.7 - Address Type
	@Nullable
	private String otherGeographicDesignation; // XAD.8 - Other Geographic Designation
	@Nullable
	private String countyParishCode; // XAD.9 - County/Parish Code
	@Nullable
	private String cencusTract; // XAD.10 - Census Tract
	@Nullable
	private String addressRepresentationCode; // XAD.11 - Address Representation Code
	@Nullable
	private Hl7DateTimeRange addressValidityRange; // XAD.12 - Address Validity Range
	@Nullable
	private Hl7TimeStamp effectiveDate; // XAD.13 - Effective Date
	@Nullable
	private Hl7TimeStamp expirationDate; // XAD.14 - Expiration Date

	@Nonnull
	public static Boolean isPresent(@Nullable XAD xad) {
		if (xad == null)
			return false;

		return Hl7StreetAddress.isPresent(xad.getStreetAddress())
				|| trimToNull(xad.getOtherDesignation().getValueOrEmpty()) != null
				|| trimToNull(xad.getCity().getValueOrEmpty()) != null
				|| trimToNull(xad.getStateOrProvince().getValueOrEmpty()) != null
				|| trimToNull(xad.getZipOrPostalCode().getValueOrEmpty()) != null
				|| trimToNull(xad.getCountry().getValueOrEmpty()) != null
				|| trimToNull(xad.getAddressType().getValueOrEmpty()) != null
				|| trimToNull(xad.getOtherGeographicDesignation().getValueOrEmpty()) != null
				|| trimToNull(xad.getCountyParishCode().getValueOrEmpty()) != null
				|| trimToNull(xad.getCensusTract().getValueOrEmpty()) != null
				|| Hl7DateTimeRange.isPresent(xad.getAddressValidityRange())
				|| Hl7TimeStamp.isPresent(xad.getEffectiveDate())
				|| Hl7TimeStamp.isPresent(xad.getExpirationDate());
	}

	public Hl7ExtendedAddress() {
		// Nothing to do
	}

	public Hl7ExtendedAddress(@Nullable XAD xad) {
		if (xad != null) {
			this.streetAddress = Hl7StreetAddress.isPresent(xad.getStreetAddress()) ? new Hl7StreetAddress(xad.getStreetAddress()) : null;
			this.otherDesignation = trimToNull(xad.getOtherDesignation().getValueOrEmpty());
			this.city = trimToNull(xad.getCity().getValueOrEmpty());
			this.stateOrProvince = trimToNull(xad.getStateOrProvince().getValueOrEmpty());
			this.zipOrPostalCode = trimToNull(xad.getZipOrPostalCode().getValueOrEmpty());
			this.country = trimToNull(xad.getCountry().getValueOrEmpty());
			this.addressType = trimToNull(xad.getAddressType().getValueOrEmpty());
			this.otherGeographicDesignation = trimToNull(xad.getOtherGeographicDesignation().getValueOrEmpty());
			this.countyParishCode = trimToNull(xad.getCountyParishCode().getValueOrEmpty());
			this.cencusTract = trimToNull(xad.getCensusTract().getValueOrEmpty());
			this.addressValidityRange = Hl7DateTimeRange.isPresent(xad.getAddressValidityRange()) ? new Hl7DateTimeRange(xad.getAddressValidityRange()) : null;
			this.effectiveDate = Hl7TimeStamp.isPresent(xad.getEffectiveDate()) ? new Hl7TimeStamp(xad.getEffectiveDate()) : null;
			this.expirationDate = Hl7TimeStamp.isPresent(xad.getExpirationDate()) ? new Hl7TimeStamp(xad.getExpirationDate()) : null;
		}
	}

	@Nullable
	public Hl7StreetAddress getStreetAddress() {
		return this.streetAddress;
	}

	public void setStreetAddress(@Nullable Hl7StreetAddress streetAddress) {
		this.streetAddress = streetAddress;
	}

	@Nullable
	public String getOtherDesignation() {
		return this.otherDesignation;
	}

	public void setOtherDesignation(@Nullable String otherDesignation) {
		this.otherDesignation = otherDesignation;
	}

	@Nullable
	public String getCity() {
		return this.city;
	}

	public void setCity(@Nullable String city) {
		this.city = city;
	}

	@Nullable
	public String getStateOrProvince() {
		return this.stateOrProvince;
	}

	public void setStateOrProvince(@Nullable String stateOrProvince) {
		this.stateOrProvince = stateOrProvince;
	}

	@Nullable
	public String getZipOrPostalCode() {
		return this.zipOrPostalCode;
	}

	public void setZipOrPostalCode(@Nullable String zipOrPostalCode) {
		this.zipOrPostalCode = zipOrPostalCode;
	}

	@Nullable
	public String getCountry() {
		return this.country;
	}

	public void setCountry(@Nullable String country) {
		this.country = country;
	}

	@Nullable
	public String getAddressType() {
		return this.addressType;
	}

	public void setAddressType(@Nullable String addressType) {
		this.addressType = addressType;
	}

	@Nullable
	public String getOtherGeographicDesignation() {
		return this.otherGeographicDesignation;
	}

	public void setOtherGeographicDesignation(@Nullable String otherGeographicDesignation) {
		this.otherGeographicDesignation = otherGeographicDesignation;
	}

	@Nullable
	public String getCountyParishCode() {
		return this.countyParishCode;
	}

	public void setCountyParishCode(@Nullable String countyParishCode) {
		this.countyParishCode = countyParishCode;
	}

	@Nullable
	public String getCencusTract() {
		return this.cencusTract;
	}

	public void setCencusTract(@Nullable String cencusTract) {
		this.cencusTract = cencusTract;
	}

	@Nullable
	public String getAddressRepresentationCode() {
		return this.addressRepresentationCode;
	}

	public void setAddressRepresentationCode(@Nullable String addressRepresentationCode) {
		this.addressRepresentationCode = addressRepresentationCode;
	}

	@Nullable
	public Hl7DateTimeRange getAddressValidityRange() {
		return this.addressValidityRange;
	}

	public void setAddressValidityRange(@Nullable Hl7DateTimeRange addressValidityRange) {
		this.addressValidityRange = addressValidityRange;
	}

	@Nullable
	public Hl7TimeStamp getEffectiveDate() {
		return this.effectiveDate;
	}

	public void setEffectiveDate(@Nullable Hl7TimeStamp effectiveDate) {
		this.effectiveDate = effectiveDate;
	}

	@Nullable
	public Hl7TimeStamp getExpirationDate() {
		return this.expirationDate;
	}

	public void setExpirationDate(@Nullable Hl7TimeStamp expirationDate) {
		this.expirationDate = expirationDate;
	}
}