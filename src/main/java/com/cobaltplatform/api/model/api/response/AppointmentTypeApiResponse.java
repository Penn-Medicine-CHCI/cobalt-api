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

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;
import com.cobaltplatform.api.model.db.AppointmentType;
import com.cobaltplatform.api.model.db.SchedulingSystem.SchedulingSystemId;
import com.cobaltplatform.api.model.db.VisitType.VisitTypeId;
import com.cobaltplatform.api.util.Formatter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.HashMap;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class AppointmentTypeApiResponse {
	@Nonnull
	private final UUID appointmentTypeId;
	@Nonnull
	private final SchedulingSystemId schedulingSystemId;
	@Nonnull
	private final VisitTypeId visitTypeId;
	@Nullable
	private final Long acuityAppointmentTypeId;
	@Nullable
	private final String epicVisitTypeId;
	@Nullable
	private final String epicVisitTypeIdType;
	@Nonnull
	private final String name;
	@Nullable
	private final String description;
	@Nonnull
	private final Long durationInMinutes;
	@Nonnull
	private final String durationInMinutesDescription;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface AppointmentTypeApiResponseFactory {
		@Nonnull
		AppointmentTypeApiResponse create(@Nonnull AppointmentType appointmentType);
	}

	@AssistedInject
	public AppointmentTypeApiResponse(@Nonnull Formatter formatter,
																		@Nonnull Strings strings,
																		@Assisted @Nonnull AppointmentType appointmentType) {
		requireNonNull(formatter);
		requireNonNull(strings);

		this.appointmentTypeId = appointmentType.getAppointmentTypeId();
		this.schedulingSystemId = appointmentType.getSchedulingSystemId();
		this.visitTypeId = appointmentType.getVisitTypeId();
		this.acuityAppointmentTypeId = appointmentType.getAcuityAppointmentTypeId();
		this.epicVisitTypeId = appointmentType.getEpicVisitTypeId();
		this.epicVisitTypeIdType = appointmentType.getEpicVisitTypeIdType();
		this.name = appointmentType.getName();
		this.description = appointmentType.getDescription();
		this.durationInMinutes = appointmentType.getDurationInMinutes();
		this.durationInMinutesDescription = strings.get("{{duration}} minutes", new HashMap<String, Object>() {{
			put("duration", appointmentType.getDurationInMinutes());
		}});
	}

	@Nonnull
	public UUID getAppointmentTypeId() {
		return appointmentTypeId;
	}

	@Nonnull
	public SchedulingSystemId getSchedulingSystemId() {
		return schedulingSystemId;
	}

	@Nonnull
	public VisitTypeId getVisitTypeId() {
		return visitTypeId;
	}

	@Nullable
	public Long getAcuityAppointmentTypeId() {
		return acuityAppointmentTypeId;
	}

	@Nullable
	public String getEpicVisitTypeId() {
		return epicVisitTypeId;
	}

	@Nullable
	public String getEpicVisitTypeIdType() {
		return epicVisitTypeIdType;
	}

	@Nonnull
	public String getName() {
		return name;
	}

	@Nullable
	public String getDescription() {
		return description;
	}

	@Nonnull
	public Long getDurationInMinutes() {
		return durationInMinutes;
	}

	@Nonnull
	public String getDurationInMinutesDescription() {
		return durationInMinutesDescription;
	}
}