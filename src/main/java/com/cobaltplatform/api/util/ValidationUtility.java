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

	static {
		EMAIL_VALIDATION_PATTERN = Pattern.compile("^.+@.+(\\.[^\\.]+)+$");
		// e.g. "#FFEE00"
		HEX_COLOR_VALIDATION_PATTERN = Pattern.compile("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$");
	}

	private ValidationUtility() {
		// Non-instantiable
	}

	public static boolean isValidStudentId(@Nullable String value){
		if(value == null) return false;
		if(trimToNull(value).length() != 8){
			return false;
		}
		return value.chars().allMatch(Character::isDigit);
	}

	public static boolean isValidVin(@Nullable String value) {
		value = trimToNull(value);

		if (value == null)
			return false;

		if (value.length() != 17) return false;

		return true;
	}

	public static <T extends Enum<T>> boolean isValidEnum(String input, Class<T> enumClass) {
		if (input == null) return false;

		try {
			Enum.valueOf(enumClass, input);
			return true;
		} catch (IllegalArgumentException argEx) {
			return false;
		}
	}

	public static boolean isValidEmailAddress(@Nullable String value) {
		value = trimToNull(value);

		if (value == null)
			return false;

		return getEmailValidationPattern().matcher(value).matches();
	}

	public static boolean isValidHexColor(@Nullable String value) {
		value = trimToNull(value);

		if (value == null)
			return false;

		return getHexColorValidationPattern().matcher(value).matches();
	}

	public static boolean isValidUUID(@Nullable String value) {
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

	public static boolean isValidLocalDate(@Nullable String value) {
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

	public static boolean isValidInteger(@Nullable String value) {
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

	public static boolean isValidDouble(@Nullable String value) {
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

	public static boolean isValidZipCode(@Nullable String value) {
		value = trimToNull(value);

		if (value == null)
			return false;

		try {
			Integer.parseInt(value);
		} catch (Exception e) {
			return false;
		}

		return value.length() == 5;
	}

	@Nonnull
	private static Pattern getEmailValidationPattern() {
		return EMAIL_VALIDATION_PATTERN;
	}

	@Nonnull
	private static Pattern getHexColorValidationPattern() {
		return HEX_COLOR_VALIDATION_PATTERN;
	}
}
