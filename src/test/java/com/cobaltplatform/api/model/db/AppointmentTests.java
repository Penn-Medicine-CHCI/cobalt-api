/*
 * Copyright 2021 The University of Pennsylvania and Penn Medicine
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package com.cobaltplatform.api.model.db;

import com.cobaltplatform.api.model.db.AttendanceStatus.AttendanceStatusId;
import org.junit.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AppointmentTests {
	@Test
	public void inSessionUsesInclusiveStartAndExclusiveEnd() {
		ZoneId timeZone = ZoneId.of("America/New_York");
		LocalDateTime localStartTime = LocalDateTime.of(2026, 7, 12, 14, 30);
		LocalDateTime localEndTime = localStartTime.plusMinutes(30);
		Instant startTime = localStartTime.atZone(timeZone).toInstant();
		Instant endTime = localEndTime.atZone(timeZone).toInstant();
		Appointment appointment = appointment(localStartTime, localEndTime, timeZone);

		assertFalse(appointment.isInSessionAt(startTime.minusNanos(1)));
		assertTrue(appointment.isInSessionAt(startTime));
		assertTrue(appointment.isInSessionAt(endTime.minusNanos(1)));
		assertFalse(appointment.isInSessionAt(endTime));
	}

	@Test
	public void canceledAppointmentsAreNeverInSessionButRecordedAttendanceDoesNotEndSession() {
		ZoneId timeZone = ZoneId.of("America/New_York");
		LocalDateTime localStartTime = LocalDateTime.of(2026, 7, 12, 14, 30);
		Appointment appointment = appointment(localStartTime, localStartTime.plusMinutes(30), timeZone);
		Instant duringSession = localStartTime.plusMinutes(10).atZone(timeZone).toInstant();

		appointment.setAttendanceStatusId(AttendanceStatusId.ATTENDED);
		assertTrue(appointment.isInSessionAt(duringSession));

		appointment.setCanceled(true);
		assertFalse(appointment.isInSessionAt(duringSession));

		appointment.setCanceled(false);
		appointment.setCanceledForReschedule(true);
		assertFalse(appointment.isInSessionAt(duringSession));

		appointment.setCanceledForReschedule(false);
		appointment.setAttendanceStatusId(AttendanceStatusId.CANCELED);
		assertFalse(appointment.isInSessionAt(duringSession));
	}

	private Appointment appointment(LocalDateTime startTime, LocalDateTime endTime, ZoneId timeZone) {
		Appointment appointment = new Appointment();
		appointment.setStartTime(startTime);
		appointment.setEndTime(endTime);
		appointment.setTimeZone(timeZone);
		appointment.setAttendanceStatusId(AttendanceStatusId.UNKNOWN);
		appointment.setCanceled(false);
		appointment.setCanceledForReschedule(false);
		return appointment;
	}
}
