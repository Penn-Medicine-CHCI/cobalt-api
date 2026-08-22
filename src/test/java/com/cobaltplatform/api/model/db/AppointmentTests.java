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

import com.cobaltplatform.api.model.db.Appointment.AppointmentTimeStatusId;
import org.junit.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.Assert.assertEquals;

public class AppointmentTests {
	@Test
	public void appointmentTimeStatusUsesInclusiveStartAndExclusiveEnd() {
		ZoneId timeZone = ZoneId.of("America/New_York");
		LocalDateTime localStartTime = LocalDateTime.of(2026, 7, 12, 14, 30);
		LocalDateTime localEndTime = localStartTime.plusMinutes(30);
		Instant startTime = localStartTime.atZone(timeZone).toInstant();
		Instant endTime = localEndTime.atZone(timeZone).toInstant();
		Appointment appointment = appointment(localStartTime, localEndTime, timeZone);

		assertEquals(AppointmentTimeStatusId.SCHEDULED,
				appointment.getAppointmentTimeStatusIdAt(startTime.minusNanos(1)));
		assertEquals(AppointmentTimeStatusId.IN_SESSION,
				appointment.getAppointmentTimeStatusIdAt(startTime));
		assertEquals(AppointmentTimeStatusId.IN_SESSION,
				appointment.getAppointmentTimeStatusIdAt(endTime.minusNanos(1)));
		assertEquals(AppointmentTimeStatusId.PASSED,
				appointment.getAppointmentTimeStatusIdAt(endTime));
	}

	private Appointment appointment(LocalDateTime startTime, LocalDateTime endTime, ZoneId timeZone) {
		Appointment appointment = new Appointment();
		appointment.setStartTime(startTime);
		appointment.setEndTime(endTime);
		appointment.setTimeZone(timeZone);
		appointment.setCanceled(false);
		appointment.setCanceledForReschedule(false);
		return appointment;
	}
}
