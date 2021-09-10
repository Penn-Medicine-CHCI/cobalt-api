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

import com.cobaltplatform.api.model.db.AppointmentReason;
import com.cobaltplatform.api.model.db.AppointmentReasonType.AppointmentReasonTypeId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class AppointmentReasonApiResponse {
	@Nonnull
	private final UUID appointmentReasonId;
	@Nonnull
	private final AppointmentReasonTypeId appointmentReasonTypeId;
	@Nonnull
	private final InstitutionId institutionId;
	@Nonnull
	private final String description;
	@Nullable
	private final String color;

	public AppointmentReasonApiResponse(@Nonnull AppointmentReason appointmentReason) {
		requireNonNull(appointmentReason);

		this.appointmentReasonId = appointmentReason.getAppointmentReasonId();
		this.appointmentReasonTypeId = appointmentReason.getAppointmentReasonTypeId();
		this.institutionId = appointmentReason.getInstitutionId();
		this.description = appointmentReason.getDescription();
		this.color = appointmentReason.getColor();
	}

	@Nonnull
	public UUID getAppointmentReasonId() {
		return appointmentReasonId;
	}

	@Nonnull
	public AppointmentReasonTypeId getAppointmentReasonTypeId() {
		return appointmentReasonTypeId;
	}

	@Nonnull
	public InstitutionId getInstitutionId() {
		return institutionId;
	}

	@Nonnull
	public String getDescription() {
		return description;
	}

	@Nullable
	public String getColor() {
		return color;
	}
}