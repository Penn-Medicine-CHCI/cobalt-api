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

import com.cobaltplatform.api.context.CurrentContext;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.cobaltplatform.api.util.ValidationUtility.isValidHexColor;
import static com.cobaltplatform.api.util.ValidationUtility.isValidIso3166TwoLetterCountryCode;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class Normalizer {
	@Nonnull
	private static final Map<String, String> ISO_3166_THREE_TO_TWO_LETTER_COUNTRY_CODES;
	@Nonnull
	private static final DateTimeFormatter US_TIME_PARSING_FORMATTER;

	@Nullable
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final Logger logger;

	static {
		Map<String, String> iso3166ThreeToTwoLetterCountryCodes = new HashMap<>();

		for (String twoLetterCountryCode : Locale.getISOCountries()) {
			String threeLetterCountryCode = new Locale("", twoLetterCountryCode).getISO3Country().toUpperCase(Locale.US);
			iso3166ThreeToTwoLetterCountryCodes.put(threeLetterCountryCode, twoLetterCountryCode);
		}

		ISO_3166_THREE_TO_TWO_LETTER_COUNTRY_CODES = Collections.unmodifiableMap(iso3166ThreeToTwoLetterCountryCodes);

		// Valid string looks like "1:05AM"
		US_TIME_PARSING_FORMATTER = DateTimeFormatter.ofPattern("h:mma", Locale.US);
	}

	public Normalizer() {
		this(null);
	}

	@Inject
	public Normalizer(@Nullable Provider<CurrentContext> currentContextProvider) {
		this.currentContextProvider = currentContextProvider;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	public Optional<String> normalizePhoneNumberToE164(@Nullable String phoneNumber) {
		return normalizePhoneNumberToE164(phoneNumber, getCurrentContext().get().getLocale());
	}

	@Nonnull
	public Optional<String> normalizePhoneNumberToE164(@Nullable String phoneNumber,
																										 @Nonnull Locale locale) {
		requireNonNull(locale);

		phoneNumber = trimToNull(phoneNumber);

		if (phoneNumber == null)
			return Optional.empty();

		final String FALLBACK_COUNTRY_CODE = "US";

		String countryCode = trimToNull(locale.getCountry());

		if (countryCode == null) {
			getLogger().info("There is no country code available for locale '{}', falling back to {}...", locale, FALLBACK_COUNTRY_CODE);
			countryCode = FALLBACK_COUNTRY_CODE;
		}

		Phonenumber.PhoneNumber parsedPhoneNumber = null;

		try {
			parsedPhoneNumber = PhoneNumberUtil.getInstance().parse(phoneNumber, countryCode);
		} catch (NumberParseException e) {
			getLogger().info(format("Unable to parse phone number %s with country code %s", phoneNumber, countryCode), e);
			return Optional.empty();
		}

		if (!PhoneNumberUtil.getInstance().isValidNumber(parsedPhoneNumber))
			return Optional.empty();

		return Optional.ofNullable(PhoneNumberUtil.getInstance().format(parsedPhoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164));
	}

	@Nonnull
	public Optional<String> normalizeCountryCodeToIso3166TwoLetter(@Nullable String countryCode) {
		countryCode = trimToEmpty(countryCode).toUpperCase(Locale.US);

		if (countryCode.length() == 0)
			return Optional.empty();

		if (countryCode.length() == 3) {
			return Optional.ofNullable(getIso3166ThreeToTwoLetterCountryCodes().get(countryCode));
		} else if (countryCode.length() == 2) {
			if (isValidIso3166TwoLetterCountryCode(countryCode))
				return Optional.of(countryCode);
			else
				return Optional.empty();
		}

		return Optional.empty();
	}

	@Nonnull
	public Optional<Integer> normalizeHexColor(@Nullable String hexColor) {
		hexColor = trimToNull(hexColor);

		if (hexColor == null || !isValidHexColor(hexColor))
			return Optional.empty();

		// Strip off leading "#"
		hexColor = hexColor.substring(1);

		return Optional.of(Integer.parseInt(hexColor, 16));
	}

	@Nonnull
	public Optional<String> normalizeEmailAddress(@Nullable String emailAddress) {
		return normalizeEmailAddress(emailAddress, null);
	}

	@Nonnull
	public static Optional<String> normalizeEmailAddress(@Nullable String emailAddress,
																											 @Nullable Locale locale) {
		emailAddress = trimToNull(emailAddress);

		if (emailAddress == null)
			return Optional.empty();

		final Locale FALLBACK_LOCALE = Locale.ENGLISH;

		if (locale == null)
			locale = FALLBACK_LOCALE;

		return Optional.of(emailAddress.toLowerCase(locale));
	}

	@Nonnull
	public Optional<LocalTime> normalizeTime(@Nullable String timeAsString) {
		return normalizeTime(timeAsString, getCurrentContext().get().getLocale());
	}

	@Nonnull
	public Optional<LocalTime> normalizeTime(@Nullable String timeAsString,
																					 @Nonnull Locale locale) {
		requireNonNull(locale);

		timeAsString = trimToNull(timeAsString);

		if (timeAsString == null)
			return Optional.empty();

		// TODO: support other locales
		if (!locale.equals(Locale.US))
			throw new IllegalArgumentException(format("Locale %s is not yet supported for time parsing", locale.toLanguageTag()));

		// Remove all whitespace
		timeAsString = timeAsString.replaceAll("\\s", "").toUpperCase(Locale.US);

		System.out.println(timeAsString);

		try {
			// Valid string looks like "1:05AM"
			return Optional.of(LocalTime.parse(timeAsString, US_TIME_PARSING_FORMATTER));
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	@Nonnull
	public static Optional<String> normalizeName(@Nullable String firstName,
																							 @Nullable String lastName) {
		firstName = trimToNull(firstName);
		lastName = trimToNull(lastName);

		if (firstName != null && lastName != null)
			return Optional.of(format("%s %s", firstName, lastName));

		if (firstName != null)
			return Optional.of(firstName);

		if (lastName != null)
			return Optional.of(lastName);

		return Optional.empty();
	}

	@Nonnull
	public static Optional<String> normalizeNameWithLastFirst(@Nullable String firstName,
																														@Nullable String lastName) {
		firstName = trimToNull(firstName);
		lastName = trimToNull(lastName);

		if (firstName != null && lastName != null)
			return Optional.of(format("%s, %s", lastName, firstName));

		if (firstName != null)
			return Optional.of(firstName);

		if (lastName != null)
			return Optional.of(lastName);

		return Optional.empty();
	}


	@Nonnull
	public Optional<String> normalizeNameCasing(@Nullable String name) {
		return normalizeNameCasing(name, getCurrentContext().get().getLocale());
	}

	/**
	 * "LOREN SANDERS" -> "Loren Sanders"
	 */
	@Nonnull
	public static Optional<String> normalizeNameCasing(@Nullable String name,
																										 @Nonnull Locale locale) {
		requireNonNull(locale);

		name = trimToNull(name);

		if (name == null)
			return Optional.empty();

		List<String> nameComponents = new ArrayList<>();

		name = name.toLowerCase(locale);

		for (String nameComponent : name.replaceAll("\\s{2,}", " ").split(" "))
			nameComponents.add(nameComponent.length() == 1
					? nameComponent.toUpperCase(locale)
					: nameComponent.substring(0, 1).toUpperCase(locale) + nameComponent.substring(1));

		return Optional.of(nameComponents.stream().collect(Collectors.joining(" ")));
	}

	@Nonnull
	public NameComponents normalizeNameToComponents(@Nullable String name) {
		return normalizeNameToComponents(name, getCurrentContext().get().getLocale());
	}

	@Nonnull
	public NameComponents normalizeNameToComponents(@Nullable String name,
																									@Nonnull Locale locale) {
		requireNonNull(locale);

		name = trimToNull(name);

		if (name == null)
			return NameComponents.emptyNameComponents();

		List<String> nameComponents = new ArrayList<>();

		name = name.toLowerCase(locale);

		for (String nameComponent : name.replaceAll("\\s{2,}", " ").split(" "))
			nameComponents.add(nameComponent);

		if (nameComponents.size() == 0)
			return NameComponents.emptyNameComponents();

		if (nameComponents.size() == 1)
			return new NameComponents(nameComponents.get(0), null);

		// TODO: support for more than 2 components
		return new NameComponents(nameComponents.get(0), nameComponents.get(1));
	}

	@Immutable
	public static class NameComponents {
		@Nonnull
		private static final NameComponents EMPTY_NAME_COMPONENTS;

		@Nullable
		private final String firstName;
		@Nullable
		private final String lastName;

		static {
			EMPTY_NAME_COMPONENTS = new NameComponents(null, null);
		}

		public NameComponents(@Nullable String firstName,
													@Nullable String lastName) {
			this.firstName = firstName;
			this.lastName = lastName;
		}

		@Nonnull
		public static NameComponents emptyNameComponents() {
			return EMPTY_NAME_COMPONENTS;
		}

		@Nullable
		public Optional<String> getFirstName() {
			return Optional.ofNullable(firstName);
		}

		@Nullable
		public Optional<String> getLastName() {
			return Optional.ofNullable(lastName);
		}
	}

	@Nonnull
	protected Map<String, String> getIso3166ThreeToTwoLetterCountryCodes() {
		return ISO_3166_THREE_TO_TWO_LETTER_COUNTRY_CODES;
	}

	@Nonnull
	protected Optional<CurrentContext> getCurrentContext() {
		return Optional.ofNullable(currentContextProvider == null ? null : currentContextProvider.get());
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}