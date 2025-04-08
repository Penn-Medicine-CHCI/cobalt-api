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
import javax.annotation.concurrent.ThreadSafe;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.util.Map;
import java.util.Set;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public final class BusinessHoursCalculator {
	private BusinessHoursCalculator() {
		// Nothing to do
	}

	/**
	 * Represents the business opening and closing times for a day.
	 */
	@Immutable
	public static class BusinessHours {
		@Nonnull
		private final LocalTime startTime;
		@Nonnull
		private final LocalTime endTime;

		@Nonnull
		public static BusinessHours from(@Nonnull LocalTime startTime,
																		 @Nonnull LocalTime endTime) {
			requireNonNull(startTime);
			requireNonNull(endTime);

			return new BusinessHours(startTime, endTime);
		}

		private BusinessHours(@Nonnull LocalTime startTime,
													@Nonnull LocalTime endTime) {
			requireNonNull(startTime);
			requireNonNull(endTime);

			if (!startTime.isBefore(endTime))
				throw new IllegalArgumentException(format("Start time %s must be before end time %s", startTime, endTime));

			this.startTime = startTime;
			this.endTime = endTime;
		}

		@Nonnull
		public LocalTime getStartTime() {
			return this.startTime;
		}

		@Nonnull
		public LocalTime getEndTime() {
			return this.endTime;
		}
	}

	/**
	 * Enum representing holidays.
	 */
	public enum Holiday {
		US_NEW_YEARS_DAY, // January 1
		US_MLK_DAY, // Third Monday in January
		US_PRESIDENTS_DAY, // Third Monday in February
		US_MEMORIAL_DAY, // Last Monday in May
		US_JUNETEENTH, // June 19
		US_INDEPENDENCE_DAY, // July 4
		US_LABOR_DAY, // First Monday in September
		US_INDIGENOUS_PEOPLES_DAY, // Second Monday in October
		US_VETERANS_DAY, // November 11
		US_THANKSGIVING, // Fourth Thursday in November
		US_CHRISTMAS // December 25
	}

	/**
	 * Calculates the number of business hours between two LocalDateTime instances.
	 *
	 * @param startDateTime            the starting LocalDateTime
	 * @param endDateTime              the ending LocalDateTime
	 * @param businessHoursByDayOfWeek a map where each DayOfWeek is associated with the corresponding BusinessHours
	 * @param holidays                 a set of Holiday enums representing days when the business is closed
	 * @return the total number of business hours (can be fractional)
	 */
	@Nonnull
	public static Double calculateBusinessHours(@Nonnull LocalDateTime startDateTime,
																							@Nonnull LocalDateTime endDateTime,
																							@Nonnull Map<DayOfWeek, BusinessHours> businessHoursByDayOfWeek,
																							@Nonnull Set<Holiday> holidays) {
		requireNonNull(startDateTime);
		requireNonNull(endDateTime);
		requireNonNull(businessHoursByDayOfWeek);
		requireNonNull(holidays);

		if (!startDateTime.isBefore(endDateTime))
			throw new IllegalArgumentException(format("Start datetime %s must be before end datetime %s", startDateTime, endDateTime));

		double totalHours = 0.0;
		LocalDate startDate = startDateTime.toLocalDate();
		LocalDate endDate = endDateTime.toLocalDate();

		// If source and target are on the same day, calculate directly.
		if (startDate.equals(endDate)) {
			totalHours += calculateBusinessHoursForDay(startDate, startDateTime.toLocalTime(), endDateTime.toLocalTime(), businessHoursByDayOfWeek, holidays);
		} else {
			// Process the first day: count from the source time until the end of business hours.
			totalHours += calculateBusinessHoursForDay(startDate, startDateTime.toLocalTime(), null, businessHoursByDayOfWeek, holidays);

			// Process the intermediate days.
			LocalDate current = startDate.plusDays(1);
			while (current.isBefore(endDate)) {
				totalHours += calculateBusinessHoursForDay(current, null, null, businessHoursByDayOfWeek, holidays);
				current = current.plusDays(1);
			}

			// Process the final day: count from the start of business hours until the target time.
			totalHours += calculateBusinessHoursForDay(endDate, null, endDateTime.toLocalTime(), businessHoursByDayOfWeek, holidays);
		}

		return totalHours;
	}

	/**
	 * Calculates the business hours for a given day.
	 *
	 * @param date                     the date for which to calculate the business hours.
	 * @param fromTime                 an optional starting time for the day (if null, the business day’s start time is used)
	 * @param toTime                   an optional ending time for the day (if null, the business day’s end time is used)
	 * @param businessHoursByDayOfWeek a map of business hours by DayOfWeek
	 * @param holidays                 a set of Holiday enums that mark when the business is closed.
	 * @return the number of business hours for the given day.
	 */
	@Nonnull
	private static Double calculateBusinessHoursForDay(@Nonnull LocalDate date,
																										 @Nullable LocalTime fromTime,
																										 @Nullable LocalTime toTime,
																										 @Nonnull Map<DayOfWeek, BusinessHours> businessHoursByDayOfWeek,
																										 @Nonnull Set<Holiday> holidays) {
		requireNonNull(date);
		requireNonNull(businessHoursByDayOfWeek);
		requireNonNull(holidays);

		// If the day is not a business day (either no business hours defined or it is a holiday), return 0.
		if (!isBusinessDay(date, businessHoursByDayOfWeek, holidays))
			return 0.0;

		BusinessHours businessHours = businessHoursByDayOfWeek.get(date.getDayOfWeek());
		LocalTime businessStart = businessHours.getStartTime();
		LocalTime businessEnd = businessHours.getEndTime();

		// Calculate the effective start and end times.
		LocalTime effectiveStart = (fromTime != null) ? maxTime(businessStart, fromTime) : businessStart;
		LocalTime effectiveEnd = (toTime != null) ? minTime(businessEnd, toTime) : businessEnd;

		if (effectiveEnd.isAfter(effectiveStart)) {
			long minutes = Duration.between(effectiveStart, effectiveEnd).toMinutes();
			return minutes / 60.0;
		}

		return 0.0;
	}

	/**
	 * Determines if a given date is a business day.
	 * It is not a business day if no business hours are specified for that day-of-week or if it matches a holiday.
	 *
	 * @param date                     the date to check.
	 * @param businessHoursByDayOfWeek a map containing business hours for each day-of-week.
	 * @param holidays                 a set of Holiday enums representing days off.
	 * @return true if the date is a business day; false otherwise.
	 */
	@Nonnull
	private static Boolean isBusinessDay(@Nonnull LocalDate date,
																			 @Nonnull Map<DayOfWeek, BusinessHours> businessHoursByDayOfWeek,
																			 @Nonnull Set<Holiday> holidays) {
		requireNonNull(date);
		requireNonNull(businessHoursByDayOfWeek);
		requireNonNull(holidays);

		// If no business hours are defined for the day.
		if (!businessHoursByDayOfWeek.containsKey(date.getDayOfWeek()))
			return false;

		// Check if the date matches any holiday.
		for (Holiday holiday : holidays)
			if (holidayMatchesDate(holiday, date))
				return false;

		return true;
	}

	/**
	 * Determines if a given Holiday falls on the specified date.
	 * This method implements logic for fixed-date holidays as well as those that are computed.
	 *
	 * @param holiday the holiday enum value.
	 * @param date    the date to check.
	 * @return true if the holiday falls on the given date; otherwise false.
	 */
	@Nonnull
	private static Boolean holidayMatchesDate(@Nonnull Holiday holiday,
																						@Nonnull LocalDate date) {
		requireNonNull(holiday);
		requireNonNull(date);

		switch (holiday) {
			case US_NEW_YEARS_DAY:
				return MonthDay.from(date).equals(MonthDay.of(1, 1));
			case US_MLK_DAY: {
				// Third Monday in January.
				LocalDate firstJanuary = LocalDate.of(date.getYear(), 1, 1);
				int adjustment = (DayOfWeek.MONDAY.getValue() - firstJanuary.getDayOfWeek().getValue() + 7) % 7;
				LocalDate firstMonday = firstJanuary.plusDays(adjustment);
				LocalDate mlkDay = firstMonday.plusDays(14); // third Monday
				return date.equals(mlkDay);
			}
			case US_PRESIDENTS_DAY: {
				// Third Monday in February.
				LocalDate firstFebruary = LocalDate.of(date.getYear(), 2, 1);
				int adjustment = (DayOfWeek.MONDAY.getValue() - firstFebruary.getDayOfWeek().getValue() + 7) % 7;
				LocalDate firstMonday = firstFebruary.plusDays(adjustment);
				LocalDate presidentsDay = firstMonday.plusDays(14); // third Monday
				return date.equals(presidentsDay);
			}
			case US_MEMORIAL_DAY: {
				// Memorial Day: Last Monday in May.
				LocalDate lastMay = LocalDate.of(date.getYear(), 5, 31);
				while (lastMay.getDayOfWeek() != DayOfWeek.MONDAY)
					lastMay = lastMay.minusDays(1);

				return date.equals(lastMay);
			}
			case US_JUNETEENTH:
				return MonthDay.from(date).equals(MonthDay.of(6, 19));
			case US_INDEPENDENCE_DAY:
				return MonthDay.from(date).equals(MonthDay.of(7, 4));
			case US_LABOR_DAY: {
				// Labor Day: First Monday in September.
				LocalDate firstSeptember = LocalDate.of(date.getYear(), 9, 1);
				int adjustment = (DayOfWeek.MONDAY.getValue() - firstSeptember.getDayOfWeek().getValue() + 7) % 7;
				LocalDate laborDay = firstSeptember.plusDays(adjustment);
				return date.equals(laborDay);
			}
			case US_INDIGENOUS_PEOPLES_DAY: {
				// Columbus Day: Second Monday in October.
				LocalDate firstOctober = LocalDate.of(date.getYear(), 10, 1);
				int adjustment = (DayOfWeek.MONDAY.getValue() - firstOctober.getDayOfWeek().getValue() + 7) % 7;
				LocalDate firstMonday = firstOctober.plusDays(adjustment);
				LocalDate columbusDay = firstMonday.plusDays(7);
				return date.equals(columbusDay);
			}
			case US_VETERANS_DAY:
				return MonthDay.from(date).equals(MonthDay.of(11, 11));
			case US_THANKSGIVING: {
				// Thanksgiving: Fourth Thursday in November.
				LocalDate firstNovember = LocalDate.of(date.getYear(), 11, 1);
				int daysToThursday = (DayOfWeek.THURSDAY.getValue() - firstNovember.getDayOfWeek().getValue() + 7) % 7;
				LocalDate firstThursday = firstNovember.plusDays(daysToThursday);
				LocalDate thanksgiving = firstThursday.plusDays(21); // fourth Thursday
				return date.equals(thanksgiving);
			}
			case US_CHRISTMAS:
				return MonthDay.from(date).equals(MonthDay.of(12, 25));
			default:
				throw new IllegalArgumentException(format("Unsupported value: %s.%s", Holiday.class.getSimpleName(), holiday.name()));
		}
	}

	// Helper methods for comparing times.
	@Nonnull
	private static LocalTime maxTime(@Nonnull LocalTime t1,
																	 @Nonnull LocalTime t2) {
		requireNonNull(t1);
		requireNonNull(t2);

		return (t1.isAfter(t2)) ? t1 : t2;
	}

	@Nonnull
	private static LocalTime minTime(@Nonnull LocalTime t1,
																	 @Nonnull LocalTime t2) {
		requireNonNull(t1);
		requireNonNull(t2);

		return (t1.isBefore(t2)) ? t1 : t2;
	}

	// Optionally, you can add a main method or unit tests to demonstrate usage.
	public static void main(String[] args) {
		// Define business hours for weekdays.
		Map<DayOfWeek, BusinessHours> openTimes = Map.of(
				DayOfWeek.MONDAY, new BusinessHours(LocalTime.of(9, 0), LocalTime.of(17, 0)),
				DayOfWeek.TUESDAY, new BusinessHours(LocalTime.of(9, 0), LocalTime.of(17, 0)),
				DayOfWeek.WEDNESDAY, new BusinessHours(LocalTime.of(9, 0), LocalTime.of(17, 0)),
				DayOfWeek.THURSDAY, new BusinessHours(LocalTime.of(9, 0), LocalTime.of(17, 0)),
				DayOfWeek.FRIDAY, new BusinessHours(LocalTime.of(9, 0), LocalTime.of(17, 0))
				// Weekends are omitted and thus considered closed.
		);

		// For demonstration, assume the business is closed on all holidays.
		Set<Holiday> holidays = Set.of(
				Holiday.US_NEW_YEARS_DAY,
				Holiday.US_MLK_DAY,
				Holiday.US_PRESIDENTS_DAY,
				Holiday.US_MEMORIAL_DAY,
				Holiday.US_INDEPENDENCE_DAY,
				Holiday.US_LABOR_DAY,
				Holiday.US_INDIGENOUS_PEOPLES_DAY,
				Holiday.US_VETERANS_DAY,
				Holiday.US_THANKSGIVING,
				Holiday.US_CHRISTMAS
		);

		LocalDateTime source = LocalDateTime.of(2025, 4, 8, 15, 30);
		LocalDateTime target = LocalDateTime.of(2025, 4, 10, 11, 0);

		double businessHours = calculateBusinessHours(source, target, openTimes, holidays);
		System.out.println("Business hours between " + source + " and " + target + ": " + businessHours);
	}
}