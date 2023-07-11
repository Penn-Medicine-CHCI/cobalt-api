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

package com.cobaltplatform.api.integration.epic.request;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class AppointmentFindFhirStu3Request {
	@Nullable
	private String patient;
	@Nullable
	private Instant startTime;
	@Nullable
	private Instant endTime;
	@Nullable
	private List<Coding> serviceTypes;
	@Nullable
	private String serviceTypesText;
	@Nullable
	private List<Coding> indications;
	@Nullable
	private String indicationsText;
	@Nullable
	private List<Coding> specialities;
	@Nullable
	private String specialitiesText;
	@Nullable
	private String locationReference;
	@Nullable
	private List<ValueTiming> timesOfDay;

	@Nullable
	public String getPatient() {
		return this.patient;
	}

	public void setPatient(@Nullable String patient) {
		this.patient = patient;
	}

	@Nullable
	public Instant getStartTime() {
		return this.startTime;
	}

	public void setStartTime(@Nullable Instant startTime) {
		this.startTime = startTime;
	}

	@Nullable
	public Instant getEndTime() {
		return this.endTime;
	}

	public void setEndTime(@Nullable Instant endTime) {
		this.endTime = endTime;
	}

	@Nullable
	public List<Coding> getServiceTypes() {
		return this.serviceTypes;
	}

	public void setServiceTypes(@Nullable List<Coding> serviceTypes) {
		this.serviceTypes = serviceTypes;
	}

	@Nullable
	public String getServiceTypesText() {
		return this.serviceTypesText;
	}

	public void setServiceTypesText(@Nullable String serviceTypesText) {
		this.serviceTypesText = serviceTypesText;
	}

	@Nullable
	public List<Coding> getIndications() {
		return this.indications;
	}

	public void setIndications(@Nullable List<Coding> indications) {
		this.indications = indications;
	}

	@Nullable
	public String getIndicationsText() {
		return this.indicationsText;
	}

	public void setIndicationsText(@Nullable String indicationsText) {
		this.indicationsText = indicationsText;
	}

	@Nullable
	public List<Coding> getSpecialities() {
		return this.specialities;
	}

	public void setSpecialities(@Nullable List<Coding> specialities) {
		this.specialities = specialities;
	}

	@Nullable
	public String getSpecialitiesText() {
		return this.specialitiesText;
	}

	public void setSpecialitiesText(@Nullable String specialitiesText) {
		this.specialitiesText = specialitiesText;
	}

	@Nullable
	public String getLocationReference() {
		return this.locationReference;
	}

	public void setLocationReference(@Nullable String locationReference) {
		this.locationReference = locationReference;
	}

	@Nullable
	public List<ValueTiming> getTimesOfDay() {
		return this.timesOfDay;
	}

	public void setTimesOfDay(@Nullable List<ValueTiming> timesOfDay) {
		this.timesOfDay = timesOfDay;
	}

	@NotThreadSafe
	public static class ValueTiming {
		@Nullable
		private Repeat repeat;

		@Nullable
		public Repeat getRepeat() {
			return this.repeat;
		}

		public void setRepeat(@Nullable Repeat repeat) {
			this.repeat = repeat;
		}

		@NotThreadSafe
		public static class Repeat {
			@Nullable
			private Integer durationInMinutes;
			@Nullable
			private List<DayOfWeek> daysOfWeek;
			@Nullable
			private List<LocalTime> timesOfDay;

			@Nullable
			public Integer getDurationInMinutes() {
				return this.durationInMinutes;
			}

			public void setDurationInMinutes(@Nullable Integer durationInMinutes) {
				this.durationInMinutes = durationInMinutes;
			}

			@Nullable
			public List<DayOfWeek> getDaysOfWeek() {
				return this.daysOfWeek;
			}

			public void setDaysOfWeek(@Nullable List<DayOfWeek> daysOfWeek) {
				this.daysOfWeek = daysOfWeek;
			}

			@Nullable
			public List<LocalTime> getTimesOfDay() {
				return this.timesOfDay;
			}

			public void setTimesOfDay(@Nullable List<LocalTime> timesOfDay) {
				this.timesOfDay = timesOfDay;
			}
		}
	}

	/**
	 * See https://build.fhir.org/codesystem-days-of-week.html
	 */
	public enum DayOfWeek {
		MONDAY("mon"),
		TUESDAY("tue"),
		WEDNESDAY("wed"),
		THURSDAY("thu"),
		FRIDAY("fri"),
		SATURDAY("sat"),
		SUNDAY("sun");

		@Nonnull
		private final String epicValue;

		DayOfWeek(@Nonnull String epicValue) {
			requireNonNull(epicValue);
			this.epicValue = epicValue;
		}

		@Nonnull
		public String getEpicValue() {
			return this.epicValue;
		}
	}

	@NotThreadSafe
	public static class Coding {
		@Nullable
		private String system;
		@Nullable
		private String code;
		@Nullable
		private String display;

		@Nullable
		public String getSystem() {
			return this.system;
		}

		public void setSystem(@Nullable String system) {
			this.system = system;
		}

		@Nullable
		public String getCode() {
			return this.code;
		}

		public void setCode(@Nullable String code) {
			this.code = code;
		}

		@Nullable
		public String getDisplay() {
			return this.display;
		}

		public void setDisplay(@Nullable String display) {
			this.display = display;
		}
	}
}
