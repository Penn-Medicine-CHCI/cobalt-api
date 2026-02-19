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

package com.cobaltplatform.api.service;

import com.cobaltplatform.api.error.ErrorReporter;
import com.cobaltplatform.api.model.api.request.CreateAddressRequest;
import com.cobaltplatform.api.model.api.request.UpdateAddressRequest;
import com.cobaltplatform.api.model.db.Address;
import com.cobaltplatform.api.model.service.Region;
import com.cobaltplatform.api.util.Normalizer;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.ValidationException.FieldError;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.lokalized.Strings;
import com.pyranid.Database;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.cobaltplatform.api.util.ValidationUtility.isValidIso3166CountryCode;
import static com.cobaltplatform.api.util.ValidationUtility.isValidUsPostalCode;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class AddressService {
	@Nonnull
	private final DatabaseProvider databaseProvider;
	@Nonnull
	private final ErrorReporter errorReporter;
	@Nonnull
	private final Normalizer normalizer;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;

	@Inject
	public AddressService(@Nonnull DatabaseProvider databaseProvider,
												@Nonnull ErrorReporter errorReporter,
												@Nonnull Normalizer normalizer,
												@Nonnull Strings strings) {
		requireNonNull(databaseProvider);
		requireNonNull(errorReporter);
		requireNonNull(normalizer);
		requireNonNull(strings);

		this.databaseProvider = databaseProvider;
		this.errorReporter = errorReporter;
		this.normalizer = normalizer;
		this.strings = strings;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	public Optional<Address> findAddressById(@Nullable UUID addressId) {
		if (addressId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM address
				WHERE address_id=?""", Address.class, addressId);
	}

	@Nonnull
	public Map<UUID, Address> findAddressesByIds(@Nonnull Set<UUID> addressIds) {
		requireNonNull(addressIds);

		if (addressIds.isEmpty())
			return Map.of();

		List<Address> addresses = getDatabase().queryForList("""
					SELECT *
					FROM address
					WHERE address_id = ANY (CAST(? AS UUID[]))
					""", Address.class, (Object) addressIds.toArray(new UUID[0]));

		Map<UUID, Address> addressesById = new LinkedHashMap<>(addresses.size());

		for (Address address : addresses)
			addressesById.put(address.getAddressId(), address);

		return addressesById;
	}

	@Nonnull
	public Optional<Address> findActiveAddressByAccountId(@Nullable UUID accountId) {
		if (accountId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT a.*
				FROM address a, account_address aa
				WHERE aa.account_id=?
				AND aa.address_id=a.address_id
				AND aa.active=TRUE
				""", Address.class, accountId);
	}

	@Nonnull
	public UUID createAddress(@Nonnull CreateAddressRequest request) {
		requireNonNull(request);

		UUID addressId = UUID.randomUUID();
		String postalName = trimToNull(request.getPostalName());
		String streetAddress1 = trimToNull(request.getStreetAddress1());
		String streetAddress2 = trimToNull(request.getStreetAddress2());
		String streetAddress3 = trimToNull(request.getStreetAddress3());
		String streetAddress4 = trimToNull(request.getStreetAddress4());
		String postOfficeBoxNumber = trimToNull(request.getPostOfficeBoxNumber());
		String crossStreet = trimToNull(request.getCrossStreet());
		String suburb = trimToNull(request.getSuburb());
		String locality = trimToNull(request.getLocality());
		String region = trimToNull(request.getRegion());
		String postalCode = trimToNull(request.getPostalCode());
		String countrySubdivisionCode = trimToNull(request.getCountrySubdivisionCode());
		String countryCode = trimToNull(request.getCountryCode());
		String googleMapsUrl = trimToNull(request.getGoogleMapsUrl());
		String googlePlaceId = trimToNull(request.getGooglePlaceId());
		Double latitude = request.getLatitude();
		Double longitude = request.getLongitude();
		String premise = trimToNull(request.getPremise());
		String subpremise = trimToNull(request.getSubpremise());
		String regionSubdivision = trimToNull(request.getRegionSubdivision());
		String postalCodeSuffix = trimToNull(request.getPostalCodeSuffix());
		String formattedAddress = trimToNull(request.getFormattedAddress());
		ValidationException validationException = new ValidationException();

		if (postalName == null)
			validationException.add(new FieldError("postalName", getStrings().get("Postal name is required.")));

		if (streetAddress1 == null)
			validationException.add(new FieldError("streetAddress1", getStrings().get("Street address is required.")));

		// TODO: remove this workaround once we determine the source of bad country data for some patients

		if (countryCode == null) {
			countryCode = "US";
			getErrorReporter().report(format("Warning: missing country code for address %s", ToStringBuilder.reflectionToString(request, ToStringStyle.SHORT_PREFIX_STYLE)));
		} else if (!isValidIso3166CountryCode(countryCode)) {
			countryCode = "US";
			getErrorReporter().report(format("Warning: invalid country code for address %s", ToStringBuilder.reflectionToString(request, ToStringStyle.SHORT_PREFIX_STYLE)));
		}

		// *** END WORKAROUND

		// Currently we only support US addresses.
		// Once we have international rollout, build out more rigorous support for non-US address validation
		if (countryCode == null) {
			validationException.add(new FieldError("countryCode", getStrings().get("Country code is required.")));
		} else if (!isValidIso3166CountryCode(countryCode)) {
			validationException.add(new FieldError("countryCode", getStrings().get("Country code must be a valid ISO-3166 (2 or 3 letter) value.")));
		} else {
			countryCode = getNormalizer().normalizeCountryCodeToIso3166TwoLetter(countryCode).get();

			// US address validation
			if ("US".equals(countryCode)) {
				if (locality == null)
					validationException.add(new FieldError("locality", getStrings().get("City name is required.")));

				// Normalize state abbreviation
				if (region != null)
					region = region.toUpperCase(Locale.US);

				// TODO: don't use US-specific wording once we go international
				if (region == null)
					validationException.add(new FieldError("region", getStrings().get("State abbreviation is required.")));
				else if (Region.forAbbreviationAndCountryCode(region, "US").isEmpty())
					validationException.add(new FieldError("region", getStrings().get("A valid state abbreviation is required.")));

				// TODO: don't use US-specific wording once we go international
				if (postalCode == null)
					validationException.add(new FieldError("postalCode", getStrings().get("ZIP code is required.")));
				else if (!isValidUsPostalCode(postalCode))
					validationException.add(new FieldError("postalCode", getStrings().get("ZIP code is invalid.")));
			} else {
				// TODO: remove once we go international
				validationException.add(new FieldError("countryCode", getStrings().get("Sorry, only US addresses are supported at this time.")));
			}
		}

		if (validationException.hasErrors()) {
			getErrorReporter().report(format("Warning: validation failed for address %s: %s", ToStringBuilder.reflectionToString(request, ToStringStyle.SHORT_PREFIX_STYLE), validationException));
			throw validationException;
		}

		getDatabase().execute("""
						INSERT INTO address (
						address_id, 
						postal_name, 
						street_address_1, 
						street_address_2, 
						street_address_3, 
						street_address_4,
						post_office_box_number, 
						cross_street, 
						suburb,
						locality,
						region,
						postal_code,
						country_subdivision_code,
						country_code,
						google_maps_url,
						google_place_id,
						latitude,
						longitude,
						premise,
						subpremise,
						region_subdivision,
						postal_code_suffix,
						formatted_address
						) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
						""", addressId, postalName, streetAddress1, streetAddress2, streetAddress3, streetAddress4,
				postOfficeBoxNumber, crossStreet, suburb, locality, region, postalCode, countrySubdivisionCode, countryCode,
				googleMapsUrl, googlePlaceId, latitude, longitude, premise, subpremise, regionSubdivision,
				postalCodeSuffix, formattedAddress);

		return addressId;
	}

	@Nonnull
	public UUID updateAddress(@Nonnull UpdateAddressRequest request) {
		requireNonNull(request);

		UUID addressId = request.getAddressId();
		String postalName = trimToNull(request.getPostalName());
		String streetAddress1 = trimToNull(request.getStreetAddress1());
		String streetAddress2 = trimToNull(request.getStreetAddress2());
		String streetAddress3 = trimToNull(request.getStreetAddress3());
		String streetAddress4 = trimToNull(request.getStreetAddress4());
		String postOfficeBoxNumber = trimToNull(request.getPostOfficeBoxNumber());
		String crossStreet = trimToNull(request.getCrossStreet());
		String suburb = trimToNull(request.getSuburb());
		String locality = trimToNull(request.getLocality());
		String region = trimToNull(request.getRegion());
		String postalCode = trimToNull(request.getPostalCode());
		String countrySubdivisionCode = trimToNull(request.getCountrySubdivisionCode());
		String countryCode = trimToNull(request.getCountryCode());
		String googleMapsUrl = trimToNull(request.getGoogleMapsUrl());
		String googlePlaceId = trimToNull(request.getGooglePlaceId());
		Double latitude = request.getLatitude();
		Double longitude = request.getLongitude();
		String premise = trimToNull(request.getPremise());
		String subpremise = trimToNull(request.getSubpremise());
		String regionSubdivision = trimToNull(request.getRegionSubdivision());
		String postalCodeSuffix = trimToNull(request.getPostalCodeSuffix());
		String formattedAddress = trimToNull(request.getFormattedAddress());
		ValidationException validationException = new ValidationException();

		if (postalName == null)
			validationException.add(new FieldError("postalName", getStrings().get("Postal name is required.")));

		if (streetAddress1 == null)
			validationException.add(new FieldError("streetAddress1", getStrings().get("Street address is required.")));

		// TODO: remove this workaround once we determine the source of bad country data for some patients

		if (countryCode == null) {
			countryCode = "US";
			getErrorReporter().report(format("Warning: missing country code for address %s", ToStringBuilder.reflectionToString(request, ToStringStyle.SHORT_PREFIX_STYLE)));
		} else if (!isValidIso3166CountryCode(countryCode)) {
			countryCode = "US";
			getErrorReporter().report(format("Warning: invalid country code for address %s", ToStringBuilder.reflectionToString(request, ToStringStyle.SHORT_PREFIX_STYLE)));
		}

		// *** END WORKAROUND

		// Currently we only support US addresses.
		// Once we have international rollout, build out more rigorous support for non-US address validation
		if (countryCode == null) {
			validationException.add(new FieldError("countryCode", getStrings().get("Country code is required.")));
		} else if (!isValidIso3166CountryCode(countryCode)) {
			validationException.add(new FieldError("countryCode", getStrings().get("Country code must be a valid ISO-3166 (2 or 3 letter) value.")));
		} else {
			countryCode = getNormalizer().normalizeCountryCodeToIso3166TwoLetter(countryCode).get();

			// US address validation
			if ("US".equals(countryCode)) {
				if (locality == null)
					validationException.add(new FieldError("locality", getStrings().get("City name is required.")));

				// Normalize state abbreviation
				if (region != null)
					region = region.toUpperCase(Locale.US);

				// TODO: don't use US-specific wording once we go international
				if (region == null)
					validationException.add(new FieldError("region", getStrings().get("State abbreviation is required.")));
				else if (Region.forAbbreviationAndCountryCode(region, "US").isEmpty())
					validationException.add(new FieldError("region", getStrings().get("A valid state abbreviation is required.")));

				// TODO: don't use US-specific wording once we go international
				if (postalCode == null)
					validationException.add(new FieldError("postalCode", getStrings().get("ZIP code is required.")));
				else if (!isValidUsPostalCode(postalCode))
					validationException.add(new FieldError("postalCode", getStrings().get("ZIP code is invalid.")));
			} else {
				// TODO: remove once we go international
				validationException.add(new FieldError("countryCode", getStrings().get("Sorry, only US addresses are supported at this time.")));
			}
		}

		if (validationException.hasErrors()) {
			getErrorReporter().report(format("Warning: validation failed for address %s: %s", ToStringBuilder.reflectionToString(request, ToStringStyle.SHORT_PREFIX_STYLE), validationException));
			throw validationException;
		}

		getDatabase().execute("""
						UPDATE address SET					 
						postal_name = ?, 
						street_address_1 = ?, 
						street_address_2 = ?, 
						street_address_3 = ?, 
						street_address_4 = ?,
						post_office_box_number = ?, 
						cross_street = ?, 
						suburb = ?,
						locality = ?,
						region = ?,
						postal_code = ?,
						country_subdivision_code = ?,
						country_code = ?,
						google_maps_url = ?,
						google_place_id = ?,
						latitude = ?,
						longitude = ?,
						premise = ?,
						subpremise = ?,
						region_subdivision = ?,
						postal_code_suffix = ?,
						formatted_address = ?
						WHERE address_id = ?						
						""", postalName, streetAddress1, streetAddress2, streetAddress3, streetAddress4,
				postOfficeBoxNumber, crossStreet, suburb, locality, region, postalCode, countrySubdivisionCode, countryCode,
				googleMapsUrl, googlePlaceId, latitude, longitude, premise, subpremise, regionSubdivision,
				postalCodeSuffix, formattedAddress, addressId);

		return addressId;
	}

	@Nonnull
	protected Database getDatabase() {
		return this.databaseProvider.get();
	}

	@Nonnull
	protected ErrorReporter getErrorReporter() {
		return this.errorReporter;
	}

	@Nonnull
	protected Normalizer getNormalizer() {
		return this.normalizer;
	}

	@Nonnull
	protected Strings getStrings() {
		return this.strings;
	}

	@Nonnull
	protected Logger getLogger() {
		return this.logger;
	}
}
