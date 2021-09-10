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

import javax.annotation.Nullable;

/**
 * @author Transmogrify, LLC.
 */
public class CreateAcuityAppointmentTypeRequest {
	@Nullable
	private Long acuityAppointmentTypeId;
	@Nullable
	private String name;
	@Nullable
	private String description;
	@Nullable
	private Long durationInMinutes;
	@Nullable
	private Boolean deleted;

	@Nullable
	public Long getAcuityAppointmentTypeId() {
		return acuityAppointmentTypeId;
	}

	public void setAcuityAppointmentTypeId(@Nullable Long acuityAppointmentTypeId) {
		this.acuityAppointmentTypeId = acuityAppointmentTypeId;
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
	public Long getDurationInMinutes() {
		return durationInMinutes;
	}

	public void setDurationInMinutes(@Nullable Long durationInMinutes) {
		this.durationInMinutes = durationInMinutes;
	}

	@Nullable
	public Boolean getDeleted() {
		return deleted;
	}

	public void setDeleted(@Nullable Boolean deleted) {
		this.deleted = deleted;
	}
}
