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

import com.cobaltplatform.api.model.db.FontSize;
import com.cobaltplatform.api.model.db.FontSize.FontSizeId;
import com.cobaltplatform.api.model.db.SchedulingSystem.SchedulingSystemId;
import com.cobaltplatform.api.model.db.VisitType.VisitTypeId;
import com.cobaltplatform.api.model.db.assessment.QuestionType.QuestionTypeId;

import javax.annotation.Nonnull;
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
	@Nonnull
	private VisitTypeId visitTypeId;
	@Nonnull
	private String name;
	@Nonnull
	private Long durationInMinutes;
	@Nonnull
	private Integer hexColor;
	@Nonnull
	private List<CreatePatientIntakeQuestionRequest> patientIntakeQuestions;
	@Nonnull
	private List<CreateScreeningQuestionRequest> screeningIntakeQuestions;

	@NotThreadSafe
	public static class CreatePatientIntakeQuestionRequest {
		@Nullable
		private String question;
		@Nullable
		private QuestionTypeId questionTypeId;
		@Nullable
		private FontSizeId fontSizeId;

		@Nullable
		public String getQuestion() {
			return question;
		}

		public void setQuestion(@Nullable String question) {
			this.question = question;
		}

		@Nullable
		public QuestionTypeId getQuestionTypeId() {
			return questionTypeId;
		}

		public void setQuestionTypeId(@Nullable QuestionTypeId questionTypeId) {
			this.questionTypeId = questionTypeId;
		}

		@Nullable
		public FontSizeId getFontSizeId() {
			return fontSizeId;
		}

		public void setFontSizeId(@Nullable FontSizeId fontSizeId) {
			this.fontSizeId = fontSizeId;
		}
	}

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

	@Nonnull
	public VisitTypeId getVisitTypeId() {
		return visitTypeId;
	}

	public void setVisitTypeId(@Nonnull VisitTypeId visitTypeId) {
		this.visitTypeId = visitTypeId;
	}

	@Nonnull
	public String getName() {
		return name;
	}

	public void setName(@Nonnull String name) {
		this.name = name;
	}

	@Nonnull
	public Long getDurationInMinutes() {
		return durationInMinutes;
	}

	public void setDurationInMinutes(@Nonnull Long durationInMinutes) {
		this.durationInMinutes = durationInMinutes;
	}

	@Nonnull
	public Integer getHexColor() {
		return hexColor;
	}

	public void setHexColor(@Nonnull Integer hexColor) {
		this.hexColor = hexColor;
	}

	@Nonnull
	public List<CreatePatientIntakeQuestionRequest> getPatientIntakeQuestions() {
		return patientIntakeQuestions;
	}

	public void setPatientIntakeQuestions(@Nonnull List<CreatePatientIntakeQuestionRequest> patientIntakeQuestions) {
		this.patientIntakeQuestions = patientIntakeQuestions;
	}

	@Nonnull
	public List<CreateScreeningQuestionRequest> getScreeningIntakeQuestions() {
		return screeningIntakeQuestions;
	}

	public void setScreeningIntakeQuestions(@Nonnull List<CreateScreeningQuestionRequest> screeningIntakeQuestions) {
		this.screeningIntakeQuestions = screeningIntakeQuestions;
	}
}
