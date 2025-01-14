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

package com.cobaltplatform.api.model.api.response;

import com.cobaltplatform.api.model.db.Address;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class AddressApiResponse {
	@Nonnull
	private final UUID addressId;
	@Nonnull
	private final String postalName;
	@Nonnull
	private final String streetAddress1;
	@Nullable
	private final String streetAddress2;
	@Nullable
	private final String streetAddress3;
	@Nullable
	private final String streetAddress4;
	@Nullable
	private final String postOfficeBoxNumber;
	@Nullable
	private final String crossStreet;
	@Nullable
	private final String suburb;
	@Nonnull
	private final String locality;
	@Nonnull
	private final String region;
	@Nonnull
	private final String postalCode;
	@Nullable
	private final String countrySubdivisionCode;
	@Nonnull
	private final String countryCode;
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

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface AddressApiResponseFactory {
		@Nonnull
		AddressApiResponse create(@Nonnull Address address);
	}

	@AssistedInject
	public AddressApiResponse(@Nonnull Formatter formatter,
														@Nonnull Strings strings,
														@Assisted @Nonnull Address address) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(address);

		this.addressId = address.getAddressId();
		this.postalName = address.getPostalName();
		this.streetAddress1 = address.getStreetAddress1();
		this.streetAddress2 = address.getStreetAddress2();
		this.streetAddress3 = address.getStreetAddress3();
		this.streetAddress4 = address.getStreetAddress4();
		this.postOfficeBoxNumber = address.getPostOfficeBoxNumber();
		this.crossStreet = address.getCrossStreet();
		this.suburb = address.getSuburb();
		this.locality = address.getLocality();
		this.region = address.getRegion();
		this.postalCode = address.getPostalCode();
		this.countrySubdivisionCode = address.getCountrySubdivisionCode();
		this.countryCode = address.getCountryCode();
	}

	@Nonnull
	public UUID getAddressId() {
		return this.addressId;
	}

	@Nonnull
	public String getPostalName() {
		return this.postalName;
	}

	@Nonnull
	public String getStreetAddress1() {
		return this.streetAddress1;
	}

	@Nullable
	public String getStreetAddress2() {
		return this.streetAddress2;
	}

	@Nullable
	public String getStreetAddress3() {
		return this.streetAddress3;
	}

	@Nullable
	public String getStreetAddress4() {
		return this.streetAddress4;
	}

	@Nullable
	public String getPostOfficeBoxNumber() {
		return this.postOfficeBoxNumber;
	}

	@Nullable
	public String getCrossStreet() {
		return this.crossStreet;
	}

	@Nullable
	public String getSuburb() {
		return this.suburb;
	}

	@Nonnull
	public String getLocality() {
		return this.locality;
	}

	@Nonnull
	public String getRegion() {
		return this.region;
	}

	@Nonnull
	public String getPostalCode() {
		return this.postalCode;
	}

	@Nullable
	public String getCountrySubdivisionCode() {
		return this.countrySubdivisionCode;
	}

	@Nonnull
	public String getCountryCode() {
		return this.countryCode;
	}

	@Nullable
	public String getGoogleMapsUrl() {
		return googleMapsUrl;
	}

	@Nullable
	public String getGooglePlaceId() {
		return googlePlaceId;
	}

	@Nullable
	public Double getLatitude() {
		return latitude;
	}

	@Nullable
	public Double getLongitude() {
		return longitude;
	}

	@Nullable
	public String getPremise() {
		return premise;
	}

	@Nullable
	public String getSubpremise() {
		return subpremise;
	}

	@Nullable
	public String getRegionSubdivision() {
		return regionSubdivision;
	}

	@Nullable
	public String getPostalCodeSuffix() {
		return postalCodeSuffix;
	}

	@Nullable
	public String getFormattedAddress() {
		return formattedAddress;
	}
}