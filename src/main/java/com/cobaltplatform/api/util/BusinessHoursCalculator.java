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

import com.cobaltplatform.api.model.db.Holiday.HolidayId;

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

		@Override
		public String toString() {
			return format("%s{startTime=%s, endTime=%s}", getClass().getSimpleName(), getStartTime(), getEndTime());
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
	 * Calculates the number of business hours between two LocalDateTime instances.
	 *
	 * @param startDateTime            the starting LocalDateTime
	 * @param endDateTime              the ending LocalDateTime
	 * @param businessHoursByDayOfWeek a map where each DayOfWeek is associated with the corresponding BusinessHours
	 * @param holidayIds               a set of Holiday enums representing days when the business is closed
	 * @return the total number of business hours (can be fractional)
	 */
	@Nonnull
	public static Double calculateBusinessHours(@Nonnull LocalDateTime startDateTime,
																							@Nonnull LocalDateTime endDateTime,
																							@Nonnull Map<DayOfWeek, BusinessHours> businessHoursByDayOfWeek,
																							@Nonnull Set<HolidayId> holidayIds) {
		requireNonNull(startDateTime);
		requireNonNull(endDateTime);
		requireNonNull(businessHoursByDayOfWeek);
		requireNonNull(holidayIds);

		if (!startDateTime.isBefore(endDateTime))
			throw new IllegalArgumentException(format("Start datetime %s must be before end datetime %s", startDateTime, endDateTime));

		double totalHours = 0.0;
		LocalDate startDate = startDateTime.toLocalDate();
		LocalDate endDate = endDateTime.toLocalDate();

		// If source and target are on the same day, calculate directly.
		if (startDate.equals(endDate)) {
			totalHours += calculateBusinessHoursForDate(startDate, startDateTime.toLocalTime(), endDateTime.toLocalTime(), businessHoursByDayOfWeek, holidayIds);
		} else {
			// Process the first day: count from the source time until the end of business hours.
			totalHours += calculateBusinessHoursForDate(startDate, startDateTime.toLocalTime(), null, businessHoursByDayOfWeek, holidayIds);

			// Process the intermediate days.
			LocalDate current = startDate.plusDays(1);
			while (current.isBefore(endDate)) {
				totalHours += calculateBusinessHoursForDate(current, null, null, businessHoursByDayOfWeek, holidayIds);
				current = current.plusDays(1);
			}

			// Process the final day: count from the start of business hours until the target time.
			totalHours += calculateBusinessHoursForDate(endDate, null, endDateTime.toLocalTime(), businessHoursByDayOfWeek, holidayIds);
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
	 * @param holidayIds               a set of Holiday enums that mark when the business is closed.
	 * @return the number of business hours for the given day.
	 */
	@Nonnull
	private static Double calculateBusinessHoursForDate(@Nonnull LocalDate date,
																											@Nullable LocalTime fromTime,
																											@Nullable LocalTime toTime,
																											@Nonnull Map<DayOfWeek, BusinessHours> businessHoursByDayOfWeek,
																											@Nonnull Set<HolidayId> holidayIds) {
		requireNonNull(date);
		requireNonNull(businessHoursByDayOfWeek);
		requireNonNull(holidayIds);

		// If the day is not a business day (either no business hours defined or it is a holiday), return 0.
		if (!isBusinessDay(date, businessHoursByDayOfWeek, holidayIds))
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
	 * @param holidayIds               a set of Holiday enums representing days off.
	 * @return true if the date is a business day; false otherwise.
	 */
	@Nonnull
	private static Boolean isBusinessDay(@Nonnull LocalDate date,
																			 @Nonnull Map<DayOfWeek, BusinessHours> businessHoursByDayOfWeek,
																			 @Nonnull Set<HolidayId> holidayIds) {
		requireNonNull(date);
		requireNonNull(businessHoursByDayOfWeek);
		requireNonNull(holidayIds);

		// If no business hours are defined for the day.
		if (!businessHoursByDayOfWeek.containsKey(date.getDayOfWeek()))
			return false;

		// Check if the date matches any holiday.
		for (HolidayId holidayId : holidayIds)
			if (holidayIdMatchesDate(holidayId, date))
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
	private static Boolean holidayIdMatchesDate(@Nonnull HolidayId holidayId,
																							@Nonnull LocalDate date) {
		requireNonNull(holidayId);
		requireNonNull(date);

		switch (holidayId) {
			case NEW_YEARS_DAY:
				return MonthDay.from(date).equals(MonthDay.of(1, 1));
			case MLK_DAY: {
				// Third Monday in January.
				LocalDate firstJanuary = LocalDate.of(date.getYear(), 1, 1);
				int adjustment = (DayOfWeek.MONDAY.getValue() - firstJanuary.getDayOfWeek().getValue() + 7) % 7;
				LocalDate firstMonday = firstJanuary.plusDays(adjustment);
				LocalDate mlkDay = firstMonday.plusDays(14); // third Monday
				return date.equals(mlkDay);
			}
			case PRESIDENTS_DAY: {
				// Third Monday in February.
				LocalDate firstFebruary = LocalDate.of(date.getYear(), 2, 1);
				int adjustment = (DayOfWeek.MONDAY.getValue() - firstFebruary.getDayOfWeek().getValue() + 7) % 7;
				LocalDate firstMonday = firstFebruary.plusDays(adjustment);
				LocalDate presidentsDay = firstMonday.plusDays(14); // third Monday
				return date.equals(presidentsDay);
			}
			case MEMORIAL_DAY: {
				// Memorial Day: Last Monday in May.
				LocalDate lastMay = LocalDate.of(date.getYear(), 5, 31);
				while (lastMay.getDayOfWeek() != DayOfWeek.MONDAY)
					lastMay = lastMay.minusDays(1);

				return date.equals(lastMay);
			}
			case JUNETEENTH:
				return MonthDay.from(date).equals(MonthDay.of(6, 19));
			case INDEPENDENCE_DAY:
				return MonthDay.from(date).equals(MonthDay.of(7, 4));
			case LABOR_DAY: {
				// Labor Day: First Monday in September.
				LocalDate firstSeptember = LocalDate.of(date.getYear(), 9, 1);
				int adjustment = (DayOfWeek.MONDAY.getValue() - firstSeptember.getDayOfWeek().getValue() + 7) % 7;
				LocalDate laborDay = firstSeptember.plusDays(adjustment);
				return date.equals(laborDay);
			}
			case INDIGENOUS_PEOPLES_DAY: {
				// Columbus Day: Second Monday in October.
				LocalDate firstOctober = LocalDate.of(date.getYear(), 10, 1);
				int adjustment = (DayOfWeek.MONDAY.getValue() - firstOctober.getDayOfWeek().getValue() + 7) % 7;
				LocalDate firstMonday = firstOctober.plusDays(adjustment);
				LocalDate columbusDay = firstMonday.plusDays(7);
				return date.equals(columbusDay);
			}
			case VETERANS_DAY:
				return MonthDay.from(date).equals(MonthDay.of(11, 11));
			case THANKSGIVING: {
				// Thanksgiving: Fourth Thursday in November.
				LocalDate firstNovember = LocalDate.of(date.getYear(), 11, 1);
				int daysToThursday = (DayOfWeek.THURSDAY.getValue() - firstNovember.getDayOfWeek().getValue() + 7) % 7;
				LocalDate firstThursday = firstNovember.plusDays(daysToThursday);
				LocalDate thanksgiving = firstThursday.plusDays(21); // fourth Thursday
				return date.equals(thanksgiving);
			}
			case CHRISTMAS:
				return MonthDay.from(date).equals(MonthDay.of(12, 25));
			default:
				throw new IllegalArgumentException(format("Unsupported value: %s.%s", HolidayId.class.getSimpleName(), holidayId.name()));
		}
	}

	/**
	 * Returns the next business day after the given date.
	 *
	 * @param date                     the starting date
	 * @param businessHoursByDayOfWeek a map containing business hours for each day-of-week
	 * @param holidayIds               a set of Holiday enums representing days off
	 * @return the next LocalDate that is a business day
	 */
	@Nonnull
	private static LocalDate determineNextBusinessDay(@Nonnull LocalDate date,
																										@Nonnull Map<DayOfWeek, BusinessHours> businessHoursByDayOfWeek,
																										@Nonnull Set<HolidayId> holidayIds) {
		requireNonNull(date);
		requireNonNull(businessHoursByDayOfWeek);
		requireNonNull(holidayIds);

		LocalDate candidate = date.plusDays(1);

		while (!isBusinessDay(candidate, businessHoursByDayOfWeek, holidayIds))
			candidate = candidate.plusDays(1);

		return candidate;
	}

	/**
	 * Returns the earliest LocalDateTime that meets the required booking window,
	 * measured as a specified number of business hours from the given startDateTime.
	 * <p>
	 * This method advances through business days—taking into account the business operating hours and holidays—
	 * accumulating available business hours. If the current day's available hours are insufficient,
	 * the algorithm moves to the next business day, until the cumulative available business time is at least the specified threshold.
	 *
	 * @param startDateTime            the reference datetime (typically "now").
	 * @param businessHoursByDayOfWeek a map where each DayOfWeek is associated with the corresponding BusinessHours.
	 * @param holidayIds               a set of Holiday enums representing days when the business is closed.
	 * @param requiredBusinessHours    the minimum business hours from startDateTime that the booking time must be.
	 * @return the earliest LocalDateTime at which the booking can be scheduled.
	 */
	@Nonnull
	public static LocalDateTime determineEarliestBookingDateTime(@Nonnull LocalDateTime startDateTime,
																															 @Nonnull Map<DayOfWeek, BusinessHours> businessHoursByDayOfWeek,
																															 @Nonnull Set<HolidayId> holidayIds,
																															 @Nonnull Integer requiredBusinessHours) {
		requireNonNull(startDateTime);
		requireNonNull(businessHoursByDayOfWeek);
		requireNonNull(holidayIds);
		requireNonNull(requiredBusinessHours);

		double remainingBusinessHours = requiredBusinessHours;
		LocalDateTime candidate = startDateTime;

		// Continue until the accumulated available business hours meet the required threshold.
		while (remainingBusinessHours > 0) {
			// If the candidate day is not a business day, advance to the next business day at its opening time.
			if (!isBusinessDay(candidate.toLocalDate(), businessHoursByDayOfWeek, holidayIds)) {
				LocalDate nextBusinessDay = determineNextBusinessDay(candidate.toLocalDate(), businessHoursByDayOfWeek, holidayIds);
				candidate = LocalDateTime.of(nextBusinessDay, businessHoursByDayOfWeek.get(nextBusinessDay.getDayOfWeek()).getStartTime());
				continue;
			}

			BusinessHours businessHours = businessHoursByDayOfWeek.get(candidate.getDayOfWeek());
			LocalTime open = businessHours.getStartTime();
			LocalTime close = businessHours.getEndTime();
			LocalTime candidateTime = candidate.toLocalTime();

			// Adjust candidate time if before open or after business hours.
			if (candidateTime.isBefore(open)) {
				candidate = LocalDateTime.of(candidate.toLocalDate(), open);
				candidateTime = open;
			}

			if (!candidateTime.isBefore(close)) {
				// Current day is done; move to the next available business day.
				LocalDate nextBusinessDay = determineNextBusinessDay(candidate.toLocalDate(), businessHoursByDayOfWeek, holidayIds);
				candidate = LocalDateTime.of(nextBusinessDay, businessHoursByDayOfWeek.get(nextBusinessDay.getDayOfWeek()).getStartTime());
				continue;
			}

			// Calculate remaining business hours available on the candidate day.
			double availableToday = Duration.between(candidateTime, close).toMinutes() / 60.0;

			if (availableToday >= remainingBusinessHours) {
				// The booking can be scheduled on the candidate day.
				long minutesToAdd = (long) (remainingBusinessHours * 60);
				candidate = candidate.plusMinutes(minutesToAdd);
				remainingBusinessHours = 0;
			} else {
				// Use up the available hours today and move to the next business day.
				remainingBusinessHours -= availableToday;
				LocalDate nextBusinessDay = determineNextBusinessDay(candidate.toLocalDate(), businessHoursByDayOfWeek, holidayIds);
				candidate = LocalDateTime.of(nextBusinessDay, businessHoursByDayOfWeek.get(nextBusinessDay.getDayOfWeek()).getStartTime());
			}
		}

		return candidate;
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
}