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

package com.cobaltplatform.api.model.service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class Region {
	@Nonnull
	private static final Map<String, Map<String, Region>> REGIONS_BY_ABBREVIATION_BY_COUNTRY_CODE;

	@Nonnull
	private final String name;
	@Nonnull
	private final String abbreviation;
	@Nonnull
	private final String countryCode; // ISO-3166 2-letter

	static {
		List<Region> regions = new ArrayList<>();

		// US regions
		regions.add(new Region("Alabama", "AL", "US"));
		regions.add(new Region("Alaska", "AK", "US"));
		regions.add(new Region("Arizona", "AZ", "US"));
		regions.add(new Region("Arkansas", "AR", "US"));
		regions.add(new Region("California", "CA", "US"));
		regions.add(new Region("Canal Zone", "CZ", "US"));
		regions.add(new Region("Colorado", "CO", "US"));
		regions.add(new Region("Connecticut", "CT", "US"));
		regions.add(new Region("Delaware", "DE", "US"));
		regions.add(new Region("District of Columbia", "DC", "US"));
		regions.add(new Region("Florida", "FL", "US"));
		regions.add(new Region("Georgia", "GA", "US"));
		regions.add(new Region("Guam", "GU", "US"));
		regions.add(new Region("Hawaii", "HI", "US"));
		regions.add(new Region("Idaho", "ID", "US"));
		regions.add(new Region("Illinois", "IL", "US"));
		regions.add(new Region("Indiana", "IN", "US"));
		regions.add(new Region("Iowa", "IA", "US"));
		regions.add(new Region("Kansas", "KS", "US"));
		regions.add(new Region("Kentucky", "KY", "US"));
		regions.add(new Region("Louisiana", "LA", "US"));
		regions.add(new Region("Maine", "ME", "US"));
		regions.add(new Region("Maryland", "MD", "US"));
		regions.add(new Region("Massachusetts", "MA", "US"));
		regions.add(new Region("Michigan", "MI", "US"));
		regions.add(new Region("Minnesota", "MN", "US"));
		regions.add(new Region("Mississippi", "MS", "US"));
		regions.add(new Region("Missouri", "MO", "US"));
		regions.add(new Region("Montana", "MT", "US"));
		regions.add(new Region("Nebraska", "NE", "US"));
		regions.add(new Region("Nevada", "NV", "US"));
		regions.add(new Region("New Hampshire", "NH", "US"));
		regions.add(new Region("New Jersey", "NJ", "US"));
		regions.add(new Region("New Mexico", "NM", "US"));
		regions.add(new Region("New York", "NY", "US"));
		regions.add(new Region("North Carolina", "NC", "US"));
		regions.add(new Region("North Dakota", "ND", "US"));
		regions.add(new Region("Ohio", "OH", "US"));
		regions.add(new Region("Oklahoma", "OK", "US"));
		regions.add(new Region("Oregon", "OR", "US"));
		regions.add(new Region("Pennsylvania", "PA", "US"));
		regions.add(new Region("Puerto Rico", "PR", "US"));
		regions.add(new Region("Rhode Island", "RI", "US"));
		regions.add(new Region("South Carolina", "SC", "US"));
		regions.add(new Region("South Dakota", "SD", "US"));
		regions.add(new Region("Tennessee", "TN", "US"));
		regions.add(new Region("Texas", "TX", "US"));
		regions.add(new Region("Utah", "UT", "US"));
		regions.add(new Region("Vermont", "VT", "US"));
		regions.add(new Region("Virgin Islands", "VI", "US"));
		regions.add(new Region("Virginia", "VA", "US"));
		regions.add(new Region("Washington", "WA", "US"));
		regions.add(new Region("West Virginia", "WV", "US"));
		regions.add(new Region("Wisconsin", "WI", "US"));
		regions.add(new Region("Wyoming", "WY", "US"));

		// TODO: support additional regions once we need to go international and replace this in-memory stuff with a DB table

		Map<String, Map<String, Region>> regionsByAbbreviationByCountryCode = new HashMap<>();

		for (Region region : regions) {
			Map<String, Region> regionsByAbbreviation = regionsByAbbreviationByCountryCode.get(region.getCountryCode());

			if (regionsByAbbreviation == null) {
				regionsByAbbreviation = new HashMap<>();
				regionsByAbbreviationByCountryCode.put(region.getCountryCode(), regionsByAbbreviation);
			}

			regionsByAbbreviation.put(region.getAbbreviation(), region);
		}

		REGIONS_BY_ABBREVIATION_BY_COUNTRY_CODE = Collections.unmodifiableMap(regionsByAbbreviationByCountryCode);
	}

	public Region(@Nonnull String name,
								@Nonnull String abbreviation,
								@Nonnull String countryCode) {
		requireNonNull(name);
		requireNonNull(abbreviation);
		requireNonNull(countryCode);

		this.name = name;
		this.abbreviation = abbreviation;
		this.countryCode = countryCode;
	}

	@Override
	public String toString() {
		return format("%s{name=%s, abbreviation=%s, countryCode=%s}", getClass().getSimpleName(),
				getName(), getAbbreviation(), getCountryCode());
	}

	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;

		if (other == null || !getClass().equals(other.getClass()))
			return false;

		Region otherRegion = (Region) other;
		return Objects.equals(this.getName(), otherRegion.getName())
				&& Objects.equals(this.getAbbreviation(), otherRegion.getAbbreviation())
				&& Objects.equals(this.getCountryCode(), otherRegion.getCountryCode());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getName(), getAbbreviation(), getCountryCode());
	}

	@Nonnull
	public static Optional<Region> forAbbreviationAndCountryCode(@Nullable String abbreviation,
																															 @Nullable String countryCode) {
		abbreviation = trimToEmpty(abbreviation).toUpperCase(Locale.US);
		countryCode = trimToEmpty(countryCode).toUpperCase(Locale.US);

		if (abbreviation.length() == 0 || countryCode.length() == 0)
			return Optional.empty();

		Map<String, Region> regionsByAbbreviation = getRegionsByAbbreviationByCountryCode().get(countryCode);

		if (regionsByAbbreviation == null)
			return Optional.empty();

		return Optional.ofNullable(regionsByAbbreviation.get(abbreviation));
	}

	@Nonnull
	public String getAbbreviation() {
		return this.abbreviation;
	}

	@Nonnull
	public String getName() {
		return this.name;
	}

	@Nonnull
	public String getCountryCode() {
		return this.countryCode;
	}

	@Nonnull
	private static Map<String, Map<String, Region>> getRegionsByAbbreviationByCountryCode() {
		return REGIONS_BY_ABBREVIATION_BY_COUNTRY_CODE;
	}
}
