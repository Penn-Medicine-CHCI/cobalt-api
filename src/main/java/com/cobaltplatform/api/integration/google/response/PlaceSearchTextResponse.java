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

package com.cobaltplatform.api.integration.google.response;

import com.google.maps.places.v1.Place;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;

import static java.lang.String.format;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class PlaceSearchTextResponse {
	@Nullable
	private List<Place> rawPlaces;
	@Nullable
	private List<NormalizedPlace> normalizedPlaces;

	@Override
	public String toString() {
		return format("%s{normalizedPlaces=%s}", getClass().getSimpleName(), getNormalizedPlaces());
	}

	@NotThreadSafe
	public static class NormalizedPlace {
		@Nullable
		private String googlePlaceId;
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

	@Nullable
	public List<Place> getRawPlaces() {
		return this.rawPlaces;
	}

	public void setRawPlaces(@Nullable List<Place> rawPlaces) {
		this.rawPlaces = rawPlaces;
	}

	@Nullable
	public List<NormalizedPlace> getNormalizedPlaces() {
		return this.normalizedPlaces;
	}

	public void setNormalizedPlaces(@Nullable List<NormalizedPlace> normalizedPlaces) {
		this.normalizedPlaces = normalizedPlaces;
	}
}
