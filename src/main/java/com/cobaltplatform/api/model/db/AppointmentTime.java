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

package com.cobaltplatform.api.model.db;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.time.LocalTime;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class AppointmentTime {
	@Nullable
	private AppointmentTimeId appointmentTimeId;
	@Nullable
	private String name;
	@Nullable
	private String description;
	@Nullable
	private LocalTime startTime;
	@Nullable
	private LocalTime endTime;
	@Nullable
	private Integer displayOrder;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	public enum AppointmentTimeId {
		EARLY_MORNING,
		MORNING,
		AFTERNOON,
		EVENING
	}

	@Nullable
	public AppointmentTimeId getAppointmentTimeId() {
		return appointmentTimeId;
	}

	public void setAppointmentTimeId(@Nullable AppointmentTimeId appointmentTimeId) {
		this.appointmentTimeId = appointmentTimeId;
	}

	@Nullable
	public String getName() {
		return name;
	}

	public void setName(@Nullable String name) {
		this.name = name;
	}

	@Nullable
	public String getDescription() {
		return description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
	}

	@Nullable
	public LocalTime getStartTime() {
		return startTime;
	}

	public void setStartTime(@Nullable LocalTime startTime) {
		this.startTime = startTime;
	}

	@Nullable
	public LocalTime getEndTime() {
		return endTime;
	}

	public void setEndTime(@Nullable LocalTime endTime) {
		this.endTime = endTime;
	}

	@Nullable
	public Integer getDisplayOrder() {
		return displayOrder;
	}

	public void setDisplayOrder(@Nullable Integer displayOrder) {
		this.displayOrder = displayOrder;
	}

	@Nullable
	public Instant getCreated() {
		return created;
	}

	public void setCreated(@Nullable Instant created) {
		this.created = created;
	}

	@Nullable
	public Instant getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(@Nullable Instant lastUpdated) {
		this.lastUpdated = lastUpdated;
	}
}