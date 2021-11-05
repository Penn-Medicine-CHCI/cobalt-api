package com.cobaltplatform.ic.backend.service;

import java.time.ZoneId;

class BusinessDayUtilTest {
	ZoneId zone = BusinessDayUtil.Department.COBALT.getTimeZone();
	BusinessDayUtil.Department department = BusinessDayUtil.Department.COBALT;
/*
	@Test
	void isWeekendBusinessHoursFalse() {
		// sunday at 11
		Instant now = LocalDate.of(2021, 3,14).atTime(11, 0).atZone(zone).toInstant();
		var isBusinessHours = new BusinessDayUtil().isInstantWithinBusinessHours(now, department);
		assertFalse(isBusinessHours);
	}

	@Test
	void isWeekendBusinessHoursTrue() {
		// sunday at 12
		Instant now = LocalDate.of(2021, 3,14).atTime(12, 0).atZone(zone).toInstant();
		var isBusinessHours = new BusinessDayUtil().isInstantWithinBusinessHours(now, department);
		assertTrue(isBusinessHours);
	}

	@Test
	void isHolidayBusinessHoursFalse() {
		// memorial day 2021, a monday, before open
		Instant now = LocalDate.of(2021, 5,31).atTime(11, 0).atZone(zone).toInstant();
		var isBusinessHours = new BusinessDayUtil().isInstantWithinBusinessHours(now, department);
		assertFalse(isBusinessHours);
	}

	@Test
	void isHolidayBusinessHoursTrue() {
		// memorial day 2021, a monday, before open
		Instant now = LocalDate.of(2021, 5,31).atTime(12, 0).atZone(zone).toInstant();
		var isBusinessHours = new BusinessDayUtil().isInstantWithinBusinessHours(now, department);
		assertTrue(isBusinessHours);
	}

	@Test
	void isDayBeforeWeekendHoursFalse() {
		// a friday at 4P
		Instant now = LocalDate.of(2021, 3,19).atTime(16, 0).atZone(zone).toInstant();
		var isBusinessHours = new BusinessDayUtil().isInstantWithinBusinessHours(now, department);
		assertFalse(isBusinessHours);
	}

	@Test
	void isDayBeforeWeekendHoursTrue() {
		// a friday at 2P
		Instant now = LocalDate.of(2021, 3,19).atTime(14, 0).atZone(zone).toInstant();
		var isBusinessHours = new BusinessDayUtil().isInstantWithinBusinessHours(now, department);
		assertTrue(isBusinessHours);
	}

	@Test
	void isMidWeekBusinessHoursTrue() {
		Instant now = LocalDate.of(2021, 3,17).atTime(12, 0).atZone(zone).toInstant();
		var isBusinessHours = new BusinessDayUtil().isInstantWithinBusinessHours(now, department);
		assertTrue(isBusinessHours);
	}

	@Test
	void isMartinLutherKingDayAHoliday() {
		Instant now = LocalDate.of(2021, 1,18).atTime(11, 0).atZone(zone).toInstant();
		var isBusinessHours = new BusinessDayUtil().isInstantWithinBusinessHours(now, department);
		assertFalse(isBusinessHours);
	}
 */
}