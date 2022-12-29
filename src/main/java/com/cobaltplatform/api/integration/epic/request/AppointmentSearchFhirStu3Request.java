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
import java.time.LocalDate;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class AppointmentSearchFhirStu3Request {
	@Nullable
	private String patient;
	@Nullable
	private String identifier;
	@Nullable
	private LocalDate date;
	@Nullable
	private Status status;

	@Nullable
	public String getPatient() {
		return this.patient;
	}

	public void setPatient(@Nullable String patient) {
		this.patient = patient;
	}

	@Nullable
	public String getIdentifier() {
		return this.identifier;
	}

	public void setIdentifier(@Nullable String identifier) {
		this.identifier = identifier;
	}

	@Nullable
	public LocalDate getDate() {
		return this.date;
	}

	public void setDate(@Nullable LocalDate date) {
		this.date = date;
	}

	@Nullable
	public Status getStatus() {
		return this.status;
	}

	public void setStatus(@Nullable Status status) {
		this.status = status;
	}

	public enum Status {
		BOOKED("booked"),
		FULFILLED("fulfilled"),
		CANCELLED("cancelled"),
		NOSHOW("noshow"),
		ARRIVED("arrived"),
		PROPOSED("proposed");

		@Nonnull
		private final String epicValue;

		Status(@Nonnull String epicValue) {
			requireNonNull(epicValue);
			this.epicValue = epicValue;
		}

		@Nonnull
		public String getEpicValue() {
			return this.epicValue;
		}
	}
}
