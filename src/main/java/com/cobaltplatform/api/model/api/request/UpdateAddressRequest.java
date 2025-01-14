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

package com.cobaltplatform.api.model.api.request;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.UUID;

/**
 * @author Transmogrify LLC.
 */
@NotThreadSafe
public class UpdateAddressRequest {
	@Nullable
	private UUID addressId;
	@Nullable
	private String postalName;
	@Nullable
	private String streetAddress1;
	@Nullable
	private String streetAddress2;
	@Nullable
	private String streetAddress3;
	@Nullable
	private String streetAddress4;
	@Nullable
	private String postOfficeBoxNumber;
	@Nullable
	private String crossStreet;
	@Nullable
	private String suburb;
	@Nullable
	private String locality;
	@Nullable
	private String region;
	@Nullable
	private String postalCode;
	@Nullable
	private String countrySubdivisionCode;
	@Nullable
	private String countryCode;
	@Nullable
	private String googleMapsUrl;
	@Nullable
	private String googlePlaceId;
	@Nullable
	private Double latitude;
	@Nullable
	private Double longitude;
	@Nullable
	private String premise;
	@Nullable
	private String subpremise;
	@Nullable
	private String regionSubdivision;
	@Nullable
	private String postalCodeSuffix;
	@Nullable
	private String formattedAddress;

	@Nullable
	public String getPostalName() {
		return this.postalName;
	}

	public void setPostalName(@Nullable String postalName) {
		this.postalName = postalName;
	}

	@Nullable
	public String getStreetAddress1() {
		return this.streetAddress1;
	}

	public void setStreetAddress1(@Nullable String streetAddress1) {
		this.streetAddress1 = streetAddress1;
	}

	@Nullable
	public String getStreetAddress2() {
		return this.streetAddress2;
	}

	public void setStreetAddress2(@Nullable String streetAddress2) {
		this.streetAddress2 = streetAddress2;
	}

	@Nullable
	public String getStreetAddress3() {
		return this.streetAddress3;
	}

	public void setStreetAddress3(@Nullable String streetAddress3) {
		this.streetAddress3 = streetAddress3;
	}

	@Nullable
	public String getStreetAddress4() {
		return this.streetAddress4;
	}

	public void setStreetAddress4(@Nullable String streetAddress4) {
		this.streetAddress4 = streetAddress4;
	}

	@Nullable
	public String getPostOfficeBoxNumber() {
		return this.postOfficeBoxNumber;
	}

	public void setPostOfficeBoxNumber(@Nullable String postOfficeBoxNumber) {
		this.postOfficeBoxNumber = postOfficeBoxNumber;
	}

	@Nullable
	public String getCrossStreet() {
		return this.crossStreet;
	}

	public void setCrossStreet(@Nullable String crossStreet) {
		this.crossStreet = crossStreet;
	}

	@Nullable
	public String getSuburb() {
		return this.suburb;
	}

	public void setSuburb(@Nullable String suburb) {
		this.suburb = suburb;
	}

	@Nullable
	public String getLocality() {
		return this.locality;
	}

	public void setLocality(@Nullable String locality) {
		this.locality = locality;
	}

	@Nullable
	public String getRegion() {
		return this.region;
	}

	public void setRegion(@Nullable String region) {
		this.region = region;
	}

	@Nullable
	public String getPostalCode() {
		return this.postalCode;
	}

	public void setPostalCode(@Nullable String postalCode) {
		this.postalCode = postalCode;
	}

	@Nullable
	public String getCountrySubdivisionCode() {
		return this.countrySubdivisionCode;
	}

	public void setCountrySubdivisionCode(@Nullable String countrySubdivisionCode) {
		this.countrySubdivisionCode = countrySubdivisionCode;
	}

	@Nullable
	public String getCountryCode() {
		return this.countryCode;
	}

	public void setCountryCode(@Nullable String countryCode) {
		this.countryCode = countryCode;
	}

	@Nullable
	public String getGoogleMapsUrl() {
		return googleMapsUrl;
	}

	public void setGoogleMapsUrl(@Nullable String googleMapsUrl) {
		this.googleMapsUrl = googleMapsUrl;
	}

	@Nullable
	public String getGooglePlaceId() {
		return googlePlaceId;
	}

	public void setGooglePlaceId(@Nullable String googlePlaceId) {
		this.googlePlaceId = googlePlaceId;
	}

	@Nullable
	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(@Nullable Double latitude) {
		this.latitude = latitude;
	}

	@Nullable
	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(@Nullable Double longitude) {
		this.longitude = longitude;
	}

	@Nullable
	public String getPremise() {
		return premise;
	}

	public void setPremise(@Nullable String premise) {
		this.premise = premise;
	}

	@Nullable
	public String getSubpremise() {
		return subpremise;
	}

	public void setSubpremise(@Nullable String subpremise) {
		this.subpremise = subpremise;
	}

	@Nullable
	public String getRegionSubdivision() {
		return regionSubdivision;
	}

	public void setRegionSubdivision(@Nullable String regionSubdivision) {
		this.regionSubdivision = regionSubdivision;
	}

	@Nullable
	public String getPostalCodeSuffix() {
		return postalCodeSuffix;
	}

	public void setPostalCodeSuffix(@Nullable String postalCodeSuffix) {
		this.postalCodeSuffix = postalCodeSuffix;
	}

	@Nullable
	public String getFormattedAddress() {
		return formattedAddress;
	}

	public void setFormattedAddress(@Nullable String formattedAddress) {
		this.formattedAddress = formattedAddress;
	}

	@Nullable
	public UUID getAddressId() {
		return addressId;
	}

	public void setAddressId(@Nullable UUID addressId) {
		this.addressId = addressId;
	}
}
