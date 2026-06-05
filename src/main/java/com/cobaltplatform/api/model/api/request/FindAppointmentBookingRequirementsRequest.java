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

package com.cobaltplatform.api.model.api.request;

import com.cobaltplatform.api.model.api.response.ProviderListDetailsApiResponse.ProviderAppointmentSelectionTypeId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class FindAppointmentBookingRequirementsRequest {
	@Nullable
	private UUID accountId;
	@Nullable
	private UUID providerId;
	@Nullable
	private UUID appointmentTypeId;
	@Nullable
	private ProviderAppointmentSelectionTypeId appointmentSelectionTypeId;
	@Nullable
	private LocalDate date;
	@Nullable
	private LocalTime time;
	@Nullable
	private UUID epicDepartmentId;
	@Nullable
	private String epicAppointmentFhirId;

	@Nullable
	public UUID getAccountId() {
		return this.accountId;
	}

	public void setAccountId(@Nullable UUID accountId) {
		this.accountId = accountId;
	}

	@Nullable
	public UUID getProviderId() {
		return this.providerId;
	}

	public void setProviderId(@Nullable UUID providerId) {
		this.providerId = providerId;
	}

	@Nullable
	public UUID getAppointmentTypeId() {
		return this.appointmentTypeId;
	}

	public void setAppointmentTypeId(@Nullable UUID appointmentTypeId) {
		this.appointmentTypeId = appointmentTypeId;
	}

	@Nullable
	public ProviderAppointmentSelectionTypeId getAppointmentSelectionTypeId() {
		return this.appointmentSelectionTypeId;
	}

	public void setAppointmentSelectionTypeId(@Nullable ProviderAppointmentSelectionTypeId appointmentSelectionTypeId) {
		this.appointmentSelectionTypeId = appointmentSelectionTypeId;
	}

	@Nullable
	public LocalDate getDate() {
		return this.date;
	}

	public void setDate(@Nullable LocalDate date) {
		this.date = date;
	}

	@Nullable
	public LocalTime getTime() {
		return this.time;
	}

	public void setTime(@Nullable LocalTime time) {
		this.time = time;
	}

	@Nullable
	public UUID getEpicDepartmentId() {
		return this.epicDepartmentId;
	}

	public void setEpicDepartmentId(@Nullable UUID epicDepartmentId) {
		this.epicDepartmentId = epicDepartmentId;
	}

	@Nullable
	public String getEpicAppointmentFhirId() {
		return this.epicAppointmentFhirId;
	}

	public void setEpicAppointmentFhirId(@Nullable String epicAppointmentFhirId) {
		this.epicAppointmentFhirId = epicAppointmentFhirId;
	}
}
