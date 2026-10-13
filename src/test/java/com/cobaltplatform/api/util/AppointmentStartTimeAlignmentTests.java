/*
 * Copyright 2021 The University of Pennsylvania and Penn Medicine
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package com.cobaltplatform.api.util;

import org.junit.Test;

import java.time.LocalTime;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class AppointmentStartTimeAlignmentTests {
	@Test
	public void absentPolicyPreservesAllStartTimes() {
		assertTrue(AppointmentStartTimeAlignment.isAllowed(LocalTime.of(13, 30), null));
		assertTrue(AppointmentStartTimeAlignment.isAllowed(LocalTime.of(13, 30, 17, 1), null));
	}

	@Test
	public void hourlyPolicyAllowsDibbleWholeHourStartsAndRejectsHalfHours() {
		for (int hour : new int[]{13, 14, 15}) {
			assertTrue(AppointmentStartTimeAlignment.isAllowed(LocalTime.of(hour, 0), 60));
			assertFalse(AppointmentStartTimeAlignment.isAllowed(LocalTime.of(hour, 30), 60));
		}
	}

	@Test
	public void quarterHourPolicyAllowsQuarterHours() {
		assertTrue(AppointmentStartTimeAlignment.isAllowed(LocalTime.of(13, 15), 15));
		assertTrue(AppointmentStartTimeAlignment.isAllowed(LocalTime.of(13, 30), 15));
		assertTrue(AppointmentStartTimeAlignment.isAllowed(LocalTime.of(13, 45), 15));
		assertFalse(AppointmentStartTimeAlignment.isAllowed(LocalTime.of(13, 20), 15));
	}

	@Test
	public void intervalsAreAnchoredAtMidnightRatherThanFirstAvailableStart() {
		assertTrue(AppointmentStartTimeAlignment.isAllowed(LocalTime.MIDNIGHT, 90));
		assertTrue(AppointmentStartTimeAlignment.isAllowed(LocalTime.of(1, 30), 90));
		assertTrue(AppointmentStartTimeAlignment.isAllowed(LocalTime.of(13, 30), 90));
		assertFalse(AppointmentStartTimeAlignment.isAllowed(LocalTime.of(13, 0), 90));
	}

	@Test
	public void configuredPoliciesRequireWholeMinuteStarts() {
		assertFalse(AppointmentStartTimeAlignment.isAllowed(LocalTime.of(13, 0, 1), 60));
		assertFalse(AppointmentStartTimeAlignment.isAllowed(LocalTime.of(13, 0, 0, 1), 60));
		assertFalse(AppointmentStartTimeAlignment.isAllowed(LocalTime.of(13, 0, 1), 1));
	}

	@Test
	public void intervalBoundsAreSupported() {
		assertTrue(AppointmentStartTimeAlignment.isAllowed(LocalTime.of(23, 59), 1));
		assertTrue(AppointmentStartTimeAlignment.isAllowed(LocalTime.MIDNIGHT, 1440));
		assertFalse(AppointmentStartTimeAlignment.isAllowed(LocalTime.of(1, 0), 1440));
	}

	@Test
	public void invalidConfiguredIntervalsFailExplicitly() {
		for (Integer interval : new Integer[]{-1, 0, 1441, Integer.MAX_VALUE})
			assertThrows(IllegalArgumentException.class,
					() -> AppointmentStartTimeAlignment.isAllowed(LocalTime.of(13, 0), interval));
	}
}
