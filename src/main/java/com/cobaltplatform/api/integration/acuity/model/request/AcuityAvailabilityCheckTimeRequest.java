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

package com.cobaltplatform.api.integration.acuity.model.request;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class AcuityAvailabilityCheckTimeRequest {
	@Nullable
	private LocalDateTime datetime;
	@Nullable
	private ZoneId timeZone;
	@Nullable
	private Long appointmentTypeId;
	@Nullable
	private Long calendarId;

	@Nullable
	public LocalDateTime getDatetime() {
		return datetime;
	}

	public void setDatetime(@Nullable LocalDateTime datetime) {
		this.datetime = datetime;
	}

	@Nullable
	public ZoneId getTimeZone() {
		return timeZone;
	}

	public void setTimeZone(@Nullable ZoneId timeZone) {
		this.timeZone = timeZone;
	}

	@Nullable
	public Long getAppointmentTypeId() {
		return appointmentTypeId;
	}

	public void setAppointmentTypeId(@Nullable Long appointmentTypeId) {
		this.appointmentTypeId = appointmentTypeId;
	}

	@Nullable
	public Long getCalendarId() {
		return calendarId;
	}

	public void setCalendarId(@Nullable Long calendarId) {
		this.calendarId = calendarId;
	}
}
