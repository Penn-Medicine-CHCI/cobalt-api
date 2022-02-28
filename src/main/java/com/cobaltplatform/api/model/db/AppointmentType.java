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

import com.cobaltplatform.api.model.db.SchedulingSystem.SchedulingSystemId;
import com.cobaltplatform.api.model.db.VisitType.VisitTypeId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.UUID;

import static java.lang.String.format;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class AppointmentType {
	@Nullable
	private UUID appointmentTypeId;
	@Nullable
	private SchedulingSystemId schedulingSystemId;
	@Nullable
	private VisitTypeId visitTypeId;
	@Nullable
	private Long acuityAppointmentTypeId;
	@Nullable
	private String epicVisitTypeId;
	@Nullable
	private String epicVisitTypeIdType;
	@Nullable
	private String name;
	@Nullable
	private String description;
	@Nullable
	private Long durationInMinutes;
	@Nullable
	private Integer hexColor;
	@Nullable
	private Boolean deleted;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	// Joined in by v_appointment_type
	@Nullable
	private UUID assessmentId;

	@Override
	public String toString() {
		return format("%s{%s (%s), scheduling system %s, %s minutes}", AppointmentType.class.getSimpleName(), getName(),
				getVisitTypeId(), getSchedulingSystemId(), getDurationInMinutes());
	}

	@Nullable
	public UUID getAppointmentTypeId() {
		return appointmentTypeId;
	}

	public void setAppointmentTypeId(@Nullable UUID appointmentTypeId) {
		this.appointmentTypeId = appointmentTypeId;
	}

	@Nullable
	public Long getAcuityAppointmentTypeId() {
		return acuityAppointmentTypeId;
	}

	public void setAcuityAppointmentTypeId(@Nullable Long acuityAppointmentTypeId) {
		this.acuityAppointmentTypeId = acuityAppointmentTypeId;
	}

	@Nullable
	public SchedulingSystemId getSchedulingSystemId() {
		return schedulingSystemId;
	}

	public void setSchedulingSystemId(@Nullable SchedulingSystemId schedulingSystemId) {
		this.schedulingSystemId = schedulingSystemId;
	}

	@Nullable
	public VisitTypeId getVisitTypeId() {
		return visitTypeId;
	}

	public void setVisitTypeId(@Nullable VisitTypeId visitTypeId) {
		this.visitTypeId = visitTypeId;
	}

	@Nullable
	public String getEpicVisitTypeId() {
		return epicVisitTypeId;
	}

	public void setEpicVisitTypeId(@Nullable String epicVisitTypeId) {
		this.epicVisitTypeId = epicVisitTypeId;
	}

	@Nullable
	public String getEpicVisitTypeIdType() {
		return epicVisitTypeIdType;
	}

	public void setEpicVisitTypeIdType(@Nullable String epicVisitTypeIdType) {
		this.epicVisitTypeIdType = epicVisitTypeIdType;
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

	@Nullable
	public Integer getHexColor() {
		return hexColor;
	}

	public void setHexColor(@Nullable Integer hexColor) {
		this.hexColor = hexColor;
	}

	@Nullable
	public UUID getAssessmentId() {
		return assessmentId;
	}

	public void setAssessmentId(@Nullable UUID assessmentId) {
		this.assessmentId = assessmentId;
	}
}