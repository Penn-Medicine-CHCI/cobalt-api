package com.cobaltplatform.ic.backend.service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;

import com.cobaltplatform.ic.backend.config.EpicConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BusinessDayUtil {
	private static final Logger logger = LoggerFactory.getLogger(BusinessDayUtil.class);

	public enum Department {
		COBALT(ZoneId.of("America/New_York"));

		ZoneId timeZone;

		Department(ZoneId timeZone) {
			this.timeZone = timeZone;
		}

		public ZoneId getTimeZone() {
			return timeZone;
		}
	}

	public boolean isInstantWithinBusinessHours(Instant now, Department department) {
		// TODO: re-enable in prod
		return EpicConfig.isFakeSsoSupported();
		//return now.equals(determineNextInstantWithinBusinessHours(now, department));
	}

	private Instant determineNextInstantWithinBusinessHours(Instant now, Department department) {
		if (department == Department.COBALT) {
			// Cobalt rules: 9-5 M-F, skip American holidays, Eastern timezone
			LocalTime afterWeekendStartTime = LocalTime.of(12, 0); // what time to start mondays
			LocalTime preWeekendEndTime = LocalTime.of(15, 0); // what time to end on fridays
			LocalDateTime nowDateTime = LocalDateTime.ofInstant(now, department.getTimeZone());

			Instant tomorrow = LocalDateTime.ofInstant(now, department.getTimeZone()).plusDays(1).atZone(department.getTimeZone()).toInstant();
			Instant tomorrowAtOpen = nowDateTime.toLocalDate().plusDays(1).atTime(afterWeekendStartTime).atZone(department.getTimeZone()).toInstant();

			if (isInstantOnWeekendOrHoliday(now, department)) {
				// IC opens at noon on sunday on a typical weekend, or noon the last day of a holiday
				if (isInstantOnWeekendOrHoliday(tomorrow, department)) {
					// if today and tomorrow are holiday move forward one day and recurse
					return determineNextInstantWithinBusinessHours(tomorrowAtOpen, department);
				}
				else if (nowDateTime.toLocalTime().isBefore(afterWeekendStartTime)) {
					// if today is holiday or weekend but tomorrow is not, and its before open, do today at "open" time
					return nowDateTime.toLocalDate().atTime(afterWeekendStartTime).atZone(department.getTimeZone()).toInstant();
				}
				else {
					// its last day of weekend or holiday and business is at open hours
					return now;
				}
			} else if (
					nowDateTime.toLocalTime().isAfter(preWeekendEndTime)
					&& (
							isInstantOnHoliday(tomorrow, department)
							|| isInstantOnWeekend(tomorrow, department)
					)) {
				// if tomorrow is weekend or holiday, and its after close, move 1 days forward
				// e.g if Wednesday is 4th of july, we want to return Wednesday at noon, but setting it to noon is handled elsewhere
				return nowDateTime.plusDays(1).atZone(department.getTimeZone()).toInstant();
			}
			else {
				// today and tomorrow are not weekends/holidays
				return now;
			}
		} else {
			throw new UnsupportedOperationException();
		}
	}

	private boolean isInstantOnWeekendOrHoliday(Instant now, Department department) {
		return isInstantOnWeekend(now, department) || isInstantOnHoliday(now, department);
	}

	private boolean isInstantOnWeekend(Instant now, Department department) {
		LocalDateTime nowDateTime = LocalDateTime.ofInstant(now, department.getTimeZone());
		return nowDateTime.getDayOfWeek().equals(DayOfWeek.SATURDAY) || nowDateTime.getDayOfWeek().equals(DayOfWeek.SUNDAY);
	}

	private boolean isInstantOnHoliday(Instant now, Department department) {
		LocalDateTime nowDateTime = LocalDateTime.ofInstant(now, department.getTimeZone());
		return holidaysForDepartmentYear(nowDateTime.getYear()).contains(nowDateTime.toLocalDate());
	}

	/*
	 * Based on a year, this will compute the actual dates of
	 *
	 * Holidays Accounted For:
	 * New Year's Day
	 * Martin Luther King Jr. Day
	 * President's Day
	 * Memorial Day
	 * Independence Day
	 * Labor Day
	 * Columbus Day
	 * Veterans Day
	 * Thanksgiving Day
	 * Christmas Day
	 *
	 */
	private static Set<LocalDate> holidaysForDepartmentYear(int year) {
		HashSet<LocalDate> offLimitDates = new HashSet<>();

		//Add in the static dates for the year.
		//New years day
		offLimitDates.add(LocalDate.of(year, 1, 1));

		//Independence Day
		offLimitDates.add(LocalDate.of(year, 7, 4));

		//Christmas
		offLimitDates.add(LocalDate.of(year, 12, 25));

		//Now deal with floating holidays.
		//Martin Luther King Day
		offLimitDates.add(calculateFloatingHoliday(3, 1, year, 1));

		//Memorial Day
		offLimitDates.add(calculateFloatingHoliday(0, 1, year, 5));

		//Labor Day
		offLimitDates.add(calculateFloatingHoliday(1, 1, year, 9));

		//Thanksgiving Day
		offLimitDates.add(calculateFloatingHoliday(4, 4, year, 11));

		return offLimitDates;
	}


	/**
	 * This method will take in the various parameters and return a Date objet
	 * that represents that value.
	 * <p>
	 * Ex. To get Martin Luther Kings BDay, which is the 3rd Monday of January,
	 * the method call would be:
	 * <p>
	 * calculateFloatingHoliday(3, 1, year, 1);
	 * <p>
	 * Reference material can be found at:
	 * http://michaelthompson.org/technikos/holidays.php#MemorialDay
	 *
	 * @param nth       0 for Last, 1 for 1st, 2 for 2nd, etc.
	 * @param dayOfWeek Use 1 for monday, 7 for sunday
	 * @param year      int
	 * @param month     Use 1 - 12
	 * @return date
	 */
	private static LocalDate calculateFloatingHoliday(int nth, int dayOfWeek, int year, int month) {

		//Determine what the very earliest day this could occur.
		//If the value was 0 for the nth parameter, increment to the following
		//month so that it can be subtracted after.
		var initialDate = LocalDate.of(year, month + ((nth <= 0) ? 1 : 0), 1);

		//Figure out which day of the week that this "earliest" could occur on
		//and then determine what the offset is for our day that we actually need.
		var baseDayOfWeek = initialDate.getDayOfWeek().getValue();
		int fwd = dayOfWeek - baseDayOfWeek;

		//Based on the offset and the nth parameter, we are able to determine the offset of days and then
		//adjust our base date.
		return initialDate.plusDays((fwd + (nth - (fwd >= 0 ? 1 : 0)) * 7L));
	}
}