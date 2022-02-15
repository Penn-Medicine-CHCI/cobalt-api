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

import com.cobaltplatform.api.model.db.SchedulingSystem.SchedulingSystemId;
import com.cobaltplatform.api.model.db.VisitType.VisitTypeId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class CreateAppointmentTypeRequest {
	@Nullable
	private UUID providerId;
	@Nullable
	private SchedulingSystemId schedulingSystemId;
	@Nullable
	private VisitTypeId visitTypeId;
	@Nullable
	private String name;
	@Nullable
	private String description;
	@Nullable
	private Long durationInMinutes;
	@Nullable
	private Integer hexColor;
	@Nullable
	private List<CreatePatientIntakeQuestionRequest> patientIntakeQuestions;
	@Nullable
	private List<CreateScreeningQuestionRequest> screeningQuestions;

	@Nullable
	public UUID getProviderId() {
		return providerId;
	}

	public void setProviderId(@Nullable UUID providerId) {
		this.providerId = providerId;
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
	public Integer getHexColor() {
		return hexColor;
	}

	public void setHexColor(@Nullable Integer hexColor) {
		this.hexColor = hexColor;
	}

	@Nullable
	public List<CreatePatientIntakeQuestionRequest> getPatientIntakeQuestions() {
		return patientIntakeQuestions;
	}

	public void setPatientIntakeQuestions(@Nullable List<CreatePatientIntakeQuestionRequest> patientIntakeQuestions) {
		this.patientIntakeQuestions = patientIntakeQuestions;
	}

	@Nullable
	public List<CreateScreeningQuestionRequest> getScreeningQuestions() {
		return screeningQuestions;
	}

	public void setScreeningQuestions(@Nullable List<CreateScreeningQuestionRequest> screeningQuestions) {
		this.screeningQuestions = screeningQuestions;
	}
}
