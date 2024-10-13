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

package com.cobaltplatform.api.integration.google.model;

import com.google.maps.places.v1.Place;
import com.google.type.LatLng;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class NormalizedPlace {
	@Nullable
	private String googlePlaceId;
	@Nullable
	private String googleMapsUrl;
	@Nullable
	private Double latitude;
	@Nullable
	private Double longitude;
	@Nullable
	private String premise; // e.g. "Chrsyler Building"
	@Nullable
	private String subpremise; // e.g. "2nd Fl" or "Suite 3"
	@Nullable
	private String formattedAddress; // One-liner representation of address
	@Nullable
	private String streetAddress1;
	@Nullable
	private String streetAddress2;
	@Nullable
	private String streetAddress3;
	@Nullable
	private String locality; // e.g. "Philadelphia" (city)
	@Nullable
	private String region; // e.g. "PA" (state abbreviation)
	@Nullable
	private String regionSubdivision; // e.g. "Montgomery County" (county)
	@Nullable
	private String postalCode; // e.g. "19428"
	@Nullable
	private String postalCodeSuffix; // e.g. "1514"
	@Nullable
	private String countryCode; // e.g. "US"

	public NormalizedPlace(@Nullable Place place) {
		if (place == null)
			return;

		LatLng location = place.getLocation();
		List<Place.AddressComponent> addressComponents = place.getAddressComponentsList();

		String googlePlaceId = place.getId();
		String formattedAddress = place.getFormattedAddress();
		Double latitude = location.getLatitude();
		Double longitude = location.getLongitude();
		String streetNumber = null;
		String route = null;
		String streetNumberAndRoute = null; // 123 Fake St.
		String premise = null; // e.g. Building Name
		String subpremise = null; // e.g. "2nd Fl" or "Suite 3"
		String locality = null;
		String administrativeAreaLevel3 = null;
		String region = null;
		String regionSubdivision = null;
		String postalCode = null;
		String postalCodeSuffix = null;
		String countryCode = null;

		for (Place.AddressComponent addressComponent : addressComponents) {
			Set<String> types = new HashSet<>(addressComponent.getTypesList());

			if (types.contains("premise")) {
				premise = addressComponent.getLongText(); // e.g. "Chrysler Building"
			} else if (types.contains("subpremise")) {
				subpremise = addressComponent.getLongText(); // e.g. "2nd Fl" or "Suite 3"

				// If it's just a number and nothing else, put a "#" in front for consistency
				if (subpremise != null && subpremise.matches("\\d+"))
					subpremise = format("# %s", subpremise);
			} else if (types.contains("street_number")) {
				streetNumber = addressComponent.getLongText(); // e.g. "123"
			} else if (types.contains("route")) {
				route = addressComponent.getLongText(); // e.g. "Fake St."
			} else if (types.contains("locality")) {
				locality = addressComponent.getLongText(); // e.g. "Philadelphia"
			} else if (types.contains("administrative_area_level_1")) {
				region = addressComponent.getShortText(); // e.g. "PA"
			} else if (types.contains("administrative_area_level_2")) {
				regionSubdivision = addressComponent.getShortText(); // e.g. "Montgomery County"
			} else if (types.contains("administrative_area_level_3")) {
				administrativeAreaLevel3 = addressComponent.getLongText(); // Sometimes city (administrative_area_level_1) is missing, this can be used to replace it
			} else if (types.contains("postal_code")) {
				postalCode = addressComponent.getLongText(); // e.g. "19119"
			} else if (types.contains("postal_code_suffix")) {
				postalCodeSuffix = addressComponent.getLongText(); // e.g. "1725"
			} else if (types.contains("country")) {
				countryCode = addressComponent.getShortText(); // e.g. "US"
			}
		}

		// Handle edge case where locality (city) can be missing, but it's available via administrative area level 3
		if (locality == null)
			locality = administrativeAreaLevel3;

		List<String> streetNumberAndRouteComponents = new ArrayList<>();

		if (streetNumber != null)
			streetNumberAndRouteComponents.add(streetNumber);
		if (route != null)
			streetNumberAndRouteComponents.add(route);

		streetNumberAndRoute = streetNumberAndRouteComponents.size() == 0 ? null : streetNumberAndRouteComponents.stream().collect(Collectors.joining(" "));

		// Figure out address lines 1, 2, and 3
		List<String> addressLines = new ArrayList<>();

		if (premise != null)
			addressLines.add(premise);

		if (streetNumberAndRoute != null)
			addressLines.add(streetNumberAndRoute);

		if (subpremise != null)
			addressLines.add(subpremise);

		String streetAddress1 = addressLines.size() > 0 ? addressLines.get(0) : null;
		String streetAddress2 = addressLines.size() > 1 ? addressLines.get(1) : null;
		String streetAddress3 = addressLines.size() > 2 ? addressLines.get(2) : null;

		setGooglePlaceId(googlePlaceId);
		setLatitude(latitude);
		setLongitude(longitude);
		setPremise(premise);
		setSubpremise(subpremise);
		setFormattedAddress(formattedAddress);
		setStreetAddress1(streetAddress1);
		setStreetAddress2(streetAddress2);
		setStreetAddress3(streetAddress3);
		setLocality(locality);
		setRegion(region);
		setRegionSubdivision(regionSubdivision);
		setPostalCode(postalCode);
		setPostalCodeSuffix(postalCodeSuffix);
		setCountryCode(countryCode);
		setGoogleMapsUrl(place.getGoogleMapsUri());
	}

	public NormalizedPlace() {
		// Nothing to do
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}

	@Nullable
	public String getGooglePlaceId() {
		return this.googlePlaceId;
	}

	public void setGooglePlaceId(@Nullable String googlePlaceId) {
		this.googlePlaceId = googlePlaceId;
	}

	@Nullable
	public String getGoogleMapsUrl() {
		return this.googleMapsUrl;
	}

	public void setGoogleMapsUrl(@Nullable String googleMapsUrl) {
		this.googleMapsUrl = googleMapsUrl;
	}

	@Nullable
	public Double getLatitude() {
		return this.latitude;
	}

	public void setLatitude(@Nullable Double latitude) {
		this.latitude = latitude;
	}

	@Nullable
	public Double getLongitude() {
		return this.longitude;
	}

	public void setLongitude(@Nullable Double longitude) {
		this.longitude = longitude;
	}

	@Nullable
	public String getPremise() {
		return this.premise;
	}

	public void setPremise(@Nullable String premise) {
		this.premise = premise;
	}

	@Nullable
	public String getSubpremise() {
		return this.subpremise;
	}

	public void setSubpremise(@Nullable String subpremise) {
		this.subpremise = subpremise;
	}

	@Nullable
	public String getFormattedAddress() {
		return this.formattedAddress;
	}

	public void setFormattedAddress(@Nullable String formattedAddress) {
		this.formattedAddress = formattedAddress;
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
	public String getRegionSubdivision() {
		return this.regionSubdivision;
	}

	public void setRegionSubdivision(@Nullable String regionSubdivision) {
		this.regionSubdivision = regionSubdivision;
	}

	@Nullable
	public String getPostalCode() {
		return this.postalCode;
	}

	public void setPostalCode(@Nullable String postalCode) {
		this.postalCode = postalCode;
	}

	@Nullable
	public String getPostalCodeSuffix() {
		return this.postalCodeSuffix;
	}

	public void setPostalCodeSuffix(@Nullable String postalCodeSuffix) {
		this.postalCodeSuffix = postalCodeSuffix;
	}

	@Nullable
	public String getCountryCode() {
		return this.countryCode;
	}

	public void setCountryCode(@Nullable String countryCode) {
		this.countryCode = countryCode;
	}
}
