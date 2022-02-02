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

import com.cobaltplatform.api.model.db.AppointmentReasonType.AppointmentReasonTypeId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.UUID;

/**
 * @author Transmogrify LLC.
 */
@NotThreadSafe
public class AppointmentReason {
	@Nullable
	private UUID appointmentReasonId;
	@Nullable
	private InstitutionId institutionId;
	@Nullable
	private AppointmentReasonTypeId appointmentReasonTypeId;
	@Nullable
	private String description;
	@Nullable
	private String color;
	@Nullable
	private Integer displayOrder;

	@Nullable
	public UUID getAppointmentReasonId() {
		return appointmentReasonId;
	}

	public void setAppointmentReasonId(@Nullable UUID appointmentReasonId) {
		this.appointmentReasonId = appointmentReasonId;
	}

	@Nullable
	public InstitutionId getInstitutionId() {
		return institutionId;
	}

	public void setInstitutionId(@Nullable InstitutionId institutionId) {
		this.institutionId = institutionId;
	}

	@Nullable
	public AppointmentReasonTypeId getAppointmentReasonTypeId() {
		return appointmentReasonTypeId;
	}

	public void setAppointmentReasonTypeId(@Nullable AppointmentReasonTypeId appointmentReasonTypeId) {
		this.appointmentReasonTypeId = appointmentReasonTypeId;
	}

	@Nullable
	public String getDescription() {
		return description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
	}

	@Nullable
	public String getColor() {
		return color;
	}

	public void setColor(@Nullable String color) {
		this.color = color;
	}

	@Nullable
	public Integer getDisplayOrder() {
		return displayOrder;
	}

	public void setDisplayOrder(@Nullable Integer displayOrder) {
		this.displayOrder = displayOrder;
	}
}