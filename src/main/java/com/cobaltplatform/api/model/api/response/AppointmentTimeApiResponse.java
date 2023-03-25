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

package com.cobaltplatform.api.model.api.response;

import com.cobaltplatform.api.model.db.AppointmentTime;
import com.cobaltplatform.api.model.db.AppointmentTime.AppointmentTimeId;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;

import java.time.LocalTime;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class AppointmentTimeApiResponse {
	@Nonnull
	private final AppointmentTimeId appointmentTimeId;
	@Nonnull
	private final String name;
	@Nonnull
	private final String description;
	@Nonnull
	private final LocalTime startTime;
	@Nonnull
	private final LocalTime endTime;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface AppointmentTimeApiResponseFactory {
		@Nonnull
		AppointmentTimeApiResponse create(@Nonnull AppointmentTime appointmentTime);
	}

	@AssistedInject
	public AppointmentTimeApiResponse(@Assisted @Nonnull AppointmentTime appointmentTime) {
		requireNonNull(appointmentTime);

		this.appointmentTimeId = appointmentTime.getAppointmentTimeId();
		this.name = appointmentTime.getName();
		this.description = appointmentTime.getDescription();
		this.startTime = appointmentTime.getStartTime();
		this.endTime = appointmentTime.getEndTime();
	}

	@Nonnull
	public AppointmentTimeId getAppointmentTimeId() {
		return appointmentTimeId;
	}

	@Nonnull
	public String getName() {
		return name;
	}

	@Nonnull
	public String getDescription() {
		return description;
	}

	@Nonnull
	public LocalTime getStartTime() {
		return startTime;
	}

	@Nonnull
	public LocalTime getEndTime() {
		return endTime;
	}
}