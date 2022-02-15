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

import com.cobaltplatform.api.model.db.AppointmentType;
import com.cobaltplatform.api.model.db.FontSize.FontSizeId;
import com.cobaltplatform.api.model.db.Question;
import com.cobaltplatform.api.model.db.QuestionContentHint.QuestionContentHintId;
import com.cobaltplatform.api.model.db.QuestionType.QuestionTypeId;
import com.cobaltplatform.api.model.db.SchedulingSystem.SchedulingSystemId;
import com.cobaltplatform.api.model.db.VisitType.VisitTypeId;
import com.cobaltplatform.api.service.AssessmentService;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static java.lang.String.format;
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
	@Nonnull
	private final Integer hexColor;
	@Nonnull
	private final String hexColorDescription;
	@Nullable
	private final UUID assessmentId;
	@Nullable
	private List<PatientIntakeQuestionApiResponse> patientIntakeQuestions;
	@Nullable
	private List<ScreeningQuestionApiResponse> screeningQuestions;

	public enum AppointmentTypeApiResponseSupplement {
		EVERYTHING, ASSESSMENT
	}

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface AppointmentTypeApiResponseFactory {
		@Nonnull
		AppointmentTypeApiResponse create(@Nonnull AppointmentType appointmentType);

		@Nonnull
		AppointmentTypeApiResponse create(@Nonnull AppointmentType appointmentType,
																			@Nonnull Set<AppointmentTypeApiResponseSupplement> supplements);
	}

	@AssistedInject
	public AppointmentTypeApiResponse(@Nonnull AssessmentService assessmentService,
																		@Nonnull Formatter formatter,
																		@Nonnull Strings strings,
																		@Assisted @Nonnull AppointmentType appointmentType) {
		this(assessmentService, formatter, strings, appointmentType, Collections.emptySet());
	}

	@AssistedInject
	public AppointmentTypeApiResponse(@Nonnull AssessmentService assessmentService,
																		@Nonnull Formatter formatter,
																		@Nonnull Strings strings,
																		@Assisted @Nonnull AppointmentType appointmentType,
																		@Assisted @Nonnull Set<AppointmentTypeApiResponseSupplement> supplements) {
		requireNonNull(assessmentService);
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(appointmentType);
		requireNonNull(supplements);

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
		this.hexColor = appointmentType.getHexColor();
		this.hexColorDescription = format("#%s", Integer.toHexString(appointmentType.getHexColor()));
		this.assessmentId = appointmentType.getAssessmentId();

		if (appointmentType.getAssessmentId() != null && (supplements.contains(AppointmentTypeApiResponseSupplement.ASSESSMENT) || supplements.contains(AppointmentTypeApiResponseSupplement.EVERYTHING))) {
			List<Question> questions = assessmentService.findQuestionsForAssessmentId(appointmentType.getAssessmentId());
			List<PatientIntakeQuestionApiResponse> patientIntakeQuestions = new ArrayList<>(questions.size());
			List<ScreeningQuestionApiResponse> screeningQuestions = new ArrayList<>(questions.size());

			for (Question question : questions) {
				if (question.getQuestionTypeId() == QuestionTypeId.QUAD) {
					// Screening intakes are of type QUAD
					screeningQuestions.add(new ScreeningQuestionApiResponse(question));
				} else if (question.getQuestionTypeId() == QuestionTypeId.TEXT) {
					// Patient intake questions are of type TEXT
					patientIntakeQuestions.add(new PatientIntakeQuestionApiResponse(question));
				} else {
					throw new IllegalStateException(format("Appointment type ID %s: we don't support appointment type " + "assessment questions of type %s", appointmentType.getAppointmentTypeId(), question.getQuestionTypeId().name()));
				}
			}

			this.patientIntakeQuestions = patientIntakeQuestions;
			this.screeningQuestions = screeningQuestions;
		}
	}

	@Nonnull
	public static class PatientIntakeQuestionApiResponse {
		@Nonnull
		private final String question;
		@Nonnull
		private final FontSizeId fontSizeId;
		@Nullable
		private final QuestionContentHintId questionContentHintId;

		public PatientIntakeQuestionApiResponse(@Nonnull Question question) {
			requireNonNull(question);
			this.question = question.getQuestionText();
			this.fontSizeId = question.getFontSizeId();
			this.questionContentHintId = question.getQuestionContentHintId();
		}

		@Nonnull
		public String getQuestion() {
			return question;
		}

		@Nonnull
		public FontSizeId getFontSizeId() {
			return fontSizeId;
		}

		@Nonnull
		public Optional<QuestionContentHintId> getQuestionContentHintId() {
			return Optional.ofNullable(questionContentHintId);
		}
	}

	@Nonnull
	public static class ScreeningQuestionApiResponse {
		@Nonnull
		private final String question;
		@Nonnull
		private final FontSizeId fontSizeId;

		public ScreeningQuestionApiResponse(@Nonnull Question question) {
			requireNonNull(question);
			this.question = question.getQuestionText();
			this.fontSizeId = question.getFontSizeId();
		}

		@Nonnull
		public String getQuestion() {
			return question;
		}

		@Nonnull
		public FontSizeId getFontSizeId() {
			return fontSizeId;
		}
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

	@Nonnull
	public Integer getHexColor() {
		return hexColor;
	}

	@Nonnull
	public String getHexColorDescription() {
		return hexColorDescription;
	}

	@Nullable
	public UUID getAssessmentId() {
		return assessmentId;
	}
}