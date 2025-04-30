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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
public class AppointmentTimeFormatter {
	@Nonnull
	private static final DateTimeFormatter SHARED_TIME_DESCRIPTION_DATE_FORMATTER;
	@Nonnull
	private static final DateTimeFormatter SHARED_TIME_DESCRIPTION_SHORT_TIME_FORMATTER;
	@Nonnull
	private static final DateTimeFormatter SHARED_TIME_DESCRIPTION_LONG_TIME_FORMATTER;
	@Nonnull
	private static final DateTimeFormatter SHARED_TIME_DESCRIPTION_AM_PM_FORMATTER;

	static {
		// Used to generate a string like "Thurs Aug 14 @ 12-12:30pm"
		// Hardcode to US for MVP
		// TODO: support other locales
		SHARED_TIME_DESCRIPTION_DATE_FORMATTER = DateTimeFormatter.ofPattern("E, MMM d", Locale.US);
		SHARED_TIME_DESCRIPTION_SHORT_TIME_FORMATTER = DateTimeFormatter.ofPattern("h", Locale.US);
		SHARED_TIME_DESCRIPTION_LONG_TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm", Locale.US);
		SHARED_TIME_DESCRIPTION_AM_PM_FORMATTER = DateTimeFormatter.ofPattern("a", Locale.US);
	}

	@Nonnull
	public static String createTimeDescription(@Nonnull LocalDateTime startDateTime,
																						 @Nonnull LocalDateTime endDateTime,
																						 @Nonnull ZoneId timeZone) {
		requireNonNull(startDateTime);
		requireNonNull(endDateTime);
		requireNonNull(timeZone);

		return createTimeDescription(startDateTime.atZone(timeZone).toInstant(), endDateTime.atZone(timeZone).toInstant(), timeZone);
	}

	@Nonnull
	public static String createTimeDescription(@Nonnull Instant startTime,
																						 @Nullable Instant endTime,
																						 @Nonnull ZoneId timeZone) {
		requireNonNull(startTime);
		requireNonNull(endTime);
		requireNonNull(timeZone);

		// Used to generate a string like "Thurs Aug 14 @ 12-12:30pm"

		// Generate "Thurs Aug 14"
		String date = getSharedTimeDescriptionDateFormatter().withZone(timeZone).format(startTime);

		// TODO: improve post-MVP
		String startTimeFormatted = getSharedTimeDescriptionLongTimeFormatter().withZone(timeZone).format(startTime);
		String startAmPm = getSharedTimeDescriptionAmPmFormatter().withZone(timeZone).format(startTime).toLowerCase(Locale.US).toUpperCase(Locale.US);
		String endTimeFormatted = getSharedTimeDescriptionLongTimeFormatter().withZone(timeZone).format(endTime);
		String endAmPm = getSharedTimeDescriptionAmPmFormatter().withZone(timeZone).format(endTime).toLowerCase(Locale.US).toUpperCase(Locale.US);

		// Don't show the initial "AM" or "PM" if it's the same as the ending one
		if (startAmPm.equals(endAmPm))
			startAmPm = "";

		return format("%s @ %s%s-%s%s", date, startTimeFormatted, startAmPm, endTimeFormatted, endAmPm);
	}

	@Nonnull
	public static String createDateDescription(@Nonnull LocalDate startDate,
																						 @Nullable LocalDate endDate) {
		String startDateDescription = getSharedTimeDescriptionDateFormatter().format(startDate);
		String endDateDescription = getSharedTimeDescriptionDateFormatter().format(endDate);

		if (startDate.equals(endDate))
			return startDateDescription;

		return format("%s - %s", startDateDescription, endDateDescription);
	}

	@Nonnull
	private static DateTimeFormatter getSharedTimeDescriptionDateFormatter() {
		return SHARED_TIME_DESCRIPTION_DATE_FORMATTER;
	}

	@Nonnull
	private static DateTimeFormatter getSharedTimeDescriptionShortTimeFormatter() {
		return SHARED_TIME_DESCRIPTION_SHORT_TIME_FORMATTER;
	}

	@Nonnull
	private static DateTimeFormatter getSharedTimeDescriptionLongTimeFormatter() {
		return SHARED_TIME_DESCRIPTION_LONG_TIME_FORMATTER;
	}

	@Nonnull
	private static DateTimeFormatter getSharedTimeDescriptionAmPmFormatter() {
		return SHARED_TIME_DESCRIPTION_AM_PM_FORMATTER;
	}
}
