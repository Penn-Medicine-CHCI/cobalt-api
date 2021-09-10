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

package com.cobaltplatform.api.integration.acuity.model;

import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class AcuityCheckTime {
	@Nullable
	@SerializedName("appointmentTypeID")
	private Long appointmentTypeId;
	@Nullable
	@SerializedName("calendarID")
	private Long calendarId;
	@Nullable
	private String datetime;
	@Nullable
	private Boolean valid;

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

	@Nullable
	public String getDatetime() {
		return datetime;
	}

	public void setDatetime(@Nullable String datetime) {
		this.datetime = datetime;
	}

	@Nullable
	public Boolean getValid() {
		return valid;
	}

	public void setValid(@Nullable Boolean valid) {
		this.valid = valid;
	}
}
