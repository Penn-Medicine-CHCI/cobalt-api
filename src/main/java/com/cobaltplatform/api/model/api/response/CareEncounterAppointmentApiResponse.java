/*
 * Copyright 2021 The University of Pennsylvania and Penn Medicine
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package com.cobaltplatform.api.model.api.response;

import com.cobaltplatform.api.model.db.Appointment;
import com.cobaltplatform.api.model.db.AttendanceStatus.AttendanceStatusId;
import com.cobaltplatform.api.util.Formatter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * Lightweight appointment representation used by Care Encounter list responses.
 */
@ThreadSafe
public class CareEncounterAppointmentApiResponse {
	@Nonnull
	private final UUID appointmentId;
	@Nonnull
	private final UUID providerId;
	@Nullable
	private final UUID appointmentTypeId;
	@Nonnull
	private final AttendanceStatusId attendanceStatusId;
	@Nonnull
	private final Boolean inSession;
	@Nonnull
	private final String title;
	@Nonnull
	private final Instant startTime;
	@Nonnull
	private final String startTimeDescription;
	@Nonnull
	private final Instant endTime;
	@Nonnull
	private final String endTimeDescription;
	@Nonnull
	private final ZoneId timeZone;
	@Nonnull
	private final Boolean canceledForReschedule;
	@Nullable
	private final Boolean canceled;

	public CareEncounterAppointmentApiResponse(@Nonnull Formatter formatter,
																			 @Nonnull Appointment appointment) {
		requireNonNull(formatter);
		requireNonNull(appointment);

		this.appointmentId = appointment.getAppointmentId();
		this.providerId = appointment.getProviderId();
		this.appointmentTypeId = appointment.getAppointmentTypeId();
		this.attendanceStatusId = appointment.getAttendanceStatusId();
		this.inSession = appointment.isInSessionAt(Instant.now());
		this.title = appointment.getTitle();
		this.startTime = appointment.getStartTime().atZone(appointment.getTimeZone()).toInstant();
		this.startTimeDescription = formatter.formatTimestamp(this.startTime);
		this.endTime = appointment.getEndTime().atZone(appointment.getTimeZone()).toInstant();
		this.endTimeDescription = formatter.formatTimestamp(this.endTime);
		this.timeZone = appointment.getTimeZone();
		this.canceledForReschedule = appointment.getCanceledForReschedule() == null
				? false
				: appointment.getCanceledForReschedule();
		this.canceled = appointment.getCanceled();
	}

	@Nonnull public UUID getAppointmentId() { return this.appointmentId; }
	@Nonnull public UUID getProviderId() { return this.providerId; }
	@Nullable public UUID getAppointmentTypeId() { return this.appointmentTypeId; }
	@Nonnull public AttendanceStatusId getAttendanceStatusId() { return this.attendanceStatusId; }
	@Nonnull public Boolean getInSession() { return this.inSession; }
	@Nonnull public String getTitle() { return this.title; }
	@Nonnull public Instant getStartTime() { return this.startTime; }
	@Nonnull public String getStartTimeDescription() { return this.startTimeDescription; }
	@Nonnull public Instant getEndTime() { return this.endTime; }
	@Nonnull public String getEndTimeDescription() { return this.endTimeDescription; }
	@Nonnull public ZoneId getTimeZone() { return this.timeZone; }
	@Nonnull public Boolean getCanceledForReschedule() { return this.canceledForReschedule; }
	@Nullable public Boolean getCanceled() { return this.canceled; }
}
