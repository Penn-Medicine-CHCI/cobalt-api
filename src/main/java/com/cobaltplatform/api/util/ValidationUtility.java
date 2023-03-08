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

package com.cobaltplatform.api.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.Locale;
import java.util.Locale.IsoCountryCode;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public final class ValidationUtility {
	@Nonnull
	private static final Pattern EMAIL_VALIDATION_PATTERN;
	@Nonnull
	private static final Pattern HEX_COLOR_VALIDATION_PATTERN;
	@Nonnull
	private static final Pattern US_POSTAL_CODE_VALIDATION_PATTERN;
	@Nonnull
	private static final Set<String> ISO_3166_TWO_LETTER_COUNTRY_CODES;
	@Nonnull
	private static final Set<String> ISO_3166_THREE_LETTER_COUNTRY_CODES;

	static {
		// Pretty lenient
		EMAIL_VALIDATION_PATTERN = Pattern.compile("^.+@.+(\\.[^\\.]+)+$");

		// e.g. "#FFEE00"
		HEX_COLOR_VALIDATION_PATTERN = Pattern.compile("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$");

		// Can be:
		// 5 digits
		// 5 digits, hyphen, 4 digits
		US_POSTAL_CODE_VALIDATION_PATTERN = Pattern.compile("^[0-9]{5}(?:-[0-9]{4})?$");
		ISO_3166_TWO_LETTER_COUNTRY_CODES = Collections.unmodifiableSet(Locale.getISOCountries(IsoCountryCode.PART1_ALPHA2));
		ISO_3166_THREE_LETTER_COUNTRY_CODES = Collections.unmodifiableSet(Locale.getISOCountries(IsoCountryCode.PART1_ALPHA3));
	}

	private ValidationUtility() {
		// Non-instantiable
	}

	@Nonnull
	public static <T extends Enum<T>> Boolean isValidEnum(String input, Class<T> enumClass) {
		if (input == null) return false;

		try {
			Enum.valueOf(enumClass, input);
			return true;
		} catch (IllegalArgumentException argEx) {
			return false;
		}
	}

	@Nonnull
	public static Boolean isValidEmailAddress(@Nullable String value) {
		value = trimToNull(value);

		if (value == null)
			return false;

		return getEmailValidationPattern().matcher(value).matches();
	}

	@Nonnull
	public static Boolean isValidUrl(@Nullable String value) {
		value = trimToNull(value);

		if (value == null)
			return false;

		value = value.toUpperCase(Locale.US);

		// Loose but "good enough" for most cases...
		return value.startsWith("https://") || value.startsWith("http://");
	}

	@Nonnull
	public static Boolean isValidHexColor(@Nullable String value) {
		value = trimToNull(value);

		if (value == null)
			return false;

		return getHexColorValidationPattern().matcher(value).matches();
	}

	@Nonnull
	public static Boolean isValidUUID(@Nullable String value) {
		value = trimToNull(value);

		if (value == null)
			return false;

		try {
			UUID.fromString(value);
		} catch (IllegalArgumentException e) {
			return false;
		}

		return true;
	}

	@Nonnull
	public static Boolean isValidLocalDate(@Nullable String value) {
		value = trimToNull(value);

		if (value == null)
			return false;

		try {
			LocalDate.parse(value);
		} catch (DateTimeParseException e) {
			return false;
		}

		return true;
	}

	@Nonnull
	public static Boolean isValidInteger(@Nullable String value) {
		value = trimToNull(value);

		if (value == null)
			return false;

		try {
			Integer.parseInt(value);
		} catch (Exception e) {
			return false;
		}

		return true;
	}

	@Nonnull
	public static Boolean isValidDouble(@Nullable String value) {
		value = trimToNull(value);

		if (value == null)
			return false;

		try {
			Double.parseDouble(value);
		} catch (Exception e) {
			return false;
		}

		return true;
	}

	@Nonnull
	public static Boolean isValidUsPostalCode(@Nullable String value) {
		value = trimToNull(value);

		if (value == null)
			return false;

		return getUsPostalCodeValidationPattern().matcher(value).matches();
	}

	public static Boolean isValidIso3166CountryCode(@Nullable String value) {
		if (value == null)
			return false;

		return isValidIso3166TwoLetterCountryCode(value) || isValidIso3166ThreeLetterCountryCode(value);
	}

	@Nonnull
	public static Boolean isValidIso3166TwoLetterCountryCode(@Nullable String value) {
		if (value == null || value.length() != 2)
			return false;

		return getIso3166TwoLetterCountryCodes().contains(value);
	}

	@Nonnull
	public static Boolean isValidIso3166ThreeLetterCountryCode(@Nullable String value) {
		if (value == null || value.length() != 3)
			return false;

		return getIso3166ThreeLetterCountryCodes().contains(value);
	}

	@Nonnull
	private static Pattern getEmailValidationPattern() {
		return EMAIL_VALIDATION_PATTERN;
	}

	@Nonnull
	private static Pattern getHexColorValidationPattern() {
		return HEX_COLOR_VALIDATION_PATTERN;
	}

	@Nonnull
	private static Pattern getUsPostalCodeValidationPattern() {
		return US_POSTAL_CODE_VALIDATION_PATTERN;
	}

	@Nonnull
	private static Set<String> getIso3166TwoLetterCountryCodes() {
		return ISO_3166_TWO_LETTER_COUNTRY_CODES;
	}

	@Nonnull
	private static Set<String> getIso3166ThreeLetterCountryCodes() {
		return ISO_3166_THREE_LETTER_COUNTRY_CODES;
	}
}
